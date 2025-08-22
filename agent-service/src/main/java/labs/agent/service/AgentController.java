package labs.agent.service;
import org.springframework.web.bind.annotation.*; import org.springframework.http.MediaType; import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.beans.factory.annotation.Value; import org.springframework.web.reactive.function.client.WebClient;
import java.util.*; import java.util.concurrent.*;

@RestController @RequestMapping("/")
public class AgentController {
  private final WebClient http = WebClient.builder().build();
  @Value("${rag.url:http://rag:8082}") String ragUrl;

  @GetMapping(value="/run-stream", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter run(@RequestParam String message){
    SseEmitter sse = new SseEmitter(0L);
    Executors.newSingleThreadExecutor().submit(() -> {
      try{
        String tool = pickTool(message);
        sse.send(SseEmitter.event().name("plan").data(Map.of("tool", tool)));
        sse.send(SseEmitter.event().name("action").data(Map.of("tool", tool, "input", message)));

        String obs = switch (tool) {
          case "calc" -> safeCalc(message);
          case "http_get" -> http.get().uri(message).retrieve().bodyToMono(String.class).map(b->b.substring(0,Math.min(400,b.length()))).onErrorReturn("Fetch error").block();
          default -> http.get().uri(ragUrl + "/query?q={q}&k=3", message).retrieve().bodyToMono(String.class).block();
        };
        sse.send(SseEmitter.event().name("observation").data(obs));
        sse.send(SseEmitter.event().name("final").data(obs));
        sse.send(SseEmitter.event().name("done").data("[DONE]"));
      }catch(Exception e){ try{ sse.send(SseEmitter.event().name("error").data(e.getMessage())); }catch(Exception ignored){} sse.completeWithError(e); }
      finally { sse.complete(); }
    });
    return sse;
  }

  private String pickTool(String msg){
    if (msg.matches(".*[\\d\\s\\+\\-\\*/\\(\\)]+.*")) return "calc";
    if (msg.startsWith("http")) return "http_get";
    return "rag_search";
  }
  private String safeCalc(String expr){
    String cleaned = expr.replaceAll("[^0-9\\+\\-\\*/\\(\\)\\.\\s]","").trim();
    try { javax.script.ScriptEngine e = new javax.script.ScriptEngineManager().getEngineByName("JavaScript"); return String.valueOf(e.eval(cleaned)); }
    catch (Exception ex) { return "Calc error"; }
  }
}
