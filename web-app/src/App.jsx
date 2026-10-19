import { useEffect, useMemo, useState } from 'react'
import './index.css'
import {
  useStore, setTheme, setRate, loginAs, logout, verifyPin, updateSettings, rateNow,
} from './store'
import { money, rateToNumber } from './utils'
import { Modal, Field, useToast } from './components'
import Dashboard from './screens/Dashboard'
import Items from './screens/Items'
import SaleInvoice from './screens/SaleInvoice'
import Invoices from './screens/Invoices'
import Customers from './screens/Customers'
import Suppliers from './screens/Suppliers'
import Purchases from './screens/Purchases'
import Withdrawals from './screens/Withdrawals'
import Workers from './screens/Workers'
import Inventory from './screens/Inventory'
import Preorders from './screens/Preorders'
import Expenses from './screens/Expenses'
import Reports from './screens/Reports'
import Settings from './screens/Settings'
import Users from './screens/Users'
import Audit from './screens/Audit'
import InvoiceView from './screens/InvoiceView'

const NAV = [
  { group: 'الرئيسية', items: [
    { id: 'dashboard', label: 'لوحة التحكم', icon: '🏠' },
  ] },
  { group: 'المبيعات', items: [
    { id: 'sale-new', label: 'فاتورة جديدة', icon: '🧾' },
    { id: 'sales', label: 'فواتير المبيعات', icon: '📑' },
    { id: 'preorders', label: 'طلبات مسبقة', icon: '🎁' },
  ] },
  { group: 'الأصناف والمخزون', items: [
    { id: 'items', label: 'الأصناف', icon: '🥜' },
    { id: 'inventory', label: 'المخزون والتحميص', icon: '📦' },
  ] },
  { group: 'الحسابات', items: [
    { id: 'customers', label: 'الزبائن', icon: '👥' },
    { id: 'suppliers', label: 'الموردون', icon: '🏭' },
    { id: 'purchases', label: 'المشتريات', icon: '🛒' },
    { id: 'expenses', label: 'المصاريف', icon: '💸' },
    { id: 'cash', label: 'الصندوق والإقفال', icon: '💰' },
    { id: 'withdrawals', label: 'مسحوبات المحل', icon: '🏦' },
  ] },
  { group: 'العمال', items: [
    { id: 'workers', label: 'العمال والسحوبات', icon: '👷' },
  ] },
  { group: 'الإدارة', items: [
    { id: 'reports', label: 'التقارير', icon: '📊' },
    { id: 'users', label: 'المستخدمون', icon: '🔐' },
    { id: 'audit', label: 'سجل العمليات', icon: '📜' },
    { id: 'settings', label: 'الإعدادات والنسخ', icon: '⚙️' },
  ] },
]

const MOBILE_NAV = [
  { id: 'dashboard', label: 'الرئيسية', icon: '🏠' },
  { id: 'sale-new', label: 'فاتورة', icon: '🧾' },
  { id: 'sales', label: 'الفواتير', icon: '📑' },
  { id: 'customers', label: 'الزبائن', icon: '👥' },
  { id: 'reports', label: 'التقارير', icon: '📊' },
]

const TITLES = {
  dashboard: 'لوحة التحكم', 'sale-new': 'فاتورة مبيعات جديدة', sales: 'فواتير المبيعات',
  items: 'الأصناف', inventory: 'المخزون والتحميص', customers: 'الزبائن', suppliers: 'الموردون',
  purchases: 'المشتريات', expenses: 'المصاريف', cash: 'الصندوق والإقفال', withdrawals: 'مسحوبات المحل',
  workers: 'العمال والسحوبات', reports: 'التقارير', users: 'المستخدمون', audit: 'سجل العمليات',
  settings: 'الإعدادات والنسخ الاحتياطي', preorders: 'الطلبات المسبقة',
}

