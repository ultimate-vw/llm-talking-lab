package labs.orchestrator.api;
import org.springframework.web.bind.annotation.*; import java.util.*; import java.util.concurrent.ThreadLocalRandom; import org.springframework.beans.factory.annotation.Value; import org.springframework.web.reactive.function.client.WebClient; import org.springframework.http.MediaType;
@RestController @RequestMapping("/api/v1")
public class RestApi {
  record RestReq(String prompt, String shape) {}
  record RestResp(String answer, Map<String,Object> meta) {}
  private final WebClient http = WebClient.builder().build();
  @Value("${ollama.url:http://localhost:11434}") String ollamaUrl;
  @Value("${ollama.model:llama3.1}") String ollamaModel;
  @PostMapping("/rest")
  public RestResp rest(@RequestBody RestReq req){
    long t0=System.currentTimeMillis();
    String prompt=req.prompt()==null?"":req.prompt();
    try{
      Map<String,Object> payload=new HashMap<>(); payload.put("model", ollamaModel); payload.put("prompt", prompt); payload.put("stream", false);
      Map<String,Object> resp=http.post().uri(ollamaUrl+"/api/generate").contentType(MediaType.APPLICATION_JSON).bodyValue(payload).retrieve().bodyToMono(Map.class).block();
      String ans=String.valueOf(resp.getOrDefault("response",""));
      return new RestResp(ans, Map.of("ttfb_ms",0,"total_ms",System.currentTimeMillis()-t0,"requestId",UUID.randomUUID().toString(),"provider","ollama","model",ollamaModel));
    }catch(Exception e){
      String ans="Echo: "+prompt; // fallback
      return new RestResp(ans, Map.of("ttfb_ms",0,"total_ms",System.currentTimeMillis()-t0,"requestId",UUID.randomUUID().toString(),"error",e.getMessage()));
    }
  }
  @PostMapping("/longpoll")
  public Map<String,Object> longpoll(@RequestBody RestReq req) throws InterruptedException {
    Thread.sleep(1200+ThreadLocalRandom.current().nextInt(300));
    return Map.of("answer","Echo: "+req.prompt(),"progress",100);
  }
}
