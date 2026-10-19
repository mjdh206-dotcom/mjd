import { JSDOM } from 'jsdom'
import fs from 'node:fs'
import path from 'node:path'

const KEY = 'mahamess_alharamayn_v1'
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
await wait(350)

const btn = (txt, exact = false) => [...document.querySelectorAll('button, a')].find((e) => (exact ? e.textContent.trim() === txt : e.textContent.includes(txt)))
const click = async (txt, exact = false) => { const el = btn(txt, exact); if (!el) return false; el.dispatchEvent(new window.MouseEvent('click', { bubbles: true, cancelable: true })); await wait(180); return true }
const setVal = (el, v) => {
  const proto = el.tagName === 'TEXTAREA' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype
  Object.getOwnPropertyDescriptor(proto, 'value').set.call(el, v)
  el.dispatchEvent(new window.Event('input', { bubbles: true }))
}
const text = () => document.getElementById('root').innerHTML
const ok = (cond, label) => console.log(`${cond ? '✓' : '✗'} ${label}`)

for (const d of ['1', '2', '3', '4']) await click(d, true)
await wait(500)
ok(text().includes('أهلاً'), 'login')
const st0 = JSON.parse(localStorage.getItem(KEY))
ok(st0.items.length > 100, `catalog loaded (${st0.items.length})`)
ok(st0.items.every((i) => i.stock === 0 && i.retail === 0), 'everything starts at zero')
ok(st0.invoices.length === 0, 'no demo invoices')

// pricing screen
await click('التسعير السريع', true)
await wait(300)
ok(text().includes('التسعير السريع'), 'pricing screen')
ok(document.querySelectorAll('.price-input').length > 0, 'price inputs rendered')
const saveBtn = [...document.querySelectorAll('button')].find((b) => b.textContent.includes('حفظ الكل'))
ok(saveBtn?.disabled, 'save disabled before changes')

const goldInputs = [...document.querySelectorAll('input.price-input.gold')]
const costInputs = [...document.querySelectorAll('input.price-input:not(.gold)')]
ok(goldInputs.length > 0, 'retail inputs')
setVal(goldInputs[0], '25000'); await wait(100)
setVal(costInputs[0], '20000'); await wait(100)
ok(text().includes('تعديل غير محفوظ') || [...document.querySelectorAll('button')].some((b) => b.textContent.includes('حفظ الكل (')), 'dirty state shown')
const save2 = [...document.querySelectorAll('button')].find((b) => b.textContent.includes('حفظ الكل') && !b.disabled)
save2?.dispatchEvent(new window.MouseEvent('click', { bubbles: true }))
await wait(400)
const st1 = JSON.parse(localStorage.getItem(KEY))
ok(st1.items.some((i) => i.retail === 25000 && i.cost === 20000), 'bulk price saved')
ok(st1.analysis !== null, 'state persisted')

// margin modal
await click('تسعير بهامش')
await wait(200)
ok(text().includes('هامش الربح %'), 'margin modal')
await click('إلغاء')
await wait(100)

// items quick price
await click('الأصناف والمنتجات', true)
await wait(300)
ok(text().includes('أصناف بلا سعر'), 'unpriced kpi')
const cells = [...document.querySelectorAll('.price-cell')]
ok(cells.length > 0, 'quick price cells')
cells.find((c) => c.textContent.includes('حدّد السعر'))?.dispatchEvent(new window.MouseEvent('click', { bubbles: true }))
await wait(250)
ok(text().includes('حفظ السعر'), 'quick price modal')
const inp = [...document.querySelectorAll('.modal input')].find((i) => i.getAttribute('inputmode') === 'decimal')
setVal(inp, '12000'); await wait(100)
await click('حفظ السعر')
await wait(350)
const st2 = JSON.parse(localStorage.getItem(KEY))
ok(st2.items.some((i) => i.retail === 12000), 'quick price saved')
ok(st2.audit.length > 0, 'audit logged')

// bulk stocktake
await click('المخزون والتحميص', true)
await wait(300)
await click('جرد شامل')
await wait(300)
ok(text().includes('الكمية الفعلية'), 'bulk stocktake modal')
const counted = document.querySelector('.modal input.price-input')
ok(!!counted, 'counted input')
setVal(counted, '12'); await wait(120)
const saveStock = [...document.querySelectorAll('button')].find((b) => b.textContent.includes('حفظ الجرد') && !b.disabled)
saveStock?.dispatchEvent(new window.MouseEvent('click', { bubbles: true }))
await wait(350)
const st3 = JSON.parse(localStorage.getItem(KEY))
ok(st3.items.some((i) => i.stock > 0), 'stock applied')
ok(st3.stocktakes.length > 0, 'stocktake recorded')

// full reset
await click('الإعدادات والنسخ', true)
await wait(250)
await click('النسخ الاحتياطي')
await wait(200)
await click('تصفير كل شيء')
await wait(250)
ok(text().includes('اكتب «تصفير» للتأكيد'), 'reset modal')
const confirmInput = [...document.querySelectorAll('.modal input')].pop()
setVal(confirmInput, 'تصفير'); await wait(150)
await click('تصفير نهائي')
await wait(400)
const st4 = JSON.parse(localStorage.getItem(KEY))
ok(st4.invoices.length === 0, 'reset clears invoices')
ok(st4.items.length > 100, 'reset restores catalog')
ok(st4.items.every((i) => i.stock === 0 && i.retail === 0), 'reset zeros everything')

console.log('errors:', errors.length)
errors.slice(0, 8).forEach((e) => console.log(' !', e))
