package labs.orchestrator.api;
import org.springframework.http.MediaType; import org.springframework.web.bind.annotation.*; import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.*; import java.util.concurrent.*;

@RestController @RequestMapping("/api/v1")
public class BatchApi {
  private final Map<String,String> jobStatus=new ConcurrentHashMap<>();
  private final Map<String,List<SseEmitter>> streams=new ConcurrentHashMap<>();

  record BatchReq(String prompt) {}
  @PostMapping("/batch")
  public Map<String,Object> submit(@RequestBody BatchReq req){
    String id=UUID.randomUUID().toString(); jobStatus.put(id,"QUEUED");
    Executors.newSingleThreadExecutor().submit(() -> runJob(id, req.prompt()));
    return Map.of("jobId", id);
  }
  @GetMapping(value="/jobs/{id}/events", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter events(@PathVariable String id){
    SseEmitter sse=new SseEmitter(0L);
    streams.computeIfAbsent(id,k->new CopyOnWriteArrayList<>()).add(sse);
    sse.onCompletion(() -> streams.getOrDefault(id,List.of()).remove(sse));
    try{ sse.send(SseEmitter.event().name("status").data(jobStatus.getOrDefault(id,"UNKNOWN"))); }catch(Exception ignored){}
    return sse;
  }
  @PostMapping("/webhook")
  public Map<String,Object> webhook(@RequestBody Map<String,Object> payload){ return Map.of("received",true,"payload",payload); }

  private void runJob(String id, String prompt){
    update(id,"PROCESSING"); sleep(600);
    update(id,"VALIDATING"); sleep(400);
    update(id,"DONE"); emit(id,"done","[DONE]");
  }
  private void update(String id,String status){ jobStatus.put(id,status); emit(id,"status",status); }
  private void emit(String id,String name,Object data){
    for(var s:streams.getOrDefault(id,List.of())){ try{ s.send(SseEmitter.event().name(name).data(data)); }catch(Exception ignored){} }
  }
  private void sleep(long ms){ try{ Thread.sleep(ms);}catch(InterruptedException ignored){} }
}
