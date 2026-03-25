import { useCallback, useEffect, useRef, useState } from 'react';
import { apiUrl } from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import { filterConversationsExcludingSelf, filterFriendsExcludingSelf, isDmWithSelf } from '../utils/dmSelf';

export type FriendRow = {
  id: number;
  loginId: string;
  nickname: string;
  profileImageUrl?: string | null;
};

export type ConvRow = {
  otherUserId: number;
  nickname: string;
  loginId: string;
  profileImageUrl?: string;
  lastMessage: string;
  lastMessageAt: string;
  unreadCount?: number;
};

export type DmMsg = {
  id?: number;
  fromUserId: number;
  toUserId: number;
  text: string;
  createdAt?: string;
};

export type LeftTab = 'conversations' | 'friends';

export function useDirectMessages(options?: { enabled?: boolean }) {
  const enabled = options?.enabled !== false;
  const { user, loading: authLoading } = useAuth();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const lastMessagesSigRef = useRef<string>('');

  const [leftTab, setLeftTab] = useState<LeftTab>('conversations');
  const [conversations, setConversations] = useState<ConvRow[]>([]);
  const [convLoading, setConvLoading] = useState(false);
  const [friends, setFriends] = useState<FriendRow[]>([]);
  const [friendsLoading, setFriendsLoading] = useState(false);

  const [selected, setSelected] = useState<FriendRow | null>(null);
  const [messages, setMessages] = useState<DmMsg[]>([]);
  const [msgLoading, setMsgLoading] = useState(false);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);

  const loadConversations = useCallback(() => {
    if (!user) return;
    setConvLoading(true);
    fetch(apiUrl('api/dm/conversations?size=50'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { list: [] }))
      .then((d: { list?: ConvRow[] }) => {
        const list = Array.isArray(d.list) ? d.list : [];
        setConversations(filterConversationsExcludingSelf(list, user.id));
      })
      .catch(() => setConversations([]))
      .finally(() => setConvLoading(false));
  }, [user]);

  const loadFriends = useCallback(() => {
    if (!user) return;
    setFriendsLoading(true);
    fetch(apiUrl('api/friends/list'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : []))
      .then((list: FriendRow[]) => {
        const arr = Array.isArray(list) ? list : [];
        setFriends(filterFriendsExcludingSelf(arr, user.id));
      })
      .catch(() => setFriends([]))
      .finally(() => setFriendsLoading(false));
  }, [user]);

  const loadMessages = useCallback((withUserId: number, opts?: { silent?: boolean }) => {
    if (user && isDmWithSelf(user.id, withUserId)) {
      setMessages([]);
      setMsgLoading(false);
      return;
    }
    const silent = opts?.silent === true;
    if (!silent) setMsgLoading(true);
    fetch(apiUrl(`api/dm?withUserId=${withUserId}&limit=100`), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { items: [] }))
      .then((d: { items?: DmMsg[] }) => {
        const items = Array.isArray(d.items) ? d.items : [];
        setMessages(items.slice().reverse());
      })
      .catch(() => {
        if (!silent) setMessages([]);
      })
      .finally(() => {
        if (!silent) setMsgLoading(false);
      });
  }, [user]);

  useEffect(() => {
    if (!enabled || authLoading || !user) return;
    loadConversations();
    loadFriends();
  }, [enabled, authLoading, user, loadConversations, loadFriends]);

  useEffect(() => {
    if (!selected) return;
    lastMessagesSigRef.current = '';
    loadMessages(selected.id);
  }, [selected, loadMessages]);

  useEffect(() => {
    if (!selected || !user) return;
    const t = window.setInterval(() => loadMessages(selected.id, { silent: true }), 5000);
    return () => window.clearInterval(t);
  }, [selected, user, loadMessages]);

  /** 폴링으로 동일 목록이 오면 스크롤하지 않음 — 새 글·대화 전환 시에만 맨 아래로 */
  useEffect(() => {
    const last = messages[messages.length - 1];
    const sig = `${messages.length}:${last?.id ?? ''}:${last?.createdAt ?? ''}:${last?.text?.length ?? 0}`;
    if (sig === lastMessagesSigRef.current) return;
    lastMessagesSigRef.current = sig;
    if (messages.length === 0) return;
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    if (!selected) return;
    const fromUserId = selected.id;
    fetch(apiUrl(`api/notifications/read-dm-from?fromUserId=${fromUserId}`), { method: 'POST', credentials: 'include' })
      .then((r) => {
        if (r.ok) {
          loadConversations();
          window.dispatchEvent(new CustomEvent('gamematcher-dm-unread-changed'));
        }
      })
      .catch(() => {});
  }, [selected?.id, loadConversations]);

  const selectConversation = useCallback(
    (c: ConvRow) => {
      if (!user || isDmWithSelf(user.id, c.otherUserId)) return;
      setSelected({
        id: c.otherUserId,
        loginId: c.loginId || '',
        nickname: c.nickname || c.loginId || '유저',
        profileImageUrl: c.profileImageUrl,
      });
    },
    [user],
  );

  const selectFriend = useCallback(
    (f: FriendRow) => {
      if (!user || isDmWithSelf(user.id, f.id)) return;
      setSelected(f);
    },
    [user],
  );

  const resetSelection = useCallback(() => {
    setSelected(null);
    setInput('');
    setMessages([]);
  }, []);

  useEffect(() => {
    if (!user || !selected) return;
    if (isDmWithSelf(user.id, selected.id)) {
      setSelected(null);
      setInput('');
      setMessages([]);
    }
  }, [user, selected]);

  const handleSend = useCallback(() => {
    if (!user || !selected || sending) return;
    if (isDmWithSelf(user.id, selected.id)) return;
    const text = input.trim();
    if (!text) return;
    setSending(true);
    fetch(apiUrl('api/dm'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ toUserId: selected.id, text }),
    })
      .then((r) => r.json().then((d) => ({ ok: r.ok, data: d as DmMsg & { message?: string } })))
      .then((res) => {
        if (!res.ok) return;
        const sent: DmMsg = {
          id: res.data.id,
          fromUserId: user.id,
          toUserId: selected.id,
          text: res.data.text ?? text,
          createdAt: res.data.createdAt,
        };
        setMessages((prev) => [...prev, sent]);
        setInput('');
        loadConversations();
      })
      .finally(() => setSending(false));
  }, [user, selected, sending, input, loadConversations]);

  return {
    user,
    authLoading,
    messagesEndRef,
    leftTab,
    setLeftTab,
    conversations,
    convLoading,
    friends,
    friendsLoading,
    selected,
    setSelected,
    messages,
    msgLoading,
    input,
    setInput,
    sending,
    loadConversations,
    loadFriends,
    selectConversation,
    selectFriend,
    resetSelection,
    handleSend,
  };
}
