import { useEffect, useMemo, useState } from 'react'
import { useStore, updatePrices } from '../store'
import { CATEGORIES, UNITS, money, parseAmount, normalizeAr } from '../utils'
import { Card, PageHead, Kpi, Modal, Field, Empty } from '../components'
import { Icon, categoryIcon } from '../icons'

const fmt = (v) => (Number(v) ? Number(v).toLocaleString('en-US') : '')

export default function Pricing({ onToast, go }) {
  const s = useStore()
  const [q, setQ] = useState('')
  const [cat, setCat] = useState('all')
  const [onlyEmpty, setOnlyEmpty] = useState(false)
  const [draft, setDraft] = useState({})
  const [marginOpen, setMarginOpen] = useState(false)

  const items = useMemo(() => {
    const needle = normalizeAr(q)
    return s.items
      .filter((i) => cat === 'all' || i.category === cat)
      .filter((i) => !needle || normalizeAr(i.name).includes(needle) || (i.barcode || '').includes(q.trim()))
      .filter((i) => !onlyEmpty || (draft[i.id]?.retail ?? i.retail) <= 0)
      .sort((a, b) => {
        const av = (draft[a.id]?.retail ?? a.retail) > 0 ? 1 : 0
        const bv = (draft[b.id]?.retail ?? b.retail) > 0 ? 1 : 0
        return av - bv || a.name.localeCompare(b.name, 'ar')
      })
  }, [s.items, q, cat, onlyEmpty, draft])

  const unpriced = s.items.filter((i) => i.retail <= 0).length
  const changed = Object.keys(draft).filter((id) => {
    const it = s.items.find((x) => x.id === id)
    if (!it) return false
    const d = draft[id]
    return Number(d.retail || 0) !== it.retail || Number(d.wholesale || 0) !== it.wholesale || Number(d.cost || 0) !== it.cost
  })
  const priced = s.items.filter((i) => i.retail > 0)
  const avgMargin = priced.length
    ? priced.reduce((a, i) => a + (i.cost > 0 ? ((i.retail - i.cost) / i.retail) * 100 : 0), 0) / priced.length
    : 0

  const val = (it, key) => {
    const d = draft[it.id]
    return d && d[key] !== undefined ? d[key] : fmt(it[key])
  }

  const setPrice = (it, key, raw) => {
    const clean = raw.replace(/[^\d.]/g, '')
    setDraft((d) => ({ ...d, [it.id]: { ...(d[it.id] || {}), [key]: clean } }))
  }

  const save = () => {
    if (!changed.length) return onToast?.('لا توجد تعديلات لحفظها', 'error')
    const entries = changed.map((id) => {
      const it = s.items.find((x) => x.id === id)
      const d = draft[id]
      return {
        id,
        retail: Math.round(parseAmount(d.retail !== undefined ? d.retail : it.retail)),
        wholesale: Math.round(parseAmount(d.wholesale !== undefined ? d.wholesale : it.wholesale)),
        cost: Math.round(parseAmount(d.cost !== undefined ? d.cost : it.cost)),
      }
    })
    updatePrices(entries)
    setDraft({})
    onToast?.(`تم حفظ أسعار ${entries.length} صنف`)
  }

  useEffect(() => {
    const h = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') { e.preventDefault(); save() }
    }
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  }) // eslint-disable-line

  const nextPrice = (e) => {
    if (e.key !== 'Enter') return
    e.preventDefault()
    const inputs = [...document.querySelectorAll('input.price-input')]
    const next = inputs[inputs.indexOf(e.target) + 1]
    if (next) { next.focus(); next.select() }
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      <PageHead
        title="التسعير السريع"
        subtitle="حدّد أسعار كل الأصناف في جدول واحد — كل الأسعار تبدأ صفراً حتى تسعّرها"
        actions={<>
          <button className="btn btn-ghost" onClick={() => setMarginOpen(true)}><Icon name="percent" />تسعير بهامش</button>
          <button className="btn btn-gold" onClick={save} disabled={!changed.length}><Icon name="save" />حفظ الكل{changed.length ? ` (${changed.length})` : ''}</button>
        </>}
      />

      <div className="grid g4">
        <Kpi label="إجمالي الأصناف" value={s.items.length} icon="package" sub={`${CATEGORIES.length} تصنيفات`} />
        <Kpi label="أصناف بلا سعر" value={unpriced} icon="alert" tone={unpriced ? 'gold' : undefined} sub={unpriced ? 'بحاجة لتسعير' : 'كل الأصناف مسعّرة'} />
        <Kpi label="متوسط هامش الربح" value={`${avgMargin.toFixed(0)}%`} icon="trendUp" sub={`${priced.length} صنف مسعّر`} />
        <Kpi label="تعديلات غير محفوظة" value={changed.length} icon="edit" tone={changed.length ? 'dark' : undefined} sub={changed.length ? 'اضغط حفظ الكل' : 'كل شيء محفوظ'} />
      </div>

      {unpriced > 0 && (
        <div className="card tight" style={{ background: 'linear-gradient(150deg,rgba(227,196,124,.18),rgba(227,196,124,.05))', borderColor: 'rgba(194,152,63,.3)' }}>
          <div className="row" style={{ gap: 10 }}>
            <span className="icon-tile" style={{ background: 'var(--brass-grad)', color: '#2A1B12' }}><Icon name="sparkles" /></span>
            <div style={{ flex: 1 }}>
              <b className="small">لديك {unpriced} صنفاً بلا سعر بيع</b>
              <div className="tiny muted">استخدم «تسعير بهامش» لتسعير التكلفة + هامش الربح دفعة واحدة، أو اكتب الأسعار يدوياً في الجدول.</div>
            </div>
          </div>
        </div>
      )}

      <Card flush>
        <div style={{ padding: '14px 16px', borderBottom: '1px solid var(--line)', display: 'grid', gap: 10 }}>
          <div className="row" style={{ gap: 8, flexWrap: 'wrap' }}>
            <div className="input-group" style={{ flex: 1, minWidth: 200 }}>
              <span style={{ paddingInlineStart: 12, color: 'var(--muted)', display: 'grid', placeItems: 'center' }}><Icon name="search" size={16} /></span>
              <input className="input" placeholder="ابحث باسم الصنف أو الباركود…" value={q} onChange={(e) => setQ(e.target.value)} />
            </div>
            <button className={`btn ${onlyEmpty ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setOnlyEmpty((v) => !v)}>
              <Icon name="alert" />غير المسعّرة فقط
            </button>
          </div>
          <div className="cat-strip">
            <button className={`cat-pill ${cat === 'all' ? 'on' : ''}`} onClick={() => setCat('all')}><Icon name="grid" />الكل</button>
            {CATEGORIES.map((c) => (
              <button key={c.id} className={`cat-pill ${cat === c.id ? 'on' : ''}`} onClick={() => setCat(c.id)}>
                <Icon name={categoryIcon(c.id)} />{c.name}
              </button>
            ))}
          </div>
        </div>

        {items.length === 0 ? (
          <div style={{ padding: 24 }}><Empty icon="search" title="لا أصناف مطابقة" text="جرّب تصنيفاً آخر أو ابحث باسم مختلف." /></div>
        ) : (
          <>
            <div className="table-wrap desktop-table">
              <table className="price-table">
                <thead>
                  <tr>
                    <th style={{ minWidth: 190 }}>الصنف</th>
                    <th style={{ width: 96 }}>الوحدة</th>
                    <th style={{ width: 120 }}>التكلفة</th>
                    <th style={{ width: 130 }}>سعر المفرق</th>
                    <th style={{ width: 120 }}>سعر الجملة</th>
                    <th style={{ width: 90 }}>الهامش</th>
                    <th style={{ width: 84 }}>الحالة</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((it) => {
                    const retail = Number((draft[it.id]?.retail ?? it.retail) || 0)
                    const cost = Number((draft[it.id]?.cost ?? it.cost) || 0)
                    const m = retail > 0 ? ((retail - cost) / retail) * 100 : 0
                    const isChanged = changed.includes(it.id)
                    return (
                      <tr key={it.id} className={isChanged ? 'row-changed' : ''}>
                        <td>
                          <div className="row" style={{ gap: 8 }}>
                            <span className="cell-icon"><Icon name={categoryIcon(it.category)} size={15} /></span>
                            <div>
                              <b className="small">{it.name}</b>
                              <div className="tiny muted">{CATEGORIES.find((c) => c.id === it.category)?.name}</div>
                            </div>
                          </div>
                        </td>
                        <td className="small">{UNITS[it.unit]?.name}</td>
                        <td><input className="input price-input" inputMode="decimal" value={val(it, 'cost')} onChange={(e) => setPrice(it, 'cost', e.target.value)} onKeyDown={nextPrice} placeholder="0" /></td>
                        <td><input className="input price-input gold" inputMode="decimal" value={val(it, 'retail')} onChange={(e) => setPrice(it, 'retail', e.target.value)} onKeyDown={nextPrice} placeholder="0" /></td>
                        <td><input className="input price-input" inputMode="decimal" value={val(it, 'wholesale')} onChange={(e) => setPrice(it, 'wholesale', e.target.value)} onKeyDown={nextPrice} placeholder="0" /></td>
                        <td><b className="small" style={{ color: m > 0 ? 'var(--success)' : 'var(--muted)' }}>{retail > 0 ? `${m.toFixed(0)}%` : '—'}</b></td>
                        <td>{retail > 0 ? <span className="badge badge-green"><Icon name="check" size={11} />مسعّر</span> : <span className="badge badge-red"><Icon name="alert" size={11} />صفر</span>}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            <div className="mobile-cards" style={{ padding: 12 }}>
              {items.map((it) => {
                const retail = Number((draft[it.id]?.retail ?? it.retail) || 0)
                const cost = Number((draft[it.id]?.cost ?? it.cost) || 0)
                const m = retail > 0 ? ((retail - cost) / retail) * 100 : 0
                return (
                  <div className={`price-card ${changed.includes(it.id) ? 'row-changed' : ''}`} key={it.id}>
                    <div className="row between">
                      <div className="row" style={{ gap: 8 }}>
                        <span className="cell-icon"><Icon name={categoryIcon(it.category)} size={15} /></span>
                        <div><b className="small">{it.name}</b><div className="tiny muted">{UNITS[it.unit]?.name} · {CATEGORIES.find((c) => c.id === it.category)?.name}</div></div>
                      </div>
                      {retail > 0 ? <span className="badge badge-green">{m.toFixed(0)}%</span> : <span className="badge badge-red">صفر</span>}
                    </div>
                    <div className="grid g3" style={{ gap: 8, marginTop: 8 }}>
                      <Field label="التكلفة"><input className="input price-input" inputMode="decimal" value={val(it, 'cost')} onChange={(e) => setPrice(it, 'cost', e.target.value)} onKeyDown={nextPrice} placeholder="0" /></Field>
                      <Field label="المفرق"><input className="input price-input gold" inputMode="decimal" value={val(it, 'retail')} onChange={(e) => setPrice(it, 'retail', e.target.value)} onKeyDown={nextPrice} placeholder="0" /></Field>
                      <Field label="الجملة"><input className="input price-input" inputMode="decimal" value={val(it, 'wholesale')} onChange={(e) => setPrice(it, 'wholesale', e.target.value)} onKeyDown={nextPrice} placeholder="0" /></Field>
                    </div>
                  </div>
                )
              })}
            </div>
          </>
        )}

        {changed.length > 0 && (
          <div className="row between" style={{ padding: '12px 16px', borderTop: '1px solid var(--line)', background: 'var(--surface-2)', position: 'sticky', bottom: 0 }}>
            <span className="small"><b style={{ color: 'var(--gold-deep)' }}>{changed.length}</b> تعديل غير محفوظ</span>
            <div className="row" style={{ gap: 8 }}>
              <button className="btn btn-ghost" onClick={() => setDraft({})}>تراجع</button>
              <button className="btn btn-gold" onClick={save}><Icon name="save" />حفظ الكل</button>
            </div>
          </div>
        )}
      </Card>

      {marginOpen && <MarginModal items={s.items} onClose={() => setMarginOpen(false)} onApply={(patch) => { setDraft(patch); setMarginOpen(false); onToast?.(`تمت تعبئة ${Object.keys(patch).length} صنفاً — راجع الأسعار ثم احفظ`) }} />}

      {!s.items.length && (
        <div className="row"><button className="btn btn-ghost" onClick={() => go?.('items')}><Icon name="package" />إدارة الأصناف</button></div>
      )}
    </div>
  )
}

function MarginModal({ items, onClose, onApply }) {
  const [pct, setPct] = useState(25)
  const [wholesaleOff, setWholesaleOff] = useState(6)
  const [scope, setScope] = useState('unpriced')
  const [roundTo, setRoundTo] = useState(50)

  const apply = () => {
    const m = Math.min(95, Math.max(1, Number(pct) || 0)) / 100
    const off = Math.min(50, Math.max(0, Number(wholesaleOff) || 0)) / 100
    const r = Math.max(1, Number(roundTo) || 1)
    const patch = {}
    items.forEach((it) => {
      if (scope === 'unpriced' && it.retail > 0) return
      if (!it.cost || it.cost <= 0) return
      const raw = it.cost / (1 - m)
      const retail = Math.max(1, Math.round(raw / r) * r)
      const wholesale = Math.max(1, Math.round((retail * (1 - off)) / r) * r)
      patch[it.id] = { retail: String(retail), wholesale: String(wholesale), cost: String(it.cost) }
    })
    if (!Object.keys(patch).length) return
    onApply(patch)
  }

  const eligible = items.filter((i) => i.cost > 0 && (scope === 'all' || i.retail <= 0)).length

  return (
    <Modal open onClose={onClose} title="تسعير بهامش الربح" subtitle="يُحسب السعر من التكلفة + الهامش، ثم تقريبه"
      icon="percent" size="sm"
      footer={<>
        <button className="btn btn-gold" onClick={apply} disabled={!eligible}><Icon name="sparkles" />تطبيق على {eligible} صنف</button>
        <button className="btn btn-ghost" onClick={onClose}>إلغاء</button>
      </>}>
      <div className="stack">
        <div className="grid g2" style={{ gap: 10 }}>
          <Field label="هامش الربح %"><input className="input" inputMode="decimal" value={pct} onChange={(e) => setPct(e.target.value)} style={{ fontWeight: 800 }} /></Field>
          <Field label="خصم الجملة عن المفرق %"><input className="input" inputMode="decimal" value={wholesaleOff} onChange={(e) => setWholesaleOff(e.target.value)} /></Field>
        </div>
        <Fixture pct={pct} cost={items.find((i) => i.cost > 0)?.cost || 0} wholesaleOff={wholesaleOff} roundTo={roundTo} />
        <Field label="التقريب إلى"><select className="select" value={roundTo} onChange={(e) => setRoundTo(e.target.value)}>
          <option value="1">بدون تقريب</option>
          <option value="50">أقرب 50 ل.س</option>
          <option value="100">أقرب 100 ل.س</option>
          <option value="500">أقرب 500 ل.س</option>
          <option value="1000">أقرب 1,000 ل.س</option>
        </select></Field>
        <Field label="نطاق التطبيق">
          <select className="select" value={scope} onChange={(e) => setScope(e.target.value)}>
            <option value="unpriced">الأصناف غير المسعّرة فقط</option>
            <option value="all">كل الأصناف (سيُستبدل السعر الحالي)</option>
          </select>
        </Field>
        <div className="row" style={{ background: 'var(--info-soft)', borderRadius: 10, padding: 10, gap: 7 }}>
          <Icon name="info" size={15} style={{ color: 'var(--info)' }} />
          <span className="tiny">تُحتسب الأسعار من سعر التكلفة المسجّل لكل صنف. الأصناف التي لا تكلفة لها تُترك كما هي.</span>
        </div>
      </div>
    </Modal>
  )
}

function Fixture({ pct, cost, wholesaleOff, roundTo }) {
  const m = Math.min(95, Math.max(1, Number(pct) || 0)) / 100
  const r = Math.max(1, Number(roundTo) || 1)
  const retail = Math.max(1, Math.round((cost / (1 - m)) / r) * r)
  const wholesale = Math.max(1, Math.round((retail * (1 - (Number(wholesaleOff) || 0) / 100)) / r) * r)
  return (
    <div className="card tight" style={{ background: 'var(--gold-3)' }}>
      <div className="row between"><span className="small">مثال على تكلفة {money(cost, 'SYP')}</span><b style={{ direction: 'ltr' }}>{money(retail, 'SYP')}</b></div>
      <div className="row between" style={{ marginTop: 4 }}><span className="tiny muted">سعر الجملة</span><b className="small" style={{ direction: 'ltr' }}>{money(wholesale, 'SYP')}</b></div>
      <div className="tiny muted" style={{ marginTop: 4 }}>الربح المتوقع: {money(retail - cost, 'SYP')} لكل كيلو/قطعة</div>
    </div>
  )
}
