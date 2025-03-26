package org.jbnu.jdevops.jcodeportallogin.security

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.jbnu.jdevops.jcodeportallogin.service.token.KeycloakAuthService
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean
import org.springframework.web.server.ResponseStatusException

@Component
class KeycloakAuthFilter(
    private val keycloakAuthService: KeycloakAuthService
) : GenericFilterBean() {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val token = httpRequest.getHeader("Authorization")?.removePrefix("Bearer ")

        if (token != null) {
            if (!keycloakAuthService.validateToken(token)) {
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token")
            }

            val claims = keycloakAuthService.extractClaims(token)

            // 이메일 추출 (email > preferred_username > sub)
            val email = claims["email"] as? String
                ?: claims["preferred_username"] as? String
                ?: claims["sub"] as? String
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing email in token")

            // 역할(Role) 추출 (Keycloak `realm_access.roles`에서 가져옴)
            val roles = keycloakAuthService.extractRoles(token)
                .map { SimpleGrantedAuthority("ROLE_$it") }

            // Security Context에 사용자 인증 정보 저장
            val auth = UsernamePasswordAuthenticationToken(email, null, roles)
            SecurityContextHolder.getContext().authentication = auth
        }

        // 다음 필터로 이동
        chain.doFilter(request, response)
    }
}
