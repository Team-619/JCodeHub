package org.jbnu.jdevops.jcodeportallogin

import org.jbnu.jdevops.jcodeportallogin.config.OpenApiConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@Import(OpenApiConfig::class)  // Swagger Custom 설정 (title, authentication ...)

@SpringBootApplication
class JcodeportalLoginApplication

fun main(args: Array<String>) {
    runApplication<JcodeportalLoginApplication>(*args)
}
