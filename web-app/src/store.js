// ============ مخزن البيانات + منطق الأعمال ============
import { useSyncExternalStore } from 'react'
import {
  convert, invoiceTotals, lineTotal, qtyBaseOf, todayISO, nowISO, uid,
  rateToNumber, unitFactor, CURRENCIES, saveBlob,
} from './utils'
import { CATALOG } from './catalog'

const KEY = 'mahamess_alharamayn_v1'
const listeners = new Set()

let state = load()

function emptyState() {
  return {
    settings: {
      shopName: 'محامص الحرمين',
      branch: 'الفرع الرئيسي',
      phone: '',
      address: '',
      ozGrams: 250,
      defaultCurrency: 'SYP',
      rate: 1250000, // 12500.00 ل.س للدولار (×100)
      theme: 'light',
      pinEnabled: true,
      managerPin: '1234',
      autoBackup: true,
      lastBackup: null,
      invoiceFooter: 'شكراً لتعاملكم مع محامص الحرمين',
      catalogVersion: 1,
      isDemo: false,
    },
    users: [
      { id: 'u_mgr', name: 'المدير', role: 'manager', pin: '1234', active: true },
      { id: 'u_w1', name: 'أحمد شحادة', role: 'worker', pin: '1111', active: true },
    ],
    session: null,
    items: [],
    customers: [],
    suppliers: [],
    workers: [],
    invoices: [],
    payments: [],
    withdrawals: [],
    workerTx: [],
    expenses: [],
    closings: [],
    preorders: [],
    stockMoves: [],
    roasts: [],
    stocktakes: [],
    audit: [],
    seq: { sale: 1, purchase: 1 },
  }
}

/* ---------- كتالوج الأصناف (كل الكميات والأسعار تبدأ صفراً) ---------- */
export function catalogItems(existing = []) {
  const byName = new Map(existing.map((i) => [i.name, i]))
  return CATALOG.map(([name, category, unit], idx) => {
    const old = byName.get(name)
    if (old) return old
    return {
      id: uid('it'), name, category, unit,
      retail: 0, wholesale: 0, cost: 0,
      stock: 0, minStock: 0,
      barcode: `HR-${String(idx + 1).padStart(4, '0')}`,
      expiry: null, packSize: 1, boxPrice: 0,
      isBasket: false, components: [],
      active: true, createdAt: nowISO(),
    }
  })
}

/* ---------- الحالة الأولى: كتالوج جاهز بكميات وأسعار صفر ---------- */
function seed() {
  const s = emptyState()
  s.items = catalogItems()
  s.customers = [{ id: 'cash', name: 'زبون نقدي', phone: '', type: 'cash', notes: '', active: true, createdAt: nowISO() }]
  return s
}

