import User from "../models/User";
import { NetworkStatus } from "./Constants";

export function getUserNetworkStatus(user: User, now?: Date): string {
    const status = user.networkStatus ?? NetworkStatus.OFFLINE;
    if (status === NetworkStatus.ONLINE) return 'Online';
    const lastVisited = user.lastVisited ?? null;
    if (lastVisited == null) return '';
    if (now == null) now = new Date();
    const diffInMs = Math.abs(lastVisited.getTime() - now.getTime());
    const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
    if (diffInMinutes === 0) {
        return 'Last seen now';
    } else if (diffInMinutes < 60) {
        return 'Last seen ' + diffInMinutes + (diffInMinutes === 1 ? ' minute' : ' minutes') + ' ago';
    }
    const todayYear = now.getFullYear();
    const todayMonth = now.getMonth();
    const todayDate = now.getDate();
    const lastVisitedYear = lastVisited.getFullYear();
    const lastVisitedMonth = lastVisited.getMonth();
    const lastVisitedDate = lastVisited.getDate();
    if (todayYear === lastVisitedYear && todayMonth === lastVisitedMonth
        && todayDate === lastVisitedDate) {
        return 'Last seen at ' + lastVisited.toLocaleTimeString(undefined,
            {
                hour: '2-digit',
                minute: '2-digit'
            })
    }
    const yesterday = new Date();
    yesterday.setDate(now.getDate() - 1);
    if (yesterday.getFullYear() === lastVisitedYear && yesterday.getMonth() === lastVisitedMonth
        && yesterday.getDate() === lastVisitedDate) {
        return 'Last seen yesterday at ' + lastVisited.toLocaleTimeString(undefined,
            {
                hour: '2-digit',
                minute: '2-digit'
            })
    }

    return 'Last seen ' + lastVisited.toLocaleDateString() + ' at ' + lastVisited.toLocaleTimeString(undefined,
        {
            hour: '2-digit',
            minute: '2-digit'
        });
}