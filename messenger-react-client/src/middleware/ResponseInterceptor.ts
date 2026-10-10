import { Headers } from "../util/Constants";
import { useAuthStore } from "../stores/authStore";

class ResponseInterceptor {
    private static _instance: ResponseInterceptor;

    constructor() {

    }

    static get Instance() {
        // Do you need arguments? Make it a regular static method instead.
        return this._instance || (this._instance = new this());      
    }

    process(response: Response): Response {
        return response;
    }

    private _processTokens(response: Response): void {
        const bearer = response.headers.get(Headers.AUTHORIZATION);
        if (!bearer) {
            return;
        }
        const accessToken = bearer.split(' ')[1].trim();
        // Store as object for future changes
        useAuthStore.getState().setAccessToken(accessToken);
    }

}

export default ResponseInterceptor.Instance;