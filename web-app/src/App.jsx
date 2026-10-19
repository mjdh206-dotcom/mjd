import { useEffect, useMemo, useRef, useState } from 'react'
import './index.css'
import {
  useStore, setTheme, setRate, loginAs, logout, verifyPin, updateSettings, rateNow,
  lowStockItems, expiringItems, partyStats,
} from './store'
import { money, rateToNumber, convert, fmtDate, daysUntil, normalizeAr, todayISO } from './utils'
import { Modal, Drawer, Field, useToast, Avatar, Seg, Toggle, Empty, StatusBadge } from './components'
import { Icon, BrandMark, categoryIcon } from './icons'
import Dashboard from './screens/Dashboard'
import Items from './screens/Items'
import Pricing from './screens/Pricing'
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
  { group: 'نظرة عامة', items: [
    { id: 'dashboard', label: 'لوحة التحكم', icon: 'dashboard' },
    { id: 'sale-new', label: 'بيع سريع', icon: 'cart', managerOnly: false, highlight: true },
  ] },
  { group: 'المبيعات', items: [
    { id: 'sales', label: 'فواتير المبيعات', icon: 'receipt' },
    { id: 'preorders', label: 'الطلبات المسبقة', icon: 'gift', managerOnly: true },
  ] },
  { group: 'المخزون', items: [
    { id: 'items', label: 'الأصناف والمنتجات', icon: 'peanut' },
    { id: 'pricing', label: 'التسعير السريع', icon: 'tag', managerOnly: true },
    { id: 'inventory', label: 'المخزون والتحميص', icon: 'warehouse', managerOnly: true },
  ] },
  { group: 'الحسابات', items: [
    { id: 'customers', label: 'الزبائن وكشوفهم', icon: 'users' },
    { id: 'suppliers', label: 'الموردون', icon: 'truck', managerOnly: true },
    { id: 'purchases', label: 'المشتريات', icon: 'bag', managerOnly: true },
    { id: 'expenses', label: 'المصاريف', icon: 'wallet', managerOnly: true },
    { id: 'cash', label: 'الصندوق والإقفال', icon: 'bank', managerOnly: true },
    { id: 'withdrawals', label: 'مسحوبات المحل', icon: 'coins', managerOnly: true },
  ] },
  { group: 'الفريق', items: [
    { id: 'workers', label: 'العمال والسحوبات', icon: 'employee', managerOnly: true },
  ] },
  { group: 'الإدارة', items: [
    { id: 'reports', label: 'التقارير والتحليلات', icon: 'chart', managerOnly: true },
    { id: 'users', label: 'المستخدمون والصلاحيات', icon: 'shield', managerOnly: true },
    { id: 'audit', label: 'سجل العمليات', icon: 'history', managerOnly: true },
    { id: 'settings', label: 'الإعدادات والنسخ', icon: 'settings', managerOnly: true },
  ] },
]

const TITLES = {
  dashboard: 'لوحة التحكم', 'sale-new': 'بيع سريع', sales: 'فواتير المبيعات', items: 'الأصناف والمنتجات', pricing: 'التسعير السريع',
  inventory: 'المخزون والتحميص', customers: 'الزبائن', suppliers: 'الموردون', purchases: 'المشتريات',
  expenses: 'المصاريف', cash: 'الصندوق والإقفال', withdrawals: 'مسحوبات المحل', workers: 'العمال والسحوبات',
  reports: 'التقارير والتحليلات', users: 'المستخدمون', audit: 'سجل العمليات', settings: 'الإعدادات والنسخ',
  preorders: 'الطلبات المسبقة', 'invoice-view': 'تفاصيل الفاتورة',
}
const SUBS = {
  dashboard: 'ملخص حي لأداء المحل اليوم',
  'sale-new': 'أنشئ فاتورة في ثوانٍ',
  pricing: 'حدّد أسعار كل الأصناف في جدول واحد',
  sales: 'كل الفواتير، قابلة للبحث والتعديل والإلغاء',
  items: 'الأسعار والوحدات والباركود والصلاحية',
  inventory: 'الجرد والتحميص وحركة المخزون',
  customers: 'الأرصدة وكشوف الحساب والدفعات',
  reports: 'أرقام دقيقة بالعملتين مع تصدير احترافي',
}

