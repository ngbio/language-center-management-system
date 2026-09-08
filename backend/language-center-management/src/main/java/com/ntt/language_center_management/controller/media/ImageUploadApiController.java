package com.ntt.language_center_management.controller.media;

import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.ImageUploadResponse;
import com.ntt.language_center_management.service.ImageUploadService;
import java.security.Principal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class ImageUploadApiController {
  private final ImageUploadService imageUploadService;

  public ImageUploadApiController(ImageUploadService imageUploadService) {
    this.imageUploadService = imageUploadService;
  }

  @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<ImageUploadResponse> uploadImage(
      @RequestPart("file") MultipartFile file,
      @RequestParam String purpose,
      Principal principal) {
    return new ApiResponse<>(
        200, "Tải ảnh lên Cloudinary thành công", imageUploadService.upload(file, purpose, principal));
  }
}
