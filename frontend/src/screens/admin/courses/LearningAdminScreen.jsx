import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { authApis, endpoints } from "../../../configs/Apis";
import { apiData, apiError } from "../../../utils/api";
import "../../../styles/Learning.css";

const newCard = () => ({ frontText: "", backText: "", exampleSentence: "", status: "ACTIVE" });
const newQuiz = () => ({ title: "", passingScore: 50, maxAttempts: "", status: "DRAFT" });
const newQuestion = () => ({ questionText: "", questionType: "SINGLE_CHOICE", explanation: "", points: 1,
  options: [{ optionText: "", correct: true }, { optionText: "", correct: false }] });

export default function LearningAdminScreen() {
  const { contentId, courseId } = useParams();
  return <LearningEditor key={contentId} contentId={contentId} courseId={courseId} />;
}

function LearningEditor({ contentId, courseId }) {
  const [title, setTitle] = useState("");
  const [cards, setCards] = useState([]);
  const [quizzes, setQuizzes] = useState([]);
  const [cardForm, setCardForm] = useState(null);
  const [quizForm, setQuizForm] = useState(null);
  const [questionForm, setQuestionForm] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const selected = quizzes.find((q) => q.id === selectedId);
  const load = useCallback(async () => {
    const client = authApis();
    const [content, c, q] = await Promise.all([
      client.get(endpoints["admin-content-details"](contentId)),
      client.get(endpoints["admin-learning-cards"](contentId)),
      client.get(endpoints["admin-learning-quizzes"](contentId)),
    ]);
    setTitle(apiData(content).title); setCards(apiData(c)); setQuizzes(apiData(q));
  }, [contentId]);
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load().catch((e) => setError(apiError(e))).finally(() => setLoading(false));
  }, [load]);
  async function mutate(action, done = () => {}) {
    setBusy(true); setError("");
    try { await action(); done(); await load(); }
    catch (e) { setError(apiError(e)); }
    finally { setBusy(false); }
  }
  function saveCard(event) {
    event.preventDefault();
    mutate(() => cardForm.id
      ? authApis().put(endpoints["admin-learning-card"](cardForm.id), cardForm)
      : authApis().post(endpoints["admin-learning-cards"](contentId), cardForm), () => setCardForm(null));
  }
  function saveQuiz(event) {
    event.preventDefault();
    const value = { ...quizForm, passingScore: Number(quizForm.passingScore), maxAttempts: quizForm.maxAttempts === "" ? null : Number(quizForm.maxAttempts) };
    mutate(() => quizForm.id
      ? authApis().put(endpoints["admin-learning-quiz"](quizForm.id), value)
      : authApis().post(endpoints["admin-learning-quizzes"](contentId), value), () => setQuizForm(null));
  }
  function saveQuestion(event) {
    event.preventDefault();
    const value = { ...questionForm, points: Number(questionForm.points) };
    mutate(() => questionForm.id
      ? authApis().put(endpoints["admin-learning-question"](questionForm.id), value)
      : authApis().post(endpoints["admin-learning-questions"](selectedId), value), () => setQuestionForm(null));
  }
  function move(index, offset) {
    const ids = cards.map((c) => c.id);
    [ids[index], ids[index + offset]] = [ids[index + offset], ids[index]];
    mutate(() => authApis().put(endpoints["admin-learning-order"](contentId), { ids }));
  }
  function changeOption(index, text) {
    setQuestionForm((current) => ({ ...current, options: current.options.map((o, i) => i === index ? { ...o, optionText: text } : o) }));
  }
  return <main className="learning-admin">
    <Link to={`/admin/courses/${courseId}/curriculum`}>← Giáo trình khóa học</Link>
    <h1>Flashcard và quiz</h1><p>{title}</p>
    {error && <p role="alert" className="public-alert">{error}</p>}
    {loading && <p role="status">Đang tải...</p>}
    <fieldset disabled={busy || loading} className="learning-editor-body">
      <section className="panel">
        <div className="learning-actions"><h2>Flashcard văn bản</h2><button type="button" onClick={() => setCardForm(newCard())}>Thêm flashcard</button></div>
        {!cards.length && <p>Chưa có flashcard.</p>}
        {cards.map((card, index) => <article className="learning-quiz-item" key={card.id}>
          <div><strong>{card.frontText}</strong><p>{card.backText}</p><small>{card.status}</small></div>
          <div className="learning-actions">
            <button type="button" aria-label={`Đưa thẻ ${index + 1} lên`} disabled={index === 0} onClick={() => move(index, -1)}>↑</button>
            <button type="button" aria-label={`Đưa thẻ ${index + 1} xuống`} disabled={index === cards.length - 1} onClick={() => move(index, 1)}>↓</button>
            <button type="button" onClick={() => setCardForm({ ...card })}>Sửa thẻ</button>
            <button type="button" disabled={card.status === "INACTIVE"} onClick={() => mutate(() => authApis().delete(endpoints["admin-learning-card"](card.id)))}>Ngừng sử dụng</button>
          </div>
        </article>)}
        {cardForm && <form onSubmit={saveCard} className="learning-form">
          <h3>{cardForm.id ? "Sửa flashcard" : "Flashcard mới"}</h3>
          <label>Mặt trước<input required maxLength={500} value={cardForm.frontText} onChange={(e) => setCardForm({ ...cardForm, frontText: e.target.value })} /></label>
          <label>Mặt sau<textarea required maxLength={16000} value={cardForm.backText} onChange={(e) => setCardForm({ ...cardForm, backText: e.target.value })} /></label>
          <label>Câu ví dụ<textarea maxLength={16000} value={cardForm.exampleSentence || ""} onChange={(e) => setCardForm({ ...cardForm, exampleSentence: e.target.value })} /></label>
          <label>Trạng thái thẻ<select value={cardForm.status} onChange={(e) => setCardForm({ ...cardForm, status: e.target.value })}><option value="ACTIVE">Đang sử dụng</option><option value="INACTIVE">Ngừng sử dụng</option></select></label>
          <div className="learning-actions"><button type="submit">Lưu flashcard</button><button type="button" onClick={() => setCardForm(null)}>Hủy</button></div>
        </form>}
      </section>
      <section className="panel">
        <div className="learning-actions"><h2>Quiz</h2><button type="button" onClick={() => setQuizForm(newQuiz())}>Thêm quiz</button></div>
        {!quizzes.length && <p>Chưa có quiz. Tạo bản nháp rồi thêm câu hỏi để xuất bản.</p>}
        {quizzes.map((quiz) => <article className="learning-quiz-item" key={quiz.id}>
          <div><strong>{quiz.title}</strong><p>{quiz.questions.length} câu · {quiz.status}</p></div>
          <div className="learning-actions">
            <button type="button" onClick={() => { setSelectedId(quiz.id); setQuestionForm(null); }}>Câu hỏi</button>
            <button type="button" onClick={() => setQuizForm({ ...quiz, maxAttempts: quiz.maxAttempts ?? "" })}>Cài đặt quiz</button>
            <button type="button" disabled={quiz.status === "ARCHIVED"} onClick={() => mutate(() => authApis().delete(endpoints["admin-learning-quiz"](quiz.id)))}>Lưu trữ</button>
          </div>
        </article>)}
        {quizForm && <form className="learning-form" onSubmit={saveQuiz}>
          <h3>{quizForm.id ? "Cài đặt quiz" : "Quiz mới"}</h3>
          <label>Tên quiz<input required maxLength={255} value={quizForm.title} onChange={(e) => setQuizForm({ ...quizForm, title: e.target.value })} /></label>
          <label>Điểm đạt (0–100)<input type="number" required min={0} max={100} step="0.01" disabled={quizForm.locked} value={quizForm.passingScore} onChange={(e) => setQuizForm({ ...quizForm, passingScore: e.target.value })} /></label>
          <label>Số lần tối đa (để trống nếu không giới hạn)<input type="number" min={1} max={1000} disabled={quizForm.locked} value={quizForm.maxAttempts} onChange={(e) => setQuizForm({ ...quizForm, maxAttempts: e.target.value })} /></label>
          <label>Trạng thái quiz<select value={quizForm.status} onChange={(e) => setQuizForm({ ...quizForm, status: e.target.value })}>
            <option value="DRAFT">Nháp</option><option value="PUBLISHED" disabled={!quizForm.id}>Xuất bản</option><option value="ARCHIVED">Lưu trữ</option>
          </select></label>
          <div className="learning-actions"><button type="submit">Lưu quiz</button><button type="button" onClick={() => setQuizForm(null)}>Hủy</button></div>
        </form>}
        {selected && <section className="learning-question-editor">
          <h3>Câu hỏi: {selected.title}</h3>
          {(selected.locked || selected.status !== "DRAFT") && <p>Chỉ chỉnh sửa câu hỏi khi quiz là bản nháp chưa có lượt làm. Quiz có lịch sử cần được giữ nguyên.</p>}
          <button type="button" disabled={selected.locked || selected.status !== "DRAFT"} onClick={() => setQuestionForm(newQuestion())}>Thêm câu hỏi</button>
          {selected.questions.map((q, index) => <article key={q.id} className="learning-quiz-item">
            <div><strong>{index + 1}. {q.questionText}</strong>{q.options.map((o) => <p key={o.id}>{o.correct ? "✓ " : ""}{o.optionText}</p>)}</div>
            <div className="learning-actions">
              <button type="button" disabled={selected.locked || selected.status !== "DRAFT"} onClick={() => setQuestionForm({ ...q, options: q.options.map((o) => ({ ...o })) })}>Sửa câu hỏi</button>
              <button type="button" disabled={selected.locked || selected.status !== "DRAFT"} onClick={() => {
                if (window.confirm("Xóa câu hỏi khỏi quiz nháp?")) mutate(() => authApis().delete(endpoints["admin-learning-question"](q.id)), () => setQuestionForm(null));
              }}>Xóa câu hỏi</button>
            </div>
          </article>)}
          {questionForm && <form className="learning-form" onSubmit={saveQuestion}>
            <label>Nội dung câu hỏi<textarea required maxLength={16000} value={questionForm.questionText} onChange={(e) => setQuestionForm({ ...questionForm, questionText: e.target.value })} /></label>
            <label>Loại câu hỏi<select value={questionForm.questionType} onChange={(e) => setQuestionForm({ ...questionForm, questionType: e.target.value,
              options: e.target.value === "TRUE_FALSE" ? [{ optionText: "Đúng", correct: true }, { optionText: "Sai", correct: false }] : questionForm.options })}>
              <option value="SINGLE_CHOICE">Một đáp án</option><option value="TRUE_FALSE">Đúng / Sai</option>
            </select></label>
            <label>Điểm câu hỏi<input type="number" required min="0.01" max="9999.99" step="0.01" value={questionForm.points} onChange={(e) => setQuestionForm({ ...questionForm, points: e.target.value })} /></label>
            <p>Chọn nút tròn tại đáp án đúng:</p>
            {questionForm.options.map((option, index) => <div className="learning-option-editor" key={index}>
              <input type="radio" name="correct-option" aria-label={`Đáp án đúng ${index + 1}`} checked={option.correct}
                onChange={() => setQuestionForm({ ...questionForm, options: questionForm.options.map((o, i) => ({ ...o, correct: i === index })) })} />
              <input required maxLength={4000} aria-label={`Lựa chọn ${index + 1}`} value={option.optionText} onChange={(e) => changeOption(index, e.target.value)} />
              <button type="button" disabled={questionForm.options.length <= 2 || questionForm.questionType === "TRUE_FALSE"} onClick={() =>
                setQuestionForm({ ...questionForm, options: questionForm.options.filter((_, i) => i !== index).map((o, i) => ({ ...o, correct: option.correct ? i === 0 : o.correct })) })}>Bỏ</button>
            </div>)}
            <button type="button" disabled={questionForm.questionType === "TRUE_FALSE" || questionForm.options.length >= 10}
              onClick={() => setQuestionForm({ ...questionForm, options: [...questionForm.options, { optionText: "", correct: false }] })}>Thêm lựa chọn</button>
            <label>Giải thích<textarea maxLength={16000} value={questionForm.explanation || ""} onChange={(e) => setQuestionForm({ ...questionForm, explanation: e.target.value })} /></label>
            <div className="learning-actions"><button type="submit">Lưu câu hỏi</button><button type="button" onClick={() => setQuestionForm(null)}>Hủy</button></div>
          </form>}
        </section>}
      </section>
    </fieldset>
  </main>;
}
