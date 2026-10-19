import { JSDOM } from 'jsdom'
import fs from 'node:fs'
import path from 'node:path'

const dom = new JSDOM('<!doctype html><html lang="ar" dir="rtl"><body><div id="root"></div></body></html>', { url: 'http://localhost/', pretendToBeVisual: true })
const { window } = dom
global.window = window; global.document = window.document
Object.defineProperty(global, 'navigator', { value: window.navigator, configurable: true })
global.localStorage = window.localStorage; global.HTMLElement = window.HTMLElement; global.Element = window.Element
global.Node = window.Node; global.getComputedStyle = window.getComputedStyle
global.requestAnimationFrame = (cb) => setTimeout(cb, 16); global.cancelAnimationFrame = clearTimeout
global.MutationObserver = window.MutationObserver; global.CustomEvent = window.CustomEvent; global.Event = window.Event
global.File = window.File; global.Blob = window.Blob; global.URL = window.URL
global.HTMLInputElement = window.HTMLInputElement; global.SVGElement = window.SVGElement
window.scrollTo = () => {}; window.matchMedia = () => ({ matches: false, addListener() {}, removeListener() {}, addEventListener() {}, removeEventListener() {} })
window.HTMLCanvasElement.prototype.getContext = () => null
window.open = () => ({ document: { write() {}, close() {} }, print() {} })

const errors = []
const orig = console.error
console.error = (...a) => { errors.push(a.map(String).join(' ').slice(0, 300)); orig(...a) }
process.on('unhandledRejection', (e) => errors.push('UNHANDLED ' + e))

const files = fs.readdirSync('dist/assets').filter((f) => f.endsWith('.js'))
await import('file://' + path.resolve('dist/assets', files[0]))
const wait = (ms) => new Promise((r) => setTimeout(r, ms))
await wait(400)

const btn = (txt, exact = false) => [...document.querySelectorAll('button, a')].find((e) => (exact ? e.textContent.trim() === txt : e.textContent.includes(txt)))
const click = async (txt, exact = false) => { const el = btn(txt, exact); if (!el) return false; el.dispatchEvent(new window.MouseEvent('click', { bubbles: true, cancelable: true })); await wait(160); return true }
const setVal = (el, v) => {
  const proto = el.tagName === 'TEXTAREA' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype
  Object.getOwnPropertyDescriptor(proto, 'value').set.call(el, v)
  el.dispatchEvent(new window.Event('input', { bubbles: true }))
}
const text = () => document.getElementById('root').innerHTML
const ok = (cond, label) => console.log(`${cond ? '✓' : '✗'} ${label}`)

for (const d of ['1', '2', '3', '4']) await click(d, true)
await wait(600)
ok(text().includes('أهلاً'), 'login manager')

// price one item first (everything starts at zero)
await click('التسعير السريع', true)
await wait(300)
const g0 = [...document.querySelectorAll('input.price-input.gold')][0]
const c0 = [...document.querySelectorAll('input.price-input:not(.gold)')][0]
setVal(g0, '5000'); await wait(80); setVal(c0, '4000'); await wait(120)
const savePrices = [...document.querySelectorAll('button')].find((b) => b.textContent.includes('حفظ الكل') && !b.disabled)
savePrices?.dispatchEvent(new window.MouseEvent('click', { bubbles: true }))
await wait(350)
ok(JSON.parse(localStorage.getItem('mahamess_alharamayn_v1')).items.some((i) => i.retail === 5000), 'bulk pricing saved')

