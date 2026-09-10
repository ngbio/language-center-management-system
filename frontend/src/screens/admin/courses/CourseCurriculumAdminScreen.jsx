import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { EmptyState, ErrorAlert, Modal, PageTitle, StatusBadge } from "../../../components/AdminUi";
import { authApis, endpoints } from "../../../configs/Apis";
import { apiData, apiError } from "../../../utils/api";

const emptySection = { title: "", description: "" };
const emptyContent = { title: "", summary: "", contentHtml: "", audioUrl: "", videoUrl: "", documentUrl: "", contentType: "LESSON", preview: false };

export default function CourseCurriculumAdminScreen() {
  const { courseId } = useParams();
  const [course, setCourse] = useState(null);
  const [sections, setSections] = useState([]);
  const [contents, setContents] = useState({});
  const [sectionForm, setSectionForm] = useState(null);
  const [contentForm, setContentForm] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadContents = useCallback(async (sectionId) => {
    const response = await authApis().get(endpoints["admin-section-contents"](sectionId));
    setContents((current) => ({ ...current, [sectionId]: apiData(response) || [] }));
  }, []);

  const load = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const api = authApis();
      const [courseResponse, sectionResponse] = await Promise.all([
        api.get(endpoints["admin-course-details"](courseId)),
        api.get(endpoints["admin-course-sections"](courseId)),
      ]);
      const values = apiData(sectionResponse) || [];
      setCourse(apiData(courseResponse)); setSections(values);
      const entries = await Promise.all(values.map(async (section) => {
        const response = await api.get(endpoints["admin-section-contents"](section.id));
        return [section.id, apiData(response) || []];
      }));
      setContents(Object.fromEntries(entries));
    } catch (requestError) { setError(apiError(requestError)); }
    finally { setLoading(false); }
  }, [courseId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const saveSection = async (event) => {
    event.preventDefault(); setError("");
    try {
      if (sectionForm.id) await authApis().put(endpoints["admin-section-details"](sectionForm.id), sectionForm);
      else await authApis().post(endpoints["admin-course-sections"](courseId), sectionForm);
      setSectionForm(null); await load();
    } catch (requestError) { setError(apiError(requestError)); }
  };

  const removeSection = async (section) => {
    if (!window.confirm(`Xóa phần “${section.title}” và toàn bộ nội dung bên trong?`)) return;
    try { await authApis().delete(endpoints["admin-section-details"](section.id)); await load(); }
    catch (requestError) { setError(apiError(requestError)); }
  };

  const moveSection = async (index, offset) => {
    const reordered = [...sections];
    [reordered[index], reordered[index + offset]] = [reordered[index + offset], reordered[index]];
    try {
      await authApis().patch(endpoints["admin-sections-reorder"], { ids: reordered.map((value) => value.id) });
      setSections(reordered.map((value, position) => ({ ...value, displayOrder: position + 1 })));
    } catch (requestError) { setError(apiError(requestError)); }
  };

  const saveContent = async (event) => {
    event.preventDefault(); setError("");
    try {
      if (contentForm.id) await authApis().put(endpoints["admin-content-details"](contentForm.id), contentForm);
      else await authApis().post(endpoints["admin-section-contents"](contentForm.sectionId), contentForm);
      const sectionId = contentForm.sectionId; setContentForm(null); await loadContents(sectionId);
    } catch (requestError) { setError(apiError(requestError)); }
  };

  const removeContent = async (content) => {
    if (!window.confirm(`Xóa nội dung “${content.title}”?`)) return;
    try { await authApis().delete(endpoints["admin-content-details"](content.id)); await loadContents(content.sectionId); }
    catch (requestError) { setError(apiError(requestError)); }
  };

  const publish = async (content, status) => {
    try {
      await authApis().patch(endpoints["admin-content-publication"](content.id), { status });
      await loadContents(content.sectionId);
    } catch (requestError) { setError(apiError(requestError)); }
  };

  const moveContent = async (sectionId, index, offset) => {
    const reordered = [...contents[sectionId]];
    [reordered[index], reordered[index + offset]] = [reordered[index + offset], reordered[index]];
    try {
      await authApis().patch(endpoints["admin-contents-reorder"], { ids: reordered.map((value) => value.id) });
      setContents((current) => ({ ...current, [sectionId]: reordered.map((value, position) => ({ ...value, displayOrder: position + 1 })) }));
    } catch (requestError) { setError(apiError(requestError)); }
  };

  return <>
    <PageTitle eyebrow="GIÁO TRÌNH KHÓA HỌC" title={course?.courseName || "Quản lý giáo trình"}
      description="Tạo, biên tập, xuất bản và sắp xếp nội dung khóa học"
      action={<div className="curriculum-title-actions"><Link className="secondary-button button-link" to="/admin/courses">← Khóa học</Link><button className="primary-button" onClick={() => setSectionForm({...emptySection})}>＋ Thêm phần</button></div>} />
    <ErrorAlert message={error} />
    {loading ? <section className="panel curriculum-loading">Đang tải giáo trình...</section> : !sections.length ? <section className="panel"><EmptyState message="Khóa học chưa có giáo trình" /></section> :
      <div className="curriculum-admin-list">{sections.map((section, sectionIndex) => <section className="panel curriculum-section" key={section.id}>
        <header><div><span>PHẦN {section.displayOrder}</span><h2>{section.title}</h2><p>{section.description || "Không có mô tả"}</p></div>
          <div className="row-actions"><button disabled={sectionIndex===0} onClick={() => moveSection(sectionIndex,-1)}>↑</button><button disabled={sectionIndex===sections.length-1} onClick={() => moveSection(sectionIndex,1)}>↓</button><button onClick={() => setSectionForm({...section})}>Sửa</button><button className="danger-link" onClick={() => removeSection(section)}>Xóa</button></div></header>
        <div className="curriculum-content-list">{(contents[section.id] || []).map((content,index) => <article key={content.id}>
          <span className="code-pill">{content.displayOrder}</span><div><strong>{content.title}</strong><small>{content.contentType} · {content.preview ? "Cho phép xem trước" : "Nội dung khóa học"}</small></div><StatusBadge value={content.publicationStatus}/>
          <div className="row-actions"><button disabled={index===0} onClick={() => moveContent(section.id,index,-1)}>↑</button><button disabled={index===(contents[section.id]?.length||0)-1} onClick={() => moveContent(section.id,index,1)}>↓</button><button onClick={() => setContentForm({...content})}>Sửa</button><button onClick={() => publish(content,content.publicationStatus==="PUBLISHED"?"DRAFT":"PUBLISHED")}>{content.publicationStatus==="PUBLISHED"?"Gỡ xuất bản":"Xuất bản"}</button><button className="danger-link" onClick={() => removeContent(content)}>Xóa</button></div>
        </article>)}</div>
        <button className="curriculum-add-content" onClick={() => setContentForm({...emptyContent,sectionId:section.id})}>＋ Thêm nội dung</button>
      </section>)}</div>}

    {sectionForm && <Modal title={sectionForm.id?"Cập nhật phần":"Thêm phần giáo trình"} onClose={() => setSectionForm(null)}><form onSubmit={saveSection}><div className="form-grid"><label>Tiêu đề<input required maxLength="255" value={sectionForm.title} onChange={(e)=>setSectionForm({...sectionForm,title:e.target.value})}/></label><label className="field-wide">Mô tả<textarea rows="3" maxLength="65535" value={sectionForm.description||""} onChange={(e)=>setSectionForm({...sectionForm,description:e.target.value})}/></label></div><div className="modal-actions"><button type="button" className="secondary-button" onClick={()=>setSectionForm(null)}>Hủy</button><button className="primary-button">Lưu</button></div></form></Modal>}
    {contentForm && <Modal title={contentForm.id?"Cập nhật nội dung":"Thêm nội dung"} onClose={() => setContentForm(null)}><form onSubmit={saveContent}><div className="form-grid">
      <label>Tiêu đề<input required maxLength="255" value={contentForm.title} onChange={(e)=>setContentForm({...contentForm,title:e.target.value})}/></label><label>Loại nội dung<select value={contentForm.contentType} onChange={(e)=>setContentForm({...contentForm,contentType:e.target.value})}>{["LESSON","VOCABULARY","GRAMMAR","LISTENING","EXERCISE"].map(value=><option key={value}>{value}</option>)}</select></label>
      <label className="field-wide">Tóm tắt<textarea rows="2" value={contentForm.summary||""} onChange={(e)=>setContentForm({...contentForm,summary:e.target.value})}/></label><label className="field-wide">Nội dung HTML<textarea rows="6" value={contentForm.contentHtml||""} onChange={(e)=>setContentForm({...contentForm,contentHtml:e.target.value})}/></label>
      <label>Audio URL<input type="url" maxLength="500" value={contentForm.audioUrl||""} onChange={(e)=>setContentForm({...contentForm,audioUrl:e.target.value})}/></label><label>Video URL<input type="url" maxLength="500" value={contentForm.videoUrl||""} onChange={(e)=>setContentForm({...contentForm,videoUrl:e.target.value})}/></label><label className="field-wide">Tài liệu URL<input type="url" maxLength="500" value={contentForm.documentUrl||""} onChange={(e)=>setContentForm({...contentForm,documentUrl:e.target.value})}/></label>
      <label className="featured-toggle"><input type="checkbox" checked={Boolean(contentForm.preview)} onChange={(e)=>setContentForm({...contentForm,preview:e.target.checked})}/><span className="featured-toggle-box">✓</span><span><strong>Cho phép xem trước</strong><small>Người chưa đăng ký có thể xem nội dung này</small></span></label>
    </div><div className="modal-actions"><button type="button" className="secondary-button" onClick={()=>setContentForm(null)}>Hủy</button><button className="primary-button">Lưu nội dung</button></div></form></Modal>}
  </>;
}
