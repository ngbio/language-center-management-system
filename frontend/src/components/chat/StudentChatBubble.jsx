import { useEffect, useRef, useState } from "react";
import { SESSION_KEYS } from "../../utils/authSession";
import { apiError } from "../../utils/api";
import {
  connectFirebaseChat,
  ensureStudentConversation,
  markConversationRead,
  sendChatMessage,
  subscribeMessages,
} from "../../services/firebaseChat";
import "../../styles/Chat.css";

export default function StudentChatBubble() {
  const [open, setOpen] = useState(false);
  const [session, setSession] = useState(null);
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState("");
  const [error, setError] = useState("");
  const [sending, setSending] = useState(false);
  const messagesRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
    let unsubscribe;
    let active = true;
    (async () => {
      try {
        setError("");
        const connected = await connectFirebaseChat();
        await ensureStudentConversation(
          connected,
          localStorage.getItem(SESSION_KEYS.email) || "",
        );
        if (!active) return;
        setSession(connected);
        unsubscribe = subscribeMessages(connected, connected.identity.uid, setMessages);
        await markConversationRead(connected, connected.identity.uid);
      } catch (chatError) {
        if (active) setError(apiError(chatError));
      }
    })();
    return () => {
      active = false;
      unsubscribe?.();
    };
  }, [open]);

  useEffect(() => {
    const container = messagesRef.current;
    if (container) container.scrollTop = container.scrollHeight;
  }, [messages]);

  const submit = async (event) => {
    event.preventDefault();
    if (!session || !text.trim()) return;
    try {
      setSending(true);
      await sendChatMessage(session, session.identity.uid, text);
      setText("");
    } catch (chatError) {
      setError(apiError(chatError));
    } finally {
      setSending(false);
    }
  };

  return (
    <aside className={`student-chat ${open ? "open" : ""}`}>
      {open && (
        <section className="chat-window" aria-label="Chat với nhân viên tư vấn">
          <header>
            <div><strong>Tư vấn trực tuyến</strong><small>{session?.identity.consultantName || "Đang kết nối..."}</small></div>
            <button type="button" onClick={() => setOpen(false)} aria-label="Đóng chat">×</button>
          </header>
          <div className="chat-messages" ref={messagesRef}>
            {!error && !session && <p className="chat-state">Đang kết nối...</p>}
            {error && <p className="chat-error">{error}</p>}
            {session && messages.length === 0 && <p className="chat-state">Hãy gửi câu hỏi, nhân viên tư vấn sẽ phản hồi tại đây.</p>}
            {messages.map((message) => (
              <article key={message.id} className={message.senderUid === session?.identity.uid ? "mine" : "theirs"}>
                <small>{message.senderName}</small><p>{message.text}</p>
              </article>
            ))}
          </div>
          <form onSubmit={submit}>
            <textarea value={text} maxLength={2000} rows={2} placeholder="Nhập nội dung cần tư vấn..." onChange={(event) => setText(event.target.value)} disabled={!session || sending} />
            <button type="submit" disabled={!session || sending || !text.trim()}>Gửi</button>
          </form>
        </section>
      )}
      <button className="chat-fab" type="button" onClick={() => setOpen((value) => !value)} aria-label="Mở chat tư vấn">
        {open ? "×" : "Chat"}
      </button>
    </aside>
  );
}
