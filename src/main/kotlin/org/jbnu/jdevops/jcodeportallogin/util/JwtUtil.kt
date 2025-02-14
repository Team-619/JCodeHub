package org.jbnu.jdevops.jcodeportallogin.util

import jakarta.servlet.http.Cookie
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JwtUtil (
    @Value("\${jwt.cookie.domain}")
    private val cookieDomain: String
) {

    fun createJwtCookie(name: String, jwt: String): Cookie {
        return Cookie(name, jwt).apply {
            isHttpOnly = true  // XSS 공격 방지
            secure = true      // HTTPS에서만 전송
            path = "/"         // 전체 도메인에서 사용 가능
            maxAge = 3600      // 1시간 동안 유효
            setAttribute("SameSite", "None")  // 리다이렉션 시 쿠키 유지
            domain = cookieDomain // 쿠키를 모든 하위 도메인에서 사용 가능하도록 설정
        }
    }
}