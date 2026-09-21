package com.ntt.language_center_management.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.ntt.language_center_management.dto.request.LearningRequest;
import com.ntt.language_center_management.entity.*;
import com.ntt.language_center_management.enums.*;
import com.ntt.language_center_management.exception.*;
import com.ntt.language_center_management.repository.*;
import com.ntt.language_center_management.service.impl.LearningServiceImpl;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LearningServiceImplTest {
  @Mock CourseRepository courses;
  @Mock CourseSectionRepository sections;
  @Mock CourseContentRepository contents;
  @Mock StudentRepository students;
  @Mock EnrollmentRepository enrollments;
  @Mock FlashcardRepository cards;
  @Mock FlashcardReviewRepository reviews;
  @Mock QuizRepository quizzes;
  @Mock QuizQuestionRepository questions;
  @Mock QuizOptionRepository options;
  @Mock QuizAttemptRepository attempts;
  @Mock QuizAttemptAnswerRepository answers;
  @Mock EntityManager entityManager;
  @Spy com.ntt.language_center_management.learning.FixedIntervalReviewStrategy reviewScheduling =
      new com.ntt.language_center_management.learning.FixedIntervalReviewStrategy();
  @Spy com.ntt.language_center_management.learning.SingleAnswerGradingStrategy questionGrading =
      new com.ntt.language_center_management.learning.SingleAnswerGradingStrategy();
  @InjectMocks LearningServiceImpl service;
  Course course;
  CourseContent content;
  Quiz quiz;
  Student student;
  Principal principal = () -> "student@example.com";

  @BeforeEach void setup() {
    course = new Course(); course.setId(1); course.setTuitionFee(BigDecimal.ZERO);
    course.setStatus(CatalogStatus.ACTIVE); course.setPublicationStatus(PublicationStatus.PUBLISHED);
    var section = new CourseSection(); section.setId(2); section.setCourseId(course);
    content = new CourseContent(); content.setId(3); content.setSectionId(section);
    content.setPublicationStatus(PublicationStatus.PUBLISHED);
    student = new Student(); student.setId(7);
    quiz = new Quiz(); quiz.setId(4L); quiz.setContent(content);
    quiz.setStatus("PUBLISHED"); quiz.setPassingScore(new BigDecimal("50.00"));
  }
  void authenticate() { when(students.findByUserId_EmailIgnoreCase(principal.getName())).thenReturn(Optional.of(student)); }
  void gradingFixture() {
    when(quizzes.findById(4L)).thenReturn(Optional.of(quiz));
    var q = new QuizQuestion(); q.setId(10L); q.setQuiz(quiz);
    q.setPoints(new BigDecimal("2")); q.setQuestionText("Hello?"); q.setQuestionType("SINGLE_CHOICE");
    q.setExplanation("Hello là xin chào");
    when(questions.findByQuiz_IdOrderByDisplayOrderAsc(4L)).thenReturn(List.of(q));
    var correct = new QuizOption(); correct.setId(11L); correct.setQuestion(q); correct.setCorrect(true); correct.setOptionText("Xin chào");
    var wrong = new QuizOption(); wrong.setId(12L); wrong.setQuestion(q); wrong.setOptionText("Tạm biệt");
    when(options.findByQuestion_IdOrderByDisplayOrderAsc(10L)).thenReturn(List.of(correct, wrong));
  }
  LearningRequest.Submission submission(Long option) {
    return new LearningRequest.Submission(List.of(new LearningRequest.Answer(10L, option)));
  }
  @Test void freeGuestCanReadWithoutEnrollment() {
    when(contents.findById(3)).thenReturn(Optional.of(content));
    when(cards.findByContent_IdOrderByDisplayOrderAsc(3)).thenReturn(List.of());
    assertTrue(service.cards(3, null).isEmpty());
    verifyNoInteractions(students, enrollments, reviews);
  }
  @Test void paidGuestCannotReadEvenPreview() {
    course.setTuitionFee(BigDecimal.TEN); content.setIsPreview(true);
    when(contents.findById(3)).thenReturn(Optional.of(content));
    assertThrows(ForbiddenException.class, () -> service.cards(3, null));
    verifyNoInteractions(cards);
  }
  @Test void paidStudentRequiresConfirmedPaidAccess() {
    authenticate(); course.setTuitionFee(BigDecimal.TEN);
    when(contents.findById(3)).thenReturn(Optional.of(content));
    when(enrollments.existsPaidConfirmedAccess(7,1)).thenReturn(false);
    assertThrows(ForbiddenException.class, () -> service.cards(3, principal));
  }
  @Test void paidStudentWithAccessCanRead() {
    authenticate(); course.setTuitionFee(BigDecimal.TEN);
    when(contents.findById(3)).thenReturn(Optional.of(content));
    when(enrollments.existsPaidConfirmedAccess(7,1)).thenReturn(true);
    when(cards.findByContent_IdOrderByDisplayOrderAsc(3)).thenReturn(List.of());
    assertTrue(service.cards(3, principal).isEmpty());
  }
  @Test void unpublishedLessonIsNotExposedEvenInFreeCourse() {
    content.setPublicationStatus(PublicationStatus.DRAFT);
    when(contents.findById(3)).thenReturn(Optional.of(content));
    assertThrows(ResourceNotFoundException.class, () -> service.cards(3,null));
  }
  @Test void freeGuestQuizIsGradedWithoutWritingHistory() {
    gradingFixture();
    var result = service.evaluate(4L, submission(11L));
    assertEquals(new BigDecimal("100.00"), result.score()); assertTrue(result.passed());
    assertNull(result.attemptId()); assertEquals(11L, result.answers().getFirst().correctOptionId());
    verifyNoInteractions(attempts, answers, students, enrollments);
  }
  @Test void unansweredQuestionScoresZero() {
    gradingFixture();
    var result = service.evaluate(4L, new LearningRequest.Submission(List.of()));
    assertEquals(new BigDecimal("0.00"), result.score()); assertFalse(result.passed());
  }
  @Test void optionFromAnotherQuestionIsRejected() {
    gradingFixture();
    assertThrows(IllegalArgumentException.class, () -> service.evaluate(4L, submission(999L)));
    verifyNoInteractions(answers, attempts);
  }
  @Test void duplicateQuestionIsRejected() {
    when(quizzes.findById(4L)).thenReturn(Optional.of(quiz));
    var q = new QuizQuestion(); q.setId(10L);
    when(questions.findByQuiz_IdOrderByDisplayOrderAsc(4L)).thenReturn(List.of(q));
    assertThrows(IllegalArgumentException.class, () -> service.evaluate(4L, new LearningRequest.Submission(
        List.of(new LearningRequest.Answer(10L,null),new LearningRequest.Answer(10L,11L)))));
  }
  @Test void questionOutsideQuizIsRejected() {
    when(quizzes.findById(4L)).thenReturn(Optional.of(quiz));
    var q = new QuizQuestion(); q.setId(10L);
    when(questions.findByQuiz_IdOrderByDisplayOrderAsc(4L)).thenReturn(List.of(q));
    assertThrows(IllegalArgumentException.class, () -> service.evaluate(4L, new LearningRequest.Submission(
        List.of(new LearningRequest.Answer(99L,11L)))));
  }
  @Test void guestCannotEvaluatePaidOrArchivedQuiz() {
    when(quizzes.findById(4L)).thenReturn(Optional.of(quiz));
    course.setTuitionFee(BigDecimal.TEN);
    assertThrows(ForbiddenException.class, () -> service.evaluate(4L, submission(11L)));
    course.setTuitionFee(BigDecimal.ZERO); quiz.setStatus("ARCHIVED");
    assertThrows(ResourceNotFoundException.class, () -> service.evaluate(4L, submission(11L)));
  }
  @Test void cannotSubmitAnotherStudentsAttempt() {
    authenticate();
    var other = new Student(); other.setId(8);
    var attempt = new QuizAttempt(); attempt.setStudent(other);
    when(attempts.lockById(9L)).thenReturn(Optional.of(attempt));
    assertThrows(ForbiddenException.class, () -> service.submit(9L, submission(11L), principal));
    verifyNoInteractions(answers);
  }
  @Test void submittedAttemptCannotBeSubmittedAgain() {
    authenticate();
    var attempt = new QuizAttempt(); attempt.setStudent(student); attempt.setQuiz(quiz); attempt.setSubmittedAt(LocalDateTime.now());
    when(attempts.lockById(9L)).thenReturn(Optional.of(attempt));
    assertThrows(IllegalArgumentException.class, () -> service.submit(9L, submission(11L), principal));
    verifyNoInteractions(answers);
  }
  @Test void maximumAttemptsIsEnforced() {
    authenticate(); quiz.setMaxAttempts(1);
    when(quizzes.lockById(4L)).thenReturn(Optional.of(quiz));
    var previous = new QuizAttempt(); previous.setSubmittedAt(LocalDateTime.now());
    when(attempts.findByStudent_IdAndQuiz_IdOrderByAttemptNumberDesc(7,4L)).thenReturn(List.of(previous));
    assertThrows(IllegalArgumentException.class, () -> service.start(4L,principal));
    verify(attempts, never()).saveAndFlush(any());
  }
  @Test void unfinishedAttemptResumesWithoutConsumingAnotherAttempt() {
    authenticate();
    when(quizzes.lockById(4L)).thenReturn(Optional.of(quiz));
    var previous = new QuizAttempt(); previous.setId(20L); previous.setQuiz(quiz); previous.setAttemptNumber(1);
    when(attempts.findByStudent_IdAndQuiz_IdOrderByAttemptNumberDesc(7,4L)).thenReturn(List.of(previous));
    when(questions.findByQuiz_IdOrderByDisplayOrderAsc(4L)).thenReturn(List.of());
    assertEquals(20L,service.start(4L,principal).id());
    verify(attempts,never()).saveAndFlush(any());
  }
  @Test void reviewSchedulesFreeStudentWithoutEnrollment() {
    authenticate();
    var card = new Flashcard(); card.setId(5L); card.setContent(content); card.setStatus("ACTIVE");
    when(cards.findById(5L)).thenReturn(Optional.of(card));
    var review = new FlashcardReview(); review.setRepetitionCount(2);
    when(reviews.findByStudent_IdAndFlashcard_Id(7,5L)).thenReturn(Optional.of(review));
    service.review(5L,new LearningRequest.Review("REMEMBERED"),principal);
    assertEquals(3,review.getRepetitionCount()); assertEquals(7,review.getIntervalDays());
    assertEquals(review.getLastReviewedAt().plusDays(7),review.getNextReviewAt());
    verifyNoInteractions(enrollments);
  }
  @Test void cannotChangeQuestionsOnceAttemptExists() {
    quiz.setStatus("DRAFT");
    when(quizzes.lockById(4L)).thenReturn(Optional.of(quiz));
    when(attempts.existsByQuiz_Id(4L)).thenReturn(true);
    assertThrows(IllegalArgumentException.class, () -> service.saveQuestion(4L,null,null));
    verifyNoInteractions(options);
  }
  @Test void studentSubmissionPersistsServerScoreAndAnswers() {
    authenticate();
    var q = new QuizQuestion(); q.setId(10L); q.setQuiz(quiz); q.setPoints(BigDecimal.ONE);
    var option = new QuizOption(); option.setId(11L); option.setQuestion(q); option.setCorrect(true);
    when(questions.findByQuiz_IdOrderByDisplayOrderAsc(4L)).thenReturn(List.of(q));
    when(options.findByQuestion_IdOrderByDisplayOrderAsc(10L)).thenReturn(List.of(option));
    var attempt = new QuizAttempt(); attempt.setId(9L); attempt.setStudent(student); attempt.setQuiz(quiz);
    when(attempts.lockById(9L)).thenReturn(Optional.of(attempt));
    var result = service.submit(9L, submission(11L), principal);
    assertEquals(new BigDecimal("100.00"), result.score());
    assertEquals(result.score(), attempt.getScore());
    assertTrue(attempt.getPassed()); assertNotNull(attempt.getSubmittedAt());
    verify(answers).saveAll(argThat(values -> {
      var iterator = values.iterator();
      var answer = iterator.next();
      return answer.getAttempt() == attempt && answer.isCorrect()
          && answer.getSelectedOption() == option && !iterator.hasNext();
    }));
    verify(attempts).save(attempt);
  }
  @Test void trueFalseRequiresExactlyTwoChoices() {
    quiz.setStatus("DRAFT");
    when(quizzes.lockById(4L)).thenReturn(Optional.of(quiz));
    var input = new LearningRequest.Question("Đúng không?", "TRUE_FALSE", null, BigDecimal.ONE,
        List.of(new LearningRequest.Option("Đúng",true),new LearningRequest.Option("Sai",false),new LearningRequest.Option("Khác",false)));
    assertThrows(IllegalArgumentException.class, () -> service.saveQuestion(4L,null,input));
  }
  @Test void publishedQuizMustHaveQuestions() {
    when(quizzes.lockById(4L)).thenReturn(Optional.of(quiz));
    when(questions.findByQuiz_IdOrderByDisplayOrderAsc(4L)).thenReturn(List.of());
    assertThrows(IllegalArgumentException.class, () -> service.saveQuiz(null,4L,new LearningRequest.Quiz("Test",BigDecimal.TEN,null,"PUBLISHED")));
  }
}
