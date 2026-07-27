import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { getProjects } from '@/api/projects'
import { getSprintsByProject } from '@/api/sprints'
import { getTicketsBySprint, updateStatus } from '@/api/tickets'
import { Project, Sprint, Ticket, TicketStatus } from '@/types'
import toast from 'react-hot-toast'
import { Plus } from 'lucide-react'
import CreateTicketModal from '@/components/modals/CreateTicketModal'
import { useSelector } from 'react-redux'
import { RootState } from '@/store'

const COLUMNS: { key: TicketStatus; label: string; color: string }[] = [
  { key: 'TODO',       label: 'Todo',       color: '#94A3B8' },
  { key: 'IN_PROGRESS',label: 'In Progress',color: '#2563EB' },
  { key: 'IN_REVIEW',  label: 'In Review',  color: '#D97706' },
  { key: 'TESTING',    label: 'Testing',    color: '#7C3AED' },
  { key: 'CLOSED',     label: 'Closed',     color: '#059669' },
]
const PRIORITY_COLOR: Record<string,string> = { CRITICAL:'#DC2626', HIGH:'#D97706', MEDIUM:'#2563EB', LOW:'#94A3B8' }

export default function KanbanPage() {
  const { sprintId } = useParams()
  const [projects, setProjects] = useState<Project[]>([])
  const [sprints, setSprints]   = useState<Sprint[]>([])
  const [tickets, setTickets]   = useState<Ticket[]>([])
  const [selProject, setSelProject] = useState(0)
  const [selSprint, setSelSprint]   = useState(parseInt(sprintId||'0'))
  const [dragging, setDragging] = useState<number|null>(null)
  const [showTicketModal, setShowTicketModal] = useState(false)

  const user = useSelector((s: RootState) => s.auth.user)
  const isDevOrTester = user && ['DEVELOPER', 'TESTER'].includes(user.role)

  const fetchTickets = () => {
    if (selSprint) getTicketsBySprint(selSprint).then(setTickets)
  }

  useEffect(() => {
    getProjects().then(ps => {
      setProjects(ps)
      if (!selProject && ps.length) setSelProject(ps[0].id)
    })
  }, [])

  useEffect(() => {
    if (selProject) getSprintsByProject(selProject).then(ss => {
      setSprints(ss)
      if (!selSprint && ss.length) setSelSprint(ss.find((s: Sprint)=>s.status==='ACTIVE')?.id || ss[0].id)
    })
  }, [selProject])

  useEffect(() => {
    fetchTickets()
    
    const handleTicketUpdate = () => {
      fetchTickets()
    }
    window.addEventListener('ticket-updated', handleTicketUpdate)
    return () => window.removeEventListener('ticket-updated', handleTicketUpdate)
  }, [selSprint])

  const handleDrop = async (e: React.DragEvent, targetStatus: TicketStatus) => {
    e.preventDefault()
    if (!dragging) return

    const currentSprintObj = sprints.find(s => s.id === selSprint)
    if (isDevOrTester && (!currentSprintObj || currentSprintObj.status !== 'ACTIVE')) {
      toast.error('Developers and Testers can only work on tickets in active sprints')
      setDragging(null)
      return
    }

    try {
      await updateStatus(dragging, { status: targetStatus })
      setTickets(prev => prev.map(t => t.id === dragging ? {...t, status: targetStatus} : t))
      toast.success(`Moved to ${targetStatus.replace('_',' ')}`)
    } catch { toast.error('Failed to update status') }
    setDragging(null)
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <h1 className="page-title">Kanban Board</h1>
        <div className="flex gap-2">
          <select className="field-input w-44 text-[12px]" value={selProject} onChange={e => setSelProject(parseInt(e.target.value))}>
            {projects.map(p => <option key={p.id} value={p.id}>{p.emoji} {p.name}</option>)}
          </select>
          <select className="field-input w-36 text-[12px]" value={selSprint} onChange={e => setSelSprint(parseInt(e.target.value))}>
            {sprints.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
          <button onClick={() => setShowTicketModal(true)} className="btn-primary text-[11px] gap-1.5"><Plus size={12} /> New Ticket</button>
        </div>
      </div>

      {/* Stats strip */}
      <div className="flex gap-2 mb-3 flex-wrap">
        {COLUMNS.map(col => {
          const count = tickets.filter(t => t.status === col.key).length
          return <div key={col.key} className="flex items-center gap-1.5 px-2.5 py-1 bg-white border border-slate-200 rounded-lg">
            <div className="w-2 h-2 rounded-full" style={{ background: col.color }} />
            <span className="text-[11px] font-semibold text-slate-600">{col.label}</span>
            <span className="text-[10px] font-bold bg-slate-100 text-slate-500 px-1.5 rounded">{count}</span>
          </div>
        })}
      </div>

      {/* Board */}
      <div className="flex gap-3 overflow-x-auto pb-4">
        {COLUMNS.map(col => {
          const colTickets = tickets.filter(t => t.status === col.key)
          return (
            <div key={col.key} className="kanban-col flex-shrink-0"
              onDragOver={e => e.preventDefault()}
              onDrop={e => handleDrop(e, col.key)}>
              <div className="flex items-center justify-between pb-2 mb-1 border-b border-slate-200">
                <div className="flex items-center gap-1.5">
                  <div className="w-2 h-2 rounded-full" style={{ background: col.color }} />
                  <span className="text-[10.5px] font-bold text-slate-700 uppercase tracking-wide">{col.label}</span>
                  <span className="text-[9px] font-bold bg-slate-200 text-slate-600 px-1.5 py-0.5 rounded">{colTickets.length}</span>
                </div>
              </div>
              {colTickets.map(t => (
                <div key={t.id} className="kanban-card"
                  draggable
                  onDragStart={() => setDragging(t.id)}>
                  <div className="absolute top-2 right-2 w-2 h-2 rounded-full" style={{ background: PRIORITY_COLOR[t.priority] }} />
                  <div className="text-[9.5px] font-mono font-bold text-slate-400 mb-1">{t.ticketKey}</div>
                  <div className="text-[11.5px] font-semibold text-slate-800 leading-snug mb-2 pr-3">{t.title}</div>
                  <div className="flex items-center justify-between">
                    <span className="text-[9.5px] font-bold bg-blue-50 text-blue-700 px-1.5 py-0.5 rounded">{t.storyPoints}sp</span>
                    {t.assignee && (
                      <div className="avatar w-5 h-5 text-[8px]" style={{ background: t.assignee.avatarColor }}>{t.assignee.initials}</div>
                    )}
                  </div>
                  {t.dueDate && <div className="text-[9px] text-slate-400 mt-1">Due {t.dueDate}</div>}
                </div>
              ))}
            </div>
          )
        })}
      </div>
      <div className="text-[11px] text-slate-400 text-center py-1">
        ↔ Drag cards between columns to update ticket status
      </div>

      <CreateTicketModal 
        isOpen={showTicketModal} 
        onClose={() => setShowTicketModal(false)} 
        onCreated={fetchTickets} 
      />
    </div>
  )
}
