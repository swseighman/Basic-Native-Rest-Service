package org.springframework.boot.test.autoconfigure.web.servlet;

import org.springframework.aot.beans.factory.BeanDefinitionRegistrar;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.web.context.WebApplicationContext;

public final class GreetingControllerTestsContextInitializer {
  public static void registerMockMvcAutoConfiguration(DefaultListableBeanFactory beanFactory) {
    BeanDefinitionRegistrar.of("org.springframework.boot.test.autoconfigure.web.servlet.MockMvcAutoConfiguration", MockMvcAutoConfiguration.class).withConstructor(WebApplicationContext.class, WebMvcProperties.class)
        .instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> new MockMvcAutoConfiguration(attributes.get(0), attributes.get(1)))).register(beanFactory);
  }
}
