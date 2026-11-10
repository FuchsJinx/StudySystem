import { Link, Route, Routes } from "react-router-dom";
import { Layout } from "./Layout";
import { PublicOnly, RequireAuth, RequireRole } from "./guards";
import { LoginPage } from "../pages/auth/LoginPage";
import { RegisterStudentPage } from "../pages/auth/RegisterStudentPage";
import { RegisterTeacherPage } from "../pages/auth/RegisterTeacherPage";
import { VerifyEmailPage } from "../pages/auth/VerifyEmailPage";
import { DashboardPage } from "../pages/DashboardPage";
import { ProfilePage } from "../pages/ProfilePage";
import { MembershipPage } from "../pages/MembershipPage";
import { StudentTasksPage } from "../pages/StudentTasksPage";
import { TeacherCheckpointsPage } from "../pages/TeacherCheckpointsPage";
import { TeacherCheckpointDetailPage } from "../pages/TeacherCheckpointDetailPage";
import { AwardFeedPage } from "../pages/AwardFeedPage";
import { ActivitiesPage } from "../pages/ActivitiesPage";
import { TeacherActivityDetailPage } from "../pages/TeacherActivityDetailPage";
import { BudgetPage } from "../pages/BudgetPage";
import { PollsPage } from "../pages/PollsPage";
import { PollCreatePage } from "../pages/PollCreatePage";
import { PollDetailPage } from "../pages/PollDetailPage";
import { PersonalFeedPage } from "../pages/PersonalFeedPage";
import { AdminPage } from "../pages/AdminPage";

function NotFoundPage() {
  return (
    <div className="auth-simple">
      <h1>404</h1>
      <p>Страница не найдена.</p>
      <Link className="button button--primary" to="/">
        На главную
      </Link>
    </div>
  );
}

export function App() {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <PublicOnly>
            <LoginPage />
          </PublicOnly>
        }
      />
      <Route
        path="/register"
        element={
          <PublicOnly>
            <RegisterStudentPage />
          </PublicOnly>
        }
      />
      <Route
        path="/register/teacher"
        element={
          <PublicOnly>
            <RegisterTeacherPage />
          </PublicOnly>
        }
      />
      <Route path="/verify-email" element={<VerifyEmailPage />} />

      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          <Route index element={<DashboardPage />} />
          <Route path="/feed" element={<AwardFeedPage />} />
          <Route path="/my-feed" element={<PersonalFeedPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/users/:id" element={<ProfilePage />} />
          <Route element={<RequireRole roles={["ADMIN"]} />}>
            <Route path="/admin" element={<AdminPage />} />
          </Route>
          <Route element={<RequireRole roles={["STUDENT", "TEACHER"]} />}>
            <Route path="/activities" element={<ActivitiesPage />} />
            <Route path="/polls" element={<PollsPage />} />
            <Route path="/polls/:id" element={<PollDetailPage />} />
          </Route>
          <Route element={<RequireRole roles={["TEACHER"]} />}>
            <Route path="/activities/:id" element={<TeacherActivityDetailPage />} />
            <Route path="/polls/new" element={<PollCreatePage />} />
            <Route path="/budget" element={<BudgetPage />} />
          </Route>
          <Route element={<RequireRole roles={["STUDENT"]} />}>
            <Route path="/tasks" element={<StudentTasksPage />} />
          </Route>
          <Route element={<RequireRole roles={["TEACHER", "ADMIN"]} />}>
            <Route path="/checkpoints" element={<TeacherCheckpointsPage />} />
            <Route path="/checkpoints/:id" element={<TeacherCheckpointDetailPage />} />
          </Route>
          <Route element={<RequireRole roles={["TEACHER", "ADMIN", "STUDENT"]} />}>
            <Route path="/membership" element={<MembershipPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
