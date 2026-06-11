import React, { useEffect, useState } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { SystemNotificationDTO } from '../types/dtos';
import { useAuth } from '../hooks/useAuth';

const NotificationCenter: React.FC = () => {
  const { currentUser } = useAuth();
  const [notifications, setNotifications] = useState<SystemNotificationDTO[]>([]);

  useEffect(() => {
    if (!currentUser) return;

    const socket = new SockJS(import.meta.env.VITE_WEBSOCKET_URL || 'http://localhost:8000/ws');
    const client = new Client({
      webSocketFactory: () => socket,
      onConnect: () => {
        client.subscribe(`/user/${currentUser.id}/queue/notifications`, (msg) => {
          setNotifications(prev => [JSON.parse(msg.body), ...prev]);
        });
        client.subscribe('/topic/global-notifications', (msg) => {
          setNotifications(prev => [JSON.parse(msg.body), ...prev]);
        });
      },
    });

    client.activate();
    return () => { client.deactivate(); };
  }, [currentUser]);

  return (
    <div className="notification-panel">
      {notifications.map(n => (
        <div key={n.id} className={`alert alert-${n.type.toLowerCase()}`}>
          {n.message}
        </div>
      ))}
    </div>
  );
};
export default NotificationCenter;