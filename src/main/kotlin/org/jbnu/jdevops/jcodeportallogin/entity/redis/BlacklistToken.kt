import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash

@RedisHash("blacklist")  // Redis에 "blacklist" 키로 저장
data class BlacklistToken(
    @Id
    val token: String
)
