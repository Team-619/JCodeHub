package org.jbnu.jdevops.jcodeportallogin.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.jbnu.jdevops.jcodeportallogin.util.JwtUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Tag(name = "Redirect API", description = "JCode(Node.js 서버)로의 리다이렉션 관련 API")
@RestController
@RequestMapping("/api/redirect")
class RedirectController(private val jwtUtil: JwtUtil) {

    @Value("\${nodejs.url}")  // 환경 변수에서 Node.js URL 가져오기
    private lateinit var nodeJsUrl: String

    // Node.js 서버로 리다이렉션 (JCode)
    @Operation(
        summary = "JCode(Node.js 서버) 리다이렉션",
        description = "url 파라미터로 courseCode, clss(courseClss) 및 st(id)을 Node.js 서버에 보내어 검증 및 jcode로 리다이렉트 합니다."
    )
    @GetMapping
    fun redirectToNode(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @RequestParam courseCode: String,
        @RequestParam clss: Int,
        @RequestParam st: String
    ): ResponseEntity<Void> {

        val token = request.getHeader("Authorization")?.removePrefix("Bearer ")
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization Token")

        // 각 파라미터 값을 UTF-8로 URL 인코딩
        val encodedCourseCode = URLEncoder.encode(courseCode, StandardCharsets.UTF_8.toString()).replace("+", "%2B")
        val encodedSt = URLEncoder.encode(st, StandardCharsets.UTF_8.toString()).replace("+", "%2B")
        println(encodedCourseCode)

        // Node.js 서버 URL에 인코딩된 파라미터를 포함하여 구성
        val finalNodeJsUrl = "$nodeJsUrl?courseCode=$encodedCourseCode&clss=$clss&st=$encodedSt"
        println(finalNodeJsUrl)

        // Keycloak Access Token을 HTTP-Only Secure 쿠키로 설정
        response.addCookie(jwtUtil.createJwtCookie("jwt", token))

        // 클라이언트를 Node.js 서버로 리다이렉트
        response.sendRedirect(finalNodeJsUrl)
        return ResponseEntity.status(HttpStatus.FOUND).build()
    }
}