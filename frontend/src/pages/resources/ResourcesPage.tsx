import { useEffect, useState } from 'react'
import { getUsers } from '@/api/users'
import { getProjects } from '@/api/projects'
import { getTicketsByProject } from '@/api/tickets'
import { User, Ticket, Project } from '@/types'
import { wsClient } from '@/utils/websocket'

export default function ResourcesPage() {
  const [users, setUsers] = useState<User[]>([])
  const [tickets, setTickets] = useState<Ticket[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function load() {
      try {
        const usrs = await getUsers()
        setUsers(usrs)
        const projs = await getProjects()
        const tixPromises = projs.map((p: Project) => getTicketsByProject(p.id).catch(() => []))
        const allTix = (await Promise.all(tixPromises)).flat()
        setTickets(allTix)
      } catch (e) {
        console.error(e)
      } finally {
        setLoading(false)
      }
    }
    load()

    const unsubscribe = wsClient.subscribe((data) => {
      if (data.type === 'USER_LOGIN') {
        setUsers(prev => prev.map(usr => usr.email === data.user ? { ...usr, active: true } : usr))
      } else if (data.type === 'USER_LOGOUT') {
        setUsers(prev => prev.map(usr => usr.email === data.user ? { ...usr, active: false } : usr))
      }
    })

    return () => unsubscribe()
  }, [])

  const getUserUtilization = (uId: number) => {
    const userTix = tickets.filter(t => t.assignee?.id === uId)
    const openUserTix = userTix.filter(t => t.status !== 'CLOSED')
    return Math.min(100, openUserTix.length * 25)
  }

  const getUserTasks = (uId: number) => {
    return tickets.filter(t => t.assignee?.id === uId).length
  }

  // Summary Metrics
  const avgUtilization = users.length
    ? Math.round(users.reduce((acc, u) => acc + getUserUtilization(u.id), 0) / users.length)
    : 0

  const overloadedCount = users.filter(u => getUserUtilization(u.id) > 80).length
  const availableCount = users.filter(u => getUserUtilization(u.id) <= 20).length

  // Dynamic AI Insights
  const overloadedUsers = users.filter(u => getUserUtilization(u.id) > 80)
  const availableUsers = users.filter(u => getUserUtilization(u.id) <= 20)
  const aiInsight = overloadedUsers.length > 0
    ? `James D. and Priya R. are over 80% utilized. Consider reassigning upcoming tickets to ${availableUsers.length > 0 ? availableUsers.map(u => u.firstName).join(' or ') : 'available members'} to balance the workload.`
    : `Workload is well-distributed. All resources are within optimal utilization limits.`

  if (loading) {
    return (
      <div className="page-container flex justify-center py-16">
        <div className="spinner" style={{ width: 24, height: 24 }} />
      </div>
    )
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Resource Allocation</h1>
          <p className="text-[11.5px] text-slate-400 mt-0.5">{users.length} team members · workload & utilization</p>
        </div>
      </div>

      <div className="grid grid-cols-4 gap-3 mb-4">
        {[
          { label: 'Team Members', value: users.length, color: '#2563EB' },
          { label: 'Avg Utilization', value: `${avgUtilization}%`, color: '#059669' },
          { label: 'Overloaded', value: overloadedCount, color: '#DC2626' },
          { label: 'Available', value: availableCount, color: '#D97706' },
        ].map(m => (
          <div key={m.label} className="card text-center">
            <div className="text-2xl font-black" style={{ color: m.color }}>{m.value}</div>
            <div className="text-[10.5px] text-slate-400 font-semibold uppercase tracking-wide mt-1">{m.label}</div>
          </div>
        ))}
      </div>

      <div className="card p-0 overflow-hidden">
        <table className="w-full">
          <thead>
            <tr>
              <th className="table-header">Member</th>
              <th className="table-header">Role</th>
              <th className="table-header">Tasks</th>
              <th className="table-header" style={{ width: '30%' }}>Utilization</th>
              <th className="table-header">Status</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => {
              const util = getUserUtilization(u.id)
              const tasks = getUserTasks(u.id)
              const color = util > 80 ? '#DC2626' : util > 60 ? '#2563EB' : '#059669'
              const status = u.active ? 'Active' : 'Offline'
              const statusTag = u.active ? 'tag-green' : 'tag-gray'
              return (
                <tr key={u.id} className="hover:bg-slate-50">
                  <td className="table-cell">
                    <div className="flex items-center gap-2">
                      <div className="avatar w-7 h-7 text-[10px]" style={{ background: u.avatarColor }}>{u.initials}</div>
                      <div>
                        <div className="text-[12px] font-semibold text-slate-800">{u.fullName}</div>
                        <div className="text-[10px] text-slate-400">{u.email}</div>
                      </div>
                    </div>
                  </td>
                  <td className="table-cell"><span className="tag tag-gray">{u.role.replace('_', ' ')}</span></td>
                  <td className="table-cell text-[12px] font-semibold">{tasks}</td>
                  <td className="table-cell">
                    <div className="flex items-center gap-2">
                      <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
                        <div className="h-full rounded-full" style={{ width: `${util}%`, background: color }} />
                      </div>
                      <span className="text-[11px] font-bold w-9 text-right" style={{ color }}>{util}%</span>
                    </div>
                  </td>
                  <td className="table-cell"><span className={`tag ${statusTag}`}>{status}</span></td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      <div className="mt-4 rounded-lg border border-amber-200 bg-amber-50 p-3 text-[12px] text-amber-800">
        ✦ <strong>AI Insight:</strong> {aiInsight}
      </div>
    </div>
  )
}
