package labs.common;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import java.io.IOException;
import java.util.UUID;

public class RequestIdFilter implements Filter {
  public static final String HEADER = "X-Request-Id";
  @Override public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest r = (HttpServletRequest) req;
    String id = r.getHeader(HEADER);
    if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
    MDC.put("requestId", id);
    try { chain.doFilter(req, res); } finally { MDC.remove("requestId"); }
  }
}
