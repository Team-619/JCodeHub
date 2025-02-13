package org.jbnu.jdevops.jcodeportallogin.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.jbnu.jdevops.jcodeportallogin.dto.JCodeDto
import org.jbnu.jdevops.jcodeportallogin.dto.JCodeRequestDto
import org.jbnu.jdevops.jcodeportallogin.service.JCodeService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@Tag(name = "JCode Admin API", description = "관리자 전용 JCode 관리 API")
@RestController
@RequestMapping("/api/users/{userId}/courses/{courseId}/jcodes")
@PreAuthorize("hasRole('ADMIN')") // ADMIN 권한이 없는 사용자는 모두 접근 불가
class JCodeController(
    private val jCodeService: JCodeService
) {
    // JCode 추가 (관리자 전용)
    @Operation(
        summary = "JCode 추가",
        description = "관리자가 특정 강의에 대해 JCode를 생성 및 할당합니다."
    )
    @PostMapping
    fun createJCode(
        @PathVariable userId: Long,
        @PathVariable courseId: Long,
        @RequestBody request: JCodeRequestDto
    ): ResponseEntity<JCodeDto> {
        return ResponseEntity.ok(jCodeService.createJCode(courseId, request.jcodeUrl, userId))
    }

    // JCode 삭제 (관리자 전용)
    @Operation(
        summary = "JCode 삭제",
        description = "관리자가 특정 강의에 대해 해당 사용자의 JCode를 삭제합니다."
    )
    @DeleteMapping
    fun deleteJCode(
        @PathVariable userId: Long,
        @PathVariable courseId: Long
    ): ResponseEntity<String> {
        jCodeService.deleteJCode(userId, courseId)
        return ResponseEntity.ok("JCode deleted successfully")
    }
}
