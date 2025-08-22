package labs.orchestrator.api;
import org.springframework.web.bind.annotation.*; import java.util.*; import java.util.concurrent.ThreadLocalRandom;
@RestController @RequestMapping("/api/v1")
public class RestApi {
  record RestReq(String prompt, String shape) {}
  record RestResp(String answer, Map<String,Object> meta) {}
  @PostMapping("/rest")
  public RestResp rest(@RequestBody RestReq req){
    long t0=System.currentTimeMillis();
    String ans="Echo: "+(req.prompt()==null?"":req.prompt());
    return new RestResp(ans, Map.of("ttfb_ms",0,"total_ms",System.currentTimeMillis()-t0,"requestId",UUID.randomUUID().toString()));
  }
  @PostMapping("/longpoll")
  public Map<String,Object> longpoll(@RequestBody RestReq req) throws InterruptedException {
    Thread.sleep(1200+ThreadLocalRandom.current().nextInt(300));
    return Map.of("answer","Echo: "+req.prompt(),"progress",100);
  }
}
