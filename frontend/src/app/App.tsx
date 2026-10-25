import { Link, Route, Routes } from "react-router-dom";
import { HomePage } from "../pages/HomePage";
import { HealthPage } from "../pages/HealthPage";

export function App() {
  return (
    <div className="shell">
      <header className="top">
        <Link to="/" className="brand">
          UniRate
        </Link>
        <nav>
          <Link to="/health">API health</Link>
        </nav>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/health" element={<HealthPage />} />
        </Routes>
      </main>
    </div>
  );
}
