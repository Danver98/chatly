import { Link } from "react-router-dom";
import "./Sidebar.css"
import { useAuthStore } from "../../../stores/authStore";

export default function Sidebar() {
    const accessToken = useAuthStore((state) => state.accessToken);
    return (
        <>
            <nav className="sidebar">
                {
                    accessToken &&
                    <ul>
                        <li>
                            <Link to="/intro">Intro</Link>
                        </li>
                        <li>
                            <Link to="/secured/">Home</Link>
                        </li>
                        <li>
                            <Link to="/secured/chats">Chats</Link>
                        </li>
                    </ul>
                }
                {
                    !accessToken &&
                    <ul>
                        <li>
                            <Link to="/intro">Intro</Link>
                        </li>
                        <li>
                            <Link to="/login">Login</Link>
                        </li>
                        <li>
                            <Link to="/register">Register</Link>
                        </li>
                    </ul>
                }
            </nav>
        </>
    )
}