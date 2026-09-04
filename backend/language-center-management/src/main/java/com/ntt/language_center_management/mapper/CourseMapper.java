package com.ntt.language_center_management.mapper;

import com.ntt.language_center_management.dto.request.CourseRequest;
import com.ntt.language_center_management.dto.response.CourseResponse;
import com.ntt.language_center_management.entity.Course;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {
  public CourseResponse toResponse(Course course) {
    var level = course.getLevelId();
    var language = level.getLanguageId();
    return new CourseResponse(
        course.getId(),
        course.getCourseCode(),
        course.getCourseName(),
        course.getSlug(),
        course.getShortDescription(),
        course.getDescription(),
        course.getThumbnailUrl(),
        course.getBannerUrl(),
        course.getTargetAudience(),
        course.getPrerequisites(),
        course.getLearningOutcomes(),
        course.getSyllabusSummary(),
        course.getCertificateInfo(),
        course.getTuitionFee(),
        course.getTotalSessions(),
        course.getDurationHours(),
        course.getStatus(),
        course.getPublicationStatus(),
        course.getPublishedAt(),
        course.getIsFeatured(),
        level.getId(),
        level.getLevelCode(),
        level.getLevelName(),
        language.getId(),
        language.getLanguageCode(),
        language.getLanguageName(),
        course.getCreatedAt(),
        course.getUpdatedAt());
  }

  public CourseRequest toRequest(Course course) {
    CourseRequest request = new CourseRequest();
    request.setId(course.getId());
    request.setCourseCode(course.getCourseCode());
    request.setCourseName(course.getCourseName());
    request.setSlug(course.getSlug());
    request.setShortDescription(course.getShortDescription());
    request.setDescription(course.getDescription());
    request.setThumbnailUrl(course.getThumbnailUrl());
    request.setBannerUrl(course.getBannerUrl());
    request.setTargetAudience(course.getTargetAudience());
    request.setPrerequisites(course.getPrerequisites());
    request.setLearningOutcomes(course.getLearningOutcomes());
    request.setSyllabusSummary(course.getSyllabusSummary());
    request.setCertificateInfo(course.getCertificateInfo());
    request.setTuitionFee(course.getTuitionFee());
    request.setTotalSessions(course.getTotalSessions());
    request.setDurationHours(course.getDurationHours());
    request.setStatus(course.getStatus());
    request.setPublicationStatus(course.getPublicationStatus());
    request.setFeatured(course.getIsFeatured());
    request.setLevelId(course.getLevelId().getId());
    return request;
  }
}