const WORKER_ALLOWED = ['dashboard', 'sale-new', 'sales', 'items', 'customers']

export default function App() {
  const state = useStore()
  const [page, setPage] = useState({ name: 'dashboard' })
  const [rateOpen, setRateOpen] = useState(false)
  const [notifOpen, setNotifOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const [paletteOpen, setPaletteOpen] = useState(false)
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [toast, notify, toastEl] = useToast()

  const session = state.session
  const isManager = session?.role === 'manager'

  useEffect(() => {
    document.documentElement.dataset.theme = state.settings.theme === 'dark' ? 'dark' : 'light'
    document.documentElement.lang = 'ar'
    document.documentElement.dir = 'rtl'
  }, [state.settings.theme])

  useEffect(() => {
    if (!isManager && session && !WORKER_ALLOWED.includes(page.name)) setPage({ name: 'dashboard' })
  }, [session, page.name, isManager])

  useEffect(() => {
    const h = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); setPaletteOpen(true) }
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'b') { e.preventDefault(); setSidebarOpen((v) => !v) }
    }
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  }, [])

  const go = (name, params = {}) => {
    setPage({ name, ...params })
    setSidebarOpen(false)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  // زر الرجوع في أندرويد: يُغلق النوافذ ثم يعود للرئيسية
  useEffect(() => {
    window.__onBack = () => {
      if (paletteOpen) { setPaletteOpen(false); return true }
      if (notifOpen) { setNotifOpen(false); return true }
      if (profileOpen) { setProfileOpen(false); return true }
      if (rateOpen) { setRateOpen(false); return true }
      if (sidebarOpen) { setSidebarOpen(false); return true }
      if (page.name === 'invoice-view') { go('sales'); return true }
      if (page.name !== 'dashboard') { go('dashboard'); return true }
      return false
    }
    return () => { delete window.__onBack }
  }, [paletteOpen, notifOpen, profileOpen, rateOpen, sidebarOpen, page.name])

  const alerts = useMemo(() => {
    if (!session) return []
    const list = []
    const low = lowStockItems()
    const exp = expiringItems(30)
    if (low.length) list.push({ id: 'low', tone: 'red', icon: 'package', title: `${low.length} أصناف تحت حد التنبيه`, sub: low.slice(0, 2).map((i) => i.name).join('، '), go: 'inventory' })
    if (exp.length) list.push({ id: 'exp', tone: 'gold', icon: 'clock', title: `${exp.length} أصناف تقترب من انتهاء الصلاحية`, sub: exp.slice(0, 2).map((i) => `${i.name} — ${daysUntil(i.expiry)} يوم`).join('، '), go: 'inventory' })
    const overdue = state.customers.filter((c) => c.id !== 'cash').map((c) => ({ c, st: partyStats(c.id, 'customer') }))
      .filter(({ st }) => (st.balances.SYP > 0 || st.balances.USD > 0) && st.lastInvoice && daysSince(st.lastInvoice.date) >= 30)
    if (overdue.length) list.push({ id: 'debt', tone: 'red', icon: 'clock', title: `${overdue.length} زبائن عليهم ديون متأخرة`, sub: overdue.slice(0, 2).map((x) => x.c.name).join('، '), go: 'customers' })
    const pending = state.preorders.filter((p) => p.status === 'pending')
    if (pending.length && isManager) list.push({ id: 'pre', tone: 'blue', icon: 'gift', title: `${pending.length} طلبات مسبقة بانتظار التحضير`, sub: pending.slice(0, 2).map((p) => p.customerName).join('، '), go: 'preorders' })
    if (!state.closings.some((c) => c.date === todayISO()) && isManager) list.push({ id: 'close', tone: 'green', icon: 'bank', title: 'لم يُسجَّل إقفال صندوق اليوم', sub: 'راجع الصندوق قبل نهاية الدوام', go: 'cash' })
    return list
  }, [state, session, isManager])

  if (!session) return <LockScreen notify={notify} toastEl={toastEl} />

  const render = () => {
    if (page.name === 'invoice-view') return <InvoiceView id={page.id} onBack={() => go('sales')} onToast={notify} go={go} />
    const props = { go, onToast: notify }
    switch (page.name) {
      case 'dashboard': return <Dashboard {...props} />
      case 'sale-new': return <SaleInvoice {...props} editId={page.id} onDone={() => go('sales')} />
      case 'sales': return <Invoices {...props} type="sale" />
      case 'purchases': return <Purchases {...props} />
      case 'items': return <Items {...props} />
      case 'pricing': return <Pricing {...props} />
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

  const visibleNav = NAV.map((g) => ({ ...g, items: g.items.filter((i) => isManager || !i.managerOnly) })).filter((g) => g.items.length)
  const mobileNav = [
    { id: 'dashboard', label: 'الرئيسية', icon: 'dashboard' },
    { id: 'sales', label: 'الفواتير', icon: 'receipt' },
    { id: 'sale-new', label: 'بيع', fab: true },
    { id: 'customers', label: 'الزبائن', icon: 'users' },
    isManager ? { id: 'reports', label: 'التقارير', icon: 'chart' } : { id: 'items', label: 'الأصناف', icon: 'peanut' },
  ]

  return (
    <div className="app">
      {sidebarOpen && <div onClick={() => setSidebarOpen(false)} style={{ position: 'fixed', inset: 0, background: 'rgba(20,12,7,.45)', zIndex: 55, backdropFilter: 'blur(2px)' }} className="no-print" />}
      <aside className={`sidebar ${sidebarOpen ? 'open' : ''}`}>
        <div className="brand">
          <BrandMark size={44} className="mark" />
          <div className="brand-txt">
            <h1>{state.settings.shopName}</h1>
            <span>MAHAMESS AL HARAMAYN</span>
          </div>
        </div>
        <div className="side-scroll">
          {visibleNav.map((g) => (
            <div key={g.group}>
              <div className="nav-label">{g.group}</div>
              {g.items.map((it) => (
                <button key={it.id} className={`nav-item ${page.name === it.id || (it.id === 'sales' && page.name === 'invoice-view') ? 'active' : ''}`} onClick={() => go(it.id)}>
                  <Icon name={it.icon} size={18} />
                  <span>{it.label}</span>
                  {it.id === 'preorders' && state.preorders.filter((p) => p.status === 'pending').length > 0 && (
                    <span className="nav-badge">{state.preorders.filter((p) => p.status === 'pending').length}</span>
                  )}
                  {it.id === 'inventory' && lowStockItems().length > 0 && <span className="nav-badge">{lowStockItems().length}</span>}
                </button>
              ))}
            </div>
          ))}
        </div>
        <div className="side-foot">
          <div className="rate-mini">
            <div className="lbl"><span className="dot" style={{ background: '#6FBF95' }} />سعر صرف الدولار</div>
            <div className="val">{rateNow().toLocaleString('en-US')} <small>ل.س</small></div>
          </div>
          <button className="user-card" onClick={() => setProfileOpen(true)}>
            <Avatar name={session.name} size="sm" />
            <div style={{ minWidth: 0 }}>
              <div className="u-name">{session.name}</div>
              <div className="u-role">{isManager ? 'مدير المحل' : 'عامل مبيعات'}</div>
            </div>
            <Icon name="more" size={17} className="out" />
          </button>
        </div>
      </aside>

      <div className="main">
        <header className="topbar no-print">
          <button className="icon-btn menu-btn" onClick={() => setSidebarOpen(true)} aria-label="القائمة"><Icon name="menu" /></button>
          <div className="page-head-txt">
            <div className="crumb"><b>محامص الحرمين</b><Icon name="chevronLeft" size={10} />{TITLES[page.name] || ''}</div>
            <div className="page-title">{TITLES[page.name] || 'لوحة التحكم'}</div>
          </div>
          <div className="top-actions">
            <button className="search-trigger" onClick={() => setPaletteOpen(true)}>
              <Icon name="search" /><span>بحث شامل…</span><span className="kbd">⌘K</span>
            </button>
            <button className="rate-chip" onClick={() => setRateOpen(true)} title="تعديل سعر الصرف">
              <span className="dot" /> <span className="hide-xs" style={{ color: 'var(--muted)' }}>الدولار</span>
              <b>{rateNow().toLocaleString('en-US')}</b><small>ل.س</small>
            </button>
            <button className="icon-btn" onClick={() => setTheme(state.settings.theme === 'dark' ? 'light' : 'dark')} title="الوضع الداكن">
              <Icon name={state.settings.theme === 'dark' ? 'sun' : 'moon'} />
            </button>
            <button className="icon-btn" onClick={() => setNotifOpen(true)} title="التنبيهات">
              <Icon name="bell" />
              {alerts.length > 0 && <span className="dot-badge">{alerts.length}</span>}
            </button>
            <button className="avatar" onClick={() => setProfileOpen(true)} title={session.name}>
              {(session.name || '؟').trim().split(/\s+/).slice(0, 2).map((w) => w[0]).join('')}
            </button>
          </div>
        </header>

        <main className="content" key={page.name}>
          <div className="fade-in">{render()}</div>
        </main>

        <nav className="bottomnav no-print">
          {mobileNav.map((it) => it.fab ? (
            <button key={it.id} className="bnav-fab" onClick={() => go(it.id)} aria-label="بيع جديد"><Icon name="plus" /></button>
          ) : (
            <button key={it.id} className={`bnav ${page.name === it.id ? 'on' : ''}`} onClick={() => go(it.id)}>
              <Icon name={it.icon} /><span>{it.label}</span>
            </button>
          ))}
        </nav>
      </div>

      {rateOpen && <RateModal onClose={() => setRateOpen(false)} notify={notify} />}
      {paletteOpen && <CommandPalette onClose={() => setPaletteOpen(false)} go={go} state={state} isManager={isManager} />}
      {notifOpen && (
        <Drawer open onClose={() => setNotifOpen(false)} title="مركز التنبيهات" subtitle={`${alerts.length} تنبيه يحتاج انتباهك`} icon="bell" width={430}>
          {alerts.length === 0 ? <Empty icon="checkCircle" title="كل شيء تحت السيطرة" text="لا توجد تنبيهات حالياً." /> : (
            <div className="stack">
              {alerts.map((a) => (
                <button key={a.id} className="card tight" style={{ textAlign: 'start', cursor: 'pointer' }} onClick={() => { setNotifOpen(false); go(a.go) }}>
                  <div className="row nowrap">
                    <span className={`insight ${a.tone}`} style={{ border: 0, boxShadow: 'none', padding: 0 }}>
                      <span className="ico" style={{ width: 38, height: 38 }}><Icon name={a.icon} /></span>
                    </span>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 12.5, fontWeight: 800 }}>{a.title}</div>
                      <div className="tiny muted" style={{ marginTop: 2 }}>{a.sub}</div>
                    </div>
                    <Icon name="chevronLeft" size={16} style={{ color: 'var(--muted)' }} />
                  </div>
                </button>
              ))}
            </div>
          )}
          <button className="btn btn-ghost btn-block" style={{ marginTop: 14 }} onClick={() => { setNotifOpen(false); go('reports') }}>
            <Icon name="chart" />فتح التقارير
          </button>
        </Drawer>
      )}
      {profileOpen && <ProfileSheet session={session} settings={state.settings} onClose={() => setProfileOpen(false)} onLogout={() => { logout(); setProfileOpen(false) }} onTheme={() => setTheme(state.settings.theme === 'dark' ? 'light' : 'dark')} notify={notify} onPalette={() => { setProfileOpen(false); setPaletteOpen(true) }} />}
      {toastEl}
    </div>
  )
}

