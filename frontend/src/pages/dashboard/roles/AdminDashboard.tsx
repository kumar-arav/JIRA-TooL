import { useState, useEffect } from 'react'
import { KpiCard, Section, EmptyRow, RoleTag } from '@/components/dashboard/shared'
import { DashboardData } from '@/components/dashboard/useDashboardData'
import toast from 'react-hot-toast'
import ReportPreviewModal from '@/components/modals/ReportPreviewModal'
import { register } from '@/api/auth'
import api from '@/api/axios'
import { Edit } from 'lucide-react'
import { useSelector } from 'react-redux'
import { RootState } from '@/store'
import { wsClient } from '@/utils/websocket'

const RBAC_MATRIX: [string, ...boolean[]][] = [
  ['Create Project',    true, true,  true,  false, false, false, false, false, false],
  ['Manage Sprints',    true, true,  false, false, false, false, false, false, false],
  ['AI Task Generate',  true, true,  true,  false, false, false, false, false, false],
  ['Assign Tickets',    true, true,  true,  false, false, true,  false, false, false],
  ['Approve Closure',   true, false, false, false, false, true,  false, true,  false],
  ['Update Tickets',    true, true,  true,  false, false, true,  true,  true,  true ],
  ['View Reports',      true, true,  true,  true,  true,  true,  false, false, false],
  ['Manage Users',      true, false, false, false, false, false, false, false, false],
]
const ROLE_HEADERS = ['Admin','SM','PO','CTO','VP','Mgr','Dev','Test','Trainee']

const formatTimestamp = (timeStr?: string) => {
  if (!timeStr) return 'N/A'
  try {
    const d = new Date(timeStr)
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) + ' ' + d.toLocaleDateString([], { month: 'short', day: 'numeric' })
  } catch (e) {
    return timeStr
  }
}