export default function App() {
  const state = useStore()
  const [page, setPage] = useState({ name: 'dashboard' })
  const [rateOpen, setRateOpen] = useState(false)
  const [toast, setToast, toastEl] = useToast()

  const session = state.session
  const isManager = session?.role === 'manager'
  const WORKER_ALLOWED = ['dashboard', 'sale-new', 'sales', 'items', 'customers']
  const visible = (id) => isManager || WORKER_ALLOWED.includes(id)

  useEffect(() => {
    document.documentElement.classList.toggle('dark', state.settings.theme === 'dark')
  }, [state.settings.theme])

  useEffect(() => {
    const allowed = isManager ? null : ['dashboard', 'sale-new', 'sales', 'items', 'customers']
    if (allowed && session && !allowed.includes(page.name)) setPage({ name: 'dashboard' })
  }, [session, page.name, isManager])

  const go = (name, params = {}) => { setPage({ name, ...params }); window.scrollTo({ top: 0, behavior: 'smooth' }) }

  if (!session) return <LockScreen onToast={setToast} toastEl={toastEl} />

  const render = () => {
    if (page.name === 'invoice-view') return <InvoiceView id={page.id} onBack={() => go('sales')} onToast={setToast} />
    const props = { go, onToast: setToast }
    switch (page.name) {
      case 'dashboard': return <Dashboard {...props} />
      case 'sale-new': return <SaleInvoice {...props} editId={page.id} onDone={() => go('sales')} />
      case 'sales': return <Invoices {...props} type="sale" />
      case 'purchases': return <Purchases {...props} />
      case 'items': return <Items {...props} />
      case 'inventory': return <Inventory {...props} />
      case 'customers': return <Customers {...props} />
      case 'suppliers': return <Suppliers {...props} />
      case 'expenses': return <Expenses {...props} />
      case 'cash': return <Expenses {...props} cashOnly />
      case 'withdrawals': return <Withdrawals {...props} />
      case 'workers': return <Workers {...props} />
      case 'preorders': return <Preorders {...props} />
      case 'reports': return <Reports {...props} />
      case 'users': return <Users {...props} />
      case 'audit': return <Audit {...props} />
      case 'settings': return <Settings {...props} />
      default: return <Dashboard {...props} />
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <div className="logo">مح</div>
          <div>
            <h1>{state.settings.shopName}<span>إدارة الفواتير والحسابات</span></h1>
          </div>
        </div>
        <div className="topbar-actions">
          <button className="pill" onClick={() => setRateOpen(true)} title="تحديث سعر الصرف">
            <span>💵 الدولار</span>
            <strong>{rateNow().toLocaleString('en-US')} ل.س</strong>
            <span style={{ opacity: .7 }}>✎</span>
          </button>
          <button className="icon-btn" title="الوضع الداكن" onClick={() => setTheme(state.settings.theme === 'dark' ? 'light' : 'dark')}>
            {state.settings.theme === 'dark' ? '☀️' : '🌙'}
          </button>
          <button className="icon-btn" title="تسجيل الخروج" onClick={logout}>⎋</button>
        </div>
      </header>

      <div className="main">
        <aside className="sidebar">
          <div className="card" style={{ marginBottom: 12, padding: 12, background: 'linear-gradient(135deg,#FFF8E1,#FFF3CD)' }}>
            <div className="row" style={{ gap: 10, flexWrap: 'nowrap' }}>
              <div style={{ width: 38, height: 38, borderRadius: 10, background: 'var(--brown)', color: '#FFD54F', display: 'grid', placeItems: 'center', fontWeight: 800 }}>
                {session.name.slice(0, 1)}
              </div>
              <div style={{ minWidth: 0 }}>
                <div style={{ fontWeight: 800, fontSize: 13 }}>{session.name}</div>
                <div className="small muted">{isManager ? 'مدير — كل الصلاحيات' : 'عامل — فواتير فقط'}</div>
              </div>
            </div>
          </div>
          {NAV.map((g) => (
            <div className="nav-group" key={g.group}>
              <div className="nav-group-title">{g.group}</div>
              {g.items.filter((it) => visible(it.id)).map((it) => (
                <button key={it.id} className={`nav-item ${page.name === it.id ? 'active' : ''}`} onClick={() => go(it.id)}>
                  <span className="ico">{it.icon}</span>{it.label}
                </button>
              ))}
            </div>
          ))}
        </aside>

        <main className="content" style={{ paddingBottom: 80 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12, gap: 8 }}>
            <div>
              <div className="small muted">{state.settings.shopName} • {state.settings.branch}</div>
              <h2 style={{ fontSize: 18 }}>{TITLES[page.name] || ''}</h2>
            </div>
            <div className="row" style={{ gap: 6 }}>
              {page.name !== 'sale-new' && <button className="btn btn-gold btn-sm" onClick={() => go('sale-new')}>＋ فاتورة</button>}
            </div>
          </div>
          {render()}
        </main>
      </div>

      <nav className="bottom-nav">
        {MOBILE_NAV.filter((it) => visible(it.id)).map((it) => (
          <button key={it.id} className={`bnav-item ${page.name === it.id ? 'active' : ''}`} onClick={() => go(it.id)}>
            <span className="ico">{it.icon}</span>{it.label}
          </button>
        ))}
        {isManager && (
          <button className="bnav-item" onClick={() => go('settings')}>
            <span className="ico">⚙️</span>الإعدادات
          </button>
        )}
      </nav>

      <Modal open={rateOpen} onClose={() => setRateOpen(false)} title="تحديث سعر صرف الدولار"
        footer={<button className="btn btn-brown" onClick={() => setRateOpen(false)}>تم</button>}>
        <RateForm onDone={() => { setRateOpen(false); setToast('تم تحديث سعر الصرف') }} />
        <p className="small muted" style={{ marginTop: 10 }}>
          سعر الصرف يُسجَّل مع كل فاتورة جديدة، وتعديله الآن لا يغيّر الفواتير القديمة.
        </p>
      </Modal>
      {toastEl}
    </div>
  )
}

function RateForm({ onDone }) {
  const state = useStore()
  const [v, setV] = useState(String(rateToNumber(state.settings.rate)))
  return (
    <div>
      <Field label="سعر الدولار بالليرة السورية">
        <input className="input" inputMode="decimal" value={v} onChange={(e) => setV(e.target.value)} />
      </Field>
      <div className="row" style={{ marginTop: 10 }}>
        <button className="btn btn-gold" onClick={() => { setRate(Number(v)); onDone?.() }}>حفظ السعر</button>
        <span className="small muted">آخر سعر محفوظ: {rateNow().toLocaleString('en-US')} ل.س</span>
      </div>
    </div>
  )
}

function LockScreen() {
  const state = useStore()
  const [pin, setPin] = useState('')
  const [err, setErr] = useState('')
  const [selected, setSelected] = useState(state.users.find((u) => u.role === 'manager')?.id || '')

  const submit = (p = pin) => {
    const user = verifyPin(p)
    if (user) { loginAs(user); setErr('') }
    else { setErr('الرقم السري غير صحيح'); setPin('') }
  }
  const press = (d) => {
    if (d === 'del') return setPin(pin.slice(0, -1))
    if (pin.length >= 6) return
    const next = pin + d
    setPin(next)
    if (next.length === 4) setTimeout(() => submit(next), 120)
  }

  return (
    <div className="pin-overlay">
      <div className="pin-card">
        <div style={{ width: 64, height: 64, margin: '0 auto 10px', borderRadius: 18, background: 'linear-gradient(135deg,#D4A017,#FFD54F)', display: 'grid', placeItems: 'center', fontSize: 28, color: '#4E342E', fontWeight: 800 }}>مح</div>
        <h2 style={{ fontFamily: 'Amiri, serif', color: '#4E342E', fontSize: 26 }}>محامص الحرمين</h2>
        <p className="small muted" style={{ marginBottom: 16 }}>أدخل الرقم السري للدخول</p>

        <div className="row" style={{ justifyContent: 'center', marginBottom: 12 }}>
          {[0, 1, 2, 3].map((i) => (
            <div key={i} style={{ width: 14, height: 14, borderRadius: 99, background: pin.length > i ? '#4E342E' : '#E0D7C8' }} />
          ))}
        </div>
        {err && <div className="small" style={{ color: '#C62828', marginBottom: 8 }}>{err}</div>}

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 8, marginBottom: 12 }}>
          {['1', '2', '3', '4', '5', '6', '7', '8', '9', 'م', '0', 'del'].map((k) => (
            <button key={k} onClick={() => press(k === 'del' ? 'del' : k === 'م' ? '' : k)}
              style={{ padding: '12px 0', borderRadius: 12, border: '1px solid #EFEBE9', background: k === 'del' ? '#FFF3E0' : '#fff', fontSize: 17, fontWeight: 700, color: '#4E342E' }}>
              {k === 'del' ? '⌫' : k === 'م' ? '' : k}
            </button>
          ))}
        </div>

        <select className="input" value={selected} onChange={(e) => setSelected(e.target.value)} style={{ marginBottom: 8 }}>
          {state.users.filter((u) => u.active).map((u) => (
            <option key={u.id} value={u.id}>{u.name} — {u.role === 'manager' ? 'مدير' : 'عامل'}</option>
          ))}
        </select>
        <button className="btn btn-brown" style={{ width: '100%' }} onClick={() => submit()}>دخول</button>
        <p className="small muted" style={{ marginTop: 12 }}>تجريبي: المدير 1234 — العامل 1111</p>
        <p className="small muted" style={{ marginTop: 4 }}>يعمل بدون إنترنت • بيانات محفوظة على الجهاز</p>
      </div>
    </div>
  )
}
