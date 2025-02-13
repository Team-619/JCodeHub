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
                courseId = it.course.courseId,
                courseName = it.course.name,
                courseCode = it.course.code,
                courseProfessor = it.course.professor,
                courseClss = it.course.clss,
                courseTerm = it.course.term,
                courseYear = it.course.year
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
                courseId = it.course.courseId,
                courseName = it.course.name,
                courseCode = it.course.code,
                courseProfessor = it.course.professor,
                courseClss = it.course.clss,
                courseTerm = it.course.term,
                courseYear = it.course.year,
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
    fun joinCourse(email: String, courseKey: String) {
        // 입력된 courseKey가 "code-clss-randomPart" 형식인지 확인하고, code와 clss 추출
        val parts = courseKey.split("-")
        if (parts.size < 3) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid course key format")
        }
        val courseCode = parts[0]
        val courseClss = parts[1].toIntOrNull() ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid class value")

        // courseCode와 courseClss를 기준으로 강의 목록 조회
        val courses = courseRepository.findByCodeAndClss(courseCode, courseClss)
        if (courses.isEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found")
        }

        // 입력된 평문 courseKey와 DB에 저장된 해시된 courseKey를 비교하여 일치하는 강의 선택
        val course = courses.firstOrNull { passwordEncoder.matches(courseKey, it.courseKey) }
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect course key")

        // 사용자 조회
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

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

        // DB 저장 후 Redis 데이터 동기화: 강의 코드와 강의 분반(clss)을 함께 사용
        val storedUserCourse = userCoursesRepository.findByUserAndCourseCode(user, course.code)
        if (storedUserCourse != null) {
            redisService.addUserToCourseList(course.code, course.clss, email)
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

        // Redis에서 해당 강의의 참여자 목록에서 해당 유저(email) 제거
        redisService.removeUserFromCourseList(course.code, course.clss, email)
    }

    @Transactional(readOnly = true)
    fun getAllUsers(): List<UserDto> {
        return userRepository.findAll().map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun getUserById(userId: Long): UserDto? {
        val user = userRepository.findByUserId(userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: $userId")
        return user.toDto()
    }

    @Transactional
    fun deleteUser(userId: Long) {
        val user = userRepository.findByUserId(userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: $userId")
        userRepository.delete(user)
    }

}
