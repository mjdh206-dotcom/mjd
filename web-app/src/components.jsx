import { useEffect, useMemo, useState } from 'react'
import { Icon } from './icons'
import { money as fmtMoney } from './utils'

export function Card({ title, subtitle, icon, badge, action, children, className = '', style, flush, tight, dark }) {
  return (
    <section className={`card ${flush ? 'flush' : ''} ${tight ? 'tight' : ''} ${dark ? 'dark' : ''} ${className}`} style={style}>
      {(title || action) && (
        <div className="card-head" style={flush ? { padding: '16px 18px 0', marginBottom: 12 } : undefined}>
          <div>
            <h3>
              {icon && <span className="card-title-row"><span className="ic"><Icon name={icon} /></span></span>}
              {title}
              {badge !== undefined && badge !== null && <span className="hint">{badge}</span>}
            </h3>
            {subtitle && <div className="sub">{subtitle}</div>}
          </div>
          {action}
        </div>
      )}
      {children}
    </section>
  )
}

export function PageHead({ title, subtitle, actions }) {
  return (
    <div className="page-head">
      <div>
        <h2>{title}</h2>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {actions && <div className="actions">{actions}</div>}
    </div>
  )
}

export function Field({ label, children, hint, required, style, className = '' }) {
  return (
    <label className={`field ${className}`} style={style}>
      {label && <div className="lbl">{label}{required && <span className="req">*</span>}</div>}
      {children}
      {hint && <div className="hint">{hint}</div>}
    </label>
  )
}

export function Modal({ open = true, onClose, title, subtitle, icon = 'info', children, footer, size = 'md', className = '' }) {
  useEffect(() => {
    if (!open) return
    const h = (e) => e.key === 'Escape' && onClose?.()
    window.addEventListener('keydown', h)
    document.body.style.overflow = 'hidden'
    return () => { window.removeEventListener('keydown', h); document.body.style.overflow = '' }
  }, [open, onClose])
  if (!open) return null
  const cls = size === 'wide' ? 'wide' : size === 'xwide' ? 'xwide' : size === 'sm' ? 'slim' : ''
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose?.()}>
      <div className={`modal ${cls} ${className}`} role="dialog" aria-modal="true">
        <div className="modal-head">
          {icon && <div className="ic"><Icon name={icon} /></div>}
          <div style={{ minWidth: 0 }}>
            <h3>{title}</h3>
            {subtitle && <p>{subtitle}</p>}
          </div>
          <button className="icon-btn x" onClick={onClose} aria-label="إغلاق" style={{ width: 32, height: 32 }}><Icon name="x" size={15} /></button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-foot">{footer}</div>}
      </div>
    </div>
  )
}

export function Drawer({ open = true, onClose, title, subtitle, icon, children, footer, side = 'end', width }) {
  useEffect(() => {
    if (!open) return
    const h = (e) => e.key === 'Escape' && onClose?.()
    window.addEventListener('keydown', h)
    document.body.style.overflow = 'hidden'
    return () => { window.removeEventListener('keydown', h); document.body.style.overflow = '' }
  }, [open, onClose])
  if (!open) return null
  return (
    <div className="drawer-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose?.()}>
      <div className={`drawer ${side === 'end' ? 'right' : ''}`} style={width ? { maxWidth: width } : undefined}>
        <div className="modal-head">
          {icon && <div className="ic"><Icon name={icon} /></div>}
          <div style={{ minWidth: 0 }}>
            <h3>{title}</h3>
            {subtitle && <p>{subtitle}</p>}
          </div>
          <button className="icon-btn x" onClick={onClose} aria-label="إغلاق" style={{ width: 32, height: 32 }}><Icon name="x" size={15} /></button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-foot">{footer}</div>}
      </div>
    </div>
  )
}

export function Empty({ icon = 'package', title = 'لا توجد بيانات بعد', text, action }) {
  return (
    <div className="empty">
      <div className="ill"><Icon name={icon} size={28} /></div>
      <b>{title}</b>
      {text && <p>{text}</p>}
      {action}
    </div>
  )
}

export function Money({ value, currency = 'SYP', bold, bare, className = '', style }) {
  return <span className={className} style={{ fontWeight: bold ? 800 : 600, direction: 'ltr', unicodeBidi: 'isolate', fontVariantNumeric: 'tabular-nums', ...style }}>{fmtMoney(value, currency, { bare })}</span>
}

export const StatusBadge = ({ status }) => {
  const map = {
    completed: ['مكتملة', 'badge-green', 'checkCircle'],
    cancelled: ['ملغاة', 'badge-red', 'x'],
    pending: ['قيد الانتظار', 'badge-warn', 'clock'],
    ready: ['جاهز', 'badge-blue', 'check'],
    delivered: ['تم التسليم', 'badge-green', 'checkCircle'],
    paid: ['مسدّدة', 'badge-green', 'check'],
    partial: ['جزئية', 'badge-warn', 'clock'],
    debt: ['دين', 'badge-red', 'clock'],
    active: ['نشط', 'badge-green', 'check'],
    inactive: ['موقوف', 'badge-neutral', 'x'],
  }
  const [t, c, ic] = map[status] || [status || '—', 'badge-neutral', 'info']
  return <span className={`badge ${c}`}>{ic && <Icon name={ic} size={11} />}{t}</span>
}

