package labs.docs.service;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/")
public class DocsController {
    private final Path INBOX = Path.of("data","inbox");
    private final Path OUT   = Path.of("data","out");

    private final ConcurrentHashMap<String, Map<String,Object>> jobs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> streams = new ConcurrentHashMap<>();

    public DocsController() throws Exception {
        Files.createDirectories(INBOX);
        Files.createDirectories(OUT);
    }

    @PostMapping(path="/submit", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> submit(@RequestPart("file") MultipartFile file) throws Exception {
        String id = UUID.randomUUID().toString();
        String fname = id + "_" + file.getOriginalFilename();
        Files.copy(file.getInputStream(), INBOX.resolve(fname), StandardCopyOption.REPLACE_EXISTING);

        Map<String,Object> job = new HashMap<>();
        job.put("id", id);
        job.put("filename", fname);
        job.put("status", "QUEUED");

        jobs.put(id, job);
        emit(id, "status", Map.of("status","QUEUED"));

        new Thread(() -> process(id)).start();
        return Map.of("jobId", id, "status","QUEUED");
    }

    @GetMapping(value="/jobs/{id}/events", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable String id){
        SseEmitter sse = new SseEmitter(0L);
        streams.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(sse);
        sse.onCompletion(() -> streams.getOrDefault(id, new CopyOnWriteArrayList<>()).remove(sse));
        return sse;
    }

    private void process(String id){
        try{
            update(id, "PROCESSING");
            Thread.sleep(400);

            String fname = (String) jobs.get(id).get("filename");
            String text = Files.readString(INBOX.resolve(fname));

            String title = Arrays.stream(text.split("\\R")).findFirst().orElse("Untitled");

            double sum = 0.0;
            Matcher m = Pattern.compile("[-+]?\\d*\\.?\\d+").matcher(text);
            while (m.find()) sum += Double.parseDouble(m.group());

            String json = String.format("{\"title\":\"%s\",\"amount\":%.2f,\"currency\":\"USD\"}",
                    title.replace("\"","'"), sum);
            Files.writeString(OUT.resolve(id + ".json"), json);

            update(id, "DONE");
            emit(id, "done", "[DONE]");
        } catch(Exception e) {
            emit(id, "status", Map.of("status","ERROR","error", e.getMessage()));
            emit(id, "done", "[DONE]");
        }
    }

    private void update(String id, String status){
        jobs.get(id).put("status", status);
        emit(id, "status", Map.of("status", status));
    }

    private void emit(String id, String name, Object data){
        CopyOnWriteArrayList<SseEmitter> list =
                streams.getOrDefault(id, new CopyOnWriteArrayList<SseEmitter>());
        for (SseEmitter s : list) {
            try {
                s.send(SseEmitter.event().name(name).data(data));
            } catch (Exception ignored) {}
        }
    }
}
