package org.jbnu.jdevops.jcodeportallogin.service.token

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import java.io.ByteArrayInputStream
import java.security.PublicKey
import java.util.*
import java.util.concurrent.TimeUnit
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

@Service
class KeycloakJwksService(
    private val redisTemplate: StringRedisTemplate
) {
    @Value("\${keycloak.jwks-url}")
    private lateinit var jwksUrl: String

    private val restTemplate = RestTemplate()
    private val objectMapper = ObjectMapper()

    // Keycloak JWKS 공개키 가져오기
    fun getPublicKey(): PublicKey {
        // Redis에서 JWKS 캐시 확인
        val cachedJwks = redisTemplate.opsForValue().get("keycloak:jwks")
        if (cachedJwks != null) {
            return parsePublicKey(cachedJwks)
        }

        // Keycloak에서 JWKS 가져오기
        val response = restTemplate.getForEntity(jwksUrl, String::class.java)
        if (response.statusCode != HttpStatus.OK) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch JWKS from Keycloak")
        }

        val jwksJson = response.body ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Empty JWKS response")
        val publicKeyPem = extractPublicKey(jwksJson)

        // Redis에 JWKS 저장 (캐싱, 유효기간 1시간)
        redisTemplate.opsForValue().set("keycloak:jwks", publicKeyPem, 1, TimeUnit.HOURS)

        return parsePublicKey(publicKeyPem)
    }

    // JWKS에서 RSA 공개키 추출 (X.509 인증서 변환 방식)
    private fun extractPublicKey(jwksJson: String): String {
        val jsonNode: JsonNode = objectMapper.readTree(jwksJson)
        val keys = jsonNode["keys"]

        // "use"가 "sig"인 키를 찾음
        val key = keys.find { it["use"]?.asText() == "sig" }
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No signing key (use: sig) found in JWKS")

        return key["x5c"].first().asText()  // Base64 인코딩된 인증서 반환
    }

    // PEM 형식의 공개키를 Java PublicKey로 변환 (X.509 인증서 처리)
    private fun parsePublicKey(publicKeyPem: String): PublicKey {
        return try {
            // x5c는 인증서이므로 X.509 Certificate로 변환해야 함
            val decodedCert = Base64.getDecoder().decode(publicKeyPem)
            val certFactory = CertificateFactory.getInstance("X.509")
            val cert = certFactory.generateCertificate(ByteArrayInputStream(decodedCert)) as X509Certificate
            cert.publicKey  // X.509 인증서에서 공개키 추출
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse JWKS public key: ${e.message}")
        }
    }
}
