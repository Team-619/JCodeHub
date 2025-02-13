package org.jbnu.jdevops.jcodeportallogin.service

import org.jbnu.jdevops.jcodeportallogin.dto.JCodeDto
import org.jbnu.jdevops.jcodeportallogin.entity.Jcode
import org.jbnu.jdevops.jcodeportallogin.repo.JCodeRepository
import org.jbnu.jdevops.jcodeportallogin.repo.CourseRepository
import org.jbnu.jdevops.jcodeportallogin.repo.UserCoursesRepository
import org.jbnu.jdevops.jcodeportallogin.repo.UserRepository
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus

@Service
class JCodeService(
    private val jCodeRepository: JCodeRepository,
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository,
    private val userCoursesRepository: UserCoursesRepository,
    private val redisService: RedisService
) {
    // JCode 생성 (관리자 전용)
    fun createJCode(courseId: Long, jcodeUrl: String, userId: Long): JCodeDto {
        val course = courseRepository.findById(courseId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found") }

        val user = userRepository.findByUserId(userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        val userCourse = userCoursesRepository.findByUserAndCourse(user, course)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "UserCourse not found")

        val jCode = jCodeRepository.save(
            Jcode(
                jcodeUrl = jcodeUrl,
                course = course,
                user = user,
                userCourse = userCourse
            )
        )

        // UserCourses 테이블의 jcode 값을 true로 변경 (JCode 생성 시)
        val updatedUserCourse = userCourse.copy(jcode = true)
        userCoursesRepository.save(updatedUserCourse)

        // DB 저장 후 Redis 동기화
        val storedJcode = jCodeRepository.findByUserAndCourse(user, course)
        if (storedJcode != null) {
            redisService.storeUserCourse(user.email, course.code, course.clss, jcodeUrl)
        }

        return JCodeDto(
            jcodeId = jCode.jcodeId,
            jcodeUrl = jCode.jcodeUrl,
            courseName = jCode.course.name
        )
    }

    // JCode 삭제 (관리자 전용)
    fun deleteJCode(userId: Long, courseId: Long) {
        val user = userRepository.findByUserId(userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        val course = courseRepository.findById(courseId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found") }

        val jCode = jCodeRepository.findByUserAndCourse(user, course)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "JCode not found for the specified user and course")

        jCodeRepository.delete(jCode)

        // UserCourses 테이블의 jcode 값을 false로 변경 (JCode 삭제 시)
        val userCourse = jCode.userCourse
        val updatedUserCourse = userCourse.copy(jcode = false)
        userCoursesRepository.save(updatedUserCourse)

        // Redis에서도 해당 정보를 삭제
        redisService.deleteUserCourse(user.email, course.code, course.clss)
    }
}
