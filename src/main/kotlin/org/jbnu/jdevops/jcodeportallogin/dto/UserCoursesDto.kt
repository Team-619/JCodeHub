package org.jbnu.jdevops.jcodeportallogin.dto

// 유저별 강의 정보 DTO
data class UserCoursesDto(
    val courseId: Long,
    val courseName: String,
    val courseCode: String,
    val courseProfessor: String,
    val courseYear: Int,
    val courseTerm: Int,
    val courseClss: Int
)