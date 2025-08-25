package labs.agent.service;
import org.springframework.web.bind.annotation.*; import org.springframework.http.MediaType; import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.beans.factory.annotation.Value; import org.springframework.web.reactive.function.client.WebClient;
import java.util.*; import java.util.concurrent.*;

@RestController @RequestMapping("/")
public class AgentController {
  private final WebClient http = WebClient.builder().build();
  @Value("${rag.url:http://rag:8082}") String ragUrl;

  @GetMapping(value="/run-stream", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter run(@RequestParam("message") String message){
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
    try { 
      System.out.println("Original: " + expr);
      System.out.println("Cleaned: " + cleaned);
      return String.valueOf(evaluateExpression(cleaned)); 
    }
    catch (Exception ex) { 
      return "Calc error: " + ex.getMessage(); 
    }
  }

  private double evaluateExpression(String expr) {
    // Simple expression evaluator for basic arithmetic
    System.out.println("Parsing expression: " + expr);
    expr = expr.replaceAll("\\s+", ""); // Remove all whitespace
    System.out.println("After removing spaces: " + expr);
    
    // Simple approach: evaluate left to right with operator precedence
    return evaluateLeftToRight(expr);
  }
  
  private double evaluateLeftToRight(String expr) {
    // Handle parentheses first
    while (expr.contains("(")) {
      int start = expr.lastIndexOf("(");
      int end = expr.indexOf(")", start);
      if (end == -1) throw new IllegalArgumentException("Mismatched parentheses");
      
      String subExpr = expr.substring(start + 1, end);
      double result = evaluateLeftToRight(subExpr);
      expr = expr.substring(0, start) + result + expr.substring(end + 1);
    }
    
    // Split by addition/subtraction (lowest precedence)
    String[] addSubParts = expr.split("(?<=[+-])|(?=[+-])");
    if (addSubParts.length == 1) {
      // No addition/subtraction, evaluate multiplication/division
      return evaluateMultDiv(expr);
    }
    
    double result = evaluateMultDiv(addSubParts[0]);
    for (int i = 1; i < addSubParts.length; i += 2) {
      if (i + 1 < addSubParts.length) {
        double num = evaluateMultDiv(addSubParts[i + 1]);
        if (addSubParts[i].equals("+")) {
          result += num;
        } else if (addSubParts[i].equals("-")) {
          result -= num;
        }
      }
    }
    return result;
  }
  
  private double evaluateMultDiv(String expr) {
    // Split by multiplication/division
    String[] multDivParts = expr.split("(?<=[*/])|(?=[*/])");
    if (multDivParts.length == 1) {
      return Double.parseDouble(expr);
    }
    
    double result = Double.parseDouble(multDivParts[0]);
    for (int i = 1; i < multDivParts.length; i += 2) {
      if (i + 1 < multDivParts.length) {
        double num = Double.parseDouble(multDivParts[i + 1]);
        if (multDivParts[i].equals("*")) {
          result *= num;
        } else if (multDivParts[i].equals("/")) {
          if (num == 0) throw new ArithmeticException("Division by zero");
          result /= num;
        }
      }
    }
    return result;
  }
}
