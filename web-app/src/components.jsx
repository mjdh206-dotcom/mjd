import { useEffect, useState } from 'react'
import { money } from './utils'

export function Card({ title, badge, children, action, style, className = '' }) {
  return (
    <section className={`card ${className}`} style={style}>
      {(title || action) && (
        <div className="card-head">
          <h3>{title} {badge && <span className="badge badge-gold" style={{ marginInlineStart: 8 }}>{badge}</span>}</h3>
          {action}
        </div>
      )}
      {children}
    </section>
  )
}

export function Stat({ icon, label, value, sub, tone = 'cream' }) {
  return (
    <div className="card stat">
      <div className="ico" style={tone === 'gold' ? { background: 'linear-gradient(135deg,#FFD54F,#FFE082)' } : undefined}>{icon}</div>
      <div style={{ minWidth: 0 }}>
        <p>{label}</p>
        <b style={{ fontSize: 17, display: 'block', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{value}</b>
        {sub && <span className="small muted">{sub}</span>}
      </div>
    </div>
  )
}

export function Field({ label, children, hint, style }) {
  return (
    <label style={{ display: 'block', ...style }}>
      {label && <div className="small" style={{ marginBottom: 6, fontWeight: 700 }}>{label}</div>}
      {children}
      {hint && <div className="small muted" style={{ marginTop: 4 }}>{hint}</div>}
    </label>
  )
}

export function Modal({ open, onClose, title, children, footer, wide }) {
  useEffect(() => {
    if (!open) return
    const h = (e) => e.key === 'Escape' && onClose?.()
    window.addEventListener('keydown', h)
    document.body.style.overflow = 'hidden'
    return () => { window.removeEventListener('keydown', h); document.body.style.overflow = '' }
  }, [open, onClose])
  if (!open) return null
  return (
    <div onClick={onClose} style={{ position: 'fixed', inset: 0, background: 'rgba(30,18,8,.55)', zIndex: 90, display: 'grid', placeItems: 'center', padding: 14, backdropFilter: 'blur(2px)' }}>
      <div onClick={(e) => e.stopPropagation()} className="card" style={{ width: '100%', maxWidth: wide ? 820 : 520, maxHeight: '92vh', overflow: 'auto', padding: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 16px', borderBottom: '1px solid var(--border)', position: 'sticky', top: 0, background: 'var(--card)', zIndex: 2 }}>
          <h3 style={{ fontSize: 15 }}>{title}</h3>
          <button className="btn btn-outline btn-sm" onClick={onClose}>إغلاق ✕</button>
        </div>
        <div style={{ padding: 16 }}>{children}</div>
        {footer && <div style={{ padding: 14, borderTop: '1px solid var(--border)', display: 'flex', gap: 8, justifyContent: 'flex-start', position: 'sticky', bottom: 0, background: 'var(--card)' }}>{footer}</div>}
      </div>
    </div>
  )
}

export function Empty({ icon = '📭', text = 'لا توجد بيانات بعد' }) {
  return (
    <div style={{ textAlign: 'center', padding: '34px 10px', color: 'var(--muted)' }}>
      <div style={{ fontSize: 34, marginBottom: 6 }}>{icon}</div>
      <div className="small">{text}</div>
    </div>
  )
}

export function Money({ value, currency, bold, className = '' }) {
  return <span className={className} style={{ fontWeight: bold ? 800 : 500, direction: 'ltr', display: 'inline-block' }}>{money(value, currency)}</span>
}

export function Toast({ text }) {
  if (!text) return null
  return <div className="toast">{text}</div>
}

export function useToast() {
  const [text, setText] = useState('')
  useEffect(() => {
    if (!text) return
    const t = setTimeout(() => setText(''), 2600)
    return () => clearTimeout(t)
  }, [text])
  return [text, setText, <Toast text={text} />]
}

export function Tabs({ tabs, value, onChange }) {
  return (
    <div className="range" style={{ marginBottom: 12 }}>
      {tabs.map((t) => (
        <button key={t.id} onClick={() => onChange(t.id)} className={`btn btn-sm ${value === t.id ? 'btn-brown' : 'btn-outline'}`}>{t.label}</button>
      ))}
    </div>
  )
}

export function Toggle({ on, onChange }) {
  return <div className={`switch ${on ? 'on' : ''}`} onClick={() => onChange(!on)}><i /></div>
}

export function StatusBadge({ status }) {
  const map = {
    completed: ['مكتملة', 'badge-green'],
    cancelled: ['ملغاة', 'badge-red'],
    pending: ['قيد الانتظار', 'badge-gold'],
    ready: ['جاهز', 'badge-brown'],
    delivered: ['تم التسليم', 'badge-green'],
  }
  const [t, c] = map[status] || [status, 'badge-gold']
  return <span className={`badge ${c}`}>{t}</span>
}

export function Progress({ value, max, color = 'var(--gold)' }) {
  const pct = Math.min(100, max ? (value / max) * 100 : 0)
  return (
    <div style={{ height: 6, background: 'var(--border)', borderRadius: 99, overflow: 'hidden' }}>
      <div style={{ width: `${pct}%`, height: '100%', background: color, transition: 'width .3s' }} />
    </div>
  )
}
