package org.jbnu.jdevops.jcodeportallogin.dto

data class AssignmentDto(
    val assignmentId: Long?,
    val assignmentName: String,
    val assignmentDescription: String? = null,
    val createdAt: String?,
    val updatedAt: String?
)