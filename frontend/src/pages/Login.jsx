import LoginUI from './LoginUI';

export default function Login({ onLoginSuccess, loginMessage, onDismissLoginMessage, onBack }) {
  return (
    <LoginUI
      onLoginSuccess={onLoginSuccess}
      loginMessage={loginMessage}
      onDismissLoginMessage={onDismissLoginMessage}
      onBack={onBack}
    />
  );
}