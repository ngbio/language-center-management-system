import { useRef, useState } from "react";
import windowFrame from "../../assets/christmas/window-frame-desktop.svg";
import christmasMusic from "../../assets/christmas/jingle-bells-orchestra.mp3";

export default function ChristmasScene() {
  const audioRef = useRef(null);
  const [playing, setPlaying] = useState(false);
  const [error, setError] = useState("");

  const toggleMusic = async () => {
    const audio = audioRef.current;
    if (!audio) return;
    setError("");
    if (!audio.paused) {
      audio.pause();
      return;
    }
    try {
      audio.volume = 0.35;
      await audio.play();
    } catch {
      setError("Chưa thể phát nhạc. Bạn thử lại nhé.");
    }
  };

  return (
    <div className="christmas-scene">
      <div className="christmas-window" role="img" aria-label="Khung cửa Giáng sinh nhìn ra khu vườn tuyết với cây thông, quà tặng và những ngôi nhà ấm áp">
        <div className="christmas-window-garden" />
        <div className="christmas-window-snow" aria-hidden="true">
          {Array.from({ length: 16 }, (_, index) => <i key={index} style={{ "--flake": index }} />)}
        </div>
        <img className="christmas-window-frame" src={windowFrame} width="680" height="600" alt="" />
      </div>
      <div className="christmas-music">
        <audio ref={audioRef} src={christmasMusic} preload="none" loop onPlay={() => setPlaying(true)} onPause={() => setPlaying(false)} onError={() => { setPlaying(false); setError("Chưa thể tải nhạc. Bạn thử lại nhé."); }} />
        <button type="button" onClick={toggleMusic} aria-pressed={playing} aria-label={playing ? "Tắt nhạc Giáng sinh" : "Bật nhạc Giáng sinh"}>
          <span aria-hidden="true">{playing ? "Ⅱ" : "♫"}</span>
          {playing ? "Tắt nhạc" : "Bật nhạc Giáng sinh"}
        </button>
        <span className="christmas-music-status" role="status">{error || "Jingle Bells · Một chút ấm áp mùa đông"}</span>
      </div>
    </div>
  );
}