/* ---------- بيانات تجريبية واقعية ---------- */
function demoSeed() {
  const s = emptyState()
  const now = new Date()
  const d = (offset) => {
    const x = new Date(now)
    x.setDate(x.getDate() - offset)
    return x.toISOString().slice(0, 10)
  }
  const expiry = (days) => {
    const x = new Date(now)
    x.setDate(x.getDate() + days)
    return x.toISOString().slice(0, 10)
  }

  const mkItem = (name, cat, unit, retail, wholesale, cost, stock, min, opts = {}) => ({
    id: uid('it'), name, category: cat, unit,
    retail, wholesale, cost,
    stock, minStock: min,
    barcode: opts.barcode || String(Math.floor(6280000000000 + Math.random() * 9999999999)),
    expiry: opts.expiry || null,
    packSize: opts.packSize || 1,
    boxPrice: opts.boxPrice || 0,
    isBasket: !!opts.isBasket,
    components: opts.components || [],
    active: true,
    createdAt: nowISO(),
  })

  s.items = [
    mkItem('فستق حلبي', 'nuts', 'kg', 250000, 235000, 205000, 18500, 3000, { expiry: expiry(120), boxPrice: 245000 }),
    mkItem('لوز مقشر', 'nuts', 'kg', 120000, 112000, 96000, 24000, 4000, { expiry: expiry(160) }),
    mkItem('كاجو', 'nuts', 'kg', 180000, 170000, 149000, 12000, 3000, { expiry: expiry(95) }),
    mkItem('بندق محمص', 'nuts', 'kg', 110000, 103000, 88000, 15500, 3000, { expiry: expiry(20) }),
    mkItem('جوز مجروش', 'nuts', 'kg', 95000, 89000, 76000, 8000, 2000),
    mkItem('تمر مجدول', 'nuts', 'kg', 45000, 42000, 34000, 30000, 5000, { expiry: expiry(9) }),
    mkItem('شوكولا بلجيكي مشكل', 'candy', 'kg', 85000, 79000, 64000, 9500, 2500, { expiry: expiry(210) }),
    mkItem('كاندي ملون', 'candy', 'kg', 40000, 36000, 27000, 14000, 3000, { expiry: expiry(60) }),
    mkItem('ملبس لوز', 'candy', 'kg', 62000, 58000, 47000, 6000, 1500, { expiry: expiry(150) }),
    mkItem('قهوة عربية بالهيل', 'coffee', 'kg', 90000, 84000, 68000, 11000, 2000, { expiry: expiry(300) }),
    mkItem('قهوة تركية محمصة', 'coffee', 'kg', 120000, 112000, 88000, 7000, 2000, { expiry: expiry(240) }),
    mkItem('بن برازيلي خام', 'coffee', 'kg', 0, 0, 52000, 22000, 5000),
    mkItem('علبة ضيافة صغيرة', 'hospitality', 'piece', 35000, 32000, 26000, 120, 20, { boxPrice: 0 }),
    mkItem('علبة ضيافة كبيرة', 'hospitality', 'piece', 90000, 85000, 70000, 60, 10),
    mkItem('سلة ضيافة فاخرة', 'hospitality', 'piece', 350000, 330000, 268000, 25, 5, {
      isBasket: true,
      components: [
        { itemId: 'auto0', qtyBase: 500 },
        { itemId: 'auto1', qtyBase: 500 },
        { itemId: 'auto2', qtyBase: 250 },
        { itemId: 'auto9', qtyBase: 250 },
      ],
    }),
  ]
  // ربط مكونات السلة بالمعرّفات الفعلية
  s.items[14].components = [
    { itemId: s.items[0].id, qtyBase: 500, name: 'فستق حلبي' },
    { itemId: s.items[1].id, qtyBase: 500, name: 'لوز مقشر' },
    { itemId: s.items[2].id, qtyBase: 250, name: 'كاجو' },
    { itemId: s.items[9].id, qtyBase: 250, name: 'قهوة عربية بالهيل' },
  ]

  s.customers = [
    { id: 'cash', name: 'زبون نقدي', phone: '', type: 'cash', notes: '', active: true, createdAt: nowISO() },
    { id: uid('cu'), name: 'أبو محمد الخضري', phone: '0932445566', type: 'credit', notes: 'زبون دائم', active: true, createdAt: nowISO() },
    { id: uid('cu'), name: 'مطعم الشام', phone: '0944556677', type: 'credit', notes: 'توريد أسبوعي', active: true, createdAt: nowISO() },
    { id: uid('cu'), name: 'شركة الضيافة الذهبية', phone: '0955667788', type: 'credit', notes: '', active: true, createdAt: nowISO() },
    { id: uid('cu'), name: 'أم أحمد', phone: '0966778899', type: 'credit', notes: '', active: true, createdAt: nowISO() },
  ]
  s.suppliers = [
    { id: uid('su'), name: 'مؤسسة المكسرات السورية', phone: '0931112222', notes: '', active: true, createdAt: nowISO() },
    { id: uid('su'), name: 'شركة البن البرازيلي', phone: '0942223333', notes: '', active: true, createdAt: nowISO() },
    { id: uid('su'), name: 'مصنع الشوكولا الدمشقي', phone: '0953334444', notes: '', active: true, createdAt: nowISO() },
  ]
  s.workers = [
    { id: uid('wk'), name: 'أحمد شحادة', phone: '0999888777', salary: 3500000, currency: 'SYP', active: true, createdAt: nowISO() },
    { id: uid('wk'), name: 'سامر العلي', phone: '0998777666', salary: 3200000, currency: 'SYP', active: true, createdAt: nowISO() },
  ]

  // فواتير مبيعات تاريخية (آخر 24 يوم)
  const plan = [
    [0, 0, 2, 'cash'], [0, 1, 1, 'credit'], [1, 2, 3, 'cash'], [2, 1, 2, 'credit'],
    [3, 3, 1, 'cash'], [4, 0, 4, 'credit'], [5, 2, 2, 'cash'], [6, 1, 1, 'cash'],
    [8, 0, 5, 'credit'], [9, 3, 2, 'cash'], [11, 1, 3, 'credit'], [13, 2, 1, 'cash'],
    [15, 0, 2, 'credit'], [18, 1, 4, 'cash'], [21, 0, 3, 'credit'], [24, 2, 2, 'cash'],
  ]
  let seq = 1
  plan.forEach(([dayOff, custIdx, qty, pay], i) => {
    const cust = s.customers[custIdx + 1]
    const item = s.items[(i * 3) % 12]
    const qtyMilli = qty * 1000
    const unitPrice = item.retail
    const line = {
      itemId: item.id, name: item.name, unit: item.unit, qtyMilli, unitPrice,
      discount: 0, costPerBase: item.cost,
      baseQty: Math.round((qtyMilli * unitFactor(item.unit, s.settings.ozGrams)) / 1000),
    }
    const totals = invoiceTotals([line], 'none', 0)
    const paid = pay === 'cash' ? totals.total : Math.round(totals.total / 2)
    const inv = {
      id: uid('inv'), no: `S-${String(seq).padStart(4, '0')}`, type: 'sale',
      date: d(dayOff), createdAt: `${d(dayOff)}T10:${10 + i}:00.000Z`,
      partyId: cust.id, partyName: cust.name,
      currency: i % 4 === 0 ? 'USD' : 'SYP',
      rate: s.settings.rate,
      lines: [line], discountType: 'none', discountValue: 0,
      ...totals, paid, paidCurrency: i % 4 === 0 ? 'USD' : 'SYP',
      remaining: totals.total - paid,
      status: 'completed', createdBy: 'المدير', notes: '',
    }
    if (inv.currency === 'USD') {
      // تحويل السطر إلى دولار
      inv.lines[0].unitPrice = Math.round(item.retail / rateToNumber(s.settings.rate) * 100)
      inv.lines[0].costPerBase = Math.round(item.cost / rateToNumber(s.settings.rate) * 100)
      const t2 = invoiceTotals(inv.lines, 'none', 0)
      Object.assign(inv, t2)
      inv.paid = pay === 'cash' ? t2.total : Math.round(t2.total / 2)
      inv.remaining = t2.total - inv.paid
    }
    s.invoices.push(inv)
    seq++
  })
  // فاتورة ملغاة كمثال
  s.invoices.push({
    ...s.invoices[2], id: uid('inv'), no: 'S-0099', status: 'cancelled',
    cancelReason: 'خطأ في إدخال الكمية', date: d(2), createdAt: `${d(2)}T12:00:00.000Z`,
  })
  s.seq.sale = seq

  // دفعات زبائن
  s.payments.push(
    { id: uid('pay'), partyType: 'customer', partyId: s.customers[1].id, date: d(5), currency: 'SYP', amount: 300000, note: 'دفعة جزئية', createdAt: nowISO(), createdBy: 'المدير' },
    { id: uid('pay'), partyType: 'customer', partyId: s.customers[2].id, date: d(2), currency: 'USD', amount: 15000, note: 'تصفية جزئية بالدولار', createdAt: nowISO(), createdBy: 'المدير' },
  )

  // مشتريات
  const pItem = s.items[0]
  const pLine = {
    itemId: pItem.id, name: pItem.name, unit: 'kg', qtyMilli: 10000,
    unitPrice: pItem.cost, discount: 0, costPerBase: pItem.cost,
    baseQty: 10000,
  }
  const pTotals = invoiceTotals([pLine], 'none', 0)
  s.invoices.push({
    id: uid('inv'), no: 'P-0001', type: 'purchase', date: d(7),
    createdAt: `${d(7)}T09:00:00.000Z`, partyId: s.suppliers[0].id, partyName: s.suppliers[0].name,
    currency: 'SYP', rate: s.settings.rate, lines: [pLine],
    discountType: 'none', discountValue: 0, ...pTotals,
    paid: Math.round(pTotals.total / 2), paidCurrency: 'SYP',
    remaining: pTotals.total - Math.round(pTotals.total / 2),
    status: 'completed', createdBy: 'المدير', notes: 'توريد فستق',
  })
  s.invoices.push({
    id: uid('inv'), no: 'P-0002', type: 'purchase', date: d(10),
    createdAt: `${d(10)}T09:30:00.000Z`, partyId: s.suppliers[1].id, partyName: s.suppliers[1].name,
    currency: 'USD', rate: s.settings.rate,
    lines: [{
      itemId: s.items[11].id, name: s.items[11].name, unit: 'kg', qtyMilli: 15000,
      unitPrice: Math.round(52000 / rateToNumber(s.settings.rate) * 100), discount: 0,
      costPerBase: s.items[11].cost, baseQty: 15000,
    }],
    discountType: 'none', discountValue: 0,
    ...invoiceTotals([{
      itemId: s.items[11].id, name: s.items[11].name, unit: 'kg', qtyMilli: 15000,
      unitPrice: Math.round(52000 / rateToNumber(s.settings.rate) * 100), discount: 0,
      costPerBase: s.items[11].cost, baseQty: 15000,
    }], 'none', 0),
    paid: 0, paidCurrency: 'USD', remaining: 0,
    status: 'completed', createdBy: 'المدير', notes: '',
  })
  const lastP = s.invoices[s.invoices.length - 1]
  lastP.paid = 0
  lastP.remaining = lastP.total
  s.seq.purchase = 3

  // مسحوبات صاحب المحل
  s.withdrawals.push(
    { id: uid('wd'), date: d(12), amount: 2000000, currency: 'SYP', note: 'مصاريف منزل', createdAt: nowISO(), createdBy: 'المدير' },
    { id: uid('wd'), date: d(4), amount: 500000, currency: 'SYP', note: 'مصروف شخصي', createdAt: nowISO(), createdBy: 'المدير' },
    { id: uid('wd'), date: d(20), amount: 30000, currency: 'USD', note: 'سفر', createdAt: nowISO(), createdBy: 'المدير' },
  )
  // سحوبات العمال
  s.workerTx.push(
    { id: uid('wt'), workerId: s.workers[0].id, date: d(6), type: 'advance', amount: 500000, currency: 'SYP', note: 'سلفة', createdAt: nowISO() },
    { id: uid('wt'), workerId: s.workers[0].id, date: d(1), type: 'goods', amount: 120000, currency: 'SYP', note: 'مكسرات', createdAt: nowISO() },
    { id: uid('wt'), workerId: s.workers[1].id, date: d(9), type: 'advance', amount: 300000, currency: 'SYP', note: 'سلفة', createdAt: nowISO() },
  )
  // مصاريف
  s.expenses.push(
    { id: uid('ex'), date: d(15), category: 'إيجار', amount: 5000000, currency: 'SYP', note: 'إيجار الشهر', createdAt: nowISO() },
    { id: uid('ex'), date: d(13), category: 'كهرباء', amount: 850000, currency: 'SYP', note: 'فاتورة كهرباء', createdAt: nowISO() },
    { id: uid('ex'), date: d(8), category: 'أجور', amount: 400000, currency: 'SYP', note: 'أجرة عامل إضافي', createdAt: nowISO() },
    { id: uid('ex'), date: d(3), category: 'مواصلات', amount: 150000, currency: 'SYP', note: 'توصيل طلبات', createdAt: nowISO() },
    { id: uid('ex'), date: d(2), category: 'أكياس وتغليف', amount: 250000, currency: 'SYP', note: '', createdAt: nowISO() },
  )
  // إقفال صندوق
  s.closings.push({
    id: uid('cl'), date: d(1), currency: 'SYP', opening: 1500000, systemCash: 3250000,
    actualCash: 3200000, diff: -50000, note: 'فرق بسيط', createdAt: nowISO(), createdBy: 'المدير',
  })
  // طلبات مسبقة
  s.preorders.push(
    { id: uid('po'), no: 'PO-001', customerName: 'الحاج محمود', phone: '0933555777', date: d(3), deliveryDate: d(-5), total: 1750000, deposit: 500000, currency: 'SYP', notes: 'سلال ضيافة للعرس - 5 سلال', status: 'pending', createdAt: nowISO() },
    { id: uid('po'), no: 'PO-002', customerName: 'شركة النور', phone: '0944666888', date: d(6), deliveryDate: d(-12), total: 900000, deposit: 900000, currency: 'SYP', notes: 'علب ضيافة للمؤتمر', status: 'ready', createdAt: nowISO() },
    { id: uid('po'), no: 'PO-003', customerName: 'أم رami', phone: '0955777999', date: d(14), deliveryDate: d(4), total: 350000, deposit: 100000, currency: 'SYP', notes: 'سلة عيد', status: 'delivered', createdAt: nowISO() },
  )
  // تحميص
  s.roasts.push({
    id: uid('ro'), date: d(11), rawItemId: s.items[11].id, roastedItemId: s.items[10].id,
    rawQtyBase: 5000, roastedQtyBase: 4200, notes: 'دفعة تحميص قهوة',
    createdAt: nowISO(), createdBy: 'المدير',
  })
  // جرد
  s.stocktakes.push({
    id: uid('st'), date: d(5), itemId: s.items[3].id, systemQty: 15500, countedQty: 15200,
    diff: -300, note: 'جرد أسبوعي', createdAt: nowISO(), createdBy: 'المدير',
  })
  // سجل العمليات
  s.audit = [
    { id: uid('lg'), at: nowISO(), user: 'المدير', action: 'إنشاء', entity: 'فاتورة', entityId: 'S-0001', details: 'فاتورة مبيعات بقيمة 250,000 ل.س' },
    { id: uid('lg'), at: nowISO(), user: 'المدير', action: 'تعديل', entity: 'صنف', entityId: 'فستق حلبي', details: 'تحديث سعر المفرق' },
    { id: uid('lg'), at: nowISO(), user: 'أحمد شحادة', action: 'إنشاء', entity: 'فاتورة', entityId: 'S-0004', details: 'فاتورة مبيعات' },
  ]
  s.settings.isDemo = true
  return s
}