export function Toggle({ on, onChange, label, hint }) {
  return (
    <div className="row between" style={{ gap: 12, flexWrap: 'nowrap' }}>
      {(label || hint) && (
        <div style={{ minWidth: 0 }}>
          {label && <div style={{ fontSize: 12.5, fontWeight: 700 }}>{label}</div>}
          {hint && <div className="tiny muted" style={{ marginTop: 2 }}>{hint}</div>}
        </div>
      )}
      <button type="button" className={`switch ${on ? 'on' : ''}`} onClick={() => onChange(!on)} aria-pressed={on}><i /></button>
    </div>
  )
}

export const Seg = ({ options, value, onChange, block, gold, size }) => (
  <div className={`seg ${block ? 'block' : ''} ${gold ? 'gold' : ''}`}>
    {options.map((o) => (
      <button key={o.id} className={value === o.id ? 'on' : ''} onClick={() => onChange(o.id)} title={o.title}
        style={size === 'sm' ? { padding: '4px 9px', fontSize: 10.5 } : undefined}>
        {o.icon && <Icon name={o.icon} size={13} />}{o.label}
      </button>
    ))}
  </div>
)

export const Tabs = Seg

export function Kpi({ label, value, sub, icon = 'chart', tone = '', delta, deltaGood = true, onClick, style }) {
  return (
    <div className={`kpi ${tone}`} style={{ cursor: onClick ? 'pointer' : undefined, ...style }} onClick={onClick}>
      <div className="top">
        <span className="lbl">{label}</span>
        <span className="ic"><Icon name={icon} /></span>
      </div>
      <div className="val">{value}</div>
      <div className="foot">
        {delta !== undefined && delta !== null && (
          <span className={`trend ${delta < 0 ? 'down' : ''}`} style={delta < 0 ? { background: deltaGood ? 'var(--warn-soft)' : 'var(--danger-soft)' } : undefined}>
            <Icon name={delta >= 0 ? 'trendUp' : 'trendDown'} size={11} />{Math.abs(delta).toFixed(0)}%
          </span>
        )}
        {sub}
      </div>
    </div>
  )
}

export function Avatar({ name = '؟', size = '' }) {
  const initials = useMemo(() => {
    const parts = String(name).trim().split(/\s+/)
    return (parts[0]?.[0] || '؟') + (parts[1]?.[0] || '')
  }, [name])
  return <div className={`avatar ${size}`}>{initials}</div>
}

export function ProgressRow({ label, value, max, right, color }) {
  const pct = Math.min(100, max ? (value / max) * 100 : 0)
  return (
    <div style={{ marginBottom: 12 }}>
      <div className="row between" style={{ marginBottom: 5, flexWrap: 'nowrap', gap: 8 }}>
        <span style={{ fontSize: 11.5, fontWeight: 700, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{label}</span>
        <span className="tiny muted" style={{ whiteSpace: 'nowrap' }}>{right}</span>
      </div>
      <div className="bar-track"><div className="bar-fill" style={{ width: `${pct}%`, background: color }} /></div>
    </div>
  )
}

export function ConfirmDialog({ open, onClose, onConfirm, title, text, confirmLabel = 'تأكيد', tone = 'danger', icon = 'alert' }) {
  return (
    <Modal open={open} onClose={onClose} title={title} icon={icon} size="sm"
      footer={<>
        <button className={`btn ${tone === 'danger' ? 'btn-danger' : 'btn-primary'}`} onClick={() => { onConfirm(); onClose() }}>{confirmLabel}</button>
        <button className="btn btn-ghost" onClick={onClose}>رجوع</button>
      </>}>
      <p style={{ fontSize: 12.5, lineHeight: 1.9, color: 'var(--ink-2)' }}>{text}</p>
    </Modal>
  )
}

export function useToast() {
  const [toast, setToast] = useState(null)
  useEffect(() => {
    if (!toast) return
    const t = setTimeout(() => setToast(null), toast.duration || 2900)
    return () => clearTimeout(t)
  }, [toast])
  const notify = (msg, type = 'ok') => {
    if (msg && typeof msg === 'object') setToast(msg)
    else setToast({ msg, type, id: Date.now() })
  }
  const el = toast ? (
    <div className="toast-stack">
      <div className={`toast ${toast.type === 'error' ? 'error' : ''}`}>
        <Icon name={toast.type === 'error' ? 'alert' : toast.type === 'info' ? 'info' : 'checkCircle'} />
        <span>{toast.msg}</span>
      </div>
    </div>
  ) : null
  return [toast, notify, el]
}

export const Toast = ({ toast }) => toast ? (
  <div className="toast-stack"><div className={`toast ${toast.type === 'error' ? 'error' : ''}`}><Icon name="checkCircle" />{toast.msg}</div></div>
) : null

export function IconPick({ name, size = 34, tone = 'gold', onClick }) {
  return (
    <button type="button" className="ic" onClick={onClick} style={{
      width: size, height: size, borderRadius: 10, display: 'grid', placeItems: 'center', border: '1px solid var(--line)',
      background: tone === 'gold' ? 'var(--gold-3)' : 'var(--surface-2)', color: 'var(--gold-deep)',
    }}><Icon name={name} size={size * 0.47} /></button>
  )
}
