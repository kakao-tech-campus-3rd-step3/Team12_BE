package unischedule.events.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import unischedule.events.dto.EventModifyRequestDto;

import java.time.LocalDateTime;

public class EventTimeValidator implements ConstraintValidator<ValidEventTime, EventModifyRequestDto> {

    @Override
    public boolean isValid(EventModifyRequestDto requestDto, ConstraintValidatorContext context) {
        LocalDateTime startTime = requestDto.startTime();
        LocalDateTime endTime = requestDto.endTime();

        if (startTime == null && endTime == null) {
            return true;
        }

        if (startTime == null || endTime == null) {
            return false;
        }

        return startTime.isBefore(endTime);
    }
}