function load() {
  try {
    const raw = localStorage.getItem(KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      const base = emptyState()
      // ترقية تلقائية من البيانات التجريبية القديمة إلى بداية نظيفة (كل شيء صفر)
      const demoFingerprint = (parsed.customers || []).some((c) => c.name === 'أبو محمد الخضري') ||
        (parsed.items || []).some((i) => i.name === 'فستق حلبي' && i.stock === 18500)
      if (demoFingerprint) {
        const clean = seed()
        try { localStorage.setItem(KEY, JSON.stringify(clean)) } catch (e) { /* ignore */ }
        return clean
      }
      const looksDemo = parsed.settings?.isDemo === undefined && (parsed.items?.length || 0) < 30 && (parsed.invoices?.length || 0) > 0
      return { ...base, ...parsed, settings: { ...base.settings, ...(parsed.settings || {}), isDemo: parsed.settings?.isDemo ?? looksDemo } }
    }
  } catch (e) {
    console.warn('failed to load store', e)
  }
  const seeded = seed()
  try { localStorage.setItem(KEY, JSON.stringify(seeded)) } catch (e) { /* ignore */ }
  return seeded
}

export function persist() {
  try { localStorage.setItem(KEY, JSON.stringify(state)) } catch (e) { console.warn(e) }
}