function daysSince(iso) {
  const d = new Date(iso + 'T00:00:00')
  return Math.round((new Date(todayISO() + 'T00:00:00') - d) / 86400000)
}

/* ============ سعر الصرف ============ */
function RateModal({ onClose, notify }) {
  const s = useStore()
  const [val, setVal] = useState(String(rateToNumber(s.settings.rate)))
  const n = Number(String(val).replace(/,/g, '')) || 0
  return (
    <Modal open onClose={onClose} title="سعر صرف الدولار" subtitle="يُسجَّل مع كل فاتورة جديدة، ولا يغيّر الفواتير السابقة"
      icon="dollar" size="sm"
      footer={<>
        <button className="btn btn-gold" onClick={() => { if (n > 0) { setRate(n); notify('تم تحديث سعر الصرف'); onClose() } else notify('أدخل سعراً صحيحاً', 'error') }}>
          <Icon name="check" />تحديث السعر
        </button>
        <button className="btn btn-ghost" onClick={onClose}>إلغاء</button>
      </>}>
      <div className="stack">
        <div style={{ background: 'linear-gradient(150deg,#2E1F15,#241710)', color: '#F7EDDD', borderRadius: 14, padding: 18, textAlign: 'center' }}>
          <div className="tiny" style={{ color: 'rgba(247,237,221,.55)', letterSpacing: 1 }}>السعر الحالي</div>
          <div style={{ fontSize: 32, fontWeight: 800, color: '#FFEFCB', direction: 'ltr', marginTop: 4 }}>
            {rateNow().toLocaleString('en-US')} <span style={{ fontSize: 13, color: 'rgba(255,239,203,.6)' }}>ل.س</span>
          </div>
          <div className="tiny" style={{ color: 'rgba(247,237,221,.5)', marginTop: 4 }}>لكل دولار أمريكي واحد</div>
        </div>
        <Field label="السعر الجديد (ليرة سورية لكل دولار)">
          <div style={{ position: 'relative' }}>
            <input className="input" inputMode="decimal" value={val} onChange={(e) => setVal(e.target.value)} style={{ fontSize: 17, fontWeight: 800, height: 48, direction: 'ltr', textAlign: 'right', paddingInlineEnd: 48 }} autoFocus />
            <span className="money-suffix" style={{ fontSize: 11 }}>ل.س</span>
          </div>
        </Field>
        {n > 0 && (
          <div className="grid g2" style={{ gap: 8 }}>
            <div className="card tight" style={{ background: 'var(--surface-2)' }}>
              <div className="tiny muted">100 دولار تعادل</div>
              <b style={{ fontSize: 14, direction: 'ltr', display: 'block', color: 'var(--gold-deep)' }}>{Math.round(n * 100).toLocaleString('en-US')} ل.س</b>
            </div>
            <div className="card tight" style={{ background: 'var(--surface-2)' }}>
              <div className="tiny muted">1,000,000 ل.س تعادل</div>
              <b style={{ fontSize: 14, direction: 'ltr', display: 'block', color: 'var(--gold-deep)' }}>${(1000000 / n).toFixed(2)}</b>
            </div>
          </div>
        )}
      </div>
    </Modal>
  )
}

