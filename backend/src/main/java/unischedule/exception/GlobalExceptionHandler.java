package unischedule.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import unischedule.exception.dto.EntityAlreadyExistsException;
import unischedule.exception.dto.ErrorResponseDto;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponseDto.of(ex.getMessage()));
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidInput(InvalidInputException ex) {
        return ResponseEntity.badRequest().body(ErrorResponseDto.of(ex.getMessage()));
    }

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ErrorResponseDto> handleEntityNotFound(ExternalApiException ex) {
        return ResponseEntity.internalServerError().body(ErrorResponseDto.of(ex.getMessage()));
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleEntityAlreadyExists(EntityAlreadyExistsException ex) {
        return ResponseEntity.badRequest().body(ErrorResponseDto.of(ex.getMessage()));
    }
    
    @ExceptionHandler(NoPermissionException.class)
    public ResponseEntity<ErrorResponseDto> handleNoPermission(NoPermissionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponseDto.of(ex.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> constraintValidationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest().body(ErrorResponseDto.of(message));
    }
}

