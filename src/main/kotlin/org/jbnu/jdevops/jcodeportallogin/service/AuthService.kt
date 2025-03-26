package org.jbnu.jdevops.jcodeportallogin.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.jbnu.jdevops.jcodeportallogin.dto.LoginUserDto

import org.jbnu.jdevops.jcodeportallogin.entity.RoleType
import org.jbnu.jdevops.jcodeportallogin.repo.LoginRepository
import org.jbnu.jdevops.jcodeportallogin.repo.UserRepository
import org.jbnu.jdevops.jcodeportallogin.service.token.JwtAuthService
import org.jbnu.jdevops.jcodeportallogin.util.JwtUtil
import org.jbnu.jdevops.jcodeportallogin.util.RefreshTokenUtil
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val loginRepository: LoginRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtAuthService: JwtAuthService,
    private val redisService: RedisService,
    private val jwtUtil: JwtUtil
) {

    fun basicLogin(loginUserDto: LoginUserDto): Map<String, String> {
        val user = userRepository.findByEmail(loginUserDto.email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        val login = loginRepository.findByUserId(user.id)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")

        if (!passwordEncoder.matches(loginUserDto.password, login.password)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect password")
        }

        // JWT 토큰 생성 (이메일 + 학교 정보)
        val jwt = jwtAuthService.createToken(user.email, RoleType.STUDENT)
        return mapOf("message" to "Login successful", "token" to jwt)
    }

    fun getAccessToken(request: HttpServletRequest): String {
        // 1. 쿠키에서 refresh token 추출
        val refreshToken = jwtUtil.extractCookieToken(request, "refreshToken")
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "No refresh token provided")

        // 2. refresh token의 클레임에서 사용자 이메일 추출
        val email = jwtAuthService.extractEmail(refreshToken)

        // 3. Refresh token 검증 (유효성, 블랙리스트, Redis 저장값 일치 여부)
        RefreshTokenUtil.validate(refreshToken, jwtAuthService, redisService, email)

        // 4. 세션 검증: HTTP 세션이 없으면 오류 처리
        val session = request.getSession(false)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "No active session")

        // 5. 사용자 조회 및 역할 추출
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")
        val role: RoleType = user.role

        // 6. 새로운 Access Token 생성
        return jwtAuthService.createToken(email, role)
    }

    // 검증: refresh token 유효성, 블랙리스트, Redis에 저장된 값 일치 여부 확인
    // 재발급: 새로운 access token과 refresh token 생성 후 Redis 및 쿠키/헤더 갱신 (RTR)
    fun refreshTokens(request: HttpServletRequest, response: HttpServletResponse): Map<String, String> {
        // 1. 쿠키에서 refresh token 추출
        val refreshToken = jwtUtil.extractCookieToken(request, "refreshToken")
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "No refresh token provided")

        // 2. refresh token의 클레임에서 사용자 정보 추출
        val email = jwtAuthService.extractEmail(refreshToken)

        // 3. refresh token 검증
        RefreshTokenUtil.validate(refreshToken, jwtAuthService, redisService, email)

        // 4. 사용자 조회
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")
        val role = user.role

        // 5. 새 access token 및 refresh token 생성 (RTR 적용)
        val newAccessToken = jwtAuthService.createToken(email, role)
        val newRefreshToken = jwtAuthService.createRefreshToken(email, role)

        // 6. 업데이트: Redis에 새로운 refresh token 저장 및 쿠키/헤더 갱신
        redisService.storeRefreshToken(email, newRefreshToken)

        // 7. Token 전달 (Access - header, Refresh - cookie)
        response.setHeader("Authorization", "Bearer $newAccessToken")
        response.addCookie(jwtUtil.createJwtCookie("refreshToken", newRefreshToken))

        return mapOf("message" to "Tokens refreshed")
    }
}
