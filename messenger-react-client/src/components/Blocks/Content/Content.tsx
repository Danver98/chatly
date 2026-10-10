import { Outlet } from "react-router-dom";
import "./Content.css"

export default function Content() {

    return (
        <div className="content">
            <Outlet />
        </div>
    )
}