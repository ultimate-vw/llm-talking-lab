package labs.orchestrator.config;
import labs.common.RequestIdFilter; import labs.common.ApiKeyFilter;
import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.*;
import jakarta.servlet.Filter;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Value("${security.apiKeyHeader:X-API-Key}") String apiHeader;
  @Value("${security.apiKey:}") String apiKey;
  @Bean public Filter requestIdFilter(){ return new RequestIdFilter(); }
  @Bean public Filter apiKeyFilter(){ return new ApiKeyFilter(apiHeader, apiKey); }
  @Override public void addCorsMappings(CorsRegistry r){ r.addMapping("/**").allowedOrigins("*").allowedMethods("*"); }
}
