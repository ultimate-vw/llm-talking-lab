package labs.orchestrator.ws;
import org.springframework.context.annotation.Configuration; import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.*; import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration @EnableWebSocket
public class WsConfig implements WebSocketConfigurer {
  @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry reg){ reg.addHandler(new StreamHandler(), "/api/v1/ws").setAllowedOrigins("*"); }
  static class StreamHandler extends TextWebSocketHandler {
    private final ObjectMapper om = new ObjectMapper();
    @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
      var node=om.readTree(message.getPayload());
      String prompt=node.path("prompt").asText("");
      session.sendMessage(new TextMessage(om.createObjectNode().put("type","plan").putObject("data").put("tool","mock_llm").toString()));
      for(String tok:("Echo: "+prompt).split(" ")){
        session.sendMessage(new TextMessage(om.createObjectNode().put("type","token").put("data",tok+" ").toString()));
        Thread.sleep(50);
      }
      session.sendMessage(new TextMessage(om.createObjectNode().put("type","final").put("data","Echo: "+prompt).toString()));
      session.sendMessage(new TextMessage(om.createObjectNode().put("type","done").toString()));
    }
  }
}
