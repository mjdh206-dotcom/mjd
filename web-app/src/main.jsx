import { Component, StrictMode } from 'react'
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

class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { error: null }
  }

  static getDerivedStateFromError(error) {
    return { error }
  }

  render() {
    if (!this.state.error) return this.props.children
    return (
      <div dir="rtl" style={{
        minHeight: '100vh', display: 'grid', placeItems: 'center', padding: 24,
        background: 'linear-gradient(160deg,#241710,#120E0A)', color: '#F5EADA',
        fontFamily: 'Cairo, Tahoma, sans-serif', textAlign: 'center',
      }}>
        <div style={{ maxWidth: 460 }}>
          <div style={{ width: 74, height: 74, margin: '0 auto 18px', borderRadius: 22, border: '1px solid rgba(194,152,63,.5)', display: 'grid', placeItems: 'center', background: 'rgba(194,152,63,.08)' }}>
            <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="#C2983F" strokeWidth="1.7">
              <path d="M20 7 12 3 4 7v6c0 5 8 8 8 8s8-3 8-8V7Z" /><path d="M12 8v5M12 16h.01" strokeLinecap="round" />
            </svg>
          </div>
          <h1 style={{ fontSize: 22, fontWeight: 800, margin: '0 0 8px' }}>حدث خلل غير متوقع</h1>
          <p style={{ fontSize: 13, color: 'rgba(245,234,218,.6)', lineHeight: 1.9, margin: '0 0 20px' }}>
            بياناتك محفوظة على الجهاز ولم تتأثر. أعد تحميل التطبيق للمتابعة، وإذا تكرر الخطأ
            فصدّر نسخة احتياطية من الإعدادات ثم أعد التثبيت.
          </p>
          <button onClick={() => window.location.reload()} style={{
            background: 'linear-gradient(180deg,#D9AE5A,#B8862F)', color: '#241710', border: 0,
            borderRadius: 12, padding: '12px 22px', fontWeight: 800, fontFamily: 'inherit', fontSize: 13, cursor: 'pointer',
          }}>إعادة تحميل التطبيق</button>
          <div style={{ marginTop: 14, fontSize: 11, color: 'rgba(245,234,218,.35)', direction: 'ltr' }}>
            {String(this.state.error?.message || this.state.error)}
          </div>
        </div>
      </div>
    )
  }
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </StrictMode>,
)
