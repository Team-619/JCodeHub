package org.jbnu.jdevops.jcodeportallogin.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "assignment")
data class Assignment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val assignmentId: Long = 0,

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    val course: Course,

    @Column(nullable = false)
    val name: String,

    @Column
    val description: String?,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
