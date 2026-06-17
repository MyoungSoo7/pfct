package lemuel.com.pfct.web

import lemuel.com.pfct.investment.application.FundingRoundNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    /** 잔여 한도 초과 등 도메인 규칙 위반 → 409 Conflict. */
    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleDomainRuleViolation(e: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse("CONFLICT", e.message))

    @ExceptionHandler(FundingRoundNotFoundException::class)
    fun handleNotFound(e: FundingRoundNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("NOT_FOUND", e.message))
}

data class ErrorResponse(val code: String, val message: String?)
