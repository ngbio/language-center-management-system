import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import APIs, { endpoints } from "../../configs/Apis";
import { apiData } from "../../utils/api";
import ChristmasScene from "../../components/public/ChristmasScene";
import { isChristmas } from "../../configs/uiEvent";
import ScrollReveal from "../../components/public/ScrollReveal";

export default function HomeScreen() {
  const [teachers, setTeachers] = useState([]);
  const [languages, setLanguages] = useState([]);

  useEffect(() => {
    let active = true;
    APIs.get(endpoints.teachers)
      .then((response) => {
        if (active) setTeachers((apiData(response) || []).slice(0, 4));
      })
      .catch(() => {
        if (active) setTeachers([]);
      });
    APIs.get(endpoints.languages)
      .then((response) => {
        if (active) setLanguages(apiData(response) || []);
      })
      .catch(() => {
        if (active) setLanguages([]);
      });
    return () => { active = false; };
  }, []);

  return (
    <>
      <section className={`public-hero ${isChristmas ? "christmas-hero" : ""}`}>
        {isChristmas && <div className="christmas-snow" aria-hidden="true">{Array.from({ length: 16 }, (_, index) => <i key={index} style={{ "--flake": index }} />)}</div>}
        <div className="public-container hero-grid">
          <ScrollReveal className="hero-copy">
            {isChristmas ? <span className="eyebrow-pill christmas-eyebrow"><span aria-hidden="true">✦</span> MỘT MÙA GIÁNG SINH, NHIỀU KHỞI ĐẦU MỚI</span> : <span className="eyebrow-pill">LỘ TRÌNH HỌC THỰC TẾ</span>}
            <h1>Chạm gần hơn đến thế giới bằng một ngôn ngữ mới.</h1>
            <p>{isChristmas ? "Mùa lễ hội này, dành tặng bản thân một khởi đầu mới. Khám phá những ngôn ngữ, kết nối những câu chuyện và học thêm điều hay mỗi ngày." : "Khóa học có cấu trúc rõ ràng, nội dung theo từng bài và lịch học linh hoạt dành cho mọi trình độ."}</p>
            <div className="hero-actions">
              <Link className="primary-cta" to="/khoa-hoc">Khám phá khóa học</Link>
              <a className="text-cta" href="/downloads/lingua-center-1.0.0.apk" download>Tải ứng dụng Android <span>↓</span></a>
              <Link className="text-cta" to="/ngon-ngu">Xem các ngôn ngữ <span>→</span></Link>
            </div>
            <div className="hero-stats"><div><strong>25+</strong><span>Bài học chọn lọc</span></div><div><strong>{languages.length || "—"}</strong><span>Ngôn ngữ</span></div><div><strong>4.9/5</strong><span>Đánh giá học viên</span></div></div>
          </ScrollReveal>
          {isChristmas ? <ScrollReveal className="hero-visual christmas-visual" delay={160}>
            <ChristmasScene />
            <div className="christmas-language-note"><span aria-hidden="true">✦</span><div><small>MỘT LỜI CHÚC, NHIỀU NGÔN NGỮ</small><strong>Merry Christmas!</strong><span>メリークリスマス · Giáng sinh an lành</span></div></div>
          </ScrollReveal> : <ScrollReveal className="hero-visual" delay={160} aria-label="Minh họa học ngoại ngữ"><div className="sun-disc" /><div className="hero-card hero-card-main"><span>日本語</span><strong>Học từng bài.<br />Tiến bộ mỗi ngày.</strong><small>Trung tâm ngoại ngữ</small></div><div className="hero-card hero-card-float"><b>あ</b><span>New lesson<br /><strong>ready to learn</strong></span></div><div className="hero-dots">••••••••••••</div></ScrollReveal>}
        </div>
      </section>

      <section className="benefits-section" id="benefits">
        <div className="public-container">
          <ScrollReveal className="section-heading centered"><div><span className="section-kicker">HỌC CÓ ĐỊNH HƯỚNG</span><h2>Một hành trình vừa sức, dễ theo dõi</h2></div></ScrollReveal>
          <div className="benefit-grid">
            {[
              ["Nội dung theo từng bài", "Mở từng phần và học theo nhịp độ phù hợp với bạn."],
              ["Lộ trình rõ ràng", "Biết mình sẽ học gì và đang tiến tới đâu."],
              ["Linh hoạt mọi thiết bị", "Học thuận tiện trên máy tính, máy tính bảng và điện thoại."],
            ].map(([title, description], index) => (
              <ScrollReveal as="article" key={title} delay={index * 100}>
                <span className="benefit-number" aria-hidden="true">0{index + 1}</span>
                <svg className="benefit-check" viewBox="0 0 32 32" fill="none" aria-hidden="true"><circle cx="14" cy="17" r="11" stroke="currentColor" strokeWidth="2" /><path d="m8 16 6 6L28 6" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" /></svg>
                <h3>{title}</h3><p>{description}</p>
              </ScrollReveal>
            ))}
          </div>
        </div>
      </section>

      {languages.length > 0 && (
        <section className="home-languages-section">
          <div className="public-container">
            <ScrollReveal className="section-heading">
              <div><span className="section-kicker">NGÔN NGỮ ĐÀO TẠO</span><h2>Các ngôn ngữ trung tâm đang giảng dạy</h2></div>
              <p>Chọn ngôn ngữ bạn quan tâm để xem các khóa học và trình độ đang mở.</p>
            </ScrollReveal>
            <div className="language-card-grid">
              {languages.map((language, index) => (
                <ScrollReveal as={Link} className="language-card" to={`/khoa-hoc?languageId=${language.id}`} key={language.id} delay={(index % 3) * 100}>
                  <span>{language.languageCode}</span>
                  <h2>{language.languageName}</h2>
                  <p>{language.description || "Khám phá các chương trình đào tạo và lộ trình phù hợp."}</p>
                  <strong>Xem khóa học →</strong>
                </ScrollReveal>
              ))}
            </div>
          </div>
        </section>
      )}

      {teachers.length > 0 && (
        <section className="teacher-showcase">
          <div className="public-container">
            <ScrollReveal className="section-heading">
              <div><span className="section-kicker">ĐỒNG HÀNH CÙNG BẠN</span><h2>Đội ngũ giảng viên chuyên nghiệp</h2></div>
              <p>Các giảng viên đang hoạt động tại trung tâm, có chuyên môn rõ ràng và kinh nghiệm giảng dạy thực tế.</p>
            </ScrollReveal>
            <div className="teacher-showcase-grid">
              {teachers.map((teacher, index) => (
                <ScrollReveal as="article" key={teacher.id} delay={index * 100}>
                  <div className={`teacher-portrait teacher-portrait-${(index % 4) + 1}`} aria-hidden="true">
                    {teacher.fullName?.split(" ").filter(Boolean).slice(-2).map((part) => part.charAt(0)).join("") || "GV"}
                  </div>
                  <small>{teacher.teacherCode}</small>
                  <h3>{teacher.fullName}</h3>
                  <p>{teacher.specialization || "Giảng viên ngoại ngữ"}</p>
                  <span>{teacher.degree || "Chuyên môn đang cập nhật"}</span>
                </ScrollReveal>
              ))}
            </div>
          </div>
        </section>
      )}
    </>
  );
}
