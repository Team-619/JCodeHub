package org.jbnu.jdevops.jcodeportallogin.service

import org.jbnu.jdevops.jcodeportallogin.dto.*
import org.jbnu.jdevops.jcodeportallogin.entity.Login
import org.jbnu.jdevops.jcodeportallogin.entity.User
import org.jbnu.jdevops.jcodeportallogin.entity.UserCourses
import org.jbnu.jdevops.jcodeportallogin.entity.toDto
import org.jbnu.jdevops.jcodeportallogin.repo.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class UserService(
    private val userRepository: UserRepository,
    private val loginRepository: LoginRepository,
    private val jcodeRepository: JCodeRepository,
    private val userCoursesRepository: UserCoursesRepository,
    private val assignmentRepository: AssignmentRepository,
    private val passwordEncoder: PasswordEncoder,
    private val courseRepository: CourseRepository,
    private val redisService: RedisService
) {
    @Transactional
    fun register(registerUserDto: RegisterUserDto): ResponseEntity<String> {
        // 이메일 중복 확인
        if (userRepository.findByEmail(registerUserDto.email) != null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already in use")
        }

        // 비밀번호 유효성 검사
        if (registerUserDto.password.length < 8) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long")
        }

        return try {
            // 비밀번호 해싱
            val hashedPassword = passwordEncoder.encode(registerUserDto.password)

            // 새 사용자 저장
            val user = userRepository.save(
                User(
                    email = registerUserDto.email,
                    role = registerUserDto.role,  // 기본적으로 학생 역할 부여
                    studentNum = registerUserDto.studentNum
                )
            )

            // 로그인 정보 저장
            loginRepository.save(Login(user = user, password = hashedPassword))

            ResponseEntity.ok("Signup successful")
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to register user")
        }
    }

    @Transactional(readOnly = true)
    fun getUserInfo(email: String): UserInfoDto {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("User not found with email: $email")

        return UserInfoDto(
            email = user.email,
            role = user.role,
            studentNum = user.studentNum
        )
    }

    @Transactional(readOnly = true)
    fun getUserCourses(email: String): List<UserCoursesDto> {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("User not found with email: $email")

        val userCourses = user.courses
        return userCourses.map {
            UserCoursesDto(
                courseName = it.course.name,
                courseCode = it.course.code
            )
        }
    }

    // 유저별 JCode 정보 조회
    fun getUserJcodes(email: String): List<JCodeDto> {
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email: $email")

        return jcodeRepository.findByUser(user).map {
            JCodeDto(
                jcodeId = it.jcodeId,
                courseName = it.course.name,
                jcodeUrl = it.jcodeUrl
            )
        }
    }

    // 유저별 참가 강의의 과제 및 JCode 정보 조회
    fun getUserCoursesWithDetails(email: String): List<UserCourseDetailsDto> {
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email: $email")

        return userCoursesRepository.findByUser(user).map {
            val assignments = assignmentRepository.findByCourse_CourseId(it.course.courseId)
            val jcode = jcodeRepository.findByUserAndCourse(user, it.course)

            UserCourseDetailsDto(
                courseName = it.course.name,
                courseCode = it.course.code,
                assignments = assignments.map { assignment ->
                    AssignmentDto(
                        assignmentId = assignment.assignmentId,
                        assignmentName = assignment.name,
                        assignmentDescription = assignment.description,
                        createdAt = assignment.createdAt.toString(),
                        updatedAt = assignment.updatedAt.toString()
                    )
                },
                jcodeUrl = jcode?.jcodeUrl
            )
        }
    }

    // 유저 강의 가입
    fun joinCourse(email: String, courseId: Long) {
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        val course = courseRepository.findById(courseId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found") }

        // 중복 가입 방지
        if (userCoursesRepository.existsByUserAndCourse(user, course)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "User already enrolled in this course")
        }

        // UserCourses 엔티티 저장
        val userCourse = UserCourses(
            user = user,
            course = course,
            jcode = false // 기본적으로 JCode 사용 여부 false
        )
        userCoursesRepository.save(userCourse)

        // DB 저장 후 Redis 데이터 검증 및 동기화
        val storedUserCourse = userCoursesRepository.findByUserAndCourseCode(user, course.code)
        if (storedUserCourse != null) {
            redisService.addUserToCourseList(course.code, email)
        }
    }

    // 유저 강의 탈퇴 (연관된 정보 삭제)
    fun leaveCourse(courseId: Long, email: String) {
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        val course = courseRepository.findById(courseId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found") }

        val userCourse = userCoursesRepository.findByUserAndCourse(user, course)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User is not enrolled in this course")

        // 해당 강의에서 사용된 JCode 삭제
        jcodeRepository.findByUserCourse(userCourse)?.let {
            jcodeRepository.delete(it)
        }

        // UserCourses에서 유저 삭제 (강의 탈퇴)
        userCoursesRepository.delete(userCourse)
    }

    @Transactional(readOnly = true)
    fun getAllUsers(): List<UserDto> {
        return userRepository.findAll().map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun getUserByEmail(email: String): UserDto? {
        return userRepository.findByEmail(email)?.toDto()
    }

    @Transactional
    fun deleteUser(email: String) {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("User not found with email: $email")
        userRepository.delete(user)
    }

}
