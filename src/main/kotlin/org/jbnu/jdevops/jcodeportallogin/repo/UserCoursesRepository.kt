package org.jbnu.jdevops.jcodeportallogin.repo

import org.jbnu.jdevops.jcodeportallogin.entity.Course
import org.jbnu.jdevops.jcodeportallogin.entity.User
import org.jbnu.jdevops.jcodeportallogin.entity.UserCourses
import org.springframework.data.jpa.repository.JpaRepository

interface UserCoursesRepository : JpaRepository<UserCourses, Int> {
    fun findByUser(user: User): List<UserCourses>
    fun findByCourse_CourseId(courseId: Long): List<UserCourses>
    fun findByUserAndCourse(user: User, course: Course): UserCourses?
    fun existsByUserAndCourse(user: User, course: Course): Boolean
    fun findByUserAndCourseCode(user: User, courseCode: String): UserCourses?
}