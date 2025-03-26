package org.jbnu.jdevops.jcodeportallogin.dto

data class CourseDto(
    val courseId: Long?,
    val name: String,
    val code: String,
    val professor: String,
    val year: Int,
    val term: Int,
    val clss: Int,
    val courseKey: String? = "hidden"
)
