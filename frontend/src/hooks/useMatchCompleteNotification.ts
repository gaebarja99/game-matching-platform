import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getWsUrl } from '../api/client';
import { clearRandomMatchPending } from '../utils/randomMatchPendingStorage';

/**
 * 구독 /topic/user/{userId} — 일반 랜덤 매칭 완료 시 매칭 채팅으로 이동.
 */
export function useMatchCompleteNotification(userId: number | undefined) {
  const navigate = useNavigate();
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (userId == null) return;
    const client = new Client({
      webSocketFactory: () => new SockJS(getWsUrl()) as unknown as WebSocket,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/topic/user/' + userId, (message) => {
          if (!message?.body) return;
          try {
            const d = JSON.parse(message.body) as { type?: string; sessionId?: number };
            if ((d.type === 'MATCH_COMPLETE' || d.type === 'MATCH_COMPLETE_LOL') && d.sessionId != null) {
              clearRandomMatchPending();
              navigate('/match-chat/' + d.sessionId);
            }
          } catch {
            /* ignore */
          }
        });
      },
    });
    client.activate();
    clientRef.current = client;
    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [userId, navigate]);
}
