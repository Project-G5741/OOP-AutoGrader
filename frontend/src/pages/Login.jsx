import LoginUI from './LoginUI';

export default function Login({ onLoginSuccess, loginMessage, onDismissLoginMessage }) {
  return (
    <LoginUI
      onLoginSuccess={onLoginSuccess}
      loginMessage={loginMessage}
      onDismissLoginMessage={onDismissLoginMessage}
    />
  );
}