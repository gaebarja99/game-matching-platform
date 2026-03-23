import { Link } from 'react-router-dom';
import { springOrigin } from '../config';
import './AppNav.css';

export default function AppNav() {
  return (
    <header className="app-nav">
      <nav className="app-nav-links">
        <Link to="/">홈</Link>
        <a href={`${springOrigin}/community/index.html`}>커뮤니티</a>
        <Link to="/profile">프로필 검색</Link>
      </nav>
    </header>
  );
}
