package org.jbnu.jdevops.jcodeportallogin.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class CourseKeyUtil(
    @Value("\${course.key.charset}")
    private val charset: String
) {
    fun generateCourseEnrollmentCode(courseCode: String, courseClss: Int, length: Int = 10): String {

        val random = SecureRandom()

        // length 길이만큼의 랜덤 문자열 생성
        val randomPart = (1..length)
            .map { charset[random.nextInt(charset.length)] }
            .joinToString("")

        // courseCode, courseClss와 랜덤 문자열을 하이픈(-)으로 조합하여 반환
        return "$courseCode-$courseClss-$randomPart"
    }
}