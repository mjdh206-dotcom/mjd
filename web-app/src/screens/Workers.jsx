import { useMemo, useState } from 'react'
import { useStore, saveWorker, addWorkerTx, workerStats } from '../store'
import { CURRENCIES, money, parseAmount, fmtDate, todayISO } from '../utils'
import { Card, Empty, Field, Modal, PageHead, Avatar, ProgressRow, Seg, ConfirmDialog } from '../components'
import { Icon } from '../icons'

export default function Workers({ onToast }) {
  const s = useStore()
  const [editing, setEditing] = useState(null)
  const [txFor, setTxFor] = useState(null)
  const [txType, setTxType] = useState('advance')
  const [q, setQ] = useState('')

  const list = s.workers.filter((w) => !q || w.name.includes(q))
  const totals = useMemo(() => {
    let salaries = 0, adv = 0, net = 0
    s.workers.filter((w) => w.active).forEach((w) => {
      const st = workerStats(w.id)
      const factor = w.currency === 'USD' ? 100 : 1
      salaries += w.salary / factor
      adv += (w.currency === 'SYP' ? st.advances.SYP : st.advances.USD) / factor
      net += st.net / factor
    })
    return { salaries, adv, net }
  }, [s.workers, s.workerTx])

  return (
    <div className="grid" style={{ gap: 16 }}>
      <PageHead title="العمال والسحوبات" subtitle="صافي الراتب = الراتب − السلف والبضاعة المسحوبة من المحل"
        actions={<button className="btn btn-gold" onClick={() => setEditing({ name: '', phone: '', salary: '', currency: 'SYP', active: true })}><Icon name="plus" />عامل جديد</button>} />

      <div className="grid g3">
        <div className="kpi"><div className="top"><span className="lbl">إجمالي الرواتب</span><span className="ic"><Icon name="employee" /></span></div><div className="val">{money(totals.salaries, 'SYP', { bare: true })}</div><div className="foot">ل.س — مكافئ للعمال النشطين</div></div>
        <div className="kpi gold"><div className="top"><span className="lbl">السلف والبضاعة</span><span className="ic"><Icon name="coins" /></span></div><div className="val">{money(totals.adv, 'SYP', { bare: true })}</div><div className="foot">مخصومة من الراتب</div></div>
        <div className="kpi"><div className="top"><span className="lbl">صافي المستحق للدفع</span><span className="ic"><Icon name="wallet" /></span></div><div className="val">{money(totals.net, 'SYP', { bare: true })}</div><div className="foot">بعد كل الخصومات</div></div>
      </div>

      <Card tight>
        <div className="field-icon"><Icon name="search" /><input className="input" placeholder="ابحث عن عامل…" value={q} onChange={(e) => setQ(e.target.value)} /></div>
      </Card>

      <div className="grid g2">
        {list.map((w) => {
          const st = workerStats(w.id)
          const adv = w.currency === 'SYP' ? st.advances.SYP : st.advances.USD
          const pct = w.salary ? Math.min(100, (adv / w.salary) * 100) : 0
          return (
            <Card key={w.id}>
              <div className="row between" style={{ marginBottom: 10 }}>
                <div className="row" style={{ gap: 10 }}>
                  <Avatar name={w.name} size="lg" />
                  <div>
                    <div style={{ fontSize: 14, fontWeight: 800 }}>{w.name}</div>
                    <div className="tiny muted">{w.phone || 'بدون هاتف'} · {w.active ? 'على رأس العمل' : 'موقوف'}</div>
                  </div>
                </div>
                <span className="badge badge-gold">{money(w.salary, w.currency)} / شهر</span>
              </div>
              <div className="grid g3" style={{ gap: 6, marginBottom: 10 }}>
                <div className="card tight" style={{ background: 'var(--surface-2)', padding: 9 }}><div className="tiny muted">الراتب</div><b className="small">{money(w.salary, w.currency, { bare: true })}</b></div>
                <div className="card tight" style={{ background: 'var(--danger-soft)', padding: 9 }}><div className="tiny muted">السحوبات</div><b className="small" style={{ color: 'var(--danger)' }}>{money(adv, w.currency, { bare: true })}</b></div>
                <div className="card tight" style={{ background: 'var(--success-soft)', padding: 9 }}><div className="tiny muted">الصافي</div><b className="small" style={{ color: 'var(--success)' }}>{money(st.net, w.currency, { bare: true })}</b></div>
              </div>
              <ProgressRow label="نسبة السحوبات من الراتب" value={adv} max={w.salary || 1} right={`${pct.toFixed(0)}%`} color={pct > 70 ? 'var(--danger)' : 'var(--gold)'} />
              <div className="row" style={{ gap: 5 }}>
                <button className="btn btn-gold btn-sm" onClick={() => { setTxFor(w); setTxType('advance') }}><Icon name="cash" />سلفة</button>
                <button className="btn btn-ghost btn-sm" onClick={() => { setTxFor(w); setTxType('goods') }}><Icon name="bag" />بضاعة</button>
                <button className="btn btn-ghost btn-sm" onClick={() => setEditing({ ...w, salary: w.salary / (w.currency === 'USD' ? 100 : 1) })}><Icon name="edit" />تعديل</button>
                <span className="spacer" />
                <span className="tiny muted">{st.txs.length} حركة</span>
              </div>
            </Card>
          )
        })}
      </div>

      {list.length === 0 && <Card><Empty icon="employee" title="لا عمال" text="أضف العاملين لتتبع رواتبهم وسلفهم." /></Card>}

      <Card title="آخر السحوبات" icon="history" badge={s.workerTx.length}>
        {s.workerTx.length === 0 ? <Empty icon="coins" title="لا حركات" /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>التاريخ</th><th>العامل</th><th>النوع</th><th>ملاحظة</th><th className="num">المبلغ</th></tr></thead>
              <tbody>
                {s.workerTx.slice(0, 30).map((t) => {
                  const w = s.workers.find((x) => x.id === t.workerId)
                  return (
                    <tr key={t.id}>
                      <td>{fmtDate(t.date)}</td>
                      <td><b>{w?.name || '—'}</b></td>
                      <td><span className={`badge ${t.type === 'advance' ? 'badge-warn' : 'badge-blue'}`}>{t.type === 'advance' ? 'سلفة نقدية' : 'بضاعة'}</span></td>
                      <td className="small muted">{t.note || '—'}</td>
                      <td className="num strong" style={{ color: 'var(--danger)' }}>{money(t.amount, t.currency)}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {editing && (
        <Modal open onClose={() => setEditing(null)} title={editing.id ? 'تعديل عامل' : 'عامل جديد'} icon="employee" size="sm"
          footer={<>
            <button className="btn btn-gold" onClick={() => {
              if (!editing.name.trim()) return onToast?.('أدخل اسم العامل', 'error')
              const salary = Math.round(parseAmount(editing.salary) * (editing.currency === 'USD' ? 100 : 1))
              saveWorker({ ...editing, salary })
              setEditing(null); onToast?.('تم حفظ العامل')
            }}>حفظ</button>
            <button className="btn btn-ghost" onClick={() => setEditing(null)}>إلغاء</button>
          </>}>
          <div className="stack">
            <Field label="اسم العامل" required><input className="input" value={editing.name} onChange={(e) => setEditing({ ...editing, name: e.target.value })} autoFocus /></Field>
            <Field label="الهاتف"><input className="input" style={{ direction: 'ltr' }} value={editing.phone} onChange={(e) => setEditing({ ...editing, phone: e.target.value })} /></Field>
            <div className="grid g2" style={{ gap: 8 }}>
              <Field label="الراتب الشهري"><input className="input" inputMode="decimal" value={editing.salary} onChange={(e) => setEditing({ ...editing, salary: e.target.value })} /></Field>
              <Field label="العملة"><select className="select" value={editing.currency} onChange={(e) => setEditing({ ...editing, currency: e.target.value })}><option value="SYP">ليرة</option><option value="USD">دولار</option></select></Field>
            </div>
            <Field label="الحالة"><Seg block value={editing.active ? 'active' : 'inactive'} onChange={(v) => setEditing({ ...editing, active: v === 'active' })} options={[{ id: 'active', label: 'على رأس العمل' }, { id: 'inactive', label: 'موقوف' }]} /></Field>
          </div>
        </Modal>
      )}

      {txFor && (
        <Modal open onClose={() => setTxFor(null)} title={txType === 'advance' ? 'سلفة نقدية' : 'بضاعة مسحوبة'} subtitle={txFor.name} icon={txType === 'advance' ? 'cash' : 'bag'} size="sm"
          footer={<>
            <button className="btn btn-gold" onClick={() => {
              const amount = Math.round(parseAmount(txFor._amount) * (txFor.currency === 'USD' ? 100 : 1))
              if (!amount) return
              addWorkerTx({ workerId: txFor.id, date: txFor._date || todayISO(), type: txType, amount, currency: txFor.currency, note: txFor._note || '' })
              setTxFor(null); onToast?.('تم تسجيل الحركة')
            }}>تسجيل</button>
            <button className="btn btn-ghost" onClick={() => setTxFor(null)}>إلغاء</button>
          </>}>
          <TxFields worker={txFor} onChange={(patch) => setTxFor({ ...txFor, ...patch })} type={txType} />
        </Modal>
      )}
    </div>
  )
}

function TxFields({ worker, onChange, type }) {
  return (
    <div className="stack">
      <Field label={`المبلغ (${CURRENCIES[worker.currency].name})`}>
        <div style={{ position: 'relative' }}>
          <input className="input" inputMode="decimal" value={worker._amount || ''} onChange={(e) => onChange({ _amount: e.target.value })} placeholder="0" style={{ height: 46, fontSize: 17, fontWeight: 800, paddingInlineEnd: 48 }} autoFocus />
          <span className="money-suffix">{CURRENCIES[worker.currency].short}</span>
        </div>
      </Field>
      <Field label="التاريخ"><input type="date" className="input" value={worker._date || todayISO()} onChange={(e) => onChange({ _date: e.target.value })} /></Field>
      <Field label="ملاحظة"><input className="input" value={worker._note || ''} onChange={(e) => onChange({ _note: e.target.value })} placeholder={type === 'advance' ? 'سلفة نقدية…' : 'مكسرات، قهوة…'} /></Field>
      <div className="tiny" style={{ background: 'var(--warn-soft)', padding: 9, borderRadius: 8, color: 'var(--warn)', fontWeight: 600 }}>
        سيُخصم المبلغ من صافي راتب العامل ويُسجَّل في سجل العمليات.
      </div>
    </div>
  )
}
