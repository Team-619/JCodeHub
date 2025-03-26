package org.jbnu.jdevops.jcodeportallogin.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import io.jsonwebtoken.Claims
import org.jbnu.jdevops.jcodeportallogin.entity.RoleType
import org.jbnu.jdevops.jcodeportallogin.service.token.JwtAuthService
import org.jbnu.jdevops.jcodeportallogin.util.JwtUtil
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtAuthService: JwtAuthService,
    private val jwtUtil: JwtUtil,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // "Authorization" 헤더에서 "Bearer {token}" 형식으로 Access Token 추출
        val accessToken = jwtUtil.extractBearerToken(request)

        if (!accessToken.isNullOrEmpty() && jwtAuthService.validateToken(accessToken)) {
            // access token이 유효하면, SecurityContext에 설정
            val claims: Claims = jwtAuthService.getClaims(accessToken)
            val email = claims.subject
            // 여기서는 JWT 클레임에서 role을 문자열로 저장했다고 가정
            val role = claims["role"] as? RoleType ?: RoleType.STUDENT

            val authorities = listOf(SimpleGrantedAuthority("ROLE_${role.toString()}"))
            val auth = UsernamePasswordAuthenticationToken(email, null, authorities)
            SecurityContextHolder.getContext().authentication = auth
        } else {
            // access token이 없거나 유효하지 않으면, 로그아웃 처리
            response.sendRedirect("/logout")
            return
        }

        filterChain.doFilter(request, response)
    }

    // access token 반환 api에선 필터 제외
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return request.requestURI == "/api/auth/token"
    }
}
