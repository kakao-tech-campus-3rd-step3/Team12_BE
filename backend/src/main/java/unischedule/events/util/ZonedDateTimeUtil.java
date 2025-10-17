package unischedule.events.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class ZonedDateTimeUtil {
    public ZonedDateTime localDateTimeToZdt(LocalDateTime time) {
        ZoneId zone = ZoneId.systemDefault();
        return time.atZone(zone);
    }
}
