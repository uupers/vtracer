package none.nintha.vtraceapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import springfox.documentation.swagger2.annotations.EnableSwagger2

@EnableAsync
@EnableSwagger2
@EnableScheduling
@SpringBootApplication
class VtraceApiApplication

fun main(args: Array<String>) {
    runApplication<VtraceApiApplication>(*args)
}
