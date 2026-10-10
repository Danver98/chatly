import { DEVICE_ID, Headers } from "../util/Constants";
import { useAuthStore } from "../stores/authStore";

class RequestInterceptor {
    private static _instance: RequestInterceptor;


    protected _defaultHeaders: object = {
        'Access-Control-Allow-Credentials': 'true',
    };

    private constructor() {

    }

    static get Instance() {
        // Do you need arguments? Make it a regular static method instead.
        return this._instance || (this._instance = new this());      
    }

    process(request: Request): Request {
        this._setTokens(request);
        this._setDeviceID(request);
        return request;
    }

    private _setTokens(request: Request): void {
        const accessToken = useAuthStore.getState().accessToken;
        // Store as object for future changes
        if (accessToken) {
            request.headers.set(Headers.AUTHORIZATION, `Bearer ${accessToken}`);
        }
    }

    private _setDeviceID(request: Request): void {
        const deviceID = localStorage.getItem(DEVICE_ID);
        if (deviceID) {
            request.headers.set(Headers.X_USER_DEVICE_ID, deviceID);
        }
    }

}

export default RequestInterceptor.Instance;