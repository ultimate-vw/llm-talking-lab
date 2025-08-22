package labs.common;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ApiKeyFilter implements Filter {
  private final String header; private final String expected;
  public ApiKeyFilter(String header, String expected){ this.header = header; this.expected = expected; }
  @Override public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
    if (expected==null || expected.isBlank()) { chain.doFilter(req,res); return; }
    HttpServletRequest r=(HttpServletRequest)req; HttpServletResponse w=(HttpServletResponse)res;
    String got = r.getHeader(header);
    if (expected.equals(got)) { chain.doFilter(req,res); }
    else { w.setStatus(401); w.getWriter().write("{\"error\":\"unauthorized\"}"); }
  }
}
