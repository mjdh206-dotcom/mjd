import { useMemo, useState } from 'react'
import { useStore, saveItem, deleteItem, rateNow } from '../store'
import { CATEGORIES, CURRENCIES, money, parseAmount, toMinor, daysUntil, fmtDate, uid, UNITS } from '../utils'
import { Card, Empty, Field, Modal, Tabs } from '../components'

const emptyItem = (cat = 'nuts') => ({
  id: null, name: '', category: cat, unit: 'kg', retail: 0, wholesale: 0, cost: 0,
  stock: 0, minStock: 0, barcode: '', expiry: '', packSize: 1, boxPrice: 0,
  isBasket: false, components: [], active: true,
})

export default function Items({ onToast }) {
  const s = useStore()
  const [tab, setTab] = useState('all')
  const [q, setQ] = useState('')
  const [editing, setEditing] = useState(null)

  const list = useMemo(() => s.items.filter((i) => {
    if (tab !== 'all' && i.category !== tab) return false
    if (q && !i.name.includes(q) && !i.barcode.includes(q)) return false
    return true
  }), [s.items, tab, q])

  return (
    <div className="grid" style={{ gap: 12 }}>
      <Card>
        <div className="row" style={{ justifyContent: 'space-between', marginBottom: 10 }}>
          <div className="row" style={{ flex: 1, minWidth: 220 }}>
            <input className="input" placeholder="ابحث بالاسم أو الباركود..." value={q} onChange={(e) => setQ(e.target.value)} />
          </div>
          <button className="btn btn-gold btn-sm" onClick={() => setEditing(emptyItem(tab === 'all' ? 'nuts' : tab))}>＋ صنف جديد</button>
        </div>
        <Tabs value={tab} onChange={setTab} tabs={[{ id: 'all', label: 'الكل' }, ...CATEGORIES.map((c) => ({ id: c.id, label: `${c.icon} ${c.name}` }))]} />
      </Card>

      <Card title="قائمة الأصناف" badge={list.length}>
        {list.length === 0 ? <Empty icon="🥜" text="لا توجد أصناف" /> : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>الصنف</th><th>التصنيف</th><th>الوحدة</th><th>سعر المفرق</th><th>سعر الجملة</th><th>التكلفة</th><th>المخزون</th><th>الصلاحية</th><th>الباركود</th><th></th></tr>
              </thead>
              <tbody>
                {list.map((i) => {
                  const d = daysUntil(i.expiry)
                  const low = i.minStock > 0 && i.stock <= i.minStock
                  return (
                    <tr key={i.id}>
                      <td style={{ fontWeight: 700 }}>
                        {i.name} {i.isBasket && <span className="badge badge-gold" style={{ marginInlineStart: 4 }}>سلة</span>}
                      </td>
                      <td className="small">{CATEGORIES.find((c) => c.id === i.category)?.name}</td>
                      <td className="small">{UNITS[i.unit]?.name}</td>
                      <td style={{ direction: 'ltr' }}>{i.retail ? money(i.retail, 'SYP', { bare: true }) : '—'}</td>
                      <td style={{ direction: 'ltr' }}>{i.wholesale ? money(i.wholesale, 'SYP', { bare: true }) : '—'}</td>
                      <td style={{ direction: 'ltr' }} className="small">{i.cost ? money(i.cost, 'SYP', { bare: true }) : '—'}</td>
                      <td>
                        <span className={`badge ${low ? 'badge-red' : 'badge-green'}`}>
                          {i.unit === 'kg' ? `${(i.stock / 1000).toLocaleString()} كغ` : `${i.stock} قطعة`}
                        </span>
                      </td>
                      <td className="small">
                        {i.expiry ? <span style={{ color: d < 0 ? 'var(--danger)' : d < 30 ? 'var(--gold-dark)' : 'inherit' }}>{fmtDate(i.expiry)}</span> : '—'}
                      </td>
                      <td className="small" style={{ direction: 'ltr' }}>{i.barcode}</td>
                      <td>
                        <div className="row" style={{ gap: 4 }}>
                          <button className="btn btn-outline btn-sm" onClick={() => setEditing({ ...i })}>تعديل</button>
                          {s.session?.role === 'manager' && (
                            <button className="btn btn-outline btn-sm" onClick={() => {
                              if (confirm(`حذف الصنف "${i.name}"؟`)) { deleteItem(i.id); onToast?.('تم حذف الصنف') }
                            }}>حذف</button>
                          )}
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {editing && <ItemModal item={editing} onClose={() => setEditing(null)} onSave={(it) => { saveItem(it); setEditing(null); onToast?.('تم حفظ الصنف') }} />}
    </div>
  )
}

function ItemModal({ item, onClose, onSave }) {
  const s = useStore()
  const [f, setF] = useState({ ...item, expiry: item.expiry || '' })
  const set = (k, v) => setF((x) => ({ ...x, [k]: v }))
  const isWeight = f.unit === 'kg'

  const submit = () => {
    if (!f.name.trim()) return
    onSave({
      ...f,
      retail: Math.round(parseAmount(f.retail)),
      wholesale: Math.round(parseAmount(f.wholesale)),
      cost: Math.round(parseAmount(f.cost)),
      stock: Math.round(parseAmount(f.stock) * (isWeight ? 1 : 1)),
      minStock: Math.round(parseAmount(f.minStock) * (isWeight ? 1 : 1)),
      boxPrice: Math.round(parseAmount(f.boxPrice)),
      packSize: Math.max(1, Math.round(parseAmount(f.packSize))),
    })
  }

  return (
    <Modal open onClose={onClose} wide title={item.id ? 'تعديل صنف' : 'صنف جديد'}
      footer={<>
        <button className="btn btn-gold" onClick={submit}>حفظ الصنف</button>
        <button className="btn btn-outline" onClick={onClose}>إلغاء</button>
      </>}>
      <div className="grid grid-2" style={{ gap: 10 }}>
        <Field label="اسم الصنف"><input className="input" value={f.name} onChange={(e) => set('name', e.target.value)} /></Field>
        <Field label="التصنيف">
          <select className="select" value={f.category} onChange={(e) => set('category', e.target.value)}>
            {CATEGORIES.map((c) => <option key={c.id} value={c.id}>{c.icon} {c.name}</option>)}
          </select>
        </Field>
        <Field label="وحدة البيع الأساسية">
          <select className="select" value={f.unit} onChange={(e) => set('unit', e.target.value)} disabled={!!item.id}>
            <option value="kg">كيلو (وزن)</option>
            <option value="piece">قطعة (عدد)</option>
          </select>
        </Field>
        <Field label="الباركود"><input className="input" style={{ direction: 'ltr' }} value={f.barcode} onChange={(e) => set('barcode', e.target.value)} /></Field>
        <Field label={`سعر المفرق (ل.س / ${isWeight ? 'كغ' : 'قطعة'})`}>
          <input className="input" inputMode="decimal" value={f.retail || ''} onChange={(e) => set('retail', e.target.value)} />
        </Field>
        <Field label={`سعر الجملة (ل.س / ${isWeight ? 'كغ' : 'قطعة'})`}>
          <input className="input" inputMode="decimal" value={f.wholesale || ''} onChange={(e) => set('wholesale', e.target.value)} />
        </Field>
        <Field label={`سعر التكلفة (ل.س / ${isWeight ? 'كغ' : 'قطعة'})`}>
          <input className="input" inputMode="decimal" value={f.cost || ''} onChange={(e) => set('cost', e.target.value)} />
        </Field>
        <Field label="تاريخ الصلاحية"><input type="date" className="input" value={f.expiry || ''} onChange={(e) => set('expiry', e.target.value)} /></Field>
        <Field label={isWeight ? 'المخزون الحالي (كغ)' : 'المخزون الحالي (قطعة)'}>
          <input className="input" inputMode="decimal" value={isWeight ? (f.stock / 1000) : f.stock}
            onChange={(e) => set('stock', isWeight ? Math.round(parseAmount(e.target.value) * 1000) : Math.round(parseAmount(e.target.value)))} />
        </Field>
        <Field label={isWeight ? 'حد التنبيه (كغ)' : 'حد التنبيه (قطعة)'}>
          <input className="input" inputMode="decimal" value={isWeight ? (f.minStock / 1000) : f.minStock}
            onChange={(e) => set('minStock', isWeight ? Math.round(parseAmount(e.target.value) * 1000) : Math.round(parseAmount(e.target.value)))} />
        </Field>
      </div>

      <div className="sep" />
      <div className="row" style={{ justifyContent: 'space-between' }}>
        <div>
          <div style={{ fontWeight: 800, fontSize: 13 }}>سلة ضيافة</div>
          <div className="small muted">تجمع عدة أصناف بسعر واحد وتخصم مكوناتها من المخزون</div>
        </div>
        <input type="checkbox" checked={!!f.isBasket} onChange={(e) => set('isBasket', e.target.checked)} style={{ width: 20, height: 20 }} />
      </div>

      {f.isBasket && (
        <div style={{ marginTop: 10, background: 'var(--cream)', borderRadius: 10, padding: 10 }}>
          {(f.components || []).map((c, idx) => (
            <div key={idx} className="row" style={{ marginBottom: 6, flexWrap: 'nowrap' }}>
              <select className="select" value={c.itemId}
                onChange={(e) => {
                  const compItem = s.items.find((x) => x.id === e.target.value)
                  const comps = [...f.components]
                  comps[idx] = { ...comps[idx], itemId: e.target.value, name: compItem?.name }
                  set('components', comps)
                }}>
                {s.items.filter((x) => !x.isBasket).map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}
              </select>
              <input className="input" style={{ width: 90 }} inputMode="decimal" value={c.qtyBase / 1000}
                onChange={(e) => {
                  const comps = [...f.components]
                  comps[idx] = { ...comps[idx], qtyBase: Math.round(parseAmount(e.target.value) * 1000) }
                  set('components', comps)
                }} />
              <span className="small muted" style={{ whiteSpace: 'nowrap' }}>{UNITS[s.items.find((x) => x.id === c.itemId)?.unit]?.name === 'قطعة' ? 'قطعة' : 'كغ'}</span>
              <button className="btn btn-outline btn-sm" onClick={() => set('components', f.components.filter((_, i) => i !== idx))}>✕</button>
            </div>
          ))}
          <button className="btn btn-outline btn-sm" onClick={() => set('components', [...(f.components || []), { itemId: s.items.find((x) => !x.isBasket)?.id, qtyBase: 1000 }])}>＋ إضافة مكوّن</button>
        </div>
      )}
    </Modal>
  )
}
