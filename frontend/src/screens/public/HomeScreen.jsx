import { Link } from "react-router-dom";

export default function HomeScreen() {
  return (
    <>
      <section className="public-hero">
        <div className="public-container hero-grid">
          <div className="hero-copy">
            <span className="eyebrow-pill">LỘ TRÌNH HỌC THỰC TẾ</span>
            <h1>Chạm gần hơn đến thế giới bằng một ngôn ngữ mới.</h1>
            <p>Khóa học có cấu trúc rõ ràng, nội dung theo từng bài và lịch học linh hoạt dành cho mọi trình độ.</p>
            <div className="hero-actions"><Link className="primary-cta" to="/khoa-hoc">Khám phá khóa học</Link><Link className="text-cta" to="/ngon-ngu">Xem các ngôn ngữ <span>→</span></Link></div>
            <div className="hero-stats"><div><strong>25+</strong><span>Bài học chọn lọc</span></div><div><strong>3</strong><span>Ngôn ngữ</span></div><div><strong>4.9/5</strong><span>Đánh giá học viên</span></div></div>
          </div>
          <div className="hero-visual" aria-label="Minh họa học ngoại ngữ">
            <div className="sun-disc" />
            <div className="hero-card hero-card-main"><span>日本語</span><strong>Học từng bài.<br />Tiến bộ mỗi ngày.</strong><small>Trung tâm ngoại ngữ</small></div>
            <div className="hero-card hero-card-float"><b>あ</b><span>New lesson<br /><strong>ready to learn</strong></span></div>
            <div className="hero-dots">••••••••••••</div>
          </div>
        </div>
      </section>

      <section className="benefits-section" id="benefits">
        <div className="public-container"><div className="section-heading centered"><div><span className="section-kicker">HỌC CÓ ĐỊNH HƯỚNG</span><h2>Một hành trình vừa sức, dễ theo dõi</h2></div></div><div className="benefit-grid"><article><span>01</span><h3>Nội dung theo từng bài</h3><p>Mở section và chỉ tải nội dung khi bạn cần xem.</p></article><article><span>02</span><h3>Lộ trình rõ ràng</h3><p>Biết mình sẽ học gì và đang tiến tới đâu.</p></article><article><span>03</span><h3>Linh hoạt mọi thiết bị</h3><p>Giao diện tối ưu cho máy tính, máy tính bảng và điện thoại.</p></article></div></div>
      </section>
    </>
  );
}
