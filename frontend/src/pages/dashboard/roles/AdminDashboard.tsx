import { useState, useEffect } from 'react'
import { KpiCard, Section, EmptyRow } from '@/components/dashboard/shared'
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
      {/* Top: System Health Metrics */}
      <div className="grid grid-cols-4 gap-3 mb-5">
        <KpiCard label="System Status" value="99.9%" sub="all services online" accent="#0EA5E9" />
        <KpiCard label="Server CPU Load" value={cpuLoad} sub="optimal operation" accent="#059669" />
        <KpiCard label="Active Sessions" value={activeSessions} sub="authenticated tokens" accent="#7C3AED" />
        <KpiCard label="Total Users registered" value={userList.length} sub={`${activeUsers} active accounts`} accent="#2563EB" />
      </div>

      {/* Main Grid: Left (User Stats), Center (Permission Matrix), Right (Security Monitoring) */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-4 mb-4 items-start">
        
        {/* Left Column: User Statistics Directory */}
        <div className="lg:col-span-1 space-y-4">
          <Section title="User Directory" sub={`${userList.length} registered accounts`} action={isAdmin && (
            <button onClick={() => setShowAddUserModal(true)} className="btn-primary text-[10px] py-1">+ Add User</button>
          )}>
            <div className="space-y-2 max-h-[300px] overflow-y-auto pr-1">
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
                      <span className={`tag text-[8px] ${u.active ? 'tag-green' : 'tag-gray'}`}>
                        {u.active ? 'ACTIVE' : 'INACTIVE'}
                      </span>
                    </div>
                  </div>
                  {(u.lastLoginTime || u.lastLogoutTime) && (
                    <div className="pl-8 flex flex-col gap-0.5 text-[9px] text-slate-500 border-t border-slate-100/50 pt-1">
                      {u.lastLoginTime && (
                        <div>
                          <span className="text-slate-400 font-medium">Last Login:</span> <span className="font-semibold text-slate-600">{formatTimestamp(u.lastLoginTime)}</span>
                        </div>
                      )}
                      {u.lastLogoutTime && (
                        <div>
                          <span className="text-slate-400 font-medium">Last Logout:</span> <span className="font-semibold text-slate-600">{formatTimestamp(u.lastLogoutTime)}</span>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </Section>
        </div>

        {/* Center Column: Permission Matrix (spans 2 columns) */}
        <div className="lg:col-span-2 space-y-4">
          <Section title="RBAC Permission Matrix" sub="Role Based Access Control policy enforcement settings">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-slate-50">
                    <th className="table-header text-[9.5px]">Policy Feature</th>
                    {ROLE_HEADERS.map(h => <th key={h} className="table-header text-[9.5px] text-center px-1">{h}</th>)}
                  </tr>
                </thead>
                <tbody>
                  {RBAC_MATRIX.map(([label, ...perms]) => (
                    <tr key={label as string} className="hover:bg-slate-50/50">
                      <td className="table-cell font-semibold text-[11px] text-slate-700">{label as string}</td>
                      {perms.map((p, idx) => (
                        <td key={idx} className="table-cell text-center px-1">
                          <span className={p ? 'text-emerald-600 font-black text-xs' : 'text-slate-300 text-xs'}>
                            {p ? '✓' : '·'}
                          </span>
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Section>
        </div>

        {/* Right Column: Security Monitoring Dashboard */}
        <div className="lg:col-span-1 space-y-4">
          <Section 
            title="Security Monitoring" 
            sub="Recent authentication attempts log"
            action={isAdmin && (
              <button onClick={() => setShowAddLoginModal(true)} className="btn-primary text-[10px] py-1">
                + Log Attempt
              </button>
            )}
          >
            <div className="space-y-2.5 max-h-[300px] overflow-y-auto pr-1">
              {loginActivity.map((l, i) => (
                <div key={i} className={`flex flex-col gap-1 p-2 rounded-lg border ${l.ok ? 'bg-emerald-50/30 border-emerald-100' : 'bg-red-50/30 border-red-100'}`}>
                  <div className="flex justify-between items-center text-[10px]">
                    <span className="font-semibold text-slate-700 truncate max-w-[120px]">{l.user}</span>
                    <span className="text-slate-400 font-mono text-[9px]">{l.ip}</span>
                  </div>
                  <div className="flex justify-between items-center text-[9.5px] mt-0.5">
                    <span className="text-slate-400 font-semibold">{l.role}</span>
                    <span className={`tag text-[8px] font-bold ${l.ok ? 'tag-green' : 'tag-red'}`}>
                      {l.ok ? 'SUCCESS' : 'BLOCKED'}
                    </span>
                  </div>
                </div>
              ))}
              {loginActivity.length === 0 && (
                <div className="text-center py-6 text-xs text-slate-400 italic">No login attempts logged.</div>
              )}
            </div>
          </Section>
        </div>

      </div>

      {/* Bottom: Audit Logs */}
      <Section 
        title="Audit Logs Trail" 
        sub="System-wide action and telemetry compliance tracking"
        action={
          <div className="flex gap-2">
            {isAdmin && <button onClick={() => setShowAddLogModal(true)} className="btn-primary text-[10px] py-1">+ Log Event</button>}
            <button onClick={() => handleOpenReport('Security Report')} className="btn-secondary text-[10px] py-1">↓ Export Security Audit</button>
          </div>
        }
      >
        <div className="space-y-2 max-h-[220px] overflow-y-auto pr-1">
          {auditLogs.map((a, i) => (
            <div key={i} className="flex items-start gap-2.5 py-2 border-b border-slate-100 last:border-0 text-xs">
              <span className={`tag text-[8px] font-bold mt-0.5 ${a.action.includes('FAILED') || a.action.includes('BLOCKED') ? 'tag-red' : 'tag-blue'}`}>
                {a.action}
              </span>
              <span className="flex-1 text-slate-600 leading-normal">{a.detail}</span>
              <span className="text-[10px] text-slate-400 font-mono flex-shrink-0">{a.when}</span>
            </div>
          ))}
          {auditLogs.length === 0 && (
            <div className="text-center py-6 text-xs text-slate-400 italic">No audit events recorded.</div>
          )}
        </div>
      </Section>

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
