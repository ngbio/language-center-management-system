package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.request.CourseRequest;
import com.ntt.language_center_management.dto.response.CourseResponse;
import com.ntt.language_center_management.dto.response.PageResponse;
import com.ntt.language_center_management.entity.Course;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.mapper.CourseMapper;
import com.ntt.language_center_management.repository.CourseRepository;
import com.ntt.language_center_management.repository.LevelRepository;
import com.ntt.language_center_management.service.CourseService;
import java.util.Date;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class CourseServiceImpl implements CourseService {
  private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE");
  private static final Set<String> SORT_FIELDS =
      Set.of("courseCode", "courseName", "tuitionFee", "createdAt");
  private final CourseRepository courseRepository;
  private final LevelRepository levelRepository;
  private final CourseMapper courseMapper;

  public CourseServiceImpl(
      CourseRepository courseRepository,
      LevelRepository levelRepository,
      CourseMapper courseMapper) {
    this.courseRepository = courseRepository;
    this.levelRepository = levelRepository;
    this.courseMapper = courseMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<CourseResponse> search(
      String keyword,
      Integer languageId,
      Integer levelId,
      String status,
      int page,
      int size,
      String sort) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 100);
    String sortField = SORT_FIELDS.contains(sort) ? sort : "courseCode";
    Specification<Course> spec = (root, query, cb) -> cb.conjunction();
    if (StringUtils.hasText(keyword)) {
      String value = "%" + keyword.trim().toLowerCase() + "%";
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.or(
                      cb.like(cb.lower(root.get("courseCode")), value),
                      cb.like(cb.lower(root.get("courseName")), value)));
    }
    if (languageId != null)
      spec =
          spec.and(
              (r, q, cb) -> cb.equal(r.get("levelId").get("languageId").get("id"), languageId));
    if (levelId != null)
      spec = spec.and((r, q, cb) -> cb.equal(r.get("levelId").get("id"), levelId));
    if (StringUtils.hasText(status)) {
      String normalized = normalizeStatus(status);
      spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), normalized));
    }
    var result =
        courseRepository
            .findAll(spec, PageRequest.of(safePage, safeSize, Sort.by(sortField).ascending()))
            .map(courseMapper::toResponse);
    return PageResponse.from(result);
  }

  @Override
  @Transactional(readOnly = true)
  public CourseResponse getById(Integer id) {
    return courseMapper.toResponse(find(id));
  }

  @Override
  @Transactional(readOnly = true)
  public CourseRequest getRequestById(Integer id) {
    return courseMapper.toRequest(find(id));
  }

  @Override
  public CourseResponse save(CourseRequest request) {
    String code = request.getCourseCode().trim().toUpperCase();
    if (request.getId() == null
        ? courseRepository.existsByCourseCodeIgnoreCase(code)
        : courseRepository.existsByCourseCodeIgnoreCaseAndIdNot(code, request.getId())) {
      throw new DuplicateResourceException("Mã khóa học đã tồn tại");
    }
    var level =
        levelRepository
            .findById(request.getLevelId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trình độ"));
    if (!"ACTIVE".equals(level.getStatus())
        || !"ACTIVE".equals(level.getLanguageId().getStatus())) {
      throw new IllegalArgumentException("Chỉ được chọn ngôn ngữ và trình độ đang hoạt động");
    }
    Course course = request.getId() == null ? new Course() : find(request.getId());
    Date now = new Date();
    if (course.getId() == null) course.setCreatedAt(now);
    course.setUpdatedAt(now);
    course.setCourseCode(code);
    course.setCourseName(request.getCourseName().trim());
    course.setDescription(trimToNull(request.getDescription()));
    course.setTuitionFee(request.getTuitionFee());
    course.setTotalSessions(request.getTotalSessions());
    course.setDurationHours(request.getDurationHours());
    course.setStatus(normalizeStatus(request.getStatus()));
    course.setLevelId(level);
    return courseMapper.toResponse(courseRepository.save(course));
  }

  @Override
  public void delete(Integer id) {
    Course course = find(id);
    if (course.getCourseclassList() != null && !course.getCourseclassList().isEmpty())
      throw new IllegalArgumentException("Không thể xóa khóa học đã có lớp");
    courseRepository.delete(course);
  }

  private Course find(Integer id) {
    return courseRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
  }

  private String normalizeStatus(String value) {
    String status = value == null ? "ACTIVE" : value.trim().toUpperCase();
    if (!STATUSES.contains(status))
      throw new IllegalArgumentException("Trạng thái khóa học không hợp lệ");
    return status;
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
