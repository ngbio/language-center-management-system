import { useRef, useState } from "react";
import { authApis, endpoints } from "../configs/Apis";
import { apiData, apiError } from "../utils/api";
import "../styles/ImageUpload.css";

export default function ImageUploadField({ label, value, onChange, purpose, wide = false }) {
  const inputRef = useRef(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");

  const upload = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    setUploading(true);
    setError("");
    try {
      const body = new FormData();
      body.append("file", file);
      const response = await authApis().post(endpoints["upload-image"], body, {
        params: { purpose },
      });
      onChange(apiData(response)?.url || "");
    } catch (requestError) {
      setError(apiError(requestError));
    } finally {
      setUploading(false);
    }
  };

  return <div className={`image-upload-field ${wide ? "field-wide" : ""}`}>
    <label>{label}<input type="url" value={value || ""} onChange={(event) => onChange(event.target.value)} maxLength={500} placeholder="https://..." /></label>
    <div className="image-upload-controls">
      <div className="image-upload-preview">{value ? <img src={value} alt={`Xem trước ${label.toLowerCase()}`} /> : <span>Chưa có ảnh</span>}</div>
      <button type="button" className="secondary-button" disabled={uploading} onClick={() => inputRef.current?.click()}>{uploading ? "Đang tải..." : "Tải ảnh từ máy"}</button>
      <input ref={inputRef} className="image-file-input" type="file" accept="image/jpeg,image/png,image/webp,image/gif" onChange={upload} />
    </div>
    {error && <small className="image-upload-error">{error}</small>}
  </div>;
}
