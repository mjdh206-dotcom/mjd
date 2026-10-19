import { JSDOM } from 'jsdom'
import fs from 'node:fs'
import path from 'node:path'

const dom = new JSDOM('<!doctype html><html lang="ar" dir="rtl"><body><div id="root"></div></body></html>', {
  url: 'http://localhost/',
  pretendToBeVisual: true,
})
const { window } = dom
global.window = window
global.document = window.document
Object.defineProperty(global, "navigator", { value: window.navigator, configurable: true })
global.localStorage = window.localStorage
global.HTMLElement = window.HTMLElement
global.Element = window.Element
global.Node = window.Node
global.getComputedStyle = window.getComputedStyle
global.requestAnimationFrame = (cb) => setTimeout(cb, 16)
global.cancelAnimationFrame = clearTimeout
window.matchMedia = window.matchMedia || (() => ({ matches: false, addListener() {}, removeListener() {}, addEventListener() {}, removeEventListener() {} }))
window.scrollTo = () => {}
global.IS_REACT_ACT_ENVIRONMENT = true
global.MutationObserver = window.MutationObserver
global.CustomEvent = window.CustomEvent
global.Event = window.Event
global.File = window.File
global.Blob = window.Blob
global.URL = window.URL
global.HTMLInputElement = window.HTMLInputElement
global.SVGElement = window.SVGElement
window.HTMLCanvasElement.prototype.getContext = () => null


const files = fs.readdirSync('dist/assets').filter((f) => f.endsWith('.js'))
const bundle = path.join('dist/assets', files[0])
await import('file://' + path.resolve(bundle))
await new Promise((r) => setTimeout(r, 600))
const html = document.getElementById('root').innerHTML
if (!html || html.length < 500) {
  console.error('RENDER FAILED, length=', html.length)
  process.exit(1)
}
console.log('rendered length:', html.length)
console.log('has shop name:', html.includes('محامص الحرمين'))
console.log('has pin screen:', html.includes('أدخل الرقم السري'))
console.log('snippet:', html.slice(0, 200).replace(/\s+/g, ' '))
