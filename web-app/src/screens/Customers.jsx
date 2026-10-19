import { useMemo, useState } from 'react'
import { useStore, saveCustomer, savePayment, partyStats } from '../store'
import { money, parseAmount, fmtDate, todayISO, CURRENCIES, openPrintWindow, openExternal } from '../utils'
import { Card, Empty, Field, Modal, Drawer, PageHead, StatusBadge, Avatar, Money, Seg } from '../components'
import { Icon } from '../icons'

export default function Customers({ go, onToast, notify }) {
  const s = useStore()
  const [q, setQ] = useState('')
  const [editing, setEditing] = useState(null)
  const [statement, setStatement] = useState(null)
  const [payFor, setPayFor] = useState(null)

  const list = useMemo(() => s.customers.filter((c) => !q || c.name.includes(q) || (c.phone || '').includes(q)), [s.customers, q])
  const totals = useMemo(() => {
    let sy = 0, us = 0, debtors = 0
    s.customers.filter((c) => c.id !== 'cash').forEach((c) => {
      const st = partyStats(c.id, 'customer')
      sy += st.balances.SYP; us += st.balances.USD
      if (st.balances.SYP > 0 || st.balances.USD > 0) debtors++
    })
    return { sy, us, debtors }
  }, [s.customers, s.invoices, s.payments])

  return (
    <div className="grid" style={{ gap: 16 }}>
      <PageHead title="الزبائن وكشوف الحساب" subtitle="أرصدة كل زبون بالعملتين منفصلتين، دفعات، وكشف حساب جاهز للطباعة والواتساب"
        actions={<button className="btn btn-gold" onClick={() => setEditing({ name: '', phone: '', type: 'credit', notes: '', active: true })}><Icon name="plus" />زبون جديد</button>} />

      <div className="grid g4">
        <div className="kpi"><div className="top"><span className="lbl">عدد الزبائن</span><span className="ic"><Icon name="users" /></span></div><div className="val">{s.customers.filter((c) => c.id !== 'cash').length}</div><div className="foot">إضافة إلى الزبون النقدي</div></div>
        <div className="kpi gold"><div className="top"><span className="lbl">ذمم بالليرة</span><span className="ic"><Icon name="wallet" /></span></div><div className="val">{money(totals.sy, 'SYP', { bare: true })}</div><div className="foot">ل.س مستحقة على الزبائن</div></div>
        <div className="kpi"><div className="top"><span className="lbl">ذمم بالدولار</span><span className="ic"><Icon name="dollar" /></span></div><div className="val">{money(totals.us, 'USD', { bare: true })}</div><div className="foot">$ مستحقة على الزبائن</div></div>
        <div className="kpi"><div className="top"><span className="lbl">زبائن عليهم رصيد</span><span className="ic"><Icon name="alert" /></span></div><div className="val">{totals.debtors}</div><div className="foot">يحتاجون متابعة تحصيل</div></div>
      </div>

      <Card tight>
        <div className="row between" style={{ gap: 10 }}>
          <div className="field-icon" style={{ flex: 1, minWidth: 210 }}>
            <Icon name="search" />
            <input className="input" placeholder="ابحث عن زبون بالاسم أو الهاتف…" value={q} onChange={(e) => setQ(e.target.value)} />
          </div>
          <div className="row" style={{ gap: 6 }}>
            <button className="btn btn-ghost btn-sm" onClick={() => go('sale-new')}><Icon name="cart" />بيع سريع</button>
            <span className="badge badge-neutral">{list.length} زبون</span>
          </div>
        </div>
      </Card>

      <Card flush>
        {list.length === 0 ? <Empty icon="users" title="لا زبائن مطابقون" /> : (
          <>
            <div className="table-wrap desktop-table" style={{ border: 0, borderRadius: 0 }}>
              <table>
                <thead><tr><th>الزبون</th><th>الهاتف</th><th>النوع</th><th className="num">فواتير</th><th className="num">رصيد ل.س</th><th className="num">رصيد $</th><th>آخر فاتورة</th><th></th></tr></thead>
                <tbody>
                  {list.map((c) => {
                    const st = partyStats(c.id, 'customer')
                    const debt = st.balances.SYP > 0 || st.balances.USD > 0
                    return (
                      <tr key={c.id}>
                        <td>
                          <div className="row" style={{ gap: 8, flexWrap: 'nowrap' }}>
                            <Avatar name={c.name} size="sm" />
                            <div>
                              <div className="strong">{c.name}</div>
                              {c.notes && <div className="tiny muted">{c.notes}</div>}
                            </div>
                          </div>
                        </td>
                        <td className="num" style={{ textAlign: 'start' }}>{c.phone || '—'}</td>
                        <td><span className={`badge ${c.type === 'cash' ? 'badge-neutral' : 'badge-gold'}`}>{c.type === 'cash' ? 'نقدي' : 'آجل'}</span></td>
                        <td className="num">{st.count}</td>
                        <td className="num" style={{ fontWeight: 700, color: st.balances.SYP > 0 ? 'var(--danger)' : 'var(--success)' }}>{money(st.balances.SYP, 'SYP', { bare: true })}</td>
                        <td className="num" style={{ fontWeight: 700, color: st.balances.USD > 0 ? 'var(--danger)' : 'var(--success)' }}>{money(st.balances.USD, 'USD', { bare: true })}</td>
                        <td className="small">{st.lastInvoice ? fmtDate(st.lastInvoice.date) : '—'}</td>
                        <td>
                          <div className="row" style={{ gap: 3, flexWrap: 'nowrap' }}>
                            {c.id !== 'cash' && (
                              <>
                                <button className="btn btn-ghost btn-sm" onClick={() => setStatement(c)}><Icon name="book" />كشف</button>
                                <button className={`btn btn-sm ${debt ? 'btn-gold' : 'btn-ghost'}`} onClick={() => setPayFor(c)}><Icon name="cash" />دفعة</button>
                              </>
                            )}
                            <button className="btn btn-ghost btn-icon" title="تعديل" onClick={() => setEditing({ ...c })}><Icon name="edit" /></button>
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            <div className="mobile-cards" style={{ padding: 12 }}>
              {list.map((c) => {
                const st = partyStats(c.id, 'customer')
                const debt = st.balances.SYP > 0 || st.balances.USD > 0
                return (
                  <div className="m-card" key={c.id}>
                    <div className="top">
                      <div className="row" style={{ gap: 9, flexWrap: 'nowrap' }}>
                        <Avatar name={c.name} />
                        <div>
                          <div className="title">{c.name}</div>
                          <div className="sub">{c.phone || 'بدون هاتف'} · {c.type === 'cash' ? 'نقدي' : 'آجل'}</div>
                        </div>
                      </div>
                      <span className={`badge ${debt ? 'badge-red' : 'badge-green'}`}>{debt ? 'عليه رصيد' : 'مسدّد'}</span>
                    </div>
                    <div className="row" style={{ gap: 16 }}>
                      <div><div className="tiny muted">رصيد ل.س</div><b style={{ fontSize: 13, color: st.balances.SYP > 0 ? 'var(--danger)' : 'var(--success)' }}>{money(st.balances.SYP, 'SYP')}</b></div>
                      <div><div className="tiny muted">رصيد $</div><b style={{ fontSize: 13, color: st.balances.USD > 0 ? 'var(--danger)' : 'var(--success)' }}>{money(st.balances.USD, 'USD')}</b></div>
                    </div>
                    <div className="foot">
                      <span>{st.count} فاتورة</span>
                      <div className="row" style={{ gap: 4 }}>
                        <button className="btn btn-ghost btn-sm" onClick={() => setStatement(c)}>كشف</button>
                        {c.id !== 'cash' && <button className="btn btn-gold btn-sm" onClick={() => setPayFor(c)}>دفعة</button>}
                        <button className="btn btn-ghost btn-sm" onClick={() => setEditing({ ...c })}><Icon name="edit" /></button>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          </>
        )}
      </Card>

      {editing && (
        <Modal open onClose={() => setEditing(null)} title={editing.id ? 'تعديل زبون' : 'زبون جديد'} icon="user" size="sm"
          footer={<>
            <button className="btn btn-gold" onClick={() => {
              if (!editing.name.trim()) return onToast?.('أدخل اسم الزبون', 'error')
              saveCustomer(editing); setEditing(null); onToast?.('تم حفظ الزبون')
            }}>حفظ</button>
            <button className="btn btn-ghost" onClick={() => setEditing(null)}>إلغاء</button>
          </>}>
          <div className="stack">
            <Field label="الاسم" required><input className="input" value={editing.name} onChange={(e) => setEditing({ ...editing, name: e.target.value })} autoFocus /></Field>
            <Field label="الهاتف"><input className="input" style={{ direction: 'ltr' }} value={editing.phone} onChange={(e) => setEditing({ ...editing, phone: e.target.value })} placeholder="09xxxxxxxx" /></Field>
            <Field label="نوع الحساب">
              <Seg block value={editing.type} onChange={(v) => setEditing({ ...editing, type: v })} options={[{ id: 'credit', label: 'آجل (كشف حساب)' }, { id: 'cash', label: 'نقدي' }]} />
            </Field>
            <Field label="ملاحظات"><input className="input" value={editing.notes || ''} onChange={(e) => setEditing({ ...editing, notes: e.target.value })} /></Field>
          </div>
        </Modal>
      )}

      {statement && <StatementDrawer customer={statement} onClose={() => setStatement(null)} onPay={() => { setPayFor(statement); setStatement(null) }} onToast={onToast} go={go} store={s} />}
      {payFor && <PaymentModal customer={payFor} onClose={() => setPayFor(null)} onSave={(p) => { savePayment(p); setPayFor(null); onToast?.('تم تسجيل الدفعة') }} />}
    </div>
  )
}

function PaymentModal({ customer, onClose, onSave }) {
  const s = useStore()
  const st = partyStats(customer.id, 'customer')
  const [form, setForm] = useState({ date: todayISO(), amount: '', currency: st.balances.SYP > 0 ? 'SYP' : 'USD', note: '' })
  const amount = Math.round(parseAmount(form.amount) * Math.pow(10, CURRENCIES[form.currency].decimals))
  return (
    <Modal open onClose={onClose} title="تسجيل دفعة" subtitle={`${customer.name} — رصيد ${money(st.balances.SYP, 'SYP')} + ${money(st.balances.USD, 'USD')}`} icon="cash" size="sm"
      footer={<>
        <button className="btn btn-gold" onClick={() => {
          if (amount <= 0) return
          onSave({ partyType: 'customer', partyId: customer.id, date: form.date, currency: form.currency, amount, note: form.note })
        }}><Icon name="check" />تسجيل الدفعة</button>
        <button className="btn btn-ghost" onClick={onClose}>إلغاء</button>
      </>}>
      <div className="stack">
        <div className="grid g2" style={{ gap: 8 }}>
          <div className="card tight" style={{ background: 'var(--danger-soft)' }}><div className="tiny muted">رصيد الليرة</div><b style={{ direction: 'ltr', display: 'block', color: 'var(--danger)' }}>{money(st.balances.SYP, 'SYP')}</b></div>
          <div className="card tight" style={{ background: 'var(--info-soft)' }}><div className="tiny muted">رصيد الدولار</div><b style={{ direction: 'ltr', display: 'block', color: 'var(--info)' }}>{money(st.balances.USD, 'USD')}</b></div>
        </div>
        <div className="grid g2" style={{ gap: 8 }}>
          <Field label="المبلغ"><input className="input" inputMode="decimal" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} autoFocus style={{ fontSize: 16, fontWeight: 800 }} /></Field>
          <Field label="العملة"><select className="select" value={form.currency} onChange={(e) => setForm({ ...form, currency: e.target.value })}><option value="SYP">ليرة سورية</option><option value="USD">دولار</option></select></Field>
        </div>
        <Field label="التاريخ"><input type="date" className="input" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} /></Field>
        <Field label="ملاحظة"><input className="input" value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} placeholder="دفعة جزئية، تصفية…" /></Field>
        {amount > 0 && <div className="row" style={{ background: 'var(--gold-3)', borderRadius: 10, padding: 10, gap: 7 }}><Icon name="info" size={15} style={{ color: 'var(--gold-deep)' }} /><span className="tiny" style={{ fontWeight: 700 }}>سيُخصم المبلغ من رصيد {form.currency === 'SYP' ? 'الليرة' : 'الدولار'} فقط.</span></div>}
      </div>
    </Modal>
  )
}

function StatementDrawer({ customer, onClose, onPay, onToast, go, store }) {
  const s = store
  const [cur, setCur] = useState('SYP')
  const st = partyStats(customer.id, 'customer')

  const entries = useMemo(() => {
    const invs = st.invoices.filter((i) => i.currency === cur).map((i) => ({ id: i.id, date: i.date, type: 'inv', label: `فاتورة ${i.no}`, debit: i.total, credit: i.paid || 0, ref: i }))
    const pays = st.payments.filter((p) => p.currency === cur).map((p) => ({ id: p.id, date: p.date, type: 'pay', label: p.note || 'دفعة', debit: 0, credit: p.amount }))
    const all = [...invs, ...pays].sort((a, b) => a.date.localeCompare(b.date))
    let bal = 0
    return all.map((e) => { bal += e.debit - e.credit; return { ...e, running: bal } })
  }, [st, cur])

  const shareText = () => {
    const lines = entries.map((e) => `${e.date} — ${e.label}: ${e.debit ? `عليه ${money(e.debit, cur)}` : `له ${money(e.credit, cur)}`}`)
    return [`*${s.settings.shopName}*`, `كشف حساب: ${customer.name}`, `العملة: ${CURRENCIES[cur].name}`, '', ...lines, '', `*الرصيد النهائي: ${money(st.balances[cur], cur)}*`, s.settings.phone ? `للتواصل: ${s.settings.phone}` : ''].filter(Boolean).join('\n')
  }

  const printStatement = () => {
    const rows = entries.map((e) => `<tr><td>${e.date}</td><td>${e.label}</td><td>${e.debit ? money(e.debit, cur) : '—'}</td><td>${e.credit ? money(e.credit, cur) : '—'}</td><td>${money(e.running, cur)}</td></tr>`).join('')
    const html = `<!doctype html><html dir="rtl" lang="ar"><head><meta charset="utf-8"><title>كشف حساب — ${customer.name}</title>
      <style>body{font-family:Tahoma,Arial,sans-serif;padding:30px;color:#2b1c12}h1{font-size:22px;margin:0} .muted{color:#8b7967;font-size:11px}table{width:100%;border-collapse:collapse;margin-top:18px;font-size:12px}th,td{border-bottom:1px solid #ddd;padding:8px;text-align:right}th{background:#f6f0e4} .tot{margin-top:16px;padding:12px;border:1px solid #c2983f;border-radius:8px;display:flex;justify-content:space-between;font-weight:bold}</style></head>
      <body><div style="display:flex;justify-content:space-between;align-items:flex-start"><div><h1>${s.settings.shopName}</h1><div class="muted">${s.settings.address || ''} · ${s.settings.phone || ''}</div></div><div class="muted">${new Date().toLocaleDateString('ar-SY')}</div></div>
      <hr style="border:0;border-top:2px solid #c2983f;margin:14px 0">
      <h2 style="font-size:16px">كشف حساب: ${customer.name}</h2><div class="muted">${customer.phone || ''} · العملة: ${CURRENCIES[cur].name}</div>
      <table><thead><tr><th>التاريخ</th><th>البيان</th><th>مدين</th><th>دائن</th><th>الرصيد</th></tr></thead><tbody>${rows || '<tr><td colspan="5">لا حركات</td></tr>'}</tbody></table>
      <div class="tot"><span>الرصيد النهائي</span><span>${money(st.balances[cur], cur)}</span></div>
      <p class="muted" style="margin-top:20px;text-align:center">${s.settings.invoiceFooter}</p>
      <script>window.onload=()=>window.print()</script></body></html>`
    if (!openPrintWindow(html)) onToast?.('تعذر فتح نافذة الطباعة', 'error')
  }

  const wa = () => {
    const phone = (customer.phone || '').replace(/\D/g, '')
    openExternal(phone ? `https://wa.me/${phone.startsWith('963') ? phone : '963' + phone.replace(/^0/, '')}?text=${encodeURIComponent(shareText())}` : `https://wa.me/?text=${encodeURIComponent(shareText())}`)
  }

  return (
    <Drawer open onClose={onClose} title={`كشف حساب — ${customer.name}`} subtitle={`${customer.phone || 'بدون هاتف'} · ${st.count} فاتورة`} icon="book" width={560}
      footer={<>
        <button className="btn btn-gold" onClick={onPay}><Icon name="cash" />دفعة</button>
        <button className="btn btn-ghost" onClick={wa}><Icon name="whatsapp" />واتساب</button>
        <button className="btn btn-ghost" onClick={printStatement}><Icon name="printer" />طباعة</button>
        <button className="btn btn-ghost" onClick={() => { onClose(); go('sale-new') }}><Icon name="cart" /></button>
      </>}>
      <div className="stack">
        <div className="grid g2" style={{ gap: 8 }}>
          <div className="card tight" style={{ background: 'var(--surface-2)' }}><div className="tiny muted">رصيد الليرة</div><b style={{ fontSize: 16, color: st.balances.SYP > 0 ? 'var(--danger)' : 'var(--success)' }}>{money(st.balances.SYP, 'SYP')}</b></div>
          <div className="card tight" style={{ background: 'var(--surface-2)' }}><div className="tiny muted">رصيد الدولار</div><b style={{ fontSize: 16, color: st.balances.USD > 0 ? 'var(--danger)' : 'var(--success)' }}>{money(st.balances.USD, 'USD')}</b></div>
        </div>
        <Seg block value={cur} onChange={setCur} options={[{ id: 'SYP', label: 'كشف الليرة' }, { id: 'USD', label: 'كشف الدولار' }]} />
        <div className="table-wrap">
          <table>
            <thead><tr><th>التاريخ</th><th>البيان</th><th className="num">مدين</th><th className="num">دائن</th><th className="num">الرصيد</th></tr></thead>
            <tbody>
              {entries.length === 0 && <tr><td colSpan={5} className="table-empty">لا حركات بهذه العملة</td></tr>}
              {entries.map((e) => (
                <tr key={e.id}>
                  <td className="small">{fmtDate(e.date)}</td>
                  <td>
                    <b className="small">{e.label}</b>
                    {e.type === 'inv' && <div className="tiny muted">{e.ref.lines?.length} بند</div>}
                  </td>
                  <td className="num" style={{ color: 'var(--danger)' }}>{e.debit ? money(e.debit, cur, { bare: true }) : '—'}</td>
                  <td className="num" style={{ color: 'var(--success)' }}>{e.credit ? money(e.credit, cur, { bare: true }) : '—'}</td>
                  <td className="num strong">{money(e.running, cur, { bare: true })}</td>
                </tr>
              ))}
            </tbody>
            {entries.length > 0 && (
              <tfoot className="tfoot"><tr><td colSpan={4}>الرصيد النهائي</td><td className="num">{money(st.balances[cur], cur)}</td></tr></tfoot>
            )}
          </table>
        </div>
        <div className="tiny muted">الرصيد يشمل مجموع المتبقي من الفواتير مطروحاً منه الدفعات المسجّلة.</div>
      </div>
    </Drawer>
  )
}
