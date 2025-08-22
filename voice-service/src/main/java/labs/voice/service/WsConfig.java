package labs.voice.service;
import org.springframework.context.annotation.Configuration; import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.*; import org.springframework.web.socket.handler.TextWebSocketHandler;

@Configuration @EnableWebSocket
public class WsConfig implements WebSocketConfigurer {
  @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry reg){ reg.addHandler(new Echo(), "/ws").setAllowedOrigins("*"); }
  static class Echo extends TextWebSocketHandler {
    @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
      String text=message.getPayload().toLowerCase().contains("time") ? "It is " + java.time.LocalTime.now().withNano(0) : "You said: " + message.getPayload();
      for(String tok:text.split("\\s+")){ session.sendMessage(new TextMessage(tok+" ")); Thread.sleep(50); }
    }
  }
}
