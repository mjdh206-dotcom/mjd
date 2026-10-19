import { useMemo, useState } from 'react'
import { useStore, savePreorder, nextNo } from '../store'
import { CURRENCIES, money, parseAmount, fmtDate, todayISO, dayOffset } from '../utils'
import { Card, Empty, Field, Modal, PageHead, StatusBadge, Seg } from '../components'
import { Icon } from '../icons'

const STATUS = [
  { id: 'pending', label: 'بانتظار التحضير', icon: 'clock', tone: 'warn' },
  { id: 'ready', label: 'جاهز للتسليم', icon: 'checkCircle', tone: 'blue' },
  { id: 'delivered', label: 'تم التسليم', icon: 'check', tone: 'green' },
]

export default function Preorders({ onToast, go }) {
  const s = useStore()
  const [editing, setEditing] = useState(null)
  const [q, setQ] = useState('')

  const list = useMemo(() => s.preorders.filter((p) => !q || p.customerName.includes(q) || (p.phone || '').includes(q)), [s.preorders, q])
  const deposits = list.reduce((a, p) => { a[p.currency] += p.deposit || 0; return a }, { SYP: 0, USD: 0 })
  const remaining = list.filter((p) => p.status !== 'delivered').reduce((a, p) => { a[p.currency] += Math.max(0, p.total - (p.deposit || 0)); return a }, { SYP: 0, USD: 0 })

  const openNew = () => setEditing({
    no: `PO-${String(s.preorders.length + 1).padStart(3, '0')}`, customerName: '', phone: '', date: todayISO(),
    deliveryDate: dayOffset(-3), total: '', deposit: '', currency: 'SYP', notes: '', status: 'pending',
  })

  const advance = (p) => {
    const idx = STATUS.findIndex((x) => x.id === p.status)
    const next = STATUS[Math.min(STATUS.length - 1, idx + 1)]
    savePreorder({ ...p, status: next.id })
    onToast?.(`تم تحويل الطلب إلى: ${next.label}`)
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      <PageHead title="الطلبات المسبقة" subtitle="طلبات السلال والعلب والمناسبات — عربون ومتابعة حتى التسليم"
        actions={<button className="btn btn-gold" onClick={openNew}><Icon name="plus" />طلب مسبق</button>} />

      <div className="grid g4">
        <div className="kpi"><div className="top"><span className="lbl">إجمالي الطلبات</span><span className="ic"><Icon name="gift" /></span></div><div className="val">{list.length}</div><div className="foot">{list.filter((p) => p.status === 'pending').length} بانتظار التحضير</div></div>
        <div className="kpi gold"><div className="top"><span className="lbl">العربون المستلم — ليرة</span><span className="ic"><Icon name="cash" /></span></div><div className="val">{money(deposits.SYP, 'SYP', { bare: true })}</div><div className="foot">+ {money(deposits.USD, 'USD')}</div></div>
        <div className="kpi"><div className="top"><span className="lbl">المتبقي على الزبائن</span><span className="ic"><Icon name="wallet" /></span></div><div className="val">{money(remaining.SYP, 'SYP', { bare: true })}</div><div className="foot">+ {money(remaining.USD, 'USD')}</div></div>
        <div className="kpi"><div className="top"><span className="lbl">جاهز للتسليم</span><span className="ic"><Icon name="checkCircle" /></span></div><div className="val">{list.filter((p) => p.status === 'ready').length}</div><div className="foot">يمكن تسليمها اليوم</div></div>
      </div>

      <Card tight>
        <div className="field-icon"><Icon name="search" /><input className="input" placeholder="ابحث باسم الزبون أو الهاتف…" value={q} onChange={(e) => setQ(e.target.value)} /></div>
      </Card>

      <div className="grid g3" style={{ alignItems: 'start' }}>
        {STATUS.map((st) => {
          const items = list.filter((p) => p.status === st.id)
          return (
            <div key={st.id} className="card tight" style={{ background: 'var(--surface-2)', minHeight: 180 }}>
              <div className="row between" style={{ marginBottom: 10 }}>
                <div className="row" style={{ gap: 7 }}>
                  <span className={`badge badge-${st.tone}`}><Icon name={st.icon} size={11} />{st.label}</span>
                </div>
                <span className="tiny muted">{items.length}</span>
              </div>
              {items.length === 0 ? <div className="empty" style={{ padding: '20px 6px' }}><div className="tiny">لا طلبات هنا</div></div> : (
                <div className="stack" style={{ gap: 8 }}>
                  {items.map((p) => (
                    <div className="card tight" key={p.id} style={{ background: 'var(--surface)' }}>
                      <div className="row between" style={{ marginBottom: 5 }}>
                        <b style={{ fontSize: 12 }}>{p.customerName}</b>
                        <span className="tiny muted">{p.no}</span>
                      </div>
                      <div className="tiny muted" style={{ lineHeight: 1.8 }}>{p.notes || '—'}</div>
                      <div className="row between" style={{ marginTop: 7 }}>
                        <span className="tiny">التسليم: <b>{fmtDate(p.deliveryDate)}</b></span>
                        <b className="small" style={{ direction: 'ltr' }}>{money(p.total, p.currency)}</b>
                      </div>
                      <div className="row between" style={{ marginTop: 3 }}>
                        <span className="tiny muted">عربون {money(p.deposit || 0, p.currency)}</span>
                        <span className="tiny" style={{ color: p.total - p.deposit > 0 ? 'var(--danger)' : 'var(--success)', fontWeight: 700 }}>متبقٍ {money(Math.max(0, p.total - p.deposit), p.currency)}</span>
                      </div>
                      <div className="row" style={{ marginTop: 8, gap: 4 }}>
                        {p.status !== 'delivered' && <button className="btn btn-gold btn-sm" onClick={() => advance(p)}>التالي<Icon name="chevronLeft" /></button>}
                        <button className="btn btn-ghost btn-sm" onClick={() => setEditing({ ...p, total: p.total / Math.pow(10, CURRENCIES[p.currency].decimals), deposit: (p.deposit || 0) / Math.pow(10, CURRENCIES[p.currency].decimals) })}><Icon name="edit" /></button>
                        <button className="btn btn-ghost btn-sm" onClick={() => go('sale-new')} title="إنشاء فاتورة"><Icon name="cart" /></button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )
        })}
      </div>

      <Card title="كل الطلبات" icon="list" flush>
        {list.length === 0 ? <Empty icon="gift" title="لا طلبات" /> : (
          <div className="table-wrap" style={{ border: 0 }}>
            <table>
              <thead><tr><th>الرقم</th><th>الزبون</th><th>الهاتف</th><th>التسليم</th><th className="num">الإجمالي</th><th className="num">العربون</th><th className="num">المتبقي</th><th>الحالة</th></tr></thead>
              <tbody>
                {list.map((p) => (
                  <tr key={p.id}>
                    <td><b>{p.no}</b></td><td>{p.customerName}</td><td className="num" style={{ textAlign: 'start' }}>{p.phone || '—'}</td>
                    <td>{fmtDate(p.deliveryDate)}</td>
                    <td className="num strong">{money(p.total, p.currency, { bare: true })}</td>
                    <td className="num">{money(p.deposit || 0, p.currency, { bare: true })}</td>
                    <td className="num" style={{ color: 'var(--danger)' }}>{money(Math.max(0, p.total - p.deposit), p.currency, { bare: true })}</td>
                    <td><StatusBadge status={p.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {editing && <PreorderModal p={editing} onClose={() => setEditing(null)} onSave={(v) => { savePreorder(v); setEditing(null); onToast?.('تم حفظ الطلب') }} />}
    </div>
  )
}

function PreorderModal({ p, onClose, onSave }) {
  const [f, setF] = useState(p)
  const dec = CURRENCIES[f.currency].decimals
  return (
    <Modal open onClose={onClose} title={p.id ? 'تعديل طلب مسبق' : 'طلب مسبق جديد'} subtitle="سلال وعلب ضيافة للمناسبات" icon="gift" size="wide"
      footer={<>
        <button className="btn btn-gold" onClick={() => {
          onSave({
            ...f,
            total: Math.round(parseAmount(f.total) * Math.pow(10, dec)),
            deposit: Math.round(parseAmount(f.deposit) * Math.pow(10, dec)),
          })
        }}>حفظ الطلب</button>
        <button className="btn btn-ghost" onClick={onClose}>إلغاء</button>
      </>}>
      <div className="stack">
        <div className="grid g2" style={{ gap: 10 }}>
          <Field label="اسم الزبون" required><input className="input" value={f.customerName} onChange={(e) => setF({ ...f, customerName: e.target.value })} autoFocus /></Field>
          <Field label="الهاتف"><input className="input" style={{ direction: 'ltr' }} value={f.phone} onChange={(e) => setF({ ...f, phone: e.target.value })} /></Field>
          <Field label="تاريخ الطلب"><input type="date" className="input" value={f.date} onChange={(e) => setF({ ...f, date: e.target.value })} /></Field>
          <Field label="تاريخ التسليم"><input type="date" className="input" value={f.deliveryDate} onChange={(e) => setF({ ...f, deliveryDate: e.target.value })} /></Field>
        </div>
        <div className="grid g3" style={{ gap: 10 }}>
          <Field label="العملة">
            <Seg block value={f.currency} onChange={(v) => setF({ ...f, currency: v })} options={[{ id: 'SYP', label: 'ليرة' }, { id: 'USD', label: 'دولار' }]} />
          </Field>
          <Field label="الإجمالي"><input className="input" inputMode="decimal" value={f.total} onChange={(e) => setF({ ...f, total: e.target.value })} /></Field>
          <Field label="العربون"><input className="input" inputMode="decimal" value={f.deposit} onChange={(e) => setF({ ...f, deposit: e.target.value })} /></Field>
        </div>
        <Field label="تفاصيل الطلب"><textarea className="textarea" value={f.notes} onChange={(e) => setF({ ...f, notes: e.target.value })} placeholder="مثال: 5 سلال ضيافة فاخرة للعرس…" /></Field>
        <Field label="الحالة">
          <Seg block value={f.status} onChange={(v) => setF({ ...f, status: v })} options={STATUS.map((x) => ({ id: x.id, label: x.label }))} />
        </Field>
      </div>
    </Modal>
  )
}
