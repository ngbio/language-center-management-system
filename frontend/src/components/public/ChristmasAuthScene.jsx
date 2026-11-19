import { isChristmas } from "../../configs/uiEvent";
import ChristmasScene from "./ChristmasScene";

export default function ChristmasAuthScene() {
  if (!isChristmas) return null;
  const snow = localStorage.getItem("christmasSnow") !== "off";
  return <div className={`christmas-auth-scene ${snow ? "christmas-snow-on" : ""}`}><ChristmasScene /><p className="christmas-auth-wish">Merry Christmas · Giáng sinh an lành</p></div>;
}
