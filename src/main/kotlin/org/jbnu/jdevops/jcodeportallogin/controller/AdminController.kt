package org.jbnu.jdevops.jcodeportallogin.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.jbnu.jdevops.jcodeportallogin.dto.RegisterUserDto
import org.jbnu.jdevops.jcodeportallogin.dto.UserDto
import org.jbnu.jdevops.jcodeportallogin.entity.RoleType
import org.jbnu.jdevops.jcodeportallogin.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Admin API", description = "관리자 전용 사용자 관리 API")
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')") // ADMIN 권한이 없는 사용자는 모두 접근 불가
class AdminController(private val userService: UserService) {
    // 모든 유저 조회
    @Operation(
        summary = "모든 유저 조회",
        description = "모든 사용자 정보를 조회합니다."
    )
    @GetMapping
    fun getAllUsers(): List<UserDto> {
        return userService.getAllUsers()
    }

    // 학생 계정 추가
    @Operation(summary = "학생 계정 추가", description = "관리자가 학생 계정을 추가합니다.")
    @PostMapping("/student")
    fun registerStudent(@RequestBody registerUserDto: RegisterUserDto): ResponseEntity<String> {
        val studentDto = registerUserDto.copy(role = RoleType.STUDENT)
        return userService.register(studentDto)
    }

    // 조교 계정 추가
    @Operation(summary = "조교 계정 추가", description = "관리자가 조교 계정을 추가합니다.")
    @PostMapping("/assistant")
    fun registerAssistant(@RequestBody registerUserDto: RegisterUserDto): ResponseEntity<String> {
        val assistantDto = registerUserDto.copy(role = RoleType.ASSISTANCE)
        return userService.register(assistantDto)
    }

    // 교수 계정 추가
    @Operation(summary = "교수 계정 추가", description = "관리자가 교수 계정을 추가합니다.")
    @PostMapping("/professor")
    fun registerProfessor(@RequestBody registerUserDto: RegisterUserDto): ResponseEntity<String> {
        val professorDto = registerUserDto.copy(role = RoleType.PROFESSOR)
        return userService.register(professorDto)
    }

    // 특정 유저 정보 조회
    @Operation(summary = "특정 유저 정보 조회", description = "관리자가 특정 사용자의 정보를 조회합니다.")
    @GetMapping("/{userId}")
    fun getUserById(@PathVariable userId: Long): ResponseEntity<UserDto> {
        return userService.getUserById(userId)?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.notFound().build()
    }

    // 특정 유저 삭제
    @Operation(summary = "특정 유저 삭제", description = "관리자가 특정 사용자를 삭제합니다.")
    @DeleteMapping("/{userId}")
    fun deleteUser(@PathVariable userId: Long): ResponseEntity<Unit> {
        return try {
            userService.deleteUser(userId)
            ResponseEntity.ok().build()
        } catch (e: ResponseStatusException) {
            ResponseEntity.notFound().build()
        }
    }
}