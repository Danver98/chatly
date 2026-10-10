import User from "../models/User";
import HttpService from "./HttpService";
import { ServiceUrl } from "../util/Constants";
import { useAuthStore } from "../stores/authStore";

export interface AuthData {
    user: User;
    accessToken: string;
    refreshToken: string;
}

const LOGOUT_URL = ServiceUrl.BACKEND_SERVICE_BASE_URL + ServiceUrl.AUTH_BASE + '/logout';

export class AuthService {

    private static _instance: AuthService;
    private static readonly URL = '/auth';

    private constructor() {

    }

    static get Instance() {
        // Do you need arguments? Make it a regular static method instead.
        return this._instance || (this._instance = new this());      
    }

    async register(data: object): Promise<any> {
        return HttpService.postJson(AuthService.URL + '/register', data);
    }

    async login(data: object): Promise<AuthData> {
        return HttpService.postJson(AuthService.URL + '/login', data);
    }

    async logout(_userId: number | string): Promise<any|void> {
        const info: RequestInit = await HttpService.prepareRequest('GET');
        try {
            await fetch(LOGOUT_URL, info);
        } catch {
            // ignore — we're logging out anyway
        } finally {
            useAuthStore.getState().clearAuth();
        }
    }

    async getRefreshToken(): Promise<any> {
        return HttpService.get(AuthService.URL + `/refreshToken`); 
    }

    async getAccessToken(): Promise<any> {
        return HttpService.get(AuthService.URL + `/accessToken`);
    }
}

export default AuthService.Instance;