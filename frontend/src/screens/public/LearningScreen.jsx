import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api, { authApis, endpoints } from "../../configs/Apis";
import { apiData, apiError, formatDateTime } from "../../utils/api";
import { SESSION_KEYS, isTokenActive } from "../../utils/authSession";
import "../../styles/Learning.css";

export default function LearningScreen() {
  const { courseId } = useParams();
  const student = localStorage.getItem(SESSION_KEYS.role) === "STUDENT"
    && isTokenActive(localStorage.getItem(SESSION_KEYS.token));
  return <LearningCourse key={`${courseId}-${student}`} courseId={courseId} student={student} />;
}

function LearningCourse({ courseId, student }) {
  const [course, setCourse] = useState(null);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    (student ? authApis() : api).get(endpoints["learning-course"](courseId, student))
      .then((r) => {
        if (!active) return;
        const value = apiData(r);
        setCourse(value);
        setSelected(value.sections.flatMap((s) => s.contents)[0] || null);
      }).catch((e) => { if (active) setError(apiError(e)); });
    return () => { active = false; };
  }, [courseId, student]);
  return <main className="learning-page public-container">
    <Link to="/khoa-hoc">← Danh sách khóa học</Link>
    <h1>{course?.title || "Ôn tập khóa học"}</h1>
    {!student && <p className="learning-notice">Bạn đang học ở chế độ khách. <Link to="/login">Đăng nhập học viên</Link> để lưu lịch ôn flashcard và kết quả quiz.</p>}
    {error && <p role="alert" className="public-alert">{error}</p>}
    {!course && !error && <p role="status">Đang tải nội dung...</p>}
    {course && <div className="learning-layout">
      <nav className="learning-sidebar" aria-label="Danh sách bài học">
        {course.sections.map((section) => <section key={section.id}>
          <h2>{section.title}</h2>
          {section.contents.length === 0 && <p>Chưa có bài được xuất bản.</p>}
          {section.contents.map((content) => <button key={content.id} type="button"
            aria-current={selected?.id === content.id ? "page" : undefined}
            onClick={() => setSelected(content)}>{content.title}</button>)}
        </section>)}
      </nav>
      {selected ? <LessonPractice key={selected.id} content={selected} student={student} />
        : <p>Khóa học chưa có nội dung được xuất bản.</p>}
    </div>}
  </main>;
}

