package org.jbnu.jdevops.jcodeportallogin.security

import jakarta.servlet.ServletException
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class CustomLogoutSuccessHandler(
//    private val jwtBlacklistService: JwtBlacklistService, // JWT 블랙리스트에 토큰 저장
    @Value("\${keycloak.logout.url}") private val keycloakLogoutUrl: String, // Keycloak 로그아웃 엔드포인트
    @Value("\${front.domain}") private val frontDomain: String // 프론트엔드 도메인
) : LogoutSuccessHandler {

    @Throws(IOException::class, ServletException::class)
    override fun onLogoutSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?
    ) {
        // 1. 세션 무효화
        request.session?.invalidate()

        // 2. SecurityContext 클리어
        SecurityContextHolder.clearContext()

//        // 3. JWT 쿠키 추출 (쿠키 이름이 "JWT_TOKEN"이라고 가정)
//        val jwtCookie = request.cookies?.firstOrNull { it.name == "jwt" }
//        if (jwtCookie != null) {
//            // JWT 블랙리스트에 토큰 저장 (이미 발급된 JWT를 재사용 못하도록)
////            jwtBlacklistService.add(jwtCookie.value)
//
//            // JWT 쿠키 제거 (maxAge=0, path 설정)
//            val expiredCookie = Cookie(jwtCookie.name, null).apply {
//                path = "/"
//                maxAge = 0
//                isHttpOnly = true
//                secure = true
//                domain = "jbnu.ac.kr"
//            }
//            response.addCookie(expiredCookie)
//        }

        // 4. Keycloak 로그아웃 처리
        // Keycloak 로그아웃 URL에 logout 후 리다이렉트할 URL을 추가합니다.
        val redirectUri = "$frontDomain/"
        val keycloakLogoutRedirectUrl = "$keycloakLogoutUrl?redirect_uri=$redirectUri"

        // 5. Keycloak 로그아웃 엔드포인트로 리다이렉트 (이 과정에서 Keycloak 세션 만료 처리)
        response.sendRedirect(keycloakLogoutRedirectUrl)
    }
}
