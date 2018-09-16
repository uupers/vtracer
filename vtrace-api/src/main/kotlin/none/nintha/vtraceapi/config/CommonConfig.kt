package none.nintha.vtraceapi.config

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.ErrorPage
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CommonConfig : WebMvcConfigurer {
    // 修改@Value中的标识符$修改为@
    @Bean
    fun kotlinPropertyConfigurer() = PropertySourcesPlaceholderConfigurer().apply {
        setPlaceholderPrefix("@{")
        setIgnoreUnresolvablePlaceholders(true)
    }

    @Bean
    fun defaultPropertyConfigurer() = PropertySourcesPlaceholderConfigurer()

    override fun addInterceptors(registry: InterceptorRegistry) {
        // 多个拦截器组成一个拦截器链
        // addPathPatterns 用于添加拦截规则
        // excludePathPatterns 用户排除拦截
        registry.addInterceptor(VtracerInterceptor()).addPathPatterns("/**")
    }

    @Bean
    fun webServerFactory(): ConfigurableServletWebServerFactory {
        val factory = TomcatServletWebServerFactory();
        factory.addErrorPages(ErrorPage(HttpStatus.NOT_FOUND, "/"))
        factory.addErrorPages(ErrorPage(HttpStatus.UNAUTHORIZED, "/401"))
        return factory
    }

}