import UserInfo from "./UserInfo/UserInfo";
import "./Header.css";

export default function Header() {
    return (
        <>
            <div className="header">
                <div style={{
                    fontFamily: 'Leckerli One',
                    color: '#FFFFFF',
                    fontWeight: 'normal',
                    fontStyle: 'normal'
                }}>
                    Chatly
                </div>
                <UserInfo />
            </div>
        </>
    )
}