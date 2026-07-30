import { useState, useEffect } from 'react'
import { getProjects } from '@/api/projects'
import { getSprintsByProject } from '@/api/sprints'
import { getUsers } from '@/api/users'
import { createTicket } from '@/api/tickets'
import { Project, Sprint, User } from '@/types'
import toast from 'react-hot-toast'

interface CreateTicketModalProps {
  isOpen: boolean
  onClose: () => void
  onCreated: () => void
  defaultProjectId?: number | ''
  defaultSprintId?: number | ''
}

export default function CreateTicketModal({ 
  isOpen, 
  onClose, 
  onCreated,
  defaultProjectId = '',
  defaultSprintId = ''
}: CreateTicketModalProps) {
  const [projects, setProjects] = useState<Project[]>([])
  const [sprints, setSprints] = useState<Sprint[]>([])
  const [users, setUsers] = useState<User[]>([])
  
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [storyPoints, setStoryPoints] = useState(3)
  const [priority, setPriority] = useState('MEDIUM')
  const [dueDate, setDueDate] = useState('')
  const [selectedProjectId, setSelectedProjectId] = useState<number | ''>(defaultProjectId)
  const [selectedSprintId, setSelectedSprintId] = useState<number | ''>(defaultSprintId)
  const [selectedAssigneeId, setSelectedAssigneeId] = useState<number | ''>('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (isOpen) {
      getProjects().then(setProjects).catch(() => toast.error('Failed to load projects'))
      getUsers().then(setUsers).catch(() => toast.error('Failed to load users'))
      setSelectedProjectId(defaultProjectId)
      setSelectedSprintId(defaultSprintId)
    }
  }, [isOpen, defaultProjectId, defaultSprintId])

  useEffect(() => {
    if (selectedProjectId) {
      getSprintsByProject(selectedProjectId)
        .then(ss => {
          setSprints(ss)
          if (defaultSprintId && ss.some((s: { id: number }) => s.id === defaultSprintId)) {
            setSelectedSprintId(defaultSprintId)
          }
        })
        .catch(() => toast.error('Failed to load sprints for selected project'))
    } else {
      setSprints([])
      setSelectedSprintId('')
    }
  }, [selectedProjectId, defaultSprintId])

  if (!isOpen) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedProjectId) return toast.error('Project selection is required')
    if (!title.trim()) return toast.error('Ticket Title is required')

    setSubmitting(true)
    try {
      await createTicket({
        title,
        description,
        storyPoints,
        priority,
        dueDate: dueDate || null,
        projectId: selectedProjectId,
        sprintId: selectedSprintId || null,
        assigneeId: selectedAssigneeId || null
      })
      toast.success('Ticket created successfully! 🎫')
      onCreated()
      onClose()
      // Reset form
      setTitle('')
      setDescription('')
      setStoryPoints(3)
      setPriority('MEDIUM')
      setDueDate('')
      setSelectedProjectId('')
      setSelectedSprintId('')
      setSelectedAssigneeId('')
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to create ticket')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-slate-900/60 z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-lg overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        <div className="bg-slate-800 text-white p-4 flex justify-between items-center">
          <h3 className="font-bold text-sm tracking-wide">Create New Ticket</h3>
          <button type="button" onClick={onClose} className="text-slate-400 hover:text-white text-xs font-bold bg-transparent border-0 cursor-pointer">✕</button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4 max-h-[80vh] overflow-y-auto">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="field-label">PROJECT *</label>
              <select 
                className="field-input text-xs" 
                value={selectedProjectId} 
                onChange={e => setSelectedProjectId(e.target.value ? Number(e.target.value) : '')}
                required
              >
                <option value="">-- Select Project --</option>
                {projects.map(p => <option key={p.id} value={p.id}>{p.emoji} {p.name}</option>)}
              </select>
            </div>
            <div>
              <label className="field-label">ASSOCIATE SPRINT</label>
              <select 
                className="field-input text-xs" 
                value={selectedSprintId} 
                onChange={e => setSelectedSprintId(e.target.value ? Number(e.target.value) : '')}
                disabled={!selectedProjectId}
              >
                <option value="">-- No Sprint (Backlog) --</option>
                {sprints.map(s => <option key={s.id} value={s.id}>{s.name} ({s.status})</option>)}
              </select>
            </div>
          </div>

          <div>
            <label className="field-label">TICKET TITLE *</label>
            <input 
              type="text" 
              className="field-input text-xs" 
              value={title} 
              onChange={e => setTitle(e.target.value)} 
              placeholder="e.g. Implement user login credentials encryption" 
              required 
            />
          </div>

          <div>
            <label className="field-label">DESCRIPTION</label>
            <textarea 
              className="field-input text-xs resize-none" 
              rows={3} 
              value={description} 
              onChange={e => setDescription(e.target.value)} 
              placeholder="Provide a detailed description of the task requirements..." 
            />
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="field-label">STORY POINTS</label>
              <select className="field-input text-xs" value={storyPoints} onChange={e => setStoryPoints(Number(e.target.value))}>
                <option value={1}>1 pt</option>
                <option value={2}>2 pts</option>
                <option value={3}>3 pts</option>
                <option value={5}>5 pts</option>
                <option value={8}>8 pts</option>
                <option value={13}>13 pts</option>
              </select>
            </div>
            <div>
              <label className="field-label">PRIORITY</label>
              <select className="field-input text-xs" value={priority} onChange={e => setPriority(e.target.value)}>
                <option value="CRITICAL">Critical</option>
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
              </select>
            </div>
            <div>
              <label className="field-label">ASSIGNEE</label>
              <select 
                className="field-input text-xs" 
                value={selectedAssigneeId} 
                onChange={e => setSelectedAssigneeId(e.target.value ? Number(e.target.value) : '')}
              >
                <option value="">-- Unassigned --</option>
                {users.map(u => <option key={u.id} value={u.id}>{u.firstName} {u.lastName}</option>)}
              </select>
            </div>
          </div>

          <div>
            <label className="field-label">DUE DATE</label>
            <input 
              type="date" 
              className="field-input text-xs" 
              value={dueDate} 
              onChange={e => setDueDate(e.target.value)} 
            />
          </div>

          <div className="flex gap-2 justify-end pt-3 border-t border-slate-100">
            <button type="button" onClick={onClose} className="btn-secondary text-[11px] py-1.5" disabled={submitting}>Cancel</button>
            <button type="submit" className="btn-primary text-[11px] py-1.5" disabled={submitting}>
              {submitting ? 'Creating...' : 'Create Ticket'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
