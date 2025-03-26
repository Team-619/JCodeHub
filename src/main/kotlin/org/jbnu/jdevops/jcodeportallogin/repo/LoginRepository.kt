package org.jbnu.jdevops.jcodeportallogin.repo

import org.jbnu.jdevops.jcodeportallogin.entity.Login
import org.springframework.data.jpa.repository.JpaRepository

interface LoginRepository : JpaRepository<Login, Long> {
    fun findByUserId(userId: Long): Login?
}