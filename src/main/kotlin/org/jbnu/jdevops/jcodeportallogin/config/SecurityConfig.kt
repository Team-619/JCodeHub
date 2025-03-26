package org.jbnu.jdevops.jcodeportallogin.config

import org.jbnu.jdevops.jcodeportallogin.security.CustomAuthenticationSuccessHandler
import org.jbnu.jdevops.jcodeportallogin.security.CustomLogoutSuccessHandler
import org.jbnu.jdevops.jcodeportallogin.security.JwtAuthenticationFilter
import org.jbnu.jdevops.jcodeportallogin.security.KeycloakAuthFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.web.cors.CorsConfiguration

@Configuration
class SecurityConfig {
    @Value("\${front.domain}")
    private lateinit var frontDomain: String

    @Value("\${nodejs.domain}")
    private lateinit var nodejsDomain: String

    @Bean
    fun securityFilterChain(http: HttpSecurity, keycloakAuthFilter: KeycloakAuthFilter,
                            customAuthenticationSuccessHandler: CustomAuthenticationSuccessHandler,
                            customLogoutSuccessHandler: CustomLogoutSuccessHandler,
                            jwtAuthenticationFilter: JwtAuthenticationFilter
    ): SecurityFilterChain {
        http
//            // CSRF 보호 활성화하되, API 경로는 CSRF 검증에서 제외 (필요에 따라 조정)
//            .csrf { csrf ->
//                csrf.ignoringRequestMatchers("/api/**", "/oidc/login", "/logout")
//            }
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
                    .requestMatchers("/login", "/error", "/oauth2/**").permitAll()
                    .requestMatchers("/api/auth/signup", "/api/auth/login/basic", "/api/auth/login/oidc/success").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/api/user/info", "/api/user/courses", "/api/user/**").permitAll()  // 임시
                    .requestMatchers("/api/user/**").hasAuthority("ADMIN")  // 임시
                    .requestMatchers("/api/user/student", "/api/user/assistant", "/api/user/professor").hasAuthority("ADMIN")
                    .requestMatchers("/api/**").permitAll()  // 임시
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().authenticated()  // 모든 요청에 대해 인증 요구
            }
            .oauth2Login { oauth2 ->
                // 기본 성공 URL 대신 커스텀 성공 핸들러 사용
                oauth2.successHandler(customAuthenticationSuccessHandler)
            }
            .logout { logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/")
                    .logoutSuccessHandler(customLogoutSuccessHandler)
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
            }
            .sessionManagement { sessionManagement ->
                sessionManagement.sessionFixation { it.migrateSession() }  // 세션 고정 보호
                sessionManagement.maximumSessions(1)  // 동시 세션 1개로 제한
            }
            // JWT 기반 인증 필터 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}