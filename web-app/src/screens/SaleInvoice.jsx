import { useEffect, useMemo, useRef, useState } from 'react'
import { useStore, saveInvoice, saveCustomer, lastPriceFor, nextNo, rateNow, partyStats } from '../store'
import {
  CURRENCIES, UNITS, money, parseAmount, convert, unitFactor, invoiceTotals,
  qtyBaseOf, lineTotal, fmtQty, todayISO, rateToNumber, CATEGORIES, uid,
} from '../utils'
import { Card, Field, Modal, Money, Seg, Avatar, Drawer, Empty } from '../components'
import { Icon, categoryIcon } from '../icons'

const HOLDS_KEY = 'haramayn_holds_v1'
const newLine = () => ({
  itemId: null, name: '', unit: 'kg', qtyMilli: 1000, unitPrice: 0, discount: 0,
  costPerBase: 0, baseQty: 0, isBasket: false,
})

const loadHolds = () => {
  try { return JSON.parse(localStorage.getItem(HOLDS_KEY) || '[]') } catch { return [] }
}

export default function SaleInvoice({ go, onToast, editId, onDone }) {
  const s = useStore()
  const isManager = s.session?.role === 'manager'
  const editing = editId ? s.invoices.find((i) => i.id === editId) : null

  const [customerId, setCustomerId] = useState(editing?.partyId || 'cash')
  const [currency, setCurrency] = useState(editing?.currency || s.settings.defaultCurrency || 'SYP')
  const [lines, setLines] = useState(editing?.lines || [])
  const [discountType, setDiscountType] = useState(editing?.discountType || 'none')
  const [discountValue, setDiscountValue] = useState(
    editing ? (editing.discountType === 'percent' ? editing.discountValue : editing.discountValue / Math.pow(10, CURRENCIES[editing.currency].decimals)) : '',
  )
  const [payMode, setPayMode] = useState(editing?.paidMethod || 'cash')
  const [payCurrency, setPayCurrency] = useState(editing?.paidCurrency || editing?.currency || s.settings.defaultCurrency || 'SYP')
  const [paidInput, setPaidInput] = useState('')
  const [tendered, setTendered] = useState('')
  const [notes, setNotes] = useState(editing?.notes || '')
  const [date, setDate] = useState(editing?.date || todayISO())
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('all')
  const [priceTier, setPriceTier] = useState('retail')
  const [pickerItem, setPickerItem] = useState(null)
  const [lineEdit, setLineEdit] = useState(null)
  const [custOpen, setCustOpen] = useState(false)
  const [holdsOpen, setHoldsOpen] = useState(false)
  const [holds, setHolds] = useState(loadHolds)
  const [barcodeMode, setBarcodeMode] = useState(false)
  const [barcode, setBarcode] = useState('')
  const [listening, setListening] = useState(false)
  const [cartOpen, setCartOpen] = useState(false)
  const [saved, setSaved] = useState(null)
  const [discountOpen, setDiscountOpen] = useState(false)
  const barcodeRef = useRef(null)
  const searchRef = useRef(null)

  const rate = editing?.rate || s.settings.rate
  const customer = s.customers.find((c) => c.id === customerId)
  const ozGrams = s.settings.ozGrams
  const dec = CURRENCIES[currency].decimals
  const factor = Math.pow(10, dec)

  const minorDiscount = discountType === 'percent' ? 0 : Math.round(parseAmount(discountValue) * factor)
  const totals = useMemo(() => invoiceTotals(lines, discountType === 'percent' ? 'percent' : 'fixed', discountType === 'percent' ? parseAmount(discountValue) : minorDiscount),
    [lines, discountType, discountValue, minorDiscount])

  const paidMinor = useMemo(() => {
    if (payMode === 'cash') return convert(totals.total, currency, payCurrency, rate)
    if (payMode === 'debt') return 0
    return Math.round(parseAmount(paidInput) * Math.pow(10, CURRENCIES[payCurrency].decimals))
  }, [payMode, paidInput, payCurrency, totals.total, currency, rate])

  const paidInInvoiceCurrency = payCurrency === currency ? paidMinor : convert(paidMinor, payCurrency, currency, rate)
  const paidFinal = Math.min(totals.total, Math.max(0, paidInInvoiceCurrency))
  const remaining = Math.max(0, totals.total - paidFinal)
  const tenderedMinor = Math.round(parseAmount(tendered) * Math.pow(10, CURRENCIES[payCurrency].decimals))
  const changeMinor = payMode === 'cash' ? Math.max(0, tenderedMinor - paidMinor) : Math.max(0, paidInInvoiceCurrency - totals.total)

  useEffect(() => { if (!editing) setPayCurrency(currency) }, [currency, editing])
  useEffect(() => { setTendered('') }, [currency, payCurrency, payMode])
  useEffect(() => { if (barcodeMode) setTimeout(() => barcodeRef.current?.focus(), 60) }, [barcodeMode])
  useEffect(() => {
    const h = (e) => {
      if (e.key === 'F2') { e.preventDefault(); submit() }
      if (e.key === 'Escape' && cartOpen) setCartOpen(false)
    }
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  })

  const filtered = useMemo(() => {
    let list = s.items.filter((i) => i.active)
    if (category !== 'all') list = list.filter((i) => i.category === category)
    const q = search.trim()
    if (q) {
      const nq = q.toLowerCase()
      list = list.filter((i) => i.name.toLowerCase().includes(nq) || (i.barcode || '').includes(q))
    }
    return list
  }, [s.items, category, search])

  const unitPriceOf = (item, unit = item.unit, tier = priceTier) => {
    let base = tier === 'wholesale' ? (item.wholesale || item.retail) : (item.retail || item.wholesale)
    if (currency !== 'SYP') base = convert(base, 'SYP', currency, rate)
    if (item.unit === 'kg') return Math.max(0, Math.round((base * unitFactor(unit, ozGrams)) / 1000))
    return Math.max(0, Math.round(base))
  }

  const addItem = (item, { qty = 1, unit, price } = {}) => {
    const u = unit || (item.unit === 'kg' ? 'kg' : 'piece')
    let unitPrice = price
    if (unitPrice === undefined) {
      const last = lastPriceFor(item.id, customerId)
      if (last && customerId !== 'cash' && last.currency === currency && !item.isBasket) unitPrice = last.unitPrice
      else unitPrice = unitPriceOf(item, u)
    }
    const cost = item.isBasket
      ? item.components.reduce((sum, c) => {
        const comp = s.items.find((x) => x.id === c.itemId)
        return sum + (comp ? comp.cost * c.qtyBase : 0)
      }, 0)
      : item.cost
    const line = {
      ...newLine(),
      itemId: item.id, name: item.name, unit: u,
      qtyMilli: Math.round(qty * 1000),
      unitPrice: Math.round(unitPrice),
      isBasket: !!item.isBasket,
      costPerBase: currency === 'SYP' ? cost : convert(cost, 'SYP', currency, rate),
    }
    line.baseQty = qtyBaseOf(line, ozGrams)
    setLines((ls) => {
      const idx = ls.findIndex((l) => l.itemId === item.id && l.unit === u && l.unitPrice === line.unitPrice && !l.discount)
      if (idx >= 0) {
        const copy = [...ls]
        const merged = { ...copy[idx], qtyMilli: copy[idx].qtyMilli + line.qtyMilli }
        merged.baseQty = qtyBaseOf(merged, ozGrams)
        copy[idx] = merged
        return copy
      }
      return [...ls, line]
    })
    setPickerItem(null)
    setSearch('')
    onToast?.(`أُضيف ${item.name}`)
  }

  const updateLine = (idx, patch) => setLines((ls) => ls.map((l, i) => {
    if (i !== idx) return l
    const next = { ...l, ...patch }
    next.baseQty = qtyBaseOf(next, ozGrams)
    return next
  }))

  const changeUnitOnLine = (idx, unit) => setLines((ls) => ls.map((l, i) => {
    if (i !== idx) return l
    const oldQty = l.qtyMilli / 1000
    const oldGrams = oldQty * unitFactor(l.unit, ozGrams)
    const newQty = Math.max(0.001, oldGrams / unitFactor(unit, ozGrams))
    const item = s.items.find((x) => x.id === l.itemId)
    const next = { ...l, unit, qtyMilli: Math.round(newQty * 1000) }
    if (item && item.unit === 'kg') next.unitPrice = unitPriceOf(item, unit)
    next.baseQty = qtyBaseOf(next, ozGrams)
    return next
  }))

  const changeCurrency = (c) => {
    if (c === currency) return
    const conv = (v) => convert(v, currency, c, rate)
    setLines((ls) => ls.map((l) => ({ ...l, unitPrice: conv(l.unitPrice), discount: conv(l.discount || 0), costPerBase: conv(l.costPerBase || 0) })))
    setCurrency(c)
    setPayCurrency(c)
  }

  const changeCustomer = (id) => {
    setCustomerId(id)
    if (id !== 'cash') {
      const st = partyStats(id, 'customer')
      if (st.balances.SYP > 0 || st.balances.USD > 0) onToast?.('تنبيه: على هذا الزبون رصيد سابق', 'info')
    }
  }

  const submit = () => {
    if (lines.length === 0) return onToast?.('أضف صنفاً واحداً على الأقل', 'error')
    if (payMode === 'debt' && customerId === 'cash') {
      onToast?.('اختر زبوناً آجلاً لتسجيل الدين', 'error')
      setCustOpen(true)
      return
    }
    const paidInPayCurrency = payCurrency === currency ? paidFinal : convert(paidFinal, currency, payCurrency, rate)
    const tenders = paidFinal > 0 ? [{ currency: payCurrency, amount: paidInPayCurrency }] : []
    const inv = {
      ...(editing || {}),
      id: editing?.id || uid('inv'),
      type: 'sale',
      no: editing?.no || nextNo('sale'),
      date,
      partyId: customerId,
      partyName: customer?.name || 'زبون نقدي',
      currency, rate, lines,
      discountType, discountValue: discountType === 'percent' ? parseAmount(discountValue) : minorDiscount,
      subtotal: totals.subtotal, discount: totals.discount, total: totals.total,
      cost: totals.cost, profit: totals.profit,
      paid: paidFinal,
      paidCurrency: payCurrency,
      paidForeign: payCurrency !== currency ? paidMinor : 0,
      paidMethod: payMode,
      tenders,
      remaining,
      change: changeMinor,
      status: editing?.status || 'completed',
      notes,
    }
    saveInvoice(inv)
    setCartOpen(false)
    if (editing) {
      onToast?.('تم تحديث الفاتورة')
      go('invoice-view', { id: editing.id })
    } else {
      setSaved(inv)
    }
  }

  const reset = () => {
    setLines([])
    setDiscountType('none')
    setDiscountValue('')
    setNotes('')
    setPayMode('cash')
    setTendered('')
    setPaidInput('')
    setSaved(null)
    setCustomerId('cash')
    onToast?.('فاتورة جديدة جاهزة')
    searchRef.current?.focus()
  }

  const hold = () => {
    if (!lines.length) return onToast?.('لا يوجد ما يُعلَّق', 'error')
    const h = { id: `h_${Date.now()}`, at: new Date().toISOString(), customerId, customerName: customer?.name || 'زبون نقدي', currency, lines, discountType, discountValue, notes }
    const next = [h, ...holds].slice(0, 12)
    setHolds(next)
    localStorage.setItem(HOLDS_KEY, JSON.stringify(next))
    setLines([]); setDiscountType('none'); setDiscountValue(''); setNotes('')
    onToast?.('تم تعليق الفاتورة')
  }

  const resume = (h) => {
    setCustomerId(h.customerId); setCurrency(h.currency); setLines(h.lines)
    setDiscountType(h.discountType || 'none'); setDiscountValue(h.discountValue || ''); setNotes(h.notes || '')
    const next = holds.filter((x) => x.id !== h.id)
    setHolds(next); localStorage.setItem(HOLDS_KEY, JSON.stringify(next))
    setHoldsOpen(false)
    onToast?.('تم استرجاع الفاتورة المعلّقة')
  }

  const removeHold = (id) => {
    const next = holds.filter((x) => x.id !== id)
    setHolds(next); localStorage.setItem(HOLDS_KEY, JSON.stringify(next))
  }

  const scanBarcode = (code) => {
    const item = s.items.find((i) => i.barcode === String(code).trim())
    if (item) { setPickerItem(item); setBarcode(''); setBarcodeMode(false) }
    else onToast?.('لا يوجد صنف بهذا الباركود', 'error')
  }

  const startVoice = () => {
    if (window.AndroidApp?.startSpeech) {
      window.__onSpeechResult = (txt) => {
        setSearch(txt)
        const found = s.items.find((i) => txt.includes(i.name) || i.name.includes(txt))
        if (found) setPickerItem(found)
        else onToast?.('لم أجد صنفاً مطابقاً', 'error')
      }
      window.AndroidApp.startSpeech()
      return
    }
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition
    if (!SR) return onToast?.('الإدخال الصوتي غير مدعوم في هذا المتصفح', 'error')
    const rec = new SR()
    rec.lang = 'ar-SY'; rec.interimResults = false; setListening(true)
    rec.onresult = (e) => {
      const text = e.results[0][0].transcript
      setListening(false); setSearch(text)
      const found = s.items.find((i) => text.includes(i.name) || i.name.includes(text))
      if (found) setPickerItem(found)
      else onToast?.('لم أجد صنفاً مطابقاً', 'error')
    }
    rec.onerror = () => { setListening(false); onToast?.('تعذر التعرف على الصوت', 'error') }
    rec.onend = () => setListening(false)
    rec.start()
  }

  const cartBody = (mobile) => (
    <>
      <div className="cart-head">
        <div className="row" style={{ gap: 7 }}>
          <span className="badge badge-dark"><Icon name="receipt" size={11} />{editing?.no || nextNo('sale')}</span>
          <span className="tiny muted">{date}</span>
        </div>
        <div className="row" style={{ gap: 4 }}>
          {holds.length > 0 && <button className="btn btn-ghost btn-sm" onClick={() => setHoldsOpen(true)} title="الفواتير المعلّقة"><Icon name="clock" />{holds.length}</button>}
          <button className="btn btn-ghost btn-sm" onClick={hold} title="تعليق الفاتورة"><Icon name="clock" />تعليق</button>
        </div>
      </div>

      <button className="cart-cust" onClick={() => setCustOpen(true)}>
        <Avatar name={customer?.name || 'زبون'} />
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="n">{customer?.name || 'زبون نقدي'}</div>
          <div className="s">{customer?.phone || 'اضغط لاختيار أو إضافة زبون'}</div>
        </div>
        {customerId !== 'cash' && (() => {
          const st = partyStats(customerId, 'customer')
          return (st.balances.SYP > 0 || st.balances.USD > 0)
            ? <span className="badge badge-red" style={{ direction: 'ltr' }}>{money(st.balances.SYP, 'SYP', { bare: true })}</span>
            : <span className="badge badge-green">لا رصيد</span>
        })()}
        <Icon name="chevronDown" size={15} style={{ color: 'var(--muted)' }} />
      </button>

      <div className="cart-lines" style={mobile ? { maxHeight: 'none' } : undefined}>
        {lines.length === 0 ? (
          <div style={{ padding: '22px 8px', textAlign: 'center', color: 'var(--muted)' }}>
            <Icon name="cart" size={26} style={{ opacity: .45, marginBottom: 6 }} />
            <div className="tiny">الفاتورة فارغة — اختر صنفاً من القائمة</div>
          </div>
        ) : lines.map((l, idx) => (
          <div className="cart-line" key={idx}>
            <div className="l1">
              <button className="nm" style={{ background: 'none', border: 0, textAlign: 'start', padding: 0 }} onClick={() => setLineEdit(idx)}>
                {l.name}
                {l.isBasket && <span className="badge badge-gold" style={{ marginInlineStart: 5, fontSize: 8 }}>سلة</span>}
              </button>
              <button className="rm" onClick={() => setLines((ls) => ls.filter((_, i) => i !== idx))} aria-label="حذف"><Icon name="trashSmall" /></button>
            </div>
            <div className="l2">
              <div className="row" style={{ gap: 4, flexWrap: 'nowrap' }}>
                <div className="stepper">
                  <button onClick={() => updateLine(idx, { qtyMilli: Math.max(1, l.qtyMilli - (l.unit === 'kg' ? 250 : 1000)) })}><Icon name="minus" /></button>
                  <input value={(l.qtyMilli / 1000).toString()} onChange={(e) => updateLine(idx, { qtyMilli: Math.max(0, Math.round(parseAmount(e.target.value) * 1000)) })} inputMode="decimal" />
                  <button onClick={() => updateLine(idx, { qtyMilli: l.qtyMilli + (l.unit === 'kg' ? 250 : 1000) })}><Icon name="plus" /></button>
                </div>
                {s.items.find((x) => x.id === l.itemId)?.unit === 'piece' ? (
                  <span className="unit-select" style={{ display: 'grid', placeItems: 'center' }}>قطعة</span>
                ) : (
                  <select className="unit-select" value={l.unit} onChange={(e) => changeUnitOnLine(idx, e.target.value)}>
                    <option value="kg">كغ</option><option value="g">غرام</option><option value="oz">أوقية</option>
                  </select>
                )}
              </div>
              <div className="row" style={{ gap: 5 }}>
                <span className="tiny muted" style={{ direction: 'ltr' }}>{money(l.unitPrice, currency, { bare: true })} ×</span>
                <span className="line-total">{money(lineTotal(l), currency)}</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {lines.length > 0 && (
        <>
          <div className="sep" />
          <div className="totals">
            <div className="tr"><span>المجموع الفرعي</span><b>{money(totals.subtotal, currency)}</b></div>
            <div className="tr">
              <button className="btn btn-ghost btn-sm" style={{ height: 24, padding: '0 7px', fontSize: 10 }} onClick={() => setDiscountOpen(true)}>
                <Icon name="percent" size={11} />{totals.discount > 0 ? 'تعديل الخصم' : 'إضافة خصم'}
              </button>
              <b style={{ color: totals.discount > 0 ? 'var(--danger)' : undefined }}>{totals.discount > 0 ? `- ${money(totals.discount, currency)}` : '—'}</b>
            </div>
            {isManager && (
              <div className="tr"><span style={{ color: 'var(--success)' }}>الربح المتوقع</span><b style={{ color: 'var(--success)' }}>{money(totals.profit, currency)}</b></div>
            )}
            <div className="tr grand"><span>الإجمالي</span><b>{money(totals.total, currency)}</b></div>
          </div>
        </>
      )}
    </>
  )

  const payBody = (mobile) => (
    <>
      <div className="pay-tabs">
        {[['cash', 'نقدي', 'cash'], ['debt', 'دين', 'clock'], ['partial', 'جزئي', 'card']].map(([v, lbl, ic]) => (
          <button key={v} className={payMode === v ? 'on' : ''} onClick={() => setPayMode(v)}><Icon name={ic} />{lbl}</button>
        ))}
      </div>

      {payMode === 'cash' && (
        <div style={{ marginTop: 8 }}>
          <div className="row nowrap" style={{ gap: 6 }}>
            <Field label="المستلم من الزبون" style={{ flex: 1 }}>
              <div style={{ position: 'relative' }}>
                <input className="input" inputMode="decimal" placeholder={(totals.total / factor).toString()} value={tendered} onChange={(e) => setTendered(e.target.value)} style={{ height: 36, paddingInlineEnd: 36, fontSize: 12 }} />
                <span className="money-suffix" style={{ fontSize: 9 }}>{CURRENCIES[payCurrency].short}</span>
              </div>
            </Field>
            <Field label="العملة" style={{ width: 78 }}>
              <select className="select" value={payCurrency} onChange={(e) => setPayCurrency(e.target.value)} style={{ height: 36, padding: '0 8px', fontSize: 10.5 }}>
                <option value="SYP">ل.س</option><option value="USD">$</option>
              </select>
            </Field>
          </div>
          <div className="quick-cash">
            {payCurrency === 'SYP' ? [5000, 10000, 25000, 50000, 100000].map((v) => <button key={v} onClick={() => setTendered(String(v))}>{v.toLocaleString('en-US')}</button>) : [1, 5, 10, 20, 50].map((v) => <button key={v} onClick={() => setTendered(String(v))}>${v}</button>)}
          </div>
          {tendered !== '' && (
            <div className="row between" style={{ marginTop: 7, fontSize: 11.5 }}>
              <span className="muted">الباقي للزبون</span>
              <b style={{ direction: 'ltr', color: changeMinor > 0 ? 'var(--success)' : 'var(--muted)' }}>{money(changeMinor, payCurrency)}</b>
            </div>
          )}
        </div>
      )}

      {payMode === 'partial' && (
        <div style={{ marginTop: 8 }}>
          <div className="row nowrap" style={{ gap: 6 }}>
            <Field label="المبلغ المدفوع" style={{ flex: 1 }}>
              <div style={{ position: 'relative' }}>
                <input className="input" inputMode="decimal" placeholder="0" value={paidInput} onChange={(e) => setPaidInput(e.target.value)} style={{ height: 36, paddingInlineEnd: 36, fontSize: 12 }} />
                <span className="money-suffix" style={{ fontSize: 9 }}>{CURRENCIES[payCurrency].short}</span>
              </div>
            </Field>
            <Field label="العملة" style={{ width: 78 }}>
              <select className="select" value={payCurrency} onChange={(e) => setPayCurrency(e.target.value)} style={{ height: 36, padding: '0 8px', fontSize: 10.5 }}>
                <option value="SYP">ل.س</option><option value="USD">$</option>
              </select>
            </Field>
          </div>
          {payCurrency !== currency && paidMinor > 0 && <div className="tiny muted" style={{ marginTop: 4 }}>يعادل {money(paidInInvoiceCurrency, currency)} بسعر الصرف المسجّل</div>}
          <div className="quick-cash">
            {[25, 50, 75].map((p) => <button key={p} onClick={() => setPaidInput(String((totals.total * p / 100 / factor).toFixed(dec)))}>{p}%</button>)}
            <button onClick={() => setPaidInput(String(totals.total / factor))}>كامل المبلغ</button>
          </div>
        </div>
      )}

      {payMode === 'debt' && <div className="tiny muted" style={{ marginTop: 8, padding: 8, background: 'var(--warn-soft)', borderRadius: 8 }}>سيُسجَّل كامل المبلغ ({money(totals.total, currency)}) ديناً على الزبون.</div>}

      <div className="sep" />
      <div className="kv"><span>المدفوع</span><b>{money(payMode === 'cash' ? totals.total : paidFinal, currency)}</b></div>
      <div className="kv"><span>المتبقي</span><b style={{ color: remaining > 0 ? 'var(--danger)' : 'var(--success)' }}>{money(remaining, currency)}</b></div>
      {payMode === 'cash' && <div className="tiny muted" style={{ marginTop: 6, textAlign: 'center' }}>سعر الصرف المسجّل: {rateToNumber(rate).toLocaleString('en-US')} ل.س / $</div>}

      <button className="btn btn-gold btn-lg btn-block" style={{ marginTop: 10 }} onClick={submit} disabled={!lines.length}>
        <Icon name="checkCircle" />حفظ الفاتورة {editing ? '(تعديل)' : ''}
      </button>
      <div className="row" style={{ justifyContent: 'center', gap: 4, marginTop: 6 }}>
        <span className="tiny muted">اضغط F2 للحفظ السريع</span>
      </div>
      {mobile && <div style={{ height: 20 }} />}
    </>
  )

  return (
    <div style={{ paddingBottom: 90 }}>
      <div className="pos">
        <div className="grid" style={{ gap: 12 }}>
          {/* شريط الأدوات */}
          <Card tight>
            <div className="pos-searchbar">
              <div className="field-icon" style={{ flex: 1 }}>
                <Icon name="search" />
                <input ref={searchRef} className="input" placeholder="ابحث عن صنف بالاسم أو الباركود…" value={search} onChange={(e) => setSearch(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter' && filtered[0]) { e.preventDefault(); setPickerItem(filtered[0]) } }} />
              </div>
              <button className={`btn ${barcodeMode ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setBarcodeMode((v) => !v)} title="مسح باركود"><Icon name="scan" /><span className="hide-mobile">باركود</span></button>
              <button className={`btn ${listening ? 'btn-gold' : 'btn-ghost'}`} onClick={startVoice} title="إدخال صوتي"><Icon name="mic" /><span className="hide-mobile">{listening ? '...' : 'صوت'}</span></button>
            </div>
            {barcodeMode && (
              <div className="row" style={{ marginTop: 8, gap: 6 }}>
                <input ref={barcodeRef} className="input" placeholder="امسح الباركود أو اكتبه ثم Enter" value={barcode}
                  onChange={(e) => setBarcode(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && scanBarcode(e.target.value)}
                  style={{ direction: 'ltr', textAlign: 'right' }} />
                <button className="btn btn-subtle" onClick={() => scanBarcode(barcode)}>إضافة</button>
              </div>
            )}
            <div className="row between" style={{ marginTop: 10, gap: 8 }}>
              <div className="row" style={{ gap: 6 }}>
                <span className="tiny muted">السعر:</span>
                <Seg value={priceTier} onChange={setPriceTier} options={[{ id: 'retail', label: 'مفرق' }, { id: 'wholesale', label: 'جملة' }]} size="sm" />
                <span className="vsep" />
                <span className="tiny muted">العملة:</span>
                <Seg value={currency} onChange={changeCurrency} options={[{ id: 'SYP', label: 'ل.س' }, { id: 'USD', label: '$' }]} size="sm" gold />
              </div>
              <div className="row" style={{ gap: 6 }}>
                <input type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} style={{ width: 140, height: 32, fontSize: 11 }} />
                <button className="btn btn-ghost btn-sm" onClick={() => setHoldsOpen(true)}><Icon name="clock" />المعلّقة {holds.length ? `(${holds.length})` : ''}</button>
              </div>
            </div>
          </Card>

          {/* التصنيفات */}
          <div className="pos-cats">
            <button className={`cat-pill ${category === 'all' ? 'on' : ''}`} onClick={() => setCategory('all')}><Icon name="grid" />الكل</button>
            {CATEGORIES.map((c) => (
              <button key={c.id} className={`cat-pill ${category === c.id ? 'on' : ''}`} onClick={() => setCategory(c.id)}>
                <Icon name={categoryIcon(c.id)} />{c.name}
              </button>
            ))}
          </div>

          {/* المنتجات */}
          <Card tight style={{ background: 'transparent', border: 0, boxShadow: 'none', padding: 0 }}>
            {filtered.length === 0 ? <Empty icon="search" title="لا توجد أصناف مطابقة" text="جرّب كلمة أخرى أو غيّر التصنيف." /> : (
              <div className="prod-grid">
                {filtered.map((i) => {
                  const price = priceTier === 'wholesale' ? (i.wholesale || i.retail) : i.retail
                  const isWeight = i.unit === 'kg'
                  const qty = isWeight ? i.stock / 1000 : i.stock
                  const low = i.minStock > 0 && i.stock <= i.minStock
                  const out = i.stock <= 0
                  return (
                    <button key={i.id} className="prod-card" onClick={() => setPickerItem(i)}>
                      {i.isBasket && <span className="basket-tag">سلة</span>}
                      <span className="cat-ic"><Icon name={categoryIcon(i.category)} /></span>
                      <span className="name">{i.name}</span>
                      <div className="meta">
                        <span className="price">
                          {money(convert(price, 'SYP', currency, rate) * (isWeight ? unitFactor('kg', ozGrams) / 1000 : 1), currency, { bare: true })}
                          <small> /{isWeight ? 'كغ' : 'قطعة'}</small>
                        </span>
                        <span className={`stock-badge ${out ? 'out' : low ? 'low' : ''}`}>{out ? 'نفد' : `${qty.toLocaleString('en-US', { maximumFractionDigits: 2 })}`}</span>
                      </div>
                    </button>
                  )
                })}
              </div>
            )}
          </Card>

          <div className="tiny muted center" style={{ paddingBottom: 8 }}>
            {filtered.length} صنف معروض · الأسعار بالعملة المختارة · المخزون بالوحدة الأساسية
          </div>
        </div>

        {/* السلة */}
        <div className="pos-cart">
          <Card tight>{cartBody(false)}</Card>
          {lines.length > 0 && <Card tight>{payBody(false)}</Card>}
          {lines.length === 0 && (
            <Card tight>
              <div className="tiny muted" style={{ lineHeight: 1.9 }}>
                <b style={{ color: 'var(--ink)' }}>نصيحة سريعة:</b> امسح الباركود مباشرة من شاشة البحث، أو استخدم <span className="kbd">F2</span> لحفظ الفاتورة. يمكنك تعليق أكثر من فاتورة والعودة إليها لاحقاً.
              </div>
            </Card>
          )}
        </div>
      </div>

      {/* شريط الجوال */}
      {lines.length > 0 && (
        <div className="pos-mobile-bar no-print">
          <div className="t"><b>{lines.length} صنف · {totals.discount > 0 ? 'خصم مفعّل' : 'بدون خصم'}</b><span>{money(totals.total, currency)}</span></div>
          <button className="btn btn-gold" onClick={() => setCartOpen(true)}><Icon name="cart" />عرض السلة</button>
        </div>
      )}

      {pickerItem && <AddItemModal item={pickerItem} currency={currency} rate={rate} priceTier={priceTier} customerId={customerId} unitPriceOf={unitPriceOf} onClose={() => setPickerItem(null)} onAdd={addItem} />}
      {lineEdit !== null && lines[lineEdit] && (
        <LineEditor line={lines[lineEdit]} currency={currency} idx={lineEdit} onClose={() => setLineEdit(null)}
          onSave={(patch) => { updateLine(lineEdit, patch); setLineEdit(null) }}
          onRemove={() => { setLines((ls) => ls.filter((_, i) => i !== lineEdit)); setLineEdit(null) }} />
      )}
      {custOpen && <CustomerPicker current={customerId} onClose={() => setCustOpen(false)} onPick={(id) => { changeCustomer(id); setCustOpen(false) }} notify={onToast} />}
      {holdsOpen && <HoldsModal holds={holds} onClose={() => setHoldsOpen(false)} onResume={resume} onRemove={removeHold} />}
      {discountOpen && (
        <Modal open onClose={() => setDiscountOpen(false)} title="خصم الفاتورة" subtitle="يمكنك الخصم كنسبة مئوية أو مبلغ ثابت" icon="percent" size="sm"
          footer={<>
            <button className="btn btn-gold" onClick={() => setDiscountOpen(false)}>تطبيق</button>
            <button className="btn btn-ghost" onClick={() => { setDiscountType('none'); setDiscountValue('') }}>إزالة الخصم</button>
          </>}>
          <div className="stack">
            <Seg block value={discountType === 'percent' ? 'percent' : 'fixed'} onChange={(v) => { setDiscountType(v); setDiscountValue('') }}
              options={[{ id: 'fixed', label: 'مبلغ ثابت' }, { id: 'percent', label: 'نسبة %' }]} />
            <Field label={discountType === 'percent' ? 'نسبة الخصم %' : `قيمة الخصم (${CURRENCIES[currency].name})`}>
              <input className="input" inputMode="decimal" value={discountValue} onChange={(e) => setDiscountValue(e.target.value)} autoFocus placeholder="0" />
            </Field>
            <div className="row between" style={{ background: 'var(--surface-2)', padding: 10, borderRadius: 10 }}>
              <span className="small muted">الخصم المحتسب</span>
              <b style={{ direction: 'ltr' }}>{money(totals.discount, currency)}</b>
            </div>
          </div>
        </Modal>
      )}

      {cartOpen && (
        <Drawer open onClose={() => setCartOpen(false)} title="سلة الفاتورة" subtitle={`${lines.length} صنف — ${money(totals.total, currency)}`} icon="cart" width={440}>
          {cartBody(true)}
          <div className="sep" />
          {payBody(true)}
        </Drawer>
      )}

      {saved && (
        <Modal open onClose={() => setSaved(null)} title="تم حفظ الفاتورة" subtitle={`${saved.no} — ${money(saved.total, saved.currency)}`} icon="checkCircle" size="sm"
          footer={<>
            <button className="btn btn-gold" onClick={() => { setSaved(null); reset() }}><Icon name="plus" />فاتورة جديدة</button>
            <button className="btn btn-ghost" onClick={() => go('invoice-view', { id: saved.id })}><Icon name="receipt" />عرض الفاتورة</button>
          </>}>
          <div className="center" style={{ padding: '8px 0' }}>
            <div style={{ width: 64, height: 64, borderRadius: 20, background: 'var(--success-soft)', color: 'var(--success)', display: 'grid', placeItems: 'center', margin: '0 auto 12px' }}>
              <Icon name="checkCircle" size={32} />
            </div>
            <div style={{ fontSize: 13, fontWeight: 700 }}>تمت العملية بنجاح</div>
            <div className="small muted" style={{ marginTop: 4 }}>
              {saved.partyName} · {saved.lines.length} بند · {saved.paidMethod === 'cash' ? 'مدفوعة نقداً' : saved.paidMethod === 'debt' ? 'دين' : 'دفعة جزئية'}
            </div>
            {saved.remaining > 0 && <div className="badge badge-red" style={{ marginTop: 8 }}>المتبقي {money(saved.remaining, saved.currency)}</div>}
          </div>
        </Modal>
      )}
    </div>
  )
}

/* ==================== إضافة صنف ==================== */
function AddItemModal({ item, currency, rate, priceTier, customerId, unitPriceOf, onClose, onAdd }) {
  const s = useStore()
  const ozGrams = s.settings.ozGrams
  const isWeight = item.unit === 'kg'
  const [unit, setUnit] = useState(isWeight ? 'kg' : 'piece')
  const [qty, setQty] = useState('1')
  const [price, setPrice] = useState(() => String(unitPriceOf(item, isWeight ? 'kg' : 'piece') / Math.pow(10, CURRENCIES[currency].decimals)))
  const dec = CURRENCIES[currency].decimals
  const last = lastPriceFor(item.id, customerId)
  const lastSame = last && last.currency === currency

  useEffect(() => {
    setPrice(String(unitPriceOf(item, unit) / Math.pow(10, dec)))
  }, [unit]) // eslint-disable-line

  const qtyN = parseAmount(qty)
  const priceMinor = Math.round(parseAmount(price) * Math.pow(10, dec))
  const total = Math.round((qtyN * 1000 * priceMinor) / 1000)
  const stockLabel = isWeight ? `${(item.stock / 1000).toLocaleString('en-US')} كغ` : `${item.stock} قطعة`

  return (
    <Modal open onClose={onClose} title={item.name} subtitle={`${CATEGORIES.find((c) => c.id === item.category)?.name || ''} · المتوفر: ${stockLabel}`} icon={categoryIcon(item.category)}
      footer={<>
        <button className="btn btn-gold" onClick={() => onAdd(item, { qty: qtyN, unit, price: priceMinor })} disabled={qtyN <= 0}><Icon name="plus" />إضافة للفاتورة</button>
        <button className="btn btn-ghost" onClick={onClose}>إلغاء</button>
      </>}>
      <div className="stack">
        {lastSame && customerId !== 'cash' && (
          <div className="row" style={{ background: 'var(--gold-3)', borderRadius: 10, padding: '8px 10px', gap: 7 }}>
            <Icon name="sparkles" size={15} style={{ color: 'var(--gold-deep)' }} />
            <span className="tiny" style={{ fontWeight: 700 }}>آخر سعر لهذا الزبون: {money(last.unitPrice, last.currency)} ({UNITS[last.unit]?.name})</span>
          </div>
        )}
        <div className="grid g2" style={{ gap: 10 }}>
          <Field label="الكمية">
            <div className="row nowrap" style={{ gap: 5 }}>
              <button className="btn btn-subtle btn-icon" onClick={() => setQty(String(Math.max(0, qtyN - (unit === 'kg' ? 0.25 : 1))))}><Icon name="minus" /></button>
              <input className="input" inputMode="decimal" value={qty} onChange={(e) => setQty(e.target.value)} style={{ textAlign: 'center', fontWeight: 800, fontSize: 16 }} autoFocus />
              <button className="btn btn-subtle btn-icon" onClick={() => setQty(String(qtyN + (unit === 'kg' ? 0.25 : 1)))}><Icon name="plus" /></button>
            </div>
          </Field>
          <Field label="الوحدة">
            <select className="select" value={unit} onChange={(e) => setUnit(e.target.value)}>
              {isWeight ? (<><option value="kg">كيلوغرام</option><option value="g">غرام</option><option value="oz">أوقية ({ozGrams} غ)</option></>) : (<option value="piece">قطعة</option>)}
            </select>
          </Field>
        </div>
        {isWeight && (
          <div className="row" style={{ gap: 5 }}>
            {['250', '500', '1000'].map((g) => <button key={g} className="chip click" onClick={() => { setUnit('g'); setQty(g) }}>{g} غ</button>)}
            <button className="chip click" onClick={() => { setUnit('kg'); setQty('1') }}>1 كغ</button>
            <button className="chip click" onClick={() => { setUnit('oz'); setQty('1') }}>أوقية</button>
          </div>
        )}
        <Field label={`سعر البيع (${CURRENCIES[currency].short} / ${UNITS[unit]?.name})`}>
          <div style={{ position: 'relative' }}>
            <input className="input" inputMode="decimal" value={price} onChange={(e) => setPrice(e.target.value)} style={{ fontSize: 17, fontWeight: 800, height: 46, paddingInlineEnd: 48, direction: 'ltr', textAlign: 'right' }} />
            <span className="money-suffix">{CURRENCIES[currency].short}</span>
          </div>
        </Field>
        <div className="row between" style={{ background: 'linear-gradient(150deg,rgba(227,196,124,.2),rgba(227,196,124,.05))', borderRadius: 11, padding: 12 }}>
          <div>
            <div className="tiny muted">المجموع</div>
            <div className="tiny muted">{(qtyN || 0).toLocaleString('en-US')} × {UNITS[unit]?.name}</div>
          </div>
          <b style={{ fontSize: 20, direction: 'ltr', color: 'var(--gold-deep)' }}>{money(total, currency)}</b>
        </div>
        {item.isBasket && <div className="tiny muted" style={{ lineHeight: 1.8 }}>سلة ضيافة: تُخصم مكوّناتها من المخزون تلقائياً عند البيع.</div>}
      </div>
    </Modal>
  )
}

/* ==================== تعديل بند ==================== */
function LineEditor({ line, currency, onClose, onSave, onRemove }) {
  const [qty, setQty] = useState(String(line.qtyMilli / 1000))
  const [price, setPrice] = useState(String(line.unitPrice / Math.pow(10, CURRENCIES[currency].decimals)))
  const [discount, setDiscount] = useState(line.discount ? String(line.discount / Math.pow(10, CURRENCIES[currency].decimals)) : '')
  const dec = CURRENCIES[currency].decimals
  const total = Math.max(0, Math.round(parseAmount(qty) * 1000 * parseAmount(price) * Math.pow(10, dec) / 1000) - Math.round(parseAmount(discount) * Math.pow(10, dec)))
  return (
    <Modal open onClose={onClose} title="تعديل البند" subtitle={line.name} icon="edit" size="sm"
      footer={<>
        <button className="btn btn-gold" onClick={() => onSave({ qtyMilli: Math.round(parseAmount(qty) * 1000), unitPrice: Math.round(parseAmount(price) * Math.pow(10, dec)), discount: Math.round(parseAmount(discount) * Math.pow(10, dec)) })}>حفظ</button>
        <button className="btn btn-danger" onClick={onRemove}><Icon name="trash" />حذف</button>
      </>}>
      <div className="grid g3" style={{ gap: 8, alignItems: 'end' }}>
        <Field label="الكمية"><input className="input" inputMode="decimal" value={qty} onChange={(e) => setQty(e.target.value)} /></Field>
        <Field label={`السعر (${CURRENCIES[currency].short})`}><input className="input" inputMode="decimal" value={price} onChange={(e) => setPrice(e.target.value)} /></Field>
        <Field label="خصم البند"><input className="input" inputMode="decimal" placeholder="0" value={discount} onChange={(e) => setDiscount(e.target.value)} /></Field>
      </div>
      <div className="row between" style={{ marginTop: 12, background: 'var(--surface-2)', padding: 12, borderRadius: 10 }}>
        <span className="small muted">المجموع</span><b style={{ direction: 'ltr', fontSize: 16 }}>{money(total, currency)}</b>
      </div>
    </Modal>
  )
}

/* ==================== اختيار الزبون ==================== */
function CustomerPicker({ current, onClose, onPick, notify }) {
  const s = useStore()
  const [q, setQ] = useState('')
  const [adding, setAdding] = useState(false)
  const [form, setForm] = useState({ name: '', phone: '', type: 'credit' })
  const list = s.customers.filter((c) => c.active && (!q || c.name.includes(q) || (c.phone || '').includes(q)))

  return (
    <Modal open onClose={onClose} title="اختيار الزبون" subtitle="زبون نقدي أو آجل أو إضافة زبون جديد" icon="users"
      footer={<>
        <button className="btn btn-ghost" onClick={onClose}>إغلاق</button>
        <button className="btn btn-gold" onClick={() => setAdding((v) => !v)}><Icon name="plus" />زبون جديد</button>
      </>}>
      <div className="stack">
        <div className="field-icon">
          <Icon name="search" />
          <input className="input" placeholder="ابحث بالاسم أو الهاتف…" value={q} onChange={(e) => setQ(e.target.value)} autoFocus />
        </div>
        {adding && (
          <div className="card tight" style={{ background: 'var(--surface-2)' }}>
            <div className="grid g2" style={{ gap: 8 }}>
              <Field label="الاسم"><input className="input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
              <Field label="الهاتف"><input className="input" style={{ direction: 'ltr' }} value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></Field>
            </div>
            <div className="row" style={{ marginTop: 8, gap: 6 }}>
              <button className="btn btn-gold btn-sm" onClick={() => {
                if (!form.name.trim()) return notify?.('أدخل اسم الزبون', 'error')
                saveCustomer({ ...form, notes: '', active: true })
                notify?.('تمت إضافة الزبون')
                setAdding(false)
                setForm({ name: '', phone: '', type: 'credit' })
              }}>إضافة</button>
              <button className="btn btn-ghost btn-sm" onClick={() => setAdding(false)}>إلغاء</button>
            </div>
          </div>
        )}
        <div style={{ maxHeight: 340, overflowY: 'auto' }}>
          {list.map((c) => {
            const st = partyStats(c.id, 'customer')
            return (
              <button key={c.id} className="list-item" style={{ width: '100%', border: 0, background: current === c.id ? 'var(--gold-3)' : 'transparent', textAlign: 'start', padding: '9px 8px', borderRadius: 8 }} onClick={() => onPick(c.id)}>
                <Avatar name={c.name} size="sm" />
                <div className="txt">
                  <div className="t1">{c.name}</div>
                  <div className="t2">{c.phone || (c.type === 'cash' ? 'بيع نقدي فوري' : 'زبون آجل')}</div>
                </div>
                {(st.balances.SYP > 0 || st.balances.USD > 0) ? (
                  <span className="badge badge-red" style={{ direction: 'ltr' }}>{money(st.balances.SYP, 'SYP', { bare: true })}</span>
                ) : current === c.id ? <Icon name="checkCircle" size={18} style={{ color: 'var(--gold-deep)' }} /> : null}
              </button>
            )
          })}
        </div>
      </div>
    </Modal>
  )
}

/* ==================== الفواتير المعلّقة ==================== */
function HoldsModal({ holds, onClose, onResume, onRemove }) {
  return (
    <Modal open onClose={onClose} title="الفواتير المعلّقة" subtitle="استرجع أي فاتورة لم تُحفظ بعد" icon="clock">
      {holds.length === 0 ? <Empty icon="clock" title="لا فواتير معلّقة" text="استخدم زر «تعليق» داخل السلة لحفظ الفاتورة مؤقتاً." /> : (
        <div className="stack">
          {holds.map((h) => (
            <div key={h.id} className="card tight" style={{ background: 'var(--surface-2)' }}>
              <div className="row between">
                <div>
                  <div style={{ fontSize: 12.5, fontWeight: 800 }}>{h.customerName}</div>
                  <div className="tiny muted">{new Date(h.at).toLocaleString('ar-SY')} · {h.lines.length} صنف · {money(h.lines.reduce((a, l) => a + lineTotal(l), 0), h.currency)}</div>
                </div>
                <div className="row" style={{ gap: 5 }}>
                  <button className="btn btn-gold btn-sm" onClick={() => onResume(h)}>استرجاع</button>
                  <button className="btn btn-ghost btn-sm" onClick={() => onRemove(h.id)}><Icon name="trash" /></button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </Modal>
  )
}
