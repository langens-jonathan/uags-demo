package delta_service.config;

import delta_service.callback.CallBackService;
import delta_service.graphs.GraphService;
import delta_service.query.QueryService;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {QueryService.class, CallBackService.class, GraphService.class})
public class RootConfiguration {
}