function LessonPractice({ content, student }) {
  const [tab, setTab] = useState("lesson");
  const [cards, setCards] = useState([]);
  const [quizzes, setQuizzes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    const client = student ? authApis() : api;
    Promise.all([
      client.get(endpoints["learning-cards"](content.id, student)),
      client.get(endpoints["learning-quizzes"](content.id, student)),
    ]).then(([c, q]) => {
      if (active) { setCards(apiData(c)); setQuizzes(apiData(q)); }
    }).catch((e) => { if (active) setError(apiError(e)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [content.id, student]);
  const documentUrl = /^https?:\/\//i.test(content.documentUrl || "") ? content.documentUrl : null;
  // A sandbox without allow-scripts or allow-same-origin isolates authored HTML.
  const srcDoc = '<!doctype html><html lang="vi"><head><meta name="viewport" content="width=device-width,initial-scale=1"><meta http-equiv="Content-Security-Policy" content="default-src \'none\'; style-src \'unsafe-inline\'; img-src https: data:"><style>body{font:16px/1.7 system-ui;padding:12px;color:#253749;overflow-wrap:anywhere}img,table{max-width:100%}pre{white-space:pre-wrap}</style></head><body>'
    + (content.contentHtml || "") + "</body></html>";
  return <section className="learning-main">
    <h2>{content.title}</h2>
    <div className="learning-tabs" aria-label="Nội dung học tập">
      {[["lesson", "Ôn tập bài"], ["cards", "Flashcard"], ["quiz", "Quiz"]].map(([key, label]) =>
        <button key={key} type="button" aria-pressed={tab === key} onClick={() => setTab(key)}>{label}</button>)}
    </div>
    {tab === "lesson" && <article>
      {content.summary && <p>{content.summary}</p>}
      {content.contentHtml ? <iframe title={content.title} className="learning-content-frame" sandbox="" srcDoc={srcDoc} />
        : <p>Bài học chưa có nội dung chi tiết.</p>}
      {documentUrl && <a href={documentUrl} target="_blank" rel="noopener noreferrer">Mở tài liệu bài học ↗</a>}
    </article>}
    {tab !== "lesson" && error && <p role="alert">{error}</p>}
    {tab !== "lesson" && loading && <p role="status">Đang tải bài tập...</p>}
    {!loading && !error && tab === "cards" && <Flashcards cards={cards} student={student} />}
    {!loading && !error && tab === "quiz" && <QuizPractice quizzes={quizzes} student={student} />}
  </section>;
}

function Flashcards({ cards, student }) {
  const [index, setIndex] = useState(0);
  const [flipped, setFlipped] = useState(false);
  const [dueOnly, setDueOnly] = useState(false);
  const [reviews, setReviews] = useState({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const available = cards.map((c) => reviews[c.id] || c).filter((c) =>
    !dueOnly || !c.nextReviewAt || new Date(c.nextReviewAt) <= new Date());
  const position = available.length ? index % available.length : 0;
  const card = available[position];
  async function rate(masteryLevel) {
    setBusy(true); setError("");
    try {
      if (student) {
        const response = await authApis().put(endpoints["learning-review"](card.id), { masteryLevel });
        const updated = apiData(response);
        setReviews((current) => ({ ...current, [card.id]: updated }));
        setNotice(`Lần ôn tiếp theo: ${formatDateTime(updated.nextReviewAt)}`);
      } else setNotice("Đã xem thẻ. Đăng nhập để lưu lịch ôn.");
      setFlipped(false);
      if (!dueOnly || !student) setIndex(position + 1);
    } catch (e) { setError(apiError(e)); } finally { setBusy(false); }
  }
  return <div className="learning-cards">
    <label className="learning-check"><input type="checkbox" checked={dueOnly} disabled={busy}
      onChange={(e) => { setDueOnly(e.target.checked); setIndex(0); setFlipped(false); }} />Chỉ thẻ mới và đến hạn</label>
    {notice && <p role="status">{notice}</p>}
    {error && <p role="alert">{error}</p>}
    {!card ? <p>{cards.length ? "Không còn thẻ cần ôn hôm nay." : "Bài này chưa có flashcard."}</p> : <>
      <p>Thẻ {position + 1} / {available.length}</p>
      <div className="learning-flashcard">
        <p className="learning-card-text">{flipped ? card.backText : card.frontText}</p>
        {flipped && card.exampleSentence && <p>{card.exampleSentence}</p>}
        {!flipped && <button type="button" onClick={() => setFlipped(true)}>Xem đáp án</button>}
      </div>
      {flipped && <div className="learning-actions">
        {[["AGAIN", "Chưa nhớ"], ["HARD", "Khó"], ["REMEMBERED", "Đã nhớ"]].map(([value, label]) =>
          <button type="button" disabled={busy} key={value} onClick={() => rate(value)}>{label}</button>)}
      </div>}
      <div className="learning-actions">
        <button type="button" disabled={busy} onClick={() => { setIndex((position - 1 + available.length) % available.length); setFlipped(false); }}>← Thẻ trước</button>
        <button type="button" disabled={busy} onClick={() => { setIndex(position + 1); setFlipped(false); }}>Thẻ tiếp →</button>
      </div>
    </>}
  </div>;
}

function QuizPractice({ quizzes, student }) {
  const [active, setActive] = useState(null);
  const [attempt, setAttempt] = useState(null);
  const [selected, setSelected] = useState({});
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function begin(quiz) {
    setBusy(true); setError(""); setSelected({}); setResult(null); setHistory([]); setActive(null);
    try {
      if (student) {
        const response = await authApis().post(endpoints["learning-attempts"](quiz.id));
        const value = apiData(response);
        setAttempt(value.id); setActive(value.quiz);
      } else { setAttempt(null); setActive(quiz); }
    } catch (e) { setError(apiError(e)); } finally { setBusy(false); }
  }
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError("");
    const answers = active.questions.map((q) => ({ questionId: q.id, selectedOptionId: selected[q.id] || null }));
    try {
      const response = student
        ? await authApis().post(endpoints["learning-submit"](attempt), { answers })
        : await api.post(endpoints["learning-evaluate"](active.id), { answers });
      setResult(apiData(response));
    } catch (e) { setError(apiError(e)); } finally { setBusy(false); }
  }
  async function loadHistory(quiz) {
    setBusy(true); setError(""); setActive(null); setResult(null);
    try {
      const response = await authApis().get(endpoints["learning-attempts"](quiz.id));
      setHistory(apiData(response));
      if (!apiData(response).length) setError("Bạn chưa làm quiz này.");
    } catch (e) { setError(apiError(e)); } finally { setBusy(false); }
  }
  return <div className="learning-quizzes">
    {!quizzes.length && <p>Bài này chưa có quiz được xuất bản.</p>}
    {quizzes.map((quiz) => <div className="learning-quiz-item" key={quiz.id}>
      <div><strong>{quiz.title}</strong><p>{quiz.questions.length} câu · Điểm đạt {quiz.passingScore}/100
        {student && quiz.maxAttempts ? ` · Tối đa ${quiz.maxAttempts} lần` : ""}</p></div>
      <div className="learning-actions"><button disabled={busy} type="button" onClick={() => begin(quiz)}>Làm quiz</button>
        {student && <button disabled={busy} type="button" onClick={() => loadHistory(quiz)}>Lịch sử</button>}</div>
    </div>)}
    {error && <p role="alert">{error}</p>}
    {busy && <p role="status">Đang xử lý...</p>}
    {history.map((item) => <button className="learning-history" type="button" key={item.id} disabled={!item.result || busy}
      onClick={() => { setActive(item.quiz); setResult(item.result); }}>
      Lần {item.attemptNumber} · {item.submittedAt ? `${item.score}/100 · ${formatDateTime(item.submittedAt)}` : "Chưa nộp — chọn Làm quiz để tiếp tục"}
    </button>)}
    {active && <form onSubmit={submit}>
      <h3>{active.title}</h3>
      {result && <p className="learning-result" role="status">{result.passed ? "Đạt" : "Chưa đạt"} · {result.score}/100</p>}
      {active.questions.map((question, index) => {
        const answer = result?.answers.find((a) => a.questionId === question.id);
        return <fieldset key={question.id} disabled={busy || Boolean(result)}>
          <legend>{index + 1}. {question.questionText} ({question.points} điểm)</legend>
          {question.options.map((option) => <label className="learning-option" key={option.id}>
            <input type="radio" name={`question-${question.id}`} value={option.id}
              checked={(answer ? answer.selectedOptionId : selected[question.id]) === option.id}
              onChange={() => setSelected((current) => ({ ...current, [question.id]: option.id }))} />
            <span>{option.optionText}{answer?.correctOptionId === option.id ? " ✓ Đáp án đúng" : ""}</span>
          </label>)}
          {answer && <p>{answer.correct ? "Chính xác." : "Chưa chính xác."} {answer.explanation}</p>}
        </fieldset>;
      })}
      {!result && <button type="submit" disabled={busy}>Nộp bài</button>}
    </form>}
  </div>;
}
