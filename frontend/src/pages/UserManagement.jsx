import { useState, useEffect, useMemo } from 'react';
import { useTheme } from '../context/ThemeContext';
import AppShell from '../components/layout/AppShell';
import UserStats from '../components/UserStats';
import UserTable from '../components/UserTable';
import UserModal from '../components/UserModal';
import { authHeaders } from '../utils/authHeaders';
import { readApiErrorMessage } from '../utils/apiError';
import {
  getUserFormErrors,
  isFormValid,
} from '../utils/validation';
import { sortRows, toggleSortState } from '../utils/sort';

const EMPTY_FORM = {
  studentIrn: '',
  lecturerIrn: '',
  fullname: '',
  email: '',
  password: '',
  roles: [],
};

const ROLE_COLORS = {
  STUDENT: 'bg-primary-light text-primary-text',
  LECTURER: 'bg-success-bg text-success-text',
};

const normalizeUser = (user) => {
  const rawRoles = user.roles || [];
  const normalizedRoles = Array.isArray(rawRoles)
    ? rawRoles
        .map((role) => (typeof role === 'string' ? role : role?.name))
        .filter(Boolean)
        .map((role) => {
          const upper = role.toUpperCase();
          return upper === 'TEACHER' ? 'LECTURER' : upper;
        })
    : [];
  const uniqueRoles = [...new Set(normalizedRoles)];
  return {
    ...user,
    irn: user.irn || user.studentCode || user.teacherCode || '',
    studentCode: user.studentCode || '',
    teacherCode: user.teacherCode || '',
    fullname: user.fullName || user.fullname || '',
    role: uniqueRoles[0] || '',
    roles: uniqueRoles.map((name) => ({ name })),
    roleNames: uniqueRoles,
    dob: user.dateOfBirth || '',
    dateOfBirth: user.dateOfBirth || '',
  };
};

const PAGE_SIZE = 10;

