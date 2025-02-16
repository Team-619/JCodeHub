package org.jbnu.jdevops.jcodeportallogin.repo

import org.jbnu.jdevops.jcodeportallogin.entity.UserCourses
import org.springframework.data.jpa.repository.JpaRepository

interface UserCoursesRepository : JpaRepository<UserCourses, Int> {
    fun findByUserId(userId: Long): List<UserCourses>
    fun findByCourseId(courseId: Long): List<UserCourses>
    fun findByUserIdAndCourseId(userId: Long, courseId: Long): UserCourses?
    fun existsByUserIdAndCourseId(userId: Long, courseId: Long): Boolean
    fun findByUserIdAndCourseCode(userId: Long, courseCode: String): UserCourses?
}