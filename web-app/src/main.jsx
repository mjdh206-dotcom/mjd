import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '@fontsource/cairo/400.css'
import '@fontsource/cairo/500.css'
import '@fontsource/cairo/600.css'
import '@fontsource/cairo/700.css'
import '@fontsource/cairo/800.css'
import '@fontsource/amiri/400.css'
import '@fontsource/amiri/700.css'
import './index.css'
import App from './App'

if ('serviceWorker' in navigator && location.protocol === 'https:') {
  window.addEventListener('load', () => navigator.serviceWorker.register('./sw.js').catch(() => {}))
}

// التقاط الروابط الخارجية داخل تطبيق أندرويد وفتحها في التطبيق المناسب
document.addEventListener('click', (e) => {
  const a = e.target?.closest?.('a[href]')
  if (!a) return
  const href = a.getAttribute('href') || ''
  if (/^https?:/i.test(href) && !href.includes('appassets.androidplatform.net')) {
    e.preventDefault()
    if (window.AndroidApp?.openUrl) window.AndroidApp.openUrl(href)
    else window.open(href, '_blank', 'noopener')
  }
}, true)

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