/* ============ لوحة الأوامر ============ */
function CommandPalette({ onClose, go, state, isManager }) {
  const [q, setQ] = useState('')
  const [idx, setIdx] = useState(0)
  const inputRef = useRef(null)
  useEffect(() => { inputRef.current?.focus() }, [])

  const results = useMemo(() => {
    const query = normalizeAr(q)
    const out = []
    const screens = NAV.flatMap((g) => g.items).filter((it) => isManager || !it.managerOnly)
    screens.filter((it) => !query || normalizeAr(it.label).includes(query)).slice(0, query ? 6 : 8).forEach((it) => out.push({ type: 'شاشة', icon: it.icon, title: it.label, action: () => go(it.id) }))
    if (query) {
      state.items.filter((i) => normalizeAr(i.name).includes(query) || (i.barcode || '').includes(q)).slice(0, 4)
        .forEach((i) => out.push({ type: 'صنف', icon: categoryIcon(i.category), title: i.name, sub: i.barcode, action: () => go('items') }))
      state.customers.filter((c) => normalizeAr(c.name).includes(query) || (c.phone || '').includes(q)).slice(0, 4)
        .forEach((c) => out.push({ type: 'زبون', icon: 'user', title: c.name, sub: c.phone, action: () => go('customers') }))
      state.invoices.filter((i) => (i.no || '').toLowerCase().includes(q.toLowerCase()) || normalizeAr(i.partyName).includes(query)).slice(0, 4)
        .forEach((i) => out.push({ type: 'فاتورة', icon: 'receipt', title: `${i.no} — ${i.partyName}`, sub: money(i.total, i.currency), action: () => go('invoice-view', { id: i.id }) }))
    }
    return out
  }, [q, state, isManager])

  useEffect(() => { setIdx(0) }, [q])
  useEffect(() => {
    const h = (e) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  }, [onClose])

  const onKey = (e) => {
    if (e.key === 'ArrowDown') { e.preventDefault(); setIdx((i) => Math.min(results.length - 1, i + 1)) }
    if (e.key === 'ArrowUp') { e.preventDefault(); setIdx((i) => Math.max(0, i - 1)) }
    if (e.key === 'Enter' && results[idx]) { e.preventDefault(); results[idx].action(); onClose() }
  }

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()} style={{ alignItems: 'flex-start', paddingTop: '8vh' }}>
      <div className="modal" style={{ maxWidth: 620 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 11, padding: '14px 18px', borderBottom: '1px solid var(--line)' }}>
          <Icon name="search" size={20} style={{ color: 'var(--gold-deep)' }} />
          <input ref={inputRef} value={q} onChange={(e) => setQ(e.target.value)} onKeyDown={onKey}
            placeholder="ابحث عن شاشة، صنف، زبون أو رقم فاتورة…"
            style={{ flex: 1, border: 0, outline: 'none', background: 'transparent', fontSize: 14, fontWeight: 600 }} />
          <span className="kbd">Esc</span>
        </div>
        <div style={{ maxHeight: '55vh', overflowY: 'auto', padding: 8 }}>
          {results.length === 0 && <Empty icon="search" title="لا نتائج" text="جرّب اسم صنف أو رقم فاتورة أو اسم شاشة." />}
          {results.map((r, i) => (
            <button key={i} onMouseEnter={() => setIdx(i)} onClick={() => { r.action(); onClose() }}
              style={{
                display: 'flex', alignItems: 'center', gap: 10, width: '100%', textAlign: 'start', padding: '10px 12px',
                borderRadius: 10, border: 0, background: i === idx ? 'var(--surface-2)' : 'transparent',
              }}>
              <span style={{ width: 32, height: 32, borderRadius: 9, background: i === idx ? 'var(--gold-3)' : 'var(--surface-2)', display: 'grid', placeItems: 'center', color: 'var(--gold-deep)' }}>
                <Icon name={r.icon} size={16} />
              </span>
              <span style={{ flex: 1, minWidth: 0 }}>
                <span style={{ display: 'block', fontSize: 12.5, fontWeight: 700 }}>{r.title}</span>
                {r.sub && <span className="tiny muted" style={{ direction: 'ltr' }}>{r.sub}</span>}
              </span>
              <span className="badge badge-neutral">{r.type}</span>
              <Icon name="chevronLeft" size={14} style={{ color: 'var(--muted)' }} />
            </button>
          ))}
        </div>
        <div className="modal-foot" style={{ padding: '10px 16px', fontSize: 10 }}>
          <span className="tiny muted"><span className="kbd">↑</span> <span className="kbd">↓</span> للتنقل</span>
          <span className="tiny muted"><span className="kbd">Enter</span> للفتح</span>
          <span className="tiny muted"><span className="kbd">⌘K</span> في أي وقت</span>
        </div>
      </div>
    </div>
  )
}