export default function UserManagement({ hideNav = false, user, onLogout, noShell = false }) {
  const { isDark } = useTheme();
  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState('');
  const [modal, setModal] = useState(null);
  const [selected, setSelected] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState('');
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [sortState, setSortState] = useState({ field: 'fullname', direction: 'asc' });

  const userSortAccessor = (user, field) => {
    if (field === 'role') {
      return user.roleNames?.[0] || user.role || '';
    }
    return user[field] ?? '';
  };

  const filteredUsers = useMemo(() => {
    const loweredSearch = search.toLowerCase();
    const matched = users.filter((item) =>
      `${item.irn || ''} ${item.studentCode || ''} ${item.teacherCode || ''} ${item.fullname || ''} ${item.email || ''}`
        .toLowerCase()
        .includes(loweredSearch)
    );
    return sortRows(matched, sortState.field, sortState.direction, (user) => userSortAccessor(user, sortState.field));
  }, [users, search, sortState]);

  const currentPageUsers = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filteredUsers.slice(start, start + PAGE_SIZE);
  }, [filteredUsers, page]);

  const currentFieldErrors = useMemo(
    () => getUserFormErrors(form, modal),
    [form, modal]
  );
  const canSave = modal === 'create' || modal === 'edit' ? isFormValid(currentFieldErrors) : false;

  const openCreate = () => {
    setForm({ ...EMPTY_FORM, roles: ['STUDENT'] });
    setFormError('');
    setSelected(null);
    setModal('create');
  };

  const handleSearchChange = (value) => {
    setSearch(value);
    setPage(1);
  };

  const handleSort = (field) => {
    setSortState((prev) => toggleSortState(prev, field));
    setPage(1);
  };

  const openEdit = (item) => {
    setSelected(item);
    const roleNames = item.roleNames || (item.roles || []).map((role) => role.name).filter(Boolean);
    const normalizedRoles = roleNames.map((role) => {
      const upper = String(role).toUpperCase();
      return upper === 'TEACHER' ? 'LECTURER' : upper;
    });

    setForm({
      studentIrn: item.studentCode || (normalizedRoles.length === 1 && normalizedRoles[0] === 'STUDENT' ? item.irn : '') || '',
      lecturerIrn: item.teacherCode || (normalizedRoles.length === 1 && normalizedRoles[0] === 'LECTURER' ? item.irn : '') || '',
      fullname: item.fullname || item.fullName || '',
      email: item.email || '',
      password: '',
      roles: normalizedRoles.length ? normalizedRoles : ['STUDENT'],
    });
    setFormError('');
    setModal('edit');
  };

  const openDelete = (item) => {
    setSelected(item);
    setModal('delete');
  };

  const handleRoleToggle = (roleName) => {
    setForm((prev) => {
      const current = prev.roles || [];
      const next = current.includes(roleName)
        ? current.filter((role) => role !== roleName)
        : [...current, roleName];
      const nextForm = { ...prev, roles: next.length ? next : current };
      return nextForm;
    });
    setFormError('');
  };

  const handleFieldChange = (key, value) => {
    setForm((prev) => ({ ...prev, [key]: value }));
    setFormError('');
  };

  const buildPayload = () => {
    const roleNames = [...(form.roles || [])];
    const studentCode = roleNames.includes('STUDENT')
      ? (form.studentIrn || selected?.studentCode || selected?.irn || '').trim()
      : null;
    const teacherCode = roleNames.includes('LECTURER')
      ? (form.lecturerIrn || selected?.teacherCode || '').trim()
      : null;
    const legacyRole = roleNames.includes('LECTURER') && !roleNames.includes('STUDENT')
      ? 'LECTURER'
      : 'STUDENT';
    const legacyIrn = legacyRole === 'LECTURER' ? teacherCode : studentCode;

    const payload = {
      fullName: form.fullname?.trim(),
      email: form.email?.trim(),
      roleNames,
      role: legacyRole,
      irn: legacyIrn,
    };
    if (studentCode) payload.studentCode = studentCode;
    if (teacherCode) payload.teacherCode = teacherCode;
    const trimmedPassword = form.password?.trim();
    if (trimmedPassword) {
      payload.password = trimmedPassword;
    }
    return payload;
  };

  useEffect(() => {
    const fetchUsers = async () => {
      setLoading(true);
      try {
        const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
        const resp = await fetch(`${API_BASE}/api/users/getAllUser?page=0&size=50`, {
          headers: authHeaders(),
        });
        if (resp.status === 401 || resp.status === 403) {
          throw new Error('You are not authorized to manage users.');
        }
        if (!resp.ok) throw new Error(`Failed to load users: ${resp.status}`);
        const data = await resp.json();
        const items = Array.isArray(data) ? data : (data.content ?? []);
        const normalized = items.map(normalizeUser);
        setUsers(normalized);
      } catch (error) {
        console.error('Error fetching users', error);
        setUsers([]);
      } finally {
        setLoading(false);
      }
    };

    fetchUsers();
  }, []);

  const handleSave = async () => {
    const nextErrors = getUserFormErrors(form, modal);
    if (!isFormValid(nextErrors)) {
      return;
    }

    try {
      const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
      const requestBody = buildPayload();
      setFormError('');

      if (modal === 'edit' && selected) {
        const resp = await fetch(`${API_BASE}/api/users/${selected.id}`, {
          method: 'PUT',
          headers: authHeaders({ 'Content-Type': 'application/json' }),
          body: JSON.stringify(requestBody),
        });
        if (resp.status === 401 || resp.status === 403) {
          throw new Error('You are not authorized to update users.');
        }
        if (!resp.ok) {
          throw new Error(await readApiErrorMessage(resp, `Update failed: ${resp.status}`));
        }
        const updatedData = await resp.json();
        setUsers((prev) => prev.map((item) => (item.id === selected.id ? normalizeUser(updatedData) : item)));
      } else {
        const createPayload = {
          ...requestBody,
          password: form.password?.trim(),
        };
        const resp = await fetch(`${API_BASE}/api/users/addUser`, {
          method: 'POST',
          headers: authHeaders({ 'Content-Type': 'application/json' }),
          body: JSON.stringify(createPayload),
        });
        if (resp.status === 401 || resp.status === 403) {
          throw new Error('You are not authorized to create users.');
        }
        if (!resp.ok) {
          throw new Error(await readApiErrorMessage(resp, `Create failed: ${resp.status}`));
        }
        const createdData = await resp.json();
        setUsers((prev) => [...prev, normalizeUser(createdData)]);
      }
      setModal(null);
      setFormError('');
    } catch (error) {
      console.error('Failed to save user', error);
      setFormError(error.message || 'Unable to save user.');
    }
  };

  const handleDelete = async () => {
    if (!selected) return;
    try {
      const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
      const resp = await fetch(`${API_BASE}/api/users/${selected.id}`, {
        method: 'DELETE',
        headers: authHeaders(),
      });
      if (resp.status === 401 || resp.status === 403) {
        throw new Error('You are not authorized to delete users.');
      }
      if (!resp.ok) throw new Error(`Delete failed: ${resp.status}`);
      setUsers((prev) => prev.filter((item) => item.id !== selected.id));
    } catch (error) {
      console.error('Failed to delete user', error);
      alert(error.message || 'Unable to delete user.');
    } finally {
      setModal(null);
    }
  };

  const stats = [
    { label: 'Total Users', value: users.length, color: 'text-primary' },
    { label: 'Students', value: users.filter((item) => (item.roleNames || []).includes('STUDENT') || (item.roles || []).some((role) => role.name?.toUpperCase() === 'STUDENT')).length, color: 'text-chart-blue' },
    { label: 'Lecturers', value: users.filter((item) => (item.roleNames || []).includes('LECTURER') || (item.roles || []).some((role) => ['LECTURER', 'TEACHER'].includes(role.name?.toUpperCase()))).length, color: 'text-success' },
  ];

  const inner = (
    <main className="space-y-6 px-4 sm:px-6 lg:px-8 max-w-full overflow-x-hidden">
      <div className="mb-5">
        <h1 className="text-xl font-semibold text-foreground">User Management</h1>
      </div>

      <UserStats stats={stats} />

      <UserTable
        search={search}
        onSearchChange={handleSearchChange}
        onCreate={openCreate}
        rows={currentPageUsers}
        users={users}
        totalItems={filteredUsers.length}
        currentPage={page}
        pageSize={PAGE_SIZE}
        onPageChange={setPage}
        onSort={handleSort}
        sortState={sortState}
        loading={loading}
        onEdit={openEdit}
        onDelete={openDelete}
        roleColors={ROLE_COLORS}
      />
    </main>
  );

  return (
    <div className={isDark ? 'dark' : ''}>
      {noShell ? (
        inner
      ) : (
        <AppShell user={user} onLogout={onLogout} showNav={!hideNav} activeNav="users" onNavigate={() => {}}>
          {inner}
        </AppShell>
      )}

      <UserModal
        modal={modal}
        selected={selected}
        form={form}
        fieldErrors={currentFieldErrors}
        formError={formError}
        canSave={canSave}
        isDark={isDark}
        onClose={() => {
          setModal(null);
          setFormError('');
        }}
        onSave={handleSave}
        onDelete={handleDelete}
        onFieldChange={handleFieldChange}
        onRoleToggle={handleRoleToggle}
      />
    </div>
  );
}
