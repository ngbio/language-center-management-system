package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.request.LearningRequest;
import com.ntt.language_center_management.dto.response.LearningResponse;
import com.ntt.language_center_management.entity.*;
import com.ntt.language_center_management.enums.CatalogStatus;
import com.ntt.language_center_management.enums.PublicationStatus;
import com.ntt.language_center_management.exception.*;
import com.ntt.language_center_management.repository.*;
import com.ntt.language_center_management.service.LearningService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearningServiceImpl implements LearningService {
  private final CourseRepository courses;
  private final CourseSectionRepository sections;
  private final CourseContentRepository contents;
  private final StudentRepository students;
  private final EnrollmentRepository enrollments;
  private final FlashcardRepository cards;
  private final FlashcardReviewRepository reviews;
  private final QuizRepository quizzes;
  private final QuizQuestionRepository questions;
  private final QuizOptionRepository options;
  private final QuizAttemptRepository attempts;
  private final QuizAttemptAnswerRepository answers;
  private final EntityManager entityManager;
  @Value("${app.time-zone:Asia/Ho_Chi_Minh}")
  private String timeZone = "Asia/Ho_Chi_Minh";

  private LocalDateTime now() { return LocalDateTime.now(ZoneId.of(timeZone)); }
  private ResourceNotFoundException missing() { return new ResourceNotFoundException("Không tìm thấy nội dung học tập"); }
  private Student student(Principal principal) {
    if (principal == null) throw new UnauthorizedException("Vui lòng đăng nhập");
    return students.findByUserId_EmailIgnoreCase(principal.getName())
        .orElseThrow(() -> new ForbiddenException("Chức năng lưu lịch sử dành cho học viên"));
  }
  private Student optionalStudent(Principal principal) { return principal == null ? null : student(principal); }
  private boolean accessible(Course course, Student student) {
    return course.getStatus() == CatalogStatus.ACTIVE
        && course.getPublicationStatus() == PublicationStatus.PUBLISHED
        && ((course.getTuitionFee() != null && course.getTuitionFee().signum() == 0)
            || (student != null && enrollments.existsPaidConfirmedAccess(student.getId(), course.getId())));
  }
  private void access(Course course, Student student) {
    if (!accessible(course, student)) throw new ForbiddenException("Khóa học chưa được mở hoặc bạn chưa có quyền học");
  }
  private CourseContent content(Integer id) { return contents.findById(id).orElseThrow(this::missing); }
  private void access(CourseContent content, Student student) {
    if (content.getPublicationStatus() != PublicationStatus.PUBLISHED) throw missing();
    access(content.getSectionId().getCourseId(), student);
  }
  private Quiz quiz(Long id) { return quizzes.findById(id).orElseThrow(this::missing); }
  private Quiz lockedQuiz(Long id) { return quizzes.lockById(id).orElseThrow(this::missing); }
  private void access(Quiz quiz, Student student) {
    if (!"PUBLISHED".equals(quiz.getStatus())) throw missing();
    access(quiz.getContent(), student);
  }
  private void editable(Quiz quiz) {
    if (!"DRAFT".equals(quiz.getStatus()) || attempts.existsByQuiz_Id(quiz.getId()))
      throw new IllegalArgumentException("Chỉ sửa câu hỏi của quiz nháp chưa có lượt làm. Hãy tạo quiz mới.");
  }

  @Override
  public LearningResponse.Course course(Integer id, Principal principal) {
    var course = courses.findById(id).orElseThrow(this::missing);
    access(course, optionalStudent(principal));
    return new LearningResponse.Course(id, course.getCourseName(),
        sections.findByCourseId_IdOrderByDisplayOrderAsc(id).stream().map(section ->
            new LearningResponse.Section(section.getId(), section.getTitle(),
                contents.findBySectionId_IdAndPublicationStatusOrderByDisplayOrderAsc(
                    section.getId(), PublicationStatus.PUBLISHED).stream().map(c ->
                    new LearningResponse.Content(c.getId(), c.getTitle(), c.getSummary(),
                        c.getContentHtml(), c.getDocumentUrl())).toList())).toList());
  }

  private LearningResponse.Card cardView(Flashcard card, Student student) {
    var review = student == null ? null : reviews.findByStudent_IdAndFlashcard_Id(student.getId(), card.getId()).orElse(null);
    return new LearningResponse.Card(card.getId(), card.getContent().getId(), card.getFrontText(),
        card.getBackText(), card.getExampleSentence(), card.getDisplayOrder(), card.getStatus(),
        review == null ? "NEW" : review.getMasteryLevel(), review == null ? null : review.getNextReviewAt());
  }
  @Override
  public List<LearningResponse.Card> cards(Integer contentId, Principal principal) {
    var student = optionalStudent(principal);
    access(content(contentId), student);
    var time = now();
    return cards.findByContent_IdOrderByDisplayOrderAsc(contentId).stream()
        .filter(c -> "ACTIVE".equals(c.getStatus())).map(c -> cardView(c, student))
        .sorted(Comparator.comparingInt((LearningResponse.Card c) ->
            c.nextReviewAt() == null || !c.nextReviewAt().isAfter(time) ? 0 : 1)).toList();
  }
  @Override
  public List<LearningResponse.Card> due(Principal principal) {
    var student = student(principal);
    return reviews.findByStudent_IdAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(student.getId(), now())
        .stream().map(FlashcardReview::getFlashcard)
        .filter(c -> "ACTIVE".equals(c.getStatus())
            && c.getContent().getPublicationStatus() == PublicationStatus.PUBLISHED
            && accessible(c.getContent().getSectionId().getCourseId(), student))
        .map(c -> cardView(c, student)).toList();
  }
  @Override @Transactional
  public LearningResponse.Card review(Long id, LearningRequest.Review request, Principal principal) {
    var student = student(principal);
    entityManager.lock(student, LockModeType.PESSIMISTIC_WRITE);
    var card = cards.findById(id).orElseThrow(this::missing);
    access(card.getContent(), student);
    if (!"ACTIVE".equals(card.getStatus())) throw missing();
    int days = switch (request.masteryLevel()) {
      case "AGAIN" -> 1; case "HARD" -> 3; case "REMEMBERED" -> 7;
      default -> throw new IllegalArgumentException("Mức độ nhớ không hợp lệ");
    };
    var review = reviews.findByStudent_IdAndFlashcard_Id(student.getId(), id).orElseGet(FlashcardReview::new);
    review.setStudent(student); review.setFlashcard(card); review.setMasteryLevel(request.masteryLevel());
    review.setRepetitionCount(review.getRepetitionCount() + 1); review.setIntervalDays(days);
    review.setLastReviewedAt(now()); review.setNextReviewAt(review.getLastReviewedAt().plusDays(days));
    reviews.saveAndFlush(review);
    return cardView(card, student);
  }

  private LearningResponse.Quiz quizView(Quiz quiz) {
    return new LearningResponse.Quiz(quiz.getId(), quiz.getTitle(), quiz.getPassingScore(), quiz.getMaxAttempts(),
        quiz.getStatus(), questions.findByQuiz_IdOrderByDisplayOrderAsc(quiz.getId()).stream().map(q ->
            new LearningResponse.Question(q.getId(), q.getQuestionText(), q.getQuestionType(), q.getPoints(),
                options.findByQuestion_IdOrderByDisplayOrderAsc(q.getId()).stream().map(o ->
                    new LearningResponse.Option(o.getId(), o.getOptionText())).toList())).toList());
  }
  @Override
  public List<LearningResponse.Quiz> quizzes(Integer contentId, Principal principal) {
    access(content(contentId), optionalStudent(principal));
    return quizzes.findByContent_IdOrderByIdAsc(contentId).stream().filter(q -> "PUBLISHED".equals(q.getStatus()))
        .map(this::quizView).toList();
  }

  private LearningResponse.Result grade(Quiz quiz, LearningRequest.Submission request, QuizAttempt attempt) {
    var items = questions.findByQuiz_IdOrderByDisplayOrderAsc(quiz.getId());
    if (items.isEmpty()) throw new IllegalArgumentException("Quiz chưa có câu hỏi");
    Map<Long, Long> selected = new HashMap<>();
    Set<Long> questionIds = items.stream().map(QuizQuestion::getId).collect(Collectors.toSet());
    for (var answer : request.answers()) {
      if (!questionIds.contains(answer.questionId()) || selected.containsKey(answer.questionId()))
        throw new IllegalArgumentException("Câu hỏi bị trùng hoặc không thuộc quiz");
      selected.put(answer.questionId(), answer.selectedOptionId());
    }
    BigDecimal total = BigDecimal.ZERO, earned = BigDecimal.ZERO;
    List<LearningResponse.Answer> results = new ArrayList<>();
    List<QuizAttemptAnswer> saved = new ArrayList<>();
    for (var question : items) {
      var choices = options.findByQuestion_IdOrderByDisplayOrderAsc(question.getId());
      var correct = choices.stream().filter(QuizOption::isCorrect).toList();
      if (correct.size() != 1) throw new IllegalArgumentException("Quiz có đáp án không hợp lệ");
      Long selectedId = selected.get(question.getId());
      var choice = selectedId == null ? null : choices.stream().filter(o -> o.getId().equals(selectedId)).findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Lựa chọn không thuộc câu hỏi"));
      boolean isCorrect = choice != null && choice.isCorrect();
      BigDecimal points = isCorrect ? question.getPoints() : BigDecimal.ZERO;
      total = total.add(question.getPoints()); earned = earned.add(points);
      results.add(new LearningResponse.Answer(question.getId(), selectedId, correct.getFirst().getId(),
          isCorrect, points, question.getExplanation()));
      if (attempt != null) {
        var answer = new QuizAttemptAnswer();
        answer.setAttempt(attempt); answer.setQuestion(question); answer.setSelectedOption(choice);
        answer.setCorrect(isCorrect); answer.setPointsAwarded(points); saved.add(answer);
      }
    }
    if (total.signum() <= 0) throw new IllegalArgumentException("Tổng điểm phải lớn hơn 0");
    BigDecimal score = earned.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    boolean passed = score.compareTo(quiz.getPassingScore()) >= 0;
    if (attempt != null) {
      answers.saveAll(saved);
      attempt.setScore(score); attempt.setPassed(passed); attempt.setSubmittedAt(now()); attempts.save(attempt);
    }
    return new LearningResponse.Result(attempt == null ? null : attempt.getId(), score, passed, results);
  }
  @Override
  public LearningResponse.Result evaluate(Long quizId, LearningRequest.Submission request) {
    var quiz = quiz(quizId); access(quiz, null);
    return grade(quiz, request, null);
  }
  @Override @Transactional
  public LearningResponse.Attempt start(Long quizId, Principal principal) {
    var student = student(principal);
    var quiz = lockedQuiz(quizId); access(quiz, student);
    var history = attempts.findByStudent_IdAndQuiz_IdOrderByAttemptNumberDesc(student.getId(), quizId);
    // Resume an unfinished attempt; repeated requests do not consume the limit.
    var open = history.stream().filter(a -> a.getSubmittedAt() == null).findFirst();
    if (open.isPresent()) return attemptView(open.get());
    if (quiz.getMaxAttempts() != null && history.size() >= quiz.getMaxAttempts())
      throw new IllegalArgumentException("Bạn đã sử dụng hết số lần làm quiz");
    var attempt = new QuizAttempt(); attempt.setQuiz(quiz); attempt.setStudent(student);
    attempt.setAttemptNumber(history.isEmpty() ? 1 : history.getFirst().getAttemptNumber() + 1);
    attempt.setStartedAt(now()); attempts.saveAndFlush(attempt);
    return attemptView(attempt);
  }
  @Override @Transactional
  public LearningResponse.Result submit(Long id, LearningRequest.Submission request, Principal principal) {
    var student = student(principal);
    var attempt = attempts.lockById(id).orElseThrow(this::missing);
    if (!attempt.getStudent().getId().equals(student.getId())) throw new ForbiddenException("Lượt làm không thuộc về bạn");
    access(attempt.getQuiz(), student);
    if (attempt.getSubmittedAt() != null) throw new IllegalArgumentException("Lượt làm này đã được nộp");
    return grade(attempt.getQuiz(), request, attempt);
  }
  private LearningResponse.Attempt attemptView(QuizAttempt attempt) {
    LearningResponse.Result result = null;
    if (attempt.getSubmittedAt() != null) {
      var detail = answers.findByAttempt_IdOrderByIdAsc(attempt.getId()).stream().map(a ->
          new LearningResponse.Answer(a.getQuestion().getId(),
              a.getSelectedOption() == null ? null : a.getSelectedOption().getId(),
              options.findByQuestion_IdOrderByDisplayOrderAsc(a.getQuestion().getId()).stream()
                  .filter(QuizOption::isCorrect).map(QuizOption::getId).findFirst().orElse(null),
              a.isCorrect(), a.getPointsAwarded(), a.getQuestion().getExplanation())).toList();
      result = new LearningResponse.Result(attempt.getId(), attempt.getScore(), attempt.getPassed(), detail);
    }
    return new LearningResponse.Attempt(attempt.getId(), attempt.getAttemptNumber(), attempt.getScore(),
        attempt.getPassed(), attempt.getStartedAt(), attempt.getSubmittedAt(), quizView(attempt.getQuiz()), result);
  }
  @Override
  public List<LearningResponse.Attempt> history(Long quizId, Principal principal) {
    var student = student(principal);
    // Archived quizzes remain in the owner's history as long as course access remains valid.
    access(quiz(quizId).getContent(), student);
    return attempts.findByStudent_IdAndQuiz_IdOrderByAttemptNumberDesc(student.getId(), quizId)
        .stream().map(this::attemptView).toList();
  }

  @Override public List<LearningResponse.Card> adminCards(Integer contentId) {
    content(contentId);
    return cards.findByContent_IdOrderByDisplayOrderAsc(contentId).stream().map(c -> cardView(c, null)).toList();
  }
  @Override @Transactional
  public LearningResponse.Card saveCard(Integer contentId, Long id, LearningRequest.Card request) {
    var card = id == null ? new Flashcard() : cards.findById(id).orElseThrow(this::missing);
    var content = id == null ? content(contentId) : card.getContent();
    entityManager.lock(content, LockModeType.PESSIMISTIC_WRITE);
    if (id == null) {
      card.setContent(content);
      card.setDisplayOrder(cards.findByContent_IdOrderByDisplayOrderAsc(content.getId()).stream()
          .mapToInt(Flashcard::getDisplayOrder).max().orElse(0) + 1);
    }
    card.setFrontText(request.frontText().trim()); card.setBackText(request.backText().trim());
    card.setExampleSentence(request.exampleSentence()); card.setStatus(request.status());
    return cardView(cards.save(card), null);
  }
  @Override @Transactional public void deactivateCard(Long id) {
    var card = cards.findById(id).orElseThrow(this::missing);
    card.setStatus("INACTIVE"); cards.save(card);
  }
  @Override @Transactional public void reorderCards(Integer contentId, LearningRequest.Order request) {
    entityManager.lock(content(contentId), LockModeType.PESSIMISTIC_WRITE);
    var all = cards.findByContent_IdOrderByDisplayOrderAsc(contentId);
    var byId = all.stream().collect(Collectors.toMap(Flashcard::getId, Function.identity()));
    if (request.ids().size() != all.size() || new HashSet<>(request.ids()).size() != all.size()
        || !byId.keySet().equals(new HashSet<>(request.ids())))
      throw new IllegalArgumentException("Danh sách sắp xếp phải chứa đủ và đúng flashcard của bài");
    int offset = all.stream().mapToInt(Flashcard::getDisplayOrder).max().orElse(0) + 1;
    for (int i=0; i<all.size(); i++) all.get(i).setDisplayOrder(offset+i);
    cards.saveAllAndFlush(all);
    for (int i=0; i<request.ids().size(); i++) byId.get(request.ids().get(i)).setDisplayOrder(i+1);
    cards.saveAllAndFlush(all);
  }

  private LearningResponse.AdminQuiz adminQuiz(Quiz quiz) {
    return new LearningResponse.AdminQuiz(quiz.getId(), quiz.getTitle(), quiz.getPassingScore(),
        quiz.getMaxAttempts(), quiz.getStatus(), attempts.existsByQuiz_Id(quiz.getId()),
        questions.findByQuiz_IdOrderByDisplayOrderAsc(quiz.getId()).stream().map(q ->
            new LearningResponse.AdminQuestion(q.getId(), q.getQuestionText(), q.getQuestionType(),
                q.getExplanation(), q.getPoints(), options.findByQuestion_IdOrderByDisplayOrderAsc(q.getId()).stream()
                    .map(o -> new LearningResponse.AdminOption(o.getId(), o.getOptionText(), o.isCorrect())).toList())).toList());
  }
  @Override public List<LearningResponse.AdminQuiz> adminQuizzes(Integer contentId) {
    content(contentId);
    return quizzes.findByContent_IdOrderByIdAsc(contentId).stream().map(this::adminQuiz).toList();
  }
  private void validQuestion(String type, List<Boolean> correct) {
    if (correct.size() < 2 || correct.stream().filter(Boolean::booleanValue).count() != 1
        || ("TRUE_FALSE".equals(type) && correct.size() != 2))
      throw new IllegalArgumentException("Mỗi câu cần đúng một đáp án đúng; câu đúng/sai cần đúng hai lựa chọn");
  }
  @Override @Transactional
  public LearningResponse.AdminQuiz saveQuiz(Integer contentId, Long id, LearningRequest.Quiz request) {
    var quiz = id == null ? new Quiz() : lockedQuiz(id);
    if (id == null) quiz.setContent(content(contentId));
    if (id != null && attempts.existsByQuiz_Id(id)
        && (quiz.getPassingScore().compareTo(request.passingScore()) != 0
            || !Objects.equals(quiz.getMaxAttempts(), request.maxAttempts())))
      throw new IllegalArgumentException("Không thay đổi thang đạt hoặc giới hạn sau khi có lượt làm");
    if ("PUBLISHED".equals(request.status())) {
      var items = id == null ? List.<QuizQuestion>of() : questions.findByQuiz_IdOrderByDisplayOrderAsc(id);
      if (items.isEmpty()) throw new IllegalArgumentException("Hãy thêm câu hỏi trước khi xuất bản");
      for (var q : items) validQuestion(q.getQuestionType(),
          options.findByQuestion_IdOrderByDisplayOrderAsc(q.getId()).stream().map(QuizOption::isCorrect).toList());
    }
    quiz.setTitle(request.title().trim()); quiz.setPassingScore(request.passingScore());
    quiz.setMaxAttempts(request.maxAttempts()); quiz.setStatus(request.status());
    return adminQuiz(quizzes.saveAndFlush(quiz));
  }
  @Override @Transactional public void archiveQuiz(Long id) {
    var quiz = lockedQuiz(id); quiz.setStatus("ARCHIVED"); quizzes.save(quiz);
  }
  @Override @Transactional
  public LearningResponse.AdminQuiz saveQuestion(Long quizId, Long id, LearningRequest.Question request) {
    var question = id == null ? new QuizQuestion() : questions.findById(id).orElseThrow(this::missing);
    var quiz = lockedQuiz(id == null ? quizId : question.getQuiz().getId()); editable(quiz);
    validQuestion(request.questionType(), request.options().stream().map(LearningRequest.Option::correct).toList());
    if (id == null) {
      if (questions.findByQuiz_IdOrderByDisplayOrderAsc(quiz.getId()).size() >= 200)
        throw new IllegalArgumentException("Quiz tối đa 200 câu");
      question.setQuiz(quiz);
      question.setDisplayOrder(questions.findByQuiz_IdOrderByDisplayOrderAsc(quiz.getId()).stream()
          .mapToInt(QuizQuestion::getDisplayOrder).max().orElse(0) + 1);
    } else {
      options.deleteAll(options.findByQuestion_IdOrderByDisplayOrderAsc(id)); options.flush();
    }
    question.setQuestionText(request.questionText().trim()); question.setQuestionType(request.questionType());
    question.setExplanation(request.explanation()); question.setPoints(request.points());
    questions.saveAndFlush(question);
    for (int i=0; i<request.options().size(); i++) {
      var input = request.options().get(i); var option = new QuizOption();
      option.setQuestion(question); option.setOptionText(input.optionText().trim());
      option.setCorrect(input.correct()); option.setDisplayOrder(i+1); options.save(option);
    }
    options.flush(); return adminQuiz(quiz);
  }
  @Override @Transactional public void deleteQuestion(Long id) {
    var question = questions.findById(id).orElseThrow(this::missing);
    editable(lockedQuiz(question.getQuiz().getId()));
    options.deleteAll(options.findByQuestion_IdOrderByDisplayOrderAsc(id)); options.flush();
    questions.delete(question);
  }
}
