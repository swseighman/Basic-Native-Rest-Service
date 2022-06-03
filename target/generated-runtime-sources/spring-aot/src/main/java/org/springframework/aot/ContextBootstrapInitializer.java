package org.springframework.aot;

import com.example.restservice.GreetingController;
import com.example.restservice.RestServiceApplication;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import java.lang.Override;
import java.lang.String;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.MultipartConfigElement;
import org.springframework.aot.beans.factory.BeanDefinitionRegistrar;
import org.springframework.aot.context.annotation.InitDestroyBeanPostProcessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.boot.actuate.autoconfigure.availability.AvailabilityHealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.ServletEndpointManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.servlet.WebMvcEndpointManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.info.InfoContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoContributorProperties;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.JvmMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.LogbackMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.PropertiesMeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.startup.StartupTimeMetricsListenerAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.task.TaskExecutorMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.client.HttpClientMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.tomcat.TomcatMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthIndicatorProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ManagementServletContext;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoker.cache.CachingOperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableServletEndpoint;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.boot.actuate.endpoint.web.ServletEndpointRegistrar;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ExposableControllerEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.web.servlet.ControllerEndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.boot.actuate.health.PingHealthIndicator;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.actuate.metrics.startup.StartupTimeMetricsListener;
import org.springframework.boot.actuate.metrics.system.DiskSpaceMetricsBinder;
import org.springframework.boot.actuate.metrics.web.servlet.DefaultWebMvcTagsProvider;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsProvider;
import org.springframework.boot.actuate.metrics.web.tomcat.TomcatMetricsBinder;
import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.LifecycleAutoConfiguration;
import org.springframework.boot.autoconfigure.context.LifecycleProperties;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.autoconfigure.info.ProjectInfoProperties;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationProperties;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateBuilderConfigurer;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.embedded.TomcatWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath;
import org.springframework.boot.autoconfigure.web.servlet.HttpEncodingAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.TomcatServletWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.boot.availability.ApplicationAvailabilityBean;
import org.springframework.boot.context.properties.BoundConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.boot.jackson.JsonComponentModule;
import org.springframework.boot.jackson.JsonMixinModule;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.boot.validation.beanvalidation.MethodValidationExcludeFilter;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.ErrorPageRegistrarBeanPostProcessor;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.filter.OrderedFormContentFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.context.support.DefaultLifecycleProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.PathMatcher;
import org.springframework.validation.Validator;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.method.support.CompositeUriComponentsContributor;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.function.support.HandlerFunctionAdapter;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPatternParser;

public class ContextBootstrapInitializer implements ApplicationContextInitializer<GenericApplicationContext> {
  private InitDestroyBeanPostProcessor createInitDestroyBeanPostProcessor(
      ConfigurableBeanFactory beanFactory) {
    Map<String, List<String>> initMethods = new LinkedHashMap<>();
    Map<String, List<String>> destroyMethods = new LinkedHashMap<>();
    destroyMethods.put("prometheusMeterRegistry", List.of("close"));
    return new InitDestroyBeanPostProcessor(beanFactory, initMethods, destroyMethods);
  }

