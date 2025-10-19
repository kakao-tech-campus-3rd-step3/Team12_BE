package unischedule.events.util;

import lombok.RequiredArgsConstructor;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.property.RRule;
import org.springframework.stereotype.Component;
import unischedule.exception.InvalidInputException;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class RRuleParser {
    private final ZonedDateTimeUtil zonedDateTimeUtil;

    public List<ZonedDateTime> calEventStartTimeListZdt(LocalDateTime firstStartTime, String rruleString) {
        Recur<ZonedDateTime> recur = getRecur(rruleString);

        ZonedDateTime seed = zonedDateTimeUtil.localDateTimeToZdt(firstStartTime);
        ZonedDateTime endBoundary = zonedDateTimeUtil.localDateTimeToZdt(getRepeatEndDate(firstStartTime, rruleString));
        return recur.getDates(seed, endBoundary);
    }

    public Recur<ZonedDateTime> getRecur(String rruleString) {
        try {
            RRule<ZonedDateTime> rrule = new RRule<>(rruleString);
            return rrule.getRecur();
        }
        catch (RuntimeException e) {
            throw new InvalidInputException("유효하지 않은 반복 규칙(RRULE) 형식입니다.");
        }
    }

    private LocalDateTime getRepeatEndDate(LocalDateTime startTime, String rruleString) {
        Optional<String> untilInfo = extractUntilValue(rruleString);

        return determineRepeatEndDate(untilInfo, startTime);
    }

    private Optional<String> extractUntilValue(String rruleString) {
        Pattern pattern = Pattern.compile("UNTIL=([0-9]{8}T[0-9]{6}Z)");
        Matcher matcher = pattern.matcher(rruleString.toUpperCase());

        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        else {
            return Optional.empty();
        }
    }

    private LocalDateTime determineRepeatEndDate(Optional<String> untilValue, LocalDateTime startTime) {
        // 시스템 부하 방지를 위해 반복 일정은 최대 2년으로 설정
        LocalDateTime maxEndDate = startTime.plusYears(2);

        if (untilValue.isEmpty()) {
            return maxEndDate;
        }

        LocalDateTime endDate = calEndDate(untilValue.get());

        if (endDate.isBefore(maxEndDate)) {
            return endDate;
        }
        else {
            return maxEndDate;
        }
    }

    private LocalDateTime calEndDate(String until) {
        int year = Integer.parseInt(until.substring(0, 4));
        int month = Integer.parseInt(until.substring(4, 6));
        int day = Integer.parseInt(until.substring(6, 8));

        return LocalDateTime.of(year, month, day, 23, 59, 59);
    }
}
