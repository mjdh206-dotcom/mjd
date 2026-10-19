// ============ أدوات الحسابات النقدية — بدون أخطاء التقريب ============
// كل المبالغ تُخزَّن كأعداد صحيحة (minor units): الليرة = 1 ، الدولار = سنت (×100)
// سعر الصرف يُخزَّن كعدد صحيح = السعر × 100

export const CURRENCIES = {
  SYP: { code: 'SYP', name: 'ليرة سورية', symbol: 'ل.س', decimals: 0, short: 'ل.س' },
  USD: { code: 'USD', name: 'دولار أمريكي', symbol: '$', decimals: 2, short: '$' },
}

export const UNITS = {
  kg: { code: 'kg', name: 'كيلو', base: 'g', factor: 1000, kind: 'weight' },
  g: { code: 'g', name: 'غرام', base: 'g', factor: 1, kind: 'weight' },
  oz: { code: 'oz', name: 'أوقية', base: 'g', factor: 250, kind: 'weight' },
  piece: { code: 'piece', name: 'قطعة', base: 'piece', factor: 1, kind: 'count' },
  box: { code: 'box', name: 'علبة', base: 'piece', factor: 1, kind: 'count' },
}

export const CATEGORIES = [
  { id: 'nuts', name: 'مكسرات', icon: '🥜' },
  { id: 'hospitality', name: 'ضيافة', icon: '🍽️' },
  { id: 'candy', name: 'كاندي', icon: '🍬' },
  { id: 'coffee', name: 'قهوة', icon: '☕' },
]

export const ROLE = { MANAGER: 'manager', WORKER: 'worker' }

export const unitFactor = (unit, ozGrams = 250) => {
  if (unit === 'kg') return 1000
  if (unit === 'g') return 1
  if (unit === 'oz') return ozGrams
  return 1
}

export const isWeightUnit = (u) => ['kg', 'g', 'oz'].includes(u)

// ---------- تنسيق ----------
export const pow10 = (d) => Math.pow(10, d)

export function money(m, currency = 'SYP', opts = {}) {
  const c = CURRENCIES[currency] || CURRENCIES.SYP
  const v = (m || 0) / pow10(c.decimals)
  const s = v.toLocaleString('en-US', {
    minimumFractionDigits: c.decimals,
    maximumFractionDigits: c.decimals,
  })
  if (opts.bare) return s
  return opts.after ? `${s} ${c.symbol}` : `${s} ${c.symbol}`
}

export function parseAmount(str) {
  if (str === null || str === undefined) return 0
  const cleaned = String(str).replace(/[^\d.-]/g, '')
  const n = Number(cleaned)
  return Number.isFinite(n) ? n : 0
}

export function toMinor(major, currency) {
  const c = CURRENCIES[currency] || CURRENCIES.SYP
  return Math.round(Number(major || 0) * pow10(c.decimals))
}

export function toMajor(minor, currency) {
  const c = CURRENCIES[currency] || CURRENCIES.SYP
  return (minor || 0) / pow10(c.decimals)
}

// ---------- العملات ----------
// rate = سعر صرف الدولار بالليرة، مخزّن ×100
export const rateToNumber = (r) => (r || 0) / 100

export function convert(minor, from, to, rate) {
  if (from === to) return minor || 0
  const r = rateToNumber(rate)
  if (!r) return 0
  if (from === 'SYP' && to === 'USD') {
    const usdMajor = (minor || 0) / r
    return Math.round(usdMajor * 100)
  }
  if (from === 'USD' && to === 'SYP') {
    const usdMajor = (minor || 0) / 100
    return Math.round(usdMajor * r)
  }
  return minor || 0
}

// سعر وحدة واحدة بوحدة العملة (minor) — يُستخدم للإدخال
export function unitPriceFromBase(pricePerBaseMinor, unit, ozGrams = 250, baseIsKg = true) {
  if (!baseIsKg) {
    // سعر القطعة / العلبة
    if (unit === 'box') return pricePerBaseMinor
    return pricePerBaseMinor
  }
  const f = unitFactor(unit, ozGrams)
  return Math.round((pricePerBaseMinor * f) / 1000)
}

export function basePriceFromUnit(unitPriceMinor, unit, ozGrams = 250, baseIsKg = true) {
  if (!baseIsKg) return unitPriceMinor
  const f = unitFactor(unit, ozGrams)
  return Math.round((unitPriceMinor * 1000) / f)
}

// ---------- البنود والفواتير ----------
export const emptyLine = () => ({
  itemId: null,
  name: '',
  unit: 'kg',
  qtyMilli: 0,
  unitPrice: 0, // minor per unit
  discount: 0, // minor
  costPerBase: 0,
  baseQty: 0,
  isBasket: false,
})

export function lineTotal(line) {
  const gross = Math.round((line.qtyMilli * line.unitPrice) / 1000)
  return Math.max(0, gross - (line.discount || 0))
}

