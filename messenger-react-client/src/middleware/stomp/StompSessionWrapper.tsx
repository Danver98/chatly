// StompSessionWrapper.tsx
import { StompSessionProvider } from 'react-stomp-hooks';
import { ServiceUrl, Headers } from '../../util/Constants';
import { useAuthStore } from '../../stores/authStore';

interface StompSessionWrapperProps {
  children: React.ReactNode;
}

const StompSessionWrapper: React.FC<StompSessionWrapperProps> = ({ children }) => {
  const hasToken = useAuthStore((state) => !!state.accessToken);
  if (!hasToken) return <>{children}</>

  return (
    <StompSessionProvider
      url={ServiceUrl.BACKEND_SERVICE_WEB_SOCKET_URL}
      beforeConnect={(client) => {
        const token = useAuthStore.getState().accessToken;
        client.configure({
          connectHeaders: token
            ? { [Headers.AUTHORIZATION]: `Bearer ${token}` }
            : {},
        });
      }}
      onConnect={(frame: any) => {
        console.log(`Successfully connected to server websocket with auth!`);
      }}
      onDisconnect={(frame: any) => {
        console.log(`Successfully disconnected from server websocket!`);
      }}
      onStompError={(error: any) => {
        console.error('STOMP error:', error);
      }}
      onWebSocketClose={(event: CloseEvent) => {
        console.log('WebSocket closed:', event);
      }}
    >
      {children}
    </StompSessionProvider>
  );
};

export default StompSessionWrapper;