function emit() {
  persist()
  listeners.forEach((l) => l())
}

export function getState() { return state }

export function setState(updater, { silent } = {}) {
  const next = typeof updater === 'function' ? updater(state) : updater
  state = next
  if (!silent) emit()
}

export function subscribe(cb) {
  listeners.add(cb)
  return () => listeners.delete(cb)
}

export function useStore() {
  return useSyncExternalStore(subscribe, getState, getState)
}

/** تحميل البيانات التجريبية (للتجربة والعرض فقط). */
export function resetDemo() {
  state = demoSeed()
  emit()
}

/** إضافة أصناف الكتالوج الناقصة دون المساس بالأصناف الحالية. */
export function ensureCatalog() {
  setState((s) => {
    const items = catalogItems(s.items)
    const added = items.length - s.items.length
    return withLog({ ...s, items }, 'إضافة', 'كتالوج', 'الأصناف', `إضافة ${added} صنفاً من الكتالوج`)
  })
}

/** تصفير كل الحركات مع الإبقاء على الأصناف والزبائن والمستخدمين. */
export function resetMovements({ clearStock = true } = {}) {
  setState((s) => {
    const items = s.items.map((i) => (clearStock ? { ...i, stock: 0 } : i))
    return withLog({
      ...s, items, settings: { ...s.settings, isDemo: false },
      invoices: [], payments: [], withdrawals: [], workerTx: [], expenses: [],
      closings: [], preorders: [], stockMoves: [], roasts: [], stocktakes: [], audit: [],
      seq: { sale: 1, purchase: 1 },
    }, 'تصفير', 'النظام', 'الحركات', clearStock ? 'تصفير الحركات والمخزون' : 'تصفير الحركات')
  })
}

/** تصفير النظام بالكامل: كل الأرقام صفر مع كتالوج الأصناف. */
export function resetSystem({ keepUsers = true, keepSettings = true } = {}) {
  const base = emptyState()
  if (keepSettings) {
    const keys = ['shopName', 'branch', 'phone', 'address', 'ozGrams', 'defaultCurrency', 'rate', 'theme', 'pinEnabled', 'managerPin', 'autoBackup', 'invoiceFooter', 'catalogVersion']
    keys.forEach((k) => { if (state.settings[k] !== undefined) base.settings[k] = state.settings[k] })
  }
  base.settings.isDemo = false
  if (keepUsers && state.users?.length) base.users = state.users.map((u) => ({ ...u }))
  base.items = catalogItems().map((i) => ({ ...i, stock: 0, retail: 0, wholesale: 0, cost: 0, minStock: 0, expiry: null }))
  base.customers = [{ id: 'cash', name: 'زبون نقدي', phone: '', type: 'cash', notes: '', active: true, createdAt: nowISO() }]
  state = base
  emit()
}

/** تحديث أسعار مجموعة أصناف دفعة واحدة (شاشة التسعير السريع). */
export function updatePrices(entries) {
  if (!entries?.length) return
  setState((s) => {
    const map = new Map(entries.map((e) => [e.id, e]))
    const items = s.items.map((it) => {
      const p = map.get(it.id)
      if (!p) return it
      return {
        ...it,
        retail: p.retail !== undefined ? Math.max(0, Math.round(p.retail)) : it.retail,
        wholesale: p.wholesale !== undefined ? Math.max(0, Math.round(p.wholesale)) : it.wholesale,
        cost: p.cost !== undefined ? Math.max(0, Math.round(p.cost)) : it.cost,
        unit: p.unit || it.unit,
      }
    })
    return withLog({ ...s, items }, 'تعديل', 'الأسعار', 'التسعير السريع', `تحديث أسعار ${entries.length} صنف`)
  })
}

/** توافق قديم. */
export function clearAll() { resetSystem() }

/* ---------- السجل ---------- */
export function log(action, entity, entityId, details, user) {
  const entry = {
    id: uid('lg'), at: nowISO(),
    user: user || state.session?.name || 'المدير',
    action, entity, entityId: String(entityId || ''), details: details || '',
  }
  return entry
}

function withLog(cur, action, entity, entityId, details) {
  return { ...cur, audit: [log(action, entity, entityId, details), ...cur.audit].slice(0, 800) }
}

/* ---------- العملات ---------- */
export const rateNow = () => rateToNumber(state.settings.rate)

