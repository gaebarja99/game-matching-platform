import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';

interface AlertContextValue {
  showAlert: (message: string) => void;
}

const AlertContext = createContext<AlertContextValue | null>(null);

export function AlertProvider({ children }: { children: React.ReactNode }) {
  const [message, setMessage] = useState<string | null>(null);

  const showAlert = useCallback((msg: string) => {
    const text = String(msg).replace(/^[^\s]+ 내용:\s*/i, '').trim() || msg;
    setMessage(text);
  }, []);

  useEffect(() => {
    const originalAlert = window.alert;
    window.alert = (msg: unknown) => {
      showAlert(msg != null ? String(msg) : '');
    };
    return () => {
      window.alert = originalAlert;
    };
  }, [showAlert]);

  const close = useCallback(() => setMessage(null), []);

  return (
    <AlertContext.Provider value={{ showAlert }}>
      {children}
      {message !== null && (
        <div className="app-alert-backdrop" role="dialog" aria-modal="true" aria-labelledby="app-alert-message">
          <div className="app-alert-box">
            <p id="app-alert-message" className="app-alert-message">{message}</p>
            <button type="button" className="app-alert-btn" onClick={close}>
              확인
            </button>
          </div>
        </div>
      )}
    </AlertContext.Provider>
  );
}

export function useAlert() {
  const ctx = useContext(AlertContext);
  return ctx?.showAlert ?? ((msg: string) => window.alert(msg));
}
