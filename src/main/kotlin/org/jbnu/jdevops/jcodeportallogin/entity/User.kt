package org.jbnu.jdevops.jcodeportallogin.entity

import jakarta.persistence.*
import org.jbnu.jdevops.jcodeportallogin.dto.UserDto
import java.time.LocalDateTime

@Entity
@Table(name = "user")
data class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val userId: Long = 0,  // JPA가 자동으로 설정

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = true)
    val studentNum: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: RoleType,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    // UserCourses와의 일대다 관계 설정
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val courses: List<UserCourses> = mutableListOf()
)

fun User.toDto(): UserDto {
    return UserDto(
        email = this.email,
        role = this.role,
        studentNum = this.studentNum
    )
}
