import { isChristmas } from "../configs/uiEvent";

export default function SeasonalAccent() {
  if (!isChristmas) return null;
  return <div className="seasonal-accent"><span aria-hidden="true">✦</span><div><strong>Giáng sinh an lành</strong><small>Thêm tri thức, thêm niềm vui.</small></div><span aria-hidden="true">✧</span></div>;
}
