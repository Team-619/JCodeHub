package org.jbnu.jdevops.jcodeportallogin.repo

import org.jbnu.jdevops.jcodeportallogin.entity.Jcode
import org.jbnu.jdevops.jcodeportallogin.entity.User
import org.jbnu.jdevops.jcodeportallogin.entity.Course
import org.jbnu.jdevops.jcodeportallogin.entity.UserCourses
import org.springframework.data.jpa.repository.JpaRepository

interface JCodeRepository : JpaRepository<Jcode, Long> {
    fun findByUser(user: User): List<Jcode>
    fun findByUserAndCourse(user: User, course: Course): Jcode?
    fun findByUserCourse(userCourse: UserCourses): Jcode?
}