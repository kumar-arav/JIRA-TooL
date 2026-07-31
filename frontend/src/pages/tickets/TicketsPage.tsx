import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getProjects } from '@/api/projects'
import { getTicketsByProject, deleteTicket } from '@/api/tickets'
import { getSprintsByProject } from '@/api/sprints'
import { Project, Ticket, Sprint, PRIORITY_TAG, STATUS_TAG } from '@/types'
import { Search, Filter } from 'lucide-react'
import toast from 'react-hot-toast'
import { wsClient } from '@/utils/websocket'

export default function TicketsPage() {
  const [projects, setProjects] = useState<Project[]>([])
  const [tickets, setTickets] = useState<Ticket[]>([])
  const [selProject, setSelProject] = useState(0)
  const [sprints, setSprints] = useState<Sprint[]>([])
  const [sprintFilter, setSprintFilter] = useState<number | ''>('')
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [priorityFilter, setPriorityFilter] = useState('')

  const handleDeleteTicket = async (id: number) => {
    if (!window.confirm("Are you sure you want to permanently delete this ticket?")) return
    try {
      await deleteTicket(id)
      setTickets(prev => prev.filter(t => t.id !== id))
      toast.success("Ticket deleted successfully! 🗑️")
    } catch {
      toast.error("Failed to delete ticket")
    }
  }

  useEffect(() => {
    getProjects().then(ps => { setProjects(ps); if (ps.length) setSelProject(ps[0].id) })
  }, [])

  const fetchTicketsAndSprints = () => {
    if (selProject) {
      getTicketsByProject(selProject).then(setTickets)
      getSprintsByProject(selProject).then(setSprints).catch(() => {})
    }
  }

  useEffect(() => {
    fetchTicketsAndSprints()
    setSprintFilter('')
  }, [selProject])

  useEffect(() => {
    if (!selProject) return
    const unsubscribe = wsClient.subscribe((evt) => {
      if (evt.type === 'TICKET_UPDATED' || evt.type === 'SPRINT_UPDATED') {
        fetchTicketsAndSprints()
      }
    })
    return () => unsubscribe()
  }, [selProject])

  const filtered = tickets.filter(t =>
    (!search || t.title.toLowerCase().includes(search.toLowerCase()) || t.ticketKey.includes(search.toUpperCase())) &&
    (!statusFilter || t.status === statusFilter) &&
    (!priorityFilter || t.priority === priorityFilter) &&
    (sprintFilter === '' || t.sprintId === sprintFilter)
  )

  return (
    <div className="page-container">
      <div className="page-header">
        <h1 className="page-title">Tickets ({filtered.length})</h1>
        <select className="field-input w-44 text-[12px]" value={selProject} onChange={e => setSelProject(parseInt(e.target.value))}>
          {projects.map(p => <option key={p.id} value={p.id}>{p.emoji} {p.name}</option>)}
        </select>
      </div>
      <div className="flex gap-2 mb-4 flex-wrap">
        <div className="relative flex-1 min-w-48">
          <Search size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input className="field-input pl-9" placeholder="Search tickets…" value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        <select className="field-input w-36 text-[12px]" value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
          <option value="">All Status</option>
          {['TODO','IN_PROGRESS','IN_REVIEW','TESTING','COMPLETED','CLOSED'].map(s => <option key={s} value={s}>{s.replace('_',' ')}</option>)}
        </select>
        <select className="field-input w-32 text-[12px]" value={priorityFilter} onChange={e => setPriorityFilter(e.target.value)}>
          <option value="">All Priority</option>
          {['CRITICAL','HIGH','MEDIUM','LOW'].map(p => <option key={p} value={p}>{p}</option>)}
        </select>
        <select className="field-input w-36 text-[12px]" value={sprintFilter} onChange={e => setSprintFilter(e.target.value ? Number(e.target.value) : '')}>
          <option value="">All Sprints</option>
          {sprints.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
      </div>
      <div className="card overflow-hidden p-0">
        <table className="w-full">
          <thead>
            <tr>
              <th className="table-header">Key</th>
              <th className="table-header">Title</th>
              <th className="table-header">Status</th>
              <th className="table-header">Priority</th>
              <th className="table-header">Assignee</th>
              <th className="table-header">Points</th>
              <th className="table-header">Due</th>
              <th className="table-header text-center">Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(t => (
              <tr key={t.id} className="hover:bg-slate-50 cursor-pointer">
                <td className="table-cell"><Link to={`/tickets/${t.id}`} className="text-[10px] font-mono font-bold text-blue-600">{t.ticketKey}</Link></td>
                <td className="table-cell"><Link to={`/tickets/${t.id}`} className="text-[12px] font-medium text-slate-800 hover:text-blue-600">{t.title}</Link></td>
                <td className="table-cell"><span className={`tag ${STATUS_TAG[t.status]} text-[9.5px]`}>{t.status.replace('_',' ')}</span></td>
                <td className="table-cell"><span className={`tag ${PRIORITY_TAG[t.priority]} text-[9.5px]`}>{t.priority}</span></td>
                <td className="table-cell">
                  {t.assignee ? (
                    <div className="flex items-center gap-1.5">
                      <div className="avatar w-5 h-5 text-[7.5px]" style={{background:t.assignee.avatarColor}}>{t.assignee.initials}</div>
                      <span className="text-[11px]">{t.assignee.firstName} {t.assignee.lastName}</span>
                    </div>
                  ) : (
                    <span className="text-slate-400 italic text-[11px]">Unassigned</span>
                  )}
                </td>
                <td className="table-cell"><span className="text-[10px] font-bold text-blue-600">{t.storyPoints}sp</span></td>
                <td className="table-cell text-[11px] text-slate-500">
                  {t.dueDate ? t.dueDate : <span className="text-slate-400 italic text-[11px]">No due date</span>}
                </td>
                <td className="table-cell text-center">
                  <button 
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDeleteTicket(t.id);
                    }}
                    className="p-1 text-slate-400 hover:text-red-650 bg-transparent border-0 cursor-pointer font-bold text-xs"
                    title="Delete Ticket"
                  >
                    🗑️
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {filtered.length === 0 && <div className="text-center py-12 text-slate-400 text-sm">No tickets found</div>}
      </div>
    </div>
  )
}