export function setRate(newRate) {
  const r = Math.round(Number(newRate) * 100)
  if (!r || r <= 0) return
  setState((s) => withLog({ ...s, settings: { ...s.settings, rate: r } }, 'تعديل', 'سعر الصرف', 'USD/SYP', `تحديث سعر الصرف إلى ${rateToNumber(r).toLocaleString()}`))
}

/* ---------- الأصناف ---------- */
export function saveItem(item) {
  setState((s) => {
    const exists = s.items.some((i) => i.id === item.id)
    const items = exists ? s.items.map((i) => (i.id === item.id ? { ...i, ...item } : i)) : [...s.items, { ...item, id: item.id || uid('it'), createdAt: nowISO() }]
    return withLog({ ...s, items }, exists ? 'تعديل' : 'إنشاء', 'صنف', item.name, exists ? 'تعديل بيانات الصنف' : 'إضافة صنف جديد')
  })
}

export function deleteItem(id) {
  setState((s) => withLog({ ...s, items: s.items.filter((i) => i.id !== id) }, 'حذف', 'صنف', s.items.find((i) => i.id === id)?.name || id, ''))
}

export function adjustStock(itemId, qtyBase, type, note, refId = '') {
  setState((s) => {
    const items = s.items.map((i) => (i.id === itemId ? { ...i, stock: Math.max(0, i.stock + qtyBase) } : i))
    const move = { id: uid('mv'), at: nowISO(), itemId, qtyBase, type, note: note || '', refId }
    return { ...s, items, stockMoves: [move, ...s.stockMoves].slice(0, 2000) }
  })
}

/* ---------- الزبائن والموردين ---------- */
export function saveCustomer(c) {
  setState((s) => {
    const exists = s.customers.some((x) => x.id === c.id)
    const customers = exists ? s.customers.map((x) => (x.id === c.id ? { ...x, ...c } : x)) : [...s.customers, { ...c, id: uid('cu'), createdAt: nowISO() }]
    return withLog({ ...s, customers }, exists ? 'تعديل' : 'إنشاء', 'زبون', c.name, '')
  })
}

export function saveSupplier(c) {
  setState((s) => {
    const exists = s.suppliers.some((x) => x.id === c.id)
    const suppliers = exists ? s.suppliers.map((x) => (x.id === c.id ? { ...x, ...c } : x)) : [...s.suppliers, { ...c, id: uid('su'), createdAt: nowISO() }]
    return withLog({ ...s, suppliers }, exists ? 'تعديل' : 'إنشاء', 'مورد', c.name, '')
  })
}

export function savePayment(p) {
  setState((s) => {
    const payment = { ...p, id: uid('pay'), createdAt: nowISO(), createdBy: s.session?.name || 'المدير' }
    return withLog({ ...s, payments: [payment, ...s.payments] }, 'إنشاء', 'دفعة', payment.partyId, `دفعة ${payment.amount} ${payment.currency}`)
  })
}

/* ---------- الفواتير ---------- */
export function nextNo(type) {
  const n = state.seq[type] || 1
  return `${type === 'sale' ? 'S' : 'P'}-${String(n).padStart(4, '0')}`
}

export function saveInvoice(inv) {
  setState((s) => {
    const isNew = !inv.id || !s.invoices.some((i) => i.id === inv.id)
    const seqKey = inv.type === 'sale' ? 'sale' : 'purchase'
    const no = inv.no || `${inv.type === 'sale' ? 'S' : 'P'}-${String(s.seq[seqKey] || 1).padStart(4, '0')}`
    const record = {
      ...inv,
      id: inv.id || uid('inv'),
      no,
      createdAt: inv.createdAt || nowISO(),
      updatedAt: nowISO(),
      createdBy: inv.createdBy || s.session?.name || 'المدير',
    }
    let invoices
    let items = s.items
    let moves = []
    if (isNew) {
      invoices = [record, ...s.invoices]
      // خصم/إضافة المخزون + تحديث متوسط التكلفة عند الشراء
      items = s.items.map((it) => {
        const line = record.lines.find((l) => l.itemId === it.id)
        if (!line) return it
        const sign = record.type === 'sale' ? -1 : 1
        let nextStock = Math.max(0, it.stock + sign * (line.baseQty || 0))
        let nextCost = it.cost
        if (record.type === 'purchase') {
          const factor = unitFactor(line.unit)
          const unitCostSYP = record.currency === 'USD' ? convert(line.unitPrice || 0, 'USD', 'SYP', record.rate) : (line.unitPrice || 0)
          const costPerBase = Math.round(unitCostSYP / (factor || 1))
          const totalCost = it.stock * it.cost + (line.baseQty || 0) * costPerBase
          nextCost = nextStock > 0 ? Math.round(totalCost / nextStock) : costPerBase
        }
        return { ...it, stock: nextStock, cost: nextCost }
      })
      moves = record.lines.map((line) => ({
        id: uid('mv'), at: nowISO(), itemId: line.itemId,
        qtyBase: (record.type === 'sale' ? -1 : 1) * (line.baseQty || 0),
        type: record.type, note: `فاتورة ${no}`, refId: record.id || no,
      }))
      // السلة تخصم مكوناتها أيضاً
      record.lines.filter((l) => l.isBasket).forEach((b) => {
        const basket = s.items.find((i) => i.id === b.itemId)
        if (!basket?.components?.length) return
        const times = (b.qtyMilli || 0) / 1000
        items = items.map((it) => {
          const comp = basket.components.find((c) => c.itemId === it.id)
          if (!comp) return it
          return { ...it, stock: Math.max(0, it.stock - Math.round(comp.qtyBase * times)) }
        })
      })
    } else {
      // إعادة المخزون للفاتورة القديمة ثم خصمه بالجديدة
      const old = s.invoices.find((i) => i.id === record.id)
      let restored = s.items.map((it) => {
        const line = old?.lines.find((l) => l.itemId === it.id)
        if (!line) return it
        const sign = old.type === 'sale' ? 1 : -1
        return { ...it, stock: Math.max(0, it.stock + sign * (line.baseQty || 0)) }
      })
      restored = restored.map((it) => {
        const line = record.lines.find((l) => l.itemId === it.id)
        if (!line) return it
        const sign = record.type === 'sale' ? -1 : 1
        return { ...it, stock: Math.max(0, it.stock + sign * (line.baseQty || 0)) }
      })
      items = restored
      invoices = s.invoices.map((i) => (i.id === record.id ? record : i))
    }
    const auditAction = isNew ? 'إنشاء' : 'تعديل'
    const detail = `${record.type === 'sale' ? 'فاتورة مبيعات' : 'فاتورة شراء'} رقم ${no} — ${record.total} ${record.currency}`
    return withLog(
      {
        ...s, invoices, items,
        stockMoves: moves.length ? [...moves, ...s.stockMoves].slice(0, 2000) : s.stockMoves,
        seq: isNew ? { ...s.seq, [seqKey]: (s.seq[seqKey] || 1) + 1 } : s.seq,
      },
      auditAction, 'فاتورة', no, detail,
    )
  })
}

