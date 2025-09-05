package unischedule.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import unischedule.exception.dto.ErrorResponseDto;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleEntityNotFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidInput(InvalidInputException ex) {
        return ResponseEntity.badRequest().body(ErrorResponseDto.of(ex.getMessage()));
    }

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ErrorResponseDto> handleEntityNotFound(ExternalApiException ex) {
        return ResponseEntity.internalServerError().body(new ErrorResponseDto(ex.getMessage()));
    }
}

