import { useMemo, useState } from 'react'
import { useStore, saveSupplier, savePayment, partyStats } from '../store'
import { money, parseAmount, fmtDate, todayISO, CURRENCIES } from '../utils'
import { Card, Empty, Field, Modal, PageHead, Avatar, Drawer, Seg } from '../components'
import { Icon } from '../icons'

export default function Suppliers({ go, onToast }) {
  const s = useStore()
  const [q, setQ] = useState('')
  const [editing, setEditing] = useState(null)
  const [payFor, setPayFor] = useState(null)
  const [statement, setStatement] = useState(null)

  const list = useMemo(() => s.suppliers.filter((c) => !q || c.name.includes(q) || (c.phone || '').includes(q)), [s.suppliers, q])
  const totals = useMemo(() => {
    let sy = 0, us = 0
    s.suppliers.forEach((c) => { const st = partyStats(c.id, 'supplier'); sy += st.balances.SYP; us += st.balances.USD })
    return { sy, us }
  }, [s.suppliers, s.invoices, s.payments])

  return (
    <div className="grid" style={{ gap: 16 }}>
      <PageHead title="الموردون" subtitle="مستحقات الموردين بالعملتين، دفعاتهم، والمشتريات المرتبطة بهم"
        actions={<>
          <button className="btn btn-ghost" onClick={() => go('purchases')}><Icon name="bag" />المشتريات</button>
          <button className="btn btn-gold" onClick={() => setEditing({ name: '', phone: '', notes: '', active: true })}><Icon name="plus" />مورد جديد</button>
        </>} />

      <div className="grid g3">
        <div className="kpi"><div className="top"><span className="lbl">عدد الموردين</span><span className="ic"><Icon name="truck" /></span></div><div className="val">{s.suppliers.length}</div><div className="foot">شركاء التوريد</div></div>
        <div className="kpi gold"><div className="top"><span className="lbl">مستحقات بالليرة</span><span className="ic"><Icon name="wallet" /></span></div><div className="val">{money(totals.sy, 'SYP', { bare: true })}</div><div className="foot">ل.س يجب دفعها</div></div>
        <div className="kpi"><div className="top"><span className="lbl">مستحقات بالدولار</span><span className="ic"><Icon name="dollar" /></span></div><div className="val">{money(totals.us, 'USD', { bare: true })}</div><div className="foot">$ يجب دفعها</div></div>
      </div>

      <Card tight>
        <div className="field-icon">
          <Icon name="search" />
          <input className="input" placeholder="ابحث عن مورد…" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
      </Card>

      <Card flush>
        {list.length === 0 ? <Empty icon="truck" title="لا موردين" /> : (
          <>
            <div className="table-wrap desktop-table" style={{ border: 0, borderRadius: 0 }}>
              <table>
                <thead><tr><th>المورد</th><th>الهاتف</th><th>ملاحظات</th><th className="num">مستحق ل.س</th><th className="num">مستحق $</th><th>آخر توريد</th><th></th></tr></thead>
                <tbody>
                  {list.map((c) => {
                    const st = partyStats(c.id, 'supplier')
                    return (
                      <tr key={c.id}>
                        <td><div className="row" style={{ gap: 8, flexWrap: 'nowrap' }}><Avatar name={c.name} size="sm" /><b>{c.name}</b></div></td>
                        <td className="num" style={{ textAlign: 'start' }}>{c.phone || '—'}</td>
                        <td className="small muted">{c.notes || '—'}</td>
                        <td className="num" style={{ fontWeight: 700, color: st.balances.SYP > 0 ? 'var(--danger)' : 'var(--success)' }}>{money(st.balances.SYP, 'SYP', { bare: true })}</td>
                        <td className="num" style={{ fontWeight: 700, color: st.balances.USD > 0 ? 'var(--danger)' : 'var(--success)' }}>{money(st.balances.USD, 'USD', { bare: true })}</td>
                        <td className="small">{st.lastInvoice ? fmtDate(st.lastInvoice.date) : '—'}</td>
                        <td>
                          <div className="row" style={{ gap: 3, flexWrap: 'nowrap' }}>
                            <button className="btn btn-ghost btn-sm" onClick={() => setStatement(c)}><Icon name="book" />كشف</button>
                            <button className="btn btn-gold btn-sm" onClick={() => setPayFor(c)}><Icon name="cash" />دفعة</button>
                            <button className="btn btn-ghost btn-icon" onClick={() => setEditing({ ...c })}><Icon name="edit" /></button>
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
            <div className="mobile-cards" style={{ padding: 12 }}>
              {list.map((c) => {
                const st = partyStats(c.id, 'supplier')
                return (
                  <div className="m-card" key={c.id}>
                    <div className="top">
                      <div className="row" style={{ gap: 9, flexWrap: 'nowrap' }}><Avatar name={c.name} /><div><div className="title">{c.name}</div><div className="sub">{c.phone || 'بدون هاتف'}</div></div></div>
                      <span className={`badge ${(st.balances.SYP + st.balances.USD) > 0 ? 'badge-red' : 'badge-green'}`}>{(st.balances.SYP + st.balances.USD) > 0 ? 'مستحق' : 'مسدّد'}</span>
                    </div>
                    <div className="row" style={{ gap: 16 }}>
                      <div><div className="tiny muted">ل.س</div><b style={{ fontSize: 13 }}>{money(st.balances.SYP, 'SYP')}</b></div>
                      <div><div className="tiny muted">$</div><b style={{ fontSize: 13 }}>{money(st.balances.USD, 'USD')}</b></div>
                    </div>
                    <div className="foot">
                      <span>{st.count} فاتورة شراء</span>
                      <div className="row" style={{ gap: 4 }}>
                        <button className="btn btn-ghost btn-sm" onClick={() => setStatement(c)}>كشف</button>
                        <button className="btn btn-gold btn-sm" onClick={() => setPayFor(c)}>دفعة</button>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          </>
        )}
      </Card>

      {editing && (
        <Modal open onClose={() => setEditing(null)} title={editing.id ? 'تعديل مورد' : 'مورد جديد'} icon="truck" size="sm"
          footer={<>
            <button className="btn btn-gold" onClick={() => { if (!editing.name.trim()) return onToast?.('أدخل اسم المورد', 'error'); saveSupplier(editing); setEditing(null); onToast?.('تم حفظ المورد') }}>حفظ</button>
            <button className="btn btn-ghost" onClick={() => setEditing(null)}>إلغاء</button>
          </>}>
          <div className="stack">
            <Field label="اسم المورد" required><input className="input" value={editing.name} onChange={(e) => setEditing({ ...editing, name: e.target.value })} autoFocus /></Field>
            <Field label="الهاتف"><input className="input" style={{ direction: 'ltr' }} value={editing.phone} onChange={(e) => setEditing({ ...editing, phone: e.target.value })} /></Field>
            <Field label="ملاحظات"><input className="input" value={editing.notes || ''} onChange={(e) => setEditing({ ...editing, notes: e.target.value })} placeholder="شروط الدفع، عنوان…" /></Field>
          </div>
        </Modal>
      )}

      {payFor && <SupplierPayment supplier={payFor} onClose={() => setPayFor(null)} onSave={(p) => { savePayment(p); setPayFor(null); onToast?.('تم تسجيل الدفعة') }} />}
      {statement && <SupplierStatement supplier={statement} onClose={() => setStatement(null)} onPay={() => { setPayFor(statement); setStatement(null) }} />}
    </div>
  )
}

function SupplierPayment({ supplier, onClose, onSave }) {
  const st = partyStats(supplier.id, 'supplier')
  const [form, setForm] = useState({ date: todayISO(), amount: '', currency: st.balances.SYP > 0 ? 'SYP' : 'USD', note: '' })
  const amount = Math.round(parseAmount(form.amount) * Math.pow(10, CURRENCIES[form.currency].decimals))
  return (
    <Modal open onClose={onClose} title="دفعة لمورد" subtitle={supplier.name} icon="cash" size="sm"
      footer={<>
        <button className="btn btn-gold" onClick={() => { if (!amount) return; onSave({ partyType: 'supplier', partyId: supplier.id, date: form.date, currency: form.currency, amount, note: form.note }) }}>تسجيل الدفعة</button>
        <button className="btn btn-ghost" onClick={onClose}>إلغاء</button>
      </>}>
      <div className="stack">
        <div className="row" style={{ gap: 8 }}>
          <span className="chip">ل.س <b>{money(st.balances.SYP, 'SYP', { bare: true })}</b></span>
          <span className="chip">$ <b>{money(st.balances.USD, 'USD', { bare: true })}</b></span>
        </div>
        <div className="grid g2" style={{ gap: 8 }}>
          <Field label="المبلغ"><input className="input" inputMode="decimal" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} autoFocus /></Field>
          <Field label="العملة"><select className="select" value={form.currency} onChange={(e) => setForm({ ...form, currency: e.target.value })}><option value="SYP">ليرة</option><option value="USD">دولار</option></select></Field>
        </div>
        <Field label="التاريخ"><input type="date" className="input" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} /></Field>
        <Field label="ملاحظة"><input className="input" value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} /></Field>
      </div>
    </Modal>
  )
}

function SupplierStatement({ supplier, onClose, onPay }) {
  const s = useStore()
  const st = partyStats(supplier.id, 'supplier')
  const [cur, setCur] = useState('SYP')
  const entries = useMemo(() => {
    const invs = st.invoices.filter((i) => i.currency === cur).map((i) => ({ id: i.id, date: i.date, label: `فاتورة شراء ${i.no}`, debit: i.total, credit: i.paid || 0 }))
    const pays = st.payments.filter((p) => p.currency === cur).map((p) => ({ id: p.id, date: p.date, label: p.note || 'دفعة', debit: 0, credit: p.amount }))
    return [...invs, ...pays].sort((a, b) => a.date.localeCompare(b.date))
  }, [st, cur])
  const balance = st.balances[cur]
  return (
    <Drawer open onClose={onClose} title={`كشف حساب — ${supplier.name}`} subtitle="حركات التوريد والدفعات" icon="book" width={540}
      footer={<><button className="btn btn-gold" onClick={onPay}><Icon name="cash" />دفعة</button><button className="btn btn-ghost" onClick={onClose}>إغلاق</button></>}>
      <div className="stack">
        <div className="grid g2" style={{ gap: 8 }}>
          <div className="card tight" style={{ background: 'var(--danger-soft)' }}><div className="tiny muted">مستحق ل.س</div><b>{money(st.balances.SYP, 'SYP')}</b></div>
          <div className="card tight" style={{ background: 'var(--info-soft)' }}><div className="tiny muted">مستحق $</div><b>{money(st.balances.USD, 'USD')}</b></div>
        </div>
        <Seg block value={cur} onChange={setCur} options={[{ id: 'SYP', label: 'كشف الليرة' }, { id: 'USD', label: 'كشف الدولار' }]} />
        <div className="table-wrap">
          <table>
            <thead><tr><th>التاريخ</th><th>البيان</th><th className="num">علينا</th><th className="num">دفعنا</th></tr></thead>
            <tbody>
              {entries.length === 0 && <tr><td colSpan={4} className="table-empty">لا حركات</td></tr>}
              {entries.map((e) => (
                <tr key={e.id}><td className="small">{fmtDate(e.date)}</td><td className="small"><b>{e.label}</b></td>
                  <td className="num">{e.debit ? money(e.debit, cur, { bare: true }) : '—'}</td>
                  <td className="num" style={{ color: 'var(--success)' }}>{e.credit ? money(e.credit, cur, { bare: true }) : '—'}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="row between card tight" style={{ background: 'var(--surface-2)' }}><b className="small">الرصيد النهائي</b><b>{money(balance, cur)}</b></div>
      </div>
    </Drawer>
  )
}
