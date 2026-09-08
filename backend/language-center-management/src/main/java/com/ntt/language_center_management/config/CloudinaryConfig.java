package com.ntt.language_center_management.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class CloudinaryConfig {

  @Bean
  public Cloudinary cloudinary(@Value("${cloudinary.url:}") String cloudinaryUrl) {
    return StringUtils.hasText(cloudinaryUrl) ? new Cloudinary(cloudinaryUrl) : new Cloudinary();
  }
}
