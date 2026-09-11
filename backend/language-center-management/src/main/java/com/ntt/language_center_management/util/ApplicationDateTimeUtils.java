package com.ntt.language_center_management.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

public final class ApplicationDateTimeUtils {

  private ApplicationDateTimeUtils() {}

  public static LocalDate toLocalDate(Date value, ZoneId zoneId) {
    if (value == null) return null;
    if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
    return Instant.ofEpochMilli(value.getTime()).atZone(zoneId).toLocalDate();
  }

  public static LocalTime toLocalTime(Date value, ZoneId zoneId) {
    if (value == null) return null;
    if (value instanceof java.sql.Time sqlTime) return sqlTime.toLocalTime();
    return Instant.ofEpochMilli(value.getTime()).atZone(zoneId).toLocalTime();
  }

  public static LocalDateTime toLocalDateTime(Date value, ZoneId zoneId) {
    return value == null
        ? null
        : Instant.ofEpochMilli(value.getTime()).atZone(zoneId).toLocalDateTime();
  }
}
