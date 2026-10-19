import { useMemo, useState } from 'react'
import { useStore, lowStockItems, expiringItems, cashFlow, partyStats, setRate, rateNow } from '../store'
import { money, todayISO, daysUntil, rateToNumber, convert, fmtDate } from '../utils'
import { Card, Stat, Empty, StatusBadge } from '../components'

export default function Dashboard({ go, onToast }) {
  const s = useStore()
  const isManager = s.session?.role === 'manager'
  const today = todayISO()
  const [rate, setRateInput] = useState(String(rateToNumber(s.settings.rate)))

  const stats = useMemo(() => {
    const todayInvoices = s.invoices.filter((i) => i.type === 'sale' && i.status !== 'cancelled' && i.date === today)
    const byCur = { SYP: 0, USD: 0 }
    todayInvoices.forEach((i) => { byCur[i.currency] += i.total })
    const profit = { SYP: 0, USD: 0 }
    todayInvoices.forEach((i) => { profit[i.currency] += (i.profit || 0) })
    const unpaid = s.invoices.filter((i) => i.type === 'sale' && i.status !== 'cancelled')
    const recv = { SYP: 0, USD: 0 }
    unpaid.forEach((i) => { recv[i.currency] += i.remaining || 0 })
    // خصم الدفعات
    s.payments.filter((p) => p.partyType === 'customer').forEach((p) => { if (recv[p.currency] !== undefined) recv[p.currency] -= p.amount })
    const payables = { SYP: 0, USD: 0 }
    s.invoices.filter((i) => i.type === 'purchase' && i.status !== 'cancelled').forEach((i) => { payables[i.currency] += i.remaining || 0 })
    s.payments.filter((p) => p.partyType === 'supplier').forEach((p) => { if (payables[p.currency] !== undefined) payables[p.currency] -= p.amount })
    return { todayInvoices, byCur, profit, recv, payables }
  }, [s, today])

  const low = lowStockItems()
  const exp = expiringItems(30)
  const cashSYP = cashFlow('SYP', today, today)
  const cashUSD = cashFlow('USD', today, today)
  const recent = s.invoices.filter((i) => i.type === 'sale').sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || '')).slice(0, 6)

  return (
    <div className="grid" style={{ gap: 12 }}>
      {/* ترحيب + سعر الصرف */}
      <Card style={{ background: 'linear-gradient(135deg,#4E342E,#6D4C41 60%,#5D4037)', color: '#fff', border: 0 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
          <div>
            <div className="small" style={{ opacity: .8 }}>{new Date().toLocaleDateString('ar-SY', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</div>
            <h2 style={{ fontFamily: 'Amiri,serif', fontSize: 24, marginTop: 2 }}>أهلاً، {s.session?.name}</h2>
            <div className="small" style={{ opacity: .85, marginTop: 2 }}>{s.settings.shopName} — {s.settings.branch}</div>
          </div>
          <div style={{ background: 'rgba(255,255,255,.12)', borderRadius: 14, padding: 12, minWidth: 230, border: '1px solid rgba(255,255,255,.18)' }}>
            <div className="small" style={{ opacity: .85 }}>سعر صرف الدولار</div>
            <div style={{ fontSize: 22, fontWeight: 800, color: '#FFD54F' }}>{rateNow().toLocaleString('en-US')} <span style={{ fontSize: 13 }}>ل.س</span></div>
            <div className="row" style={{ gap: 6, marginTop: 6, flexWrap: 'nowrap' }}>
              <input className="input" style={{ padding: '6px 8px', fontSize: 12, background: 'rgba(255,255,255,.14)', border: '1px solid rgba(255,255,255,.22)', color: '#fff' }}
                inputMode="decimal" value={rate} onChange={(e) => setRateInput(e.target.value)} />
              <button className="btn btn-gold btn-sm" style={{ whiteSpace: 'nowrap' }} onClick={() => { setRate(Number(rate)); onToast?.('تم تحديث سعر الصرف') }}>تحديث</button>
            </div>
          </div>
        </div>
      </Card>

      {/* إحصاءات اليوم */}
      <div className="grid grid-4">
        <Stat icon="🧾" label="مبيعات اليوم — ليرة" value={money(stats.byCur.SYP, 'SYP', { bare: true })} sub={`${stats.todayInvoices.filter((i) => i.currency === 'SYP').length} فاتورة`} tone="gold" />
        <Stat icon="💵" label="مبيعات اليوم — دولار" value={money(stats.byCur.USD, 'USD', { bare: true })} sub={`${stats.todayInvoices.filter((i) => i.currency === 'USD').length} فاتورة`} />
        <Stat icon="📥" label="ذمم الزبائن" value={money(stats.recv.SYP, 'SYP', { bare: true })} sub={`+ ${money(stats.recv.USD, 'USD')}`} />
        <Stat icon="📤" label="مستحقات الموردين" value={money(stats.payables.SYP, 'SYP', { bare: true })} sub={`+ ${money(stats.payables.USD, 'USD')}`} />
      </div>

      {isManager && (
        <div className="grid grid-4">
          <Stat icon="📈" label="ربح اليوم — ليرة" value={money(stats.profit.SYP, 'SYP', { bare: true })} sub="بعد خصم التكلفة" tone="gold" />
          <Stat icon="📈" label="ربح اليوم — دولار" value={money(stats.profit.USD, 'USD', { bare: true })} />
          <Stat icon="💰" label="صندوق الليرة اليوم" value={money(cashSYP.net, 'SYP', { bare: true })} sub="صافي حركة الصندوق" />
          <Stat icon="💵" label="صندوق الدولار اليوم" value={money(cashUSD.net, 'USD', { bare: true })} sub="صافي حركة الصندوق" />
        </div>
      )}

      {/* اختصارات */}
      <Card title="اختصارات سريعة">
        <div className="grid" style={{ gridTemplateColumns: 'repeat(auto-fill,minmax(120px,1fr))', gap: 8 }}>
          {[
            ['sale-new', '🧾', 'فاتورة جديدة'],
            ['customers', '👥', 'الزبائن'],
            ['items', '🥜', 'الأصناف'],
            ['purchases', '🛒', 'شراء'],
            ['inventory', '📦', 'المخزون'],
            ['expenses', '💸', 'مصروف'],
            ['preorders', '🎁', 'طلب مسبق'],
            ['reports', '📊', 'التقارير'],
          ].map(([id, ic, label]) => (
            <button key={id} onClick={() => go(id)} style={{ padding: '14px 8px', borderRadius: 12, border: '1px solid var(--border)', background: 'var(--card)', display: 'grid', gap: 6, justifyItems: 'center', color: 'var(--text)' }}>
              <span style={{ fontSize: 22 }}>{ic}</span>
              <span className="small" style={{ fontWeight: 700 }}>{label}</span>
            </button>
          ))}
        </div>
      </Card>

      <div className="grid grid-2">
        {/* التنبيهات */}
        <Card title="تنبيهات المخزون" badge={low.length + exp.length}>
          {low.length === 0 && exp.length === 0 && <Empty icon="✅" text="كل شيء تحت السيطرة" />}
          {low.slice(0, 4).map((i) => (
            <div key={i.id} className="row" style={{ justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--border)' }}>
              <div>
                <div style={{ fontWeight: 700, fontSize: 13 }}>{i.name}</div>
                <div className="small muted">الكمية: {i.unit === 'kg' ? `${(i.stock / 1000).toLocaleString()} كغ` : `${i.stock} قطعة`} — الحد: {i.minStock}</div>
              </div>
              <span className="badge badge-red">مخزون منخفض</span>
            </div>
          ))}
          {exp.slice(0, 4).map((i) => {
            const d = daysUntil(i.expiry)
            return (
              <div key={i.id} className="row" style={{ justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--border)' }}>
                <div>
                  <div style={{ fontWeight: 700, fontSize: 13 }}>{i.name}</div>
                  <div className="small muted">ينتهي في {fmtDate(i.expiry)}</div>
                </div>
                <span className={`badge ${d < 0 ? 'badge-red' : 'badge-gold'}`}>{d < 0 ? `منتهي منذ ${Math.abs(d)} يوم` : `باقٍ ${d} يوم`}</span>
              </div>
            )
          })}
          <button className="btn btn-outline btn-sm" style={{ marginTop: 10 }} onClick={() => go('inventory')}>عرض المخزون</button>
        </Card>

        {/* آخر الفواتير */}
        <Card title="آخر الفواتير" action={<button className="btn btn-outline btn-sm" onClick={() => go('sales')}>الكل</button>}>
          {recent.length === 0 && <Empty />}
          {recent.map((i) => (
            <div key={i.id} className="row" style={{ justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--border)', cursor: 'pointer' }} onClick={() => go('invoice-view', { id: i.id })}>
              <div>
                <div style={{ fontWeight: 700, fontSize: 13 }}>{i.no} — {i.partyName}</div>
                <div className="small muted">{fmtDate(i.date)} • {i.lines.length} بند</div>
              </div>
              <div style={{ textAlign: 'left' }}>
                <div style={{ fontWeight: 800, fontSize: 13, direction: 'ltr' }}>{money(i.total, i.currency)}</div>
                <StatusBadge status={i.status} />
              </div>
            </div>
          ))}
        </Card>
      </div>

      {/* ملخص بالعملتين */}
      <Card title="نظرة عامة على الأرصدة">
        <div className="table-wrap">
          <table>
            <thead><tr><th>البند</th><th>بالليرة السورية</th><th>بالدولار</th><th>مقدّر (ل.س)</th></tr></thead>
            <tbody>
              <tr>
                <td>ذمم الزبائن</td>
                <td>{money(stats.recv.SYP, 'SYP')}</td>
                <td>{money(stats.recv.USD, 'USD')}</td>
                <td>{money(stats.recv.SYP + convert(stats.recv.USD, 'USD', 'SYP', s.settings.rate), 'SYP')}</td>
              </tr>
              <tr>
                <td>مستحقات الموردين</td>
                <td>{money(stats.payables.SYP, 'SYP')}</td>
                <td>{money(stats.payables.USD, 'USD')}</td>
                <td>{money(stats.payables.SYP + convert(stats.payables.USD, 'USD', 'SYP', s.settings.rate), 'SYP')}</td>
              </tr>
              <tr>
                <td>صافي اليوم</td>
                <td>{money(cashSYP.net, 'SYP')}</td>
                <td>{money(cashUSD.net, 'USD')}</td>
                <td>{money(cashSYP.net + convert(cashUSD.net, 'USD', 'SYP', s.settings.rate), 'SYP')}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  )
}
