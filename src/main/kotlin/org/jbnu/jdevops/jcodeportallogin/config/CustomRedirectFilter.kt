package org.jbnu.jdevops.jcodeportallogin.config

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
class CustomRedirectFilter : OncePerRequestFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authentication: Authentication? = SecurityContextHolder.getContext().authentication

        // 특정 경로는 리다이렉트하지 않음
//        if (request.requestURI != "/login/success" && authentication != null && authentication.isAuthenticated) {
//            // 세션이 유효하고 사용자가 인증된 경우 리다이렉트
//            response.sendRedirect("https://jcode.jbnu.ac.kr/jcode/?folder=/config/workspace")
//            return
//        }

        filterChain.doFilter(request, response)
    }
}