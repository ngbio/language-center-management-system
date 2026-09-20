package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.request.LearningRequest;
import com.ntt.language_center_management.dto.response.LearningResponse;
import java.security.Principal;
import java.util.List;

public interface LearningService {
  LearningResponse.Course course(Integer id, Principal principal);
  List<LearningResponse.Card> cards(Integer contentId, Principal principal);
  List<LearningResponse.Card> due(Principal principal);
  LearningResponse.Card review(Long id, LearningRequest.Review request, Principal principal);
  List<LearningResponse.Quiz> quizzes(Integer contentId, Principal principal);
  LearningResponse.Result evaluate(Long quizId, LearningRequest.Submission request);
  LearningResponse.Attempt start(Long quizId, Principal principal);
  LearningResponse.Result submit(Long attemptId, LearningRequest.Submission request, Principal principal);
  List<LearningResponse.Attempt> history(Long quizId, Principal principal);
  List<LearningResponse.Card> adminCards(Integer contentId);
  LearningResponse.Card saveCard(Integer contentId, Long id, LearningRequest.Card request);
  void deactivateCard(Long id);
  void reorderCards(Integer contentId, LearningRequest.Order request);
  List<LearningResponse.AdminQuiz> adminQuizzes(Integer contentId);
  LearningResponse.AdminQuiz saveQuiz(Integer contentId, Long id, LearningRequest.Quiz request);
  void archiveQuiz(Long id);
  LearningResponse.AdminQuiz saveQuestion(Long quizId, Long id, LearningRequest.Question request);
  void deleteQuestion(Long id);
}
