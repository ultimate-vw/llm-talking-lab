package labs.orchestrator.api;
import org.springframework.http.MediaType; import org.springframework.web.bind.annotation.*; import org.springframework.web.servlet.mvc.method.annotation.SseEmitter; import org.springframework.beans.factory.annotation.Value; import org.springframework.web.reactive.function.client.WebClient; import reactor.core.publisher.Flux;
import java.io.IOException; import java.util.*; import java.util.concurrent.*;

@RestController @RequestMapping("/api/v1")
public class SseApi {
  private final WebClient http = WebClient.builder().build();
  @Value("${ollama.url:http://localhost:11434}") String ollamaUrl;
  @Value("${ollama.model:llama3.1}") String ollamaModel;
  @GetMapping(value="/sse", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter sse(@RequestParam("prompt") String prompt){
    SseEmitter sse=new SseEmitter(0L); String requestId=UUID.randomUUID().toString();
    Executors.newSingleThreadExecutor().submit(() -> {
      try{
        sse.send(SseEmitter.event().name("meta").data("{\"requestId\":\""+requestId+"\",\"provider\":\"ollama\",\"model\":\""+ollamaModel+"\"}"));
        Map<String,Object> payload=new HashMap<>(); payload.put("model", ollamaModel); payload.put("prompt", prompt); payload.put("stream", true);
        Flux<String> stream=http.post().uri(ollamaUrl+"/api/generate").contentType(MediaType.APPLICATION_JSON).bodyValue(payload).retrieve().bodyToFlux(String.class);
        final int[] i={1};
        stream.subscribe(chunk -> {
          try{
            // Ollama streams JSON lines; parse token from "response"
            String token=extractToken(chunk);
            if(token!=null && !token.isEmpty()) sse.send(SseEmitter.event().id(String.valueOf(i[0]++)).name("token").data(token));
          }catch(Exception ignored){}
        }, err -> {
          try{ sse.send(SseEmitter.event().name("error").data(err.getMessage())); }catch(IOException ignored){}
          sse.completeWithError(err);
        }, () -> {
          try{ sse.send(SseEmitter.event().name("done").data("[DONE]")); }catch(IOException ignored){}
        });
      }catch(Exception e){ try{ sse.send(SseEmitter.event().name("error").data(e.getMessage())); }catch(IOException ignored){} sse.completeWithError(e); }
      finally{ sse.complete(); }
    });
    return sse;
  }
  private String extractToken(String json){
    try{
      int idx=json.indexOf("\"response\":"); if(idx<0) return null;
      int start=json.indexOf('"', idx+11); if(start<0) return null; start++;
      int end=json.indexOf('"', start); if(end<0) end=json.length();
      return json.substring(start, end);
    }catch(Exception e){ return null; }
  }
}
