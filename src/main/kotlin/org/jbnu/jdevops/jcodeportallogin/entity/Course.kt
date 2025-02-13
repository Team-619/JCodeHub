package org.jbnu.jdevops.jcodeportallogin.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "course")
data class Course(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val courseId: Long = 0,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false) // 강의 개설 년도
    val year: Int,

    @Column(nullable = false) // 강의 개설 학기
    val term: Int,

    @Column(nullable = false) // 강의 담당 교수
    val professor: String,

    @Column(nullable = false) // 강의 분반
    val clss: Int,

    @Column(nullable = false) // 강의 비번 (mutable - 수정 가능)
    var courseKey: String,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)