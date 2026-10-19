import { useMemo, useState } from 'react'
import { useStore, addWithdrawal, cashFlow } from '../store'
import { CURRENCIES, money, parseAmount, fmtDate, todayISO, startOfMonth } from '../utils'
import { Card, Empty, Field, PageHead, Seg } from '../components'
import { Icon } from '../icons'

export default function Withdrawals({ onToast }) {
  const s = useStore()
  const [form, setForm] = useState({ date: todayISO(), amount: '', currency: 'SYP', note: '' })
  const [range, setRange] = useState('month')
  const from = range === 'month' ? startOfMonth() : range === 'year' ? `${todayISO().slice(0, 4)}-01-01` : '2000-01-01'
  const to = todayISO()

  const list = useMemo(() => s.withdrawals.filter((w) => w.date >= from && w.date <= to).sort((a, b) => b.date.localeCompare(a.date)), [s.withdrawals, from, to])
  const tS = list.filter((w) => w.currency === 'SYP').reduce((a, b) => a + b.amount, 0)
  const tU = list.filter((w) => w.currency === 'USD').reduce((a, b) => a + b.amount, 0)
  const cash = { SYP: cashFlow('SYP', todayISO(), todayISO()), USD: cashFlow('USD', todayISO(), todayISO()) }

  const add = () => {
    const amount = Math.round(parseAmount(form.amount) * Math.pow(10, CURRENCIES[form.currency].decimals))
    if (amount <= 0) return onToast?.('أدخل مبلغاً صحيحاً', 'error')
    addWithdrawal({ ...form, amount })
    setForm({ ...form, amount: '', note: '' })
    onToast?.('تم تسجيل المسحوب')
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      <PageHead title="مسحوبات المحل" subtitle="مسحوبات صاحب المحل تُخصم من الصندوق ولا تُحسب ضمن مصاريف التشغيل" />

      <div className="grid g4">
        <div className="kpi gold"><div className="top"><span className="lbl">مسحوبات الفترة — ليرة</span><span className="ic"><Icon name="coins" /></span></div><div className="val">{money(tS, 'SYP', { bare: true })}</div><div className="foot">{list.filter((w) => w.currency === 'SYP').length} عملية</div></div>
        <div className="kpi"><div className="top"><span className="lbl">مسحوبات الفترة — دولار</span><span className="ic"><Icon name="dollar" /></span></div><div className="val">{money(tU, 'USD', { bare: true })}</div><div className="foot">{list.filter((w) => w.currency === 'USD').length} عملية</div></div>
        <div className="kpi"><div className="top"><span className="lbl">صندوق الليرة اليوم</span><span className="ic"><Icon name="bank" /></span></div><div className="val">{money(cash.SYP.net, 'SYP', { bare: true })}</div><div className="foot">بعد خصم المسحوبات</div></div>
        <div className="kpi"><div className="top"><span className="lbl">صندوق الدولار اليوم</span><span className="ic"><Icon name="cash" /></span></div><div className="val">{money(cash.USD.net, 'USD', { bare: true })}</div><div className="foot">بعد خصم المسحوبات</div></div>
      </div>

      <div className="grid" style={{ gridTemplateColumns: 'minmax(0,1fr) minmax(0,1.5fr)', gap: 16, alignItems: 'start' }}>
        <Card title="تسجيل مسحوب" icon="coins" subtitle="يُخصم من صندوق العملة المختارة">
          <div className="stack">
            <Field label={`المبلغ (${CURRENCIES[form.currency].name})`}>
              <div style={{ position: 'relative' }}>
                <input className="input" inputMode="decimal" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} placeholder="0" style={{ height: 46, fontSize: 18, fontWeight: 800, paddingInlineEnd: 48 }} />
                <span className="money-suffix">{CURRENCIES[form.currency].short}</span>
              </div>
            </Field>
            <Seg block value={form.currency} onChange={(v) => setForm({ ...form, currency: v })} options={[{ id: 'SYP', label: 'ليرة سورية' }, { id: 'USD', label: 'دولار' }]} />
            <Field label="التاريخ"><input type="date" className="input" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} /></Field>
            <Field label="البيان"><input className="input" value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} placeholder="مصاريف منزل، سفر…" /></Field>
            <button className="btn btn-gold" onClick={add}><Icon name="plus" />تسجيل المسحوب</button>
          </div>
        </Card>

        <Card title="سجل المسحوبات" icon="history" badge={list.length}
          action={<Seg value={range} onChange={setRange} options={[{ id: 'month', label: 'هذا الشهر' }, { id: 'year', label: 'هذه السنة' }, { id: 'all', label: 'الكل' }]} size="sm" />}>
          {list.length === 0 ? <Empty icon="coins" title="لا مسحوبات" /> : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>التاريخ</th><th>البيان</th><th>العملة</th><th className="num">المبلغ</th><th>بواسطة</th></tr></thead>
                <tbody>
                  {list.map((w) => (
                    <tr key={w.id}>
                      <td>{fmtDate(w.date)}</td><td>{w.note || '—'}</td>
                      <td><span className="badge badge-neutral">{w.currency === 'SYP' ? 'ل.س' : '$'}</span></td>
                      <td className="num strong" style={{ color: 'var(--danger)' }}>{money(w.amount, w.currency, { bare: true })}</td>
                      <td className="small">{w.createdBy}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </div>
    </div>
  )
}
