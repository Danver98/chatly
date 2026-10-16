import { RefObject, useCallback, useEffect, useRef, useEffectEvent} from "react";
import Header from "../Blocks/Header/Header"
import Sidebar from "../Blocks/Sidebar/Sidebar"
import "./Layout.css";
import Content from "../Blocks/Content/Content";
import { useAuthStore } from "../../stores/authStore";
import { NetworkStatus } from "../../util/Constants";
import AuthService from "../../services/AuthService";
import { ID } from "../../util/Types";

const LAST_NETWORK_STATUS_UPDATE_TIME = 'last-network-status-update-time';
const LAST_VISIBLE_TIME = 'last-visible-time';
const LAST_HIDDEN_TIME = 'last-hidden-time';
const NETWORK_STATUS = 'network-status';
const TAB_ID = 'tab-id';
const TABS_COUNT = 'tabs-count';
const UPDATE_INTERVAL = 5000;
const STATUS_UPDATE_INTERVAL = 10000
const STATUS_THRESHOLD = 5000;

/**
 * TODO:
 * 1. Update status when all tabs of a site are closed (or browser is closed)
 * to set OFFLINE status
 * 2. Use singleton mode network status update (do not launch them on every tab, but on the single one)
 */

async function updateNetworkStatus(userId: ID, timeoutRef: RefObject<number|null>): Promise<any> {
    if (!userId) {
        timeoutRef.current = setTimeout(() => updateNetworkStatus(userId, timeoutRef), UPDATE_INTERVAL);
        return;
    }
    const lastNetworkStatusUpdateTime = localStorage.getItem(LAST_NETWORK_STATUS_UPDATE_TIME);
    let lastVisibleTime: Date | string | null = localStorage.getItem(LAST_VISIBLE_TIME);
    let lastHiddenTime: Date | string | null = localStorage.getItem(LAST_HIDDEN_TIME);
    lastVisibleTime = lastVisibleTime ? new Date(lastVisibleTime) : null;
    lastHiddenTime = lastHiddenTime ? new Date(lastHiddenTime) : null;
    if (lastNetworkStatusUpdateTime && (Date.now() - Date.parse(lastNetworkStatusUpdateTime) > STATUS_UPDATE_INTERVAL)) {
        if (lastHiddenTime && lastVisibleTime) {
            let status = NetworkStatus.OFFLINE;
            if (lastVisibleTime >= lastHiddenTime || lastHiddenTime.getTime() - lastVisibleTime.getTime() < STATUS_THRESHOLD) {
                status = NetworkStatus.ONLINE;
            }
            localStorage.setItem(NETWORK_STATUS, status.toString());
            localStorage.setItem(LAST_NETWORK_STATUS_UPDATE_TIME, (new Date()).toString());
            await AuthService.updateNetworkStatus(status);
        }
    }
    timeoutRef.current = setTimeout(() => updateNetworkStatus(userId, timeoutRef), UPDATE_INTERVAL);
}

function pageVisibilityChanged(terminatedRef: RefObject<boolean>): void {
    if (document.visibilityState === 'visible') {
        localStorage.setItem(LAST_VISIBLE_TIME, (new Date()).toString());
        const networkStatus = localStorage.getItem(NETWORK_STATUS);
        // If status is offline, set it to online
        if (networkStatus && +networkStatus === NetworkStatus.OFFLINE) {
            localStorage.setItem(NETWORK_STATUS, NetworkStatus.ONLINE.toString());
            localStorage.setItem(LAST_NETWORK_STATUS_UPDATE_TIME, (new Date()).toString());
            AuthService.updateNetworkStatus(NetworkStatus.ONLINE);
        }
    } else if (document.visibilityState === 'hidden') {
        localStorage.setItem(LAST_HIDDEN_TIME, (new Date()).toString());
        setTimeout(
            () => {
                if (terminatedRef.current) {
                    // Closing the tab or browser
                    // ATTENTION! Unreliable block of code
                    const tabsCount = localStorage.getItem(TABS_COUNT);
                    localStorage.setItem(TABS_COUNT, (tabsCount ? parseInt(tabsCount) -1 : 0).toString());
                    localStorage.setItem(NETWORK_STATUS, NetworkStatus.OFFLINE.toString());
                    localStorage.setItem(LAST_NETWORK_STATUS_UPDATE_TIME, (new Date()).toString());
                    // if it is last visible tab, set status to offline
                    if (tabsCount && parseInt(tabsCount) <=1) {
                        AuthService.updateNetworkStatus(NetworkStatus.OFFLINE); 
                    }
                }
            }
        );
    }
}
export default function Layout() {
    const userId: ID = useAuthStore((state) => state.currentLoggedUser?.id);
    const timeoutRef = useRef<number|null>(null);
    const tabRef = useRef<string|null>(null);
    const terminatedRef = useRef<boolean>(false);

    useEffect(() => {
        // On user logged in/out, update network status
        const status = userId ? NetworkStatus.ONLINE : NetworkStatus.OFFLINE
        localStorage.setItem(NETWORK_STATUS, status.toString());
        localStorage.setItem(LAST_NETWORK_STATUS_UPDATE_TIME, (new Date()).toString());
        AuthService.updateNetworkStatus(userId ? NetworkStatus.ONLINE : NetworkStatus.OFFLINE);
    }, [userId]);
 
    const updateNetworkStatusEvent = useEffectEvent(() => {
        // Getting fresh value of userId
        updateNetworkStatus(userId, timeoutRef);
    });
    useEffect(() => {
        localStorage.setItem(LAST_VISIBLE_TIME, (new Date()).toString());
        if (!sessionStorage.getItem(TAB_ID)) {
            const tabId = 'tab-' + (Math.floor(Math.random() * 100)).toString();
            tabRef.current = tabId;
            sessionStorage.setItem(TAB_ID, tabId);
            const tabsCount = localStorage.getItem(TABS_COUNT);
            localStorage.setItem(TABS_COUNT, (tabsCount ? parseInt(tabsCount) + 1 : 1).toString());

        }
        timeoutRef.current = setTimeout(() => updateNetworkStatusEvent(), UPDATE_INTERVAL);
        return () => {
            if (timeoutRef.current) {
                clearTimeout(timeoutRef.current);
            }
        }
    }, []);

    const pageVisibilityEffect = useEffectEvent(() => {
        pageVisibilityChanged(terminatedRef);
    });

    useEffect(() => {
        document.addEventListener('visibilitychange', pageVisibilityEffect);
        return () => {
            document.removeEventListener('visibilitychange', pageVisibilityEffect);
        }
    }, []);

    const pageHide = useCallback(() => {
        terminatedRef.current = true;
    }, [])

    useEffect(() => {
        window.addEventListener('pagehide', pageHide);
        return () => {
            window.removeEventListener('pagehide', pageHide);
        }  
    }, [pageHide])
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