export function cancelInvoice(id, reason) {
  setState((s) => {
    const inv = s.invoices.find((i) => i.id === id)
    if (!inv || inv.status === 'cancelled') return s
    const items = s.items.map((it) => {
      const line = inv.lines.find((l) => l.itemId === it.id)
      if (!line) return it
      const sign = inv.type === 'sale' ? 1 : -1
      return { ...it, stock: Math.max(0, it.stock + sign * (line.baseQty || 0)) }
    })
    return withLog(
      { ...s, items, invoices: s.invoices.map((i) => (i.id === id ? { ...i, status: 'cancelled', cancelReason: reason, cancelledAt: nowISO(), cancelledBy: s.session?.name || 'المدير' } : i)) },
      'إلغاء', 'فاتورة', inv.no, `سبب الإلغاء: ${reason || 'غير محدد'}`,
    )
  })
}

export function invoicesFor(type, { from, to, search, status } = {}) {
  let list = state.invoices.filter((i) => i.type === type)
  if (status && status !== 'all') list = list.filter((i) => i.status === status)
  if (from) list = list.filter((i) => i.date >= from)
  if (to) list = list.filter((i) => i.date <= to)
  if (search) {
    const q = String(search).trim()
    list = list.filter((i) => i.no.includes(q) || (i.partyName || '').includes(q) || (i.notes || '').includes(q))
  }
  return list.sort((a, b) => (b.createdAt || b.date).localeCompare(a.createdAt || a.date))
}

/* ---------- حسابات الأرصدة ---------- */
export function partyBalance(partyId, currency, partyType = 'customer') {
  const type = partyType === 'customer' ? 'sale' : 'purchase'
  const invs = state.invoices.filter((i) => i.partyId === partyId && i.type === type && i.status !== 'cancelled' && i.currency === currency)
  const invTotal = invs.reduce((s, i) => s + (i.remaining || 0), 0)
  const pays = state.payments.filter((p) => p.partyId === partyId && p.partyType === partyType && p.currency === currency)
    .reduce((s, p) => s + p.amount, 0)
  return invTotal - pays
}

export function partyStats(partyId, partyType = 'customer') {
  const type = partyType === 'customer' ? 'sale' : 'purchase'
  const invs = state.invoices.filter((i) => i.partyId === partyId && i.type === type && i.status !== 'cancelled')
  const pays = state.payments.filter((p) => p.partyId === partyId && p.partyType === partyType)
  const byCur = { SYP: 0, USD: 0 }
  invs.forEach((i) => { if (byCur[i.currency] !== undefined) byCur[i.currency] += i.remaining || 0 })
  pays.forEach((p) => { if (byCur[p.currency] !== undefined) byCur[p.currency] -= p.amount })
  return {
    invoices: invs,
    payments: pays,
    balances: byCur,
    totalSales: invs.reduce((s, i) => s + i.total, 0),
    count: invs.length,
    lastInvoice: invs.sort((a, b) => b.date.localeCompare(a.date))[0] || null,
  }
}

export function lastPriceFor(itemId, customerId) {
  const invs = state.invoices
    .filter((i) => i.type === 'sale' && i.status === 'completed' && i.partyId === customerId && i.currency)
    .sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''))
  for (const inv of invs) {
    const line = inv.lines.find((l) => l.itemId === itemId)
    if (line) return { unitPrice: line.unitPrice, unit: line.unit, currency: inv.currency, date: inv.date }
  }
  return null
}

/* ---------- العمال ---------- */
export function saveWorker(w) {
  setState((s) => {
    const exists = s.workers.some((x) => x.id === w.id)
    const workers = exists ? s.workers.map((x) => (x.id === w.id ? { ...x, ...w } : x)) : [...s.workers, { ...w, id: uid('wk'), createdAt: nowISO() }]
    return withLog({ ...s, workers }, exists ? 'تعديل' : 'إنشاء', 'عامل', w.name, '')
  })
}

export function addWorkerTx(tx) {
  setState((s) => withLog({ ...s, workerTx: [{ ...tx, id: uid('wt'), createdAt: nowISO() }, ...s.workerTx] }, 'إنشاء', 'سحب عامل', tx.workerId, `${tx.type === 'advance' ? 'سلفة' : 'بضاعة'} ${tx.amount} ${tx.currency}`))
}

export function workerStats(workerId) {
  const worker = state.workers.find((w) => w.id === workerId)
  const txs = state.workerTx.filter((t) => t.workerId === workerId)
  const byCur = { SYP: 0, USD: 0 }
  txs.forEach((t) => { if (byCur[t.currency] !== undefined) byCur[t.currency] += t.amount })
  return {
    worker, txs,
    advances: byCur,
    net: worker ? (worker.currency === 'SYP' ? worker.salary - byCur.SYP : worker.salary - byCur.USD) : 0,
  }
}

/* ---------- المسحوبات ---------- */
export function addWithdrawal(w) {
  setState((s) => withLog({ ...s, withdrawals: [{ ...w, id: uid('wd'), createdAt: nowISO(), createdBy: s.session?.name || 'المدير' }, ...s.withdrawals] }, 'إنشاء', 'مسحوب', w.id, `${w.amount} ${w.currency}`))
}

