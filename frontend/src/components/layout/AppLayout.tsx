import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { RootState } from '@/store'
import { logout } from '@/store/slices/authSlice'
import { toggleSidebar } from '@/store/slices/uiSlice'
import {
  LayoutDashboard, FolderKanban, Timer, Sparkles, Users,
  KanbanSquare, Ticket, BarChart2, Bell, LogOut, Menu, Plus
} from 'lucide-react'
import { useState, useEffect } from 'react'
import { getUnreadCount } from '@/api/notifications'
import CreateTicketModal from '../modals/CreateTicketModal'
import { logoutUser } from '@/api/auth'
import { wsClient } from '@/utils/websocket'
import toast from 'react-hot-toast'

// ── Role-based navigation configuration ─────────────────────────────────────
// Each nav item declares which roles can see it. Sidebar is filtered per user.
type NavItem = { to: string; icon: any; label: string; badge?: string; roles: string[] }
type NavSection = { label: string; items: NavItem[] }

const ALL = ['ADMIN', 'SCRUM_MASTER', 'PROJECT_OWNER', 'CTO', 'VP', 'MANAGER', 'DEVELOPER', 'TESTER', 'TRAINEE']
const PLANNERS = ['ADMIN', 'SCRUM_MASTER', 'PROJECT_OWNER']
const LEADERSHIP = ['ADMIN', 'SCRUM_MASTER', 'PROJECT_OWNER', 'CTO', 'VP', 'MANAGER']

