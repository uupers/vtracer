package none.nintha.vtraceapi.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

public class TimeUtil {
    public final static ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");

    /**
     * 转换时间戳（秒）
     *
     * @param timestamp
     * @return
     */
    public static LocalDateTime ofEpochSec(long timestamp) {
        Instant instant = Instant.ofEpochSecond(timestamp);
        return LocalDateTime.ofInstant(instant, ZONE_SHANGHAI);
    }

    public static Date toDate(LocalDateTime localDateTime) {
        ZonedDateTime zdt = localDateTime.atZone(ZONE_SHANGHAI);
        return Date.from(zdt.toInstant());
    }
}
