import { useMemo, useState } from 'react'
import { useStore, reportFor, topItems } from '../store'
import { money, todayISO, dayOffset, startOfMonth, fmtDate, convert, downloadCSV, CURRENCIES, openPrintWindow } from '../utils'
import { Card, Empty, PageHead, Seg } from '../components'
import { Icon } from '../icons'
import { Bars, Donut, AreaChart } from '../charts'

const COLORS = ['#C2983F', '#2C7A58', '#3B6485', '#B23B3B', '#6B5A8E', '#A9711D', '#8B7967']

export default function Reports({ onToast }) {
  const s = useStore()
  const [preset, setPreset] = useState('month')
  const [from, setFrom] = useState(startOfMonth())
  const [to, setTo] = useState(todayISO())

  const apply = (id) => {
    setPreset(id)
    if (id === 'today') { setFrom(todayISO()); setTo(todayISO()) }
    if (id === 'week') { setFrom(dayOffset(6)); setTo(todayISO()) }
    if (id === 'month') { setFrom(startOfMonth()); setTo(todayISO()) }
    if (id === 'prev') {
      const d = new Date(); d.setMonth(d.getMonth() - 1)
      const first = new Date(d.getFullYear(), d.getMonth(), 1)
      const last = new Date(d.getFullYear(), d.getMonth() + 1, 0)
      setFrom(`${first.getFullYear()}-${String(first.getMonth() + 1).padStart(2, '0')}-01`)
      setTo(`${last.getFullYear()}-${String(last.getMonth() + 1).padStart(2, '0')}-${String(last.getDate()).padStart(2, '0')}`)
    }
    if (id === 'year') { setFrom(`${todayISO().slice(0, 4)}-01-01`); setTo(todayISO()) }
  }

  const r = useMemo(() => reportFor(from, to), [s, from, to])
  const rate = s.settings.rate
  const eq = (sy, us) => sy + convert(us, 'USD', 'SYP', rate)
  const totals = {
    sales: eq(r.byCur.SYP.sales, r.byCur.USD.sales),
    profit: eq(r.byCur.SYP.profit, r.byCur.USD.profit),
    purchases: eq(r.byCur.SYP.purchases, r.byCur.USD.purchases),
    expenses: eq(r.byCur.SYP.expenses, r.byCur.USD.expenses),
    count: r.byCur.SYP.count + r.byCur.USD.count,
  }
  const avg = totals.count ? totals.sales / totals.count : 0
  const margin = totals.sales ? (totals.profit / totals.sales) * 100 : 0

  const daily = useMemo(() => {
    const days = []
    const start = new Date(from + 'T00:00:00')
    const end = new Date(to + 'T00:00:00')
    const n = Math.min(62, Math.round((end - start) / 86400000) + 1)
    for (let k = 0; k < n; k++) {
      const d = new Date(start); d.setDate(d.getDate() + k)
      const iso = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
      const inv = r.sales.filter((i) => i.date === iso)
      const v = inv.reduce((a, i) => a + i.total * (i.currency === 'USD' ? rate / 100 : 1), 0)
      days.push({ date: iso, label: d.toLocaleDateString('ar-SY', { day: 'numeric', month: 'numeric' }), value: v })
    }
    return days
  }, [r.sales, from, to, rate])

  const top = useMemo(() => topItems(from, to, 8), [s, from, to])
  const topCustomers = useMemo(() => {
    const map = {}
    r.sales.forEach((i) => {
      const key = i.partyName || 'زبون نقدي'
      if (!map[key]) map[key] = { name: key, total: 0, count: 0 }
      map[key].total += i.total * (i.currency === 'USD' ? rate / 100 : 1)
      map[key].count++
    })
    return Object.values(map).sort((a, b) => b.total - a.total).slice(0, 6)
  }, [r.sales, rate])
  const payments = { cash: 0, debt: 0, partial: 0 }
  r.sales.forEach((i) => {
    const v = i.total * (i.currency === 'USD' ? rate / 100 : 1)
    payments[i.paidMethod || 'cash'] += v
  })
  const expensesCats = {}
  r.expenses.forEach((e) => { expensesCats[e.category] = (expensesCats[e.category] || 0) + e.amount * (e.currency === 'USD' ? rate / 100 : 1) })
  const expList = Object.entries(expensesCats).sort((a, b) => b[1] - a[1])

  const summaryRows = [
    ['البند', 'ليرة سورية', 'دولار أمريكي', 'المكافئ بالليرة'],
    ['المبيعات', r.byCur.SYP.sales, r.byCur.USD.sales, totals.sales],
    ['المحصّل', r.byCur.SYP.salesPaid, r.byCur.USD.salesPaid, eq(r.byCur.SYP.salesPaid, r.byCur.USD.salesPaid)],
    ['الذمم', r.byCur.SYP.salesRemaining, r.byCur.USD.salesRemaining, eq(r.byCur.SYP.salesRemaining, r.byCur.USD.salesRemaining)],
    ['الأرباح', r.byCur.SYP.profit, r.byCur.USD.profit, totals.profit],
    ['المشتريات', r.byCur.SYP.purchases, r.byCur.USD.purchases, totals.purchases],
    ['المصاريف', r.byCur.SYP.expenses, r.byCur.USD.expenses, totals.expenses],
    ['مسحوبات المحل', r.byCur.SYP.withdrawals, r.byCur.USD.withdrawals, eq(r.byCur.SYP.withdrawals, r.byCur.USD.withdrawals)],
    ['سحوبات العمال', r.byCur.SYP.workerTx, r.byCur.USD.workerTx, eq(r.byCur.SYP.workerTx, r.byCur.USD.workerTx)],
  ]
  const net = totals.profit - totals.expenses - eq(r.byCur.SYP.withdrawals, r.byCur.USD.withdrawals) - eq(r.byCur.SYP.workerTx, r.byCur.USD.workerTx)

  const exportCSV = () => {
    downloadCSV(`تقرير-${from}-${to}.csv`, [
      ['تقرير محامص الحرمين', `${from} إلى ${to}`],
      [`سعر الصرف`, rate / 100],
      [],
      ...summaryRows,
      [],
      ['الأكثر مبيعاً', 'الكمية (غرام/قطعة)', 'الإيراد', 'الربح'],
      ...top.map((t) => [t.name, t.qty, t.revenue, t.profit]),
      [],
      ['أفضل الزبائن', 'عدد الفواتير', 'الإجمالي المكافئ'],
      ...topCustomers.map((c) => [c.name, c.count, Math.round(c.total)]),
    ])
    onToast?.('تم تصدير التقرير بصيغة CSV (تفتح في Excel)')
  }

  const printReport = () => {
    const rowHtml = summaryRows.slice(1).map((r2) => `<tr><td>${r2[0]}</td><td>${money(r2[1], 'SYP')}</td><td>${money(r2[2], 'USD')}</td><td>${money(r2[3], 'SYP')}</td></tr>`).join('')
    const topHtml = top.map((t, i) => `<tr><td>${i + 1}</td><td>${t.name}</td><td>${t.qty.toLocaleString('en-US')}</td><td>${money(t.revenue, t.currency || 'SYP')}</td></tr>`).join('')
    const html = `<!doctype html><html dir="rtl" lang="ar"><head><meta charset="utf-8"><title>تقرير ${from} — ${to}</title>
      <style>body{font-family:Tahoma,Arial,sans-serif;color:#2b1c12;padding:30px} h1{font-size:24px;margin:0;color:#241710} .muted{color:#8b7967;font-size:11px}
      .head{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:2px solid #c2983f;padding-bottom:12px;margin-bottom:16px}
      .kpis{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin:14px 0}.kpi{border:1px solid #e6dcc9;border-radius:8px;padding:10px}.kpi b{display:block;font-size:15px;margin-top:4px}
      table{width:100%;border-collapse:collapse;font-size:11px;margin:10px 0 20px}th{background:#f6f0e4;text-align:right;padding:7px;border-bottom:1px solid #d8cbb4}td{padding:6px 7px;border-bottom:1px solid #eee}
      h2{font-size:14px;margin:18px 0 6px}</style></head><body>
      <div class="head"><div><h1>${s.settings.shopName}</h1><div class="muted">${s.settings.address || ''} · ${s.settings.phone || ''}</div></div>
      <div style="text-align:left"><b>تقرير مالي</b><div class="muted">${fmtDate(from)} — ${fmtDate(to)}</div><div class="muted">سعر الصرف: ${(rate / 100).toLocaleString('en-US')} ل.س / $</div></div></div>
      <div class="kpis"><div class="kpi">إجمالي المبيعات<b>${money(totals.sales, 'SYP')}</b></div><div class="kpi">صافي الربح<b>${money(totals.profit, 'SYP')}</b></div><div class="kpi">عدد الفواتير<b>${totals.count}</b></div><div class="kpi">المصاريف<b>${money(totals.expenses, 'SYP')}</b></div></div>
      <h2>الملخص المالي بالعملتين</h2><table><thead><tr><th>البند</th><th>ليرة</th><th>دولار</th><th>المكافئ بالليرة</th></tr></thead><tbody>${rowHtml}</tbody></table>
      <div class="kpis"><div class="kpi">صافي بعد المصاريف والمسحوبات<b>${money(net, 'SYP')}</b></div><div class="kpi">متوسط الفاتورة<b>${money(avg, 'SYP')}</b></div><div class="kpi">هامش الربح<b>${margin.toFixed(1)}%</b></div><div class="kpi">أفضل زبون<b>${topCustomers[0]?.name || '—'}</b></div></div>
      <h2>الأكثر مبيعاً</h2><table><thead><tr><th>#</th><th>الصنف</th><th>الكمية</th><th>الإيراد</th></tr></thead><tbody>${topHtml || '<tr><td colspan="4">لا مبيعات</td></tr>'}</tbody></table>
      <p class="muted" style="text-align:center;margin-top:26px">${s.settings.invoiceFooter} — تم إنشاء التقرير بواسطة نظام ${s.settings.shopName}</p>
      <script>window.onload=()=>window.print()</script></body></html>`
    if (!openPrintWindow(html)) onToast?.('تعذر فتح نافذة الطباعة', 'error')
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      <PageHead title="التقارير والتحليلات" subtitle="أرقام دقيقة بالعملتين، أرباح، أفضل الأصناف والزبائن — جاهزة للطباعة والتصدير"
        actions={<>
          <button className="btn btn-ghost" onClick={exportCSV}><Icon name="download" />Excel / CSV</button>
          <button className="btn btn-primary" onClick={printReport}><Icon name="printer" />طباعة / PDF</button>
        </>} />

      <Card>
        <div className="row between" style={{ gap: 10 }}>
          <Seg value={preset} onChange={apply} options={[
            { id: 'today', label: 'اليوم' }, { id: 'week', label: '7 أيام' }, { id: 'month', label: 'هذا الشهر' },
            { id: 'prev', label: 'الشهر الماضي' }, { id: 'year', label: 'هذه السنة' },
          ]} size="sm" />
          <div className="row" style={{ gap: 5 }}>
            <input type="date" className="input" value={from} onChange={(e) => { setFrom(e.target.value); setPreset('') }} style={{ width: 140, height: 33, fontSize: 11 }} />
            <Icon name="arrowLeft" size={13} style={{ color: 'var(--muted)' }} />
            <input type="date" className="input" value={to} onChange={(e) => { setTo(e.target.value); setPreset('') }} style={{ width: 140, height: 33, fontSize: 11 }} />
          </div>
        </div>
      </Card>

      <div className="grid g4">
        <div className="kpi gold"><div className="top"><span className="lbl">إجمالي المبيعات</span><span className="ic"><Icon name="cart" /></span></div><div className="val">{money(totals.sales, 'SYP', { bare: true })}</div><div className="foot">ل.س · مكافئ للعملتين</div></div>
        <div className="kpi"><div className="top"><span className="lbl">صافي الربح</span><span className="ic"><Icon name="trendUp" /></span></div><div className="val">{money(totals.profit, 'SYP', { bare: true })}</div><div className="foot">هامش {margin.toFixed(1)}%</div></div>
        <div className="kpi"><div className="top"><span className="lbl">متوسط الفاتورة</span><span className="ic"><Icon name="receipt" /></span></div><div className="val">{money(avg, 'SYP', { bare: true })}</div><div className="foot">{totals.count} فاتورة</div></div>
        <div className="kpi"><div className="top"><span className="lbl">المصاريف</span><span className="ic"><Icon name="wallet" /></span></div><div className="val">{money(totals.expenses, 'SYP', { bare: true })}</div><div className="foot">{r.expenses.length} مصروف</div></div>
      </div>

      <div className="split-2 grid">
        <Card title="حركة المبيعات اليومية" subtitle="مكافئ بالليرة السورية" icon="line"
          action={<span className="chip"><Icon name="dollar" />1$ = {(rate / 100).toLocaleString('en-US')}</span>}>
          {daily.length === 0 ? <Empty icon="chart" title="لا بيانات" /> : <Bars data={daily} height={190} valueFmt={(v) => money(Math.round(v), 'SYP')} />}
          <div className="sep" />
          <div className="grid g3">
            <div><div className="tiny muted">أفضل يوم</div><b className="small">{money(Math.round(Math.max(...daily.map((d) => d.value), 0)), 'SYP')}</b></div>
            <div><div className="tiny muted">متوسط اليوم</div><b className="small">{money(Math.round(daily.reduce((a, b) => a + b.value, 0) / (daily.length || 1)), 'SYP')}</b></div>
            <div><div className="tiny muted">المحصّل نقداً</div><b className="small">{money(eq(r.byCur.SYP.salesPaid, r.byCur.USD.salesPaid), 'SYP')}</b></div>
          </div>
        </Card>

        <Card title="صافي بعد كل الالتزامات" icon="scale" subtitle="الربح ناقص المصاريف والمسحوبات وسحوبات العمال">
          <div className="center" style={{ padding: '10px 0' }}>
            <div className="tiny muted">صافي الفترة</div>
            <div style={{ fontSize: 30, fontWeight: 800, color: net >= 0 ? 'var(--success)' : 'var(--danger)', direction: 'ltr', marginTop: 4 }}>{money(net, 'SYP')}</div>
            <div className="tiny muted" style={{ marginTop: 4 }}>{fmtDate(from)} — {fmtDate(to)}</div>
          </div>
          <div className="sep" />
          {[
            ['الأرباح', totals.profit, 'var(--success)'],
            ['المصاريف', -totals.expenses, 'var(--danger)'],
            ['مسحوبات المحل', -eq(r.byCur.SYP.withdrawals, r.byCur.USD.withdrawals), 'var(--warn)'],
            ['سحوبات العمال', -eq(r.byCur.SYP.workerTx, r.byCur.USD.workerTx), 'var(--warn)'],
            ['المشتريات', -totals.purchases, 'var(--info)'],
          ].map(([l, v, c]) => (
            <div key={l} className="row between" style={{ padding: '6px 0', borderBottom: '1px dashed var(--line)' }}>
              <span className="small muted">{l}</span><b className="small" style={{ color: c, direction: 'ltr' }}>{money(v, 'SYP')}</b>
            </div>
          ))}
        </Card>
      </div>

      <div className="grid" style={{ gridTemplateColumns: 'minmax(0,1.45fr) minmax(0,1fr)', gap: 16, alignItems: 'start' }}>
        <Card title="الملخص المالي بالعملتين" subtitle="لا يتم خلط العملات — كل عمود مستقل" icon="scale" flush>
          <div className="table-wrap" style={{ border: 0 }}>
            <table>
              <thead><tr><th>البند</th><th className="num">ليرة سورية</th><th className="num">دولار أمريكي</th><th className="num">المكافئ (ل.س)</th></tr></thead>
              <tbody>
                {summaryRows.slice(1).map(([label, sy, us, eqv]) => (
                  <tr key={label}>
                    <td className="strong">{label}</td>
                    <td className="num">{money(sy, 'SYP', { bare: true })}</td>
                    <td className="num">{money(us, 'USD', { bare: true })}</td>
                    <td className="num strong" style={{ color: 'var(--gold-deep)' }}>{money(eqv, 'SYP', { bare: true })}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>

        <div className="grid" style={{ gap: 16 }}>
          <Card title="طرق الدفع" icon="card" subtitle="حسب قيمة الفواتير (مكافئ)">
            {r.sales.length === 0 ? <Empty icon="card" title="لا فواتير" /> : (
              <div className="donut-wrap">
                <Donut segments={[
                  { value: payments.cash, color: '#2C7A58' },
                  { value: payments.partial, color: '#C2983F' },
                  { value: payments.debt, color: '#B23B3B' },
                ]} size={120} thickness={14} center={`${r.sales.length}`} sub="فاتورة" />
                <div style={{ flex: 1, minWidth: 120 }}>
                  {[['نقدي', payments.cash, '#2C7A58'], ['جزئي', payments.partial, '#C2983F'], ['دين', payments.debt, '#B23B3B']].map(([l, v, c]) => (
                    <div key={l} className="row between" style={{ marginBottom: 5, fontSize: 10.5 }}>
                      <span className="row" style={{ gap: 5 }}><i style={{ width: 8, height: 8, borderRadius: 2, background: c }} />{l}</span>
                      <b style={{ direction: 'ltr' }}>{money(v, 'SYP', { bare: true })}</b>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </Card>

          <Card title="أفضل الزبائن" icon="users">
            {topCustomers.length === 0 ? <Empty icon="users" title="لا مبيعات" /> : topCustomers.map((c, i) => (
              <div key={c.name} className="row between" style={{ padding: '6px 0', borderBottom: i < topCustomers.length - 1 ? '1px dashed var(--line)' : 0 }}>
                <div className="row" style={{ gap: 6 }}><span className="tiny muted">{i + 1}</span><span className="small" style={{ fontWeight: 700 }}>{c.name}</span></div>
                <div style={{ textAlign: 'end' }}><b className="small" style={{ direction: 'ltr' }}>{money(c.total, 'SYP')}</b><div className="tiny muted">{c.count} فاتورة</div></div>
              </div>
            ))}
          </Card>
        </div>
      </div>

      <div className="split-2 grid">
        <Card title="الأكثر مبيعاً" subtitle="الكمية والإيراد والربح خلال الفترة" icon="star" flush>
          <div className="table-wrap" style={{ border: 0 }}>
            <table>
              <thead><tr><th>#</th><th>الصنف</th><th className="num">الكمية</th><th className="num">الإيراد</th><th className="num">الربح</th></tr></thead>
              <tbody>
                {top.length === 0 && <tr><td colSpan={5} className="table-empty">لا مبيعات في الفترة</td></tr>}
                {top.map((t, i) => (
                  <tr key={t.itemId || i}>
                    <td>{i + 1}</td>
                    <td><b>{t.name}</b></td>
                    <td className="num">{t.qty >= 1000 ? `${(t.qty / 1000).toLocaleString('en-US')} كغ` : `${t.qty} قطعة`}</td>
                    <td className="num">{money(t.revenue, t.currency || 'SYP', { bare: true })}</td>
                    <td className="num" style={{ color: 'var(--success)', fontWeight: 700 }}>{money(t.profit, t.currency || 'SYP', { bare: true })}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>

        <Card title="توزيع المصاريف" icon="pie" subtitle="خلال الفترة المختارة">
          {expList.length === 0 ? <Empty icon="pie" title="لا مصاريف" /> : (
            <div className="stack" style={{ gap: 10 }}>
              {expList.map(([cat, v], i) => (
                <div key={cat}>
                  <div className="row between" style={{ marginBottom: 4 }}>
                    <span className="small" style={{ fontWeight: 700 }}>{cat}</span>
                    <b className="small" style={{ direction: 'ltr' }}>{money(Math.round(v), 'SYP')}</b>
                  </div>
                  <div className="bar-track"><div className="bar-fill" style={{ width: `${(v / expList[0][1]) * 100}%`, background: COLORS[i % COLORS.length] }} /></div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      <div className="card tight" style={{ background: 'var(--surface-2)' }}>
        <div className="row" style={{ gap: 7 }}>
          <Icon name="info" size={16} style={{ color: 'var(--gold-deep)' }} />
          <span className="tiny muted">التقرير يعتمد سعر الصرف الحالي للتحويل بين العملات. أما الفواتير نفسها فتحتفظ بسعر الصرف المسجّل وقت إصدارها.</span>
        </div>
      </div>
    </div>
  )
}
