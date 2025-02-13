package org.jbnu.jdevops.jcodeportallogin.config

import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSessionEvent
import jakarta.servlet.http.HttpSessionListener
import org.jbnu.jdevops.jcodeportallogin.security.KeycloakAuthFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration

@Configuration
class SecurityConfig {
    @Value("\${front.domain}")
    private lateinit var frontDomain: String

    @Value("\${nodejs.domain}")
    private lateinit var nodejsDomain: String

    @Bean
    fun securityFilterChain(http: HttpSecurity, keycloakAuthFilter: KeycloakAuthFilter): SecurityFilterChain {
        http
            .csrf { it.disable() }  // CSRF 보호 비활성화
            .cors { cors ->
                cors.configurationSource {
                    val configuration = CorsConfiguration()
                    configuration.allowedOrigins = listOf(frontDomain, nodejsDomain)
                    configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    configuration.allowedHeaders = listOf("*")
                    configuration.exposedHeaders = listOf("Authorization", "Set-Cookie")
                    configuration.allowCredentials = true
                    configuration
                }
            }
            .authorizeHttpRequests { authz ->
                authz
                    .requestMatchers("/api/auth/signup", "/api/auth/login/basic", "/api/auth/login/oidc/success").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/api/user/info", "/api/user/courses", "/api/user/**").permitAll()  // 임시
                    .requestMatchers("/api/user/**").hasAuthority("ADMIN")  // 임시
                    .requestMatchers("/api/user/student", "/api/user/assistant", "/api/user/professor").hasAuthority("ADMIN")
                    .requestMatchers("/api/**").permitAll()  // 임시
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().authenticated()  // 모든 요청에 대해 인증 요구
            }
            .sessionManagement { sessionManagement ->
                sessionManagement
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // API 요청은 JWT 인증 (세션 X)
            }
            // JWT 기반 인증 필터 추가 (Keycloak 검증)
            .addFilterBefore(keycloakAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}