  @Override
  public void initialize(GenericApplicationContext context) {
    // infrastructure
    DefaultListableBeanFactory beanFactory = context.getDefaultListableBeanFactory();
    beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
    beanFactory.addBeanPostProcessor(createInitDestroyBeanPostProcessor(beanFactory));

    BeanDefinitionRegistrar.of("com.example.restservice.RestServiceApplication", RestServiceApplication.class)
        .instanceSupplier(RestServiceApplication::new).register(beanFactory);
    BeanDefinitionRegistrar.of("greetingController", GreetingController.class)
        .instanceSupplier(GreetingController::new).register(beanFactory);
    org.springframework.boot.autoconfigure.ContextBootstrapInitializer.registerAutoConfigurationPackages_BasePackages(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration", PropertyPlaceholderAutoConfiguration.class)
        .instanceSupplier(PropertyPlaceholderAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("propertySourcesPlaceholderConfigurer", PropertySourcesPlaceholderConfigurer.class).withFactoryMethod(PropertyPlaceholderAutoConfiguration.class, "propertySourcesPlaceholderConfigurer")
        .instanceSupplier(() -> PropertyPlaceholderAutoConfiguration.propertySourcesPlaceholderConfigurer()).register(beanFactory);
    org.springframework.boot.autoconfigure.websocket.servlet.ContextBootstrapInitializer.registerWebSocketServletAutoConfiguration_TomcatWebSocketConfiguration(beanFactory);
    org.springframework.boot.autoconfigure.websocket.servlet.ContextBootstrapInitializer.registerTomcatWebSocketConfiguration_websocketServletWebServerCustomizer(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration", WebSocketServletAutoConfiguration.class)
        .instanceSupplier(WebSocketServletAutoConfiguration::new).register(beanFactory);
    org.springframework.boot.autoconfigure.web.servlet.ContextBootstrapInitializer.registerServletWebServerFactoryConfiguration_EmbeddedTomcat(beanFactory);
    org.springframework.boot.autoconfigure.web.servlet.ContextBootstrapInitializer.registerEmbeddedTomcat_tomcatServletWebServerFactory(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration", ServletWebServerFactoryAutoConfiguration.class)
        .instanceSupplier(ServletWebServerFactoryAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("servletWebServerFactoryCustomizer", ServletWebServerFactoryCustomizer.class).withFactoryMethod(ServletWebServerFactoryAutoConfiguration.class, "servletWebServerFactoryCustomizer", ServerProperties.class, ObjectProvider.class, ObjectProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(ServletWebServerFactoryAutoConfiguration.class).servletWebServerFactoryCustomizer(attributes.get(0), attributes.get(1), attributes.get(2)))).register(beanFactory);
    BeanDefinitionRegistrar.of("tomcatServletWebServerFactoryCustomizer", TomcatServletWebServerFactoryCustomizer.class).withFactoryMethod(ServletWebServerFactoryAutoConfiguration.class, "tomcatServletWebServerFactoryCustomizer", ServerProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(ServletWebServerFactoryAutoConfiguration.class).tomcatServletWebServerFactoryCustomizer(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor", ConfigurationPropertiesBindingPostProcessor.class)
        .instanceSupplier(ConfigurationPropertiesBindingPostProcessor::new).customize((bd) -> bd.setRole(2)).register(beanFactory);
    org.springframework.boot.context.properties.ContextBootstrapInitializer.registerConfigurationPropertiesBinder_Factory(beanFactory);
    org.springframework.boot.context.properties.ContextBootstrapInitializer.registerConfigurationPropertiesBinder(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.context.properties.BoundConfigurationProperties", BoundConfigurationProperties.class)
        .instanceSupplier(BoundConfigurationProperties::new).customize((bd) -> bd.setRole(2)).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.context.properties.EnableConfigurationPropertiesRegistrar.methodValidationExcludeFilter", MethodValidationExcludeFilter.class)
        .instanceSupplier(() -> MethodValidationExcludeFilter.byAnnotation(ConfigurationProperties.class)).customize((bd) -> bd.setRole(2)).register(beanFactory);
    BeanDefinitionRegistrar.of("server-org.springframework.boot.autoconfigure.web.ServerProperties", ServerProperties.class)
        .instanceSupplier(ServerProperties::new).register(beanFactory);
    BeanDefinitionRegistrar.of("webServerFactoryCustomizerBeanPostProcessor", WebServerFactoryCustomizerBeanPostProcessor.class)
        .instanceSupplier(WebServerFactoryCustomizerBeanPostProcessor::new).customize((bd) -> bd.setSynthetic(true)).register(beanFactory);
    BeanDefinitionRegistrar.of("errorPageRegistrarBeanPostProcessor", ErrorPageRegistrarBeanPostProcessor.class)
        .instanceSupplier(ErrorPageRegistrarBeanPostProcessor::new).customize((bd) -> bd.setSynthetic(true)).register(beanFactory);
    org.springframework.boot.autoconfigure.web.servlet.ContextBootstrapInitializer.registerDispatcherServletAutoConfiguration_DispatcherServletConfiguration(beanFactory);
    org.springframework.boot.autoconfigure.web.servlet.ContextBootstrapInitializer.registerDispatcherServletConfiguration_dispatcherServlet(beanFactory);
    BeanDefinitionRegistrar.of("spring.mvc-org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties", WebMvcProperties.class)
        .instanceSupplier(WebMvcProperties::new).register(beanFactory);
    org.springframework.boot.autoconfigure.web.servlet.ContextBootstrapInitializer.registerDispatcherServletAutoConfiguration_DispatcherServletRegistrationConfiguration(beanFactory);
    org.springframework.boot.autoconfigure.web.servlet.ContextBootstrapInitializer.registerDispatcherServletRegistrationConfiguration_dispatcherServletRegistration(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration", DispatcherServletAutoConfiguration.class)
        .instanceSupplier(DispatcherServletAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration", TaskExecutionAutoConfiguration.class)
        .instanceSupplier(TaskExecutionAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("taskExecutorBuilder", TaskExecutorBuilder.class).withFactoryMethod(TaskExecutionAutoConfiguration.class, "taskExecutorBuilder", TaskExecutionProperties.class, ObjectProvider.class, ObjectProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(TaskExecutionAutoConfiguration.class).taskExecutorBuilder(attributes.get(0), attributes.get(1), attributes.get(2)))).register(beanFactory);
    BeanDefinitionRegistrar.of("applicationTaskExecutor", ThreadPoolTaskExecutor.class).withFactoryMethod(TaskExecutionAutoConfiguration.class, "applicationTaskExecutor", TaskExecutorBuilder.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(TaskExecutionAutoConfiguration.class).applicationTaskExecutor(attributes.get(0)))).customize((bd) -> bd.setLazyInit(true)).register(beanFactory);
    BeanDefinitionRegistrar.of("spring.task.execution-org.springframework.boot.autoconfigure.task.TaskExecutionProperties", TaskExecutionProperties.class)
        .instanceSupplier(TaskExecutionProperties::new).register(beanFactory);
    org.springframework.boot.autoconfigure.web.servlet.error.ContextBootstrapInitializer.registerErrorMvcAutoConfiguration_WhitelabelErrorViewConfiguration(beanFactory);
    org.springframework.boot.autoconfigure.web.servlet.error.ContextBootstrapInitializer.registerWhitelabelErrorViewConfiguration_error(beanFactory);
    org.springframework.boot.autoconfigure.web.servlet.error.ContextBootstrapInitializer.registerWhitelabelErrorViewConfiguration_beanNameViewResolver(beanFactory);
    org.springframework.boot.autoconfigure.web.servlet.error.ContextBootstrapInitializer.registerErrorMvcAutoConfiguration_DefaultErrorViewResolverConfiguration(beanFactory);
    org.springframework.boot.autoconfigure.web.servlet.error.ContextBootstrapInitializer.registerDefaultErrorViewResolverConfiguration_conventionErrorViewResolver(beanFactory);
    BeanDefinitionRegistrar.of("spring.web-org.springframework.boot.autoconfigure.web.WebProperties", WebProperties.class)
        .instanceSupplier(WebProperties::new).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration", ErrorMvcAutoConfiguration.class).withConstructor(ServerProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> new ErrorMvcAutoConfiguration(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("errorAttributes", DefaultErrorAttributes.class).withFactoryMethod(ErrorMvcAutoConfiguration.class, "errorAttributes")
        .instanceSupplier(() -> beanFactory.getBean(ErrorMvcAutoConfiguration.class).errorAttributes()).register(beanFactory);
    BeanDefinitionRegistrar.of("basicErrorController", BasicErrorController.class).withFactoryMethod(ErrorMvcAutoConfiguration.class, "basicErrorController", ErrorAttributes.class, ObjectProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(ErrorMvcAutoConfiguration.class).basicErrorController(attributes.get(0), attributes.get(1)))).register(beanFactory);
    org.springframework.boot.autoconfigure.web.servlet.error.ContextBootstrapInitializer.registerErrorMvcAutoConfiguration_errorPageCustomizer(beanFactory);
    org.springframework.boot.autoconfigure.web.servlet.error.ContextBootstrapInitializer.registerErrorMvcAutoConfiguration_preserveErrorControllerTargetClassPostProcessor(beanFactory);
    org.springframework.boot.autoconfigure.web.servlet.ContextBootstrapInitializer.registerWebMvcAutoConfiguration_EnableWebMvcConfiguration(beanFactory);
    BeanDefinitionRegistrar.of("requestMappingHandlerAdapter", RequestMappingHandlerAdapter.class).withFactoryMethod(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class, "requestMappingHandlerAdapter", ContentNegotiationManager.class, FormattingConversionService.class, Validator.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).requestMappingHandlerAdapter(attributes.get(0), attributes.get(1), attributes.get(2)))).register(beanFactory);
    org.springframework.boot.autoconfigure.web.servlet.ContextBootstrapInitializer.registerEnableWebMvcConfiguration_welcomePageHandlerMapping(beanFactory);
    BeanDefinitionRegistrar.of("localeResolver", LocaleResolver.class).withFactoryMethod(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class, "localeResolver")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).localeResolver()).register(beanFactory);
    BeanDefinitionRegistrar.of("themeResolver", ThemeResolver.class).withFactoryMethod(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class, "themeResolver")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).themeResolver()).register(beanFactory);
    BeanDefinitionRegistrar.of("flashMapManager", FlashMapManager.class).withFactoryMethod(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class, "flashMapManager")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).flashMapManager()).register(beanFactory);
    BeanDefinitionRegistrar.of("mvcConversionService", FormattingConversionService.class).withFactoryMethod(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class, "mvcConversionService")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).mvcConversionService()).register(beanFactory);
    BeanDefinitionRegistrar.of("mvcValidator", Validator.class).withFactoryMethod(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class, "mvcValidator")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).mvcValidator()).register(beanFactory);
    BeanDefinitionRegistrar.of("mvcContentNegotiationManager", ContentNegotiationManager.class).withFactoryMethod(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class, "mvcContentNegotiationManager")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).mvcContentNegotiationManager()).register(beanFactory);
    BeanDefinitionRegistrar.of("requestMappingHandlerMapping", RequestMappingHandlerMapping.class).withFactoryMethod(WebMvcConfigurationSupport.class, "requestMappingHandlerMapping", ContentNegotiationManager.class, FormattingConversionService.class, ResourceUrlProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(WebMvcConfigurationSupport.class).requestMappingHandlerMapping(attributes.get(0), attributes.get(1), attributes.get(2)))).register(beanFactory);
    BeanDefinitionRegistrar.of("mvcPatternParser", PathPatternParser.class).withFactoryMethod(WebMvcConfigurationSupport.class, "mvcPatternParser")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcConfigurationSupport.class).mvcPatternParser()).register(beanFactory);
    BeanDefinitionRegistrar.of("mvcUrlPathHelper", UrlPathHelper.class).withFactoryMethod(WebMvcConfigurationSupport.class, "mvcUrlPathHelper")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcConfigurationSupport.class).mvcUrlPathHelper()).register(beanFactory);
    BeanDefinitionRegistrar.of("mvcPathMatcher", PathMatcher.class).withFactoryMethod(WebMvcConfigurationSupport.class, "mvcPathMatcher")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcConfigurationSupport.class).mvcPathMatcher()).register(beanFactory);
    BeanDefinitionRegistrar.of("viewControllerHandlerMapping", HandlerMapping.class).withFactoryMethod(WebMvcConfigurationSupport.class, "viewControllerHandlerMapping", FormattingConversionService.class, ResourceUrlProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(WebMvcConfigurationSupport.class).viewControllerHandlerMapping(attributes.get(0), attributes.get(1)))).register(beanFactory);
    BeanDefinitionRegistrar.of("beanNameHandlerMapping", BeanNameUrlHandlerMapping.class).withFactoryMethod(WebMvcConfigurationSupport.class, "beanNameHandlerMapping", FormattingConversionService.class, ResourceUrlProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(WebMvcConfigurationSupport.class).beanNameHandlerMapping(attributes.get(0), attributes.get(1)))).register(beanFactory);
    BeanDefinitionRegistrar.of("routerFunctionMapping", RouterFunctionMapping.class).withFactoryMethod(WebMvcConfigurationSupport.class, "routerFunctionMapping", FormattingConversionService.class, ResourceUrlProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(WebMvcConfigurationSupport.class).routerFunctionMapping(attributes.get(0), attributes.get(1)))).register(beanFactory);
    BeanDefinitionRegistrar.of("resourceHandlerMapping", HandlerMapping.class).withFactoryMethod(WebMvcConfigurationSupport.class, "resourceHandlerMapping", ContentNegotiationManager.class, FormattingConversionService.class, ResourceUrlProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(WebMvcConfigurationSupport.class).resourceHandlerMapping(attributes.get(0), attributes.get(1), attributes.get(2)))).register(beanFactory);
    BeanDefinitionRegistrar.of("mvcResourceUrlProvider", ResourceUrlProvider.class).withFactoryMethod(WebMvcConfigurationSupport.class, "mvcResourceUrlProvider")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcConfigurationSupport.class).mvcResourceUrlProvider()).register(beanFactory);
    BeanDefinitionRegistrar.of("defaultServletHandlerMapping", HandlerMapping.class).withFactoryMethod(WebMvcConfigurationSupport.class, "defaultServletHandlerMapping")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcConfigurationSupport.class).defaultServletHandlerMapping()).register(beanFactory);
    BeanDefinitionRegistrar.of("handlerFunctionAdapter", HandlerFunctionAdapter.class).withFactoryMethod(WebMvcConfigurationSupport.class, "handlerFunctionAdapter")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcConfigurationSupport.class).handlerFunctionAdapter()).register(beanFactory);
    BeanDefinitionRegistrar.of("mvcUriComponentsContributor", CompositeUriComponentsContributor.class).withFactoryMethod(WebMvcConfigurationSupport.class, "mvcUriComponentsContributor", FormattingConversionService.class, RequestMappingHandlerAdapter.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(WebMvcConfigurationSupport.class).mvcUriComponentsContributor(attributes.get(0), attributes.get(1)))).register(beanFactory);
    BeanDefinitionRegistrar.of("httpRequestHandlerAdapter", HttpRequestHandlerAdapter.class).withFactoryMethod(WebMvcConfigurationSupport.class, "httpRequestHandlerAdapter")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcConfigurationSupport.class).httpRequestHandlerAdapter()).register(beanFactory);
    BeanDefinitionRegistrar.of("simpleControllerHandlerAdapter", SimpleControllerHandlerAdapter.class).withFactoryMethod(WebMvcConfigurationSupport.class, "simpleControllerHandlerAdapter")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcConfigurationSupport.class).simpleControllerHandlerAdapter()).register(beanFactory);
    BeanDefinitionRegistrar.of("handlerExceptionResolver", HandlerExceptionResolver.class).withFactoryMethod(WebMvcConfigurationSupport.class, "handlerExceptionResolver", ContentNegotiationManager.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(WebMvcConfigurationSupport.class).handlerExceptionResolver(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("mvcViewResolver", ViewResolver.class).withFactoryMethod(WebMvcConfigurationSupport.class, "mvcViewResolver", ContentNegotiationManager.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(WebMvcConfigurationSupport.class).mvcViewResolver(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("mvcHandlerMappingIntrospector", HandlerMappingIntrospector.class).withFactoryMethod(WebMvcConfigurationSupport.class, "mvcHandlerMappingIntrospector")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcConfigurationSupport.class).mvcHandlerMappingIntrospector()).customize((bd) -> bd.setLazyInit(true)).register(beanFactory);
    BeanDefinitionRegistrar.of("viewNameTranslator", RequestToViewNameTranslator.class).withFactoryMethod(WebMvcConfigurationSupport.class, "viewNameTranslator")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcConfigurationSupport.class).viewNameTranslator()).register(beanFactory);
    org.springframework.boot.autoconfigure.web.servlet.ContextBootstrapInitializer.registerWebMvcAutoConfiguration_WebMvcAutoConfigurationAdapter(beanFactory);
    BeanDefinitionRegistrar.of("defaultViewResolver", InternalResourceViewResolver.class).withFactoryMethod(WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter.class, "defaultViewResolver")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter.class).defaultViewResolver()).register(beanFactory);
    BeanDefinitionRegistrar.of("viewResolver", ContentNegotiatingViewResolver.class).withFactoryMethod(WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter.class, "viewResolver", BeanFactory.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter.class).viewResolver(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("requestContextFilter", RequestContextFilter.class).withFactoryMethod(WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter.class, "requestContextFilter")
        .instanceSupplier(() -> WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter.requestContextFilter()).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration", WebMvcAutoConfiguration.class)
        .instanceSupplier(WebMvcAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("formContentFilter", OrderedFormContentFilter.class).withFactoryMethod(WebMvcAutoConfiguration.class, "formContentFilter")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcAutoConfiguration.class).formContentFilter()).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration", ApplicationAvailabilityAutoConfiguration.class)
        .instanceSupplier(ApplicationAvailabilityAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("applicationAvailability", ApplicationAvailabilityBean.class).withFactoryMethod(ApplicationAvailabilityAutoConfiguration.class, "applicationAvailability")
        .instanceSupplier(() -> beanFactory.getBean(ApplicationAvailabilityAutoConfiguration.class).applicationAvailability()).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.availability.AvailabilityHealthContributorAutoConfiguration", AvailabilityHealthContributorAutoConfiguration.class)
        .instanceSupplier(AvailabilityHealthContributorAutoConfiguration::new).register(beanFactory);
    org.springframework.boot.autoconfigure.jackson.ContextBootstrapInitializer.registerJacksonAutoConfiguration_Jackson2ObjectMapperBuilderCustomizerConfiguration(beanFactory);
    org.springframework.boot.autoconfigure.jackson.ContextBootstrapInitializer.registerJackson2ObjectMapperBuilderCustomizerConfiguration_standardJacksonObjectMapperBuilderCustomizer(beanFactory);
    BeanDefinitionRegistrar.of("spring.jackson-org.springframework.boot.autoconfigure.jackson.JacksonProperties", JacksonProperties.class)
        .instanceSupplier(JacksonProperties::new).register(beanFactory);
    org.springframework.boot.autoconfigure.jackson.ContextBootstrapInitializer.registerJacksonAutoConfiguration_JacksonObjectMapperBuilderConfiguration(beanFactory);
    org.springframework.boot.autoconfigure.jackson.ContextBootstrapInitializer.registerJacksonObjectMapperBuilderConfiguration_jacksonObjectMapperBuilder(beanFactory);
    org.springframework.boot.autoconfigure.jackson.ContextBootstrapInitializer.registerJacksonAutoConfiguration_ParameterNamesModuleConfiguration(beanFactory);
    org.springframework.boot.autoconfigure.jackson.ContextBootstrapInitializer.registerParameterNamesModuleConfiguration_parameterNamesModule(beanFactory);
    org.springframework.boot.autoconfigure.jackson.ContextBootstrapInitializer.registerJacksonAutoConfiguration_JacksonObjectMapperConfiguration(beanFactory);
    org.springframework.boot.autoconfigure.jackson.ContextBootstrapInitializer.registerJacksonObjectMapperConfiguration_jacksonObjectMapper(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration", JacksonAutoConfiguration.class)
        .instanceSupplier(JacksonAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("jsonComponentModule", JsonComponentModule.class).withFactoryMethod(JacksonAutoConfiguration.class, "jsonComponentModule")
        .instanceSupplier(() -> beanFactory.getBean(JacksonAutoConfiguration.class).jsonComponentModule()).register(beanFactory);
    BeanDefinitionRegistrar.of("jsonMixinModule", JsonMixinModule.class).withFactoryMethod(JacksonAutoConfiguration.class, "jsonMixinModule", ApplicationContext.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(JacksonAutoConfiguration.class).jsonMixinModule(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration", ServletManagementContextAutoConfiguration.class)
        .instanceSupplier(ServletManagementContextAutoConfiguration::new).register(beanFactory);
    org.springframework.boot.actuate.autoconfigure.web.servlet.ContextBootstrapInitializer.registerServletManagementContextAutoConfiguration_servletWebChildContextFactory(beanFactory);
    BeanDefinitionRegistrar.of("managementServletContext", ManagementServletContext.class).withFactoryMethod(ServletManagementContextAutoConfiguration.class, "managementServletContext", WebEndpointProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(ServletManagementContextAutoConfiguration.class).managementServletContext(attributes.get(0)))).register(beanFactory);
    org.springframework.boot.actuate.autoconfigure.health.ContextBootstrapInitializer.registerHealthEndpointConfiguration(beanFactory);
    org.springframework.boot.actuate.autoconfigure.health.ContextBootstrapInitializer.registerHealthEndpointConfiguration_healthStatusAggregator(beanFactory);
    org.springframework.boot.actuate.autoconfigure.health.ContextBootstrapInitializer.registerHealthEndpointConfiguration_healthHttpCodeStatusMapper(beanFactory);
    org.springframework.boot.actuate.autoconfigure.health.ContextBootstrapInitializer.registerHealthEndpointConfiguration_healthEndpointGroups(beanFactory);
    org.springframework.boot.actuate.autoconfigure.health.ContextBootstrapInitializer.registerHealthEndpointConfiguration_healthContributorRegistry(beanFactory);
    org.springframework.boot.actuate.autoconfigure.health.ContextBootstrapInitializer.registerHealthEndpointConfiguration_healthEndpoint(beanFactory);
    org.springframework.boot.actuate.autoconfigure.health.ContextBootstrapInitializer.registerHealthEndpointConfiguration_healthEndpointGroupsBeanPostProcessor(beanFactory);
    org.springframework.boot.actuate.autoconfigure.health.ContextBootstrapInitializer.registerHealthEndpointWebExtensionConfiguration_MvcAdditionalHealthEndpointPathsConfiguration(beanFactory);
    org.springframework.boot.actuate.autoconfigure.health.ContextBootstrapInitializer.registerMvcAdditionalHealthEndpointPathsConfiguration_healthEndpointWebMvcHandlerMapping(beanFactory);
    org.springframework.boot.actuate.autoconfigure.health.ContextBootstrapInitializer.registerHealthEndpointWebExtensionConfiguration(beanFactory);
    org.springframework.boot.actuate.autoconfigure.health.ContextBootstrapInitializer.registerHealthEndpointWebExtensionConfiguration_healthEndpointWebExtension(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration", HealthEndpointAutoConfiguration.class)
        .instanceSupplier(HealthEndpointAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("management.endpoint.health-org.springframework.boot.actuate.autoconfigure.health.HealthEndpointProperties", HealthEndpointProperties.class)
        .instanceSupplier(HealthEndpointProperties::new).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration", ProjectInfoAutoConfiguration.class).withConstructor(ProjectInfoProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> new ProjectInfoAutoConfiguration(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("spring.info-org.springframework.boot.autoconfigure.info.ProjectInfoProperties", ProjectInfoProperties.class)
        .instanceSupplier(ProjectInfoProperties::new).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.info.InfoContributorAutoConfiguration", InfoContributorAutoConfiguration.class)
        .instanceSupplier(InfoContributorAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("management.info-org.springframework.boot.actuate.autoconfigure.info.InfoContributorProperties", InfoContributorProperties.class)
        .instanceSupplier(InfoContributorProperties::new).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration", InfoEndpointAutoConfiguration.class)
        .instanceSupplier(InfoEndpointAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("infoEndpoint", InfoEndpoint.class).withFactoryMethod(InfoEndpointAutoConfiguration.class, "infoEndpoint", ObjectProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(InfoEndpointAutoConfiguration.class).infoEndpoint(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration", EndpointAutoConfiguration.class)
        .instanceSupplier(EndpointAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("endpointOperationParameterMapper", ParameterValueMapper.class).withFactoryMethod(EndpointAutoConfiguration.class, "endpointOperationParameterMapper", ObjectProvider.class, ObjectProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(EndpointAutoConfiguration.class).endpointOperationParameterMapper(attributes.get(0), attributes.get(1)))).register(beanFactory);
    BeanDefinitionRegistrar.of("endpointCachingOperationInvokerAdvisor", CachingOperationInvokerAdvisor.class).withFactoryMethod(EndpointAutoConfiguration.class, "endpointCachingOperationInvokerAdvisor", Environment.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(EndpointAutoConfiguration.class).endpointCachingOperationInvokerAdvisor(attributes.get(0)))).register(beanFactory);
    org.springframework.boot.actuate.autoconfigure.endpoint.web.ContextBootstrapInitializer.registerWebEndpointAutoConfiguration_WebEndpointServletConfiguration(beanFactory);
    org.springframework.boot.actuate.autoconfigure.endpoint.web.ContextBootstrapInitializer.registerWebEndpointServletConfiguration_servletEndpointDiscoverer(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration", WebEndpointAutoConfiguration.class).withConstructor(ApplicationContext.class, WebEndpointProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> new WebEndpointAutoConfiguration(attributes.get(0), attributes.get(1)))).register(beanFactory);
    BeanDefinitionRegistrar.of("webEndpointPathMapper", PathMapper.class).withFactoryMethod(WebEndpointAutoConfiguration.class, "webEndpointPathMapper")
        .instanceSupplier(() -> beanFactory.getBean(WebEndpointAutoConfiguration.class).webEndpointPathMapper()).register(beanFactory);
    BeanDefinitionRegistrar.of("endpointMediaTypes", EndpointMediaTypes.class).withFactoryMethod(WebEndpointAutoConfiguration.class, "endpointMediaTypes")
        .instanceSupplier(() -> beanFactory.getBean(WebEndpointAutoConfiguration.class).endpointMediaTypes()).register(beanFactory);
    BeanDefinitionRegistrar.of("webEndpointDiscoverer", WebEndpointDiscoverer.class).withFactoryMethod(WebEndpointAutoConfiguration.class, "webEndpointDiscoverer", ParameterValueMapper.class, EndpointMediaTypes.class, ObjectProvider.class, ObjectProvider.class, ObjectProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(WebEndpointAutoConfiguration.class).webEndpointDiscoverer(attributes.get(0), attributes.get(1), attributes.get(2), attributes.get(3), attributes.get(4)))).register(beanFactory);
    BeanDefinitionRegistrar.of("controllerEndpointDiscoverer", ControllerEndpointDiscoverer.class).withFactoryMethod(WebEndpointAutoConfiguration.class, "controllerEndpointDiscoverer", ObjectProvider.class, ObjectProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(WebEndpointAutoConfiguration.class).controllerEndpointDiscoverer(attributes.get(0), attributes.get(1)))).register(beanFactory);
    BeanDefinitionRegistrar.of("pathMappedEndpoints", PathMappedEndpoints.class).withFactoryMethod(WebEndpointAutoConfiguration.class, "pathMappedEndpoints", Collection.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(WebEndpointAutoConfiguration.class).pathMappedEndpoints(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("webExposeExcludePropertyEndpointFilter", ResolvableType.forClassWithGenerics(IncludeExcludeEndpointFilter.class, ExposableWebEndpoint.class)).withFactoryMethod(WebEndpointAutoConfiguration.class, "webExposeExcludePropertyEndpointFilter")
        .instanceSupplier(() -> beanFactory.getBean(WebEndpointAutoConfiguration.class).webExposeExcludePropertyEndpointFilter()).register(beanFactory);
    BeanDefinitionRegistrar.of("controllerExposeExcludePropertyEndpointFilter", ResolvableType.forClassWithGenerics(IncludeExcludeEndpointFilter.class, ExposableControllerEndpoint.class)).withFactoryMethod(WebEndpointAutoConfiguration.class, "controllerExposeExcludePropertyEndpointFilter")
        .instanceSupplier(() -> beanFactory.getBean(WebEndpointAutoConfiguration.class).controllerExposeExcludePropertyEndpointFilter()).register(beanFactory);
    BeanDefinitionRegistrar.of("management.endpoints.web-org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties", WebEndpointProperties.class)
        .instanceSupplier(WebEndpointProperties::new).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthContributorAutoConfiguration", DiskSpaceHealthContributorAutoConfiguration.class)
        .instanceSupplier(DiskSpaceHealthContributorAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("diskSpaceHealthIndicator", DiskSpaceHealthIndicator.class).withFactoryMethod(DiskSpaceHealthContributorAutoConfiguration.class, "diskSpaceHealthIndicator", DiskSpaceHealthIndicatorProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(DiskSpaceHealthContributorAutoConfiguration.class).diskSpaceHealthIndicator(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("management.health.diskspace-org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthIndicatorProperties", DiskSpaceHealthIndicatorProperties.class)
        .instanceSupplier(DiskSpaceHealthIndicatorProperties::new).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration", HealthContributorAutoConfiguration.class)
        .instanceSupplier(HealthContributorAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("pingHealthContributor", PingHealthIndicator.class).withFactoryMethod(HealthContributorAutoConfiguration.class, "pingHealthContributor")
        .instanceSupplier(() -> beanFactory.getBean(HealthContributorAutoConfiguration.class).pingHealthContributor()).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration", MetricsAutoConfiguration.class)
        .instanceSupplier(MetricsAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("micrometerClock", Clock.class).withFactoryMethod(MetricsAutoConfiguration.class, "micrometerClock")
        .instanceSupplier(() -> beanFactory.getBean(MetricsAutoConfiguration.class).micrometerClock()).register(beanFactory);
    org.springframework.boot.actuate.autoconfigure.metrics.ContextBootstrapInitializer.registerMetricsAutoConfiguration_meterRegistryPostProcessor(beanFactory);
    BeanDefinitionRegistrar.of("propertiesMeterFilter", PropertiesMeterFilter.class).withFactoryMethod(MetricsAutoConfiguration.class, "propertiesMeterFilter", MetricsProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(MetricsAutoConfiguration.class).propertiesMeterFilter(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("management.metrics-org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties", MetricsProperties.class)
        .instanceSupplier(MetricsProperties::new).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration$PrometheusScrapeEndpointConfiguration", PrometheusMetricsExportAutoConfiguration.PrometheusScrapeEndpointConfiguration.class)
        .instanceSupplier(PrometheusMetricsExportAutoConfiguration.PrometheusScrapeEndpointConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("prometheusEndpoint", PrometheusScrapeEndpoint.class).withFactoryMethod(PrometheusMetricsExportAutoConfiguration.PrometheusScrapeEndpointConfiguration.class, "prometheusEndpoint", CollectorRegistry.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(PrometheusMetricsExportAutoConfiguration.PrometheusScrapeEndpointConfiguration.class).prometheusEndpoint(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration", PrometheusMetricsExportAutoConfiguration.class)
        .instanceSupplier(PrometheusMetricsExportAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("prometheusConfig", PrometheusConfig.class).withFactoryMethod(PrometheusMetricsExportAutoConfiguration.class, "prometheusConfig", PrometheusProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(PrometheusMetricsExportAutoConfiguration.class).prometheusConfig(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("prometheusMeterRegistry", PrometheusMeterRegistry.class).withFactoryMethod(PrometheusMetricsExportAutoConfiguration.class, "prometheusMeterRegistry", PrometheusConfig.class, CollectorRegistry.class, Clock.class, ObjectProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(PrometheusMetricsExportAutoConfiguration.class).prometheusMeterRegistry(attributes.get(0), attributes.get(1), attributes.get(2), attributes.get(3)))).register(beanFactory);
    BeanDefinitionRegistrar.of("collectorRegistry", CollectorRegistry.class).withFactoryMethod(PrometheusMetricsExportAutoConfiguration.class, "collectorRegistry")
        .instanceSupplier(() -> beanFactory.getBean(PrometheusMetricsExportAutoConfiguration.class).collectorRegistry()).register(beanFactory);
    BeanDefinitionRegistrar.of("management.metrics.export.prometheus-org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusProperties", PrometheusProperties.class)
        .instanceSupplier(PrometheusProperties::new).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration", CompositeMeterRegistryAutoConfiguration.class)
        .instanceSupplier(CompositeMeterRegistryAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.metrics.JvmMetricsAutoConfiguration", JvmMetricsAutoConfiguration.class)
        .instanceSupplier(JvmMetricsAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("jvmGcMetrics", JvmGcMetrics.class).withFactoryMethod(JvmMetricsAutoConfiguration.class, "jvmGcMetrics")
        .instanceSupplier(() -> beanFactory.getBean(JvmMetricsAutoConfiguration.class).jvmGcMetrics()).register(beanFactory);
    BeanDefinitionRegistrar.of("jvmHeapPressureMetrics", JvmHeapPressureMetrics.class).withFactoryMethod(JvmMetricsAutoConfiguration.class, "jvmHeapPressureMetrics")
        .instanceSupplier(() -> beanFactory.getBean(JvmMetricsAutoConfiguration.class).jvmHeapPressureMetrics()).register(beanFactory);
    BeanDefinitionRegistrar.of("jvmMemoryMetrics", JvmMemoryMetrics.class).withFactoryMethod(JvmMetricsAutoConfiguration.class, "jvmMemoryMetrics")
        .instanceSupplier(() -> beanFactory.getBean(JvmMetricsAutoConfiguration.class).jvmMemoryMetrics()).register(beanFactory);
    BeanDefinitionRegistrar.of("jvmThreadMetrics", JvmThreadMetrics.class).withFactoryMethod(JvmMetricsAutoConfiguration.class, "jvmThreadMetrics")
        .instanceSupplier(() -> beanFactory.getBean(JvmMetricsAutoConfiguration.class).jvmThreadMetrics()).register(beanFactory);
    BeanDefinitionRegistrar.of("classLoaderMetrics", ClassLoaderMetrics.class).withFactoryMethod(JvmMetricsAutoConfiguration.class, "classLoaderMetrics")
        .instanceSupplier(() -> beanFactory.getBean(JvmMetricsAutoConfiguration.class).classLoaderMetrics()).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.metrics.LogbackMetricsAutoConfiguration", LogbackMetricsAutoConfiguration.class)
        .instanceSupplier(LogbackMetricsAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("logbackMetrics", LogbackMetrics.class).withFactoryMethod(LogbackMetricsAutoConfiguration.class, "logbackMetrics")
        .instanceSupplier(() -> beanFactory.getBean(LogbackMetricsAutoConfiguration.class).logbackMetrics()).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.metrics.MetricsEndpointAutoConfiguration", MetricsEndpointAutoConfiguration.class)
        .instanceSupplier(MetricsEndpointAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("metricsEndpoint", MetricsEndpoint.class).withFactoryMethod(MetricsEndpointAutoConfiguration.class, "metricsEndpoint", MeterRegistry.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(MetricsEndpointAutoConfiguration.class).metricsEndpoint(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration", SystemMetricsAutoConfiguration.class)
        .instanceSupplier(SystemMetricsAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("uptimeMetrics", UptimeMetrics.class).withFactoryMethod(SystemMetricsAutoConfiguration.class, "uptimeMetrics")
        .instanceSupplier(() -> beanFactory.getBean(SystemMetricsAutoConfiguration.class).uptimeMetrics()).register(beanFactory);
    BeanDefinitionRegistrar.of("processorMetrics", ProcessorMetrics.class).withFactoryMethod(SystemMetricsAutoConfiguration.class, "processorMetrics")
        .instanceSupplier(() -> beanFactory.getBean(SystemMetricsAutoConfiguration.class).processorMetrics()).register(beanFactory);
    BeanDefinitionRegistrar.of("fileDescriptorMetrics", FileDescriptorMetrics.class).withFactoryMethod(SystemMetricsAutoConfiguration.class, "fileDescriptorMetrics")
        .instanceSupplier(() -> beanFactory.getBean(SystemMetricsAutoConfiguration.class).fileDescriptorMetrics()).register(beanFactory);
    BeanDefinitionRegistrar.of("diskSpaceMetrics", DiskSpaceMetricsBinder.class).withFactoryMethod(SystemMetricsAutoConfiguration.class, "diskSpaceMetrics", MetricsProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(SystemMetricsAutoConfiguration.class).diskSpaceMetrics(attributes.get(0)))).register(beanFactory);
    org.springframework.boot.actuate.autoconfigure.metrics.integration.ContextBootstrapInitializer.registerIntegrationMetricsAutoConfiguration(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.metrics.startup.StartupTimeMetricsListenerAutoConfiguration", StartupTimeMetricsListenerAutoConfiguration.class)
        .instanceSupplier(StartupTimeMetricsListenerAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("startupTimeMetrics", StartupTimeMetricsListener.class).withFactoryMethod(StartupTimeMetricsListenerAutoConfiguration.class, "startupTimeMetrics", MeterRegistry.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(StartupTimeMetricsListenerAutoConfiguration.class).startupTimeMetrics(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration", TaskSchedulingAutoConfiguration.class)
        .instanceSupplier(TaskSchedulingAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("scheduledBeanLazyInitializationExcludeFilter", LazyInitializationExcludeFilter.class).withFactoryMethod(TaskSchedulingAutoConfiguration.class, "scheduledBeanLazyInitializationExcludeFilter")
        .instanceSupplier(() -> TaskSchedulingAutoConfiguration.scheduledBeanLazyInitializationExcludeFilter()).register(beanFactory);
    BeanDefinitionRegistrar.of("taskSchedulerBuilder", TaskSchedulerBuilder.class).withFactoryMethod(TaskSchedulingAutoConfiguration.class, "taskSchedulerBuilder", TaskSchedulingProperties.class, ObjectProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(TaskSchedulingAutoConfiguration.class).taskSchedulerBuilder(attributes.get(0), attributes.get(1)))).register(beanFactory);
    BeanDefinitionRegistrar.of("spring.task.scheduling-org.springframework.boot.autoconfigure.task.TaskSchedulingProperties", TaskSchedulingProperties.class)
        .instanceSupplier(TaskSchedulingProperties::new).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.metrics.task.TaskExecutorMetricsAutoConfiguration", TaskExecutorMetricsAutoConfiguration.class)
        .instanceSupplier((instanceContext) -> {
          TaskExecutorMetricsAutoConfiguration bean = new TaskExecutorMetricsAutoConfiguration();
          instanceContext.method("bindTaskExecutorsToRegistry", Map.class, MeterRegistry.class)
              .invoke(beanFactory, (attributes) -> bean.bindTaskExecutorsToRegistry(attributes.get(0), attributes.get(1)));
          return bean;
        }).register(beanFactory);
    org.springframework.boot.autoconfigure.http.ContextBootstrapInitializer.registerHttpMessageConvertersAutoConfiguration_StringHttpMessageConverterConfiguration(beanFactory);
    org.springframework.boot.autoconfigure.http.ContextBootstrapInitializer.registerStringHttpMessageConverterConfiguration_stringHttpMessageConverter(beanFactory);
    org.springframework.boot.autoconfigure.http.ContextBootstrapInitializer.registerJacksonHttpMessageConvertersConfiguration_MappingJackson2HttpMessageConverterConfiguration(beanFactory);
    org.springframework.boot.autoconfigure.http.ContextBootstrapInitializer.registerMappingJackson2HttpMessageConverterConfiguration_mappingJackson2HttpMessageConverter(beanFactory);
    org.springframework.boot.autoconfigure.http.ContextBootstrapInitializer.registerJacksonHttpMessageConvertersConfiguration(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration", HttpMessageConvertersAutoConfiguration.class)
        .instanceSupplier(HttpMessageConvertersAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("messageConverters", HttpMessageConverters.class).withFactoryMethod(HttpMessageConvertersAutoConfiguration.class, "messageConverters", ObjectProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(HttpMessageConvertersAutoConfiguration.class).messageConverters(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration", RestTemplateAutoConfiguration.class)
        .instanceSupplier(RestTemplateAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("restTemplateBuilderConfigurer", RestTemplateBuilderConfigurer.class).withFactoryMethod(RestTemplateAutoConfiguration.class, "restTemplateBuilderConfigurer", ObjectProvider.class, ObjectProvider.class, ObjectProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(RestTemplateAutoConfiguration.class).restTemplateBuilderConfigurer(attributes.get(0), attributes.get(1), attributes.get(2)))).customize((bd) -> bd.setLazyInit(true)).register(beanFactory);
    BeanDefinitionRegistrar.of("restTemplateBuilder", RestTemplateBuilder.class).withFactoryMethod(RestTemplateAutoConfiguration.class, "restTemplateBuilder", RestTemplateBuilderConfigurer.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(RestTemplateAutoConfiguration.class).restTemplateBuilder(attributes.get(0)))).customize((bd) -> bd.setLazyInit(true)).register(beanFactory);
    org.springframework.boot.actuate.autoconfigure.metrics.web.client.ContextBootstrapInitializer.registerRestTemplateMetricsConfiguration(beanFactory);
    org.springframework.boot.actuate.autoconfigure.metrics.web.client.ContextBootstrapInitializer.registerRestTemplateMetricsConfiguration_restTemplateExchangeTagsProvider(beanFactory);
    org.springframework.boot.actuate.autoconfigure.metrics.web.client.ContextBootstrapInitializer.registerRestTemplateMetricsConfiguration_metricsRestTemplateCustomizer(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.metrics.web.client.HttpClientMetricsAutoConfiguration", HttpClientMetricsAutoConfiguration.class)
        .instanceSupplier(HttpClientMetricsAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("metricsHttpClientUriTagFilter", MeterFilter.class).withFactoryMethod(HttpClientMetricsAutoConfiguration.class, "metricsHttpClientUriTagFilter", MetricsProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(HttpClientMetricsAutoConfiguration.class).metricsHttpClientUriTagFilter(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration", WebMvcMetricsAutoConfiguration.class).withConstructor(MetricsProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> new WebMvcMetricsAutoConfiguration(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("webMvcTagsProvider", DefaultWebMvcTagsProvider.class).withFactoryMethod(WebMvcMetricsAutoConfiguration.class, "webMvcTagsProvider", ObjectProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(WebMvcMetricsAutoConfiguration.class).webMvcTagsProvider(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("webMvcMetricsFilter", ResolvableType.forClassWithGenerics(FilterRegistrationBean.class, WebMvcMetricsFilter.class)).withFactoryMethod(WebMvcMetricsAutoConfiguration.class, "webMvcMetricsFilter", MeterRegistry.class, WebMvcTagsProvider.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(WebMvcMetricsAutoConfiguration.class).webMvcMetricsFilter(attributes.get(0), attributes.get(1)))).register(beanFactory);
    BeanDefinitionRegistrar.of("metricsHttpServerUriTagFilter", MeterFilter.class).withFactoryMethod(WebMvcMetricsAutoConfiguration.class, "metricsHttpServerUriTagFilter")
        .instanceSupplier(() -> beanFactory.getBean(WebMvcMetricsAutoConfiguration.class).metricsHttpServerUriTagFilter()).register(beanFactory);
    org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.ContextBootstrapInitializer.registerWebMvcMetricsAutoConfiguration_metricsWebMvcConfigurer(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.metrics.web.tomcat.TomcatMetricsAutoConfiguration", TomcatMetricsAutoConfiguration.class)
        .instanceSupplier(TomcatMetricsAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("tomcatMetricsBinder", TomcatMetricsBinder.class).withFactoryMethod(TomcatMetricsAutoConfiguration.class, "tomcatMetricsBinder", MeterRegistry.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(TomcatMetricsAutoConfiguration.class).tomcatMetricsBinder(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.aop.AopAutoConfiguration", AopAutoConfiguration.class)
        .instanceSupplier(AopAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration", ConfigurationPropertiesAutoConfiguration.class)
        .instanceSupplier(ConfigurationPropertiesAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.context.LifecycleAutoConfiguration", LifecycleAutoConfiguration.class)
        .instanceSupplier(LifecycleAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("lifecycleProcessor", DefaultLifecycleProcessor.class).withFactoryMethod(LifecycleAutoConfiguration.class, "defaultLifecycleProcessor", LifecycleProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(LifecycleAutoConfiguration.class).defaultLifecycleProcessor(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("spring.lifecycle-org.springframework.boot.autoconfigure.context.LifecycleProperties", LifecycleProperties.class)
        .instanceSupplier(LifecycleProperties::new).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration", SqlInitializationAutoConfiguration.class)
        .instanceSupplier(SqlInitializationAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("spring.sql.init-org.springframework.boot.autoconfigure.sql.init.SqlInitializationProperties", SqlInitializationProperties.class)
        .instanceSupplier(SqlInitializationProperties::new).register(beanFactory);
    org.springframework.boot.sql.init.dependency.ContextBootstrapInitializer.registerDatabaseInitializationDependencyConfigurer_DependsOnDatabaseInitializationPostProcessor(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration$TomcatWebServerFactoryCustomizerConfiguration", EmbeddedWebServerFactoryCustomizerAutoConfiguration.TomcatWebServerFactoryCustomizerConfiguration.class)
        .instanceSupplier(EmbeddedWebServerFactoryCustomizerAutoConfiguration.TomcatWebServerFactoryCustomizerConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("tomcatWebServerFactoryCustomizer", TomcatWebServerFactoryCustomizer.class).withFactoryMethod(EmbeddedWebServerFactoryCustomizerAutoConfiguration.TomcatWebServerFactoryCustomizerConfiguration.class, "tomcatWebServerFactoryCustomizer", Environment.class, ServerProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(EmbeddedWebServerFactoryCustomizerAutoConfiguration.TomcatWebServerFactoryCustomizerConfiguration.class).tomcatWebServerFactoryCustomizer(attributes.get(0), attributes.get(1)))).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration", EmbeddedWebServerFactoryCustomizerAutoConfiguration.class)
        .instanceSupplier(EmbeddedWebServerFactoryCustomizerAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.web.servlet.HttpEncodingAutoConfiguration", HttpEncodingAutoConfiguration.class).withConstructor(ServerProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> new HttpEncodingAutoConfiguration(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("characterEncodingFilter", CharacterEncodingFilter.class).withFactoryMethod(HttpEncodingAutoConfiguration.class, "characterEncodingFilter")
        .instanceSupplier(() -> beanFactory.getBean(HttpEncodingAutoConfiguration.class).characterEncodingFilter()).register(beanFactory);
    org.springframework.boot.autoconfigure.web.servlet.ContextBootstrapInitializer.registerHttpEncodingAutoConfiguration_localeCharsetMappingsCustomizer(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration", MultipartAutoConfiguration.class).withConstructor(MultipartProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> new MultipartAutoConfiguration(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("multipartConfigElement", MultipartConfigElement.class).withFactoryMethod(MultipartAutoConfiguration.class, "multipartConfigElement")
        .instanceSupplier(() -> beanFactory.getBean(MultipartAutoConfiguration.class).multipartConfigElement()).register(beanFactory);
    BeanDefinitionRegistrar.of("multipartResolver", StandardServletMultipartResolver.class).withFactoryMethod(MultipartAutoConfiguration.class, "multipartResolver")
        .instanceSupplier(() -> beanFactory.getBean(MultipartAutoConfiguration.class).multipartResolver()).register(beanFactory);
    BeanDefinitionRegistrar.of("spring.servlet.multipart-org.springframework.boot.autoconfigure.web.servlet.MultipartProperties", MultipartProperties.class)
        .instanceSupplier(MultipartProperties::new).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.endpoint.web.ServletEndpointManagementContextConfiguration$WebMvcServletEndpointManagementContextConfiguration", ServletEndpointManagementContextConfiguration.WebMvcServletEndpointManagementContextConfiguration.class)
        .instanceSupplier(ServletEndpointManagementContextConfiguration.WebMvcServletEndpointManagementContextConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("servletEndpointRegistrar", ServletEndpointRegistrar.class).withFactoryMethod(ServletEndpointManagementContextConfiguration.WebMvcServletEndpointManagementContextConfiguration.class, "servletEndpointRegistrar", WebEndpointProperties.class, ServletEndpointsSupplier.class, DispatcherServletPath.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(ServletEndpointManagementContextConfiguration.WebMvcServletEndpointManagementContextConfiguration.class).servletEndpointRegistrar(attributes.get(0), attributes.get(1), attributes.get(2)))).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.endpoint.web.ServletEndpointManagementContextConfiguration", ServletEndpointManagementContextConfiguration.class)
        .instanceSupplier(ServletEndpointManagementContextConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("servletExposeExcludePropertyEndpointFilter", ResolvableType.forClassWithGenerics(IncludeExcludeEndpointFilter.class, ExposableServletEndpoint.class)).withFactoryMethod(ServletEndpointManagementContextConfiguration.class, "servletExposeExcludePropertyEndpointFilter", WebEndpointProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(ServletEndpointManagementContextConfiguration.class).servletExposeExcludePropertyEndpointFilter(attributes.get(0)))).register(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.endpoint.web.servlet.WebMvcEndpointManagementContextConfiguration", WebMvcEndpointManagementContextConfiguration.class)
        .instanceSupplier(WebMvcEndpointManagementContextConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("webEndpointServletHandlerMapping", WebMvcEndpointHandlerMapping.class).withFactoryMethod(WebMvcEndpointManagementContextConfiguration.class, "webEndpointServletHandlerMapping", WebEndpointsSupplier.class, ServletEndpointsSupplier.class, ControllerEndpointsSupplier.class, EndpointMediaTypes.class, CorsEndpointProperties.class, WebEndpointProperties.class, Environment.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(WebMvcEndpointManagementContextConfiguration.class).webEndpointServletHandlerMapping(attributes.get(0), attributes.get(1), attributes.get(2), attributes.get(3), attributes.get(4), attributes.get(5), attributes.get(6)))).register(beanFactory);
    BeanDefinitionRegistrar.of("controllerEndpointHandlerMapping", ControllerEndpointHandlerMapping.class).withFactoryMethod(WebMvcEndpointManagementContextConfiguration.class, "controllerEndpointHandlerMapping", ControllerEndpointsSupplier.class, CorsEndpointProperties.class, WebEndpointProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(WebMvcEndpointManagementContextConfiguration.class).controllerEndpointHandlerMapping(attributes.get(0), attributes.get(1), attributes.get(2)))).register(beanFactory);
    BeanDefinitionRegistrar.of("management.endpoints.web.cors-org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties", CorsEndpointProperties.class)
        .instanceSupplier(CorsEndpointProperties::new).register(beanFactory);
    org.springframework.boot.actuate.autoconfigure.web.server.ContextBootstrapInitializer.registerSameManagementContextConfiguration_EnableSameManagementContextConfiguration(beanFactory);
    org.springframework.boot.actuate.autoconfigure.web.server.ContextBootstrapInitializer.registerManagementContextAutoConfiguration_SameManagementContextConfiguration(beanFactory);
    BeanDefinitionRegistrar.of("org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration", ManagementContextAutoConfiguration.class)
        .instanceSupplier(ManagementContextAutoConfiguration::new).register(beanFactory);
    BeanDefinitionRegistrar.of("management.server-org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties", ManagementServerProperties.class)
        .instanceSupplier(ManagementServerProperties::new).register(beanFactory);
  }
}
