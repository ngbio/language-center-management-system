import { useCallback, useEffect, useRef, useState } from "react";
import { authApis, endpoints } from "../../configs/Apis";
import { apiData, formatDateTime } from "../../utils/api";

const POLL_INTERVAL_MS = 60_000;

export default function StudentNotificationBell() {
  const panelRef = useRef(null);
  const [open, setOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);

  const loadUnreadCount = useCallback(async () => {
    try {
      const response = await authApis().get(endpoints["unread-notification-count"]);
      setUnreadCount(apiData(response)?.unreadCount || 0);
    } catch {
      // A transient notification error must not interrupt the rest of the student portal.
    }
  }, []);

  const loadNotifications = useCallback(async () => {
    setLoading(true);
    try {
      const response = await authApis().get(endpoints["my-notifications"], {
        params: { page: 0, size: 20 },
      });
      setItems(apiData(response)?.content || []);
    } catch {
      setItems([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const initialLoadId = window.setTimeout(loadUnreadCount, 0);
    const intervalId = window.setInterval(() => {
      if (document.visibilityState === "visible") loadUnreadCount();
    }, POLL_INTERVAL_MS);
    const handleFocus = () => loadUnreadCount();
    window.addEventListener("focus", handleFocus);
    return () => {
      window.clearTimeout(initialLoadId);
      window.clearInterval(intervalId);
      window.removeEventListener("focus", handleFocus);
    };
  }, [loadUnreadCount]);

  useEffect(() => {
    if (!open) return undefined;
    const initialLoadId = window.setTimeout(loadNotifications, 0);
    const close = (event) => {
      if (!panelRef.current?.contains(event.target)) setOpen(false);
    };
    document.addEventListener("mousedown", close);
    return () => {
      window.clearTimeout(initialLoadId);
      document.removeEventListener("mousedown", close);
    };
  }, [open, loadNotifications]);

  const markRead = async (notification) => {
    if (notification.read) return;
    await authApis().patch(endpoints["read-notification"](notification.id));
    setItems((current) => current.map((item) =>
      item.id === notification.id ? { ...item, read: true } : item));
    setUnreadCount((current) => Math.max(0, current - 1));
  };

  const markAllRead = async () => {
    await authApis().patch(endpoints["read-all-notifications"]);
    setItems((current) => current.map((item) => ({ ...item, read: true })));
    setUnreadCount(0);
  };

  return (
    <div className="student-notifications" ref={panelRef}>
      <button
        className="notification-trigger"
        type="button"
        aria-label="Thông báo"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
      >
        <span aria-hidden="true">🔔</span>
        {unreadCount > 0 && <strong>{unreadCount > 99 ? "99+" : unreadCount}</strong>}
      </button>
      {open && (
        <section className="notification-panel" aria-label="Danh sách thông báo">
          <header>
            <div><strong>Thông báo</strong><small>{unreadCount} chưa đọc</small></div>
            {unreadCount > 0 && <button type="button" onClick={markAllRead}>Đọc tất cả</button>}
          </header>
          <div className="notification-list">
            {loading && <p className="notification-empty">Đang tải thông báo...</p>}
            {!loading && !items.length && <p className="notification-empty">Bạn chưa có thông báo.</p>}
            {!loading && items.map((item) => (
              <button
                type="button"
                className={item.read ? "notification-item" : "notification-item unread"}
                key={item.id}
                onClick={() => markRead(item)}
              >
                <span className="notification-dot" />
                <span><strong>{item.title}</strong><small>{item.content}</small><time>{formatDateTime(item.createdAt)}</time></span>
              </button>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
