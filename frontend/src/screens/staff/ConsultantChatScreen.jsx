import { useEffect, useRef, useState } from "react";
import { apiError } from "../../utils/api";
import {
  connectFirebaseChat,
  markConversationRead,
  sendChatMessage,
  subscribeConsultantConversations,
  subscribeMessages,
} from "../../services/firebaseChat";
import "../../styles/Chat.css";

export default function ConsultantChatScreen() {
  const [session, setSession] = useState(null);
  const [conversations, setConversations] = useState([]);
  const [selected, setSelected] = useState(null);
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState("");
  const [error, setError] = useState("");
  const [sending, setSending] = useState(false);
  const messagesRef = useRef(null);

  useEffect(() => {
    let unsubscribe;
    let active = true;
    connectFirebaseChat()
      .then((connected) => {
        if (!active) return;
        setSession(connected);
        unsubscribe = subscribeConsultantConversations(connected, setConversations);
      })
      .catch((chatError) => active && setError(apiError(chatError)));
    return () => { active = false; unsubscribe?.(); };
  }, []);

  useEffect(() => {
    if (!session || !selected) return undefined;
    const unsubscribe = subscribeMessages(session, selected.studentUid, setMessages);
    markConversationRead(session, selected.studentUid).catch((chatError) => setError(apiError(chatError)));
    return unsubscribe;
  }, [session, selected]);

  useEffect(() => {
    const container = messagesRef.current;
    if (container) container.scrollTop = container.scrollHeight;
  }, [messages]);

  const submit = async (event) => {
    event.preventDefault();
    if (!session || !selected || !text.trim() || sending) return;
    try {
      setSending(true);
      setError("");
      await sendChatMessage(session, selected.studentUid, text);
      setText("");
    } catch (chatError) {
      setError(apiError(chatError));
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="consultant-chat-page">
      <div className="page-title"><div><span>HỖ TRỢ REALTIME</span><h1>Tin nhắn học viên</h1><p>Mỗi học viên chỉ được kết nối với nhân viên tư vấn đã được hệ thống phân công.</p></div></div>
      {error && <div className="message error">{error}</div>}
      <section className="consultant-chat-panel panel">
        <aside className="conversation-list">
          <header><strong>Cuộc trò chuyện</strong><span>{conversations.length}</span></header>
          {!session && !error && <p>Đang kết nối Firebase...</p>}
          {session && conversations.length === 0 && <p>Chưa có học viên gửi tin nhắn.</p>}
          {conversations.map((conversation) => (
            <button key={conversation.studentUid} type="button" className={selected?.studentUid === conversation.studentUid ? "active" : ""} onClick={() => setSelected(conversation)}>
              <span>{conversation.studentName?.charAt(0)?.toUpperCase() || "H"}</span>
              <div><strong>{conversation.studentName}</strong><small>{conversation.lastMessage || conversation.studentEmail}</small></div>
              {conversation.unreadConsultant > 0 && <b>{conversation.unreadConsultant}</b>}
            </button>
          ))}
        </aside>
        <div className="consultant-thread">
          {!selected && <p className="chat-state">Chọn một học viên để bắt đầu hỗ trợ.</p>}
          {selected && <>
            <header><strong>{selected.studentName}</strong><small>{selected.studentEmail}</small></header>
            <div className="chat-messages" ref={messagesRef}>
              {messages.map((message) => <article key={message.id} className={message.senderUid === session?.identity.uid ? "mine" : "theirs"}><small>{message.senderName}</small><p>{message.text}</p></article>)}
            </div>
            <form onSubmit={submit}><textarea rows={2} maxLength={2000} value={text} placeholder="Nhập câu trả lời..." onChange={(event) => setText(event.target.value)} disabled={sending} /><button type="submit" disabled={sending || !text.trim()}>{sending ? "Đang gửi..." : "Gửi"}</button></form>
          </>}
        </div>
      </section>
    </div>
  );
}