/* ---------- المصاريف ---------- */
export function addExpense(e) {
  setState((s) => withLog({ ...s, expenses: [{ ...e, id: uid('ex'), createdAt: nowISO(), createdBy: s.session?.name || 'المدير' }, ...s.expenses] }, 'إنشاء', 'مصروف', e.category, `${e.amount} ${e.currency}`))
}

/* ---------- الصندوق ---------- */
export function cashFlow(currency, from, to) {
  const inRange = (d) => (!from || d >= from) && (!to || d <= to)
  const sales = state.invoices.filter((i) => i.type === 'sale' && i.status !== 'cancelled' && inRange(i.date))
  // المقبوضات: نعتمد على العملة الفعلية المسلَّمة (tenders) إن وُجدت
  let cashIn = 0
  sales.forEach((i) => {
    if (i.tenders && i.tenders.length) {
      i.tenders.forEach((t) => { if (t.currency === currency) cashIn += t.amount })
    } else if (i.currency === currency) {
      cashIn += i.paid || 0
    }
  })
  const payIn = state.payments.filter((p) => p.partyType === 'customer' && p.currency === currency && inRange(p.date)).reduce((s, p) => s + p.amount, 0)
  const purchases = state.invoices.filter((i) => i.type === 'purchase' && i.status !== 'cancelled' && i.currency === currency && inRange(i.date))
  const cashOutPurch = purchases.reduce((s, i) => s + (i.paid || 0), 0)
  const exp = state.expenses.filter((e) => e.currency === currency && inRange(e.date)).reduce((s, e) => s + e.amount, 0)
  const wd = state.withdrawals.filter((w) => w.currency === currency && inRange(w.date)).reduce((s, w) => s + w.amount, 0)
  const wtx = state.workerTx.filter((w) => w.currency === currency && inRange(w.date)).reduce((s, w) => s + w.amount, 0)
  const out = cashOutPurch + exp + wd + wtx
  return { cashIn, payIn, out, purchases: cashOutPurch, expenses: exp, withdrawals: wd, workerTx: wtx, net: cashIn + payIn - out }
}

export function saveClosing(c) {
  setState((s) => withLog({ ...s, closings: [{ ...c, id: uid('cl'), createdAt: nowISO(), createdBy: s.session?.name || 'المدير' }, ...s.closings] }, 'إنشاء', 'إقفال صندوق', c.date, `فرق ${c.diff}`))
}

/* ---------- المخزون ---------- */
export function addStocktake(st) {
  setState((s) => {
    const diff = st.countedQty - st.systemQty
    const items = s.items.map((i) => (i.id === st.itemId ? { ...i, stock: st.countedQty } : i))
    const record = { ...st, id: uid('st'), diff, createdAt: nowISO(), createdBy: s.session?.name || 'المدير' }
    return withLog({ ...s, items, stocktakes: [record, ...s.stocktakes], stockMoves: [{ id: uid('mv'), at: nowISO(), itemId: st.itemId, qtyBase: diff, type: 'stocktake', note: st.note || 'جرد' }, ...s.stockMoves] }, 'جرد', 'مخزون', s.items.find((i) => i.id === st.itemId)?.name || '', `الفرق ${diff}`)
  })
}

/** جرد شامل: ضبط كميات عدة أصناف دفعة واحدة. */
export function applyStocktakes(entries) {
  if (!entries?.length) return
  setState((s) => {
    const map = new Map(entries.map((e) => [e.itemId, e]))
    const items = s.items.map((i) => (map.has(i.id) ? { ...i, stock: Math.max(0, map.get(i.id).countedQty) } : i))
    const at = nowISO()
    const records = entries.map((e) => ({
      id: uid('st'), date: todayISO(), itemId: e.itemId, systemQty: e.systemQty, countedQty: e.countedQty,
      diff: e.countedQty - e.systemQty, note: e.note || 'جرد شامل', createdAt: at, createdBy: s.session?.name || 'المدير',
    }))
    const moves = entries.filter((e) => e.countedQty !== e.systemQty).map((e) => ({
      id: uid('mv'), at, itemId: e.itemId, qtyBase: e.countedQty - e.systemQty, type: 'stocktake', note: 'جرد شامل',
    }))
    return withLog({ ...s, items, stocktakes: [...records, ...s.stocktakes], stockMoves: [...moves, ...s.stockMoves] },
      'جرد', 'مخزون', 'جرد شامل', `${entries.length} صنف`)
  })
}

export function addRoast(r) {
  setState((s) => {
    const raw = s.items.find((i) => i.id === r.rawItemId)
    const roasted = s.items.find((i) => i.id === r.roastedItemId)
    const loss = r.rawQtyBase - r.roastedQtyBase
    const rawCost = Math.round(raw ? raw.cost * r.rawQtyBase : 0)
    const actualCostPerKg = r.roastedQtyBase > 0 ? Math.round((rawCost * 1000) / r.roastedQtyBase) : 0
    const record = { ...r, id: uid('ro'), loss, lossPct: r.rawQtyBase ? (loss / r.rawQtyBase) * 100 : 0, rawCost, actualCostPerKg, createdAt: nowISO(), createdBy: s.session?.name || 'المدير' }
    let items = s.items.map((i) => (i.id === r.rawItemId ? { ...i, stock: Math.max(0, i.stock - r.rawQtyBase) } : i))
    if (roasted) items = items.map((i) => (i.id === r.roastedItemId ? { ...i, stock: i.stock + r.roastedQtyBase, cost: actualCostPerKg } : i))
    return withLog({ ...s, items, roasts: [record, ...s.roasts] }, 'تحميص', 'مخزون', roasted?.name || '', `الفاقد ${loss} غ — التكلفة الفعلية ${actualCostPerKg} /كغ`)
  })
}

export function lowStockItems() {
  return state.items.filter((i) => i.active && i.minStock > 0 && i.stock <= i.minStock)
}

export function expiringItems(days = 30) {
  const t = new Date(todayISO() + 'T00:00:00')
  return state.items.filter((i) => {
    if (!i.expiry || !i.active) return false
    const d = new Date(i.expiry + 'T00:00:00')
    return (d - t) / 86400000 <= days
  }).sort((a, b) => a.expiry.localeCompare(b.expiry))
}