const NAV: NavSection[] = [
  {
    label: 'Overview', items: [
      { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard', roles: ALL },
      { to: '/projects', icon: FolderKanban, label: 'Projects', roles: LEADERSHIP.concat('DEVELOPER', 'TESTER') },
    ]
  },
  {
    label: 'Planning', items: [
      { to: '/sprints', icon: Timer, label: 'Sprints', roles: LEADERSHIP },
      { to: '/ai', icon: Sparkles, label: 'AI Planner', badge: 'AI', roles: PLANNERS },
      { to: '/resources', icon: Users, label: 'Resources', roles: LEADERSHIP },
    ]
  },
  {
    label: 'Work', items: [
      { to: '/kanban', icon: KanbanSquare, label: 'Kanban Board', roles: ALL },
      { to: '/tickets', icon: Ticket, label: 'Tickets', roles: ALL },
    ]
  },
  {
    label: 'Insights', items: [
      { to: '/reports', icon: BarChart2, label: 'Reports', roles: LEADERSHIP },
      { to: '/notifications', icon: Bell, label: 'Notifications', roles: ALL },
    ]
  },
]

export default function AppLayout() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const user = useSelector((s: RootState) => s.auth.user)
  const sidebarOpen = useSelector((s: RootState) => s.ui.sidebarOpen)
  const [unread, setUnread] = useState(0)
  const [showTicketModal, setShowTicketModal] = useState(false)
  const role = user?.role || 'DEVELOPER'

  useEffect(() => {
    wsClient.connect()
    getUnreadCount().then(setUnread).catch(() => { })

    const unsubscribe = wsClient.subscribe((data) => {
      if (data.type === 'NOTIFICATION_RECEIVED' && data.recipientId === user?.id) {
        setUnread(prev => prev + 1)
        toast.success(`🔔 ${data.title}\n${data.message}`, { duration: 5000 })
      } else if (data.type === 'USER_REGISTERED' && user?.role === 'ADMIN') {
        toast.success(`🛡️ Security: User ${data.user} registered as ${data.role}!`)
      } else if (data.type === 'USER_LOGIN' && user?.role === 'ADMIN') {
        toast.success(`🔑 Security: User ${data.user} logged in!`)
      } else if (data.type === 'TICKET_UPDATED') {
        window.dispatchEvent(new CustomEvent('ticket-updated', { detail: data }))
      }
    })

    const interval = setInterval(() => getUnreadCount().then(setUnread).catch(() => { }), 60000)
    return () => {
      unsubscribe()
      clearInterval(interval)
    }
  }, [user])

  const handleLogout = () => {
    if (user?.email) {
      logoutUser(user.email).catch(() => { })
    }
    dispatch(logout())
    navigate('/login')
  }

  // Filter nav per role — sections with no visible items disappear entirely
  const visibleNav = NAV
    .map(section => ({ ...section, items: section.items.filter(i => i.roles.includes(role)) }))
    .filter(section => section.items.length > 0)

  const canCreateTickets = ['ADMIN', 'SCRUM_MASTER', 'PROJECT_OWNER', 'MANAGER'].includes(role)
  const canUseAI = ['ADMIN', 'SCRUM_MASTER', 'PROJECT_OWNER'].includes(role)

  return (
    <div className="flex h-screen overflow-hidden" style={{ background: '#FAF6EE' }}>
      <aside className={`${sidebarOpen ? 'w-56' : 'w-0 overflow-hidden'} flex-shrink-0 bg-slate-900 text-slate-300 flex flex-col transition-all duration-200 z-30`}>
        <div className="flex items-center gap-2.5 px-4 py-3.5 border-b border-slate-800 bg-slate-950 flex-shrink-0">
          <div className="w-7 h-7 bg-brand rounded-lg flex items-center justify-center text-white text-xs font-black">IS</div>
          <span className="text-sm font-black text-slate-100 tracking-tight">IntelliSprint</span>
          <span className="text-[9px] font-bold bg-gradient-to-r from-brand to-fs-amber text-white px-1.5 py-0.5 rounded">AI</span>
        </div>

        <nav className="flex-1 overflow-y-auto py-2 px-2">
          {visibleNav.map(section => (
            <div key={section.label} className="py-1">
              <div className="px-3.5 py-1 text-[9px] font-bold text-slate-500 uppercase tracking-wider">{section.label}</div>
              {section.items.map(item => (
                <NavLink key={item.to} to={item.to} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                  <item.icon size={14} className="flex-shrink-0 opacity-70" />
                  <span className="flex-1">{item.label}</span>
                  {item.badge && (
                    <span className="text-[8px] font-bold bg-gradient-to-r from-brand to-fs-amber text-white px-1.5 py-0.5 rounded">{item.badge}</span>
                  )}
                  {item.label === 'Notifications' && unread > 0 && (
                    <span className="text-[9px] font-bold bg-red-500 text-white px-1 py-0.5 rounded-full min-w-[16px] text-center">{unread}</span>
                  )}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        <div className="border-t border-slate-800 p-3 flex-shrink-0 bg-slate-950">
          <div className="flex items-center gap-2 p-1.5 rounded-xl hover:bg-slate-900 cursor-pointer group">
            <div className="avatar w-7 h-7 text-[10px] flex-shrink-0" style={{ background: user?.avatarColor || '#E65F2B' }}>{user?.initials}</div>
            <div className="flex-1 min-w-0">
              <div className="text-xs font-bold text-slate-200 truncate">{user?.fullName}</div>
              <div className="text-[10px] text-slate-400 font-medium">{user?.role?.replace('_', ' ')}</div>
            </div>
            <button onClick={handleLogout} className="opacity-0 group-hover:opacity-100 transition-opacity p-0.5 text-slate-400 hover:text-red-500">
              <LogOut size={12} />
            </button>
          </div>
        </div>
      </aside>

      <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
        <header className="h-14 bg-white border-b border-slate-200 flex items-center px-4 gap-3 flex-shrink-0 z-20">
          <button onClick={() => dispatch(toggleSidebar())} className="btn-ghost p-1.5"><Menu size={15} /></button>
          <div className="flex-1" />
          {canUseAI && (
            <NavLink to="/ai" className="btn-secondary text-[11px] gap-1.5"><Sparkles size={12} /> AI Generate</NavLink>
          )}
          {canCreateTickets && (
            <button type="button" onClick={() => setShowTicketModal(true)} className="btn-primary text-[11px] gap-1.5"><Plus size={12} /> New Ticket</button>
          )}
          <NavLink to="/notifications" className="relative p-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 flex items-center">
            <Bell size={15} className="text-slate-500" />
            {unread > 0 && (
              <span className="absolute -top-1 -right-1 w-4 h-4 bg-red-500 text-white text-[7px] font-bold rounded-full flex items-center justify-center border-2 border-white">
                {unread > 9 ? '9+' : unread}
              </span>
            )}
          </NavLink>
        </header>
        <main className="flex-1 overflow-y-auto"><Outlet /></main>
      </div>

      <CreateTicketModal
        isOpen={showTicketModal}
        onClose={() => setShowTicketModal(false)}
        onCreated={() => window.location.reload()}
      />
    </div>
  )
}
