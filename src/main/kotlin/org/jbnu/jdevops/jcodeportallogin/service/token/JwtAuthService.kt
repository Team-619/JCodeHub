package org.jbnu.jdevops.jcodeportallogin.service.token

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.jbnu.jdevops.jcodeportallogin.entity.RoleType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
class JwtAuthService(
    @Value("\${jwt.secret}") private val secretKey: String,
    @Value("\${jwt.expire}") private val expireTime: Long,
    @Value("\${jwt.secret}") private val refreshSecretKey: String,
    @Value("\${jwt.expire}") private val refreshExpireTime: Long
) {

    fun createToken(email: String, role: RoleType): String {
        // 시크릿 키를 바이트 배열로 변환 후 HMAC SHA 키 생성
        val key = Keys.hmacShaKeyFor(secretKey.toByteArray())

        val claims = Jwts.claims().setSubject(email)
        claims["role"] = role

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expireTime))
            .signWith(key)
            .compact()
    }

    fun createRefreshToken(email: String, role: RoleType): String {
        val key = Keys.hmacShaKeyFor(refreshSecretKey.toByteArray())

        val claims = Jwts.claims().setSubject(email)
        claims["role"] = role

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + refreshExpireTime))
            .signWith(key)
            .compact()
    }

    // JWT에서 전체 클레임 추출
    fun getClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(secretKey.toByteArray())
            .build()
            .parseClaimsJws(token)
            .body
    }

    // 이메일 추출
    fun extractEmail(token: String): String {
        return getClaims(token).subject
    }

    // 토큰 검증
    fun validateToken(token: String): Boolean {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(secretKey.toByteArray())
                .build()
                .parseClaimsJws(token)

            !claims.body.expiration.before(Date())
        } catch (e: Exception) {
            false
        }
    }

    fun validateRefreshToken(token: String): Boolean {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(refreshSecretKey.toByteArray())
                .build()
                .parseClaimsJws(token)

            !claims.body.expiration.before(Date())
        } catch (e: Exception) {
            false
        }
    }
}