package unischedule.events.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.dto.RecurringInstanceModifyRequestDto;

import java.time.LocalDateTime;

public class EventTimeValidator implements ConstraintValidator<ValidEventTime, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        LocalDateTime startTime;
        LocalDateTime endTime;

        if (value == null) {
            return false;
        }

        if (value instanceof EventModifyRequestDto requestDto) {
            startTime = requestDto.startTime();
            endTime = requestDto.endTime();
        }
        else if (value instanceof RecurringInstanceModifyRequestDto requestDto) {
            startTime = requestDto.startTime();
            endTime = requestDto.endTime();
        }
        else {
            return false;
        }

        if (startTime == null && endTime == null) {
            return true;
        }

        if (startTime == null || endTime == null) {
            return false;
        }

        return startTime.isBefore(endTime);
    }
}
