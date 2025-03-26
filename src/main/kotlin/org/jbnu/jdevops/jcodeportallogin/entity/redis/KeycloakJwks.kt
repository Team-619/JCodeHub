import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash

@RedisHash("keycloak:jwks")  // Redis에 "keycloak:jwks" 키로 저장
data class KeycloakJwks(
    @Id
    val key: String
)
