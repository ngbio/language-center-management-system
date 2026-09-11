package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.ntt.language_center_management.entity.Role;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.repository.TeacherRepository;
import com.ntt.language_center_management.security.CurrentUserResolver;
import com.ntt.language_center_management.service.impl.CloudinaryImageUploadService;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;

class CloudinaryImageUploadServiceTest {
  private Cloudinary cloudinary;
  private Uploader uploader;
  private UserRepository users;
  private CloudinaryImageUploadService service;
  private MultipartFile file;
  private CurrentUserResolver currentUserResolver;

  @BeforeEach
  void setUp() {
    cloudinary = mock(Cloudinary.class);
    uploader = mock(Uploader.class);
    users = mock(UserRepository.class);
    currentUserResolver = new CurrentUserResolver(
        users, mock(StudentRepository.class), mock(TeacherRepository.class));
    service = new CloudinaryImageUploadService(cloudinary, currentUserResolver,
        "cloudinary://key:secret@demo", 5 * 1024 * 1024L);
    file = mock(MultipartFile.class);
    when(cloudinary.uploader()).thenReturn(uploader);
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(100L);
    when(file.getContentType()).thenReturn("image/png");
    when(users.findByEmailIgnoreCase("student@example.com"))
        .thenReturn(Optional.of(user(7, "STUDENT")));
    when(users.findByEmailIgnoreCase("admin@example.com"))
        .thenReturn(Optional.of(user(1, "ADMIN")));
  }

  @Test
  void shouldRejectNullEmptyOversizedAndUnsupportedFiles() {
    assertThatThrownBy(() -> service.upload(null, "STUDENT_AVATAR", studentPrincipal()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("chọn một ảnh");

    when(file.isEmpty()).thenReturn(true);
    assertThatThrownBy(() -> service.upload(file, "STUDENT_AVATAR", studentPrincipal()))
        .hasMessageContaining("chọn một ảnh");

    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(5 * 1024 * 1024L + 1);
    assertThatThrownBy(() -> service.upload(file, "STUDENT_AVATAR", studentPrincipal()))
        .hasMessageContaining("5 MB");

    when(file.getSize()).thenReturn(100L);
    when(file.getContentType()).thenReturn("application/pdf");
    assertThatThrownBy(() -> service.upload(file, "STUDENT_AVATAR", studentPrincipal()))
        .hasMessageContaining("JPG, PNG");
  }

  @Test
  void shouldNormalizePurposeAndEnforceRolePermissions() throws Exception {
    assertThatThrownBy(() -> service.upload(file, "unknown", studentPrincipal()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Mục đích ảnh");
    assertThatThrownBy(() -> service.upload(file, "course_banner", studentPrincipal()))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> service.upload(file, "student_avatar", () -> "admin@example.com"))
        .isInstanceOf(ForbiddenException.class);
    verify(uploader, never()).upload(any(), anyMap());
  }

  @Test
  void shouldRejectMissingPrincipalUserAndConfiguration() {
    assertThatThrownBy(() -> service.upload(file, "STUDENT_AVATAR", null))
        .isInstanceOf(com.ntt.language_center_management.exception.UnauthorizedException.class);
    assertThatThrownBy(() -> service.upload(file, "STUDENT_AVATAR", () -> "missing@example.com"))
        .isInstanceOf(com.ntt.language_center_management.exception.ResourceNotFoundException.class)
        .hasMessageContaining("Không tìm thấy");

    CloudinaryImageUploadService unconfigured =
        new CloudinaryImageUploadService(
            cloudinary, currentUserResolver, "", 5 * 1024 * 1024L);
    assertThatThrownBy(() -> unconfigured.upload(file, "STUDENT_AVATAR", studentPrincipal()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("CLOUDINARY_URL");
  }

  @Test
  void shouldUploadStudentAvatarWithExpectedOptionsAndMapNumericResponse() throws Exception {
    when(file.getBytes()).thenReturn(new byte[] {1, 2, 3});
    when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
        "secure_url", "https://res.cloudinary.com/demo/image.png",
        "public_id", "language-center/student-avatars/avatar",
        "format", "png", "width", 640L, "height", 480, "bytes", 12345L));

    var response = service.upload(file, " student_avatar ", studentPrincipal());

    assertThat(response.url()).isEqualTo("https://res.cloudinary.com/demo/image.png");
    assertThat(response.publicId()).contains("student-avatars");
    assertThat(response.width()).isEqualTo(640);
    assertThat(response.height()).isEqualTo(480);
    assertThat(response.bytes()).isEqualTo(12345L);
    ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);
    verify(uploader).upload(any(byte[].class), options.capture());
    assertThat(options.getValue()).containsEntry("folder", "language-center/student-avatars")
        .containsEntry("resource_type", "image")
        .containsEntry("overwrite", false);
  }

  @Test
  void shouldUploadCourseImageOnlyForAdmin() throws Exception {
    when(file.getBytes()).thenReturn(new byte[] {1});
    when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
        "secure_url", "https://example.com/course.jpg", "public_id", "course/id"));

    service.upload(file, "COURSE_THUMBNAIL", () -> "admin@example.com");

    ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);
    verify(uploader).upload(any(byte[].class), options.capture());
    assertThat(options.getValue()).containsEntry("folder", "language-center/course-images");
  }

  @Test
  void shouldWrapCloudinaryIoFailure() throws Exception {
    when(file.getBytes()).thenThrow(new IOException("disk secret"));

    assertThatThrownBy(() -> service.upload(file, "STUDENT_AVATAR", studentPrincipal()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Không thể đọc hoặc tải ảnh lên Cloudinary")
        .hasCauseInstanceOf(IOException.class);
  }

  private Principal studentPrincipal() {
    return () -> "student@example.com";
  }

  private User user(int id, String role) {
    User value = new User(id);
    value.setRoleId(new Role(id, role, role));
    return value;
  }
}
