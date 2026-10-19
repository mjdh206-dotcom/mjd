import { useMemo, useState } from 'react'
import { useStore, addExpense, cashFlow, saveClosing } from '../store'
import { money, parseAmount, fmtDate, todayISO, CURRENCIES, startOfMonth, dayOffset, downloadCSV } from '../utils'
import { Card, Empty, Field, PageHead, Seg, Money } from '../components'
import { Icon } from '../icons'
import { Donut } from '../charts'

const EXPENSE_CATS = [
  { id: 'إيجار', icon: 'bank' }, { id: 'كهرباء', icon: 'flame' }, { id: 'ماء', icon: 'info' },
  { id: 'أجور', icon: 'employee' }, { id: 'مواصلات', icon: 'truck' }, { id: 'أكياس وتغليف', icon: 'package' },
  { id: 'صيانة', icon: 'settings' }, { id: 'ضرائب', icon: 'receipt' }, { id: 'أخرى', icon: 'more' },
]

export default function Expenses({ onToast, cashOnly }) {
  return cashOnly ? <CashScreen onToast={onToast} /> : <ExpensesScreen onToast={onToast} />
}

function ExpensesScreen({ onToast }) {
  const s = useStore()
  const [range, setRange] = useState('month')
  const [from, setFrom] = useState(startOfMonth())
  const [to, setTo] = useState(todayISO())
  const [form, setForm] = useState({ date: todayISO(), category: 'كهرباء', amount: '', currency: 'SYP', note: '' })

  const preset = (id) => {
    setRange(id)
    if (id === 'today') { setFrom(todayISO()); setTo(todayISO()) }
    if (id === 'week') { setFrom(dayOffset(7)); setTo(todayISO()) }
    if (id === 'month') { setFrom(startOfMonth()); setTo(todayISO()) }
  }

  const list = useMemo(() => s.expenses.filter((e) => e.date >= from && e.date <= to).sort((a, b) => b.date.localeCompare(a.date)), [s.expenses, from, to])
  const tSYP = list.filter((e) => e.currency === 'SYP').reduce((a, b) => a + b.amount, 0)
  const tUSD = list.filter((e) => e.currency === 'USD').reduce((a, b) => a + b.amount, 0)
  const byCat = useMemo(() => {
    const m = {}
    list.forEach((e) => {
      const key = e.category
      m[key] = (m[key] || 0) + (e.currency === 'SYP' ? e.amount : e.amount * 100 * (s.settings.rate / 100))
    })
    return Object.entries(m).map(([k, v]) => ({ label: k, value: v })).sort((a, b) => b.value - a.value)
  }, [list, s.settings.rate])

  const colors = ['#C2983F', '#8C6724', '#E3C47C', '#2C7A58', '#3B6485', '#B23B3B', '#6B5A8E', '#A9711D', '#8B7967']
  const add = () => {
    const amount = Math.round(parseAmount(form.amount) * Math.pow(10, CURRENCIES[form.currency].decimals))
    if (amount <= 0) return onToast?.('أدخل مبلغاً صحيحاً', 'error')
    addExpense({ ...form, amount })
    setForm({ ...form, amount: '', note: '' })
    onToast?.('تم تسجيل المصروف')
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      <PageHead title="المصاريف" subtitle="سجّل كل مصروف بعملته، وتابع توزيع التكاليف على بنود المحل"
        actions={<button className="btn btn-ghost" onClick={() => { downloadCSV('المصاريف.csv', [['التاريخ', 'البند', 'المبلغ', 'العملة', 'ملاحظة'], ...list.map((e) => [e.date, e.category, e.amount, e.currency, e.note])]); onToast?.('تم التصدير') }}><Icon name="download" />تصدير</button>} />

      <div className="grid g4">
        <div className="kpi gold"><div className="top"><span className="lbl">مصاريف الفترة — ليرة</span><span className="ic"><Icon name="wallet" /></span></div><div className="val">{money(tSYP, 'SYP', { bare: true })}</div><div className="foot">{list.filter((e) => e.currency === 'SYP').length} مصروف</div></div>
        <div className="kpi"><div className="top"><span className="lbl">مصاريف الفترة — دولار</span><span className="ic"><Icon name="dollar" /></span></div><div className="val">{money(tUSD, 'USD', { bare: true })}</div><div className="foot">{list.filter((e) => e.currency === 'USD').length} مصروف</div></div>
        <div className="kpi"><div className="top"><span className="lbl">أكبر بند</span><span className="ic"><Icon name="pie" /></span></div><div className="val" style={{ fontSize: 17 }}>{byCat[0]?.label || '—'}</div><div className="foot">{byCat[0] ? money(Math.round(byCat[0].value), 'SYP') : ''}</div></div>
        <div className="kpi"><div className="top"><span className="lbl">عدد المصاريف</span><span className="ic"><Icon name="receipt" /></span></div><div className="val">{list.length}</div><div className="foot">{fmtDate(from)} — {fmtDate(to)}</div></div>
      </div>

      <div className="grid" style={{ gridTemplateColumns: 'minmax(0,1.6fr) minmax(0,1fr)', gap: 16, alignItems: 'start' }}>
        <Card title="تسجيل مصروف جديد" icon="plus" subtitle="يُخصم مباشرة من صندوق العملة المختارة">
          <div className="grid g2" style={{ gap: 10 }}>
            <Field label="التاريخ"><input type="date" className="input" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} /></Field>
            <Field label="البند">
              <select className="select" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
                {EXPENSE_CATS.map((c) => <option key={c.id} value={c.id}>{c.id}</option>)}
              </select>
            </Field>
          </div>
          <div className="row" style={{ gap: 5, marginTop: 8 }}>
            {EXPENSE_CATS.map((c) => (
              <button key={c.id} className={`chip click ${form.category === c.id ? 'active' : ''}`} onClick={() => setForm({ ...form, category: c.id })}>
                <Icon name={c.icon} />{c.id}
              </button>
            ))}
          </div>
          <div className="grid" style={{ gridTemplateColumns: '1.4fr 1fr', gap: 10, marginTop: 10 }}>
            <Field label={`المبلغ (${CURRENCIES[form.currency].name})`}>
              <div style={{ position: 'relative' }}>
                <input className="input" inputMode="decimal" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} placeholder="0" style={{ fontSize: 17, fontWeight: 800, height: 46, paddingInlineEnd: 46 }} />
                <span className="money-suffix">{CURRENCIES[form.currency].short}</span>
              </div>
            </Field>
            <Field label="العملة">
              <Seg block value={form.currency} onChange={(v) => setForm({ ...form, currency: v })} options={[{ id: 'SYP', label: 'ليرة' }, { id: 'USD', label: 'دولار' }]} />
            </Field>
          </div>
          <Field label="ملاحظة" style={{ marginTop: 10 }}><input className="input" value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} placeholder="تفاصيل المصروف (اختياري)" /></Field>
          <button className="btn btn-gold btn-block" style={{ marginTop: 12 }} onClick={add}><Icon name="plus" />تسجيل المصروف</button>
        </Card>

        <Card title="توزيع المصاريف" icon="pie" subtitle="محوّلة إلى الليرة">
          {byCat.length === 0 ? <Empty icon="pie" title="لا مصاريف في الفترة" /> : (
            <>
              <div className="donut-wrap" style={{ justifyContent: 'center' }}>
                <Donut segments={byCat.slice(0, 6).map((c, i) => ({ value: c.value, color: colors[i] }))} size={140} thickness={15}
                  center={money(tSYP + Math.round(tUSD * s.settings.rate / 100), 'SYP', { bare: true })} sub="إجمالي مقدّر" />
                <div style={{ flex: 1, minWidth: 130 }}>
                  {byCat.slice(0, 6).map((c, i) => (
                    <div key={c.label} className="row between" style={{ fontSize: 10.5, marginBottom: 5 }}>
                      <span className="row" style={{ gap: 5 }}><i style={{ width: 8, height: 8, borderRadius: 2, background: colors[i], display: 'inline-block' }} />{c.label}</span>
                      <b style={{ direction: 'ltr' }}>{money(Math.round(c.value), 'SYP', { bare: true })}</b>
                    </div>
                  ))}
                </div>
              </div>
              <div className="sep" />
              <div className="row between"><span className="small muted">متوسط المصروف</span><b>{money(list.length ? Math.round((tSYP + tUSD * s.settings.rate / 100) / list.length) : 0, 'SYP')}</b></div>
            </>
          )}
        </Card>
      </div>

      <Card>
        <div className="row between" style={{ marginBottom: 12 }}>
          <div className="row" style={{ gap: 5 }}>
            <Seg value={range} onChange={preset} options={[{ id: 'today', label: 'اليوم' }, { id: 'week', label: '7 أيام' }, { id: 'month', label: 'هذا الشهر' }]} size="sm" />
            <input type="date" className="input" value={from} onChange={(e) => { setFrom(e.target.value); setRange('') }} style={{ width: 138, height: 32, fontSize: 11 }} />
            <input type="date" className="input" value={to} onChange={(e) => { setTo(e.target.value); setRange('') }} style={{ width: 138, height: 32, fontSize: 11 }} />
          </div>
          <span className="badge badge-neutral">{list.length} مصروف</span>
        </div>
        {list.length === 0 ? <Empty icon="wallet" title="لا مصاريف" /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>التاريخ</th><th>البند</th><th>ملاحظة</th><th className="num">المبلغ</th><th>سجّلها</th></tr></thead>
              <tbody>
                {list.map((e) => (
                  <tr key={e.id}>
                    <td>{fmtDate(e.date)}</td>
                    <td><span className="badge badge-neutral">{e.category}</span></td>
                    <td className="small muted">{e.note || '—'}</td>
                    <td className="num strong" style={{ color: 'var(--danger)' }}>{money(e.amount, e.currency)}</td>
                    <td className="small">{e.createdBy || 'المدير'}</td>
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
  const [tab, setTab] = useState('flow')
  const [date, setDate] = useState(todayISO())
  const [form, setForm] = useState({ currency: 'SYP', opening: '', actual: '', note: '' })

  const flows = { SYP: cashFlow('SYP', date, date), USD: cashFlow('USD', date, date) }
  const dayClosings = s.closings.filter((c) => c.date === date)
  const sysCash = (cur) => {
    const prior = s.closings.filter((c) => c.currency === cur && c.date < date).sort((a, b) => b.date.localeCompare(a.date))[0]
    const opening = form[`opening_${cur}`] !== undefined ? Number(form[`opening_${cur}`] || 0) : (prior?.actualCash || 0)
    return { opening, net: flows[cur].net, system: opening + flows[cur].net }
  }
  const [openSYP, setOpenSYP] = useState('')
  const [openUSD, setOpenUSD] = useState('')
  const oSY = openSYP === '' ? (s.closings.filter((c) => c.currency === 'SYP' && c.date < date).sort((a, b) => b.date.localeCompare(a.date))[0]?.actualCash || 0) : Math.round(Number(openSYP) || 0)
  const oUS = openUSD === '' ? (s.closings.filter((c) => c.currency === 'USD' && c.date < date).sort((a, b) => b.date.localeCompare(a.date))[0]?.actualCash || 0) : Math.round(Number(openUSD) || 0)
  const sysSY = oSY + flows.SYP.net
  const sysUS = oUS + flows.USD.net
  const actualSY = Math.round(Number(form.actualSYP || 0) * 1)
  const actualUS = Math.round(Number(form.actualUSD || 0) * 100)
  const diffSY = actualSY - sysSY
  const diffUS = actualUS - sysUS

  const save = () => {
    if (form.actualSYP === '' || form.actualUSD === '') return onToast?.('أدخل النقد الفعلي للعملتين', 'error')
    saveClosing({ date, currency: 'SYP', opening: oSY, systemCash: sysSY, actualCash: actualSY, diff: diffSY, note: form.note })
    saveClosing({ date, currency: 'USD', opening: oUS, systemCash: sysUS, actualCash: actualUS, diff: diffUS, note: form.note })
    setForm({ ...form, actualSYP: '', actualUSD: '', note: '' })
    onToast?.('تم إقفال الصندوق للعملتين')
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      <PageHead title="الصندوق والإقفال" subtitle="حركة النقد بالليرة والدولار منفصلتين، وإقفال يومي دقيق"
        actions={<input type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} style={{ width: 160 }} />} />

      <div className="grid g2">
        {['SYP', 'USD'].map((cur) => {
          const f = flows[cur]
          return (
            <Card key={cur} dark>
              <div className="row between" style={{ marginBottom: 12 }}>
                <div className="row" style={{ gap: 8 }}>
                  <span className="ic" style={{ width: 38, height: 38, borderRadius: 11, background: 'rgba(255,255,255,.08)', display: 'grid', placeItems: 'center', color: '#E9CC8C' }}><Icon name={cur === 'SYP' ? 'bank' : 'dollar'} /></span>
                  <div><div style={{ fontSize: 13, fontWeight: 800 }}>صندوق {CURRENCIES[cur].name}</div><div className="tiny" style={{ color: 'rgba(247,237,221,.5)' }}>حركة يوم {fmtDate(date)}</div></div>
                </div>
                <div style={{ textAlign: 'end' }}>
                  <div className="tiny" style={{ color: 'rgba(247,237,221,.5)' }}>الصافي</div>
                  <b style={{ fontSize: 20, color: f.net >= 0 ? '#9AD9B6' : '#F0B3AE', direction: 'ltr' }}>{money(f.net, cur)}</b>
                </div>
              </div>
              <div className="grid g2" style={{ gap: 6 }}>
                {[['مبيعات نقدية', f.cashIn, 'green'], ['دفعات زبائن', f.payIn, 'green'], ['مشتريات مدفوعة', -f.purchases, 'red'], ['مصاريف', -f.expenses, 'red'], ['مسحوبات المحل', -f.withdrawals, 'red'], ['سلف العمال', -f.workerTx, 'red']].map(([l, v, t]) => (
                  <div key={l} className="row between" style={{ fontSize: 10.5, padding: '5px 0', borderBottom: '1px solid rgba(255,255,255,.06)' }}>
                    <span style={{ color: 'rgba(247,237,221,.6)' }}>{l}</span>
                    <b style={{ direction: 'ltr', color: v === 0 ? 'rgba(247,237,221,.35)' : t === 'green' ? '#9AD9B6' : '#F0B3AE' }}>{money(v, cur)}</b>
                  </div>
                ))}
              </div>
            </Card>
          )
        })}
      </div>

      <Seg block value={tab} onChange={setTab} options={[{ id: 'flow', label: 'حركة الصندوق', icon: 'swap' }, { id: 'close', label: 'إقفال الصندوق', icon: 'lock' }, { id: 'history', label: 'سجل الإقفالات', icon: 'history' }]} />

      {tab === 'flow' && (
        <Card title="تفاصيل الحركة" icon="swap" subtitle={`كل الحركات النقدية ليوم ${fmtDate(date)}`}>
          <div className="grid g2">
            {['SYP', 'USD'].map((cur) => {
              const f = flows[cur]
              const rows = [['مقبوضات المبيعات', f.cashIn], ['دفعات الزبائن', f.payIn], ['مدفوعات المشتريات', -f.purchases], ['المصاريف', -f.expenses], ['مسحوبات المحل', -f.withdrawals], ['سلف وبضاعة العمال', -f.workerTx]]
              return (
                <div key={cur}>
                  <div className="row between" style={{ marginBottom: 5 }}><b className="small">{CURRENCIES[cur].name}</b><span className={`badge ${f.net >= 0 ? 'badge-green' : 'badge-red'}`}>{money(f.net, cur)}</span></div>
                  <div className="bar-track" style={{ marginBottom: 10 }}><div className="bar-fill" style={{ width: `${Math.min(100, Math.abs(f.net) / Math.max(1, f.cashIn + f.payIn) * 100)}%`, background: f.net >= 0 ? 'var(--success)' : 'var(--danger)' }} /></div>
                  {rows.map(([l, v]) => <div key={l} className="row between" style={{ padding: '5px 0', borderBottom: '1px dashed var(--line)', fontSize: 11 }}><span className="muted">{l}</span><b style={{ direction: 'ltr' }}>{money(v, cur)}</b></div>)}
                </div>
              )
            })}
          </div>
        </Card>
      )}

      {tab === 'close' && (
        <div className="grid" style={{ gridTemplateColumns: 'minmax(0,1.3fr) minmax(0,1fr)', gap: 16, alignItems: 'start' }}>
          <Card title="إقفال صندوق اليوم" icon="lock" subtitle={fmtDate(date)}>
            <div className="grid g2" style={{ gap: 10 }}>
              {['SYP', 'USD'].map((cur) => (
                <div className="card tight" key={cur} style={{ background: 'var(--surface-2)' }}>
                  <b style={{ fontSize: 11.5 }}>{CURRENCIES[cur].name}</b>
                  <Field label="رصيد الافتتاح" style={{ marginTop: 6 }}>
                    <input className="input" inputMode="decimal" placeholder="0" value={cur === 'SYP' ? (openSYP === '' ? oSY / 1 : openSYP) : (openUSD === '' ? oUS / 100 : openUSD)}
                      onChange={(e) => cur === 'SYP' ? setOpenSYP(e.target.value) : setOpenUSD(e.target.value)} style={{ height: 34, fontSize: 11 }} />
                  </Field>
                  <div className="kv" style={{ marginTop: 6 }}><span>حركة اليوم</span><b>{money(flows[cur].net, cur)}</b></div>
                  <div className="kv"><span>النظام يتوقع</span><b>{money(cur === 'SYP' ? sysSY : sysUS, cur)}</b></div>
                  <Field label="النقد الفعلي" style={{ marginTop: 6 }}>
                    <input className="input" inputMode="decimal" value={cur === 'SYP' ? (form.actualSYP || '') : (form.actualUSD || '')}
                      onChange={(e) => setForm({ ...form, [cur === 'SYP' ? 'actualSYP' : 'actualUSD']: e.target.value })} style={{ height: 34, fontSize: 12, fontWeight: 800 }} />
                  </Field>
                  <div className="kv" style={{ marginTop: 4 }}><span>الفرق</span>
                    <b style={{ color: (cur === 'SYP' ? diffSY : diffUS) === 0 ? 'var(--muted)' : (cur === 'SYP' ? diffSY : diffUS) > 0 ? 'var(--success)' : 'var(--danger)' }}>
                      {form[cur === 'SYP' ? 'actualSYP' : 'actualUSD'] === '' ? '—' : money(cur === 'SYP' ? diffSY : diffUS, cur)}
                    </b>
                  </div>
                </div>
              ))}
            </div>
            <Field label="ملاحظة الإقفال" style={{ marginTop: 10 }}><input className="input" value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} placeholder="أي فرق أو ملاحظة…" /></Field>
            <button className="btn btn-gold btn-block" style={{ marginTop: 10 }} onClick={save}><Icon name="lock" />إقفال الصندوق للعملتين</button>
          </Card>

          <Card title="ملخص الإقفال" icon="receipt">
            {dayClosings.length > 0 && <div className="row" style={{ gap: 6, marginBottom: 10 }}><span className="badge badge-green"><Icon name="check" size={11} />تم الإقفال لهذا اليوم</span></div>}
            <div className="stack">
              {['SYP', 'USD'].map((cur) => {
                const c = dayClosings.find((x) => x.currency === cur)
                return (
                  <div key={cur} className="card tight" style={{ background: 'var(--surface-2)' }}>
                    <div className="row between"><b className="small">{CURRENCIES[cur].name}</b>{c ? <span className={`badge ${c.diff === 0 ? 'badge-green' : c.diff < 0 ? 'badge-red' : 'badge-warn'}`}>{c.diff === 0 ? 'مطابق' : money(c.diff, cur)}</span> : <span className="badge badge-neutral">لم يُقفل</span>}</div>
                    <div className="kv"><span>النظام</span><b>{money(cur === 'SYP' ? sysSY : sysUS, cur)}</b></div>
                    {c && <><div className="kv"><span>الفعلي</span><b>{money(c.actualCash, cur)}</b></div><div className="kv"><span>بواسطة</span><b>{c.createdBy}</b></div></>}
                  </div>
                )
              })}
            </div>
            <div className="tiny muted" style={{ marginTop: 10, lineHeight: 1.8 }}>
              يُحسب رصيد الافتتاح تلقائياً من آخر إقفال سابق، ويمكن تعديله يدوياً عند الحاجة.
            </div>
          </Card>
        </div>
      )}

      {tab === 'history' && (
        <Card title="سجل الإقفالات" icon="history" flush>
          {s.closings.length === 0 ? <Empty icon="history" title="لا إقفالات بعد" /> : (
            <div className="table-wrap" style={{ border: 0 }}>
              <table>
                <thead><tr><th>التاريخ</th><th>العملة</th><th className="num">الافتتاح</th><th className="num">توقع النظام</th><th className="num">الفعلي</th><th className="num">الفرق</th><th>ملاحظة</th><th>بواسطة</th></tr></thead>
                <tbody>
                  {s.closings.slice().sort((a, b) => b.date.localeCompare(a.date)).map((c) => (
                    <tr key={c.id}>
                      <td>{fmtDate(c.date)}</td>
                      <td><span className="badge badge-neutral">{c.currency === 'SYP' ? 'ل.س' : '$'}</span></td>
                      <td className="num">{money(c.opening, c.currency, { bare: true })}</td>
                      <td className="num">{money(c.systemCash, c.currency, { bare: true })}</td>
                      <td className="num strong">{money(c.actualCash, c.currency, { bare: true })}</td>
                      <td className="num" style={{ color: c.diff === 0 ? 'var(--success)' : 'var(--danger)', fontWeight: 800 }}>{c.diff === 0 ? 'مطابق' : money(c.diff, c.currency, { bare: true })}</td>
                      <td className="small muted">{c.note || '—'}</td>
                      <td className="small">{c.createdBy}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      )}
    </div>
  )
}
