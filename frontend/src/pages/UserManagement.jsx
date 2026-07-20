import { useState, useEffect, useMemo } from 'react';
import { useTheme } from '../context/ThemeContext';
import AppShell from '../components/layout/AppShell';
import UserStats from '../components/UserStats';
import UserTable from '../components/UserTable';
import UserModal from '../components/UserModal';

const EMPTY_FORM = { irn: '', fullname: '', email: '', password: '', role: 'STUDENT' };

const ROLE_COLORS = {
  STUDENT: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  LECTURER: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
};

const normalizeUser = (user) => {
  const rawRoles = user.roles || [];
  const normalizedRoles = Array.isArray(rawRoles)
    ? rawRoles
        .map((role) => (typeof role === 'string' ? role : role?.name))
        .filter(Boolean)
        .map((role) => role.toUpperCase())
    : [];
  const mainRole = normalizedRoles[0] || '';
  return {
    ...user,
    irn: user.irn || user.studentCode || user.teacherCode || '',
    fullname: user.fullName || user.fullname || '',
    role: mainRole,
    dob: user.dateOfBirth || '',
    dateOfBirth: user.dateOfBirth || '',
    roles: normalizedRoles.map((name) => ({ name })),
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
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [sortDirection, setSortDirection] = useState('asc');

  const sortUsers = (list) => {
    return [...list].sort((a, b) => {
      const left = a.fullname?.toLowerCase() || '';
      const right = b.fullname?.toLowerCase() || '';
      if (left < right) return sortDirection === 'asc' ? -1 : 1;
      if (left > right) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  };

  const filteredUsers = useMemo(() => {
    const loweredSearch = search.toLowerCase();
    const matched = users.filter((item) =>
      `${item.irn || ''} ${item.fullname || ''} ${item.email || ''}`.toLowerCase().includes(loweredSearch)
    );
    return sortUsers(matched);
  }, [users, search, sortDirection]);

  const pageCount = Math.max(1, Math.ceil(filteredUsers.length / PAGE_SIZE));

  const currentPageUsers = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filteredUsers.slice(start, start + PAGE_SIZE);
  }, [filteredUsers, page]);

  const openCreate = () => {
    setForm({ ...EMPTY_FORM });
    setSelected(null);
    setModal('create');
  };

  const handleSearchChange = (value) => {
    setSearch(value);
    setPage(1);
  };

  const handleSortByName = () => {
    setSortDirection((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    setPage(1);
  };

  const openEdit = (item) => {
    setSelected(item);
    const rawRoles = item.roles || [];
    const normalizedRoles = Array.isArray(rawRoles)
      ? rawRoles.map((role) => (typeof role === 'string' ? role : role?.name)).filter(Boolean).map((role) => role.toUpperCase())
      : [];

    setForm({
      irn: item.irn || item.studentCode || item.teacherCode || '',
      fullname: item.fullname || item.fullName || '',
      email: item.email || '',
      password: '',
      role: item.role || normalizedRoles[0] || 'STUDENT',
    });
    setModal('edit');
  };

  const openDelete = (item) => {
    setSelected(item);
    setModal('delete');
  };

  useEffect(() => {
    const fetchUsers = async () => {
      setLoading(true);
      try {
        const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
        const resp = await fetch(`${API_BASE}/api/users/getAllUser`);
        if (!resp.ok) throw new Error(`Failed to load users: ${resp.status}`);
        const data = await resp.json();
        // console.log('Raw data from API:', data); for testing DB
        const normalized = (data || []).map(normalizeUser);
        // console.log('Normalized users:', normalized); for testing DB
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
    try {
      const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
      if (modal === 'edit' && selected) {
        const requestBody = {
          irn: form.irn,
          fullName: form.fullname,
          email: form.email,
          role: form.role,
        };
        const resp = await fetch(`${API_BASE}/api/users/${selected.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(requestBody),
        });
        if (!resp.ok) throw new Error(`Update failed: ${resp.status}`);
        const updatedData = await resp.json();
        setUsers((prev) => prev.map((item) => (item.id === selected.id ? normalizeUser(updatedData) : item)));
      } else {
        const userPayload = {
          fullName: form.fullname,
          email: form.email,
          password: form.password,
          studentCode: form.role === 'STUDENT' ? form.irn : null,
          teacherCode: form.role === 'LECTURER' ? form.irn : null,
          dateOfBirth: null,
          roleNames: [form.role],
        };
        const resp = await fetch(`${API_BASE}/api/users/addUser`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(userPayload),
        });
        if (!resp.ok) throw new Error(`Create failed: ${resp.status}`);
        const createdData = await resp.json();
        setUsers((prev) => [...prev, normalizeUser(createdData)]);
      }
      setModal(null);
    } catch (error) {
      console.error('Failed to save user', error);
      alert('Unable to save user.');
    }
  };

  const handleDelete = async () => {
    if (!selected) return;
    try {
      const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
      const resp = await fetch(`${API_BASE}/api/users/${selected.id}`, {
        method: 'DELETE',
      });
      if (!resp.ok) throw new Error(`Delete failed: ${resp.status}`);
      setUsers((prev) => prev.filter((item) => item.id !== selected.id));
    } catch (error) {
      console.error('Failed to delete user', error);
      alert('Unable to delete user.');
    } finally {
      setModal(null);
    }
  };

  const stats = [
    { label: 'Total Users', value: users.length, color: 'text-purple-500' },
    { label: 'Students', value: users.filter((item) => (item.roles || []).some((role) => role.name?.toUpperCase() === 'STUDENT')).length, color: 'text-blue-500' },
    { label: 'Lecturers', value: users.filter((item) => (item.roles || []).some((role) => role.name?.toUpperCase() === 'LECTURER')).length, color: 'text-green-500' },
  ];

  const inner = (
    <main className="space-y-6">
      <div className="mb-5">
        <h1 className="text-xl font-semibold text-gray-900 dark:text-white">User Management</h1>
        <p className="mt-0.5 text-sm text-gray-500 dark:text-gray-400">Manage all system users — students, lecturers and admins</p>
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
        onSort={handleSortByName}
        sortDirection={sortDirection}
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
        isDark={isDark}
        onClose={() => setModal(null)}
        onSave={handleSave}
        onDelete={handleDelete}
        onFieldChange={(key, value) => setForm((prev) => ({ ...prev, [key]: value }))}
        onRoleChange={(value) => setForm((prev) => ({ ...prev, role: value }))}
      />
    </div>
  );
}