/* ============ بطاقة المستخدم ============ */
function ProfileSheet({ session, settings, onClose, onLogout, onTheme, notify, onPalette }) {
  const dark = settings.theme === 'dark'
  return (
    <Modal open onClose={onClose} title="حسابي" subtitle="بيانات الجلسة وإعدادات سريعة" icon="user" size="sm"
      footer={<>
        <button className="btn btn-danger" onClick={onLogout}><Icon name="logout" />تسجيل الخروج</button>
        <button className="btn btn-ghost" onClick={onClose}>إغلاق</button>
      </>}>
      <div className="stack">
        <div className="row" style={{ gap: 12 }}>
          <Avatar name={session.name} size="lg" />
          <div>
            <div style={{ fontSize: 15, fontWeight: 800 }}>{session.name}</div>
            <div className="tiny muted">{session.role === 'manager' ? 'مدير المحل — صلاحيات كاملة' : 'عامل مبيعات — فواتير فقط'}</div>
          </div>
        </div>
        <div className="sep" />
        <Toggle on={dark} onChange={() => onTheme()} label="الوضع الداكن" hint="مريح للعين أثناء العمل الليلي" />
        <button className="btn btn-subtle btn-block" style={{ justifyContent: 'space-between' }} onClick={onPalette}>
          <span className="row" style={{ gap: 8 }}><Icon name="command" />البحث الشامل</span><span className="kbd">⌘K</span>
        </button>
        <div className="card tight" style={{ background: 'var(--surface-2)' }}>
          <div className="row between"><span className="tiny muted">إصدار التطبيق</span><b className="tiny">2.0.0 — Pro</b></div>
          <div className="row between" style={{ marginTop: 6 }}><span className="tiny muted">الوضع</span><b className="tiny">يعمل دون إنترنت · بيانات محلية</b></div>
          <div className="row between" style={{ marginTop: 6 }}><span className="tiny muted">PIN</span><b className="tiny">مفعّل</b></div>
        </div>
      </div>
    </Modal>
  )
}

