import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getProjects } from '@/api/projects'
import { Project, PROJECT_STATUS_TAG, PRIORITY_TAG } from '@/types'
import { Search, Plus, FolderKanban } from 'lucide-react'
import CreateProjectModal from '@/components/modals/CreateProjectModal'

export default function ProjectsPage() {
  const [projects, setProjects] = useState<Project[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [showCreateModal, setShowCreateModal] = useState(false)

  const fetchProjects = () => {
    setLoading(true)
    getProjects().then(setProjects).finally(() => setLoading(false))
  }

  useEffect(() => {
    fetchProjects()
  }, [])

  const filtered = projects.filter(p =>
    p.name.toLowerCase().includes(search.toLowerCase()) ||
    p.projectKey.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="page-container">
      <div className="page-header">
        <div><h1 className="page-title">Projects</h1><p className="text-[11.5px] text-slate-400 mt-0.5">{projects.length} projects across your workspace</p></div>
        <button onClick={() => setShowCreateModal(true)} className="btn-primary"><Plus size={12} /> New Project</button>
      </div>
      <div className="flex gap-2 mb-4">
        <div className="relative flex-1 max-w-sm">
          <Search size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input className="field-input pl-9" placeholder="Search projects…" value={search} onChange={e => setSearch(e.target.value)} />
        </div>
      </div>
      {loading ? <div className="flex justify-center py-16"><div className="spinner" style={{width:24,height:24}} /></div> : (
        <div className="grid grid-cols-2 gap-4">
          {filtered.map(p => (
            <Link key={p.id} to={`/projects/${p.id}`} className="card hover:border-blue-300 hover:shadow-sm transition-all block">
              <div className="flex items-start gap-3 mb-3">
                <div className="w-9 h-9 rounded-lg flex items-center justify-center text-lg flex-shrink-0 bg-slate-50">{p.emoji}</div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-0.5">
                    <span className="text-[13px] font-bold text-slate-900">{p.name}</span>
                    <span className={`tag ${PROJECT_STATUS_TAG[p.status]}`}>{p.status}</span>
                  </div>
                  <div className="text-[10.5px] text-slate-400">{p.projectKey} · {p.startDate}</div>
                </div>
                <span className={`tag ${PRIORITY_TAG[p.priority]}`}>{p.priority}</span>
              </div>
              <p className="text-[12px] text-slate-600 leading-relaxed mb-3 line-clamp-2">{p.description}</p>
              <div className="progress-bar mb-1.5">
                <div className="progress-fill bg-blue-500" style={{ width: `${p.progressPercent}%` }} />
              </div>
              <div className="flex justify-between text-[10.5px] text-slate-500 mb-3">
                <span>{p.progressPercent}% complete · Sprint {p.totalSprints} sprints</span>
                <span>Due {p.endDate}</span>
              </div>
              <div className="flex items-center gap-2 pt-2 border-t border-slate-100">
                <div className="flex">
                  {p.members.slice(0,4).map((m, i) => (
                    <div key={m.id} className="avatar w-5 h-5 text-[7.5px] border-2 border-white" style={{ background: m.avatarColor, marginLeft: i > 0 ? -4 : 0 }}>{m.initials}</div>
                  ))}
                </div>
                <span className="text-[10.5px] text-slate-400">{p.members.length} members</span>
                <span className="ml-auto text-[11px] font-semibold text-slate-500">{p.totalTickets} tickets</span>
              </div>
            </Link>
          ))}
        </div>
      )}

      <CreateProjectModal 
        isOpen={showCreateModal} 
        onClose={() => setShowCreateModal(false)} 
        onCreated={fetchProjects} 
      />
    </div>
  )
}
