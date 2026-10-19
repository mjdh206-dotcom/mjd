import { useMemo, useState } from 'react'
import { useStore, saveWorker, addWorkerTx, workerStats } from '../store'
import { money, parseAmount, fmtDate, todayISO, CURRENCIES } from '../utils'
import { Card, Empty, Field, Modal, Stat } from '../components'

export default function Workers({ onToast }) {
  const s = useStore()
  const [editing, setEditing] = useState(null)
  const [txFor, setTxFor] = useState(null)
  const [statement, setStatement] = useState(null)

  const totals = useMemo(() => {
    let net = 0, adv = 0
    s.workers.forEach((w) => {
      const st = workerStats(w.id)
      if (w.currency === 'SYP') { net += st.net; adv += st.advances.SYP }
    })
    return { net, adv }
  }, [s])

  return (
    <div className="grid" style={{ gap: 12 }}>
      <div className="grid grid-3">
        <Stat icon="👷" label="عدد العمال" value={String(s.workers.filter((w) => w.active).length)} />
        <Stat icon="💰" label="إجمالي السحوبات (ل.س)" value={money(totals.adv, 'SYP', { bare: true })} tone="gold" />
        <Stat icon="📊" label="صافي الرواتب المستحق (ل.س)" value={money(totals.net, 'SYP', { bare: true })} sub="الراتب − السحوبات" />
      </div>

      <Card>
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <div className="small muted">السحوبات تشمل السلف والبضاعة المسحوبة من المحل</div>
          <button className="btn btn-gold btn-sm" onClick={() => setEditing({ name: '', phone: '', salary: '', currency: 'SYP', active: true })}>＋ عامل جديد</button>
        </div>
      </Card>

      <Card title="العمال" badge={s.workers.length}>
        {s.workers.length === 0 ? <Empty icon="👷" /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>الاسم</th><th>الهاتف</th><th>الراتب</th><th>السحوبات</th><th>الصافي</th><th>العملة</th><th></th></tr></thead>
              <tbody>
                {s.workers.map((w) => {
                  const st = workerStats(w.id)
                  const adv = st.advances[w.currency] || 0
                  return (
                    <tr key={w.id}>
                      <td style={{ fontWeight: 700 }}>{w.name}</td>
                      <td className="small" style={{ direction: 'ltr' }}>{w.phone || '—'}</td>
                      <td style={{ direction: 'ltr' }}>{money(w.salary, w.currency, { bare: true })}</td>
                      <td style={{ direction: 'ltr', color: 'var(--danger)' }}>{money(adv, w.currency, { bare: true })}</td>
                      <td style={{ direction: 'ltr', fontWeight: 800, color: 'var(--success)' }}>{money(st.net, w.currency, { bare: true })}</td>
                      <td>{w.currency === 'SYP' ? 'ل.س' : '$'}</td>
                      <td>
                        <div className="row" style={{ gap: 4 }}>
                          <button className="btn btn-gold btn-sm" onClick={() => setTxFor(w)}>＋ سحب</button>
                          <button className="btn btn-outline btn-sm" onClick={() => setStatement(w)}>كشف حساب</button>
                          <button className="btn btn-outline btn-sm" onClick={() => setEditing({ ...w, salary: w.salary / Math.pow(10, CURRENCIES[w.currency].decimals) })}>تعديل</button>
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
        <Modal open onClose={() => setEditing(null)} title={editing.id ? 'تعديل عامل' : 'عامل جديد'}
          footer={<button className="btn btn-gold" onClick={() => {
            if (!editing.name.trim()) return
            saveWorker({ ...editing, salary: Math.round(parseAmount(editing.salary) * Math.pow(10, CURRENCIES[editing.currency].decimals)) })
            setEditing(null); onToast?.('تم حفظ العامل')
          }}>حفظ</button>}>
          <div className="grid" style={{ gap: 10 }}>
            <Field label="الاسم"><input className="input" value={editing.name} onChange={(e) => setEditing({ ...editing, name: e.target.value })} /></Field>
            <Field label="الهاتف"><input className="input" style={{ direction: 'ltr' }} value={editing.phone} onChange={(e) => setEditing({ ...editing, phone: e.target.value })} /></Field>
            <div className="grid grid-2" style={{ gap: 8 }}>
              <Field label="الراتب الشهري"><input className="input" inputMode="decimal" value={editing.salary} onChange={(e) => setEditing({ ...editing, salary: e.target.value })} /></Field>
              <Field label="العملة"><select className="select" value={editing.currency} onChange={(e) => setEditing({ ...editing, currency: e.target.value })}><option value="SYP">ليرة</option><option value="USD">دولار</option></select></Field>
            </div>
          </div>
        </Modal>
      )}

      {txFor && <WorkerTx worker={txFor} onClose={() => setTxFor(null)} onSave={(tx) => { addWorkerTx(tx); setTxFor(null); onToast?.('تم تسجيل السحب') }} />}
      {statement && <WorkerStatement worker={statement} onClose={() => setStatement(null)} />}
    </div>
  )
}

function WorkerTx({ worker, onClose, onSave }) {
  const [type, setType] = useState('advance')
  const [amount, setAmount] = useState('')
  const [currency, setCurrency] = useState(worker.currency || 'SYP')
  const [date, setDate] = useState(todayISO())
  const [note, setNote] = useState('')
  return (
    <Modal open onClose={onClose} title={`سحب عامل — ${worker.name}`}
      footer={<button className="btn btn-gold" onClick={() => {
        const a = Math.round(parseAmount(amount) * Math.pow(10, CURRENCIES[currency].decimals))
        if (a > 0) onSave({ workerId: worker.id, type, amount: a, currency, date, note })
      }}>حفظ</button>}>
      <div className="grid" style={{ gap: 10 }}>
        <div className="row" style={{ gap: 6 }}>
          <button className={`btn btn-sm ${type === 'advance' ? 'btn-brown' : 'btn-outline'}`} style={{ flex: 1 }} onClick={() => setType('advance')}>سلفة نقدية</button>
          <button className={`btn btn-sm ${type === 'goods' ? 'btn-brown' : 'btn-outline'}`} style={{ flex: 1 }} onClick={() => setType('goods')}>بضاعة</button>
        </div>
        <div className="grid grid-2" style={{ gap: 8 }}>
          <Field label="المبلغ"><input className="input" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} /></Field>
          <Field label="العملة"><select className="select" value={currency} onChange={(e) => setCurrency(e.target.value)}><option value="SYP">ليرة</option><option value="USD">دولار</option></select></Field>
        </div>
        <Field label="التاريخ"><input type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} /></Field>
        <Field label="ملاحظة"><input className="input" value={note} onChange={(e) => setNote(e.target.value)} /></Field>
      </div>
    </Modal>
  )
}

function WorkerStatement({ worker, onClose }) {
  const s = useStore()
  const st = workerStats(worker.id)
  const share = () => {
    const text = [
      `*${s.settings.shopName}*`,
      `كشف حساب العامل: ${worker.name}`,
      `الراتب: ${money(worker.salary, worker.currency)}`,
      ...st.txs.map((t) => `${fmtDate(t.date)} | ${t.type === 'advance' ? 'سلفة' : 'بضاعة'} | ${money(t.amount, t.currency)} ${t.note ? `— ${t.note}` : ''}`),
      '',
      `إجمالي السحوبات: ${money(st.advances[worker.currency] || 0, worker.currency)}`,
      `*صافي الراتب: ${money(st.net, worker.currency)}*`,
    ].join('\n')
    const phone = (worker.phone || '').replace(/[^0-9]/g, '')
    window.open(phone ? `https://wa.me/${phone.startsWith('963') ? phone : '963' + phone.replace(/^0/, '')}?text=${encodeURIComponent(text)}` : `https://wa.me/?text=${encodeURIComponent(text)}`, '_blank')
  }
  return (
    <Modal open onClose={onClose} wide title={`كشف حساب — ${worker.name}`}
      footer={<><button className="btn btn-gold" onClick={share}>إرسال واتساب</button><button className="btn btn-outline" onClick={onClose}>إغلاق</button></>}>
      <div className="grid grid-3" style={{ marginBottom: 10 }}>
        <div className="card" style={{ padding: 10 }}><div className="small muted">الراتب</div><b style={{ direction: 'ltr' }}>{money(worker.salary, worker.currency)}</b></div>
        <div className="card" style={{ padding: 10 }}><div className="small muted">السحوبات</div><b style={{ direction: 'ltr', color: 'var(--danger)' }}>{money(st.advances[worker.currency] || 0, worker.currency)}</b></div>
        <div className="card" style={{ padding: 10 }}><div className="small muted">الصافي</div><b style={{ direction: 'ltr', color: 'var(--success)' }}>{money(st.net, worker.currency)}</b></div>
      </div>
      <div className="table-wrap">
        <table>
          <thead><tr><th>التاريخ</th><th>النوع</th><th>المبلغ</th><th>العملة</th><th>ملاحظة</th></tr></thead>
          <tbody>
            {st.txs.map((t) => (
              <tr key={t.id}><td>{fmtDate(t.date)}</td><td>{t.type === 'advance' ? 'سلفة' : 'بضاعة'}</td><td style={{ direction: 'ltr' }}>{money(t.amount, t.currency)}</td><td>{t.currency}</td><td>{t.note || '—'}</td></tr>
            ))}
            {st.txs.length === 0 && <tr><td colSpan={5}><Empty text="لا توجد سحوبات" /></td></tr>}
          </tbody>
        </table>
      </div>
    </Modal>
  )
}
