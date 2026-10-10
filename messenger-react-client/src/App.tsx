import './App.css';
import { Toaster } from 'sonner';
import Routing from './routing/Routing';
import { Provider as BusProvider } from 'react-bus';
import StompChatDataProvider from './middleware/stomp/StompChatDataProvider';
import StompSessionWrapper from './middleware/stomp/StompSessionWrapper';

function App() {
  return (
    <>
      <Toaster
        position="bottom-right"
        toastOptions={{
          duration: 3000,
          style: {
            background: '#1e293b',
            color: '#f1f5f9',
            borderRadius: '12px',
            padding: '12px 20px',
            boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1)',
          },
        }}
      />
      <BusProvider>
          <StompSessionWrapper>
            <StompChatDataProvider>
              <Routing />
            </StompChatDataProvider>
          </StompSessionWrapper>
      </BusProvider>
    </>
  )
}

export default App;