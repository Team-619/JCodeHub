package org.jbnu.jdevops.jcodeportallogin.dto

import org.jbnu.jdevops.jcodeportallogin.entity.RoleType

data class UserDto(
    val email: String,
    val role: RoleType = RoleType.STUDENT,
    val studentNum: Int?  // nullable
)
