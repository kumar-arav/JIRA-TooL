import { ReactNode } from 'react'
import { TrendingUp, TrendingDown, Minus } from 'lucide-react'

// ── KPI Card ────────────────────────────────────────────────────────────────
export function KpiCard({
  label, value, sub, trend, accent = '#2563EB', icon,
}: {
  label: string
  value: string | number
  sub?: string
  trend?: 'up' | 'down' | 'flat'
  accent?: string
  icon?: ReactNode
}) {
  return (
    <div className="metric-card">
      <div className="absolute top-0 left-0 right-0 h-[3px] rounded-t-xl" style={{ background: accent }} />
      <div className="flex items-start justify-between mb-1.5">
        <div className="text-[10.5px] font-bold uppercase tracking-wide" style={{ color: accent }}>{label}</div>
        {icon && <div style={{ color: accent }} className="opacity-70">{icon}</div>}
      </div>
      <div className="text-3xl font-black text-slate-900 tracking-tight">{value}</div>
      {sub && (
        <div className="flex items-center gap-1 mt-1.5 text-[11px] text-slate-400">
          {trend === 'up'   && <TrendingUp size={11} className="text-emerald-500" />}
          {trend === 'down' && <TrendingDown size={11} className="text-red-500" />}
          {trend === 'flat' && <Minus size={11} className="text-slate-400" />}
          {sub}
        </div>
      )}
    </div>
  )
}

// ── Section wrapper ─────────────────────────────────────────────────────────
export function Section({
  title, sub, action, children, className = '',
}: { title: string; sub?: string; action?: ReactNode; children: ReactNode; className?: string }) {
  return (
    <div className={`card ${className}`}>
      <div className="flex items-center justify-between mb-3">
        <div>
          <div className="section-title">{title}</div>
          {sub && <div className="text-[11px] text-slate-400 mt-0.5">{sub}</div>}
        </div>
        {action}
      </div>
      {children}
    </div>
  )
}

// ── Thin labeled progress row (used for workload / timeline lists) ─────────
export function ProgressRow({
  label, pct, color, rightLabel, avatarColor, avatarText,
}: { label: string; pct: number; color: string; rightLabel?: string; avatarColor?: string; avatarText?: string }) {
  return (
    <div className="flex items-center gap-2 mb-2">
      {avatarText && (
        <div className="avatar w-6 h-6 text-[9px] flex-shrink-0" style={{ background: avatarColor }}>{avatarText}</div>
      )}
      <div className="w-24 text-[11px] font-semibold text-slate-700 truncate">{label}</div>
      <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
        <div className="h-full rounded-full transition-all" style={{ width: `${pct}%`, background: color }} />
      </div>
      <div className="text-[10.5px] font-bold w-10 text-right" style={{ color }}>{rightLabel ?? `${pct}%`}</div>
    </div>
  )
}

import { Inbox, ShieldAlert, Award, FolderOpen, UserCheck, CalendarDays } from 'lucide-react'

// ── Empty state ──────────────────────────────────────────────────────────────
export function EmptyRow({ text, type }: { text: string; type?: 'tasks' | 'bugs' | 'roadmap' | 'approvals' | 'portfolio' | 'default' }) {
  let Icon = Inbox;
  let colorClass = 'text-slate-400 bg-slate-50/30 border-slate-200/50';
  
  if (type === 'tasks') {
    Icon = CalendarDays;
    colorClass = 'text-amber-600 bg-amber-50/30 border-amber-200/50';
  } else if (type === 'bugs') {
    Icon = ShieldAlert;
    colorClass = 'text-rose-600 bg-rose-50/30 border-rose-200/50';
  } else if (type === 'roadmap') {
    Icon = Award;
    colorClass = 'text-indigo-600 bg-indigo-50/30 border-indigo-200/50';
  } else if (type === 'approvals') {
    Icon = UserCheck;
    colorClass = 'text-emerald-600 bg-emerald-50/30 border-emerald-200/50';
  } else if (type === 'portfolio') {
    Icon = FolderOpen;
    colorClass = 'text-blue-600 bg-blue-50/30 border-blue-200/50';
  }

  return (
    <div className={`flex flex-col items-center justify-center py-6 px-4 rounded-xl border border-dashed ${colorClass} text-center`}>
      <Icon size={20} className="mb-1.5 opacity-80" />
      <span className="text-[11px] font-semibold leading-relaxed">{text}</span>
    </div>
  )
}

// ── Role badge pill for headers ─────────────────────────────────────────────
export function RoleTag({ role }: { role: string }) {
  return <span className="tag tag-purple">{role.replace('_', ' ')}</span>
}
