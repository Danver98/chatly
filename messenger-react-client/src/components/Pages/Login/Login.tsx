import User from "../../../models/User";
import AuthService, { AuthData } from "../../../services/AuthService"
import { SecuredPages } from "../../../util/Constants";
import "./Login.css";
import { useNavigate } from "react-router-dom";
import { toast } from 'sonner';
import { useAuthStore } from "../../../stores/authStore";

export default function Login() {
    const navigate = useNavigate();

    const handleSubmit = async (event: any) => {
        const formData = new FormData(event.currentTarget);
        event.preventDefault();
        const data = {
            'email': formData.get('email'),
            'password': formData.get('password'),
        };
        const authData: AuthData = await AuthService.login(data);
        const user: User = authData?.user;
        if (user) {
            useAuthStore.getState().setCurrentLoggedUser(user);
            useAuthStore.getState().setAccessToken(authData.accessToken);
            navigate(SecuredPages.HOME_PAGE, {replace: true});
        } else {
            // Something's gone wrong. Show notification
            toast.error('Something went wrong. Please try again.');
        };
        // Check if success and navigate to '/home'
    }

    return (
        <div className="login-wrapper">
            <h1>Login</h1>
            <form
                method="POST"
                onSubmit={handleSubmit}
                >
                <label htmlFor="email">
                    <p>Email</p>
                    <input name="email" type="email" required={true} onChange={e => {return}}></input>
                </label>
                <label htmlFor="password">
                    <p>Password</p>
                    <input name="password" type="password" required={true} onChange={e => {return}}></input>
                </label>

                <div className="margin-top-m">
                    <button type="submit">Login</button>
                </div>
            </form>
        </div>
    )
}