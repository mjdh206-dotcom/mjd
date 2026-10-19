import { useMemo, useState } from 'react'
import { useStore, addWithdrawal } from '../store'
import { money, parseAmount, fmtDate, todayISO, CURRENCIES, startOfMonth } from '../utils'
import { Card, Empty, Field, Stat } from '../components'

export default function Withdrawals({ onToast }) {
  const s = useStore()
  const [from, setFrom] = useState(startOfMonth())
  const [to, setTo] = useState(todayISO())
  const [form, setForm] = useState({ date: todayISO(), amount: '', currency: 'SYP', note: '' })

  const list = useMemo(() => s.withdrawals.filter((w) => w.date >= from && w.date <= to).sort((a, b) => b.date.localeCompare(a.date)), [s.withdrawals, from, to])
  const totalSYP = list.filter((w) => w.currency === 'SYP').reduce((a, b) => a + b.amount, 0)
  const totalUSD = list.filter((w) => w.currency === 'USD').reduce((a, b) => a + b.amount, 0)

  const submit = () => {
    const amount = Math.round(parseAmount(form.amount) * Math.pow(10, CURRENCIES[form.currency].decimals))
    if (amount <= 0) return
    addWithdrawal({ ...form, amount })
    setForm({ ...form, amount: '', note: '' })
    onToast?.('تم تسجيل المسحوب')
  }

  return (
    <div className="grid" style={{ gap: 12 }}>
      <div className="grid grid-3">
        <Stat icon="🏦" label="إجمالي المسحوبات — ليرة" value={money(totalSYP, 'SYP', { bare: true })} sub={`${list.filter((w) => w.currency === 'SYP').length} عملية`} tone="gold" />
        <Stat icon="💵" label="إجمالي المسحوبات — دولار" value={money(totalUSD, 'USD', { bare: true })} sub={`${list.filter((w) => w.currency === 'USD').length} عملية`} />
        <Stat icon="📅" label="الفترة" value={`${fmtDate(from)} → ${fmtDate(to)}`} />
      </div>

      <Card title="تسجيل مسحوب جديد">
        <div className="grid" style={{ gridTemplateColumns: '1fr 1fr 1fr 2fr auto', gap: 8, alignItems: 'end' }}>
          <Field label="التاريخ"><input type="date" className="input" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} /></Field>
          <Field label="المبلغ"><input className="input" inputMode="decimal" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} /></Field>
          <Field label="العملة">
            <select className="select" value={form.currency} onChange={(e) => setForm({ ...form, currency: e.target.value })}>
              <option value="SYP">ليرة سورية</option><option value="USD">دولار</option>
            </select>
          </Field>
          <Field label="الملاحظة"><input className="input" value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} placeholder="مثال: مصاريف منزل" /></Field>
          <button className="btn btn-gold" onClick={submit}>تسجيل</button>
        </div>
      </Card>

      <Card>
        <div className="row" style={{ marginBottom: 10 }}>
          <Field label="من"><input type="date" className="input" value={from} onChange={(e) => setFrom(e.target.value)} /></Field>
          <Field label="إلى"><input type="date" className="input" value={to} onChange={(e) => setTo(e.target.value)} /></Field>
        </div>
        {list.length === 0 ? <Empty icon="🏦" text="لا توجد مسحوبات في هذه الفترة" /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>التاريخ</th><th>المبلغ</th><th>العملة</th><th>الملاحظة</th><th>بواسطة</th></tr></thead>
              <tbody>
                {list.map((w) => (
                  <tr key={w.id}>
                    <td>{fmtDate(w.date)}</td>
                    <td style={{ direction: 'ltr', fontWeight: 700 }}>{money(w.amount, w.currency, { bare: true })}</td>
                    <td>{w.currency === 'SYP' ? 'ليرة' : 'دولار'}</td>
                    <td>{w.note || '—'}</td>
                    <td className="small muted">{w.createdBy}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  )
}
