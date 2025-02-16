package org.jbnu.jdevops.jcodeportallogin.security

import io.jsonwebtoken.Claims
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.jbnu.jdevops.jcodeportallogin.service.RedisService
import org.jbnu.jdevops.jcodeportallogin.service.token.JwtAuthService
import org.jbnu.jdevops.jcodeportallogin.util.JwtUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.io.IOException

@Component
class CustomLogoutSuccessHandler(
    private val redisService: RedisService,
    @Value("\${keycloak.logout.url}") private val keycloakLogoutUrl: String, // Keycloak 로그아웃 엔드포인트
    @Value("\${front.domain}") private val frontDomain: String, // 프론트엔드 도메인
    private val jwtUtil: JwtUtil,
    private val jwtAuthService: JwtAuthService
) : LogoutSuccessHandler {

    @Throws(IOException::class, ServletException::class)
    override fun onLogoutSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?
    ) {
        // 미리 SecurityContext에서 email을 추출 (로그아웃 전에 캡처)
        val emailFromAuth = authentication?.principal?.toString()

        // 1. 세션 무효화 및 SecurityContext 클리어
        request.session?.invalidate()
        SecurityContextHolder.clearContext()

        // 2. Authorization 헤더에서 Access Token 추출 ("Bearer {token}")
        val accessToken = jwtUtil.extractBearerToken(request)
        if (!accessToken.isNullOrEmpty()) {
            val jwtClaims: Claims = jwtAuthService.getClaims(accessToken)
            val ttlMillis = jwtClaims.expiration.time - System.currentTimeMillis()
            redisService.addToJwtBlacklist(accessToken, ttlMillis)
        }

        // 3. refresh token 쿠키 추출 (쿠키 이름 "refreshToken")
        val refreshToken = jwtUtil.extractCookieToken(request, "refreshToken")
        var emailFromRefresh: String? = null

        if (refreshToken != null) {
            // refresh token에서 claim(만료 시간, email) 추출 및 만료까지의 시간 계산
            val refreshClaims: Claims = jwtAuthService.getClaims(refreshToken)
            val refreshTtlMillis = refreshClaims.expiration.time - System.currentTimeMillis()
            emailFromRefresh = refreshClaims.subject ?:
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token missing subject")

            // refresh token 블랙리스트 저장 및 redis와 쿠키에서 폐기
            redisService.addToJwtBlacklist(refreshToken, refreshTtlMillis)
            redisService.deleteRefreshToken(emailFromRefresh)
            response.addCookie(jwtUtil.createExpiredCookie("refreshToken"))
        } else {
            // refresh token 쿠키가 없으면 로그를 남기고 계속 진행
            println("Refresh token cookie not found; proceeding with logout")
        }

        // 4. 최종 email 결정: refresh token에서 가져온 이메일 우선, 없으면 SecurityContext에서 가져온 email 사용
        val email = emailFromRefresh ?: emailFromAuth

        // 5. Redis에서 id_token 조회 및 삭제 (email이 있을 경우에만 처리)
        var idToken: String? = null
        if (email != null) {
            idToken = redisService.getIdToken(email)
            if (!idToken.isNullOrEmpty()) {
                redisService.deleteIdToken(email)
            }
        }

        // 6. Keycloak 로그아웃 URL 구성: id_token_hint 포함 (있다면)
        val redirectUri = "$frontDomain/"
        val keycloakLogoutRedirectUrl = if (!idToken.isNullOrEmpty()) {
            "$keycloakLogoutUrl?id_token_hint=$idToken&post_logout_redirect_uri=$redirectUri"
        } else {
            "$keycloakLogoutUrl?post_logout_redirect_uri=$redirectUri"
        }

        // 7. Keycloak 로그아웃 엔드포인트로 리다이렉트
        response.sendRedirect(keycloakLogoutRedirectUrl)
    }
}
