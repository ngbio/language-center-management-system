package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.response.ImageUploadResponse;
import java.security.Principal;
import org.springframework.web.multipart.MultipartFile;

public interface ImageUploadService {
  ImageUploadResponse upload(MultipartFile file, String purpose, Principal principal);
}
