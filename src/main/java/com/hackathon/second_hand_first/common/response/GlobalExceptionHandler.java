package com.hackathon.second_hand_first.common.response;

import com.hackathon.second_hand_first.auth.exception.UnauthorizedException;
import com.hackathon.second_hand_first.location.exception.KakaoLocalException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    @ExceptionHandler(KakaoLocalException.class)
    public ResponseEntity<ApiResponse<Void>> handleKakaoLocalException(
            KakaoLocalException exception
    ) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(
            HttpMessageNotReadableException exception
    ) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("요청 본문 형식을 확인해 주세요."));
    }
}
