package org.jbnu.jdevops.jcodeportallogin.service.token

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Service
import java.security.PublicKey
import java.util.*

@Service
class KeycloakAuthService(
    private val keycloakJwksService: KeycloakJwksService
) {
    // -----------------------------------
    // JWT 서명 검증 (Redis에서 JWKS 가져옴)
    fun validateToken(token: String): Boolean {
        try {
            // Redis에서 JWKS 가져와 검증 시도
            val publicKey: PublicKey = keycloakJwksService.getPublicKey()
            if (verifyTokenWithKey(token, publicKey)) {
                return true
            }

            // 첫 번째 검증 실패 시 JWKS를 갱신하고 다시 시도
            println("JWT verification failed. Refreshing JWKS and retrying...")

            refreshJwks() // JWKS 갱신 (Keycloak에서 다시 가져옴)

            val newPublicKey: PublicKey = keycloakJwksService.getPublicKey()
            return verifyTokenWithKey(token, newPublicKey)
        } catch (e: Exception) {
            println("Token validation failed: ${e.message}")
            return false
        }
    }

    // JWKS를 갱신하는 함수 (Keycloak에서 다시 가져와 Redis에 저장)
    private fun refreshJwks() {
        try {
            keycloakJwksService.getPublicKey()  // Keycloak에서 새로운 JWKS 가져오기
            println("JWKS refreshed successfully.")
        } catch (e: Exception) {
            println("JWKS refresh failed: ${e.message}")
        }
    }

    // 토큰을 주어진 Public Key로 검증하는 함수
    private fun verifyTokenWithKey(token: String, publicKey: PublicKey): Boolean {
        return try {
            val parser = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()

            val claims = parser.parseClaimsJws(token).body

            // 만료 시간 확인
            val expiration = claims.expiration
            if (expiration.before(Date())) {
                println("Token expired: ${token}")
                return false
            }

            true
        } catch (e: Exception) {
            println(" Token verification failed with current JWKS: ${e.message}")
            false
        }
    }
    // ---------------------------------

    fun parseTokenClaims(token: String): Claims {
        val publicKey: PublicKey = keycloakJwksService.getPublicKey()
        return Jwts.parserBuilder()
            .setSigningKey(publicKey)
            .build()
            .parseClaimsJws(token)
            .body
    }

    // JWT에서 클레임 추출 (이메일, 역할 등)
    fun extractClaims(token: String): Map<String, Any> {
        val publicKey: PublicKey = keycloakJwksService.getPublicKey()
        return Jwts.parserBuilder()
            .setSigningKey(publicKey)
            .build()
            .parseClaimsJws(token)
            .body
    }

    fun extractRoles(token: String): List<String> {
        val claims = parseTokenClaims(token)

        // "resource_access" → "jcodehub" → "roles" 추출
        val resourceAccess = claims["resource_access"] as? Map<*, *> ?: return emptyList()
        val jcodehubAccess = resourceAccess["jcodehub"] as? Map<*, *> ?: return emptyList()
        val roles = jcodehubAccess["roles"] as? List<*> ?: return emptyList()

        return roles.mapNotNull { it?.toString() }
    }
}
