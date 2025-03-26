package org.jbnu.jdevops.jcodeportallogin.dto

import org.jbnu.jdevops.jcodeportallogin.entity.RoleType

data class RegisterUserDto(
    val email: String,
    val studentNum: Int,
    val role: RoleType = RoleType.STUDENT, // 기본적으로 학생 역할 부여
    val password: String
)