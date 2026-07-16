import { useState, useEffect } from 'react';
import { useTheme } from '../context/ThemeContext';
import NavBar from '../components/NavBar';
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
    dateOfBirth: user.dateOfBirth || '',
    roles: normalizedRoles.map((name) => ({ name })),
  };
};

export default function UserManagement({ hideNav = false }) {
  const { isDark, toggleTheme } = useTheme();
  const [activeNav, setActiveNav] = useState('users');
  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState('');
  const [modal, setModal] = useState(null);
  const [selected, setSelected] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [loading, setLoading] = useState(true);

  const filteredUsers = users.filter((user) =>
    `${user.irn || ''} ${user.fullname || ''} ${user.email || ''}`.toLowerCase().includes(search.toLowerCase())
  );

  const openCreate = () => {
    setForm({ ...EMPTY_FORM });
    setSelected(null);
    setModal('create');
  };

  const openEdit = (user) => {
    setSelected(user);
    const rawRoles = user.roles || [];
    const normalizedRoles = Array.isArray(rawRoles)
      ? rawRoles.map((role) => (typeof role === 'string' ? role : role?.name)).filter(Boolean).map((role) => role.toUpperCase())
      : [];

    setForm({
      irn: user.irn || user.studentCode || user.teacherCode || '',
      fullname: user.fullname || user.fullName || '',
      email: user.email || '',
      password: '',
      role: user.role || normalizedRoles[0] || 'STUDENT',
    });
    setModal('edit');
  };

  const openDelete = (user) => {
    setSelected(user);
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
        setUsers((data || []).map(normalizeUser));
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
        setUsers((prev) => prev.map((user) => (user.id === selected.id ? normalizeUser(updatedData) : user)));
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
      setUsers((prev) => prev.filter((user) => user.id !== selected.id));
    } catch (error) {
      console.error('Failed to delete user', error);
      alert('Unable to delete user.');
    } finally {
      setModal(null);
    }
  };

  const stats = [
    { label: 'Total Users', value: users.length, color: 'text-purple-500' },
    { label: 'Students', value: users.filter((user) => (user.roles || []).some((role) => role.name?.toUpperCase() === 'STUDENT')).length, color: 'text-blue-500' },
    { label: 'Lecturers', value: users.filter((user) => (user.roles || []).some((role) => role.name?.toUpperCase() === 'LECTURER')).length, color: 'text-green-500' },
  ];

  return (
    <div className={isDark ? 'dark' : ''}>
      <div className="min-h-screen bg-gray-50 dark:bg-[#151b24] transition-colors">
        {!hideNav && (
          <NavBar
            active={activeNav}
            onNavigate={setActiveNav}
            isDark={isDark}
            onToggleTheme={toggleTheme}
          />
        )}

        <main className="max-w-7xl mx-auto px-6 py-6">
          <div className="mb-5">
            <h1 className="text-gray-900 dark:text-white font-semibold text-xl">User Management</h1>
            <p className="text-gray-500 dark:text-gray-400 text-sm mt-0.5">Manage all system users — students, lecturers and admins</p>
          </div>

          <UserStats stats={stats} />

          <UserTable
            search={search}
            onSearchChange={setSearch}
            onCreate={openCreate}
            filteredUsers={filteredUsers}
            users={users}
            loading={loading}
            onEdit={openEdit}
            onDelete={openDelete}
            roleColors={ROLE_COLORS}
          />
        </main>
      </div>

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
