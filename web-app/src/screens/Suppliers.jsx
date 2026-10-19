import { useState } from 'react'
import { useStore, saveSupplier, savePayment, partyStats } from '../store'
import { money, parseAmount, fmtDate, todayISO, CURRENCIES } from '../utils'
import { Card, Empty, Field, Modal } from '../components'

export default function Suppliers({ onToast }) {
  const s = useStore()
  const [editing, setEditing] = useState(null)
  const [payFor, setPayFor] = useState(null)
  const [st, setSt] = useState(null)

  return (
    <div className="grid" style={{ gap: 12 }}>
      <Card>
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <div className="small muted">حساب كل مورد: له (مستحق لنا) وعليه (مستحق علينا)</div>
          <button className="btn btn-gold btn-sm" onClick={() => setEditing({ name: '', phone: '', notes: '', active: true })}>＋ مورد جديد</button>
        </div>
      </Card>

      <Card title="الموردون" badge={s.suppliers.length}>
        {s.suppliers.length === 0 ? <Empty icon="🏭" /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>المورد</th><th>الهاتف</th><th>فواتير</th><th>مستحق عليه ل.س</th><th>مستحق عليه $</th><th>آخر فاتورة</th><th></th></tr></thead>
              <tbody>
                {s.suppliers.map((sp) => {
                  const stt = partyStats(sp.id, 'supplier')
                  return (
                    <tr key={sp.id}>
                      <td style={{ fontWeight: 700 }}>{sp.name}</td>
                      <td className="small" style={{ direction: 'ltr' }}>{sp.phone || '—'}</td>
                      <td>{stt.count}</td>
                      <td style={{ direction: 'ltr', fontWeight: 700, color: stt.balances.SYP > 0 ? 'var(--danger)' : 'var(--success)' }}>{money(stt.balances.SYP, 'SYP', { bare: true })}</td>
                      <td style={{ direction: 'ltr', fontWeight: 700 }}>{money(stt.balances.USD, 'USD', { bare: true })}</td>
                      <td className="small">{stt.lastInvoice ? fmtDate(stt.lastInvoice.date) : '—'}</td>
                      <td>
                        <div className="row" style={{ gap: 4 }}>
                          <button className="btn btn-outline btn-sm" onClick={() => setSt({ supplier: sp, stats: stt })}>كشف حساب</button>
                          <button className="btn btn-gold btn-sm" onClick={() => setPayFor(sp)}>دفعة</button>
                          <button className="btn btn-outline btn-sm" onClick={() => setEditing({ ...sp })}>تعديل</button>
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

      {editing && (
        <Modal open onClose={() => setEditing(null)} title={editing.id ? 'تعديل مورد' : 'مورد جديد'}
          footer={<button className="btn btn-gold" onClick={() => { saveSupplier(editing); setEditing(null); onToast?.('تم الحفظ') }}>حفظ</button>}>
          <div className="grid" style={{ gap: 10 }}>
            <Field label="الاسم"><input className="input" value={editing.name} onChange={(e) => setEditing({ ...editing, name: e.target.value })} /></Field>
            <Field label="الهاتف"><input className="input" style={{ direction: 'ltr' }} value={editing.phone} onChange={(e) => setEditing({ ...editing, phone: e.target.value })} /></Field>
            <Field label="ملاحظات"><textarea className="textarea" rows={2} value={editing.notes} onChange={(e) => setEditing({ ...editing, notes: e.target.value })} /></Field>
          </div>
        </Modal>
      )}

      {payFor && <SupplierPay supplier={payFor} onClose={() => setPayFor(null)} onSave={(p) => { savePayment(p); setPayFor(null); onToast?.('تم تسجيل الدفعة للمورد') }} />}

      {st && (
        <Modal open onClose={() => setSt(null)} wide title={`كشف حساب المورد — ${st.supplier.name}`} footer={<button className="btn btn-outline" onClick={() => setSt(null)}>إغلاق</button>}>
          <div className="table-wrap">
            <table>
              <thead><tr><th>التاريخ</th><th>البيان</th><th>قيمة الفاتورة</th><th>مدفوع</th><th>متبقي</th><th>العملة</th></tr></thead>
              <tbody>
                {st.stats.invoices.map((i) => (
                  <tr key={i.id}><td>{fmtDate(i.date)}</td><td>{i.no}</td><td style={{ direction: 'ltr' }}>{money(i.total, i.currency)}</td><td style={{ direction: 'ltr' }}>{money(i.paid, i.currency)}</td><td style={{ direction: 'ltr' }}>{money(i.remaining, i.currency)}</td><td>{i.currency}</td></tr>
                ))}
                {st.stats.payments.map((p) => (
                  <tr key={p.id}><td>{fmtDate(p.date)}</td><td>دفعة{p.note ? ` — ${p.note}` : ''}</td><td>—</td><td style={{ direction: 'ltr' }}>{money(p.amount, p.currency)}</td><td>—</td><td>{p.currency}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="row" style={{ justifyContent: 'flex-end', marginTop: 10, gap: 16 }}>
            <b>الرصيد: {money(st.stats.balances.SYP, 'SYP')} + {money(st.stats.balances.USD, 'USD')}</b>
          </div>
        </Modal>
      )}
    </div>
  )
}

function SupplierPay({ supplier, onClose, onSave }) {
  const [currency, setCurrency] = useState('SYP')
  const [amount, setAmount] = useState('')
  const [date, setDate] = useState(todayISO())
  const [note, setNote] = useState('')
  return (
    <Modal open onClose={onClose} title={`دفعة لمورد — ${supplier.name}`}
      footer={<button className="btn btn-gold" onClick={() => {
        const a = Math.round(parseAmount(amount) * Math.pow(10, CURRENCIES[currency].decimals))
        if (a > 0) onSave({ partyType: 'supplier', partyId: supplier.id, date, currency, amount: a, note })
      }}>حفظ</button>}>
      <div className="grid" style={{ gap: 10 }}>
        <div className="row" style={{ gap: 6 }}>
          {['SYP', 'USD'].map((c) => <button key={c} className={`btn btn-sm ${currency === c ? 'btn-brown' : 'btn-outline'}`} style={{ flex: 1 }} onClick={() => setCurrency(c)}>{CURRENCIES[c].name}</button>)}
        </div>
        <Field label="المبلغ"><input className="input" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} /></Field>
        <Field label="التاريخ"><input type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} /></Field>
        <Field label="ملاحظة"><input className="input" value={note} onChange={(e) => setNote(e.target.value)} /></Field>
      </div>
    </Modal>
  )
}
