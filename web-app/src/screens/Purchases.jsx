import { useMemo, useState } from 'react'
import { useStore, saveInvoice, nextNo, rateNow, partyStats } from '../store'
import { CURRENCIES, UNITS, money, parseAmount, convert, unitFactor, invoiceTotals, qtyBaseOf, todayISO, fmtDate, rateToNumber, lineTotal } from '../utils'
import { Card, Empty, Field, Modal, PageHead, Drawer, StatusBadge, Seg, Money } from '../components'
import { Icon } from '../icons'

const newLine = () => ({ itemId: '', name: '', unit: 'kg', qtyMilli: 1000, unitPrice: 0, discount: 0, costPerBase: 0, baseQty: 0 })

export default function Purchases({ onToast }) {
  const s = useStore()
  const [open, setOpen] = useState(false)
  const [detail, setDetail] = useState(null)
  const [q, setQ] = useState('')

  const list = useMemo(() => s.invoices.filter((i) => i.type === 'purchase').filter((i) => !q || i.no.includes(q) || (i.partyName || '').includes(q)).sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || '')), [s.invoices, q])
  const totals = list.filter((i) => i.status !== 'cancelled').reduce((a, i) => { a[i.currency] += i.total; a.rem[i.currency] += i.remaining || 0; return a }, { SYP: 0, USD: 0, rem: { SYP: 0, USD: 0 } })

  return (
    <div className="grid" style={{ gap: 16 }}>
      <PageHead title="المشتريات" subtitle="توريد الأصناف يزيد المخزون ويحدّث متوسط التكلفة تلقائياً"
        actions={<button className="btn btn-gold" onClick={() => setOpen(true)}><Icon name="plus" />فاتورة شراء</button>} />

      <div className="grid g4">
        <div className="kpi gold"><div className="top"><span className="lbl">مشتريات بالليرة</span><span className="ic"><Icon name="bag" /></span></div><div className="val">{money(totals.SYP, 'SYP', { bare: true })}</div><div className="foot">{list.filter((i) => i.currency === 'SYP').length} فاتورة</div></div>
        <div className="kpi"><div className="top"><span className="lbl">مشتريات بالدولار</span><span className="ic"><Icon name="dollar" /></span></div><div className="val">{money(totals.USD, 'USD', { bare: true })}</div><div className="foot">{list.filter((i) => i.currency === 'USD').length} فاتورة</div></div>
        <div className="kpi"><div className="top"><span className="lbl">متبقٍ للموردين</span><span className="ic"><Icon name="wallet" /></span></div><div className="val">{money(totals.rem.SYP, 'SYP', { bare: true })}</div><div className="foot">+ {money(totals.rem.USD, 'USD')}</div></div>
        <div className="kpi"><div className="top"><span className="lbl">عدد المشتريات</span><span className="ic"><Icon name="truck" /></span></div><div className="val">{list.length}</div><div className="foot">آخر توريد {list[0] ? fmtDate(list[0].date) : '—'}</div></div>
      </div>

      <Card tight>
        <div className="field-icon">
          <Icon name="search" />
          <input className="input" placeholder="ابحث برقم الفاتورة أو اسم المورد…" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
      </Card>

      <Card flush>
        {list.length === 0 ? <Empty icon="bag" title="لا مشتريات بعد" text="سجّل أول توريد لزيادة المخزون." action={<button className="btn btn-gold" onClick={() => setOpen(true)}>فاتورة شراء</button>} /> : (
          <>
            <div className="table-wrap desktop-table" style={{ border: 0, borderRadius: 0 }}>
              <table>
                <thead><tr><th>الفاتورة</th><th>التاريخ</th><th>المورد</th><th>العملة</th><th className="num">الإجمالي</th><th className="num">المدفوع</th><th className="num">المتبقي</th><th>الحالة</th><th></th></tr></thead>
                <tbody>
                  {list.map((i) => (
                    <tr key={i.id}>
                      <td><b>{i.no}</b><div className="tiny muted">{i.lines?.length} صنف</div></td>
                      <td>{fmtDate(i.date)}</td>
                      <td>{i.partyName}</td>
                      <td><span className="badge badge-neutral">{i.currency === 'SYP' ? 'ل.س' : '$'}</span></td>
                      <td className="num strong">{money(i.total, i.currency, { bare: true })}</td>
                      <td className="num">{money(i.paid || 0, i.currency, { bare: true })}</td>
                      <td className="num" style={{ color: i.remaining > 0 ? 'var(--danger)' : 'var(--muted)' }}>{money(i.remaining || 0, i.currency, { bare: true })}</td>
                      <td><StatusBadge status={i.status} /></td>
                      <td><button className="btn btn-ghost btn-sm" onClick={() => setDetail(i)}><Icon name="eye" />عرض</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="mobile-cards" style={{ padding: 12 }}>
              {list.map((i) => (
                <div className="m-card" key={i.id}>
                  <div className="top">
                    <div><div className="title">{i.no} · {i.partyName}</div><div className="sub">{fmtDate(i.date)} · {i.lines?.length} صنف</div></div>
                    <div className="amount">{money(i.total, i.currency)}</div>
                  </div>
                  <div className="foot"><span>المتبقي {money(i.remaining || 0, i.currency)}</span><button className="btn btn-ghost btn-sm" onClick={() => setDetail(i)}>عرض</button></div>
                </div>
              ))}
            </div>
          </>
        )}
      </Card>

      {open && <PurchaseForm onClose={() => setOpen(false)} onSave={() => { setOpen(false); onToast?.('تم حفظ فاتورة الشراء وتحديث المخزون') }} />}
      {detail && <PurchaseDetail inv={detail} onClose={() => setDetail(null)} />}
    </div>
  )
}

function PurchaseForm({ onClose, onSave }) {
  const s = useStore()
  const [supplierId, setSupplierId] = useState(s.suppliers[0]?.id || '')
  const [currency, setCurrency] = useState('SYP')
  const [date, setDate] = useState(todayISO())
  const [lines, setLines] = useState([])
  const [paidInput, setPaidInput] = useState('')
  const [payCurrency, setPayCurrency] = useState('SYP')
  const [notes, setNotes] = useState('')
  const rate = s.settings.rate
  const dec = CURRENCIES[currency].decimals
  const totals = invoiceTotals(lines, 'none', 0)
  const paidMinor = Math.round(parseAmount(paidInput) * Math.pow(10, CURRENCIES[payCurrency].decimals))
  const paidInv = payCurrency === currency ? paidMinor : convert(paidMinor, payCurrency, currency, rate)
  const paid = Math.min(totals.total, Math.max(0, paidInv))
  const remaining = totals.total - paid

  const addLine = () => {
    const item = s.items.find((x) => x.active && !x.isBasket)
    if (!item) return
    const costSYP = item.cost || 0
    const cost = currency === 'SYP' ? costSYP : convert(costSYP, 'SYP', currency, rate)
    const line = { ...newLine(), itemId: item.id, name: item.name, unit: item.unit === 'kg' ? 'kg' : 'piece', unitPrice: cost, costPerBase: cost }
    line.baseQty = qtyBaseOf(line, s.settings.ozGrams)
    setLines((ls) => [...ls, line])
  }

  const setLine = (idx, patch) => setLines((ls) => ls.map((l, i) => {
    if (i !== idx) return l
    const next = { ...l, ...patch }
    next.baseQty = qtyBaseOf(next, s.settings.ozGrams)
    return next
  }))

  const setItem = (idx, itemId) => {
    const item = s.items.find((x) => x.id === itemId)
    if (!item) return
    const costSY = item.cost || 0
    const cost = currency === 'SYP' ? costSY : convert(costSY, 'SYP', currency, rate)
    setLine(idx, { itemId, name: item.name, unit: item.unit === 'kg' ? 'kg' : 'piece', unitPrice: cost, costPerBase: cost })
  }

  const submit = () => {
    if (!supplierId) return
    if (!lines.length) return
    const supplier = s.suppliers.find((x) => x.id === supplierId)
    const inv = {
      type: 'purchase', no: nextNo('purchase'), date,
      partyId: supplierId, partyName: supplier?.name || '',
      currency, rate, lines,
      discountType: 'none', discountValue: 0,
      subtotal: totals.subtotal, discount: 0, total: totals.total, cost: totals.total, profit: 0,
      paid, paidCurrency: payCurrency, paidForeign: payCurrency !== currency ? paidMinor : 0,
      paidMethod: paid >= totals.total ? 'cash' : paid > 0 ? 'partial' : 'debt',
      tenders: paid > 0 ? [{ currency: payCurrency, amount: paidMinor }] : [],
      remaining, change: 0, status: 'completed', notes,
    }
    saveInvoice(inv)
    onSave()
  }

  return (
    <Drawer open onClose={onClose} title="فاتورة شراء جديدة" subtitle="إضافة توريد إلى المخزون وتحديث التكلفة" icon="bag" width={680}
      footer={<>
        <button className="btn btn-gold" onClick={submit} disabled={!lines.length}><Icon name="checkCircle" />حفظ الفاتورة</button>
        <button className="btn btn-ghost" onClick={onClose}>إلغاء</button>
      </>}>
      <div className="stack">
        <div className="grid g2" style={{ gap: 8 }}>
          <Field label="المورد">
            <select className="select" value={supplierId} onChange={(e) => setSupplierId(e.target.value)}>
              {s.suppliers.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}
            </select>
          </Field>
          <Field label="التاريخ"><input type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} /></Field>
        </div>
        <div className="row between">
          <Field label="عملة الشراء">
            <Seg value={currency} onChange={(v) => { setCurrency(v); setPayCurrency(v) }} options={[{ id: 'SYP', label: 'ليرة' }, { id: 'USD', label: 'دولار' }]} />
          </Field>
          <span className="chip"><Icon name="dollar" />{rateToNumber(rate).toLocaleString('en-US')} ل.س / $</span>
        </div>

        <div className="sep" />
        <div className="row between">
          <b style={{ fontSize: 13 }}>الأصناف الموردة</b>
          <button className="btn btn-subtle btn-sm" onClick={addLine}><Icon name="plus" />إضافة صنف</button>
        </div>
        {lines.length === 0 ? <Empty icon="package" title="أضف أصناف التوريد" text="اختر الصنف ثم أدخل الكمية وسعر التكلفة." /> : (
          <div className="stack" style={{ gap: 8 }}>
            {lines.map((l, idx) => {
              const item = s.items.find((x) => x.id === l.itemId)
              return (
                <div className="card tight" key={idx} style={{ background: 'var(--surface-2)' }}>
                  <div className="row between" style={{ marginBottom: 7 }}>
                    <select className="select" value={l.itemId} onChange={(e) => setItem(idx, e.target.value)} style={{ flex: 1, height: 34, fontSize: 11.5 }}>
                      {s.items.filter((x) => x.active && !x.isBasket).map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}
                    </select>
                    <button className="btn btn-ghost btn-icon" onClick={() => setLines((ls) => ls.filter((_, i) => i !== idx))}><Icon name="trash" /></button>
                  </div>
                  <div className="grid" style={{ gridTemplateColumns: '1fr 1fr 1.2fr 1fr', gap: 6 }}>
                    <Field label="الكمية"><input className="input" inputMode="decimal" value={l.qtyMilli / 1000} onChange={(e) => setLine(idx, { qtyMilli: Math.max(0, Math.round(parseAmount(e.target.value) * 1000)) })} style={{ height: 34, fontSize: 11 }} /></Field>
                    <Field label="الوحدة">
                      <select className="select" value={l.unit} onChange={(e) => setLine(idx, { unit: e.target.value })} style={{ height: 34, fontSize: 11 }}>
                        {item?.unit === 'kg' ? <><option value="kg">كغ</option><option value="g">غرام</option></> : <option value="piece">قطعة</option>}
                      </select>
                    </Field>
                    <Field label={`التكلفة / ${UNITS[l.unit]?.name}`}><input className="input" inputMode="decimal" value={l.unitPrice / Math.pow(10, dec)} onChange={(e) => setLine(idx, { unitPrice: Math.round(parseAmount(e.target.value) * Math.pow(10, dec)) })} style={{ height: 34, fontSize: 11 }} /></Field>
                    <div><div className="tiny muted" style={{ marginBottom: 6 }}>المجموع</div><div style={{ height: 34, display: 'flex', alignItems: 'center', fontWeight: 800, fontSize: 11.5, direction: 'ltr' }}>{money(lineTotal(l), currency)}</div></div>
                  </div>
                </div>
              )
            })}
          </div>
        )}

        <div className="sep" />
        <div className="totals">
          <div className="tr"><span>المجموع</span><b>{money(totals.total, currency)}</b></div>
        </div>
        <div className="grid g2" style={{ gap: 8 }}>
          <Field label="المدفوع للمورد"><input className="input" inputMode="decimal" value={paidInput} onChange={(e) => setPaidInput(e.target.value)} placeholder="0" /></Field>
          <Field label="عملة الدفع"><select className="select" value={payCurrency} onChange={(e) => setPayCurrency(e.target.value)}><option value="SYP">ليرة</option><option value="USD">دولار</option></select></Field>
        </div>
        <div className="row between card tight" style={{ background: 'var(--surface-2)' }}>
          <div><div className="tiny muted">المتبقي للمورد</div><b style={{ color: remaining > 0 ? 'var(--danger)' : 'var(--success)' }}>{money(remaining, currency)}</b></div>
          <div style={{ textAlign: 'end' }}><div className="tiny muted">قيمة المخزون المضافة</div><b>{money(totals.total, currency)}</b></div>
        </div>
        <Field label="ملاحظات"><input className="input" value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="رقم إشعار التوريد…" /></Field>
      </div>
    </Drawer>
  )
}

function PurchaseDetail({ inv, onClose }) {
  return (
    <Modal open onClose={onClose} title={`فاتورة شراء ${inv.no}`} subtitle={`${inv.partyName} · ${fmtDate(inv.date)}`} icon="bag" size="wide"
      footer={<button className="btn btn-ghost" onClick={onClose}>إغلاق</button>}>
      <div className="table-wrap">
        <table>
          <thead><tr><th>الصنف</th><th>الكمية</th><th className="num">السعر</th><th className="num">المجموع</th></tr></thead>
          <tbody>
            {inv.lines.map((l, i) => (
              <tr key={i}><td><b>{l.name}</b></td><td>{l.qtyMilli / 1000} {UNITS[l.unit]?.name}</td><td className="num">{money(l.unitPrice, inv.currency, { bare: true })}</td><td className="num strong">{money(lineTotal(l), inv.currency, { bare: true })}</td></tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="row between" style={{ marginTop: 12, background: 'var(--surface-2)', padding: 12, borderRadius: 10 }}>
        <div><div className="tiny muted">الإجمالي</div><b style={{ fontSize: 16 }}>{money(inv.total, inv.currency)}</b></div>
        <div><div className="tiny muted">المدفوع</div><b>{money(inv.paid, inv.currency)}</b></div>
        <div><div className="tiny muted">المتبقي</div><b style={{ color: inv.remaining > 0 ? 'var(--danger)' : 'var(--success)' }}>{money(inv.remaining, inv.currency)}</b></div>
        <StatusBadge status={inv.status} />
      </div>
      {inv.notes && <div className="tiny muted" style={{ marginTop: 8 }}>ملاحظة: {inv.notes}</div>}
    </Modal>
  )
}
