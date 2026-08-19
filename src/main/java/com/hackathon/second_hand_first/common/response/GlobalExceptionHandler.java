package com.hackathon.second_hand_first.common.response;

import com.hackathon.second_hand_first.auth.exception.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
        String message = exception.getMessage();
        if (exception instanceof MethodArgumentNotValidException validationException) {
            message = validationException.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getDefaultMessage())
                    .orElse("요청 값을 확인해 주세요.");
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }
}
