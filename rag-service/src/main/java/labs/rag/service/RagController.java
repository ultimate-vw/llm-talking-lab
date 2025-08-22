package labs.rag.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

@RestController
@RequestMapping("/")
public class RagController {

    // In-memory index compatible with Lucene 9+
    private final Directory memoryIndex = new ByteBuffersDirectory();
    private final Analyzer analyzer = new StandardAnalyzer();

    // Use an external folder for corpus so it works in a fat JAR and Docker
    @Value("${rag.corpusDir:./data/corpus}")
    private String corpusDir;

    @PostMapping("/ingest")
    public Map<String, Object> ingest() throws Exception {
        Path corpus = Paths.get(corpusDir);
        Files.createDirectories(corpus);

        int chunks = 0;
        int docs = 0;

        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        try (IndexWriter w = new IndexWriter(memoryIndex, cfg)) {
            w.deleteAll();

            try (DirectoryStream<Path> ds = Files.newDirectoryStream(corpus)) {
                for (Path p : ds) {
                    String name = p.getFileName().toString();
                    if (!(name.endsWith(".txt") || name.endsWith(".md"))) continue;
                    docs++;

                    String text = Files.readString(p);
                    List<String> cs = chunk(text, 500, 50);
                    for (int i = 0; i < cs.size(); i++) {
                        Document doc = new Document();
                        doc.add(new StoredField("doc", name));
                        doc.add(new StoredField("chunk_id", i));
                        doc.add(new TextField("content", cs.get(i), Field.Store.NO));
                        w.addDocument(doc);
                        chunks++;
                    }
                }
            }

            w.commit();
        }

        Map<String, Object> out = new HashMap<String, Object>();
        out.put("ok", Boolean.TRUE);
        out.put("chunks", chunks);
        out.put("docs", docs);
        return out;
    }

    @GetMapping("/query")
    public Map<String, Object> query(@RequestParam String q,
                                     @RequestParam(defaultValue = "5") int k) throws Exception {
        QueryParser qp = new QueryParser("content", analyzer);
        Query query = qp.parse(q);

        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try (IndexReader reader = DirectoryReader.open(memoryIndex)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs td = searcher.search(query, k);
            for (ScoreDoc sd : td.scoreDocs) {
                Document d = searcher.doc(sd.doc);
                Map<String, Object> hit = new HashMap<String, Object>();
                hit.put("doc", d.get("doc"));
                hit.put("chunk_id", Integer.valueOf(d.get("chunk_id")));
                hit.put("score", Float.valueOf(sd.score));
                results.add(hit);
            }
        }

        Map<String, Object> out = new HashMap<String, Object>();
        out.put("q", q);
        out.put("results", results);
        return out;
    }

    @GetMapping(value = "/query-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter queryStream(@RequestParam String q,
                                  @RequestParam(defaultValue = "5") int k) {
        final SseEmitter sse = new SseEmitter(0L);

        Executors.newSingleThreadExecutor().submit(() -> {
            IndexReader reader = null;
            try {
                Query query = new QueryParser("content", analyzer).parse(q);
                reader = DirectoryReader.open(memoryIndex);
                IndexSearcher searcher = new IndexSearcher(reader);
                TopDocs td = searcher.search(query, k);

                int i = 1;
                for (ScoreDoc sd : td.scoreDocs) {
                    Document d = searcher.doc(sd.doc);
                    Map<String, Object> hit = new HashMap<String, Object>();
                    hit.put("doc", d.get("doc"));
                    hit.put("chunk_id", Integer.valueOf(d.get("chunk_id")));
                    hit.put("score", Float.valueOf(sd.score));

                    sse.send(SseEmitter.event()
                            .id(String.valueOf(i++))
                            .name("hit")
                            .data(hit));

                    Thread.sleep(60);
                }
                sse.send(SseEmitter.event().name("done").data("[DONE]"));
            } catch (Exception e) {
                try { sse.send(SseEmitter.event().name("error").data(e.getMessage())); }
                catch (IOException ignore) {}
                sse.completeWithError(e);
            } finally {
                try { if (reader != null) reader.close(); } catch (IOException ignore) {}
                sse.complete();
            }
        });

        return sse;
    }

    private static List<String> chunk(String text, int size, int overlap) {
        String[] words = text.split("\\s+");
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < words.length; i += Math.max(1, size - overlap)) {
            int end = Math.min(words.length, i + size);
            out.add(String.join(" ", Arrays.asList(words).subList(i, end)));
        }
        return out;
    }
}
