package org.jbnu.jdevops.jcodeportallogin.repo

import org.jbnu.jdevops.jcodeportallogin.entity.User
import org.jbnu.jdevops.jcodeportallogin.entity.UserCourses
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, String> {
    fun findByEmail(email: String): User?
    fun findByUserId(userId: Long): User?
}