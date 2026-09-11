package com.ntt.language_center_management.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ntt.language_center_management.dto.response.ImageUploadResponse;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.security.CurrentUserResolver;
import com.ntt.language_center_management.service.ImageUploadService;
import java.io.IOException;
import java.security.Principal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryImageUploadService implements ImageUploadService {
  private static final Set<String> IMAGE_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

  private final Cloudinary cloudinary;
  private final CurrentUserResolver currentUserResolver;
  private final String cloudinaryUrl;
  private final long maxSizeBytes;

  public CloudinaryImageUploadService(
      Cloudinary cloudinary,
      CurrentUserResolver currentUserResolver,
      @Value("${cloudinary.url:}") String cloudinaryUrl,
      @Value("${cloudinary.image.max-size-bytes:5242880}") long maxSizeBytes) {
    this.cloudinary = cloudinary;
    this.currentUserResolver = currentUserResolver;
    this.cloudinaryUrl = cloudinaryUrl;
    this.maxSizeBytes = maxSizeBytes;
  }

  @Override
  public ImageUploadResponse upload(MultipartFile file, String purpose, Principal principal) {
    User user = currentUser(principal);
    String normalizedPurpose = normalizePurpose(purpose);
    authorize(user.getRoleId().getRoleCode(), normalizedPurpose);
    validate(file);
    if (!StringUtils.hasText(cloudinaryUrl)) {
      throw new IllegalArgumentException(
          "Cloudinary chưa được cấu hình. Hãy thêm CLOUDINARY_URL vào file .env");
    }

    String folder =
        "STUDENT_AVATAR".equals(normalizedPurpose)
            ? "language-center/student-avatars"
            : "language-center/course-images";
    try {
      Map<?, ?> result =
          cloudinary
              .uploader()
              .upload(
                  file.getBytes(),
                  ObjectUtils.asMap(
                      "folder", folder,
                      "resource_type", "image",
                      "use_filename", true,
                      "unique_filename", true,
                      "overwrite", false));
      return new ImageUploadResponse(
          String.valueOf(result.get("secure_url")),
          String.valueOf(result.get("public_id")),
          stringValue(result.get("format")),
          intValue(result.get("width")),
          intValue(result.get("height")),
          longValue(result.get("bytes")));
    } catch (IOException exception) {
      throw new IllegalStateException("Không thể đọc hoặc tải ảnh lên Cloudinary", exception);
    }
  }

  private User currentUser(Principal principal) {
    return currentUserResolver.requireUser(principal);
  }

  private String normalizePurpose(String purpose) {
    String value = StringUtils.hasText(purpose) ? purpose.trim().toUpperCase(Locale.ROOT) : "";
    if (!Set.of("STUDENT_AVATAR", "COURSE_THUMBNAIL", "COURSE_BANNER").contains(value)) {
      throw new IllegalArgumentException(
          "Mục đích ảnh phải là STUDENT_AVATAR, COURSE_THUMBNAIL hoặc COURSE_BANNER");
    }
    return value;
  }

  private void authorize(String role, String purpose) {
    if ("STUDENT_AVATAR".equals(purpose) && "STUDENT".equals(role)) return;
    if (!"STUDENT_AVATAR".equals(purpose) && "ADMIN".equals(role)) return;
    throw new ForbiddenException("Bạn không có quyền tải ảnh cho mục đích này");
  }

  private void validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Vui lòng chọn một ảnh để tải lên");
    }
    if (file.getSize() > maxSizeBytes) {
      throw new IllegalArgumentException("Ảnh không được vượt quá 5 MB");
    }
    String contentType = file.getContentType();
    if (contentType == null || !IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPG, PNG, WebP hoặc GIF");
    }
  }

  private String stringValue(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private Integer intValue(Object value) {
    return value instanceof Number number ? number.intValue() : null;
  }

  private long longValue(Object value) {
    return value instanceof Number number ? number.longValue() : 0L;
  }
}
