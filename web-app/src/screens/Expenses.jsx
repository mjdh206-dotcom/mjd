import { useMemo, useState } from 'react'
import { useStore, addExpense, cashFlow, saveClosing } from '../store'
import { money, parseAmount, fmtDate, todayISO, CURRENCIES, startOfMonth } from '../utils'
import { Card, Empty, Field, Stat } from '../components'

const CATEGORIES = ['إيجار', 'كهرباء', 'ماء', 'أجور', 'مواصلات', 'أكياس وتغليف', 'صيانة', 'ضرائب', 'أخرى']

export default function Expenses({ onToast, cashOnly }) {
  if (cashOnly) return <CashScreen onToast={onToast} />
  return <ExpensesScreen onToast={onToast} />
}

function ExpensesScreen({ onToast }) {
  const s = useStore()
  const [from, setFrom] = useState(startOfMonth())
  const [to, setTo] = useState(todayISO())
  const [form, setForm] = useState({ date: todayISO(), category: 'كهرباء', amount: '', currency: 'SYP', note: '' })

  const list = useMemo(() => s.expenses.filter((e) => e.date >= from && e.date <= to).sort((a, b) => b.date.localeCompare(a.date)), [s.expenses, from, to])
  const tSYP = list.filter((e) => e.currency === 'SYP').reduce((a, b) => a + b.amount, 0)
  const tUSD = list.filter((e) => e.currency === 'USD').reduce((a, b) => a + b.amount, 0)
  const byCat = useMemo(() => {
    const m = {}
    list.forEach((e) => { m[e.category] = (m[e.category] || 0) + (e.currency === 'SYP' ? e.amount : e.amount * 100) })
    return Object.entries(m).sort((a, b) => b[1] - a[1])
  }, [list])

  return (
    <div className="grid" style={{ gap: 12 }}>
      <div className="grid grid-3">
        <Stat icon="💸" label="مصاريف الفترة — ليرة" value={money(tSYP, 'SYP', { bare: true })} tone="gold" />
        <Stat icon="💵" label="مصاريف الفترة — دولار" value={money(tUSD, 'USD', { bare: true })} />
        <Stat icon="📊" label="عدد المصاريف" value={String(list.length)} />
      </div>

      <Card title="تسجيل مصروف">
        <div className="grid" style={{ gridTemplateColumns: '1fr 1fr 1fr 2fr auto', gap: 8, alignItems: 'end' }}>
          <Field label="التاريخ"><input type="date" className="input" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} /></Field>
          <Field label="البند">
            <select className="select" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
              {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          </Field>
          <Field label="المبلغ / العملة">
            <div className="row" style={{ gap: 4, flexWrap: 'nowrap' }}>
              <input className="input" inputMode="decimal" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} />
              <select className="select" style={{ width: 85 }} value={form.currency} onChange={(e) => setForm({ ...form, currency: e.target.value })}>
                <option value="SYP">ل.س</option><option value="USD">$</option>
              </select>
            </div>
          </Field>
          <Field label="ملاحظة"><input className="input" value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} /></Field>
          <button className="btn btn-gold" onClick={() => {
            const amount = Math.round(parseAmount(form.amount) * Math.pow(10, CURRENCIES[form.currency].decimals))
            if (amount <= 0) return
            addExpense({ ...form, amount })
            setForm({ ...form, amount: '', note: '' })
            onToast?.('تم تسجيل المصروف')
          }}>تسجيل</button>
        </div>
      </Card>

      <div className="grid grid-2">
        <Card title="توزيع المصاريف">
          {byCat.length === 0 ? <Empty icon="💸" /> : byCat.map(([cat, total]) => (
            <div key={cat} className="row" style={{ justifyContent: 'space-between', padding: '7px 0', borderBottom: '1px solid var(--border)' }}>
              <span className="small">{cat}</span>
              <b className="small" style={{ direction: 'ltr' }}>{money(Math.round(total), 'SYP')}</b>
            </div>
          ))}
        </Card>
        <Card title="فلترة">
          <div className="row">
            <Field label="من"><input type="date" className="input" value={from} onChange={(e) => setFrom(e.target.value)} /></Field>
            <Field label="إلى"><input type="date" className="input" value={to} onChange={(e) => setTo(e.target.value)} /></Field>
          </div>
        </Card>
      </div>

      <Card title="قائمة المصاريف" badge={list.length}>
        {list.length === 0 ? <Empty icon="💸" /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>التاريخ</th><th>البند</th><th>المبلغ</th><th>العملة</th><th>ملاحظة</th><th>بواسطة</th></tr></thead>
              <tbody>
                {list.map((e) => (
                  <tr key={e.id}>
                    <td>{fmtDate(e.date)}</td><td style={{ fontWeight: 700 }}>{e.category}</td>
                    <td style={{ direction: 'ltr' }}>{money(e.amount, e.currency, { bare: true })}</td>
                    <td>{e.currency === 'SYP' ? 'ليرة' : 'دولار'}</td>
                    <td className="small">{e.note || '—'}</td><td className="small muted">{e.createdBy}</td>
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

function CashScreen({ onToast }) {
  const s = useStore()
  const [date, setDate] = useState(todayISO())
  const [currency, setCurrency] = useState('SYP')
  const [opening, setOpening] = useState('')
  const [actual, setActual] = useState('')
  const [note, setNote] = useState('')

  const dec = CURRENCIES[currency].decimals
  const flow = cashFlow(currency, date, date)
  const openingMinor = Math.round(parseAmount(opening) * Math.pow(10, dec))
  const actualMinor = Math.round(parseAmount(actual) * Math.pow(10, dec))
  const systemCash = openingMinor + flow.net
  const diff = actual === '' ? 0 : actualMinor - systemCash

  const closings = s.closings.filter((c) => c.currency === currency).sort((a, b) => b.date.localeCompare(a.date))

  return (
    <div className="grid" style={{ gap: 12 }}>
      <div className="grid grid-4">
        <Stat icon="📥" label="مقبوضات اليوم" value={money(flow.cashIn + flow.payIn, currency, { bare: true })} sub={currency === 'SYP' ? 'ل.س' : '$'} tone="gold" />
        <Stat icon="📤" label="مدفوعات اليوم" value={money(flow.out, currency, { bare: true })} sub={`مصاريف ${money(flow.expenses, currency, { bare: true })}`} />
        <Stat icon="💰" label="صافي الحركة" value={money(flow.net, currency, { bare: true })} />
        <Stat icon="🏦" label="صندوق منفصل" value={currency === 'SYP' ? 'الليرة' : 'الدولار'} sub="كل عملة بصندوق مستقل" />
      </div>

      <Card title="إقفال صندوق يومي">
        <div className="grid" style={{ gridTemplateColumns: 'repeat(4,1fr)', gap: 8, alignItems: 'end' }}>
          <Field label="التاريخ"><input type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} /></Field>
          <Field label="العملة">
            <select className="select" value={currency} onChange={(e) => setCurrency(e.target.value)}>
              <option value="SYP">ليرة سورية</option><option value="USD">دولار أمريكي</option>
            </select>
          </Field>
          <Field label="الرصيد الافتتاحي"><input className="input" inputMode="decimal" value={opening} onChange={(e) => setOpening(e.target.value)} /></Field>
          <Field label="المبلغ الفعلي في الدرج"><input className="input" inputMode="decimal" value={actual} onChange={(e) => setActual(e.target.value)} /></Field>
        </div>

        <div className="grid grid-3" style={{ marginTop: 12 }}>
          <div className="card" style={{ background: 'var(--cream)' }}>
            <div className="small muted">المبلغ المحسوب</div>
            <b style={{ fontSize: 18, direction: 'ltr', display: 'block' }}>{money(systemCash, currency)}</b>
            <div className="small muted">افتتاحي + مقبوضات − مدفوعات</div>
          </div>
          <div className="card" style={{ background: 'var(--cream)' }}>
            <div className="small muted">المبلغ الفعلي</div>
            <b style={{ fontSize: 18, direction: 'ltr', display: 'block' }}>{money(actualMinor, currency)}</b>
          </div>
          <div className="card" style={{ background: diff === 0 ? '#E8F5E9' : diff > 0 ? '#FFF8E1' : '#FFEBEE' }}>
            <div className="small muted">الفرق</div>
            <b style={{ fontSize: 18, direction: 'ltr', display: 'block', color: diff < 0 ? 'var(--danger)' : 'var(--success)' }}>{money(diff, currency)}</b>
            <div className="small">{diff === 0 ? 'مطابق' : diff > 0 ? 'زيادة في الدرج' : 'نقص في الدرج'}</div>
          </div>
        </div>

        <Field label="ملاحظة" style={{ marginTop: 10 }}><input className="input" value={note} onChange={(e) => setNote(e.target.value)} /></Field>
        <button className="btn btn-gold" style={{ marginTop: 10 }} onClick={() => {
          if (actual === '') return
          saveClosing({ date, currency, opening: openingMinor, systemCash, actualCash: actualMinor, diff, note })
          setActual(''); setNote(''); setOpening('')
          onToast?.('تم حفظ إقفال الصندوق')
        }}>حفظ الإقفال</button>
      </Card>

      <Card title="سجل الإقفالات" badge={closings.length}>
        {closings.length === 0 ? <Empty icon="💰" text="لا توجد إقفالات بعد" /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>التاريخ</th><th>افتتاحي</th><th>محسوب</th><th>فعلي</th><th>الفرق</th><th>ملاحظة</th><th>بواسطة</th></tr></thead>
              <tbody>
                {closings.map((c) => (
                  <tr key={c.id}>
                    <td>{fmtDate(c.date)}</td>
                    <td style={{ direction: 'ltr' }}>{money(c.opening, c.currency, { bare: true })}</td>
                    <td style={{ direction: 'ltr' }}>{money(c.systemCash, c.currency, { bare: true })}</td>
                    <td style={{ direction: 'ltr' }}>{money(c.actualCash, c.currency, { bare: true })}</td>
                    <td style={{ direction: 'ltr', color: c.diff === 0 ? 'inherit' : c.diff < 0 ? 'var(--danger)' : 'var(--success)', fontWeight: 700 }}>{money(c.diff, c.currency, { bare: true })}</td>
                    <td className="small">{c.note || '—'}</td>
                    <td className="small muted">{c.createdBy}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Card title="تفصيل حركة الصندوق اليوم">
        <div className="table-wrap">
          <table>
            <tbody>
              <tr><td>مبيعات نقدية</td><td style={{ direction: 'ltr' }}>{money(flow.cashIn, currency)}</td></tr>
              <tr><td>دفعات زبائن</td><td style={{ direction: 'ltr' }}>{money(flow.payIn, currency)}</td></tr>
              <tr><td>مدفوعات للموردين</td><td style={{ direction: 'ltr', color: 'var(--danger)' }}>- {money(flow.purchases, currency)}</td></tr>
              <tr><td>مصاريف</td><td style={{ direction: 'ltr', color: 'var(--danger)' }}>- {money(flow.expenses, currency)}</td></tr>
              <tr><td>مسحوبات صاحب المحل</td><td style={{ direction: 'ltr', color: 'var(--danger)' }}>- {money(flow.withdrawals, currency)}</td></tr>
              <tr><td>سحوبات العمال</td><td style={{ direction: 'ltr', color: 'var(--danger)' }}>- {money(flow.workerTx, currency)}</td></tr>
              <tr><td><b>الصافي</b></td><td style={{ direction: 'ltr', fontWeight: 800 }}>{money(flow.net, currency)}</td></tr>
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  )
}
