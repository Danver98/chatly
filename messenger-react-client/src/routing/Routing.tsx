import { createBrowserRouter, RouterProvider, Navigate, Outlet } from "react-router-dom";
import Layout from "../components/Layout/Layout";
import Chats from "../components/Pages/Chats/Chats";
import Intro from "../components/Pages/Intro";
import Register from "../components/Pages/Register/Register";
import Login from "../components/Pages/Login/Login";
import Home from "../components/Pages/Home";
import Profile from "../components/Pages/Settings/Profile";
import Settings from "../components/Pages/Settings/Settings";
import { Pages, SecuredPages } from "../util/Constants";
import { useAuthStore } from "../stores/authStore";

// Require auth
function RequireAuth() {
    const accessToken = useAuthStore((s) => s.accessToken);
    if (!accessToken) return <Navigate to={Pages.LOGIN_PAGE} replace />;
    return <Outlet />;
}

// Redirect away from public pages if already logged in
function RedirectIfAuthed({ children }: { children: React.ReactNode }) {
    const accessToken = useAuthStore((s) => s.accessToken);
    if (accessToken) return <Navigate to={SecuredPages.HOME_PAGE} replace />;
    return <>{children}</>;
}

const router = createBrowserRouter([
    {
        path: "/",
        element: <Layout />,
        children: [
            { path: "/intro", element: <Intro /> },
            { path: "/login", element: <RedirectIfAuthed><Login /></RedirectIfAuthed> },
            { path: "/register", element: <RedirectIfAuthed><Register /></RedirectIfAuthed> },

            {
                path: SecuredPages.ROOT, 
                element: <RequireAuth />,
                children: [
                    { path: 'home', element: <Home /> },
                    { path: 'chats', element: <Chats /> },
                    { path: 'profile', element: <Profile /> },
                    { path: 'settings', element: <Settings /> },
                ],
            },
        ],
    },
]);

const Routing = () => <RouterProvider router={router} />;
export default Routing;