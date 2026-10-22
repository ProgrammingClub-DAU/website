package com.cpclub.backend.common.config;

import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Keeps scheduled jobs eager under {@code spring.main.lazy-initialization}.
 *
 * <p>The prod profile turns lazy initialization on to save memory. Spring registers
 * a {@code @Scheduled} method only when its bean is created, so under lazy
 * initialization a scheduled service that nothing else had asked for yet simply
 * never ran: after every restart -- and Render's free tier restarts the instance
 * after each idle spell -- the six-hourly sync, the daily totals snapshot, the
 * score computation and the reminders all waited for some unrelated request to
 * happen to create their bean first.</p>
 */
@Configuration
public class SchedulingConfig {

    @Bean
    static LazyInitializationExcludeFilter scheduledBeansAreEager() {
        return (beanName, beanDefinition, beanType) -> hasScheduledMethods(beanType);
    }

    static boolean hasScheduledMethods(Class<?> type) {
        if (type == null) return false;
        return !MethodIntrospector.selectMethods(type,
                (MethodIntrospector.MetadataLookup<Set<Scheduled>>) (Method method) -> {
                    Set<Scheduled> found = AnnotatedElementUtils.getMergedRepeatableAnnotations(
                            method, Scheduled.class, Schedules.class);
                    return found.isEmpty() ? null : found;
                }).isEmpty();
    }
}
