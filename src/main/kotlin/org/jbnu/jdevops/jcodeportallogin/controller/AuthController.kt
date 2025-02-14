package org.jbnu.jdevops.jcodeportallogin.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.jbnu.jdevops.jcodeportallogin.dto.LoginUserDto
import org.jbnu.jdevops.jcodeportallogin.dto.RegisterUserDto
import org.jbnu.jdevops.jcodeportallogin.service.*
import org.jbnu.jdevops.jcodeportallogin.util.JwtUtil
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "Auth API", description = "인증 및 회원가입 관련 API")
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val userService: UserService,
    private val jwtUtil: JwtUtil
) {
    // 일반 회원가입
    @Operation(summary = "일반 회원가입", description = "사용자가 일반 회원가입을 요청합니다.")
    @PostMapping("/signup")
    fun register(@RequestBody registerUserDto: RegisterUserDto): ResponseEntity<String> {
        return userService.register(registerUserDto)
    }

    // 일반 로그인 ( ADMIN, PROFESSOR, ASSISTANT )
    @Operation(
        summary = "일반 로그인",
        description = "ADMIN, PROFESSOR, ASSISTANT 역할의 사용자가 일반 로그인을 수행합니다. 인증 후 JWT 토큰이 발급됩니다."
    )
    @PostMapping("/login/basic")
    fun basicLogin(@RequestBody loginUserDto: LoginUserDto, response: HttpServletResponse): ResponseEntity<Map<String, String>> {
        val result = authService.basicLogin(loginUserDto)
        response.addCookie(jwtUtil.createJwtCookie("jwt_auth", result["token"] ?: ""))
        return ResponseEntity.ok(result)
    }

    // KeyCloak 로그인 ( STUDENT )
    @Operation(
        summary = "KeyCloak 로그인 (STUDENT)",
        description = "STUDENT 역할의 사용자가 KeyCloak을 통해 로그인을 수행합니다. 인증된 사용자의 이메일과 역할 정보를 기반으로 로그인 처리를 진행합니다."
    )
    @GetMapping("/login/oidc/success")
    fun loginOidcSuccess(
        authentication: Authentication,
        response: HttpServletResponse
    ): ResponseEntity<Map<String, String>> {

        val email = authentication.principal as? String
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing email in authentication")

        val roles = authentication.authorities.map { it.authority }

        // 기존 서비스 로직 유지
        val result = authService.oidcLogin(email, roles)
        return ResponseEntity.ok(result)
    }
}