import { useNavigate } from "react-router-dom";
import AuthService from "../../../services/AuthService";
import './Register.css';
import { Pages, RANDOM_AVATAR_URL, RANDOM_CHAT_AVATAR_URL } from "../../../util/Constants";

export default function Register({ onRegister }: { onRegister?: (result: object) => {} }) {
    const navigate = useNavigate();

    const handleSubmit = async (event: any) => {
        const formData = new FormData(event.currentTarget);
        event.preventDefault();
        const data = {
            'name': formData.get('name'),
            'surname': formData.get('surname'),
            'email': formData.get('email'),
            'password': formData.get('password'),
            'avatar': RANDOM_CHAT_AVATAR_URL + `/${100}/${100}`
        };
        const result = await AuthService.register(data);
        onRegister?.(result);
        navigate(Pages.LOGIN_PAGE, { replace: true });
    }

    return (
        <div className="register-wrapper">
        <h1>Register</h1>
        <form
            method="POST"
            onSubmit={handleSubmit}
        >
            <label htmlFor="name">
                <p>Username</p>
                <input name="name" type="text" required={true} onChange={e => { return }}></input>
            </label>
            <label htmlFor="surname">
                <p>Surname</p>
                <input name="surname" type="text" required={true} onChange={e => { return }}></input>
            </label>
            <label htmlFor="email">
                <p>Email</p>
                <input name="email" type="email" required={true} onChange={e => { return }}></input>
            </label>
            <label htmlFor="password">
                <p>Password</p>
                <input name="password" type="password" required={true} onChange={e => { return }}></input>
            </label>

            <div className="margin-top-m">
                <button type="submit">Register</button>
            </div>
        </form>
    </div>
    )
}