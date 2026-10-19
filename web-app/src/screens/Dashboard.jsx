import { useMemo, useState } from 'react'
import { useStore, lowStockItems, expiringItems, cashFlow, partyStats, setRate, rateNow, topItems } from '../store'
import { money, todayISO, convert, fmtDate, dayOffset } from '../utils'
import { Card, Kpi, Empty, StatusBadge, Seg, Money, Avatar, Field } from '../components'
import { AreaChart, Donut, Bars } from '../charts'
import { Icon, categoryIcon } from '../icons'

export default function Dashboard({ go, onToast }) {
  const s = useStore()
  const isManager = s.session?.role === 'manager'
  const today = todayISO()
  const [rateInput, setRateInput] = useState(String(rateNow()))
  const [chartMode, setChartMode] = useState('sales')
  const [range, setRange] = useState(14)

  const d = useMemo(() => {
    const sales = s.invoices.filter((i) => i.type === 'sale' && i.status !== 'cancelled')
    const todayInv = sales.filter((i) => i.date === today)
    const yInv = sales.filter((i) => i.date === dayOffset(1))
    const sum = (list, c) => list.filter((i) => i.currency === c).reduce((a, b) => a + b.total, 0)
    const byCur = { SYP: sum(todayInv, 'SYP'), USD: sum(todayInv, 'USD') }
    const yByCur = { SYP: sum(yInv, 'SYP'), USD: sum(yInv, 'USD') }
    const profit = { SYP: 0, USD: 0 }
    todayInv.forEach((i) => { profit[i.currency] += i.profit || 0 })
    const totalTodayEq = byCur.SYP + convert(byCur.USD, 'USD', 'SYP', s.settings.rate)
    const totalYEq = yByCur.SYP + convert(yByCur.USD, 'USD', 'SYP', s.settings.rate)
    const delta = totalYEq ? ((totalTodayEq - totalYEq) / totalYEq) * 100 : null

    const recv = { SYP: 0, USD: 0 }
    sales.forEach((i) => { recv[i.currency] += i.remaining || 0 })
    s.payments.filter((p) => p.partyType === 'customer').forEach((p) => { recv[p.currency] -= p.amount })
    const payables = { SYP: 0, USD: 0 }
    s.invoices.filter((i) => i.type === 'purchase' && i.status !== 'cancelled').forEach((i) => { payables[i.currency] += i.remaining || 0 })
    s.payments.filter((p) => p.partyType === 'supplier').forEach((p) => { payables[p.currency] -= p.amount })

    const series = []
    for (let k = range - 1; k >= 0; k--) {
      const date = dayOffset(k)
      const day = sales.filter((i) => i.date === date)
      const v = day.reduce((a, i) => a + i.total * (i.currency === 'USD' ? rateNow() : 1), 0)
      const p = day.reduce((a, i) => a + (i.profit || 0) * (i.currency === 'USD' ? rateNow() : 1), 0)
      series.push({ date, sales: v, profit: p, count: day.length })
    }
    const cashSYP = cashFlow('SYP', today, today)
    const cashUSD = cashFlow('USD', today, today)
    const top = topItems(dayOffset(30), today, 6)
    const avg = todayInv.length ? totalTodayEq / todayInv.length : 0
    return { sales, todayInv, byCur, profit, totalTodayEq, delta, recv, payables, series, cashSYP, cashUSD, top, avg }
  }, [s, today, range])

  const low = lowStockItems()
  const exp = expiringItems(30)
  const recent = useMemo(() => s.invoices.filter((i) => i.type === 'sale').sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || '')).slice(0, 5), [s.invoices])
  const overdue = useMemo(() => {
    const t = new Date(today + 'T00:00:00').getTime()
    return s.customers.filter((c) => c.id !== 'cash').map((c) => {
      const st = partyStats(c.id, 'customer')
      const days = st.lastInvoice ? Math.round((t - new Date(st.lastInvoice.date + 'T00:00:00').getTime()) / 86400000) : 999
      return { c, st, days }
    }).filter((x) => (x.st.balances.SYP > 0 || x.st.balances.USD > 0) && x.days >= 30).slice(0, 4)
  }, [s.customers, s.invoices, s.payments, today])

  const chartData = d.series.map((x) => chartMode === 'sales' ? x.sales : x.profit)
  const chartLabels = range === 14
    ? d.series.filter((_, i) => i % 2 === 1).map((x) => new Date(x.date + 'T00:00:00').toLocaleDateString('ar-SY', { day: 'numeric', month: 'numeric' }))
    : [d.series[0], d.series[Math.floor(range / 2)], d.series[range - 1]].filter(Boolean).map((x) => new Date(x.date + 'T00:00:00').toLocaleDateString('ar-SY', { day: 'numeric', month: 'numeric' }))

  const bestDay = d.series.reduce((a, b) => (b.sales > a.sales ? b : a), d.series[0] || { sales: 0 })
  const expensesToday = s.expenses.filter((e) => e.date === today)

  const heroStat = (label, v, c, sub) => (
    <div>
      <div className="tiny" style={{ color: 'rgba(249,240,224,.45)', fontWeight: 700 }}>{label}</div>
      <div style={{ fontSize: 19, fontWeight: 800, color: '#FFEFCB', direction: 'ltr', marginTop: 2 }}>{money(v, c, { bare: true })}</div>
      {sub && <div className="tiny" style={{ color: 'rgba(249,240,224,.4)', marginTop: 1 }}>{sub}</div>}
    </div>
  )

  return (
    <div className="grid" style={{ gap: 16 }}>
      {/* الترويسة الترحيبية */}
      <section className="hero">
        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0,1.25fr) minmax(0,1fr)', gap: 22, alignItems: 'center' }} className="hero-grid">
          <div>
            <div className="eyebrow">لوحة تحكم المحل</div>
            <div className="greet">أهلاً، {s.session?.name}</div>
            <div className="date">{new Date().toLocaleDateString('ar-SY', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })} · {s.settings.branch}</div>

            <div style={{ marginTop: 18, display: 'flex', alignItems: 'flex-end', gap: 12, flexWrap: 'wrap' }}>
              <div>
                <div className="tiny" style={{ color: 'rgba(249,240,224,.5)', fontWeight: 700, marginBottom: 2 }}>مبيعات اليوم — مكافئ بالليرة</div>
                <div className="amount">{Math.round(d.totalTodayEq).toLocaleString('en-US')} <small>ل.س</small></div>
              </div>
              {d.delta !== null && (
                <span className={`badge ${d.delta >= 0 ? 'badge-green' : 'badge-red'}`} style={{ marginBottom: 6, background: d.delta >= 0 ? 'rgba(111,191,149,.16)' : 'rgba(224,140,135,.16)', color: d.delta >= 0 ? '#9AD9B6' : '#F0B3AE', borderColor: 'transparent' }}>
                  <Icon name={d.delta >= 0 ? 'trendUp' : 'trendDown'} size={11} />{Math.abs(d.delta).toFixed(0)}% مقارنة بالأمس
                </span>
              )}
            </div>
            <div className="row" style={{ gap: 18, marginTop: 14, flexWrap: 'wrap' }}>
              {heroStat('بالليرة', d.byCur.SYP, 'SYP', `${d.todayInv.filter((i) => i.currency === 'SYP').length} فاتورة`)}
              {heroStat('بالدولار', d.byCur.USD, 'USD', `${d.todayInv.filter((i) => i.currency === 'USD').length} فاتورة`)}
              <div style={{ width: 1, alignSelf: 'stretch', background: 'rgba(255,255,255,.12)' }} />
              <div>
                <div className="tiny" style={{ color: 'rgba(249,240,224,.45)', fontWeight: 700 }}>عدد الفواتير</div>
                <div style={{ fontSize: 19, fontWeight: 800, color: '#FFEFCB', marginTop: 2 }}>{d.todayInv.length}</div>
                <div className="tiny" style={{ color: 'rgba(249,240,224,.4)', marginTop: 1 }}>فاتورة اليوم</div>
              </div>
            </div>
            <div className="row" style={{ gap: 8, marginTop: 20 }}>
              <button className="btn btn-gold" onClick={() => go('sale-new')}><Icon name="plus" />فاتورة جديدة</button>
              <button className="btn" style={{ background: 'rgba(255,255,255,.08)', border: '1px solid rgba(255,255,255,.14)', color: '#F9F0E0' }} onClick={() => go('reports')}>
                <Icon name="chart" />عرض التقارير
              </button>
            </div>
          </div>
          <div className="glass" style={{ padding: 14 }}>
            <div className="row between" style={{ marginBottom: 6 }}>
              <span className="tiny" style={{ color: 'rgba(249,240,224,.55)', fontWeight: 700 }}>حركة المبيعات — {range} يوم</span>
              <span className="tiny" style={{ color: '#E9CC8C', fontWeight: 800 }}>مكافئ ل.س</span>
            </div>
            <AreaChart data={chartData} height={112} color="#E3C47C" labels={[]} compact />
            <div className="row between" style={{ marginTop: 8 }}>
              <span className="tiny" style={{ color: 'rgba(249,240,224,.45)' }}>أفضل يوم: {bestDay?.date ? fmtDate(bestDay.date) : '—'}</span>
              <span className="tiny" style={{ color: 'rgba(249,240,224,.45)' }}>متوسط اليوم: {Math.round(d.series.reduce((a, b) => a + b.sales, 0) / (range || 1)).toLocaleString('en-US')} ل.س</span>
            </div>
          </div>
        </div>
        <style>{`@media(max-width:900px){.hero-grid{grid-template-columns:1fr!important}}`}</style>
      </section>

      {/* مؤشرات */}
      <div className="grid g4">
        <Kpi label="مبيعات اليوم" value={<>{money(d.byCur.SYP, 'SYP', { bare: true })} <small>ل.س</small></>} icon="cart" tone="gold"
          sub={<>+ <Money value={d.byCur.USD} currency="USD" /> دولار</>} />
        <Kpi label={isManager ? 'ربح اليوم' : 'فواتير اليوم'} value={isManager ? <>{money(d.profit.SYP, 'SYP', { bare: true })} <small>ل.س</small></> : String(d.todayInv.length)}
          icon={isManager ? 'trendUp' : 'receipt'} sub={isManager ? <>+ <Money value={d.profit.USD} currency="USD" /></> : `متوسط الفاتورة ${money(Math.round(d.avg), 'SYP', { bare: true })} ل.س`} />
        <Kpi label="ذمم الزبائن" value={<>{money(d.recv.SYP, 'SYP', { bare: true })} <small>ل.س</small></>} icon="users"
          sub={<>+ <Money value={d.recv.USD} currency="USD" /> · {overdue.length ? `${overdue.length} متأخر` : 'لا تأخير'}</>} onClick={() => go('customers')} />
        <Kpi label="صافي صندوق اليوم" value={<>{money(d.cashSYP.net, 'SYP', { bare: true })} <small>ل.س</small></>} icon="bank"
          sub={<>+ <Money value={d.cashUSD.net} currency="USD" /> دولار</>} onClick={isManager ? () => go('cash') : undefined} />
      </div>

      {/* تنبيهات */}
      <div className="grid g4">
        <button className={`insight ${low.length ? 'red' : 'green'}`} onClick={() => go('inventory')} style={{ textAlign: 'start' }}>
          <span className="ico"><Icon name="package" /></span>
          <div style={{ flex: 1, minWidth: 0 }}><div className="t">تنبيه المخزون</div><div className="s">{low.length ? `${low.length} صنف تحت الحد الأدنى` : 'المخزون بمستوى آمن'}</div></div>
          <Icon name="chevronLeft" size={15} style={{ color: 'var(--muted)' }} />
        </button>
        <button className={`insight ${exp.length ? 'gold' : 'green'}`} onClick={() => go('inventory')} style={{ textAlign: 'start' }}>
          <span className="ico"><Icon name="clock" /></span>
          <div style={{ flex: 1, minWidth: 0 }}><div className="t">الصلاحية</div><div className="s">{exp.length ? `${exp.length} صنف ينتهي خلال 30 يوماً` : 'لا أصناف قاربت الانتهاء'}</div></div>
          <Icon name="chevronLeft" size={15} style={{ color: 'var(--muted)' }} />
        </button>
        <button className={`insight ${overdue.length ? 'red' : 'green'}`} onClick={() => go('customers')} style={{ textAlign: 'start' }}>
          <span className="ico"><Icon name="wallet" /></span>
          <div style={{ flex: 1, minWidth: 0 }}><div className="t">ديون متأخرة</div><div className="s">{overdue.length ? `${overdue.length} زبون لم يسدّد منذ 30 يوماً` : 'لا ديون متأخرة'}</div></div>
          <Icon name="chevronLeft" size={15} style={{ color: 'var(--muted)' }} />
        </button>
        <button className="insight blue" onClick={() => go('preorders')} style={{ textAlign: 'start' }}>
          <span className="ico"><Icon name="gift" /></span>
          <div style={{ flex: 1, minWidth: 0 }}><div className="t">طلبات مسبقة</div><div className="s">{s.preorders.filter((p) => p.status !== 'delivered').length} طلب قيد التنفيذ</div></div>
          <Icon name="chevronLeft" size={15} style={{ color: 'var(--muted)' }} />
        </button>
      </div>

      {/* الرسم + الأكثر مبيعاً */}
      <div className="split-2 grid">
        <Card title="أداء المبيعات" subtitle="حركة يومية بالعملتين محوّلة إلى الليرة" icon="line"
          action={<Seg value={chartMode} onChange={setChartMode} options={[{ id: 'sales', label: 'المبيعات' }, { id: 'profit', label: 'الأرباح' }]} />}>
          <div className="row between" style={{ marginBottom: 8 }}>
            <div>
              <div className="tiny muted">إجمالي الفترة المختارة</div>
              <div style={{ fontSize: 24, fontWeight: 800, letterSpacing: '-.5px', direction: 'ltr', textAlign: 'right' }}>
                {Math.round(d.series.reduce((a, b) => a + (chartMode === 'sales' ? b.sales : b.profit), 0)).toLocaleString('en-US')}
                <small style={{ fontSize: 11, color: 'var(--muted)', marginInlineStart: 5 }}>ل.س</small>
              </div>
            </div>
            <Seg value={range} onChange={setRange} options={[{ id: 7, label: '7 أيام' }, { id: 14, label: '14 يوم' }, { id: 30, label: '30 يوم' }]} size="sm" />
          </div>
          <AreaChart data={chartData} height={168} color={chartMode === 'sales' ? '#C2983F' : '#2C7A58'} labels={chartLabels} />
          <div className="sep" />
          <div className="grid g3">
            <div><div className="tiny muted">أعلى يوم</div><b style={{ fontSize: 13 }}>{Math.round(bestDay?.sales || 0).toLocaleString('en-US')} <span className="tiny muted">ل.س</span></b></div>
            <div><div className="tiny muted">إجمالي الفواتير</div><b style={{ fontSize: 13 }}>{d.series.reduce((a, b) => a + b.count, 0)}</b></div>
            <div><div className="tiny muted">متوسط الفاتورة</div><b style={{ fontSize: 13 }}>{Math.round(d.avg).toLocaleString('en-US')} <span className="tiny muted">ل.س</span></b></div>
          </div>
        </Card>

        <Card title="الأكثر مبيعاً" subtitle="آخر 30 يوماً" icon="star" action={<button className="btn btn-ghost btn-sm" onClick={() => go('reports')}>التفاصيل</button>}>
          {d.top.length === 0 ? <Empty icon="chart" title="لا مبيعات بعد" /> : (
            <div className="stack" style={{ gap: 12 }}>
              {d.top.map((it, idx) => (
                <div key={it.itemId || idx}>
                  <div className="row between" style={{ marginBottom: 5, flexWrap: 'nowrap' }}>
                    <div className="row" style={{ gap: 8, minWidth: 0 }}>
                      <span style={{ width: 22, height: 22, borderRadius: 7, background: idx === 0 ? 'var(--gold-3)' : 'var(--surface-2)', color: idx === 0 ? 'var(--gold-deep)' : 'var(--muted)', display: 'grid', placeItems: 'center', fontSize: 10, fontWeight: 800, flex: '0 0 auto' }}>{idx + 1}</span>
                      <span style={{ fontSize: 11.5, fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{it.name}</span>
                    </div>
                    <Money value={it.revenue} currency={it.currency || 'SYP'} className="small" bold />
                  </div>
                  <div className="bar-track"><div className="bar-fill" style={{ width: `${(it.revenue / (d.top[0]?.revenue || 1)) * 100}%` }} /></div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      {/* آخر الفواتير + سعر الصرف والديون */}
      <div className="split-2 grid">
        <Card title="آخر الفواتير" subtitle="أحدث 5 فواتير مبيعات" icon="receipt" action={<button className="btn btn-ghost btn-sm" onClick={() => go('sales')}>عرض الكل<Icon name="chevronLeft" size={14} /></button>}>
          {recent.length === 0 ? <Empty icon="receipt" title="لا فواتير بعد" text="ابدأ بإنشاء أول فاتورة بيع." /> : (
            <div>
              {recent.map((i) => (
                <button key={i.id} className="list-item" style={{ width: '100%', background: 'transparent', border: 0, textAlign: 'start', cursor: 'pointer' }} onClick={() => go('invoice-view', { id: i.id })}>
                  <span className="ic-tile"><Icon name="receipt" /></span>
                  <div className="txt">
                    <div className="t1">{i.no} — {i.partyName}</div>
                    <div className="t2">{fmtDate(i.date)} · {i.lines?.length || 0} بند · بواسطة {i.createdBy}</div>
                  </div>
                  <div className="end">
                    <b style={{ display: 'block' }}>{money(i.total, i.currency)}</b>
                    <span style={{ marginTop: 3, display: 'inline-block' }}><StatusBadge status={i.status} /></span>
                  </div>
                </button>
              ))}
            </div>
          )}
        </Card>

        <div className="grid" style={{ gap: 16 }}>
          <Card title="سعر الصرف" icon="dollar" subtitle="سجّل السعر قبل إصدار الفواتير">
            <div className="row nowrap" style={{ alignItems: 'flex-end', gap: 8 }}>
              <Field label="ليرة لكل دولار" style={{ flex: 1 }}>
                <input className="input" inputMode="decimal" value={rateInput} onChange={(e) => setRateInput(e.target.value)} style={{ direction: 'ltr', fontWeight: 800 }} />
              </Field>
              <button className="btn btn-gold" onClick={() => { const r = Number(rateInput); if (r > 0) { setRate(r); onToast?.('تم تحديث سعر الصرف') } else onToast?.('أدخل سعراً صحيحاً', 'error') }}>تحديث</button>
            </div>
            <div className="row between" style={{ marginTop: 10 }}>
              <span className="tiny muted">آخر تحديث: الآن</span>
              <span className="tiny" style={{ color: 'var(--success)', fontWeight: 700 }}><Icon name="checkCircle" size={12} style={{ verticalAlign: -2 }} /> مسجّل مع كل فاتورة</span>
            </div>
          </Card>

          <Card title="ديون تحتاج متابعة" icon="alert" action={overdue.length > 0 && <span className="badge badge-red">{overdue.length}</span>}>
            {overdue.length === 0 ? <div className="row" style={{ gap: 8 }}><span className="badge badge-green"><Icon name="check" size={11} />لا ديون متأخرة</span></div> : (
              <div className="stack" style={{ gap: 8 }}>
                {overdue.map(({ c, st, days }) => (
                  <button key={c.id} className="row between" style={{ width: '100%', background: 'transparent', border: 0, textAlign: 'start', gap: 8 }} onClick={() => go('customers')}>
                    <div className="row" style={{ gap: 8, minWidth: 0 }}>
                      <Avatar name={c.name} size="sm" />
                      <div style={{ minWidth: 0 }}>
                        <div style={{ fontSize: 11.5, fontWeight: 700 }}>{c.name}</div>
                        <div className="tiny muted">آخر فاتورة قبل {days} يوم</div>
                      </div>
                    </div>
                    <div style={{ textAlign: 'end' }}>
                      {st.balances.SYP > 0 && <div className="tiny" style={{ fontWeight: 800, direction: 'ltr' }}>{money(st.balances.SYP, 'SYP')}</div>}
                      {st.balances.USD > 0 && <div className="tiny" style={{ fontWeight: 800, direction: 'ltr' }}>{money(st.balances.USD, 'USD')}</div>}
                    </div>
                  </button>
                ))}
              </div>
            )}
          </Card>
        </div>
      </div>

      {/* الملخص المالي */}
      <Card title="نظرة عامة على الأرصدة" subtitle="كل عملة على حدة — مع القيمة المقدّرة بسعر الصرف الحالي" icon="scale"
        action={<span className="badge badge-gold"><Icon name="dollar" size={11} />1$ = {rateNow().toLocaleString('en-US')} ل.س</span>}>
        <div className="desktop-table">
          <div className="table-wrap">
            <table>
              <thead><tr><th>البند</th><th>بالليرة السورية</th><th>بالدولار</th><th>الإجمالي المقدّر (ل.س)</th></tr></thead>
              <tbody>
                <tr><td className="strong">ذمم الزبائن</td><td className="num">{money(d.recv.SYP, 'SYP')}</td><td className="num">{money(d.recv.USD, 'USD')}</td><td className="num strong" style={{ color: 'var(--gold-deep)' }}>{money(d.recv.SYP + convert(d.recv.USD, 'USD', 'SYP', s.settings.rate), 'SYP')}</td></tr>
                <tr><td className="strong">مستحقات الموردين</td><td className="num">{money(d.payables.SYP, 'SYP')}</td><td className="num">{money(d.payables.USD, 'USD')}</td><td className="num strong">{money(d.payables.SYP + convert(d.payables.USD, 'USD', 'SYP', s.settings.rate), 'SYP')}</td></tr>
                <tr><td className="strong">صافي حركة الصندوق اليوم</td><td className="num">{money(d.cashSYP.net, 'SYP')}</td><td className="num">{money(d.cashUSD.net, 'USD')}</td><td className="num strong">{money(d.cashSYP.net + convert(d.cashUSD.net, 'USD', 'SYP', s.settings.rate), 'SYP')}</td></tr>
                <tr><td className="strong">مصاريف اليوم</td><td className="num">{money(expensesToday.filter((e) => e.currency === 'SYP').reduce((a, b) => a + b.amount, 0), 'SYP')}</td><td className="num">{money(expensesToday.filter((e) => e.currency === 'USD').reduce((a, b) => a + b.amount, 0), 'USD')}</td><td className="num">—</td></tr>
              </tbody>
            </table>
          </div>
        </div>
        <div className="mobile-cards" style={{ gridTemplateColumns: '1fr 1fr' }}>
          {[
            ['ذمم الزبائن', d.recv.SYP, d.recv.USD],
            ['مستحقات الموردين', d.payables.SYP, d.payables.USD],
            ['صندوق اليوم', d.cashSYP.net, d.cashUSD.net],
          ].map(([label, sy, us]) => (
            <div className="m-card" key={label}>
              <div className="sub" style={{ marginTop: 0 }}>{label}</div>
              <div style={{ fontSize: 13, fontWeight: 800, marginTop: 4 }}>{money(sy, 'SYP')}</div>
              <div className="tiny muted" style={{ direction: 'ltr' }}>{money(us, 'USD')}</div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  )
}
