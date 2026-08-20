package com.hackathon.second_hand_first.common.response;

import com.hackathon.second_hand_first.activity.exception.RedirectForbiddenException;
import com.hackathon.second_hand_first.activity.exception.RedirectTargetNotFoundException;
import com.hackathon.second_hand_first.auth.exception.UnauthorizedException;
import com.hackathon.second_hand_first.location.exception.KakaoLocalException;
import com.hackathon.second_hand_first.product.exception.ProductNotFoundException;
import com.hackathon.second_hand_first.search.exception.AiServerUnavailableException;
import com.hackathon.second_hand_first.search.exception.ExternalPlatformSearchException;
import com.hackathon.second_hand_first.search.exception.SearchSessionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class
    })
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

    @ExceptionHandler(ExternalPlatformSearchException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalPlatformSearch(
            ExternalPlatformSearchException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(AiServerUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiServerUnavailable(
            AiServerUnavailableException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductNotFound(ProductNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(SearchSessionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSearchSessionNotFound(
            SearchSessionNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(RedirectForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleRedirectForbidden(RedirectForbiddenException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(RedirectTargetNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRedirectTargetNotFound(
            RedirectTargetNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(exception.getMessage()));
    }
}
