import { useMemo, useState } from 'react'
import { useStore, saveCustomer, savePayment, partyStats } from '../store'
import { money, parseAmount, fmtDate, todayISO, CURRENCIES } from '../utils'
import { Card, Empty, Field, Modal, StatusBadge } from '../components'

export default function Customers({ go, onToast }) {
  const s = useStore()
  const [q, setQ] = useState('')
  const [editing, setEditing] = useState(null)
  const [statement, setStatement] = useState(null)
  const [payFor, setPayFor] = useState(null)

  const list = useMemo(() => s.customers.filter((c) => !q || c.name.includes(q) || (c.phone || '').includes(q)), [s.customers, q])

  return (
    <div className="grid" style={{ gap: 12 }}>
      <Card>
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <input className="input" style={{ maxWidth: 320 }} placeholder="ابحث عن زبون..." value={q} onChange={(e) => setQ(e.target.value)} />
          <button className="btn btn-gold btn-sm" onClick={() => setEditing({ name: '', phone: '', type: 'credit', notes: '', active: true })}>＋ زبون جديد</button>
        </div>
      </Card>

      <Card title="الزبائن" badge={list.length}>
        {list.length === 0 ? <Empty icon="👥" /> : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>الاسم</th><th>الهاتف</th><th>النوع</th><th>عدد الفواتير</th><th>رصيد ل.س</th><th>رصيد $</th><th>آخر فاتورة</th><th></th></tr>
              </thead>
              <tbody>
                {list.map((c) => {
                  const st = partyStats(c.id, 'customer')
                  const balS = st.balances.SYP
                  const balU = st.balances.USD
                  return (
                    <tr key={c.id}>
                      <td style={{ fontWeight: 700 }}>{c.name}</td>
                      <td className="small" style={{ direction: 'ltr' }}>{c.phone || '—'}</td>
                      <td><span className="badge badge-brown">{c.type === 'cash' ? 'نقدي' : 'آجل'}</span></td>
                      <td>{st.count}</td>
                      <td style={{ direction: 'ltr', color: balS > 0 ? 'var(--danger)' : 'var(--success)', fontWeight: 700 }}>{money(balS, 'SYP', { bare: true })}</td>
                      <td style={{ direction: 'ltr', color: balU > 0 ? 'var(--danger)' : 'var(--success)', fontWeight: 700 }}>{money(balU, 'USD', { bare: true })}</td>
                      <td className="small">{st.lastInvoice ? fmtDate(st.lastInvoice.date) : '—'}</td>
                      <td>
                        <div className="row" style={{ gap: 4 }}>
                          {c.id !== 'cash' && (
                            <>
                              <button className="btn btn-outline btn-sm" onClick={() => setStatement(c)}>كشف حساب</button>
                              <button className="btn btn-gold btn-sm" onClick={() => setPayFor(c)}>دفعة</button>
                            </>
                          )}
                          <button className="btn btn-outline btn-sm" onClick={() => setEditing({ ...c })}>تعديل</button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {editing && (
        <Modal open onClose={() => setEditing(null)} title={editing.id ? 'تعديل زبون' : 'زبون جديد'}
          footer={<>
            <button className="btn btn-gold" onClick={() => {
              if (!editing.name.trim()) return
              saveCustomer(editing); setEditing(null); onToast?.('تم حفظ الزبون')
            }}>حفظ</button>
            <button className="btn btn-outline" onClick={() => setEditing(null)}>إلغاء</button>
          </>}>
          <div className="grid" style={{ gap: 10 }}>
            <Field label="الاسم"><input className="input" value={editing.name} onChange={(e) => setEditing({ ...editing, name: e.target.value })} /></Field>
            <Field label="الهاتف"><input className="input" style={{ direction: 'ltr' }} value={editing.phone} onChange={(e) => setEditing({ ...editing, phone: e.target.value })} /></Field>
            <Field label="النوع">
              <select className="select" value={editing.type} onChange={(e) => setEditing({ ...editing, type: e.target.value })}>
                <option value="credit">آجل</option><option value="cash">نقدي</option>
              </select>
            </Field>
            <Field label="ملاحظات"><textarea className="textarea" rows={2} value={editing.notes} onChange={(e) => setEditing({ ...editing, notes: e.target.value })} /></Field>
          </div>
        </Modal>
      )}

      {statement && <Statement customer={statement} onClose={() => setStatement(null)} onPay={() => { setPayFor(statement); setStatement(null) }} onToast={onToast} go={go} />}
      {payFor && <PaymentModal customer={payFor} onClose={() => setPayFor(null)} onSave={(p) => { savePayment(p); setPayFor(null); onToast?.('تم تسجيل الدفعة') }} />}
    </div>
  )
}

function Statement({ customer, onClose, onPay, onToast, go }) {
  const s = useStore()
  const st = partyStats(customer.id, 'customer')
  const events = [
    ...st.invoices.map((i) => ({ kind: 'inv', date: i.date, ref: i.no, debit: i.total, credit: i.paid || 0, currency: i.currency, status: i.status, id: i.id, remaining: i.remaining })),
    ...st.payments.map((p) => ({ kind: 'pay', date: p.date, ref: 'دفعة', debit: 0, credit: p.amount, currency: p.currency, note: p.note })),
  ].sort((a, b) => a.date.localeCompare(b.date))

  const share = () => {
    const text = [
      `*${s.settings.shopName}*`,
      `كشف حساب: ${customer.name}`,
      `التاريخ: ${fmtDate(todayISO())}`,
      '',
      ...events.map((e) => `${fmtDate(e.date)} | ${e.ref} | ${e.kind === 'inv' ? 'فاتورة' : 'دفعة'} | ${money(e.kind === 'inv' ? e.debit : e.credit, e.currency)}`),
      '',
      `*الرصيد المستحق:*`,
      `ليرة سورية: ${money(st.balances.SYP, 'SYP')}`,
      `دولار أمريكي: ${money(st.balances.USD, 'USD')}`,
      '',
      s.settings.phone ? `للتواصل: ${s.settings.phone}` : '',
    ].filter(Boolean).join('\n')
    const phone = (customer.phone || '').replace(/[^0-9]/g, '')
    window.open(phone ? `https://wa.me/${phone.startsWith('963') ? phone : '963' + phone.replace(/^0/, '')}?text=${encodeURIComponent(text)}` : `https://wa.me/?text=${encodeURIComponent(text)}`, '_blank')
    onToast?.('تم فتح واتساب لإرسال كشف الحساب')
  }

  return (
    <Modal open onClose={onClose} wide title={`كشف حساب — ${customer.name}`}
      footer={<>
        <button className="btn btn-gold" onClick={share}>إرسال واتساب</button>
        <button className="btn btn-brown" onClick={onPay}>تسجيل دفعة</button>
        <button className="btn btn-outline" onClick={onClose}>إغلاق</button>
      </>}>
      <div className="grid grid-3" style={{ marginBottom: 12 }}>
        <div className="card" style={{ padding: 10 }}>
          <div className="small muted">الرصيد بالليرة</div>
          <b style={{ fontSize: 17, direction: 'ltr', display: 'block', color: st.balances.SYP > 0 ? 'var(--danger)' : 'var(--success)' }}>{money(st.balances.SYP, 'SYP')}</b>
        </div>
        <div className="card" style={{ padding: 10 }}>
          <div className="small muted">الرصيد بالدولار</div>
          <b style={{ fontSize: 17, direction: 'ltr', display: 'block', color: st.balances.USD > 0 ? 'var(--danger)' : 'var(--success)' }}>{money(st.balances.USD, 'USD')}</b>
        </div>
        <div className="card" style={{ padding: 10 }}>
          <div className="small muted">إجمالي المبيعات</div>
          <b style={{ fontSize: 15 }}>{st.count} فاتورة</b>
        </div>
      </div>

      <div className="table-wrap" style={{ maxHeight: 340 }}>
        <table>
          <thead><tr><th>التاريخ</th><th>البيان</th><th>مدين</th><th>دائن</th><th>العملة</th><th>الحالة</th></tr></thead>
          <tbody>
            {events.map((e, i) => (
              <tr key={i}>
                <td className="small">{fmtDate(e.date)}</td>
                <td>
                  {e.kind === 'inv'
                    ? <span style={{ cursor: 'pointer', color: 'var(--gold-dark)', fontWeight: 700 }} onClick={() => go?.('invoice-view', { id: e.id })}>{e.ref}</span>
                    : e.ref}
                  {e.note ? <span className="small muted"> — {e.note}</span> : ''}
                </td>
                <td style={{ direction: 'ltr' }}>{e.debit ? money(e.debit, e.currency, { bare: true }) : '—'}</td>
                <td style={{ direction: 'ltr' }}>{e.credit ? money(e.credit, e.currency, { bare: true }) : '—'}</td>
                <td className="small">{e.currency === 'SYP' ? 'ل.س' : '$'}</td>
                <td>{e.status ? <StatusBadge status={e.status} /> : '—'}</td>
              </tr>
            ))}
            {events.length === 0 && <tr><td colSpan={6}><Empty /></td></tr>}
          </tbody>
        </table>
      </div>
      <p className="small muted" style={{ marginTop: 8 }}>كل عملة تُحسب على حدة، ولا يتم خلط الليرة بالدولار في الرصيد.</p>
    </Modal>
  )
}

function PaymentModal({ customer, onClose, onSave }) {
  const s = useStore()
  const [currency, setCurrency] = useState('SYP')
  const [amount, setAmount] = useState('')
  const [date, setDate] = useState(todayISO())
  const [note, setNote] = useState('')
  const bal = partyStats(customer.id, 'customer').balances[currency]

  return (
    <Modal open onClose={onClose} title={`تسجيل دفعة — ${customer.name}`}
      footer={<>
        <button className="btn btn-gold" onClick={() => {
          const a = Math.round(parseAmount(amount) * Math.pow(10, CURRENCIES[currency].decimals))
          if (a > 0) onSave({ partyType: 'customer', partyId: customer.id, date, currency, amount: a, note })
        }}>حفظ الدفعة</button>
        <button className="btn btn-outline" onClick={onClose}>إلغاء</button>
      </>}>
      <div className="grid" style={{ gap: 10 }}>
        <div className="row" style={{ justifyContent: 'space-between', background: 'var(--cream)', padding: 10, borderRadius: 10 }}>
          <span className="small">الرصيد الحالي ({currency === 'SYP' ? 'ليرة' : 'دولار'})</span>
          <b style={{ direction: 'ltr', color: bal > 0 ? 'var(--danger)' : 'var(--success)' }}>{money(bal, currency)}</b>
        </div>
        <div className="row" style={{ gap: 6 }}>
          {['SYP', 'USD'].map((c) => (
            <button key={c} className={`btn btn-sm ${currency === c ? 'btn-brown' : 'btn-outline'}`} style={{ flex: 1 }} onClick={() => setCurrency(c)}>
              {CURRENCIES[c].name}
            </button>
          ))}
        </div>
        <Field label="المبلغ"><input className="input" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} /></Field>
        <div className="row" style={{ gap: 6 }}>
          <button className="btn btn-outline btn-sm" onClick={() => setAmount(String(bal / Math.pow(10, CURRENCIES[currency].decimals)))}>تصفية كامل الرصيد</button>
        </div>
        <Field label="التاريخ"><input type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} /></Field>
        <Field label="ملاحظة"><input className="input" value={note} onChange={(e) => setNote(e.target.value)} /></Field>
      </div>
    </Modal>
  )
}
