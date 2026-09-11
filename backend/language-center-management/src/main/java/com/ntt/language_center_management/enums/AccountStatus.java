package com.ntt.language_center_management.enums;

public enum AccountStatus {
    ACTIVE,   // Tài khoản đã được phép đăng nhập và sử dụng chức năng theo role.
    INACTIVE, // Tài khoản chưa được duyệt hoặc đã tạm ngừng hoạt động.
    LOCKED    // Tài khoản bị khóa và không được tiếp tục sử dụng hệ thống.
}
