package com.danver.messengerserver.configs;

import com.danver.messengerserver.filters.ChatsAuthorizationFilter;
import com.danver.messengerserver.filters.JwtTokenFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {
    /*

    Be careful when you declare your filter as a Spring bean, either by annotating it with @Component or by declaring it
    as a bean in your configuration, because Spring Boot will automatically register it with the embedded container.
    That may cause the filter to be invoked twice, once by the container and once by Spring Security and in a different order.
    If you still want to declare your filter as a Spring bean to take advantage of dependency injection for example,
    and avoid the duplicate invocation, you can tell Spring Boot to not register it with the container by declaring
    a FilterRegistrationBean bean and setting its enabled property to false
     */
    @Bean
    public FilterRegistrationBean<JwtTokenFilter> jwtTokenFilterRegistration(JwtTokenFilter filter) {
        FilterRegistrationBean<JwtTokenFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ChatsAuthorizationFilter> chatsAuthorizationFilterRegistration(ChatsAuthorizationFilter filter) {
        FilterRegistrationBean<ChatsAuthorizationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
