import RequestInterceptor from "../middleware/RequestInterceptor";
import ResponseInterceptor from "../middleware/ResponseInterceptor";
import { Headers, Pages } from "../util/Constants";
import { ServiceUrl } from "../util/Constants";
import { getDeviceId } from "../util/Functions";
import { toast } from "sonner";
import { useAuthStore } from "../stores/authStore";


interface RequestOptions {
    json?: boolean;
}

const LOGIN_URL = ServiceUrl.BACKEND_SERVICE_BASE_URL + ServiceUrl.AUTH_BASE + Pages.LOGIN_PAGE;
const REFRESH_URL = ServiceUrl.BACKEND_SERVICE_BASE_URL + ServiceUrl.AUTH_BASE + '/refreshToken';
const LOGOUT_URL = ServiceUrl.BACKEND_SERVICE_BASE_URL + ServiceUrl.AUTH_BASE + '/logout';

class HttpService {

    private static _instance: HttpService;
    private _tokenRefreshPromise: Promise<any> | null = null;
    protected _defaultHeaders: object = {
    };

    private constructor() {
    }

    static get Instance() {
        // Do you need arguments? Make it a regular static method instead.
        return this._instance || (this._instance = new this());
    }

    protected async _getHeaders(headers?: HeadersInit): Promise<HeadersInit> {
        const customHeaders: { [key: string]: any } = {
            ...this._defaultHeaders,
        }

        const accessToken = useAuthStore.getState().accessToken;
        const deviceId = await getDeviceId();
        if (accessToken) {
            customHeaders[Headers.AUTHORIZATION] = `Bearer ${accessToken}`;
        }
        if (deviceId) {
            customHeaders[Headers.X_USER_DEVICE_ID] = deviceId;
        }
        if (headers) {
            return {
                ...headers,
                ...customHeaders
            }
        }
        return customHeaders;
    }

    protected _beforeRequest(request: Request) {
        RequestInterceptor.process(request);
    }

    protected _afterResponse(response: Response) {
        ResponseInterceptor.process(response);
    }

    public async prepareRequest(method = 'GET', data = {}, headers?: HeadersInit, signal?: AbortSignal | null): Promise<RequestInit> {

        const info: RequestInit = {
            method: method,
            headers: await this._getHeaders(headers),
            credentials: 'include',
            body: method !== 'GET' && data && Object.keys(data).length > 0 ? JSON.stringify(data) : null,
            signal: signal
        }
        return info;
    }

    protected async prepareRequestFile(method = 'GET', data?: FormData | null, headers?: HeadersInit, signal?: AbortSignal | null): Promise<RequestInit> {

        const info: RequestInit = {
            method: method,
            headers: await this._getHeaders(headers),
            credentials: 'include',
            body: data,
            signal: signal
        }
        return info;
    }

    protected async getNewTokens(): Promise<string | null> {
        if (this._tokenRefreshPromise) {
            return this._tokenRefreshPromise;
        }
        this._tokenRefreshPromise = (async() => {
            const info: RequestInit = await this.prepareRequest('GET');
            if (info.headers) {
                delete (info.headers as any)[Headers.AUTHORIZATION];
            }
            try {
                const response = await fetch(REFRESH_URL, info);
                if (response.status === 401 || response.status === 403) {
                    return null;
                }
                if (!response.ok) {
                    throw new Error(`Refresh failed with status ${response.status}`);
                }
                const bearer = response.headers.get(Headers.AUTHORIZATION);
                if (!bearer) return null;
                return bearer.split(' ')[1].trim();
            } catch {
                return null;
            } finally {
                this._tokenRefreshPromise = null;
            }
        })();
        return this._tokenRefreshPromise;
    }

    protected async _fetch(input: URL | RequestInfo, init?: RequestInit, options?: RequestOptions): Promise<any> {
        const isJson = options?.json ?? false;
        const url = typeof input === 'string' ? input : input.toString();
        const isAuthEndpoint = url.includes(LOGOUT_URL) || url.includes(REFRESH_URL);
        if (init && init.headers) {
            (init.headers as any)[Headers.AUTHORIZATION] = `Bearer ${useAuthStore.getState().accessToken}`;
        }
        try {
            let response = await fetch(input, init);
            if (input === LOGIN_URL && !response.ok) {
                const text = await response.text();
                toast.error(`Request failed on LOGIN_PAGE: status code - ${response.status}, body: ${text}`);
                return null;
            }
            if (response.status === 401 && !isAuthEndpoint) {
                const token = await this.getNewTokens();
                if (!token) {
                    useAuthStore.getState().clearAuth();
                    toast.error('You have been logged out!');
                    return null;
                }
                useAuthStore.getState().setAccessToken(token);
                // Starting pending requests with new token
                if (init && init.headers) {
                    (init.headers as any)[Headers.AUTHORIZATION] = `Bearer ${token}`;
                }
                // Trying request once again with new token
                response = await fetch(input, init);
            }
            this._afterResponse(response);
            if (isJson) {
                if (response.headers.get('content-length') === '0') return null;
                return await response.json();
            }
            return response.text();
        } catch (error: any) {
            toast.error(`${error}`);
            return null;
        }
    }

