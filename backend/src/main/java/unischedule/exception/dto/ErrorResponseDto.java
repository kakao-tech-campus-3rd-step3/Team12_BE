package unischedule.exception.dto;

public record ErrorResponseDto(
        String message
) {
    public static ErrorResponseDto of(String message){
        return new ErrorResponseDto(message);
    }
}
