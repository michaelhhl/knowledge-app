import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import { QaPage } from './pages/QaPage';
import { KnowledgePage } from './pages/KnowledgePage';
import { IndexBanner } from './components/IndexBanner';

export function App() {
  return (
    <div className="app">
      <a className="skip-link" href="#main">
        跳到主要内容
      </a>
      <header className="app-header">
        <h1 className="brand">知识问答</h1>
        <nav className="primary-nav" aria-label="主导航">
          <NavLink to="/" end className="nav-link">
            问答
          </NavLink>
          <NavLink to="/knowledge" className="nav-link">
            知识库
          </NavLink>
        </nav>
      </header>
      <IndexBanner />
      <main id="main" className="app-main">
        <Routes>
          <Route path="/" element={<QaPage />} />
          <Route path="/knowledge/*" element={<KnowledgePage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
