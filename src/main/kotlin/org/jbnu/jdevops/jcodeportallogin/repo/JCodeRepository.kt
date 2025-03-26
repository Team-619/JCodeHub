package org.jbnu.jdevops.jcodeportallogin.repo

import org.jbnu.jdevops.jcodeportallogin.entity.Jcode
import org.jbnu.jdevops.jcodeportallogin.entity.User
import org.jbnu.jdevops.jcodeportallogin.entity.Course
import org.jbnu.jdevops.jcodeportallogin.entity.UserCourses
import org.springframework.data.jpa.repository.JpaRepository

interface JCodeRepository : JpaRepository<Jcode, Long> {
    fun findByUserId(userId: Long): List<Jcode>
    fun findByUserIdAndCourseId(userId: Long, courseId: Long): Jcode?
    fun findByUserCourse(userCourse: UserCourses): Jcode?
}