/* ---------- الطلبات المسبقة ---------- */
export function savePreorder(p) {
  setState((s) => {
    const exists = s.preorders.some((x) => x.id === p.id)
    const list = exists ? s.preorders.map((x) => (x.id === p.id ? { ...x, ...p } : x)) : [{ ...p, id: uid('po'), createdAt: nowISO() }, ...s.preorders]
    return withLog({ ...s, preorders: list }, exists ? 'تعديل' : 'إنشاء', 'طلب مسبق', p.no || '', p.customerName || '')
  })
}

/* ---------- التقارير ---------- */
export function reportFor(from, to) {
  const inr = (d) => (!from || d >= from) && (!to || d <= to)
  const sales = state.invoices.filter((i) => i.type === 'sale' && i.status !== 'cancelled' && inr(i.date))
  const purchases = state.invoices.filter((i) => i.type === 'purchase' && i.status !== 'cancelled' && inr(i.date))
  const expenses = state.expenses.filter((e) => inr(e.date))
  const withdrawals = state.withdrawals.filter((w) => inr(w.date))
  const workerTx = state.workerTx.filter((w) => inr(w.date))
  const byCur = { SYP: {}, USD: {} }
  ;['SYP', 'USD'].forEach((c) => {
    const s = sales.filter((x) => x.currency === c)
    const p = purchases.filter((x) => x.currency === c)
    byCur[c] = {
      sales: s.reduce((a, b) => a + b.total, 0),
      salesPaid: s.reduce((a, b) => a + (b.paid || 0), 0),
      salesRemaining: s.reduce((a, b) => a + (b.remaining || 0), 0),
      profit: s.reduce((a, b) => a + (b.profit || 0), 0),
      count: s.length,
      purchases: p.reduce((a, b) => a + b.total, 0),
      purchasesPaid: p.reduce((a, b) => a + (b.paid || 0), 0),
      expenses: expenses.filter((e) => e.currency === c).reduce((a, b) => a + b.amount, 0),
      withdrawals: withdrawals.filter((w) => w.currency === c).reduce((a, b) => a + b.amount, 0),
      workerTx: workerTx.filter((w) => w.currency === c).reduce((a, b) => a + b.amount, 0),
      payments: state.payments.filter((p2) => p2.partyType === 'customer' && p2.currency === c && inr(p2.date)).reduce((a, b) => a + b.amount, 0),
    }
  })
  const allSales = byCur.SYP.sales + convert(byCur.USD.sales, 'USD', 'SYP', state.settings.rate)
  const allProfit = byCur.SYP.profit + convert(byCur.USD.profit, 'USD', 'SYP', state.settings.rate)
  const allPurch = byCur.SYP.purchases + convert(byCur.USD.purchases, 'USD', 'SYP', state.settings.rate)
  const allExp = byCur.SYP.expenses + convert(byCur.USD.expenses, 'USD', 'SYP', state.settings.rate)
  return { sales, purchases, expenses, withdrawals, workerTx, byCur, allSales, allProfit, allPurch, allExp }
}

export function topItems(from, to, limit = 10) {
  const rows = {}
  state.invoices.filter((i) => i.type === 'sale' && i.status !== 'cancelled' && (!from || i.date >= from) && (!to || i.date <= to))
    .forEach((inv) => {
      inv.lines.forEach((l) => {
        if (!rows[l.itemId]) rows[l.itemId] = { itemId: l.itemId, name: l.name, qty: 0, revenue: 0, cost: 0, profit: 0, currency: inv.currency }
        const r = rows[l.itemId]
        r.qty += l.baseQty || 0
        const rev = lineTotal(l) - (l.discount || 0)
        r.revenue += rev
        r.cost += Math.round((l.baseQty || 0) * (l.costPerBase || 0))
        r.profit += rev - Math.round((l.baseQty || 0) * (l.costPerBase || 0))
      })
    })
  return Object.values(rows).sort((a, b) => b.revenue - a.revenue).slice(0, limit)
}

/* ---------- المستخدمون ---------- */
export function verifyPin(pin) {
  const users = state.users.filter((u) => u.active)
  const u = users.find((x) => x.pin === pin)
  if (u) return u
  if (pin === state.settings.managerPin) return state.users.find((x) => x.role === 'manager')
  return null
}

export function loginAs(user) {
  setState((s) => ({ ...s, session: user }))
}

export function logout() {
  setState((s) => ({ ...s, session: null }))
}

export function setTheme(t) {
  setState((s) => ({ ...s, settings: { ...s.settings, theme: t } }))
}

export function updateSettings(patch) {
  setState((s) => withLog({ ...s, settings: { ...s.settings, ...patch } }, 'تعديل', 'الإعدادات', '', Object.keys(patch).join(', ')))
}

export function saveUser(u) {
  setState((s) => {
    const exists = s.users.some((x) => x.id === u.id)
    const users = exists ? s.users.map((x) => (x.id === u.id ? { ...x, ...u } : x)) : [...s.users, { ...u, id: uid('u') }]
    return withLog({ ...s, users }, exists ? 'تعديل' : 'إنشاء', 'مستخدم', u.name, u.role)
  })
}

/* ---------- النسخ الاحتياطي ---------- */
export function exportBackup() {
  const data = JSON.stringify({ app: 'mahamess-alharamayn', version: 1, exportedAt: nowISO(), state: { ...state, session: null } }, null, 2)
  const blob = new Blob([data], { type: 'application/json' })
  const filename = `نسخة-محامص-الحرمين-${todayISO()}.json`
  if (window.AndroidApp?.saveBase64) saveBlob(blob, filename)
  else {
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = filename
    a.click()
    URL.revokeObjectURL(a.href)
  }
  setState((s) => ({ ...s, settings: { ...s.settings, lastBackup: nowISO() } }))
}

export function importBackup(json) {
  const parsed = JSON.parse(json)
  const incoming = parsed.state || parsed
  if (!incoming.items || !incoming.invoices) throw new Error('ملف غير صالح')
  state = { ...emptyState(), ...incoming, session: null }
  emit()
}
