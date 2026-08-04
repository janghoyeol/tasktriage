import { Link, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Layout() {
  const { logout } = useAuth();

  return (
    <div className="app-shell">
      <nav className="app-nav">
        <span className="app-nav__brand">TaskTriage</span>
        <Link to="/tasks">작업 목록</Link>
        <Link to="/dashboard">대시보드</Link>
        <button type="button" onClick={logout}>
          로그아웃
        </button>
      </nav>
      <main className="app-content">
        <Outlet />
      </main>
    </div>
  );
}