    protected async _fetchJSON(input: URL | RequestInfo, init?: RequestInit | undefined): Promise<any> {
        return this._fetch(input, init, {json: true});
    }

    protected _checkType(response: Response, contentType: string) {
        const responseType = response.headers.get("content-type");
        if (!responseType || !responseType.includes(contentType)) {
            throw new TypeError(`Oops, we haven't got proper type: ${contentType}!`);
        }
    }

    async getJson(url: string = '', data: object = {}, headers?: HeadersInit, signal?: AbortSignal | null): Promise<any> {
        const info: RequestInit = await this.prepareRequest('GET', data, {
            'Content-Type': 'application/json',
            ...headers
        }, signal);
        return this._fetchJSON(ServiceUrl.BACKEND_SERVICE_BASE_URL + url, info);
    }

    async postJson(url: string = '', data: object = {}, headers?: HeadersInit, signal?: AbortSignal | null): Promise<any> {
        const info: RequestInit = await this.prepareRequest('POST', data, {
            'Content-Type': 'application/json',
            ...headers
        }, signal);
        return this._fetchJSON(ServiceUrl.BACKEND_SERVICE_BASE_URL + url, info);
    }

    async putJson(url: string = '', data: object = {}, headers?: HeadersInit, signal?: AbortSignal | null): Promise<any> {
        const info: RequestInit = await this.prepareRequest('PUT', data, {
            'Content-Type': 'application/json',
            ...headers
        }, signal);
        return this._fetchJSON(ServiceUrl.BACKEND_SERVICE_BASE_URL + url, info);
    }

    async patchJson(url: string = '', data: object = {}, headers?: HeadersInit, signal?: AbortSignal | null): Promise<any> {
        const info: RequestInit = await this.prepareRequest('PATCH', data, {
            'Content-Type': 'application/json',
            ...headers
        }, signal);
        return this._fetchJSON(ServiceUrl.BACKEND_SERVICE_BASE_URL + url, info);
    }

    async deleteJson(url: string = '', data: object = {}, headers?: HeadersInit, signal?: AbortSignal | null): Promise<any> {
        const info: RequestInit = await this.prepareRequest('DELETE', data, headers, signal);
        return this._fetchJSON(ServiceUrl.BACKEND_SERVICE_BASE_URL + url, info);
    }

    // =================================================================================

    async get(url: string = '', data: object = {}, headers?: HeadersInit, signal?: AbortSignal | null): Promise<any> {
        const info: RequestInit = await this.prepareRequest('GET', data, headers, signal);
        return this._fetch(ServiceUrl.BACKEND_SERVICE_BASE_URL + url, info);
    }

    async post(url: string = '', data: object = {}, headers?: HeadersInit, signal?: AbortSignal | null): Promise<any> {
        const info: RequestInit = await this.prepareRequest('POST', data, headers, signal);
        return this._fetch(ServiceUrl.BACKEND_SERVICE_BASE_URL + url, info);
    }

    async postByUrl(fullUrl: string, data: object = {}, headers?: HeadersInit, signal?: AbortSignal | null): Promise<any> {
        const info: RequestInit = await this.prepareRequest('POST', data, headers, signal);
        return this._fetch(fullUrl, info);
    }

    async put(url: string = '', data: object = {}, headers?: HeadersInit, signal?: AbortSignal | null): Promise<any> {
        const info: RequestInit = await this.prepareRequest('PUT', data, headers, signal);
        return this._fetch(ServiceUrl.BACKEND_SERVICE_BASE_URL + url, info);
    }

    async patch(url: string = '', data: object = {}, headers?: HeadersInit, signal?: AbortSignal | null): Promise<any> {
        const info: RequestInit = await this.prepareRequest('PATCH', data, headers, signal);
        return this._fetch(ServiceUrl.BACKEND_SERVICE_BASE_URL + url, info);
    }

    async delete(url: string = '', data: object = {}, headers?: HeadersInit, signal?: AbortSignal | null): Promise<any> {
        const info: RequestInit = await this.prepareRequest('DELETE', data, headers, signal);
        return this._fetch(ServiceUrl.BACKEND_SERVICE_BASE_URL + url, info);
    }

    // =============================================================================================
    async postFile(url: string = '', data: any = {}, headers?: HeadersInit, signal?: AbortSignal | null): Promise<any> {
        const formData = new FormData();
        for (let key in data) {
            formData.append(key, data[key]);
        }
        const info: RequestInit = await this.prepareRequestFile('POST', formData, headers, signal);
        return this._fetch(ServiceUrl.BACKEND_SERVICE_BASE_URL + url, info);
    }

}

export default HttpService.Instance;