export function lineCost(line) {
  const base = line.baseQty || 0
  return Math.round(base * (line.costPerBase || 0))
}

export function invoiceTotals(lines, discountType = 'none', discountValue = 0) {
  const subtotal = lines.reduce((s, l) => s + lineTotal(l), 0)
  let discount = 0
  if (discountType === 'percent') discount = Math.round((subtotal * (discountValue || 0)) / 100)
  else if (discountType === 'fixed') discount = Math.round(discountValue || 0)
  discount = Math.min(discount, subtotal)
  const total = Math.max(0, subtotal - discount)
  const cost = lines.reduce((s, l) => s + lineCost(l), 0)
  return { subtotal, discount, total, cost, profit: total - cost }
}

export function qtyBaseOf(line, ozGrams = 250) {
  const f = unitFactor(line.unit, ozGrams)
  return Math.round((line.qtyMilli * f) / 1000)
}

export function fmtQty(line, ozGrams = 250) {
  const q = (line.qtyMilli || 0) / 1000
  const u = UNITS[line.unit]?.name || ''
  const s = q.toLocaleString('en-US', { maximumFractionDigits: 3 })
  return `${s} ${u}`
}

export function fmtBaseQty(baseQty, unitKind) {
  if (unitKind === 'g') {
    if (baseQty >= 1000) return `${(baseQty / 1000).toLocaleString('en-US', { maximumFractionDigits: 3 })} كغ`
    return `${baseQty} غ`
  }
  return `${baseQty} قطعة`
}

// ---------- التواريخ ----------
const localISO = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
export const todayISO = () => localISO(new Date())
export const nowISO = () => new Date().toISOString()
export const dayOffset = (n = 0) => { const d = new Date(); d.setDate(d.getDate() - n); return localISO(d) }
export const prettyDate = (iso) => {
  if (!iso) return ''
  const d = new Date(String(iso).length <= 10 ? iso + 'T00:00:00' : iso)
  return isNaN(d) ? iso : d.toLocaleDateString('ar-SY', { weekday: 'short', day: 'numeric', month: 'short' })
}

export function fmtDate(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (isNaN(d)) return iso
  return d.toLocaleDateString('ar-SY', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

export function fmtDateTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return `${d.toLocaleDateString('ar-SY')} ${d.toLocaleTimeString('ar-SY', { hour: '2-digit', minute: '2-digit' })}`
}

export function daysUntil(iso) {
  if (!iso) return null
  const d = new Date(iso + 'T00:00:00')
  const t = new Date(todayISO() + 'T00:00:00')
  return Math.round((d - t) / 86400000)
}

export function monthKey(iso) {
  const d = new Date(iso)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

export function startOfMonth(dateStr = todayISO()) {
  const d = new Date(dateStr + 'T00:00:00')
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
}

export const uid = (prefix = 'id') => `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 7)}`

export function normalizeAr(s) {
  return String(s || '')
    .replace(/[أإآٱ]/g, 'ا')
    .replace(/ى/g, 'ي')
    .replace(/ة/g, 'ه')
    .replace(/[\u064B-\u0652]/g, '')
    .trim()
    .toLowerCase()
}


// ---------- تصدير ----------
export const hasAndroidBridge = () => typeof window !== 'undefined' && !!window.AndroidApp

const blobToBase64 = (blob) => new Promise((resolve, reject) => {
  const r = new FileReader()
  r.onload = () => resolve(String(r.result).split(',')[1] || '')
  r.onerror = reject
  r.readAsDataURL(blob)
})

export async function saveBlob(blob, filename) {
  if (window.AndroidApp?.saveBase64) {
    try {
      const b64 = await blobToBase64(blob)
      window.AndroidApp.saveBase64(filename, b64, blob.type || 'application/octet-stream')
      return true
    } catch { /* fallback */ }
  }
  return false
}

export function downloadCSV(filename, rows) {
  const csv = rows.map((r) => r.map((cell) => {
    const v = cell === null || cell === undefined ? '' : String(cell)
    return /[",\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v
  }).join(',')).join('\r\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' })
  if (window.AndroidApp?.saveBase64) { saveBlob(blob, filename); return }
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(a.href), 1500)
}

export function printPage() { window.print() }

/** فتح صفحة للطباعة: عبر جسر أندرويد إن وُجد، وإلا نافذة جديدة. */
export function openPrintWindow(html) {
  if (window.AndroidApp?.printHtml) {
    window.AndroidApp.printHtml(html)
    return true
  }
  const w = window.open('', '_blank', 'width=880,height=980')
  if (!w) return false
  w.document.write(html)
  w.document.close()
  setTimeout(() => { try { w.focus(); w.print() } catch { /* ignore */ } }, 350)
  return true
}

/** فتح رابط خارجي (واتساب…) عبر جسر أندرويد إن وُجد. */
export function openExternal(url) {
  if (window.AndroidApp?.openUrl) {
    window.AndroidApp.openUrl(url)
    return
  }
  window.open(url, '_blank', 'noopener')
}
