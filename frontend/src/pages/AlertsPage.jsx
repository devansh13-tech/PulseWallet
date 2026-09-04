import { useEffect, useState } from 'react';
import { notificationApi } from '../lib/api';

function formatDateTime(value) {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '—';
    return new Intl.DateTimeFormat('en-US', {
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
    }).format(date);
}

export default function AlertsPage() {
    const [alerts, setAlerts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [unreadCount, setUnreadCount] = useState(0);

    async function loadAlerts() {
        setLoading(true);
        setError('');

        try {
            const [notificationsResult, unreadCountResult] = await Promise.all([
                notificationApi.list(),
                notificationApi.unreadCount(),
            ]);

            const nextAlerts = Array.isArray(notificationsResult) ? notificationsResult : [];
            setAlerts(nextAlerts);
            setUnreadCount(Number(unreadCountResult ?? 0));
        } catch (err) {
            setError(err?.message || 'Unable to load notifications. Try again.');
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadAlerts();
        const timer = window.setInterval(loadAlerts, 30000);
        return () => window.clearInterval(timer);
    }, []);

    async function handleMarkRead(notificationId) {
        try {
            await notificationApi.markRead(notificationId);
            setAlerts((current) => current.map((item) => item.id === notificationId ? { ...item, read: true } : item));
            setUnreadCount((current) => Math.max(0, current - 1));
        } catch (err) {
            setError(err?.message || 'Unable to update notification.');
        }
    }

    return (
        <div className="page-shell">
            <div className="page-header">
                <div>
                    <p className="eyebrow">PulseWallet</p>
                    <h1>Alerts & notifications</h1>
                </div>
                <button type="button" className="secondary-btn" onClick={loadAlerts}>Refresh</button>
            </div>

            <div className="page-card">
                <div className="alert-summary">
                    <span>{unreadCount} unread</span>
                </div>

                {error && <div className="inline-error" role="alert">{error}</div>}

                {loading ? (
                    <div className="loader-row">Loading notifications…</div>
                ) : alerts.length === 0 ? (
                    <div className="empty-card">No fraud alerts.</div>
                ) : (
                    <ul className="alert-list">
                        {alerts.map((alert) => (
                            <li key={alert.id ?? `${alert.title}-${alert.createdAt}`}>
                                <div className={`severity-dot ${alert.read ? 'low' : 'medium'}`} aria-hidden="true" />
                                <div className="alert-copy">
                                    <strong>{alert.title || 'Alert'}</strong>
                                    <span>{alert.message || 'No message provided.'}</span>
                                    <small>{formatDateTime(alert.createdAt)} • {alert.read ? 'Read' : 'Unread'}</small>
                                </div>
                                {!alert.read && (
                                    <button type="button" className="mini-btn" onClick={() => handleMarkRead(alert.id)}>Mark read</button>
                                )}
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </div>
    );
}
