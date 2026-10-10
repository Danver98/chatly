import { create } from "zustand";
import User from "../models/User";
import { persist } from "zustand/middleware";

interface AuthStoreState {
    accessToken: string | null;
    setAccessToken: (token: string | null) => void;
    clearAccessToken: () => void;
    setCurrentLoggedUser: (user: User | null) => void;
    currentLoggedUser: User | null;
    clearAuth: () => void
}

export const useAuthStore = create<AuthStoreState>()(
    persist(
        (set) => ({
            accessToken: null,
            setAccessToken: (accessToken: string | null) => set({ accessToken }),
            clearAccessToken: () => set({ accessToken: null }),
            currentLoggedUser: null,
            setCurrentLoggedUser: (user: User | null) => set({ currentLoggedUser: user }),
            clearAuth: () => set({ accessToken: null, currentLoggedUser: null }),
        }),
        {
            name: "auth-storage"
        }
    )
);