import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './styles/global.css'
import './styles/typography-glitch.css'
import App from './App.tsx'

window.addEventListener('gm:push-received', (evt) => {
  const detail = (evt as CustomEvent<{ title?: string; body?: string; url?: string }>).detail;
  if (!detail) return;
  if (Notification.permission !== 'granted') return;

  const n = new Notification(detail.title || '알림', { body: detail.body || '' });
  n.onclick = () => {
    const target = detail.url || '/';
    window.focus();
    window.location.href = target;
  };
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
