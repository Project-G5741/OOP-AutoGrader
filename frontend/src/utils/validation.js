export const MESSAGES = {
  required: 'This field is required',
  loginCode: 'Please enter your student code or lecturer code.',
  loginPassword: 'Please enter your password.',
  studentIrn: 'Student IRN must be exactly 10 digits',
  lecturerCode: 'Lecturer code is required',
  passwordMin: 'Password must be at least 6 characters',
  passwordMax: 'Password must be less than 100 characters',
  passwordMismatch: 'Passwords do not match',
  passwordSameAsCurrent: 'New password must be different from current password',
  currentPasswordRequired: 'Current password is required',
  emailInvalid: 'Please enter a valid email address',
  emailDomain: 'Email must end with @eiu.edu.vn',
  fullName: 'Full name is required',
};

const STUDENT_IRN_PATTERN = /^\d{10}$/;
const EIU_EMAIL_DOMAIN_BODY = 'eiu.edu.vn';

export function validateRequired(value, message = MESSAGES.required) {
  if (value === null || value === undefined || String(value).trim() === '') {
    return message;
  }
  return '';
}

export function validateStudentIrn(value) {
  const trimmed = String(value ?? '').trim();
  if (!trimmed) {
    return MESSAGES.required;
  }
  if (!STUDENT_IRN_PATTERN.test(trimmed)) {
    return MESSAGES.studentIrn;
  }
  return '';
}

export function validateLecturerCode(value) {
  return validateRequired(value, MESSAGES.lecturerCode);
}

export function validatePassword(value, { required = true } = {}) {
  const trimmed = String(value ?? '').trim();
  if (!trimmed) {
    return required ? MESSAGES.required : '';
  }
  if (trimmed.length < 6) {
    return MESSAGES.passwordMin;
  }
  if (trimmed.length > 100) {
    return MESSAGES.passwordMax;
  }
  return '';
}

export function validatePasswordConfirm(password, confirm) {
  if (!confirm) {
    return MESSAGES.required;
  }
  if (password !== confirm) {
    return MESSAGES.passwordMismatch;
  }
  return '';
}

export function validateNewPasswordDifferent(currentPassword, newPassword) {
  if (!currentPassword || !newPassword) {
    return '';
  }
  if (currentPassword === newPassword) {
    return MESSAGES.passwordSameAsCurrent;
  }
  return '';
}

export function validateEmail(value) {
  const trimmed = String(value ?? '').trim();
  if (!trimmed) {
    return MESSAGES.required;
  }
  const atIndex = trimmed.lastIndexOf('@');
  if (atIndex <= 0 || atIndex === trimmed.length - 1) {
    return MESSAGES.emailInvalid;
  }
  const localPart = trimmed.slice(0, atIndex);
  const domain = trimmed.slice(atIndex + 1).toLowerCase();
  if (!localPart || domain.includes(' ')) {
    return MESSAGES.emailInvalid;
  }
  if (domain !== EIU_EMAIL_DOMAIN_BODY) {
    return MESSAGES.emailDomain;
  }
  return '';
}

export function validateFullName(value) {
  return validateRequired(value, MESSAGES.fullName);
}

export function isFormValid(errors) {
  return Object.values(errors).every((message) => !message);
}

export function getLoginFieldErrors(irn, password) {
  return {
    irn: validateRequired(irn, MESSAGES.loginCode),
    password: validateRequired(password, MESSAGES.loginPassword),
  };
}

export function getFirstTimeSetupErrors(irn, password, confirm) {
  return {
    irn: validateStudentIrn(irn),
    password: validatePassword(password),
    confirm: validatePasswordConfirm(password, confirm),
  };
}

export function getResetPasswordErrors(newPassword, confirmPassword) {
  return {
    newPassword: validatePassword(newPassword),
    confirmPassword: validatePasswordConfirm(newPassword, confirmPassword),
  };
}

export function getChangePasswordErrors(currentPassword, newPassword, confirmPassword) {
  return {
    currentPassword: validateRequired(currentPassword, MESSAGES.currentPasswordRequired),
    newPassword: validatePassword(newPassword) || validateNewPasswordDifferent(currentPassword, newPassword),
    confirmPassword: validatePasswordConfirm(newPassword, confirmPassword),
  };
}

export function getUserFormErrors(form, mode) {
  const roleNames = form.roles || [];
  const errors = {
    roles: roleNames.length ? '' : 'Select at least one role.',
    studentIrn: '',
    lecturerIrn: '',
    fullname: validateFullName(form.fullname),
    email: validateEmail(form.email),
    password: validatePassword(form.password, { required: mode === 'create' }),
  };

  if (roleNames.includes('STUDENT')) {
    errors.studentIrn = validateStudentIrn(form.studentIrn);
  }
  if (roleNames.includes('LECTURER')) {
    errors.lecturerIrn = validateLecturerCode(form.lecturerIrn);
  }

  return errors;
}
