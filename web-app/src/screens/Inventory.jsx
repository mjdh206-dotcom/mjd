import { useMemo, useState } from 'react'
import { useStore, addStocktake, addRoast } from '../store'
import { money, parseAmount, fmtDate, fmtDateTime, daysUntil, todayISO } from '../utils'
import { Card, Empty, Field, Modal, Stat, Tabs, Progress } from '../components'

export default function Inventory({ onToast }) {
  const s = useStore()
  const [tab, setTab] = useState('overview')
  const [stocktake, setStocktake] = useState(null)
  const [roast, setRoast] = useState(false)

  const low = s.items.filter((i) => i.active && i.minStock > 0 && i.stock <= i.minStock)
  const exp = s.items.filter((i) => i.expiry && i.active).sort((a, b) => a.expiry.localeCompare(b.expiry))
  const totalValue = s.items.reduce((a, i) => a + i.stock * (i.unit === 'kg' ? i.cost / 1000 : i.cost), 0)

  return (
    <div className="grid" style={{ gap: 12 }}>
      <div className="grid grid-3">
        <Stat icon="📦" label="قيمة المخزون (بالتكلفة)" value={money(Math.round(totalValue), 'SYP', { bare: true })} tone="gold" />
        <Stat icon="⚠️" label="أصناف تحت الحد الأدنى" value={String(low.length)} sub={low.slice(0, 2).map((x) => x.name).join('، ')} />
        <Stat icon="⏳" label="أصناف قاربت الانتهاء" value={String(exp.filter((i) => daysUntil(i.expiry) <= 60).length)} sub="خلال 60 يوماً" />
      </div>

      <Card>
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <Tabs value={tab} onChange={setTab} tabs={[
            { id: 'overview', label: 'المخزون' },
            { id: 'alerts', label: `التنبيهات (${low.length + exp.filter((i) => daysUntil(i.expiry) <= 30).length})` },
            { id: 'roast', label: 'التحميص' },
            { id: 'moves', label: 'حركات المخزون' },
            { id: 'stocktakes', label: 'الجرد' },
          ]} />
          <div className="row" style={{ gap: 6 }}>
            <button className="btn btn-outline btn-sm" onClick={() => setRoast(true)}>🔥 تحميص</button>
            <button className="btn btn-gold btn-sm" onClick={() => setStocktake({ itemId: s.items[0]?.id, counted: '', note: '' })}>📋 جرد يدوي</button>
          </div>
        </div>

        {tab === 'overview' && (
          <div className="table-wrap">
            <table>
              <thead><tr><th>الصنف</th><th>المخزون</th><th>الحد الأدنى</th><th>الحالة</th><th>التكلفة</th><th>الصلاحية</th></tr></thead>
              <tbody>
                {s.items.filter((i) => i.active).map((i) => {
                  const isW = i.unit === 'kg'
                  const ratio = i.minStock ? i.stock / (i.minStock * 3) : 1
                  return (
                    <tr key={i.id}>
                      <td style={{ fontWeight: 700 }}>{i.name}</td>
                      <td style={{ minWidth: 120 }}>
                        <div className="small" style={{ marginBottom: 3 }}>{isW ? `${(i.stock / 1000).toLocaleString()} كغ` : `${i.stock} قطعة`}</div>
                        <Progress value={Math.min(ratio, 1)} max={1} color={i.stock <= i.minStock ? 'var(--danger)' : 'var(--gold)'} />
                      </td>
                      <td className="small">{isW ? `${(i.minStock / 1000).toLocaleString()} كغ` : `${i.minStock}`}</td>
                      <td>{i.stock <= i.minStock ? <span className="badge badge-red">تحت الحد</span> : <span className="badge badge-green">جيد</span>}</td>
                      <td style={{ direction: 'ltr' }} className="small">{money(i.cost, 'SYP')}</td>
                      <td className="small">{i.expiry ? fmtDate(i.expiry) : '—'}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}

        {tab === 'alerts' && (
          <div className="grid grid-2">
            <div>
              <b style={{ fontSize: 13 }}>⚠️ مخزون منخفض</b>
              {low.length === 0 ? <Empty icon="✅" text="لا يوجد نقص" /> : low.map((i) => (
                <div key={i.id} className="row" style={{ justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--border)' }}>
                  <div><div style={{ fontWeight: 700, fontSize: 13 }}>{i.name}</div><div className="small muted">المتوفر: {i.unit === 'kg' ? `${i.stock / 1000} كغ` : `${i.stock}`}</div></div>
                  <span className="badge badge-red">الحد {i.unit === 'kg' ? i.minStock / 1000 : i.minStock}</span>
                </div>
              ))}
            </div>
            <div>
              <b style={{ fontSize: 13 }}>⏳ قرب انتهاء الصلاحية</b>
              {exp.length === 0 ? <Empty icon="✅" text="لا يوجد" /> : exp.map((i) => {
                const d = daysUntil(i.expiry)
                return (
                  <div key={i.id} className="row" style={{ justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--border)' }}>
                    <div><div style={{ fontWeight: 700, fontSize: 13 }}>{i.name}</div><div className="small muted">{fmtDate(i.expiry)}</div></div>
                    <span className={`badge ${d < 0 ? 'badge-red' : d <= 30 ? 'badge-gold' : 'badge-green'}`}>{d < 0 ? 'منتهي' : `باقٍ ${d} يوم`}</span>
                  </div>
                )
              })}
            </div>
          </div>
        )}

        {tab === 'roast' && (
          <div className="grid" style={{ gap: 10 }}>
            <div className="small muted">سجّل كمية البن الخام وكمية المحمّص الناتجة لحساب الفاقد والتكلفة الفعلية للكيلو المحمّص.</div>
            {s.roasts.length === 0 ? <Empty icon="🔥" text="لا توجد عمليات تحميص" /> : (
              <div className="table-wrap">
                <table>
                  <thead><tr><th>التاريخ</th><th>الخام</th><th>الكمية الخام</th><th>المحمّص</th><th>الكمية المحمّصة</th><th>الفاقد</th><th>نسبة الفاقد</th><th>التكلفة الفعلية/كغ</th></tr></thead>
                  <tbody>
                    {s.roasts.map((r) => {
                      const raw = s.items.find((i) => i.id === r.rawItemId)
                      const roasted = s.items.find((i) => i.id === r.roastedItemId)
                      return (
                        <tr key={r.id}>
                          <td>{fmtDate(r.date)}</td>
                          <td>{raw?.name || '—'}</td>
                          <td>{(r.rawQtyBase / 1000).toFixed(2)} كغ</td>
                          <td>{roasted?.name || '—'}</td>
                          <td>{(r.roastedQtyBase / 1000).toFixed(2)} كغ</td>
                          <td style={{ color: 'var(--danger)' }}>{(r.loss / 1000).toFixed(2)} كغ</td>
                          <td>{r.lossPct?.toFixed(1)}%</td>
                          <td style={{ direction: 'ltr', fontWeight: 700 }}>{money(r.actualCostPerKg, 'SYP')}</td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {tab === 'moves' && (
          s.stockMoves.length === 0 ? <Empty icon="🔄" text="لا توجد حركات مسجلة" /> : (
            <div className="table-wrap" style={{ maxHeight: 420 }}>
              <table>
                <thead><tr><th>التاريخ</th><th>الصنف</th><th>الكمية</th><th>النوع</th><th>ملاحظة</th></tr></thead>
                <tbody>
                  {s.stockMoves.slice(0, 100).map((m) => {
                    const it = s.items.find((i) => i.id === m.itemId)
                    return (
                      <tr key={m.id}>
                        <td className="small">{fmtDateTime(m.at)}</td>
                        <td>{it?.name}</td>
                        <td style={{ direction: 'ltr', color: m.qtyBase >= 0 ? 'var(--success)' : 'var(--danger)', fontWeight: 700 }}>
                          {m.qtyBase >= 0 ? '+' : ''}{m.qtyBase} {it?.unit === 'kg' ? 'غ' : ''}
                        </td>
                        <td className="small">{{ stocktake: 'جرد', roast: 'تحميص', purchase: 'شراء', sale: 'بيع' }[m.type] || m.type}</td>
                        <td className="small">{m.note}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )
        )}

        {tab === 'stocktakes' && (
          s.stocktakes.length === 0 ? <Empty icon="📋" text="لم يتم إجراء جرد بعد" /> : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>التاريخ</th><th>الصنف</th><th>النظام</th><th>الفعلي</th><th>الفرق</th><th>ملاحظة</th><th>بواسطة</th></tr></thead>
                <tbody>
                  {s.stocktakes.map((t) => {
                    const it = s.items.find((i) => i.id === t.itemId)
                    return (
                      <tr key={t.id}>
                        <td>{fmtDate(t.date)}</td><td>{it?.name}</td>
                        <td>{t.systemQty}</td><td>{t.countedQty}</td>
                        <td style={{ color: t.diff === 0 ? 'inherit' : 'var(--danger)', fontWeight: 700 }}>{t.diff > 0 ? '+' : ''}{t.diff}</td>
                        <td>{t.note}</td><td className="small muted">{t.createdBy}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )
        )}
      </Card>

      {stocktake && <StocktakeModal init={stocktake} onClose={() => setStocktake(null)} onSave={(v) => { addStocktake(v); setStocktake(null); onToast?.('تم تسجيل الجرد وتعديل المخزون') }} />}
      {roast && <RoastModal onClose={() => setRoast(false)} onSave={(v) => { addRoast(v); setRoast(false); onToast?.('تم تسجيل عملية التحميص') }} />}
    </div>
  )
}

function StocktakeModal({ init, onClose, onSave }) {
  const s = useStore()
  const [itemId, setItemId] = useState(init.itemId)
  const [counted, setCounted] = useState('')
  const [note, setNote] = useState('')
  const [date, setDate] = useState(todayISO())
  const item = s.items.find((i) => i.id === itemId)
  const isW = item?.unit === 'kg'
  const countedBase = isW ? Math.round(parseAmount(counted) * 1000) : Math.round(parseAmount(counted))

  return (
    <Modal open onClose={onClose} title="جرد يدوي"
      footer={<button className="btn btn-gold" onClick={() => {
        if (!item) return
        onSave({ itemId, systemQty: item.stock, countedQty: countedBase, note, date })
      }}>حفظ الجرد</button>}>
      <div className="grid" style={{ gap: 10 }}>
        <Field label="الصنف">
          <select className="select" value={itemId} onChange={(e) => setItemId(e.target.value)}>
            {s.items.map((i) => <option key={i.id} value={i.id}>{i.name}</option>)}
          </select>
        </Field>
        <div className="row" style={{ justifyContent: 'space-between', background: 'var(--cream)', padding: 10, borderRadius: 10 }}>
          <span className="small">الكمية حسب النظام</span>
          <b>{isW ? `${(item?.stock || 0) / 1000} كغ` : `${item?.stock || 0} قطعة`}</b>
        </div>
        <Field label={`الكمية الفعلية (${isW ? 'كغ' : 'قطعة'})`}>
          <input className="input" inputMode="decimal" value={counted} onChange={(e) => setCounted(e.target.value)} />
        </Field>
        {counted !== '' && (
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <span className="small">الفرق</span>
            <b style={{ direction: 'ltr', color: countedBase - (item?.stock || 0) === 0 ? 'inherit' : 'var(--danger)' }}>
              {((countedBase - (item?.stock || 0)) / (isW ? 1000 : 1)).toFixed(isW ? 2 : 0)} {isW ? 'كغ' : 'قطعة'}
            </b>
          </div>
        )}
        <Field label="تاريخ الجرد"><input type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} /></Field>
        <Field label="ملاحظة"><input className="input" value={note} onChange={(e) => setNote(e.target.value)} /></Field>
      </div>
    </Modal>
  )
}

function RoastModal({ onClose, onSave }) {
  const s = useStore()
  const coffeeItems = s.items.filter((i) => i.category === 'coffee')
  const [rawItemId, setRaw] = useState(coffeeItems.find((i) => i.name.includes('خام'))?.id || coffeeItems[0]?.id)
  const [roastedItemId, setRoasted] = useState(coffeeItems.find((i) => i.name.includes('تركية'))?.id || coffeeItems[1]?.id)
  const [rawQty, setRawQty] = useState('')
  const [roastedQty, setRoastedQty] = useState('')
  const [date, setDate] = useState(todayISO())
  const [notes, setNotes] = useState('')

  const rawBase = Math.round(parseAmount(rawQty) * 1000)
  const roastedBase = Math.round(parseAmount(roastedQty) * 1000)
  const raw = s.items.find((i) => i.id === rawItemId)
  const loss = rawBase - roastedBase
  const cost = raw ? Math.round((raw.cost * rawBase) / (roastedBase || 1) * 100) / 100 : 0

  return (
    <Modal open onClose={onClose} title="تحويل التحميص"
      footer={<button className="btn btn-gold" onClick={() => {
        if (rawBase > 0 && roastedBase > 0) onSave({ rawItemId, roastedItemId, rawQtyBase: rawBase, roastedQtyBase: roastedBase, date, notes })
      }}>حفظ عملية التحميص</button>}>
      <div className="grid" style={{ gap: 10 }}>
        <div className="grid grid-2" style={{ gap: 8 }}>
          <Field label="البن الخام">
            <select className="select" value={rawItemId} onChange={(e) => setRaw(e.target.value)}>
              {coffeeItems.map((i) => <option key={i.id} value={i.id}>{i.name}</option>)}
            </select>
          </Field>
          <Field label="الصنف المحمّص">
            <select className="select" value={roastedItemId} onChange={(e) => setRoasted(e.target.value)}>
              {coffeeItems.map((i) => <option key={i.id} value={i.id}>{i.name}</option>)}
            </select>
          </Field>
          <Field label="كمية الخام (كغ)"><input className="input" inputMode="decimal" value={rawQty} onChange={(e) => setRawQty(e.target.value)} /></Field>
          <Field label="كمية المحمّص (كغ)"><input className="input" inputMode="decimal" value={roastedQty} onChange={(e) => setRoastedQty(e.target.value)} /></Field>
        </div>
        <Field label="التاريخ"><input type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} /></Field>
        <Field label="ملاحظة"><input className="input" value={notes} onChange={(e) => setNotes(e.target.value)} /></Field>
        <div className="card" style={{ background: 'var(--cream)' }}>
          <div className="row" style={{ justifyContent: 'space-between' }}><span className="small">الفاقد</span><b>{(loss / 1000).toFixed(2)} كغ ({rawBase ? ((loss / rawBase) * 100).toFixed(1) : 0}%)</b></div>
          <div className="row" style={{ justifyContent: 'space-between' }}><span className="small">التكلفة الفعلية للكيلو المحمّص</span><b style={{ direction: 'ltr' }}>{money(Math.round(cost), 'SYP')}</b></div>
        </div>
      </div>
    </Modal>
  )
}
