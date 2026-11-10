import { useAuth } from "../shared/auth/AuthContext";
import { StudentActivitiesPage } from "./StudentActivitiesPage";
import { TeacherActivitiesPage } from "./TeacherActivitiesPage";

export function ActivitiesPage() {
  const { user } = useAuth();
  if (user?.role === "TEACHER") {
    return <TeacherActivitiesPage />;
  }
  return <StudentActivitiesPage />;
}
