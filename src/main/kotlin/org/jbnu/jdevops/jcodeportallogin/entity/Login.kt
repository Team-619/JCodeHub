package org.jbnu.jdevops.jcodeportallogin.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "login")
data class Login(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val loginId: Long = 0,

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    val password: String,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)