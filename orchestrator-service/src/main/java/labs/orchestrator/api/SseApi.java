package labs.orchestrator.api;
import org.springframework.http.MediaType; import org.springframework.web.bind.annotation.*; import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException; import java.util.*; import java.util.concurrent.*;

@RestController @RequestMapping("/api/v1")
public class SseApi {
  @GetMapping(value="/sse", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter sse(@RequestParam String prompt){
    SseEmitter sse=new SseEmitter(0L); String requestId=UUID.randomUUID().toString();
    Executors.newSingleThreadExecutor().submit(() -> {
      try{
        sse.send(SseEmitter.event().name("meta").data("{\"requestId\":\""+requestId+"\"}"));
        int i=1; for(String tok:("Echo: "+prompt).split(" ")){
          sse.send(SseEmitter.event().id(String.valueOf(i++)).name("token").data(tok+" "));
          Thread.sleep(50);
        }
        sse.send(SseEmitter.event().name("done").data("[DONE]"));
      }catch(Exception e){ try{ sse.send(SseEmitter.event().name("error").data(e.getMessage())); }catch(IOException ignored){} sse.completeWithError(e); }
      finally{ sse.complete(); }
    });
    return sse;
  }
}
