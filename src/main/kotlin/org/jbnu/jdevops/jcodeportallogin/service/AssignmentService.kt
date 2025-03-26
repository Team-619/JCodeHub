package org.jbnu.jdevops.jcodeportallogin.service

import org.jbnu.jdevops.jcodeportallogin.dto.AssignmentDto
import org.jbnu.jdevops.jcodeportallogin.entity.Assignment
import org.jbnu.jdevops.jcodeportallogin.repo.AssignmentRepository
import org.jbnu.jdevops.jcodeportallogin.repo.CourseRepository
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

@Service
class AssignmentService(
    private val assignmentRepository: AssignmentRepository,
    private val courseRepository: CourseRepository
) {

    // 과제 추가
    fun createAssignment(courseId: Long, assignmentDto: AssignmentDto): AssignmentDto {
        val course = courseRepository.findById(courseId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found") }

        val assignment = assignmentRepository.save(Assignment(name = assignmentDto.assignmentName, description = assignmentDto.assignmentDescription, course = course))
        return AssignmentDto(
            assignmentId = assignment.id,
            assignmentName = assignment.name,
            assignmentDescription = assignment.description,
            createdAt = assignment.createdAt.toString(),
            updatedAt = assignment.updatedAt.toString())
    }

    // 과제 수정 (업데이트)
    fun updateAssignment(courseId: Long, assignmentId: Long, assignmentDto: AssignmentDto): AssignmentDto {
        val course = courseRepository.findById(courseId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found") }

        val assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found") }

        // 수정된 데이터 저장
        val updatedAssignment = assignment.copy(
            name = assignmentDto.assignmentName,
            description = assignmentDto.assignmentDescription,
            updatedAt = LocalDateTime.now()
        )

        assignmentRepository.save(updatedAssignment)

        return AssignmentDto(
            assignmentId = assignment.id,
            assignmentName = updatedAssignment.name,
            assignmentDescription = updatedAssignment.description,
            createdAt = updatedAssignment.createdAt.toString(),
            updatedAt = updatedAssignment.updatedAt.toString()
        )
    }

    // 과제 삭제
    fun deleteAssignment(courseId: Long, assignmentId: Long) {
        if (!assignmentRepository.existsById(assignmentId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found")
        }
        assignmentRepository.deleteById(assignmentId)
    }
}
