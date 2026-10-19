import { useMemo, useState } from 'react'
import { useStore, reportFor, topItems, rateNow } from '../store'
import {
  money, fmtDate, todayISO, startOfMonth, convert, CURRENCIES, rateToNumber, CATEGORIES, lineTotal,
} from '../utils'
import { Card, Empty, Field, Stat, Tabs } from '../components'

const csvEscape = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`

export default function Reports({ onToast }) {
  const s = useStore()
  const isManager = s.session?.role === 'manager'
  const [tab, setTab] = useState('month')
  const [from, setFrom] = useState(startOfMonth())
  const [to, setTo] = useState(todayISO())
  const [display, setDisplay] = useState('SYP')

  const range = useMemo(() => {
    if (tab === 'today') return [todayISO(), todayISO()]
    if (tab === 'week') {
      const d = new Date(); d.setDate(d.getDate() - 6)
      return [d.toISOString().slice(0, 10), todayISO()]
    }
    if (tab === 'month') return [startOfMonth(), todayISO()]
    return [from, to]
  }, [tab, from, to])

  const r = useMemo(() => reportFor(range[0], range[1]), [s, range])
  const top = useMemo(() => topItems(range[0], range[1], 12), [s, range])
  const rate = s.settings.rate

  // تحويل للمجموع المقدّر
  const toDisplay = (minor, currency) => {
    if (display === 'MIX') return minor
    return convert(minor, currency, display, rate)
  }
  const sum = (fn) => {
    if (display === 'MIX') {
      return r.byCur.SYP[fn] + convert(r.byCur.USD[fn], 'USD', 'SYP', rate)
    }
    return r.byCur.SYP[fn] + convert(r.byCur.USD[fn], 'USD', display, rate)
  }

  const curLabel = display === 'MIX' ? 'ل.س (مقدّر)' : CURRENCIES[display].name

  const exportCSV = () => {
    const rows = []
    rows.push(['محامص الحرمين — تقرير', ''])
    rows.push(['من', range[0], 'إلى', range[1]])
    rows.push(['سعر الصرف المستخدم', rateToNumber(rate)])
    rows.push([])
    rows.push(['البند', 'ليرة سورية', 'دولار أمريكي'])
    rows.push(['المبيعات', r.byCur.SYP.sales, (r.byCur.USD.sales / 100).toFixed(2)])
    rows.push(['عدد فواتير البيع', r.byCur.SYP.count, r.byCur.USD.count])
    rows.push(['المشتريات', r.byCur.SYP.purchases, (r.byCur.USD.purchases / 100).toFixed(2)])
    rows.push(['المصاريف', r.byCur.SYP.expenses, (r.byCur.USD.expenses / 100).toFixed(2)])
    rows.push(['المسحوبات', r.byCur.SYP.withdrawals, (r.byCur.USD.withdrawals / 100).toFixed(2)])
    rows.push(['سحوبات العمال', r.byCur.SYP.workerTx, (r.byCur.USD.workerTx / 100).toFixed(2)])
    if (isManager) {
      rows.push(['الربح الإجمالي', r.byCur.SYP.profit, (r.byCur.USD.profit / 100).toFixed(2)])
      rows.push(['صافي الربح بعد المصاريف', r.byCur.SYP.profit - r.byCur.SYP.expenses - r.byCur.SYP.withdrawals - r.byCur.SYP.workerTx, ((r.byCur.USD.profit - r.byCur.USD.expenses - r.byCur.USD.withdrawals - r.byCur.USD.workerTx) / 100).toFixed(2)])
    }
    rows.push([])
    rows.push(['الأصناف الأكثر مبيعاً'])
    rows.push(['الصنف', 'الكمية (غ/قطعة)', 'الإيراد', 'الربح'])
    top.forEach((t) => rows.push([t.name, t.qty, (t.revenue / 100).toFixed(2), (t.profit / 100).toFixed(2)]))
    rows.push([])
    rows.push(['رقم الفاتورة', 'التاريخ', 'الزبون', 'العملة', 'الإجمالي', 'المدفوع', 'المتبقي', 'الحالة'])
    r.sales.forEach((i) => rows.push([i.no, i.date, i.partyName, i.currency, (i.total / Math.pow(10, CURRENCIES[i.currency].decimals)).toFixed(2), (i.paid / Math.pow(10, CURRENCIES[i.currency].decimals)).toFixed(2), (i.remaining / Math.pow(10, CURRENCIES[i.currency].decimals)).toFixed(2), i.status === 'cancelled' ? 'ملغاة' : 'مكتملة']))

    const csv = '\uFEFF' + rows.map((row) => row.map(csvEscape).join(',')).join('\r\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `تقرير-محامص-الحرمين-${range[0]}_${range[1]}.csv`
    a.click()
    onToast?.('تم تصدير ملف Excel (CSV)')
  }

  const exportPDF = () => {
    window.print()
    onToast?.('اختر "حفظ كـ PDF" من نافذة الطباعة')
  }

  return (
    <div className="grid" style={{ gap: 12 }}>
      <Card>
        <Tabs value={tab} onChange={setTab} tabs={[
          { id: 'today', label: 'اليوم' },
          { id: 'week', label: 'آخر 7 أيام' },
          { id: 'month', label: 'هذا الشهر' },
          { id: 'custom', label: 'فترة مخصصة' },
        ]} />
        <div className="grid" style={{ gridTemplateColumns: '1fr 1fr 1.2fr auto', gap: 8, alignItems: 'end' }}>
          <Field label="من تاريخ"><input type="date" className="input" value={range[0]} onChange={(e) => { setTab('custom'); setFrom(e.target.value) }} /></Field>
          <Field label="إلى تاريخ"><input type="date" className="input" value={range[1]} onChange={(e) => { setTab('custom'); setTo(e.target.value) }} /></Field>
          <Field label="العرض بعملة">
            <select className="select" value={display} onChange={(e) => setDisplay(e.target.value)}>
              <option value="SYP">ليرة سورية</option>
              <option value="USD">دولار أمريكي</option>
              <option value="MIX">كل عملة على حدة</option>
            </select>
          </Field>
          <div className="row" style={{ gap: 6 }}>
            <button className="btn btn-gold btn-sm" onClick={exportCSV}>📊 Excel</button>
            <button className="btn btn-brown btn-sm" onClick={exportPDF}>🖨 PDF</button>
          </div>
        </div>
      </Card>

      <div className="grid grid-4" id="report-cards">
        <Stat icon="🧾" label={`المبيعات (${curLabel})`} value={display === 'MIX' ? money(r.byCur.SYP.sales, 'SYP', { bare: true }) : money(sum('sales'), display, { bare: true })} sub={display === 'MIX' ? `+ ${money(r.byCur.USD.sales, 'USD')}` : `${r.byCur.SYP.count + r.byCur.USD.count} فاتورة`} tone="gold" />
        <Stat icon="🛒" label="المشتريات" value={display === 'MIX' ? money(r.byCur.SYP.purchases, 'SYP', { bare: true }) : money(sum('purchases'), display, { bare: true })} />
        <Stat icon="💸" label="المصاريف" value={display === 'MIX' ? money(r.byCur.SYP.expenses, 'SYP', { bare: true }) : money(sum('expenses'), display, { bare: true })} />
        <Stat icon="🏦" label="المسحوبات" value={display === 'MIX' ? money(r.byCur.SYP.withdrawals, 'SYP', { bare: true }) : money(sum('withdrawals'), display, { bare: true })} />
      </div>

      {isManager && (
        <div className="grid grid-3">
          <Stat icon="📈" label="الربح الإجمالي" value={display === 'MIX' ? money(r.byCur.SYP.profit, 'SYP', { bare: true }) : money(sum('profit'), display, { bare: true })} sub="المبيعات − التكلفة" tone="gold" />
          <Stat icon="💵" label="صافي الربح" value={display === 'MIX' ? money(r.byCur.SYP.profit - r.byCur.SYP.expenses - r.byCur.SYP.withdrawals - r.byCur.SYP.workerTx, 'SYP', { bare: true }) : money(sum('profit') - sum('expenses') - sum('withdrawals') - sum('workerTx'), display, { bare: true })} sub="بعد المصاريف والمسحوبات" />
          <Stat icon="📥" label="ذمم غير محصلة" value={money(r.byCur.SYP.salesRemaining, 'SYP', { bare: true })} sub={`+ ${money(r.byCur.USD.salesRemaining, 'USD')}`} />
        </div>
      )}

      <div className="grid grid-2">
        <Card title="ملخص بالعملتين">
          <div className="table-wrap">
            <table>
              <thead><tr><th>البند</th><th>ليرة سورية</th><th>دولار أمريكي</th></tr></thead>
              <tbody>
                {[
                  ['المبيعات', 'sales'], ['المشتريات', 'purchases'], ['المصاريف', 'expenses'],
                  ['المسحوبات', 'withdrawals'], ['سحوبات العمال', 'workerTx'],
                  ...(isManager ? [['الأرباح', 'profit']] : []),
                ].map(([label, key]) => (
                  <tr key={key}>
                    <td>{label}</td>
                    <td style={{ direction: 'ltr' }}>{money(r.byCur.SYP[key], 'SYP', { bare: true })}</td>
                    <td style={{ direction: 'ltr' }}>{money(r.byCur.USD[key], 'USD', { bare: true })}</td>
                  </tr>
                ))}
                <tr style={{ background: 'var(--cream)' }}>
                  <td><b>صافي الربح التقديري</b></td>
                  <td style={{ direction: 'ltr', fontWeight: 800 }}>{money(r.byCur.SYP.profit - r.byCur.SYP.expenses - r.byCur.SYP.withdrawals - r.byCur.SYP.workerTx, 'SYP', { bare: true })}</td>
                  <td style={{ direction: 'ltr', fontWeight: 800 }}>{money(r.byCur.USD.profit - r.byCur.USD.expenses - r.byCur.USD.withdrawals - r.byCur.USD.workerTx, 'USD', { bare: true })}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p className="small muted" style={{ marginTop: 8 }}>
            المجموع المقدّر يستخدم سعر الصرف الحالي ({rateToNumber(rate).toLocaleString()} ل.س) وقد يختلف عن أسعار الفواتير المسجّلة.
          </p>
        </Card>

        <Card title="الأصناف الأكثر مبيعاً" badge={top.length}>
          {top.length === 0 ? <Empty icon="📊" /> : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>الصنف</th><th>الكمية</th><th>الإيراد</th>{isManager && <th>الربح</th>}</tr></thead>
                <tbody>
                  {top.map((t) => {
                    const item = s.items.find((i) => i.id === t.itemId)
                    return (
                      <tr key={t.itemId}>
                        <td style={{ fontWeight: 700 }}>{t.name}</td>
                        <td className="small">{item?.unit === 'kg' ? `${(t.qty / 1000).toFixed(2)} كغ` : `${t.qty} قطعة`}</td>
                        <td style={{ direction: 'ltr' }}>{money(t.revenue, t.currency, { bare: true })}</td>
                        {isManager && <td style={{ direction: 'ltr', color: 'var(--success)', fontWeight: 700 }}>{money(t.profit, t.currency, { bare: true })}</td>}
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </div>

      <Card title="تفصيل المبيعات حسب اليوم">
        <div className="table-wrap">
          <table>
            <thead><tr><th>اليوم</th><th>فواتير</th><th>مبيعات ل.س</th><th>مبيعات $</th><th>مصاريف ل.س</th><th>مسحوبات ل.س</th>{isManager && <th>ربح ل.س</th>}</tr></thead>
            <tbody>
              {(() => {
                const days = {}
                r.sales.forEach((i) => {
                  if (!days[i.date]) days[i.date] = { count: 0, syp: 0, usd: 0, exp: 0, wd: 0, profit: 0 }
                  days[i.date].count++
                  if (i.currency === 'SYP') { days[i.date].syp += i.total; days[i.date].profit += i.profit }
                  else days[i.date].usd += i.total
                })
                r.expenses.forEach((e) => { if (days[e.date] && e.currency === 'SYP') days[e.date].exp += e.amount })
                r.withdrawals.forEach((w) => { if (days[w.date] && w.currency === 'SYP') days[w.date].wd += w.amount })
                const rows = Object.entries(days).sort((a, b) => b[0].localeCompare(a[0]))
                if (rows.length === 0) return <tr><td colSpan={7}><Empty /></td></tr>
                return rows.map(([day, v]) => (
                  <tr key={day}>
                    <td>{fmtDate(day)}</td><td>{v.count}</td>
                    <td style={{ direction: 'ltr' }}>{money(v.syp, 'SYP', { bare: true })}</td>
                    <td style={{ direction: 'ltr' }}>{money(v.usd, 'USD', { bare: true })}</td>
                    <td style={{ direction: 'ltr' }}>{money(v.exp, 'SYP', { bare: true })}</td>
                    <td style={{ direction: 'ltr' }}>{money(v.wd, 'SYP', { bare: true })}</td>
                    {isManager && <td style={{ direction: 'ltr', color: 'var(--success)' }}>{money(v.profit, 'SYP', { bare: true })}</td>}
                  </tr>
                ))
              })()}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  )
}
