import { useEffect, useMemo, useRef, useState } from 'react'
import { useStore, saveInvoice, lastPriceFor, nextNo, rateNow } from '../store'
import {
  CURRENCIES, UNITS, money, parseAmount, toMinor, convert, unitFactor, invoiceTotals,
  qtyBaseOf, lineTotal, fmtQty, uid, rateToNumber, todayISO,
} from '../utils'
import { Card, Field, Modal, Money } from '../components'

const newLine = () => ({
  itemId: null, name: '', unit: 'kg', qtyMilli: 1000, unitPrice: 0, discount: 0,
  costPerBase: 0, baseQty: 0, isBasket: false,
})

export default function SaleInvoice({ go, onToast, editId, onDone }) {
  const s = useStore()
  const isManager = s.session?.role === 'manager'
  const editing = editId ? s.invoices.find((i) => i.id === editId) : null

  const [customerId, setCustomerId] = useState(editing?.partyId || 'cash')
  const [currency, setCurrency] = useState(editing?.currency || s.settings.defaultCurrency || 'SYP')
  const [lines, setLines] = useState(editing?.lines || [])
  const [discountType, setDiscountType] = useState(editing?.discountType || 'none')
  const [discountValue, setDiscountValue] = useState(
    editing ? (editing.discountType === 'percent' ? editing.discountValue : editing.discountValue / Math.pow(10, CURRENCIES[editing.currency].decimals)) : 0,
  )
  const [payMode, setPayMode] = useState(editing?.paidMethod || 'cash')
  const [payCurrency, setPayCurrency] = useState(editing?.paidCurrency || editing?.currency || s.settings.defaultCurrency || 'SYP')
  const [paidInput, setPaidInput] = useState(
    editing && editing.paidMethod === 'partial'
      ? String((editing.paidForeign || editing.paid) / Math.pow(10, CURRENCIES[editing.paidCurrency || editing.currency].decimals))
      : '',
  )
  const [notes, setNotes] = useState(editing?.notes || '')
  const [search, setSearch] = useState('')
  const [pickerItem, setPickerItem] = useState(null)
  const [barcodeMode, setBarcodeMode] = useState(false)
  const [date, setDate] = useState(editing?.date || todayISO())
  const [priceTier, setPriceTier] = useState('retail')
  const [listening, setListening] = useState(false)
  const barcodeRef = useRef(null)
  const searchRef = useRef(null)

  const rate = editing?.rate || s.settings.rate
  const customer = s.customers.find((c) => c.id === customerId)
  const ozGrams = s.settings.ozGrams
  const dec = CURRENCIES[currency].decimals

  const discountMinor = discountType === 'percent'
    ? 0
    : Math.round(parseAmount(discountValue) * Math.pow(10, dec))

  const totals = useMemo(() => {
    if (discountType === 'percent') return invoiceTotals(lines, 'percent', parseAmount(discountValue))
    return invoiceTotals(lines, 'fixed', discountMinor)
  }, [lines, discountType, discountValue, discountMinor])

  const filteredItems = useMemo(() => {
    const q = search.trim()
    let list = s.items.filter((i) => i.active)
    if (q) list = list.filter((i) => i.name.includes(q) || i.barcode.includes(q))
    return list
  }, [s.items, search])

  // الدفع
  const paidMinor = useMemo(() => {
    if (payMode === 'cash') return totals.total
    if (payMode === 'debt') return 0
    return Math.round(parseAmount(paidInput) * Math.pow(10, CURRENCIES[payCurrency].decimals))
  }, [payMode, paidInput, payCurrency, totals.total])

  const paidInInvoiceCurrency = payCurrency === currency ? paidMinor : convert(paidMinor, payCurrency, currency, rate)
  const remaining = Math.max(0, totals.total - paidInInvoiceCurrency)
  const change = Math.max(0, paidInInvoiceCurrency - totals.total)

  useEffect(() => {
    if (!editing) setPayCurrency(currency)
  }, [currency, editing])

  const addItem = (item, { qty = 1, unit, price } = {}) => {
    const unitKind = item.unit === 'kg' ? 'kg' : 'piece'
    const defaultUnit = unit || (item.unit === 'kg' ? 'kg' : (item.isBasket ? 'piece' : 'piece'))
    const last = lastPriceFor(item.id, customerId)
    let pricePerUnit
    if (price !== undefined) pricePerUnit = price
    else {
      let basePrice = priceTier === 'wholesale' ? item.wholesale : item.retail
      if (!basePrice) basePrice = item.retail || item.wholesale
      // آخر سعر لنفس الزبون
      if (last && customerId !== 'cash') {
        let lp = last.unitPrice
        if (last.currency !== currency) {
          lp = convert(lp, last.currency, currency, rate)
        }
        pricePerUnit = lp
      } else {
        pricePerUnit = item.isBasket || item.unit === 'piece'
          ? basePrice
          : Math.round((basePrice * unitFactor(defaultUnit, ozGrams)) / 1000)
      }
    }
    const qtyMilli = Math.round(qty * 1000)
    const line = {
      ...newLine(),
      itemId: item.id,
      name: item.name,
      unit: defaultUnit,
      qtyMilli,
      unitPrice: Math.round(pricePerUnit),
      isBasket: !!item.isBasket,
      costPerBase: item.isBasket
        ? item.components.reduce((sum, c) => {
          const comp = s.items.find((x) => x.id === c.itemId)
          return sum + (comp ? comp.cost * c.qtyBase : 0)
        }, 0)
        : item.cost,
    }
    line.baseQty = qtyBaseOf(line, ozGrams)
    setLines((ls) => {
      const idx = ls.findIndex((l) => l.itemId === item.id && l.unit === unit && l.unitPrice === line.unitPrice)
      if (idx >= 0) {
        const copy = [...ls]
        const merged = { ...copy[idx], qtyMilli: copy[idx].qtyMilli + qtyMilli }
        merged.baseQty = qtyBaseOf(merged, ozGrams)
        copy[idx] = merged
        return copy
      }
      return [...ls, line]
    })
    setPickerItem(null)
    setSearch('')
    onToast?.(`تمت إضافة ${item.name}`)
  }

  const updateLine = (idx, patch) => {
    setLines((ls) => ls.map((l, i) => {
      if (i !== idx) return l
      const next = { ...l, ...patch }
      next.baseQty = qtyBaseOf(next, ozGrams)
      return next
    }))
  }

  const removeLine = (idx) => setLines((ls) => ls.filter((_, i) => i !== idx))

  const submit = () => {
    if (lines.length === 0) return onToast?.('أضف صنفاً واحداً على الأقل')
    const tenders = paidMinor > 0 ? [{ currency: payCurrency, amount: paidMinor }] : []
    const inv = {
      ...(editing || {}),
      type: 'sale',
      no: editing?.no || nextNo('sale'),
      date,
      partyId: customerId,
      partyName: customer?.name || 'زبون نقدي',
      currency,
      rate,
      lines,
      discountType,
      discountValue: discountType === 'percent' ? parseAmount(discountValue) : discountMinor,
      subtotal: totals.subtotal,
      discount: totals.discount,
      total: totals.total,
      cost: totals.cost,
      profit: totals.profit,
      paid: paidInInvoiceCurrency,
      paidCurrency: payCurrency,
      paidForeign: payCurrency !== currency ? paidMinor : 0,
      paidMethod: payMode,
      tenders,
      remaining,
      change,
      status: editing?.status || 'completed',
      notes,
    }
    saveInvoice(inv)
    onToast?.(`تم حفظ الفاتورة ${inv.no}`)
    onDone ? onDone() : go('sales')
  }

  const startVoice = () => {
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition
    if (!SR) return onToast?.('المتصفح لا يدعم الإدخال الصوتي — استخدم نسخة أندرويد')
    const rec = new SR()
    rec.lang = 'ar-SY'
    rec.interimResults = false
    setListening(true)
    rec.onresult = (e) => {
      const text = e.results[0][0].transcript
      setListening(false)
      setSearch(text)
      const found = s.items.find((i) => text.includes(i.name) || i.name.includes(text))
      if (found) setPickerItem(found)
      else onToast?.(`لم أجد صنفاً مطابقاً لـ "${text}"`)
    }
    rec.onerror = () => { setListening(false); onToast?.('تعذر التعرف على الصوت') }
    rec.onend = () => setListening(false)
    rec.start()
  }

  const scanBarcode = (code) => {
    const item = s.items.find((i) => i.barcode === code.trim())
    if (item) { setPickerItem(item); setBarcodeMode(false) }
    else onToast?.('لا يوجد صنف بهذا الباركود')
  }

  return (
    <div style={{ paddingBottom: 100 }}>
      <div className="grid" style={{ gridTemplateColumns: 'minmax(0,1fr) 340px', alignItems: 'start', gap: 12 }} id="sale-grid">
        <div className="grid" style={{ gap: 12 }}>
          <Card>
            <div className="grid grid-2" style={{ gap: 10 }}>
              <Field label="الزبون">
                <select className="input" value={customerId} onChange={(e) => setCustomerId(e.target.value)}>
                  {s.customers.filter((c) => c.active).map((c) => <option key={c.id} value={c.id}>{c.name}{c.phone ? ` — ${c.phone}` : ''}</option>)}
                </select>
              </Field>
              <Field label="التاريخ">
                <input type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} />
              </Field>
              <Field label="عملة الفاتورة">
                <div className="row" style={{ gap: 6 }}>
                  {['SYP', 'USD'].map((c) => (
                    <button key={c} className={`btn btn-sm ${currency === c ? 'btn-brown' : 'btn-outline'}`} style={{ flex: 1 }} onClick={() => setCurrency(c)}>
                      {CURRENCIES[c].name}
                    </button>
                  ))}
                </div>
              </Field>
              <Field label="سعر الصرف المسجّل" hint="يُحفظ مع الفاتورة ولا يتأثر بتعديل السعر لاحقاً">
                <div className="input" style={{ background: 'var(--cream)', fontWeight: 700, direction: 'ltr', textAlign: 'right' }}>
                  {rateToNumber(rate).toLocaleString('en-US')} ل.س / $
                </div>
              </Field>
            </div>
          </Card>

          <Card title="الأصناف">
            <div className="row" style={{ gap: 6, marginBottom: 10, flexWrap: 'nowrap' }}>
              <input ref={searchRef} className="input" placeholder="ابحث بالاسم أو الباركود..." value={search} onChange={(e) => setSearch(e.target.value)} />
              <button className="btn btn-outline btn-sm" style={{ whiteSpace: 'nowrap' }} onClick={() => { setBarcodeMode(true); setTimeout(() => barcodeRef.current?.focus(), 60) }}>▮▯ باركود</button>
              <button className={`btn btn-sm ${listening ? 'btn-gold' : 'btn-outline'}`} style={{ whiteSpace: 'nowrap' }} onClick={startVoice}>{listening ? '...جاري التسجيل' : '🎙 صوت'}</button>
            </div>

            <div className="row" style={{ gap: 6, marginBottom: 10 }}>
              <span className="small muted">سعر البيع:</span>
              <button className={`btn btn-sm ${priceTier === 'retail' ? 'btn-gold' : 'btn-outline'}`} onClick={() => setPriceTier('retail')}>مفرق</button>
              <button className={`btn btn-sm ${priceTier === 'wholesale' ? 'btn-gold' : 'btn-outline'}`} onClick={() => setPriceTier('wholesale')}>جملة</button>
            </div>

            {barcodeMode && (
              <div className="row" style={{ gap: 6, marginBottom: 10 }}>
                <input ref={barcodeRef} className="input" placeholder="امسح أو أدخل الباركود ثم Enter" onKeyDown={(e) => e.key === 'Enter' && scanBarcode(e.target.value)} />
                <button className="btn btn-outline btn-sm" onClick={() => setBarcodeMode(false)}>إلغاء</button>
              </div>
            )}

            <div className="grid" style={{ gridTemplateColumns: 'repeat(auto-fill,minmax(150px,1fr))', gap: 8, maxHeight: 320, overflow: 'auto' }}>
              {filteredItems.map((i) => {
                const price = priceTier === 'wholesale' ? i.wholesale : i.retail
                return (
                  <button key={i.id} onClick={() => setPickerItem(i)}
                    style={{ textAlign: 'right', padding: 10, borderRadius: 12, border: '1px solid var(--border)', background: 'var(--card)', color: 'var(--text)' }}>
                    <div style={{ fontWeight: 700, fontSize: 13 }}>{i.name}</div>
                    <div className="small muted">{i.unit === 'kg' ? 'بالكيلو' : 'بالقطعة'} • {i.stock > 0 ? 'متوفر' : 'غير متوفر'}</div>
                    <div className="small" style={{ color: 'var(--gold-dark)', fontWeight: 800, direction: 'ltr', textAlign: 'right', marginTop: 3 }}>
                      {price ? money(price, currency) : 'سعر عند البيع'}
                    </div>
                  </button>
                )
              })}
              {filteredItems.length === 0 && <div className="small muted" style={{ padding: 12 }}>لا توجد نتائج</div>}
            </div>
          </Card>

          <Card title="بنود الفاتورة" badge={lines.length}>
            {lines.length === 0 ? (
              <div className="small muted" style={{ textAlign: 'center', padding: 20 }}>اختر صنفاً من الأعلى لإضافته إلى الفاتورة</div>
            ) : (
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr><th>الصنف</th><th>الكمية</th><th>الوحدة</th><th>السعر</th><th>خصم البند</th><th>المجموع</th><th></th></tr>
                  </thead>
                  <tbody>
                    {lines.map((l, idx) => (
                      <tr key={idx}>
                        <td style={{ fontWeight: 700 }}>{l.name}</td>
                        <td>
                          <input className="input" style={{ width: 80, padding: '6px 8px' }} inputMode="decimal"
                            value={(l.qtyMilli / 1000).toString()}
                            onChange={(e) => updateLine(idx, { qtyMilli: Math.round(parseAmount(e.target.value) * 1000) })} />
                        </td>
                        <td>
                          <select className="select" style={{ width: 90, padding: '6px 8px' }} value={l.unit}
                            onChange={(e) => updateLine(idx, { unit: e.target.value })}>
                            {s.items.find((it) => it.id === l.itemId)?.unit === 'piece' ? (
                              <option value="piece">قطعة</option>
                            ) : (
                              <>
                                <option value="kg">كيلو</option>
                                <option value="g">غرام</option>
                                <option value="oz">أوقية</option>
                              </>
                            )}
                          </select>
                        </td>
                        <td>
                          <input className="input" style={{ width: 100, padding: '6px 8px' }} inputMode="decimal"
                            value={(l.unitPrice / Math.pow(10, dec)).toString()}
                            onChange={(e) => updateLine(idx, { unitPrice: Math.round(parseAmount(e.target.value) * Math.pow(10, dec)) })} />
                        </td>
                        <td>
                          <input className="input" style={{ width: 80, padding: '6px 8px' }} inputMode="decimal" placeholder="0"
                            value={l.discount ? (l.discount / Math.pow(10, dec)).toString() : ''}
                            onChange={(e) => updateLine(idx, { discount: Math.round(parseAmount(e.target.value) * Math.pow(10, dec)) })} />
                        </td>
                        <td style={{ fontWeight: 800, direction: 'ltr' }}>{money(lineTotal(l), currency, { bare: true })}</td>
                        <td><button className="btn btn-outline btn-sm" onClick={() => removeLine(idx)}>✕</button></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </div>

        {/* الملخص */}
        <div className="grid" style={{ gap: 12, position: 'sticky', top: 70 }}>
          <Card>
            <h3 style={{ marginBottom: 10, fontSize: 14 }}>ملخص الفاتورة</h3>
            <Row label="المجموع الفرعي" value={money(totals.subtotal, currency)} />

            <div style={{ margin: '8px 0' }}>
              <div className="small" style={{ fontWeight: 700, marginBottom: 5 }}>خصم الفاتورة</div>
              <div className="row" style={{ gap: 5, flexWrap: 'nowrap' }}>
                <select className="select" style={{ width: 90, padding: '7px 8px' }} value={discountType}
                  onChange={(e) => { setDiscountType(e.target.value); setDiscountValue(0) }}>
                  <option value="none">بدون</option>
                  <option value="percent">%</option>
                  <option value="fixed">{CURRENCIES[currency].short}</option>
                </select>
                <input className="input" style={{ padding: '7px 8px' }} inputMode="decimal" disabled={discountType === 'none'}
                  value={discountValue || ''} onChange={(e) => setDiscountValue(e.target.value)} placeholder="0" />
              </div>
            </div>

            <div className="sep" />
            <div className="row" style={{ justifyContent: 'space-between' }}>
              <b style={{ fontSize: 15 }}>الإجمالي</b>
              <b style={{ fontSize: 20, color: 'var(--gold-dark)', direction: 'ltr' }}>{money(totals.total, currency)}</b>
            </div>
            {isManager && (
              <div className="row" style={{ justifyContent: 'space-between', marginTop: 6 }}>
                <span className="small muted">الربح (للمدير فقط)</span>
                <span className="small" style={{ fontWeight: 800, color: 'var(--success)', direction: 'ltr' }}>{money(totals.profit, currency)}</span>
              </div>
            )}

            <div className="sep" />
            <div className="small" style={{ fontWeight: 700, marginBottom: 6 }}>الدفع</div>
            <div className="row" style={{ gap: 5, marginBottom: 8 }}>
              {[['cash', 'نقدي'], ['debt', 'دين'], ['partial', 'دفعة جزئية']].map(([v, l]) => (
                <button key={v} className={`btn btn-sm ${payMode === v ? 'btn-brown' : 'btn-outline'}`} style={{ flex: 1 }} onClick={() => setPayMode(v)}>{l}</button>
              ))}
            </div>

            {payMode === 'partial' && (
              <div className="grid" style={{ gap: 6 }}>
                <div className="row" style={{ gap: 5, flexWrap: 'nowrap' }}>
                  <input className="input" inputMode="decimal" placeholder={`المبلغ المدفوع`} value={paidInput} onChange={(e) => setPaidInput(e.target.value)} />
                  <select className="select" style={{ width: 100, padding: '9px 6px' }} value={payCurrency} onChange={(e) => setPayCurrency(e.target.value)}>
                    <option value="SYP">ل.س</option><option value="USD">$</option>
                  </select>
                </div>
                {payCurrency !== currency && paidMinor > 0 && (
                  <div className="small muted">يُعادل {money(paidInInvoiceCurrency, currency)} بسعر الصرف المسجّل</div>
                )}
              </div>
            )}
            {payMode === 'debt' && <div className="small muted">سيُسجَّل كامل المبلغ ديناً على الزبون</div>}

            <div className="sep" />
            <Row label="المدفوع" value={money(paidInInvoiceCurrency, currency)} />
            <Row label="المتبقي" value={money(remaining, currency)} strong />
            {change > 0 && <Row label="الباقي للزبون" value={money(change, currency)} />}

            <Field label="ملاحظة" style={{ marginTop: 10 }}>
              <input className="input" value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="ملاحظة على الفاتورة (اختياري)" />
            </Field>

            <button className="btn btn-gold" style={{ width: '100%', marginTop: 12, padding: 13 }} onClick={submit}>
              💾 حفظ الفاتورة {editing ? '(تعديل)' : ''}
            </button>
            {editing && <div className="small muted" style={{ marginTop: 6, textAlign: 'center' }}>تعديل الفاتورة يُسجَّل في سجل العمليات</div>}
          </Card>

          {customerId !== 'cash' && <CustomerHint customerId={customerId} />}
        </div>
      </div>

      {pickerItem && <AddItemModal item={pickerItem} currency={currency} rate={rate} priceTier={priceTier}
        customerId={customerId} onClose={() => setPickerItem(null)} onAdd={addItem} />}

      <style>{`@media(max-width:900px){#sale-grid{grid-template-columns:1fr !important}}`}</style>
    </div>
  )
}

function Row({ label, value, strong }) {
  return (
    <div className="row" style={{ justifyContent: 'space-between', padding: '4px 0' }}>
      <span className="small" style={{ color: strong ? 'var(--text)' : 'var(--muted)', fontWeight: strong ? 800 : 500 }}>{label}</span>
      <span style={{ fontWeight: strong ? 800 : 600, direction: 'ltr', fontSize: strong ? 15 : 13 }}>{value}</span>
    </div>
  )
}

function AddItemModal({ item, currency, rate, priceTier, customerId, onClose, onAdd }) {
  const s = useStore()
  const ozGrams = s.settings.ozGrams
  const isWeight = item.unit === 'kg'
  const [unit, setUnit] = useState(isWeight ? 'kg' : 'piece')
  const [qty, setQty] = useState(1)
  const [price, setPrice] = useState('')
  const dec = CURRENCIES[currency].decimals
  const last = lastPriceFor(item.id, customerId)

  useEffect(() => {
    let p
    if (last && customerId !== 'cash') {
      p = last.currency === currency ? last.unitPrice : convert(last.unitPrice, last.currency, currency, rate)
    } else {
      const base = priceTier === 'wholesale' ? item.wholesale : item.retail
      p = isWeight ? Math.round((base * unitFactor(unit, ozGrams)) / 1000) : base
    }
    setPrice(String(p / Math.pow(10, dec)))
  }, [unit]) // eslint-disable-line

  const total = Math.round((qty * 1000 * (parseAmount(price) * Math.pow(10, dec))) / 1000)
  const stockLabel = isWeight ? `${(item.stock / 1000).toLocaleString()} كغ` : `${item.stock} قطعة`

  return (
    <Modal open onClose={onClose} title={item.name}
      footer={<>
        <button className="btn btn-gold" onClick={() => onAdd(item, { qty, unit, price: Math.round(parseAmount(price) * Math.pow(10, dec)) })}>إضافة للفاتورة</button>
        <button className="btn btn-outline" onClick={onClose}>إلغاء</button>
      </>}>
      <div className="grid" style={{ gap: 10 }}>
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <span className="small muted">المخزون المتاح</span>
          <span className="badge badge-brown">{stockLabel}</span>
        </div>
        {last && customerId !== 'cash' && (
          <div className="small" style={{ background: 'var(--cream)', padding: 8, borderRadius: 8 }}>
            💡 آخر سعر بيع لهذا الزبون: <b>{money(last.unitPrice, last.currency)}</b> ({UNITS[last.unit]?.name}) — تم اقتراحه تلقائياً
          </div>
        )}
        <div className="grid grid-2" style={{ gap: 8 }}>
          <Field label="الكمية">
            <input className="input" inputMode="decimal" value={qty} onChange={(e) => setQty(parseAmount(e.target.value))} />
          </Field>
          <Field label="الوحدة">
            <select className="select" value={unit} onChange={(e) => setUnit(e.target.value)}>
              {isWeight ? (
                <><option value="kg">كيلو</option><option value="g">غرام</option><option value="oz">أوقية ({ozGrams} غ)</option></>
              ) : (
                <><option value="piece">قطعة</option></>
              )}
            </select>
          </Field>
        </div>
        <Field label={`سعر البيع (${CURRENCIES[currency].name} لكل ${UNITS[unit]?.name})`}>
          <input className="input" inputMode="decimal" value={price} onChange={(e) => setPrice(e.target.value)} />
        </Field>
        <div className="row" style={{ gap: 6 }}>
          {['250', '500', '1000'].map((g) => isWeight && (
            <button key={g} className="btn btn-outline btn-sm" onClick={() => { setUnit('g'); setQty(Number(g)) }}>{g} غ</button>
          ))}
          <button className="btn btn-outline btn-sm" onClick={() => { setUnit(isWeight ? 'kg' : 'piece'); setQty(1) }}>{isWeight ? '1 كيلو' : '1 قطعة'}</button>
        </div>
        <div className="row" style={{ justifyContent: 'space-between', background: 'var(--cream)', borderRadius: 10, padding: 10 }}>
          <b>المجموع</b>
          <b style={{ fontSize: 18, direction: 'ltr', color: 'var(--gold-dark)' }}>{money(total, currency)}</b>
        </div>
      </div>
    </Modal>
  )
}

function CustomerHint({ customerId }) {
  const s = useStore()
  const c = s.customers.find((x) => x.id === customerId)
  const balSYP = s.invoices.filter((i) => i.type === 'sale' && i.partyId === customerId && i.currency === 'SYP' && i.status !== 'cancelled').reduce((a, b) => a + b.remaining, 0)
  const balUSD = s.invoices.filter((i) => i.type === 'sale' && i.partyId === customerId && i.currency === 'USD' && i.status !== 'cancelled').reduce((a, b) => a + b.remaining, 0)
  const paySYP = s.payments.filter((p) => p.partyId === customerId && p.currency === 'SYP' && p.partyType === 'customer').reduce((a, b) => a + b.amount, 0)
  const payUSD = s.payments.filter((p) => p.partyId === customerId && p.currency === 'USD' && p.partyType === 'customer').reduce((a, b) => a + b.amount, 0)
  return (
    <Card>
      <div className="small muted">رصيد الزبون {c?.name}</div>
      <div className="row" style={{ justifyContent: 'space-between', marginTop: 6 }}>
        <span className="small">ليرة</span>
        <b style={{ direction: 'ltr' }}>{money(balSYP - paySYP, 'SYP')}</b>
      </div>
      <div className="row" style={{ justifyContent: 'space-between' }}>
        <span className="small">دولار</span>
        <b style={{ direction: 'ltr' }}>{money(balUSD - payUSD, 'USD')}</b>
      </div>
    </Card>
  )
}
