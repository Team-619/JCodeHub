package org.jbnu.jdevops.jcodeportallogin.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    // ResponseStatusException 예외 처리 (사용자 정의 상태 코드 적용)
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<Map<String, Any>> {
        val responseBody = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "status" to ex.statusCode.value(),
            "error" to (ex.reason ?: "Unknown error"),
            "path" to ""  // 요청 경로 추가 가능
        )
        return ResponseEntity(responseBody, ex.statusCode)
    }

    // 일반적인 IllegalArgumentException 처리
    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): Map<String, Any> {
        return mapOf(
            "timestamp" to System.currentTimeMillis(),
            "status" to HttpStatus.BAD_REQUEST.value(),
            "error" to (ex.message ?: "Invalid argument"),
            "path" to ""
        )
    }

    // 기타 예상치 못한 예외 처리
    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<Map<String, Any>> {
        val responseBody = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "status" to HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "error" to "An unexpected error occurred",
            "details" to (ex.message ?: "No details available")
        )
        return ResponseEntity(responseBody, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}