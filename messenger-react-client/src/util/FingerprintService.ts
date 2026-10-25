import { DEVICE_ID } from "./Constants";
import * as FingerprintJS from '@fingerprintjs/fingerprintjs'

class FingerprintService {
    private static instance: FingerprintService;
    private fpPromise: Promise<any>;
    private deviceIdPromise: Promise<string> | null = null;

    private constructor() {
        this.fpPromise = FingerprintJS.load({monitoring: false});
    }

    static getInstance(): FingerprintService {
        if (!FingerprintService.instance) {
            FingerprintService.instance = new FingerprintService();
        }
        return FingerprintService.instance;
    }

    async getDeviceId(): Promise<string | null> {
        // Check localStorage first
        const deviceId = localStorage.getItem(DEVICE_ID);
        if (deviceId) {
            return deviceId;
        }

        // Use cached promise to avoid multiple fingerprint calculations
        if (!this.deviceIdPromise) {
            this.deviceIdPromise = this.fpPromise
                .then((fp: any) => fp.get())
                .then((result: any) => {
                    localStorage.setItem(DEVICE_ID, result.visitorId);
                    return result.visitorId;
                });
        }

        return this.deviceIdPromise;
    }

    setDeviceId(deviceId: string): string {
        localStorage.setItem(DEVICE_ID, deviceId);
        return deviceId;
    }

    // Optional: Clear cached fingerprint (useful for logout)
    clearCache(): void {
        this.deviceIdPromise = null;
    }
}

export const getDeviceId = () => FingerprintService.getInstance().getDeviceId();
export const setDeviceId = (deviceId: string) => localStorage.setItem(DEVICE_ID, deviceId);