/* ============ شاشة القفل ============ */
function LockScreen({ notify, toastEl }) {
  const s = useStore()
  const [pin, setPin] = useState('')
  const [err, setErr] = useState(false)
  const [showHint, setShowHint] = useState(false)

  const submit = (code = pin) => {
    const user = verifyPin(code)
    if (user) {
      loginAs(user)
      notify(`أهلاً بك، ${user.name}`)
      setErr(false)
    } else {
      setErr(true)
      setTimeout(() => { setPin(''); setErr(false) }, 650)
      notify('الرقم السري غير صحيح', 'error')
    }
  }

  const press = (d) => {
    if (pin.length >= 6) return
    const next = pin + d
    setPin(next)
    const active = s.users.filter((u) => u.active)
    const exact = active.some((u) => u.pin === next)
    const couldBeLonger = active.some((u) => u.pin.length > next.length && u.pin.startsWith(next))
    if (exact && !couldBeLonger) setTimeout(() => submit(next), 140)
  }

  return (
    <div className="lock">
      <div className="lock-brand">
        <div className="brand-lg">
          <BrandMark size={62} />
          <div>
            <h1>{s.settings.shopName}</h1>
            <div className="en">MAHAMESS AL HARAMAYN · EST. 2026</div>
          </div>
        </div>
        <div className="lock-hero">
          <span className="kicker"><Icon name="sparkles" size={13} />نظام إدارة المحل — الإصدار الاحترافي</span>
          <h2>كل حبّة مكسرات،<br />محسوبة بدقّة.</h2>
          <p>فواتير سريعة، مخزون مضبوط، وأرباح واضحة بالليرة والدولار — كل ذلك يعمل على جهازك دون إنترنت.</p>
          <div className="lock-feats">
            {[['cart', 'بيع سريع بالباركود'], ['package', 'جرد وتحميص ذكي'], ['chart', 'تقارير وأرباح دقيقة'], ['shield', 'صلاحيات وسجل عمليات']].map(([ic, t]) => (
              <div className="lock-feat" key={t}><Icon name={ic} />{t}</div>
            ))}
          </div>
        </div>
        <div className="lock-stats">
          <div className="lock-stat"><b>2</b><span>عملة (ليرة / دولار)</span></div>
          <div className="lock-stat"><b>100%</b><span>يعمل بلا إنترنت</span></div>
          <div className="lock-stat"><b>v2.1</b><span>الإصدار الاحترافي</span></div>
        </div>
      </div>

      <div className="lock-form">
        <div className="lock-card">
          <div className="mobile-brand">
            <BrandMark size={44} />
            <div><b>{s.settings.shopName}</b><span>MAHAMESS AL HARAMAYN</span></div>
          </div>
          <div className="hello">أهلاً بعودتك</div>
          <h2>الدخول إلى المحل</h2>
          <p>أدخل الرقم السري الخاص بك، أو استخدم لوحة الأرقام أدناه.</p>

          <div className={`pin-dots ${err ? 'shake' : ''}`}>
            {[0, 1, 2, 3, 4, 5].map((i) => <span key={i} className={`pin-dot ${pin.length > i ? 'on' : ''} ${err ? 'err' : ''}`} />)}
          </div>

          <div className="row" style={{ gap: 6, marginBottom: 12 }}>
            <input className="input" type="password" inputMode="numeric" placeholder="الرقم السري" value={pin}
              onChange={(e) => setPin(e.target.value.replace(/\D/g, '').slice(0, 6))}
              onKeyDown={(e) => e.key === 'Enter' && submit()}
              style={{ textAlign: 'center', letterSpacing: 8, fontSize: 17, height: 46, direction: 'ltr' }} />
            <button className="btn btn-gold" style={{ height: 46, paddingInline: 20 }} onClick={() => submit()}><Icon name="arrowLeft" />دخول</button>
          </div>

          <div className="keypad">
            {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((d) => <button key={d} onClick={() => press(d)}>{d}</button>)}
            <button className="act" onClick={() => setPin('')}><Icon name="rotate" size={16} /></button>
            <button onClick={() => press('0')}>0</button>
            <button className="act" onClick={() => setPin((p) => p.slice(0, -1))}><Icon name="x" size={16} /></button>
          </div>

          <div className="row" style={{ justifyContent: 'center', marginTop: 18, gap: 6 }}>
            <button className="btn btn-ghost btn-sm" onClick={() => setShowHint((v) => !v)}><Icon name="key" />نسيت الرقم؟</button>
            <button className="btn btn-ghost btn-sm" onClick={() => { setPin('1234'); setTimeout(() => submit('1234'), 100) }}>دخول تجريبي للمدير</button>
          </div>
          {showHint && (
            <div className="card tight" style={{ marginTop: 10, background: 'var(--surface-2)', fontSize: 11, lineHeight: 1.8, color: 'var(--muted)' }}>
              المدير الافتراضي: <b style={{ color: 'var(--ink)' }}>1234</b> · العامل: <b style={{ color: 'var(--ink)' }}>1111</b><br />
              يمكن تغيير الأرقام من شاشة المستخدمين بعد الدخول.
            </div>
          )}
          <div className="center tiny muted" style={{ marginTop: 18 }}>
            بياناتك محفوظة على هذا الجهاز فقط · نسخة احتياطية من الإعدادات
          </div>
        </div>
      </div>
      {toastEl}
    </div>
  )
}
