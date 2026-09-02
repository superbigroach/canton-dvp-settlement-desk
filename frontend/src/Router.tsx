// Route table for /desk (basename). Sections per docs/PRODUCT-PLAN.md §2.
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import App from './App';
import Login from './pages/Login';
import Shell, { Home } from './shell/Shell';
import Proposals from './pages/sign/Proposals';
import History from './pages/sign/History';
import Settings from './pages/sign/Settings';
import Funds from './pages/ap/Funds';
import FundDetail from './pages/ap/FundDetail';
import Receipts from './pages/ap/Receipts';
import FundDashboard from './pages/fund/Dashboard';
import Schedule from './pages/admin/Schedule';
import Committees from './pages/admin/Committees';
import Users from './pages/admin/Users';
import Events from './pages/admin/Events';
import Fallback from './pages/admin/Fallback';
import AuditEvents from './pages/audit/AuditEvents';
import AuditSeries from './pages/audit/AuditSeries';

function RouteError() {
  return (
    <div className="card" style={{ margin: 24 }}>
      <h2>Something went wrong on this page</h2>
      <p className="hint">The desk itself is fine. Reload, or go back to <a href="/desk/">the desk</a>.</p>
    </div>
  );
}

const router = createBrowserRouter(
  [
    { path: '/login', element: <Login />, errorElement: <RouteError /> },
    {
      path: '/',
      element: <Shell />,
      errorElement: <RouteError />,
      children: [
        { index: true, element: <Home /> },
        { path: 'sign', element: <Proposals /> },
        { path: 'sign/history', element: <History /> },
        { path: 'sign/settings', element: <Settings /> },
        { path: 'ap', element: <Funds /> },
        { path: 'ap/funds/:id', element: <FundDetail /> },
        { path: 'ap/receipts', element: <Receipts /> },
        { path: 'fund', element: <FundDashboard /> },
        { path: 'fund/:id', element: <FundDashboard /> },
        { path: 'admin', element: <Schedule /> },
        { path: 'admin/committees', element: <Committees /> },
        { path: 'admin/users', element: <Users /> },
        { path: 'admin/events', element: <Events /> },
        { path: 'admin/fallback', element: <Fallback /> },
        { path: 'audit', element: <AuditEvents /> },
        { path: 'audit/series', element: <AuditSeries /> },
        { path: 'ops', element: <App /> },
        { path: '*', element: <Home /> },
      ],
    },
  ],
  { basename: '/desk' },
);

export default function Router() {
  return <RouterProvider router={router} />;
}
