package org.jbnu.jdevops.jcodeportallogin.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "user_courses")
data class UserCourses(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    val course: Course,

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    val jcode: Boolean,

    @OneToMany(mappedBy = "userCourse", cascade = [CascadeType.ALL], orphanRemoval = true)
    val jcodes: List<Jcode> = mutableListOf(),

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)