import Header from "../Blocks/Header/Header"
import Sidebar from "../Blocks/Sidebar/Sidebar"
import "./Layout.css";
import Content from "../Blocks/Content/Content";


export default function Layout() {
    return (
        <>
            <Header />
            <div className="main-block">
                <Sidebar>
                </Sidebar>
                <Content />
            </div>
        </>
    )
}