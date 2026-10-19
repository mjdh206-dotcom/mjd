import { useMemo, useState } from 'react'
import { useStore, addStocktake, addRoast, lowStockItems, expiringItems } from '../store'
import { money, parseAmount, fmtDate, todayISO, daysUntil, UNITS, fmtQty } from '../utils'
import { Card, Empty, Field, PageHead, Seg, StatusBadge } from '../components'
import { Icon } from '../icons'

export default function Inventory({ onToast }) {
  const s = useStore()
  const [tab, setTab] = useState('overview')
  const low = lowStockItems()
  const exp = expiringItems(45)
  const value = s.items.reduce((a, i) => a + i.stock * (i.cost || 0), 0)
  const grams = s.items.filter((i) => i.unit === 'kg').reduce((a, i) => a + i.stock, 0)
  const pieces = s.items.filter((i) => i.unit !== 'kg').reduce((a, i) => a + i.stock, 0)

  return (
    <div className="grid" style={{ gap: 16 }}>
      <PageHead title="المخزون والتحميص" subtitle="جرد دقيق، تحميص القهوة، وتتبع كل حركة مخزون" />

      <div className="grid g4">
        <div className="kpi"><div className="top"><span className="lbl">قيمة المخزون</span><span className="ic"><Icon name="warehouse" /></span></div><div className="val">{money(value, 'SYP', { bare: true })}</div><div className="foot">ل.س بسعر التكلفة</div></div>
        <div className="kpi gold"><div className="top"><span className="lbl">المواد الموزونة</span><span className="ic"><Icon name="scale" /></span></div><div className="val">{(grams / 1000).toLocaleString('en-US', { maximumFractionDigits: 1 })}</div><div className="foot">كيلوغرام إجمالي</div></div>
        <div className="kpi"><div className="top"><span className="lbl">القطع والعلب</span><span className="ic"><Icon name="box" /></span></div><div className="val">{pieces}</div><div className="foot">قطعة في المخزون</div></div>
        <div className="kpi"><div className="top"><span className="lbl">تنبيهات</span><span className="ic"><Icon name="alert" /></span></div><div className="val">{low.length + exp.length}</div><div className="foot">{low.length} منخفض · {exp.length} صلاحية</div></div>
      </div>

      <Seg block value={tab} onChange={setTab} options={[
        { id: 'overview', label: 'نظرة عامة', icon: 'grid' },
        { id: 'stocktake', label: 'الجرد', icon: 'clipboardCheck' },
        { id: 'roast', label: 'التحميص', icon: 'flame' },
        { id: 'moves', label: 'حركات المخزون', icon: 'swap' },
      ]} />

      {tab === 'overview' && (
        <div className="grid" style={{ gridTemplateColumns: 'minmax(0,1fr) minmax(0,1fr)', gap: 16, alignItems: 'start' }}>
          <Card title="أصناف تحت الحد الأدنى" icon="alert" badge={low.length} action={<button className="btn btn-ghost btn-sm" onClick={() => setTab('stocktake')}>جرد</button>}>
            {low.length === 0 ? <Empty icon="checkCircle" title="المخزون آمن" text="كل الأصناف فوق الحد الأدنى." /> : (
              <div className="stack" style={{ gap: 0 }}>
                {low.map((i) => (
                  <div key={i.id} className="list-item">
                    <span className="ic-tile" style={{ background: 'var(--danger-soft)', color: 'var(--danger)', border: 0 }}><Icon name="package" /></span>
                    <div className="txt"><div className="t1">{i.name}</div><div className="t2">الحد الأدنى: {i.unit === 'kg' ? `${i.minStock / 1000} كغ` : `${i.minStock} قطعة`}</div></div>
                    <div className="end"><b style={{ color: 'var(--danger)' }}>{i.unit === 'kg' ? `${(i.stock / 1000).toLocaleString('en-US')} كغ` : `${i.stock}`}</b></div>
                  </div>
                ))}
              </div>
            )}
          </Card>

          <Card title="صلاحية قريبة" icon="clock" badge={exp.length}>
            {exp.length === 0 ? <Empty icon="checkCircle" title="لا أصناف قاربت الانتهاء" /> : (
              <div className="stack" style={{ gap: 0 }}>
                {exp.map((i) => {
                  const d = daysUntil(i.expiry)
                  return (
                    <div key={i.id} className="list-item">
                      <span className="ic-tile" style={{ background: d < 0 ? 'var(--danger-soft)' : 'var(--warn-soft)', color: d < 0 ? 'var(--danger)' : 'var(--warn)', border: 0 }}><Icon name="clock" /></span>
                      <div className="txt"><div className="t1">{i.name}</div><div className="t2">ينتهي {fmtDate(i.expiry)}</div></div>
                      <span className={`badge ${d < 0 ? 'badge-red' : d < 15 ? 'badge-warn' : 'badge-neutral'}`}>{d < 0 ? `منتهٍ منذ ${Math.abs(d)} يوم` : `${d} يوم`}</span>
                    </div>
                  )
                })}
              </div>
            )}
          </Card>

          <Card title="كل الأصناف" icon="package" badge={s.items.length} className="g2-span" style={{ gridColumn: '1 / -1' }}>
            <div className="table-wrap">
              <table>
                <thead><tr><th>الصنف</th><th>الوحدة</th><th className="num">المخزون</th><th className="num">الحد الأدنى</th><th className="num">التكلفة/وحدة</th><th>الحالة</th></tr></thead>
                <tbody>
                  {s.items.map((i) => {
                    const isLow = i.minStock > 0 && i.stock <= i.minStock
                    return (
                      <tr key={i.id}>
                        <td><b>{i.name}</b><div className="tiny muted">{i.barcode}</div></td>
                        <td className="small">{UNITS[i.unit]?.name}</td>
                        <td className="num strong">{i.unit === 'kg' ? `${(i.stock / 1000).toLocaleString('en-US', { maximumFractionDigits: 2 })} كغ` : `${i.stock} قطعة`}</td>
                        <td className="num muted">{i.minStock ? (i.unit === 'kg' ? `${i.minStock / 1000} كغ` : i.minStock) : '—'}</td>
                        <td className="num">{money(i.cost, 'SYP', { bare: true })}</td>
                        <td><span className={`badge ${isLow ? 'badge-red' : 'badge-green'}`}>{isLow ? 'منخفض' : 'آمن'}</span></td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </Card>
        </div>
      )}

      {tab === 'stocktake' && <StocktakeTab onToast={onToast} />}
      {tab === 'roast' && <RoastTab onToast={onToast} />}
      {tab === 'moves' && <MovesTab />}
    </div>
  )
}

function StocktakeTab({ onToast }) {
  const s = useStore()
  const [itemId, setItemId] = useState(s.items[0]?.id || '')
  const [counted, setCounted] = useState('')
  const [note, setNote] = useState('')
  const item = s.items.find((i) => i.id === itemId)
  const isWeight = item?.unit === 'kg'
  const countedBase = Math.round(parseAmount(counted) * (isWeight ? 1000 : 1))
  const diff = item ? countedBase - item.stock : 0

  return (
    <div className="grid" style={{ gridTemplateColumns: 'minmax(0,1fr) minmax(0,1.5fr)', gap: 16, alignItems: 'start' }}>
      <Card title="جرد صنف" icon="clipboardCheck" subtitle="أدخل الكمية الفعلية بعد العدّ">
        <div className="stack">
          <Field label="الصنف">
            <select className="select" value={itemId} onChange={(e) => { setItemId(e.target.value); setCounted('') }}>
              {s.items.map((i) => <option key={i.id} value={i.id}>{i.name}</option>)}
            </select>
          </Field>
          <div className="row between card tight" style={{ background: 'var(--surface-2)' }}>
            <span className="small muted">كمية النظام</span>
            <b>{item ? (isWeight ? `${(item.stock / 1000).toLocaleString('en-US')} كغ` : `${item.stock} قطعة`) : '—'}</b>
          </div>
          <Field label={isWeight ? 'الكمية المعدودة (كغ)' : 'الكمية المعدودة (قطعة)'}>
            <input className="input" inputMode="decimal" value={counted} onChange={(e) => setCounted(e.target.value)} placeholder="0" style={{ fontSize: 17, fontWeight: 800 }} />
          </Field>
          <Field label="ملاحظة"><input className="input" value={note} onChange={(e) => setNote(e.target.value)} placeholder="جرد أسبوعي، تلف…" /></Field>
          {counted !== '' && item && (
            <div className="row between card tight" style={{ background: diff === 0 ? 'var(--success-soft)' : diff > 0 ? 'var(--info-soft)' : 'var(--danger-soft)' }}>
              <span className="small" style={{ fontWeight: 700 }}>الفرق</span>
              <b style={{ color: diff === 0 ? 'var(--success)' : diff > 0 ? 'var(--info)' : 'var(--danger)' }}>
                {diff === 0 ? 'مطابق تماماً' : `${diff > 0 ? '+' : ''}${isWeight ? `${(diff / 1000).toLocaleString('en-US')} كغ` : `${diff} قطعة`}`}
              </b>
            </div>
          )}
          <button className="btn btn-gold" onClick={() => {
            if (counted === '' || !item) return onToast?.('أدخل الكمية المعدودة', 'error')
            addStocktake({ date: todayISO(), itemId, systemQty: item.stock, countedQty: countedBase, note })
            setCounted(''); setNote(''); onToast?.('تم تسجيل الجرد وتعديل المخزون')
          }}><Icon name="save" />حفظ الجرد</button>
        </div>
      </Card>

      <Card title="سجل الجرد" icon="history" badge={s.stocktakes.length}>
        {s.stocktakes.length === 0 ? <Empty icon="clipboard" title="لا جرد بعد" /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>التاريخ</th><th>الصنف</th><th className="num">النظام</th><th className="num">الفعلي</th><th className="num">الفرق</th><th>ملاحظة</th></tr></thead>
              <tbody>
                {s.stocktakes.map((st) => {
                  const it = s.items.find((x) => x.id === st.itemId)
                  const w = it?.unit === 'kg'
                  return (
                    <tr key={st.id}>
                      <td>{fmtDate(st.date)}</td><td><b>{it?.name || '—'}</b></td>
                      <td className="num">{w ? (st.systemQty / 1000).toLocaleString('en-US') : st.systemQty}</td>
                      <td className="num">{w ? (st.countedQty / 1000).toLocaleString('en-US') : st.countedQty}</td>
                      <td className="num" style={{ color: st.diff === 0 ? 'var(--success)' : st.diff < 0 ? 'var(--danger)' : 'var(--info)', fontWeight: 800 }}>{st.diff > 0 ? '+' : ''}{w ? (st.diff / 1000).toLocaleString('en-US') : st.diff}</td>
                      <td className="small muted">{st.note || '—'}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  )
}

function RoastTab({ onToast }) {
  const s = useStore()
  const coffee = s.items.filter((i) => i.category === 'coffee')
  const [rawId, setRawId] = useState(coffee[0]?.id || '')
  const [roastedId, setRoastedId] = useState(coffee[1]?.id || '')
  const [rawQty, setRawQty] = useState('')
  const [roastedQty, setRoastedQty] = useState('')
  const [note, setNote] = useState('')
  const raw = parseAmount(rawQty) * 1000
  const roasted = parseAmount(roastedQty) * 1000
  const loss = raw - roasted
  const lossPct = raw > 0 ? (loss / raw) * 100 : 0
  const rawCost = s.items.find((i) => i.id === rawId)?.cost || 0
  const costPerKg = roasted > 0 ? Math.round((rawCost * raw) / roasted) : 0

  return (
    <div className="grid" style={{ gridTemplateColumns: 'minmax(0,1fr) minmax(0,1.4fr)', gap: 16, alignItems: 'start' }}>
      <Card title="دفعة تحميص" icon="flame" subtitle="تخصم الخام وتضيف المحمّص بتكلفته الفعلية">
        <div className="stack">
          <div className="grid g2" style={{ gap: 8 }}>
            <Field label="البن الخام">
              <select className="select" value={rawId} onChange={(e) => setRawId(e.target.value)}>
                {coffee.map((i) => <option key={i.id} value={i.id}>{i.name}</option>)}
              </select>
            </Field>
            <Field label="البن المحمّص">
              <select className="select" value={roastedId} onChange={(e) => setRoastedId(e.target.value)}>
                {coffee.map((i) => <option key={i.id} value={i.id}>{i.name}</option>)}
              </select>
            </Field>
          </div>
          <div className="grid g2" style={{ gap: 8 }}>
            <Field label="الكمية الخام (كغ)"><input className="input" inputMode="decimal" value={rawQty} onChange={(e) => setRawQty(e.target.value)} placeholder="0" /></Field>
            <Field label="الكمية بعد التحميص (كغ)"><input className="input" inputMode="decimal" value={roastedQty} onChange={(e) => setRoastedQty(e.target.value)} placeholder="0" /></Field>
          </div>
          {raw > 0 && roasted > 0 && (
            <div className="grid g2" style={{ gap: 8 }}>
              <div className="card tight" style={{ background: 'var(--warn-soft)' }}><div className="tiny muted">الفاقد</div><b style={{ color: 'var(--warn)' }}>{(loss / 1000).toLocaleString('en-US')} كغ · {lossPct.toFixed(1)}%</b></div>
              <div className="card tight" style={{ background: 'var(--surface-2)' }}><div className="tiny muted">التكلفة الفعلية للمحمّص</div><b>{money(costPerKg, 'SYP')} / كغ</b></div>
            </div>
          )}
          <Field label="ملاحظة"><input className="input" value={note} onChange={(e) => setNote(e.target.value)} placeholder="درجة التحميص، المدة…" /></Field>
          <button className="btn btn-gold" onClick={() => {
            if (raw <= 0 || roasted <= 0) return onToast?.('أدخل كميتين صحيحتين', 'error')
            addRoast({ date: todayISO(), rawItemId: rawId, roastedItemId: roastedId, rawQtyBase: raw, roastedQtyBase: roasted, notes: note })
            setRawQty(''); setRoastedQty(''); setNote(''); onToast?.('تم تسجيل دفعة التحميص')
          }}><Icon name="flame" />تسجيل التحميص</button>
        </div>
      </Card>

      <Card title="سجل التحميص" icon="history" badge={s.roasts.length}>
        {s.roasts.length === 0 ? <Empty icon="flame" title="لا دفعات تحميص" /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>التاريخ</th><th>الخام → المحمّص</th><th className="num">الخام</th><th className="num">الناتج</th><th className="num">الفاقد</th><th className="num">التكلفة/كغ</th></tr></thead>
              <tbody>
                {s.roasts.map((r) => {
                  const rawIt = s.items.find((x) => x.id === r.rawItemId)
                  const roIt = s.items.find((x) => x.id === r.roastedItemId)
                  return (
                    <tr key={r.id}>
                      <td>{fmtDate(r.date)}</td>
                      <td className="small">{rawIt?.name || '—'} <Icon name="arrowLeft" size={11} style={{ verticalAlign: -1 }} /> {roIt?.name || '—'}</td>
                      <td className="num">{(r.rawQtyBase / 1000).toLocaleString('en-US')} كغ</td>
                      <td className="num">{(r.roastedQtyBase / 1000).toLocaleString('en-US')} كغ</td>
                      <td className="num" style={{ color: 'var(--warn)', fontWeight: 700 }}>{r.lossPct?.toFixed(1)}%</td>
                      <td className="num strong">{money(r.actualCostPerKg, 'SYP', { bare: true })}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  )
}

function MovesTab() {
  const s = useStore()
  const [type, setType] = useState('all')
  const list = useMemo(() => s.stockMoves.filter((m) => type === 'all' || m.type === type).slice(0, 150), [s.stockMoves, type])
  const labels = { sale: 'بيع', purchase: 'شراء', stocktake: 'جرد', 'adjust-in': 'إضافة يدوية', 'adjust-out': 'خصم يدوي', roast: 'تحميص' }
  return (
    <Card title="حركات المخزون" icon="swap" badge={s.stockMoves.length}
      action={<Seg value={type} onChange={setType} options={[{ id: 'all', label: 'الكل' }, { id: 'sale', label: 'بيع' }, { id: 'purchase', label: 'شراء' }, { id: 'stocktake', label: 'جرد' }]} size="sm" />}>
      {list.length === 0 ? <Empty icon="swap" title="لا حركات" /> : (
        <div className="table-wrap">
          <table>
            <thead><tr><th>التاريخ</th><th>الصنف</th><th>الحركة</th><th className="num">الكمية</th><th>المرجع</th><th>ملاحظة</th></tr></thead>
            <tbody>
              {list.map((m) => {
                const it = s.items.find((x) => x.id === m.itemId)
                const w = it?.unit === 'kg'
                return (
                  <tr key={m.id}>
                    <td className="small">{new Date(m.at).toLocaleString('ar-SY', { dateStyle: 'short', timeStyle: 'short' })}</td>
                    <td><b>{it?.name || m.itemId}</b></td>
                    <td><span className={`badge ${['sale', 'adjust-out'].includes(m.type) ? 'badge-red' : m.type === 'stocktake' ? 'badge-warn' : 'badge-green'}`}>{labels[m.type] || m.type}</span></td>
                    <td className="num" style={{ fontWeight: 800, color: m.qtyBase < 0 ? 'var(--danger)' : 'var(--success)' }}>{m.qtyBase > 0 ? '+' : ''}{w ? (m.qtyBase / 1000).toLocaleString('en-US') : m.qtyBase}{w ? ' كغ' : ''}</td>
                    <td className="small muted">{m.refId || '—'}</td>
                    <td className="small muted">{m.note || '—'}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  )
}
