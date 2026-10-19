import { useMemo, useState } from 'react'
import { useStore, saveItem, deleteItem, adjustStock, updatePrices } from '../store'
import { CATEGORIES, money, parseAmount, daysUntil, fmtDate, UNITS, normalizeAr } from '../utils'
import { Card, Empty, Field, Modal, Drawer, PageHead, Seg, ConfirmDialog, Toggle } from '../components'
import { Icon, categoryIcon } from '../icons'

const emptyItem = (cat = 'nuts') => ({
  id: null, name: '', category: cat, unit: 'kg', retail: 0, wholesale: 0, cost: 0,
  stock: 0, minStock: 0, barcode: '', expiry: '', packSize: 1, boxPrice: 0,
  isBasket: false, components: [], active: true,
})

export default function Items({ onToast, go }) {
  const s = useStore()
  const isManager = s.session?.role === 'manager'
  const [tab, setTab] = useState('all')
  const [q, setQ] = useState('')
  const [editing, setEditing] = useState(null)
  const [adjust, setAdjust] = useState(null)
  const [priceFor, setPriceFor] = useState(null)
  const [del, setDel] = useState(null)

  const list = useMemo(() => s.items.filter((i) => {
    if (tab === 'unpriced') return i.retail <= 0
    if (tab === 'low') return i.active && i.minStock > 0 && i.stock <= i.minStock
    if (tab !== 'all' && i.category !== tab) return false
    if (q && !normalizeAr(i.name).includes(normalizeAr(q)) && !(i.barcode || '').includes(q)) return false
    return true
  }), [s.items, tab, q])

  const stockValue = s.items.reduce((a, i) => a + (i.stock || 0) * (i.cost || 0), 0)
  const lowCount = s.items.filter((i) => i.active && i.minStock > 0 && i.stock <= i.minStock).length
  const unpricedCount = s.items.filter((i) => i.retail <= 0).length
  const stockLabel = (i) => {
    if (i.unit === 'kg') return `${(i.stock / 1000).toLocaleString('en-US', { maximumFractionDigits: 2 })} كغ`
    if (i.unit === 'box') return `${i.stock} علبة`
    return `${i.stock} قطعة`
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      <PageHead title="الأصناف والمنتجات" subtitle="أسعار المفرق والجملة والتكلفة، الوحدات، الباركود، الصلاحية وسلال الضيافة"
        actions={isManager && <>
          <button className="btn btn-ghost" onClick={() => go('pricing')}><Icon name="tag" />تسعير سريع{unpricedCount ? ` (${unpricedCount})` : ''}</button>
          <button className="btn btn-gold" onClick={() => setEditing(emptyItem(['all', 'low', 'unpriced'].includes(tab) ? 'nuts' : tab))}><Icon name="plus" />صنف جديد</button>
        </>} />

      <div className="grid g5">
        <div className="kpi"><div className="top"><span className="lbl">عدد الأصناف</span><span className="ic"><Icon name="peanut" /></span></div><div className="val">{s.items.length}</div><div className="foot">{s.items.filter((i) => i.isBasket).length} سلة ضيافة</div></div>
        <div className={`kpi ${unpricedCount ? 'gold' : ''}`}><div className="top"><span className="lbl">أصناف بلا سعر</span><span className="ic"><Icon name="tag" /></span></div><div className="val">{unpricedCount}</div><div className="foot">{unpricedCount ? 'بحاجة لتسعير' : 'كل الأصناف مسعّرة'}</div></div>
        <div className="kpi"><div className="top"><span className="lbl">قيمة المخزون بالتكلفة</span><span className="ic"><Icon name="coins" /></span></div><div className="val">{money(stockValue, 'SYP', { bare: true })}</div><div className="foot">ل.س — حسب سعر التكلفة</div></div>
        <div className="kpi gold"><div className="top"><span className="lbl">أصناف تحت الحد الأدنى</span><span className="ic"><Icon name="alert" /></span></div><div className="val">{lowCount}</div><div className="foot">تحتاج إعادة طلب</div></div>
        <div className="kpi"><div className="top"><span className="lbl">تنتهي خلال 30 يوماً</span><span className="ic"><Icon name="clock" /></span></div><div className="val">{s.items.filter((i) => i.expiry && daysUntil(i.expiry) <= 30 && daysUntil(i.expiry) !== null).length}</div><div className="foot">راجع الصلاحية</div></div>
      </div>

      <Card tight>
        <div className="row between" style={{ gap: 10 }}>
          <div className="field-icon" style={{ flex: 1, minWidth: 200 }}>
            <Icon name="search" />
            <input className="input" placeholder="ابحث بالاسم أو الباركود…" value={q} onChange={(e) => setQ(e.target.value)} />
          </div>
          <div className="row" style={{ gap: 4 }}>
            <button className="btn btn-ghost btn-sm" onClick={() => go('pricing')}><Icon name="tag" />تسعير</button>
            <button className="btn btn-ghost btn-sm" onClick={() => go('inventory')}><Icon name="warehouse" />المخزون</button>
          </div>
        </div>
        <div className="pos-cats" style={{ marginTop: 10 }}>
          <button className={`cat-pill ${tab === 'all' ? 'on' : ''}`} onClick={() => setTab('all')}><Icon name="grid" />الكل</button>
          {CATEGORIES.map((c) => <button key={c.id} className={`cat-pill ${tab === c.id ? 'on' : ''}`} onClick={() => setTab(c.id)}><Icon name={categoryIcon(c.id)} />{c.name}</button>)}
          <button className={`cat-pill ${tab === 'unpriced' ? 'on' : ''}`} onClick={() => setTab('unpriced')}><Icon name="tag" />بلا سعر</button>
          <button className={`cat-pill ${tab === 'low' ? 'on' : ''}`} onClick={() => setTab('low')}><Icon name="alert" />تحت الحد</button>
        </div>
      </Card>

      <Card flush>
        {list.length === 0 ? <Empty icon="peanut" title="لا توجد أصناف" text="أضف أول صنف لبدء البيع." /> : (
          <>
            <div className="table-wrap desktop-table" style={{ border: 0, borderRadius: 0 }}>
              <table>
                <thead><tr><th>الصنف</th><th>التصنيف</th><th>الوحدة</th><th className="num">مفرق</th><th className="num">جملة</th><th className="num">تكلفة</th><th>المخزون</th><th>الصلاحية</th><th>باركود</th><th></th></tr></thead>
                <tbody>
                  {list.map((i) => {
                    const d = daysUntil(i.expiry)
                    const low = i.minStock > 0 && i.stock <= i.minStock
                    return (
                      <tr key={i.id}>
                        <td>
                          <div className="row" style={{ gap: 8, flexWrap: 'nowrap' }}>
                            <span style={{ width: 30, height: 30, borderRadius: 9, background: 'var(--surface-2)', display: 'grid', placeItems: 'center', color: 'var(--gold-deep)' }}><Icon name={categoryIcon(i.category)} size={15} /></span>
                            <div>
                              <div className="strong">{i.name}</div>
                              {i.isBasket && <span className="badge badge-gold" style={{ fontSize: 8, marginTop: 2 }}>سلة ضيافة</span>}
                            </div>
                          </div>
                        </td>
                        <td className="small">{CATEGORIES.find((c) => c.id === i.category)?.name}</td>
                        <td className="small">{UNITS[i.unit]?.name}</td>
                        <td className="num"><button className={`price-cell ${i.retail ? '' : 'empty'}`} onClick={() => setPriceFor(i)} title="تعديل السعر">{i.retail ? money(i.retail, 'SYP', { bare: true }) : 'حدّد السعر'}</button></td>
                        <td className="num">{i.wholesale ? money(i.wholesale, 'SYP', { bare: true }) : '—'}</td>
                        <td className="num muted">{i.cost ? money(i.cost, 'SYP', { bare: true }) : '—'}</td>
                        <td>
                          <span className={`badge ${low ? 'badge-red' : 'badge-green'}`}>{stockLabel(i)}</span>
                        </td>
                        <td className="small">
                          {i.expiry ? <span style={{ color: d < 0 ? 'var(--danger)' : d < 30 ? 'var(--warn)' : 'inherit', fontWeight: d < 30 ? 700 : 500 }}>{fmtDate(i.expiry)}</span> : '—'}
                        </td>
                        <td className="small num" style={{ color: 'var(--muted)' }}>{i.barcode}</td>
                        <td>
                          <div className="row" style={{ gap: 3, flexWrap: 'nowrap' }}>
                            {isManager && <button className="btn btn-ghost btn-icon" title="تعديل" onClick={() => setEditing({ ...i, expiry: i.expiry || '' })}><Icon name="edit" /></button>}
                            <button className="btn btn-ghost btn-icon" title="تعديل المخزون" onClick={() => setAdjust(i)}><Icon name="swap" /></button>
                            {isManager && <button className="btn btn-ghost btn-icon" title="حذف" onClick={() => setDel(i)}><Icon name="trash" /></button>}
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            <div className="mobile-cards" style={{ padding: 12 }}>
              {list.map((i) => {
                const low = i.minStock > 0 && i.stock <= i.minStock
                return (
                  <div className="m-card" key={i.id}>
                    <div className="top">
                      <div className="row" style={{ gap: 8, flexWrap: 'nowrap' }}>
                        <span style={{ width: 34, height: 34, borderRadius: 10, background: 'var(--surface-2)', display: 'grid', placeItems: 'center', color: 'var(--gold-deep)' }}><Icon name={categoryIcon(i.category)} size={16} /></span>
                        <div>
                          <div className="title">{i.name}</div>
                          <div className="sub">{CATEGORIES.find((c) => c.id === i.category)?.name} · {UNITS[i.unit]?.name}</div>
                        </div>
                      </div>
                      <span className={`badge ${low ? 'badge-red' : 'badge-green'}`}>{stockLabel(i)}</span>
                    </div>
                    <div className="row" style={{ gap: 12 }}>
                      <button className="price-mini" onClick={() => setPriceFor(i)}>
                        <span className="tiny muted">مفرق</span>
                        <b className={i.retail ? '' : 'empty'}>{i.retail ? money(i.retail, 'SYP') : 'حدّد السعر'}</b>
                      </button>
                      <div><div className="tiny muted">جملة</div><b className="small">{i.wholesale ? money(i.wholesale, 'SYP') : '—'}</b></div>
                      <div><div className="tiny muted">تكلفة</div><b className="small">{i.cost ? money(i.cost, 'SYP') : '—'}</b></div>
                    </div>
                    <div className="foot">
                      <span>{i.barcode}</span>
                      <div className="row" style={{ gap: 4 }}>
                        <button className="btn btn-ghost btn-sm" onClick={() => setPriceFor(i)}><Icon name="tag" />سعر</button>
                        <button className="btn btn-ghost btn-sm" onClick={() => setAdjust(i)}><Icon name="swap" />مخزون</button>
                        {isManager && <button className="btn btn-ghost btn-sm" onClick={() => setEditing({ ...i, expiry: i.expiry || '' })}><Icon name="edit" /></button>}
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          </>
        )}
      </Card>

      {priceFor && <QuickPrice item={priceFor} onClose={() => setPriceFor(null)} onSave={(p) => { updatePrices([p]); setPriceFor(null); onToast?.('تم تحديث سعر الصنف') }} />}
      {editing && <ItemDrawer item={editing} onClose={() => setEditing(null)} onSave={(it) => { saveItem(it); setEditing(null); onToast?.('تم حفظ الصنف') }} notify={onToast} />}
      {adjust && <AdjustModal item={adjust} onClose={() => setAdjust(null)} onSave={(qty, type, note) => { adjustStock(adjust.id, qty, type, note); setAdjust(null); onToast?.('تم تعديل المخزون') }} />}
      <ConfirmDialog open={!!del} onClose={() => setDel(null)} title="حذف الصنف" text={`سيتم حذف «${del?.name}» نهائياً. الفواتير القديمة تحتفظ باسمه وسعره.`}
        confirmLabel="حذف الصنف" onConfirm={() => { deleteItem(del.id); onToast?.('تم حذف الصنف') }} />
    </div>
  )
}

function ItemDrawer({ item, onClose, onSave, notify }) {
  const s = useStore()
  const [f, setF] = useState({ ...item, expiry: item.expiry || '' })
  const [tab, setTab] = useState('basic')
  const set = (k, v) => setF((x) => ({ ...x, [k]: v }))
  const isWeight = f.unit === 'kg'
  const margin = f.retail > 0 ? ((f.retail - f.cost) / f.retail) * 100 : 0
  const profit = f.retail - f.cost

  const submit = () => {
    if (!f.name.trim()) return notify?.('أدخل اسم الصنف', 'error')
    const unitChanged = item.id && item.unit !== f.unit
    onSave({
      ...f,
      name: f.name.trim(),
      retail: Math.round(parseAmount(f.retail)),
      wholesale: Math.round(parseAmount(f.wholesale)),
      cost: Math.round(parseAmount(f.cost)),
      stock: unitChanged ? 0 : Math.round(parseAmount(f.stock)),
      minStock: Math.round(parseAmount(f.minStock)),
      boxPrice: Math.round(parseAmount(f.boxPrice)),
      packSize: Math.max(1, Math.round(parseAmount(f.packSize))),
      barcode: f.barcode || String(Math.floor(6280000000000 + Math.random() * 9999999999)),
    })
  }

  return (
    <Drawer open onClose={onClose} title={item.id ? 'تعديل صنف' : 'صنف جديد'} subtitle="بيانات التسعير والمخزون" icon="peanut" width={620}
      footer={<>
        <button className="btn btn-gold" onClick={submit}><Icon name="save" />حفظ الصنف</button>
        <button className="btn btn-ghost" onClick={onClose}>إلغاء</button>
      </>}>
      <div className="stack">
        <Seg block value={tab} onChange={setTab} options={[{ id: 'basic', label: 'البيانات الأساسية' }, { id: 'prices', label: 'التسعير والمخزون' }, { id: 'extra', label: 'الصلاحية والسلال' }]} />

        {tab === 'basic' && (
          <>
            <Field label="اسم الصنف" required><input className="input" value={f.name} onChange={(e) => set('name', e.target.value)} placeholder="مثال: فستق حلبي ممتاز" autoFocus /></Field>
            <div className="grid g2" style={{ gap: 10 }}>
              <Field label="التصنيف">
                <select className="select" value={f.category} onChange={(e) => set('category', e.target.value)}>
                  {CATEGORIES.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </Field>
              <Field label="وحدة البيع" hint={item.id && item.unit !== f.unit ? 'تغيير الوحدة سيعيد ضبط المخزون إلى صفر' : 'الكيلو للأوزان، القطعة/العلبة للمعدود'}>
                <select className="select" value={f.unit} onChange={(e) => set('unit', e.target.value)}>
                  <option value="kg">كيلوغرام (وزن)</option>
                  <option value="piece">قطعة (عدد)</option>
                  <option value="box">علبة (عدد)</option>
                </select>
              </Field>
            </div>
            <Field label="الباركود" hint="اتركه فارغاً لتوليد باركود تلقائي">
              <div className="input-group">
                <input className="input" style={{ direction: 'ltr', textAlign: 'right' }} value={f.barcode} onChange={(e) => set('barcode', e.target.value)} />
                <button className="btn btn-subtle" type="button" onClick={() => set('barcode', String(Math.floor(6280000000000 + Math.random() * 9999999999)))}><Icon name="refresh" /></button>
              </div>
            </Field>
            <Toggle on={!!f.active} onChange={(v) => set('active', v)} label="صنف نشط" hint="الأصناف الموقوفة لا تظهر في شاشة البيع" />
          </>
        )}

        {tab === 'prices' && (
          <>
            <div className="grid g3" style={{ gap: 8 }}>
              <Field label={`سعر المفرق (ل.س / ${isWeight ? 'كغ' : 'قطعة'})`}>
                <input className="input" inputMode="decimal" value={f.retail || ''} onChange={(e) => set('retail', e.target.value)} style={{ fontWeight: 800 }} />
              </Field>
              <Field label="سعر الجملة">
                <input className="input" inputMode="decimal" value={f.wholesale || ''} onChange={(e) => set('wholesale', e.target.value)} />
              </Field>
              <Field label="سعر التكلفة">
                <input className="input" inputMode="decimal" value={f.cost || ''} onChange={(e) => set('cost', e.target.value)} />
              </Field>
            </div>
            <div className="card tight" style={{ background: 'var(--surface-2)' }}>
              <div className="row between"><span className="small muted">هامش الربح على المفرق</span><b style={{ color: margin > 0 ? 'var(--success)' : 'var(--danger)', fontSize: 14 }}>{margin.toFixed(1)}%</b></div>
              <div className="row between" style={{ marginTop: 4 }}><span className="small muted">الربح لكل {isWeight ? 'كيلو' : 'قطعة'}</span><b>{money(profit, 'SYP')}</b></div>
              <div className="tiny muted" style={{ marginTop: 4 }}>سعر الجملة يعادل {f.retail ? (((f.wholesale || 0) / f.retail) * 100).toFixed(0) : 0}% من المفرق</div>
            </div>
            <div className="grid g2" style={{ gap: 10 }}>
              <Field label={isWeight ? 'المخزون (كغ)' : 'المخزون (قطعة)'}>
                <input className="input" inputMode="decimal" value={isWeight ? (f.stock / 1000) : f.stock}
                  onChange={(e) => set('stock', isWeight ? Math.round(parseAmount(e.target.value) * 1000) : Math.round(parseAmount(e.target.value)))} />
              </Field>
              <Field label={isWeight ? 'حد التنبيه (كغ)' : 'حد التنبيه (قطعة)'}>
                <input className="input" inputMode="decimal" value={isWeight ? (f.minStock / 1000) : f.minStock}
                  onChange={(e) => set('minStock', isWeight ? Math.round(parseAmount(e.target.value) * 1000) : Math.round(parseAmount(e.target.value)))} />
              </Field>
            </div>
            <div className="tiny muted" style={{ background: 'var(--surface-2)', padding: 9, borderRadius: 8, lineHeight: 1.8 }}>
              تُخزَّن الأسعار بالليرة السورية، وتُحوَّل تلقائياً إلى الدولار في فواتير الدولار بسعر الصرف المسجّل.
            </div>
          </>
        )}

        {tab === 'extra' && (
          <>
            <Field label="تاريخ الصلاحية"><input type="date" className="input" value={f.expiry || ''} onChange={(e) => set('expiry', e.target.value)} /></Field>
            <div className="sep" />
            <Toggle on={!!f.isBasket} onChange={(v) => set('isBasket', v)} label="سلة ضيافة مركّبة" hint="تجمع عدة أصناف بسعر واحد وتخصم مكوناتها من المخزون" />
            {f.isBasket && (
              <div className="card tight" style={{ background: 'var(--surface-2)' }}>
                {(f.components || []).map((c, idx) => (
                  <div key={idx} className="row" style={{ marginBottom: 8, flexWrap: 'nowrap', gap: 6 }}>
                    <select className="select" value={c.itemId} onChange={(e) => {
                      const compItem = s.items.find((x) => x.id === e.target.value)
                      const comps = [...f.components]
                      comps[idx] = { ...comps[idx], itemId: e.target.value, name: compItem?.name }
                      set('components', comps)
                    }} style={{ fontSize: 11 }}>
                      {s.items.filter((x) => !x.isBasket).map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}
                    </select>
                    <input className="input" style={{ width: 88 }} inputMode="decimal" value={c.qtyBase / 1000}
                      onChange={(e) => {
                        const comps = [...f.components]
                        comps[idx] = { ...comps[idx], qtyBase: Math.round(parseAmount(e.target.value) * 1000) }
                        set('components', comps)
                      }} />
                    <span className="tiny muted" style={{ whiteSpace: 'nowrap' }}>{UNITS[s.items.find((x) => x.id === c.itemId)?.unit]?.name === 'قطعة' ? 'قطعة' : 'كغ'}</span>
                    <button className="btn btn-ghost btn-icon" onClick={() => set('components', f.components.filter((_, i) => i !== idx))}><Icon name="x" /></button>
                  </div>
                ))}
                <button className="btn btn-subtle btn-sm" onClick={() => set('components', [...(f.components || []), { itemId: s.items.find((x) => !x.isBasket)?.id, qtyBase: 1000 }])}><Icon name="plus" />إضافة مكوّن</button>
              </div>
            )}
          </>
        )}
      </div>
    </Drawer>
  )
}

function AdjustModal({ item, onClose, onSave }) {
  const [mode, setMode] = useState('in')
  const [qty, setQty] = useState('')
  const [note, setNote] = useState('')
  const isWeight = item.unit === 'kg'
  const n = parseAmount(qty)
  const delta = mode === 'in' ? Math.round(n * (isWeight ? 1000 : 1)) : -Math.round(n * (isWeight ? 1000 : 1))
  return (
    <Modal open onClose={onClose} title="تعديل المخزون" subtitle={`${item.name} — المتوفر ${isWeight ? `${item.stock / 1000} كغ` : `${item.stock} قطعة`}`} icon="swap" size="sm"
      footer={<>
        <button className="btn btn-gold" onClick={() => { if (!n) return; onSave(delta, mode === 'in' ? 'adjust-in' : 'adjust-out', note) }}>تنفيذ التعديل</button>
        <button className="btn btn-ghost" onClick={onClose}>إلغاء</button>
      </>}>
      <div className="stack">
        <Seg block gold value={mode} onChange={setMode} options={[{ id: 'in', label: 'إضافة كمية', icon: 'plus' }, { id: 'out', label: 'خصم كمية', icon: 'minus' }]} />
        <Field label={isWeight ? 'الكمية (كغ)' : 'الكمية (قطعة)'}>
          <input className="input" inputMode="decimal" value={qty} onChange={(e) => setQty(e.target.value)} placeholder="0" autoFocus />
        </Field>
        <Field label="السبب / ملاحظة"><input className="input" value={note} onChange={(e) => setNote(e.target.value)} placeholder="جرد، تلف، مرتجع…" /></Field>
        <div className="row between" style={{ background: 'var(--surface-2)', padding: 10, borderRadius: 10 }}>
          <span className="small muted">المخزون بعد التعديل</span>
          <b>{isWeight ? `${Math.max(0, item.stock + delta) / 1000} كغ` : `${Math.max(0, item.stock + delta)} قطعة`}</b>
        </div>
      </div>
    </Modal>
  )
}


function QuickPrice({ item, onClose, onSave }) {
  const [f, setF] = useState({
    retail: item.retail ? String(item.retail) : '',
    wholesale: item.wholesale ? String(item.wholesale) : '',
    cost: item.cost ? String(item.cost) : '',
  })
  const retail = Math.round(parseAmount(f.retail))
  const cost = Math.round(parseAmount(f.cost))
  const margin = retail > 0 ? ((retail - cost) / retail) * 100 : 0
  const unitName = UNITS[item.unit]?.name || 'وحدة'
  const save = () => {
    if (retail <= 0) return
    onSave({
      id: item.id,
      retail,
      wholesale: Math.round(parseAmount(f.wholesale)),
      cost,
    })
  }
  return (
    <Modal open onClose={onClose} title={`تسعير — ${item.name}`} subtitle={`${unitName} · سعر البيع بالليرة السورية`} icon="tag" size="sm"
      footer={<>
        <button className="btn btn-gold" onClick={save} disabled={retail <= 0}><Icon name="save" />حفظ السعر</button>
        <button className="btn btn-ghost" onClick={onClose}>إلغاء</button>
      </>}>
      <div className="stack">
        <div className="grid g3" style={{ gap: 8 }}>
          <Field label={`المفرق / ${unitName}`}><input className="input" inputMode="decimal" value={f.retail} onChange={(e) => setF({ ...f, retail: e.target.value })} autoFocus style={{ fontWeight: 800 }} placeholder="0" /></Field>
          <Field label="الجملة"><input className="input" inputMode="decimal" value={f.wholesale} onChange={(e) => setF({ ...f, wholesale: e.target.value })} placeholder="0" /></Field>
          <Field label="التكلفة"><input className="input" inputMode="decimal" value={f.cost} onChange={(e) => setF({ ...f, cost: e.target.value })} placeholder="0" /></Field>
        </div>
        <div className="card tight" style={{ background: 'var(--surface-2)' }}>
          <div className="row between"><span className="small muted">هامش الربح</span><b style={{ color: margin > 0 ? 'var(--success)' : 'var(--muted)' }}>{retail > 0 ? `${margin.toFixed(1)}%` : '—'}</b></div>
          <div className="row between" style={{ marginTop: 4 }}><span className="small muted">الربح لكل {unitName}</span><b>{retail > 0 ? money(retail - cost, 'SYP') : '—'}</b></div>
        </div>
        <div className="tiny muted">يمكنك لاحقاً تعديل كل الأسعار دفعة واحدة من «التسعير السريع».</div>
      </div>
    </Modal>
  )
}
