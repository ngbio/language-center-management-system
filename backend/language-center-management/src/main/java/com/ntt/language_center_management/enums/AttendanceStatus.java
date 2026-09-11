package com.ntt.language_center_management.enums;

public enum AttendanceStatus {
    PRESENT, // Có mặt và được tính là tham dự.
    ABSENT,  // Vắng mặt không phép, không được tính là tham dự.
    LATE,    // Đi trễ nhưng vẫn được tính là tham dự trong tỷ lệ chuyên cần.
    EXCUSED  // Vắng mặt có phép, hiện không được tính là tham dự.
}
