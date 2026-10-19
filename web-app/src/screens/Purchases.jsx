import { useMemo, useState } from 'react'
import { useStore, saveInvoice, nextNo, invoicesFor, rateNow } from '../store'
import { money, parseAmount, fmtDate, CURRENCIES, invoiceTotals, qtyBaseOf, lineTotal, todayISO, rateToNumber, fmtQty } from '../utils'
import { Card, Empty, Field, Modal, StatusBadge } from '../components'

export default function Purchases({ onToast }) {
  const s = useStore()
  const [open, setOpen] = useState(false)
  const [list, setList] = useState([])
  const [view, setView] = useState(null)

  const purchases = useMemo(() => invoicesFor('purchase'), [s])

  return (
    <div className="grid" style={{ gap: 12 }}>
      <Card>
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <div className="small muted">فاتورة الشراء تزيد المخزون تلقائياً وتحدّث متوسط تكلفة الصنف</div>
          <button className="btn btn-gold btn-sm" onClick={() => setOpen(!open)}>{open ? 'إغلاق' : '＋ فاتورة شراء'}</button>
        </div>
      </Card>

      {open && <PurchaseEditor onDone={(inv) => { saveInvoice(inv); setOpen(false); onToast?.(`تم حفظ فاتورة الشراء ${inv.no} وتحديث المخزون`) }} onCancel={() => setOpen(false)} />}

      <Card title="فواتير المشتريات" badge={purchases.length}>
        {purchases.length === 0 ? <Empty icon="🛒" /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>الرقم</th><th>التاريخ</th><th>المورد</th><th>العملة</th><th>الإجمالي</th><th>المدفوع</th><th>المتبقي</th><th>الحالة</th><th></th></tr></thead>
              <tbody>
                {purchases.map((i) => (
                  <tr key={i.id}>
                    <td style={{ fontWeight: 800 }}>{i.no}</td>
                    <td>{fmtDate(i.date)}</td>
                    <td>{i.partyName}</td>
                    <td>{i.currency === 'SYP' ? 'ل.س' : '$'}</td>
                    <td style={{ direction: 'ltr', fontWeight: 700 }}>{money(i.total, i.currency, { bare: true })}</td>
                    <td style={{ direction: 'ltr' }}>{money(i.paid, i.currency, { bare: true })}</td>
                    <td style={{ direction: 'ltr' }}>{money(i.remaining, i.currency, { bare: true })}</td>
                    <td><StatusBadge status={i.status} /></td>
                    <td><button className="btn btn-outline btn-sm" onClick={() => setView(i)}>عرض</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {view && (
        <Modal open onClose={() => setView(null)} wide title={`فاتورة شراء ${view.no}`} footer={<button className="btn btn-outline" onClick={() => setView(null)}>إغلاق</button>}>
          <div className="small muted" style={{ marginBottom: 8 }}>{view.partyName} — {fmtDate(view.date)}</div>
          <div className="table-wrap">
            <table>
              <thead><tr><th>الصنف</th><th>الكمية</th><th>سعر الوحدة</th><th>المجموع</th></tr></thead>
              <tbody>
                {view.lines.map((l, i) => (
                  <tr key={i}><td>{l.name}</td><td>{fmtQty(l)}</td><td style={{ direction: 'ltr' }}>{money(l.unitPrice, view.currency)}</td><td style={{ direction: 'ltr' }}>{money(lineTotal(l), view.currency)}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="row" style={{ justifyContent: 'flex-end', marginTop: 10, gap: 16 }}>
            <span>الإجمالي: <b style={{ direction: 'ltr' }}>{money(view.total, view.currency)}</b></span>
            <span>المدفوع: <b style={{ direction: 'ltr' }}>{money(view.paid, view.currency)}</b></span>
            <span>المتبقي: <b style={{ direction: 'ltr', color: 'var(--danger)' }}>{money(view.remaining, view.currency)}</b></span>
          </div>
        </Modal>
      )}
    </div>
  )
}

function PurchaseEditor({ onDone, onCancel }) {
  const s = useStore()
  const [supplierId, setSupplierId] = useState(s.suppliers[0]?.id || '')
  const [currency, setCurrency] = useState('SYP')
  const [rate] = useState(rateNow())
  const [date, setDate] = useState(todayISO())
  const [lines, setLines] = useState([])
  const [pick, setPick] = useState(null)
  const [payMode, setPayMode] = useState('cash')
  const [paidInput, setPaidInput] = useState('')
  const [notes, setNotes] = useState('')
  const dec = CURRENCIES[currency].decimals

  const totals = invoiceTotals(lines, 'none', 0)
  const paid = payMode === 'cash' ? totals.total
    : payMode === 'debt' ? 0
      : Math.round(parseAmount(paidInput) * Math.pow(10, dec))

  const addLine = (item) => {
    const unit = item.unit === 'kg' ? 'kg' : 'piece'
    const unitPrice = item.unit === 'kg' ? item.cost : item.cost
    const line = {
      itemId: item.id, name: item.name, unit, qtyMilli: 1000, unitPrice,
      discount: 0, costPerBase: unitPrice, baseQty: qtyBaseOf({ unit, qtyMilli: 1000, unitPrice }, s.settings.ozGrams),
    }
    setLines((ls) => [...ls, line])
    setPick(null)
  }

  const update = (idx, patch) => setLines((ls) => ls.map((l, i) => {
    if (i !== idx) return l
    const next = { ...l, ...patch }
    next.costPerBase = next.unitPrice
    next.baseQty = qtyBaseOf(next, s.settings.ozGrams)
    return next
  }))

  const save = () => {
    if (!lines.length) return
    const inv = {
      type: 'purchase', no: nextNo('purchase'), date,
      partyId: supplierId, partyName: s.suppliers.find((x) => x.id === supplierId)?.name || '',
      currency, rate, lines, discountType: 'none', discountValue: 0,
      subtotal: totals.subtotal, discount: 0, total: totals.total, cost: totals.cost, profit: 0,
      paid, paidCurrency: currency, remaining: totals.total - paid,
      status: 'completed', notes,
    }
    onDone(inv)
  }

  return (
    <Card title="فاتورة شراء جديدة">
      <div className="grid grid-3" style={{ gap: 10 }}>
        <Field label="المورد">
          <select className="select" value={supplierId} onChange={(e) => setSupplierId(e.target.value)}>
            {s.suppliers.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}
          </select>
        </Field>
        <Field label="التاريخ"><input type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} /></Field>
        <Field label="العملة">
          <div className="row" style={{ gap: 6 }}>
            {['SYP', 'USD'].map((c) => <button key={c} className={`btn btn-sm ${currency === c ? 'btn-brown' : 'btn-outline'}`} style={{ flex: 1 }} onClick={() => setCurrency(c)}>{CURRENCIES[c].name}</button>)}
          </div>
        </Field>
      </div>

      <div className="sep" />
      <div className="row" style={{ justifyContent: 'space-between', marginBottom: 8 }}>
        <b style={{ fontSize: 13 }}>أصناف الفاتورة</b>
        <select className="select" style={{ maxWidth: 260 }} value="" onChange={(e) => { const it = s.items.find((x) => x.id === e.target.value); if (it) addLine(it) }}>
          <option value="">＋ اختر صنفاً لإضافته</option>
          {s.items.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}
        </select>
      </div>

      {lines.length === 0 ? <Empty icon="📦" text="أضف أصنافاً إلى فاتورة الشراء" /> : (
        <div className="table-wrap">
          <table>
            <thead><tr><th>الصنف</th><th>الكمية</th><th>الوحدة</th><th>سعر الشراء</th><th>المجموع</th><th></th></tr></thead>
            <tbody>
              {lines.map((l, i) => (
                <tr key={i}>
                  <td style={{ fontWeight: 700 }}>{l.name}</td>
                  <td><input className="input" style={{ width: 80, padding: '6px 8px' }} inputMode="decimal" value={l.qtyMilli / 1000} onChange={(e) => update(i, { qtyMilli: Math.round(parseAmount(e.target.value) * 1000) })} /></td>
                  <td>
                    <select className="select" style={{ width: 85, padding: '6px 6px' }} value={l.unit} onChange={(e) => update(i, { unit: e.target.value })}>
                      <option value="kg">كيلو</option><option value="g">غرام</option><option value="piece">قطعة</option>
                    </select>
                  </td>
                  <td><input className="input" style={{ width: 100, padding: '6px 8px' }} inputMode="decimal" value={l.unitPrice / Math.pow(10, dec)} onChange={(e) => update(i, { unitPrice: Math.round(parseAmount(e.target.value) * Math.pow(10, dec)) })} /></td>
                  <td style={{ direction: 'ltr', fontWeight: 700 }}>{money(lineTotal(l), currency, { bare: true })}</td>
                  <td><button className="btn btn-outline btn-sm" onClick={() => setLines((ls) => ls.filter((_, x) => x !== i))}>✕</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="grid grid-3" style={{ marginTop: 12, gap: 10, alignItems: 'end' }}>
        <div>
          <Field label="الدفع">
            <div className="row" style={{ gap: 5 }}>
              {[['cash', 'نقدي'], ['debt', 'دين'], ['partial', 'جزئي']].map(([v, l]) => (
                <button key={v} className={`btn btn-sm ${payMode === v ? 'btn-brown' : 'btn-outline'}`} style={{ flex: 1 }} onClick={() => setPayMode(v)}>{l}</button>
              ))}
            </div>
          </Field>
          {payMode === 'partial' && <input className="input" style={{ marginTop: 6 }} inputMode="decimal" placeholder="المبلغ المدفوع" value={paidInput} onChange={(e) => setPaidInput(e.target.value)} />}
        </div>
        <Field label="ملاحظة"><input className="input" value={notes} onChange={(e) => setNotes(e.target.value)} /></Field>
        <div className="card" style={{ background: 'var(--cream)', padding: 10 }}>
          <div className="row" style={{ justifyContent: 'space-between' }}><span className="small">الإجمالي</span><b style={{ direction: 'ltr' }}>{money(totals.total, currency)}</b></div>
          <div className="row" style={{ justifyContent: 'space-between' }}><span className="small">المتبقي</span><b style={{ direction: 'ltr', color: 'var(--danger)' }}>{money(totals.total - paid, currency)}</b></div>
        </div>
      </div>

      <div className="row" style={{ marginTop: 10 }}>
        <button className="btn btn-gold" onClick={save}>💾 حفظ فاتورة الشراء</button>
        <button className="btn btn-outline" onClick={onCancel}>إلغاء</button>
        <span className="small muted">سعر الصرف: {rateToNumber(rate).toLocaleString()} ل.س</span>
      </div>
    </Card>
  )
}