export default function AdminDashboard({ data }: { data: DashboardData }) {
  const { users, projects } = data
  const currentUser = useSelector((s: RootState) => s.auth.user)
  const isAdmin = currentUser?.role === 'ADMIN'
  const [userList, setUserList] = useState(users)
  const [selectedReport, setSelectedReport] = useState<{ title: string; type: string; data?: any } | null>(null)
  
  // Add User Form States
  const [showAddUserModal, setShowAddUserModal] = useState(false)
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [selectedRole, setSelectedRole] = useState('DEVELOPER')
  const [submitting, setSubmitting] = useState(false)

  // Login Activity & Audit Logs States
  const [loginActivity, setLoginActivity] = useState<Array<{ user: string; role: string; when: string; ip: string; ok: boolean }>>(() => {
    const saved = localStorage.getItem('flowsync_login_activity')
    return saved ? JSON.parse(saved) : [
      { user: 'sarah.chen@flowsync.com', role: 'SCRUM_MASTER', when: 'Today 09:12', ip: '10.0.4.21', ok: true },
      { user: 'james.doe@flowsync.com',  role: 'DEVELOPER',    when: 'Today 09:05', ip: '10.0.4.34', ok: true },
      { user: 'priya.rao@flowsync.com',  role: 'TESTER',       when: 'Today 08:58', ip: '10.0.4.19', ok: true },
      { user: 'unknown@external.com',    role: '—',            when: 'Today 03:41', ip: '185.22.8.4', ok: false },
      { user: 'rita.patel@flowsync.com', role: 'MANAGER',      when: 'Yesterday 17:22', ip: '10.0.4.52', ok: true },
    ]
  })

  const [auditLogs, setAuditLogs] = useState<Array<{ action: string; detail: string; when: string }>>(() => {
    const saved = localStorage.getItem('flowsync_audit_logs')
    return saved ? JSON.parse(saved) : [
      { action: 'TICKET_STATUS_CHANGED', detail: 'EHR-105 → TESTING by priya.rao', when: '09:44' },
      { action: 'USER_LOGIN',            detail: 'sarah.chen authenticated with MFA', when: '09:12' },
      { action: 'SPRINT_STARTED',        detail: 'Sprint 3 activated by sarah.chen', when: 'Yesterday' },
      { action: 'PROJECT_MEMBER_ADDED',  detail: 'dan.okafor added to EHR System', when: 'Yesterday' },
      { action: 'FAILED_LOGIN',          detail: '3 failed attempts from 185.22.8.4 — blocked', when: '03:41' },
    ]
  })

  const [showAddLogModal, setShowAddLogModal] = useState(false)
  const [newLogAction, setNewLogAction] = useState('USER_LOGIN')
  const [newLogDetail, setNewLogDetail] = useState('')

  const [showAddLoginModal, setShowAddLoginModal] = useState(false)
  const [newLoginUser, setNewLoginUser] = useState('')
  const [newLoginRole, setNewLoginRole] = useState('DEVELOPER')
  const [newLoginIp, setNewLoginIp] = useState('10.0.4.55')
  const [newLoginOk, setNewLoginOk] = useState(true)

  const handleAddLog = (e: React.FormEvent) => {
    e.preventDefault()
    if (!newLogDetail.trim()) return
    const newLog = { action: newLogAction, detail: newLogDetail, when: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) }
    const updated = [newLog, ...auditLogs]
    setAuditLogs(updated)
    localStorage.setItem('flowsync_audit_logs', JSON.stringify(updated))
    setNewLogDetail('')
    setShowAddLogModal(false)
    toast.success('Audit log added!')
  }

  const handleAddLogin = (e: React.FormEvent) => {
    e.preventDefault()
    if (!newLoginUser.trim()) return
    const newLogin = {
      user: newLoginUser,
      role: newLoginRole,
      when: 'Today ' + new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      ip: newLoginIp,
      ok: newLoginOk
    }
    const updated = [newLogin, ...loginActivity]
    setLoginActivity(updated)
    localStorage.setItem('flowsync_login_activity', JSON.stringify(updated))
    setNewLoginUser('')
    setShowAddLoginModal(false)
    toast.success('Login activity recorded!')
  }

  // Edit User Form States
  const [showEditModal, setShowEditModal] = useState(false)
  const [editingUserId, setEditingUserId] = useState<number | null>(null)
  const [editFirstName, setEditFirstName] = useState('')
  const [editLastName, setEditLastName] = useState('')
  const [editEmail, setEditEmail] = useState('')

  // Admin Config Panel States
  const [showAdminConfigModal, setShowAdminConfigModal] = useState(false)
  const [adminConfigTab, setAdminConfigTab] = useState<'EMPLOYEE' | 'PROJECT' | 'MEMBER'>('EMPLOYEE')

  // Employee Form
  const [empName, setEmpName] = useState('')
  const [empEmail, setEmpEmail] = useState('')
  const [empDept, setEmpDept] = useState('Engineering')
  const [empPosition, setEmpPosition] = useState('Developer')
  const [customDeptPos, setCustomDeptPos] = useState(false)

  const deptPositions: Record<string, string[]> = {
    'Engineering': ['Developer', 'Scrum Master', 'CTO', 'Trainee'],
    'QA': ['Tester'],
    'Product & Management': ['Project Owner', 'Manager', 'VP'],
    'Design': ['UI Designer', 'UX Designer']
  }

  const handleDeptChange = (dept: string) => {
    setEmpDept(dept)
    const positions = deptPositions[dept] || []
    if (positions.length > 0) {
      setEmpPosition(positions[0])
    }
  }

  // Project Form
  const [projName, setProjName] = useState('')
  const [projKey, setProjKey] = useState('')
  const [projDuration, setProjDuration] = useState('')
  const [projGitRepo, setProjGitRepo] = useState('')

  // Member Form
  const [memberProjId, setMemberProjId] = useState('')
  const [memberUserId, setMemberUserId] = useState('')


  const handleAddEmployeeSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!empName.trim() || !empEmail.trim()) {
      return toast.error("Employee Name and Email are required")
    }
    setSubmitting(true)
    try {
      await api.post('/admin/add-employee', {
        name: empName,
        email: empEmail,
        department: empDept,
        position: empPosition
      })
      toast.success("Employee registered successfully! Temp password sent by email. ✉️")
      // Refresh the full users list from the API so the new user appears in Assign Member dropdown
      const freshUsers = await api.get('/users')
      setUserList(freshUsers.data?.data || freshUsers.data || [])
      setEmpName('')
      setEmpEmail('')
      setCustomDeptPos(false)
      setEmpDept('Engineering')
      setEmpPosition('Developer')
      setShowAdminConfigModal(false)
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Failed to add employee")
    } finally {
      setSubmitting(false)
    }
  }

  const handleAddProjectSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!projName.trim() || !projKey.trim()) {
      return toast.error("Project Name and Key are required")
    }
    setSubmitting(true)
    try {
      await api.post('/projects', {
        name: projName,
        projectKey: projKey,
        duration: projDuration,
        gitRepo: projGitRepo,
        priority: 'MEDIUM',
        status: 'PLANNING'
      })
      toast.success("Project created successfully! 🚀")
      setProjName('')
      setProjKey('')
      setProjDuration('')
      setProjGitRepo('')
      setShowAdminConfigModal(false)
      setTimeout(() => window.location.reload(), 1000)
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Failed to create project")
    } finally {
      setSubmitting(false)
    }
  }

  const handleDeleteUser = async (id: number) => {
    if (!window.confirm("Are you sure you want to permanently delete this employee? This will wipe all their details and access.")) return
    try {
      await api.delete(`/admin/delete-employee/${id}`)
      toast.success("Employee deleted successfully! 🗑️")
      const freshUsers = await api.get('/users')
      setUserList(freshUsers.data?.data || freshUsers.data || [])
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Failed to delete employee")
    }
  }

  const handleAddMemberSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!memberProjId || !memberUserId) {
      return toast.error("Please select a project and a team member")
    }
    setSubmitting(true)
    try {
      await api.post(`/projects/${memberProjId}/members/${memberUserId}`)
      toast.success("Member added to project successfully! Notification email sent. ✉️")
      setMemberProjId('')
      setMemberUserId('')
      setShowAdminConfigModal(false)
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Failed to assign member to project")
    } finally {
      setSubmitting(false)
    }
  }

  useEffect(() => {
    setUserList(users)
  }, [users])

  useEffect(() => {
    const unsubscribe = wsClient.subscribe((evt) => {
      if (evt.type === 'USER_LOGIN') {
        setUserList(prev => prev.map(u => u.email === evt.user ? { ...u, active: true, lastLoginTime: evt.lastLoginTime } : u))
      } else if (evt.type === 'USER_LOGOUT') {
        setUserList(prev => prev.map(u => u.email === evt.user ? { ...u, active: false, lastLogoutTime: evt.lastLogoutTime } : u))
      }
    })
    return () => unsubscribe()
  }, [])

  const activeUsers = userList.filter(u => u.active).length

  const handleOpenReport = (type: string) => {
    const reportData = {
      logs: auditLogs.map(a => [
        a.when,
        a.action,
        a.detail,
        a.action.includes('FAILED') ? '185.22.8.4' : '10.0.4.21',
        a.action.includes('FAILED') ? 'BLOCKED' : 'SUCCESS'
      ])
    }
    setSelectedReport({ title: type, type, data: reportData })
  }

  const handleAddUserSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!firstName.trim() || !lastName.trim() || !email.trim() || !password.trim()) {
      return toast.error('All fields are required')
    }

    setSubmitting(true)
    try {
      const newUser = await register({
        firstName,
        lastName,
        email,
        password,
        role: selectedRole
      })
      setUserList(prev => [...prev, newUser])
      toast.success('User registered successfully! 🎉')

      // Append registration event to audit logs
      const newLog = {
        action: 'USER_REGISTERED',
        detail: `User ${firstName} ${lastName} (${selectedRole}) registered successfully by Admin`,
        when: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      }
      const updatedLogs = [newLog, ...auditLogs]
      setAuditLogs(updatedLogs)
      localStorage.setItem('flowsync_audit_logs', JSON.stringify(updatedLogs))

      // Reset & Close
      setShowAddUserModal(false)
      setFirstName('')
      setLastName('')
      setEmail('')
      setPassword('')
      setSelectedRole('DEVELOPER')
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Registration failed')
    } finally {
      setSubmitting(false)
    }
  }

  const handleEditUserSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!editingUserId) return
    if (!editFirstName.trim() || !editLastName.trim() || !editEmail.trim()) {
      return toast.error('All fields are required')
    }
    try {
      const res = await api.put(`/users/${editingUserId}`, {
        firstName: editFirstName,
        lastName: editLastName,
        email: editEmail
      })
      const updatedUser = res.data.data
      setUserList(prev => prev.map(u => u.id === editingUserId ? updatedUser : u))
      toast.success('User details updated successfully! 🎉')
      setShowEditModal(false)
      setEditingUserId(null)
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to update user')
    }
  }

  const cpuLoad = `${Math.min(95, Math.max(5, userList.length * 3 + projects.length * 2))}%`
  const activeSessions = `${userList.filter(u => u.active).length} active`

  return (
    <>
      {/* Admin Panel Controls */}
      <div className="flex justify-between items-center mb-5 bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <div>
          <h2 className="text-sm font-black text-slate-805 uppercase tracking-wider">System Administration Control Panel</h2>
          <p className="text-[11px] text-slate-500 mt-0.5">Manage employees, projects, and team assignments</p>
        </div>
        <button 
          onClick={() => setShowAdminConfigModal(true)} 
          className="btn-primary text-xs py-1.5 px-4 bg-slate-900 hover:bg-slate-800 text-white font-extrabold flex items-center gap-1.5 shadow-sm cursor-pointer"
        >
          ⚙️ Admin Config
        </button>
      </div>

      {/* Greeting Section below Admin Panel Controls */}
      <div className="flex items-center justify-between mb-5">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-black text-slate-900 tracking-tight">
              {(() => {
                const hrs = new Date().getHours()
                if (hrs < 12) return 'Good morning'
                if (hrs < 17) return 'Good afternoon'
                return 'Good evening'
              })()}, {currentUser?.fullName?.split(' ')[0]} 👋
            </h1>
            <RoleTag role="ADMIN" />
          </div>
          <p className="text-sm text-slate-500 mt-0.5">
            Admin Dashboard · {new Date().toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
          </p>
        </div>
      </div>

      {/* Top: System Health Metrics */}
      <div className="grid grid-cols-2 gap-3 mb-5">
        <KpiCard label="Active Sessions" value={activeSessions} sub="authenticated tokens" accent="#7C3AED" />
        <KpiCard label="Total Users registered" value={userList.length} sub={`${activeUsers} active accounts`} accent="#2563EB" />
      </div>

      {/* Main Grid: User Statistics Directory Only */}
      <div className="grid grid-cols-1 gap-4 mb-4">
        
        {/* User Statistics Directory */}
        <div className="space-y-4">
          <Section title="User Directory" sub={`${userList.length} registered accounts`} action={isAdmin && (
            <button onClick={() => setShowAddUserModal(true)} className="btn-primary text-[10px] py-1">+ Add User</button>
          )}>
            <div className="space-y-2 max-h-[500px] overflow-y-auto pr-1">
              {userList.map(u => (
                <div key={u.id} className="flex flex-col gap-1 p-2 bg-slate-50 border border-slate-100 rounded hover:bg-white transition-colors">
                  <div className="flex items-center gap-2">
                    <div className="avatar w-6 h-6 text-[9px] flex-shrink-0" style={{ background: u.avatarColor }}>{u.initials}</div>
                    <div className="flex-1 min-w-0">
                      <div className="text-[11px] font-semibold text-slate-700 truncate">{u.fullName}</div>
                      <div className="text-[9.5px] text-slate-400 font-mono truncate">{u.email}</div>
                    </div>
                    <div className="flex items-center gap-1">
                      <button 
                        onClick={() => {
                          setEditingUserId(u.id)
                          setEditFirstName(u.firstName || u.fullName.split(' ')[0] || '')
                          setEditLastName(u.lastName || u.fullName.split(' ')[1] || '')
                          setEditEmail(u.email)
                          setShowEditModal(true)
                        }}
                        className="p-1 text-slate-400 hover:text-teal-600 bg-transparent border-0 cursor-pointer"
                        title="Edit User"
                      >
                        <Edit size={11} />
                      </button>
                      {u.role !== 'ADMIN' && (
                        <button 
                          onClick={() => handleDeleteUser(u.id)}
                          className="p-1 text-slate-400 hover:text-red-650 bg-transparent border-0 cursor-pointer font-bold"
                          title="Delete User"
                        >
                          🗑️
                        </button>
                      )}
                      <span className={`tag text-[8px] ${u.active ? 'tag-green' : 'tag-gray'}`}>
                        {u.active ? 'ACTIVE' : 'INACTIVE'}
                      </span>
                    </div>
                  </div>
                  {(u.lastLoginTime || u.lastLogoutTime) && (
                    <div className="pl-8 flex flex-col gap-0.5 text-[9px] text-slate-500 border-t border-slate-100/50 pt-1">
                      {u.lastLoginTime && (
                        <div>
                          <span className="text-slate-400 font-medium">Last Login:</span> <span className="font-semibold text-slate-650">{formatTimestamp(u.lastLoginTime)}</span>
                        </div>
                      )}
                      {u.lastLogoutTime && (
                        <div>
                          <span className="text-slate-400 font-medium">Last Logout:</span> <span className="font-semibold text-slate-650">{formatTimestamp(u.lastLogoutTime)}</span>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </Section>
        </div>

      </div>

      {/* Add Log Modal */}
      {showAddLogModal && (
        <div className="fixed inset-0 bg-slate-900/60 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-150">
            <div className="bg-slate-800 text-white p-4 flex justify-between items-center">
              <h3 className="font-bold text-sm tracking-wide">Log Custom Audit Event</h3>
              <button type="button" onClick={() => setShowAddLogModal(false)} className="text-slate-400 hover:text-white text-xs font-bold bg-transparent border-0 cursor-pointer">✕</button>
            </div>
            <form onSubmit={handleAddLog} className="p-5 space-y-4">
              <div>
                <label className="field-label">ACTION / EVENT TYPE</label>
                <select 
                  value={newLogAction} 
                  onChange={e => setNewLogAction(e.target.value)}
                  className="field-input text-xs"
                >
                  <option value="USER_LOGIN">USER_LOGIN</option>
                  <option value="FAILED_LOGIN">FAILED_LOGIN</option>
                  <option value="TICKET_STATUS_CHANGED">TICKET_STATUS_CHANGED</option>
                  <option value="PROJECT_MEMBER_ADDED">PROJECT_MEMBER_ADDED</option>
                  <option value="SYSTEM_UPDATE">SYSTEM_UPDATE</option>
                </select>
              </div>
              <div>
                <label className="field-label">DETAIL</label>
                <textarea 
                  value={newLogDetail} 
                  onChange={e => setNewLogDetail(e.target.value)}
                  placeholder="Detail explanation of the event..."
                  className="field-input text-xs h-16 resize-none"
                  required
                />
              </div>
              <button type="submit" className="btn-primary text-xs py-2 w-full justify-center">Record Event</button>
            </form>
          </div>
        </div>
      )}

      {/* Add Login Attempt Modal */}
      {showAddLoginModal && (
        <div className="fixed inset-0 bg-slate-900/60 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-150">
            <div className="bg-slate-800 text-white p-4 flex justify-between items-center">
              <h3 className="font-bold text-sm tracking-wide">Log Authentication Attempt</h3>
              <button type="button" onClick={() => setShowAddLoginModal(false)} className="text-slate-400 hover:text-white text-xs font-bold bg-transparent border-0 cursor-pointer">✕</button>
            </div>
            <form onSubmit={handleAddLogin} className="p-5 space-y-4">
              <div>
                <label className="field-label">USER EMAIL</label>
                <input 
                  type="email" 
                  value={newLoginUser} 
                  onChange={e => setNewLoginUser(e.target.value)}
                  placeholder="user@example.com"
                  className="field-input text-xs"
                  required
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="field-label">ROLE</label>
                  <select 
                    value={newLoginRole} 
                    onChange={e => setNewLoginRole(e.target.value)}
                    className="field-input text-xs"
                  >
                    <option value="DEVELOPER">Developer</option>
                    <option value="TESTER">Tester</option>
                    <option value="MANAGER">Manager</option>
                    <option value="SCRUM_MASTER">Scrum Master</option>
                    <option value="TRAINEE">Trainee</option>
                    <option value="—">—</option>
                  </select>
                </div>
                <div>
                  <label className="field-label">IP ADDRESS</label>
                  <input 
                    type="text" 
                    value={newLoginIp} 
                    onChange={e => setNewLoginIp(e.target.value)}
                    placeholder="10.0.4.55"
                    className="field-input text-xs"
                    required
                  />
                </div>
              </div>
              <div>
                <label className="field-label">STATUS</label>
                <select 
                  value={newLoginOk ? 'true' : 'false'} 
                  onChange={e => setNewLoginOk(e.target.value === 'true')}
                  className="field-input text-xs"
                >
                  <option value="true">SUCCESS</option>
                  <option value="false">BLOCKED / FAILED</option>
                </select>
              </div>
              <button type="submit" className="btn-primary text-xs py-2 w-full justify-center">Record Attempt</button>
            </form>
          </div>
        </div>
      )}

      {/* Add User Modal Overlay */}
      {showAddUserModal && (
        <div className="fixed inset-0 bg-slate-900/60 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-150">
            <div className="bg-slate-800 text-white p-4 flex justify-between items-center">
              <h3 className="font-bold text-sm tracking-wide">Register New User Account</h3>
              <button type="button" onClick={() => setShowAddUserModal(false)} className="text-slate-400 hover:text-white text-xs font-bold bg-transparent border-0 cursor-pointer">✕</button>
            </div>
            
            <form onSubmit={handleAddUserSubmit} className="p-5 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="field-label">FIRST NAME *</label>
                  <input 
                    type="text" 
                    className="field-input text-xs" 
                    placeholder="e.g. John"
                    value={firstName}
                    onChange={e => setFirstName(e.target.value)}
                    required
                  />
                </div>
                <div>
                  <label className="field-label">LAST NAME *</label>
                  <input 
                    type="text" 
                    className="field-input text-xs" 
                    placeholder="e.g. Doe"
                    value={lastName}
                    onChange={e => setLastName(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div>
                <label className="field-label">EMAIL ADDRESS *</label>
                <input 
                  type="email" 
                  className="field-input text-xs" 
                  placeholder="e.g. john.doe@flowsync.com"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  required
                />
              </div>

              <div>
                <label className="field-label">PASSWORD *</label>
                <input 
                  type="password" 
                  className="field-input text-xs" 
                  placeholder="••••••••"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  required
                />
              </div>

              <div>
                <label className="field-label">ROLE ASSIGNMENT *</label>
                <select 
                  className="field-input text-xs" 
                  value={selectedRole} 
                  onChange={e => setSelectedRole(e.target.value)}
                  required
                >
                  <option value="ADMIN">System Admin</option>
                  <option value="SCRUM_MASTER">Scrum Master</option>
                  <option value="PROJECT_OWNER">Project Owner</option>
                  <option value="CTO">Chief Technology Officer (CTO)</option>
                  <option value="VP">Vice President (VP)</option>
                  <option value="MANAGER">Product Manager</option>
                  <option value="DEVELOPER">Software Developer</option>
                  <option value="TESTER">QA Tester</option>
                  <option value="TRAINEE">Engineering Trainee</option>
                </select>
              </div>

              <div className="flex gap-2 justify-end pt-2">
                <button type="button" onClick={() => setShowAddUserModal(false)} className="btn-secondary text-[11px] py-1.5" disabled={submitting}>Cancel</button>
                <button type="submit" className="btn-primary text-[11px] py-1.5" disabled={submitting}>
                  {submitting ? 'Registering...' : 'Register User'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit User Modal Overlay */}
      {showEditModal && (
        <div className="fixed inset-0 bg-slate-900/60 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-150">
            <div className="bg-slate-800 text-white p-4 flex justify-between items-center">
              <h3 className="font-bold text-sm tracking-wide">Edit User Account</h3>
              <button type="button" onClick={() => { setShowEditModal(false); setEditingUserId(null); }} className="text-slate-400 hover:text-white text-xs font-bold bg-transparent border-0 cursor-pointer">✕</button>
            </div>
            
            <form onSubmit={handleEditUserSubmit} className="p-5 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="field-label">FIRST NAME *</label>
                  <input 
                    type="text" 
                    className="field-input text-xs" 
                    value={editFirstName}
                    onChange={e => setEditFirstName(e.target.value)}
                    required
                  />
                </div>
                <div>
                  <label className="field-label">LAST NAME *</label>
                  <input 
                    type="text" 
                    className="field-input text-xs" 
                    value={editLastName}
                    onChange={e => setEditLastName(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div>
                <label className="field-label">EMAIL ADDRESS *</label>
                <input 
                  type="email" 
                  className="field-input text-xs" 
                  value={editEmail}
                  onChange={e => setEditEmail(e.target.value)}
                  required
                />
              </div>

              <div className="flex gap-2 justify-end pt-2">
                <button type="button" onClick={() => { setShowEditModal(false); setEditingUserId(null); }} className="btn-secondary text-[11px] py-1.5">Cancel</button>
                <button type="submit" className="btn-primary text-[11px] py-1.5">Save Changes</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Admin Config Modal Panel */}
      {showAdminConfigModal && (
        <div className="fixed inset-0 bg-slate-900/60 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-lg overflow-hidden animate-in fade-in zoom-in-95 duration-150 border border-slate-200">
            <div className="bg-slate-800 text-white p-4 flex justify-between items-center">
              <div>
                <h3 className="font-bold text-sm tracking-wide">⚙️ Administration Configuration Panel</h3>
                <p className="text-[10px] text-slate-300">Admin-only operations tool</p>
              </div>
              <button 
                type="button" 
                onClick={() => setShowAdminConfigModal(false)} 
                className="text-slate-400 hover:text-white text-xs font-bold bg-transparent border-0 cursor-pointer"
              >
                ✕
              </button>
            </div>

            {/* Modal Tabs Header */}
            <div className="flex border-b border-slate-200 bg-slate-50">
              <button 
                type="button"
                onClick={() => setAdminConfigTab('EMPLOYEE')}
                className={`flex-1 py-2.5 text-center text-xs font-extrabold cursor-pointer border-0 bg-transparent ${adminConfigTab === 'EMPLOYEE' ? 'border-b-2 border-brand text-brand' : 'text-slate-650 hover:bg-slate-100'}`}
              >
                👥 Add Employee
              </button>
              <button 
                type="button"
                onClick={() => setAdminConfigTab('PROJECT')}
                className={`flex-1 py-2.5 text-center text-xs font-extrabold cursor-pointer border-0 bg-transparent ${adminConfigTab === 'PROJECT' ? 'border-b-2 border-brand text-brand' : 'text-slate-650 hover:bg-slate-100'}`}
              >
                🚀 Add Project
              </button>
              <button 
                type="button"
                onClick={() => setAdminConfigTab('MEMBER')}
                className={`flex-1 py-2.5 text-center text-xs font-extrabold cursor-pointer border-0 bg-transparent ${adminConfigTab === 'MEMBER' ? 'border-b-2 border-brand text-brand' : 'text-slate-650 hover:bg-slate-100'}`}
              >
                🔗 Assign Member
              </button>
            </div>

            <div className="p-6 max-h-[400px] overflow-y-auto">
              
              {/* TAB 1: ADD EMPLOYEE FORM */}
              {adminConfigTab === 'EMPLOYEE' && (
                <form onSubmit={handleAddEmployeeSubmit} className="space-y-4">
                  <div className="text-[11px] text-slate-500 bg-slate-50 p-2.5 rounded border border-slate-100">
                    Add new employee. A temporary password starting with <code>EMP-</code> will be generated and dispatched to their email address.
                  </div>
                  <div>
                    <label className="field-label">FULL NAME *</label>
                    <input 
                      type="text" 
                      className="field-input text-xs" 
                      placeholder="e.g. John Doe"
                      value={empName}
                      onChange={e => setEmpName(e.target.value)}
                      required
                    />
                  </div>
                  <div>
                    <label className="field-label">EMAIL ADDRESS *</label>
                    <input 
                      type="email" 
                      className="field-input text-xs" 
                      placeholder="e.g. john.doe@flowsync.com"
                      value={empEmail}
                      onChange={e => setEmpEmail(e.target.value)}
                      required
                    />
                  </div>
                  <div className="flex justify-end mb-2">
                    <label className="flex items-center gap-1.5 cursor-pointer select-none text-[10px] font-bold text-slate-650 uppercase tracking-wide">
                      <input 
                        type="checkbox" 
                        checked={customDeptPos} 
                        onChange={e => {
                          setCustomDeptPos(e.target.checked)
                          if (e.target.checked) {
                            setEmpDept('')
                            setEmpPosition('')
                          } else {
                            setEmpDept('Engineering')
                            setEmpPosition('Developer')
                          }
                        }}
                        className="rounded border-slate-350 text-brand focus:ring-brand"
                      />
                      Use Custom Dept & Position
                    </label>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="field-label">DEPARTMENT *</label>
                      {customDeptPos ? (
                        <input 
                          type="text" 
                          className="field-input text-xs" 
                          placeholder="e.g. Marketing" 
                          value={empDept} 
                          onChange={e => setEmpDept(e.target.value)} 
                          required 
                        />
                      ) : (
                        <select 
                          className="field-input text-xs cursor-pointer"
                          value={empDept}
                          onChange={e => handleDeptChange(e.target.value)}
                          required
                        >
                          {Object.keys(deptPositions).map(d => (
                            <option key={d} value={d}>{d}</option>
                          ))}
                        </select>
                      )}
                    </div>
                    <div>
                      <label className="field-label">POSITION *</label>
                      {customDeptPos ? (
                        <input 
                          type="text" 
                          className="field-input text-xs" 
                          placeholder="e.g. Creative Lead" 
                          value={empPosition} 
                          onChange={e => setEmpPosition(e.target.value)} 
                          required 
                        />
                      ) : (
                        <select 
                          className="field-input text-xs cursor-pointer"
                          value={empPosition}
                          onChange={e => setEmpPosition(e.target.value)}
                          required
                        >
                          {(deptPositions[empDept] || []).map(pos => (
                            <option key={pos} value={pos}>{pos}</option>
                          ))}
                        </select>
                      )}
                    </div>
                  </div>
                  <div className="flex gap-2 justify-end pt-3 border-t border-slate-100">
                    <button type="button" onClick={() => setShowAdminConfigModal(false)} className="btn-secondary text-[11px] py-1.5" disabled={submitting}>Cancel</button>
                    <button type="submit" className="btn-primary text-[11px] py-1.5" disabled={submitting}>
                      {submitting ? 'Registering...' : 'Add Employee'}
                    </button>
                  </div>
                </form>
              )}

              {/* TAB 2: ADD PROJECT FORM */}
              {adminConfigTab === 'PROJECT' && (
                <form onSubmit={handleAddProjectSubmit} className="space-y-4">
                  <div className="text-[11px] text-slate-500 bg-slate-50 p-2.5 rounded border border-slate-100">
                    Create a new system project portfolio.
                  </div>
                  <div className="grid grid-cols-3 gap-4">
                    <div className="col-span-2">
                      <label className="field-label">PROJECT NAME *</label>
                      <input 
                        type="text" 
                        className="field-input text-xs" 
                        placeholder="e.g. Mobile Application"
                        value={projName}
                        onChange={e => setProjName(e.target.value)}
                        required
                      />
                    </div>
                    <div>
                      <label className="field-label">KEY (Max 10) *</label>
                      <input 
                        type="text" 
                        className="field-input text-xs font-mono uppercase" 
                        placeholder="e.g. MOB"
                        maxLength={10}
                        value={projKey}
                        onChange={e => setProjKey(e.target.value)}
                        required
                      />
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="field-label">DURATION</label>
                      <input 
                        type="text" 
                        className="field-input text-xs" 
                        placeholder="e.g. 12 Weeks"
                        value={projDuration}
                        onChange={e => setProjDuration(e.target.value)}
                      />
                    </div>
                    <div>
                      <label className="field-label">GIT REPOSITORY URL</label>
                      <input 
                        type="text" 
                        className="field-input text-xs font-mono" 
                        placeholder="e.g. https://github.com/..."
                        value={projGitRepo}
                        onChange={e => setProjGitRepo(e.target.value)}
                      />
                    </div>
                  </div>
                  <div className="flex gap-2 justify-end pt-3 border-t border-slate-100">
                    <button type="button" onClick={() => setShowAdminConfigModal(false)} className="btn-secondary text-[11px] py-1.5" disabled={submitting}>Cancel</button>
                    <button type="submit" className="btn-primary text-[11px] py-1.5" disabled={submitting}>
                      {submitting ? 'Creating...' : 'Create Project'}
                    </button>
                  </div>
                </form>
              )}

              {/* TAB 3: ASSIGN MEMBER FORM */}
              {adminConfigTab === 'MEMBER' && (
                <form onSubmit={handleAddMemberSubmit} className="space-y-4">
                  <div className="text-[11px] text-slate-500 bg-slate-50 p-2.5 rounded border border-slate-100">
                    Add a registered employee as a team member of a project. This grants them full access to the project details.
                  </div>
                  <div>
                    <label className="field-label">SELECT PROJECT *</label>
                    <select 
                      className="field-input text-xs cursor-pointer"
                      value={memberProjId}
                      onChange={e => setMemberProjId(e.target.value)}
                      required
                    >
                      <option value="">-- Choose Project --</option>
                      {projects.map(p => (
                        <option key={p.id} value={p.id}>{p.emoji} {p.name} ({p.projectKey})</option>
                      ))}
                    </select>
                  </div>

                  {/* Associated Git Repo auto-populated */}
                  <div>
                    <label className="field-label">ASSOCIATED GIT REPOSITORY</label>
                    <input 
                      type="text" 
                      className="field-input text-xs bg-slate-100 cursor-not-allowed text-slate-500 font-mono"
                      value={projects.find(proj => String(proj.id) === memberProjId)?.gitRepo || 'No Git Repository Associated'}
                      disabled
                      readOnly
                    />
                  </div>

                  <div>
                    <label className="field-label">SELECT TEAM MEMBER *</label>
                    <select 
                      className="field-input text-xs cursor-pointer"
                      value={memberUserId}
                      onChange={e => setMemberUserId(e.target.value)}
                      required
                    >
                      <option value="">-- Choose Member --</option>
                      {userList.filter(u => u.role !== 'ADMIN').map(u => (
                        <option key={u.id} value={u.id}>{u.fullName} — {u.role?.replace(/_/g,' ')} {u.department ? `(${u.department})` : ''}</option>
                      ))}
                    </select>
                  </div>
                  <div className="flex gap-2 justify-end pt-3 border-t border-slate-100">
                    <button type="button" onClick={() => setShowAdminConfigModal(false)} className="btn-secondary text-[11px] py-1.5" disabled={submitting}>Cancel</button>
                    <button type="submit" className="btn-primary text-[11px] py-1.5" disabled={submitting}>
                      {submitting ? 'Assigning...' : 'Assign Member'}
                    </button>
                  </div>
                </form>
              )}

            </div>
          </div>
        </div>
      )}

      {selectedReport && (
        <ReportPreviewModal
          isOpen={!!selectedReport}
          onClose={() => setSelectedReport(null)}
          reportTitle={selectedReport.title}
          reportType={selectedReport.type}
          data={selectedReport.data}
        />
      )}
    </>
  )
}
