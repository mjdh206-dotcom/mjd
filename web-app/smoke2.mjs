import { JSDOM } from 'jsdom'
import fs from 'node:fs'
import path from 'node:path'

const dom = new JSDOM('<!doctype html><html lang="ar" dir="rtl"><body><div id="root"></div></body></html>', { url: 'http://localhost/', pretendToBeVisual: true })
const { window } = dom
global.window = window
global.document = window.document
Object.defineProperty(global, 'navigator', { value: window.navigator, configurable: true })
global.localStorage = window.localStorage
global.HTMLElement = window.HTMLElement
global.Element = window.Element
global.Node = window.Node
global.getComputedStyle = window.getComputedStyle
global.requestAnimationFrame = (cb) => setTimeout(cb, 16)
global.cancelAnimationFrame = clearTimeout
global.MutationObserver = window.MutationObserver
global.CustomEvent = window.CustomEvent
global.Event = window.Event
global.File = window.File
global.Blob = window.Blob
global.URL = window.URL
global.HTMLInputElement = window.HTMLInputElement
global.SVGElement = window.SVGElement
window.scrollTo = () => {}
window.matchMedia = () => ({ matches: false, addListener() {}, removeListener() {}, addEventListener() {}, removeEventListener() {} })
window.HTMLCanvasElement.prototype.getContext = () => null

const errors = []
const origError = console.error
console.error = (...args) => { errors.push(args.map(String).join(' ').slice(0, 400)); origError(...args) }
process.on('unhandledRejection', (e) => errors.push('UNHANDLED: ' + e))

const files = fs.readdirSync('dist/assets').filter((f) => f.endsWith('.js'))
await import('file://' + path.resolve('dist/assets', files[0]))
const wait = (ms) => new Promise((r) => setTimeout(r, ms))
await wait(400)

const clickText = async (txt, exact = false) => {
  const els = [...document.querySelectorAll('button, a, select option')]
  const el = els.find((e) => (exact ? e.textContent.trim() === txt : e.textContent.includes(txt)))
  if (!el) return false
  el.dispatchEvent(new window.MouseEvent('click', { bubbles: true, cancelable: true }))
  await wait(120)
  return true
}

// إدخال الرقم السري 1234
for (const d of ['1', '2', '3', '4']) await clickText(d, true)
await wait(700)
const loggedIn = document.body.innerHTML.includes('لوحة التحكم') || document.body.innerHTML.includes('أهلاً')
console.log('login ok:', loggedIn)

const screens = ['الأصناف والمنتجات', 'المخزون والتحميص', 'الزبائن وكشوفهم', 'الموردون', 'المشتريات', 'المصاريف', 'الصندوق والإقفال', 'مسحوبات المحل', 'العمال والسحوبات', 'التقارير والتحليلات', 'المستخدمون والصلاحيات', 'سجل العمليات', 'الإعدادات والنسخ', 'الطلبات المسبقة', 'فواتير المبيعات', 'بيع سريع']
for (const s of screens) {
  const ok = await clickText(s)
  await wait(200)
  const len = document.getElementById('root').innerHTML.length
  console.log(`${ok ? '✓' : '✗'} ${s} -> len ${len}`)
}
await wait(300)
console.log('errors:', errors.length)
errors.slice(0, 12).forEach((e) => console.log('  !', e))
