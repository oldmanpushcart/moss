package io.github.oldmanpushcart.moss.backend.extra.caldav.util;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class CaldavUtils {

    /**
     * CalDAV日期格式化器
     */
    public static final DateTimeFormatter CALDAV_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);

    public static String formatDate(Date date) {
        return CALDAV_DATE_FORMATTER.format(date.toInstant());
    }

}