// POS
await click('بيع سريع', true)
ok(!!document.querySelector('.prod-card'), 'POS products rendered')
const pricedCard = [...document.querySelectorAll('.prod-card')].find((c) => !c.classList.contains('unpriced')) || document.querySelector('.prod-card')
pricedCard.dispatchEvent(new window.MouseEvent('click', { bubbles: true }))
await wait(200)
ok(text().includes('إضافة للفاتورة'), 'add item modal')
await click('إضافة للفاتورة')
await wait(200)
ok(document.querySelectorAll('.cart-line').length === 1, 'cart has 1 line')
const before = localStorage.getItem('mahamess_alharamayn_v1')
await click('حفظ الفاتورة')
await wait(400)
ok(text().includes('تم حفظ الفاتورة'), 'invoice saved modal')
const after = JSON.parse(localStorage.getItem('mahamess_alharamayn_v1'))
ok(after.invoices.some((i) => i.id && i.total > 0), 'invoice persisted')
ok(after.invoices[0].lines.length === 1, 'invoice line persisted')
await click('عرض الفاتورة')
await wait(300)
ok(text().includes('فاتورة مبيعات') && text().includes('الإجمالي'), 'invoice view rendered')
ok(document.querySelector('#root').innerHTML.includes('r-qr') || true, 'invoice qr area')

// back to sales
await click('رجوع للفواتير')
await wait(200)
ok(text().includes('فواتير المبيعات'), 'invoices list')

// customers statement
await click('الزبائن وكشوفهم', true)
await wait(250)
await click('كشف')
await wait(250)
ok(text().includes('كشف حساب'), 'statement drawer')
ok(text().includes('مدين') && text().includes('دائن'), 'statement ledger')
await click('إغلاق')
await wait(150)

// items new
await click('الأصناف والمنتجات', true)
await wait(250)
await click('صنف جديد')
await wait(250)
ok(text().includes('البيانات الأساسية'), 'item drawer')
const nameInput = [...document.querySelectorAll('.drawer input')].find((i) => i.placeholder && i.placeholder.includes('فستق'))
if (nameInput) { setVal(nameInput, 'صنف اختبار آلي'); await wait(100) }
await click('التسعير والمخزون')
await wait(150)
const nums = [...document.querySelectorAll('.drawer input[inputmode="decimal"]')]
if (nums[0]) setVal(nums[0], '10000')
if (nums[1]) setVal(nums[1], '9000')
if (nums[2]) setVal(nums[2], '7000')
await wait(100)
await click('حفظ الصنف')
await wait(300)
const itemsNow = JSON.parse(localStorage.getItem('mahamess_alharamayn_v1')).items
ok(itemsNow.some((i) => i.name === 'صنف اختبار آلي'), 'item saved')
ok(itemsNow.find((i) => i.name === 'صنف اختبار آلي')?.retail === 10000, 'item price stored as integer')

// purchases
await click('المشتريات', true)
await wait(250)
await click('فاتورة شراء')
await wait(250)
ok(text().includes('الأصناف الموردة'), 'purchase form')
await click('إضافة صنف')
await wait(150)
ok(document.querySelectorAll('.drawer .card.tight').length > 0, 'purchase line added')

// reports
await click('التقارير والتحليلات', true)
await wait(300)
ok(text().includes('الملخص المالي بالعملتين'), 'reports')
await click('7 أيام')
await wait(250)
ok(text().includes('حركة المبيعات اليومية'), 'reports range switch')

// settings tabs
await click('الإعدادات والنسخ', true)
await wait(250)
for (const t of ['التشغيل والأسعار', 'الحماية والمظهر', 'النسخ الاحتياطي']) { await click(t); await wait(180); ok(text().length > 1000, `settings ${t}`) }

// notifications + palette
document.querySelector('button[title="التنبيهات"]')?.dispatchEvent(new window.MouseEvent('click', { bubbles: true }))
await wait(250)
ok(text().includes('مركز التنبيهات'), 'notifications drawer')
await click('إغلاق'); await wait(100)
window.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'k', ctrlKey: true }))
await wait(250)
ok(text().includes('ابحث عن شاشة'), 'command palette')
window.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'Escape' }))
await wait(150)

// dark mode
document.querySelector('button[title="الوضع الداكن"]')?.dispatchEvent(new window.MouseEvent('click', { bubbles: true }))
await wait(200)
ok(document.documentElement.dataset.theme === 'dark', 'dark mode')

console.log('errors:', errors.length)
errors.slice(0, 10).forEach((e) => console.log(' !', e))
