package ge.croco.order.exception;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleOrderNotFoundException(OrderNotFoundException e, HttpServletRequest request) {
        log.error("Request: {} raised", request.getRequestURL(), e);
        ErrorResponse errorResponse = ErrorResponse.builder(e, HttpStatus.NOT_FOUND, e.getMessage()).build();

        return ResponseEntity.status(errorResponse.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(OrderAlreadyExistsException.class)
    protected ResponseEntity<ErrorResponse> handleOrderAlreadyExistsException(OrderAlreadyExistsException e, HttpServletRequest request) {
        log.error("Request: {} raised", request.getRequestURL(), e);
        ErrorResponse errorResponse = ErrorResponse.builder(e, HttpStatus.CONFLICT, e.getMessage()).build();

        return ResponseEntity.status(errorResponse.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(OrderInvalidStatusException.class)
    protected ResponseEntity<ErrorResponse> handleOrderAlreadyExistsException(OrderInvalidStatusException e, HttpServletRequest request) {
        log.error("Request: {} raised", request.getRequestURL(), e);
        ErrorResponse errorResponse = ErrorResponse.builder(e, HttpStatus.FORBIDDEN, e.getMessage()).build();

        return ResponseEntity.status(errorResponse.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e, HttpServletRequest request) {
        log.error("Request: {} raised", request.getRequestURL(), e);
        String errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(",\n"));
        ErrorResponse errorResponse = ErrorResponse.builder(e, HttpStatus.BAD_REQUEST, errors).build();

        return ResponseEntity.status(errorResponse.getStatusCode()).body(errorResponse);
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(AuthenticationException e, HttpServletRequest request) {
        log.error("Request: {} raised", request.getRequestURL(), e);
        ErrorResponse errorResponse = ErrorResponse.builder(e, HttpStatus.UNAUTHORIZED, e.getMessage()).build();

        return ResponseEntity.status(errorResponse.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException e, HttpServletRequest request) {
        log.error("Request: {} raised", request.getRequestURL(), e);
        ErrorResponse errorResponse = ErrorResponse.builder(e, HttpStatus.FORBIDDEN, e.getMessage()).build();

        return ResponseEntity.status(errorResponse.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("Request: {} raised", request.getRequestURL(), e);
        ErrorResponse errorResponse = ErrorResponse.builder(e, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()).build();

        return ResponseEntity.status(errorResponse.getStatusCode()).body(errorResponse);
    }

}
