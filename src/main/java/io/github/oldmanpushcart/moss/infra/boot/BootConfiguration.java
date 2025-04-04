package io.github.oldmanpushcart.moss.infra.boot;

import jakarta.validation.constraints.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.atomic.AtomicInteger;

@SpringBootConfiguration
public class BootConfiguration {

    private static final AtomicInteger bootBeanTotalRef = new AtomicInteger(0);
    private static final AtomicInteger bootBeanLoadedRef = new AtomicInteger(0);

    @Bean
    public static BeanDefinitionRegistryPostProcessor bootstrapBeanCounter() {
        return registry ->
                bootBeanTotalRef.set(registry.getBeanDefinitionCount());
    }

    @Bean
    public static BeanPostProcessor bootstrapSplashUpdater(ApplicationEventPublisher publisher) {
        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
                final var total = bootBeanTotalRef.get();
                final var loaded = bootBeanLoadedRef.incrementAndGet();
                final var event = new BootEvent(this, total, loaded, beanName);
                publisher.publishEvent(event);
                return bean;
            }

        };
    }

}
