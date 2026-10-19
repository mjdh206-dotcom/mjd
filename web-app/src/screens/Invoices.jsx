import { useMemo, useState } from 'react'
import { useStore, invoicesFor, cancelInvoice } from '../store'
import { money, fmtDate, todayISO } from '../utils'
import { Card, Empty, Modal, StatusBadge, Field } from '../components'

export default function Invoices({ go, onToast, type = 'sale' }) {
  const s = useStore()
  const isManager = s.session?.role === 'manager'
  const [search, setSearch] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [status, setStatus] = useState('all')
  const [cancelId, setCancelId] = useState(null)
  const [reason, setReason] = useState('')

  const list = useMemo(() => invoicesFor(type, { from, to, search, status }), [s, type, from, to, search, status])
  const totals = useMemo(() => {
    const t = { SYP: 0, USD: 0, rem: 0 }
    list.filter((i) => i.status !== 'cancelled').forEach((i) => { t[i.currency] += i.total })
    return t
  }, [list])

  return (
    <div className="grid" style={{ gap: 12 }}>
      <Card>
        <div className="grid" style={{ gridTemplateColumns: '2fr 1fr 1fr 1fr', gap: 8 }}>
          <Field label="بحث (رقم الفاتورة / الزبون)">
            <input className="input" placeholder="ابحث..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </Field>
          <Field label="من تاريخ"><input type="date" className="input" value={from} onChange={(e) => setFrom(e.target.value)} /></Field>
          <Field label="إلى تاريخ"><input type="date" className="input" value={to} onChange={(e) => setTo(e.target.value)} /></Field>
          <Field label="الحالة">
            <select className="select" value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="all">الكل</option>
              <option value="completed">مكتملة</option>
              <option value="cancelled">ملغاة</option>
            </select>
          </Field>
        </div>
        <div className="row" style={{ marginTop: 8, gap: 8 }}>
          <span className="chip">العدد: <b>{list.length}</b></span>
          <span className="chip">ليرة: <b>{money(totals.SYP, 'SYP')}</b></span>
          <span className="chip">دولار: <b>{money(totals.USD, 'USD')}</b></span>
          <button className="btn btn-outline btn-sm" onClick={() => { setFrom(''); setTo(''); setSearch(''); setStatus('all') }}>مسح الفلاتر</button>
          <button className="btn btn-gold btn-sm" onClick={() => go('sale-new')}>＋ فاتورة جديدة</button>
        </div>
      </Card>

      <Card>
        {list.length === 0 ? <Empty icon="🧾" text="لا توجد فواتير مطابقة" /> : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>الرقم</th><th>التاريخ</th><th>الزبون</th><th>العملة</th><th>الإجمالي</th><th>المدفوع</th><th>المتبقي</th><th>الحالة</th><th></th></tr>
              </thead>
              <tbody>
                {list.map((i) => (
                  <tr key={i.id}>
                    <td style={{ fontWeight: 800 }}>{i.no}</td>
                    <td>{fmtDate(i.date)}</td>
                    <td>{i.partyName}</td>
                    <td>{i.currency === 'SYP' ? 'ل.س' : '$'}</td>
                    <td style={{ direction: 'ltr', fontWeight: 700 }}>{money(i.total, i.currency, { bare: true })}</td>
                    <td style={{ direction: 'ltr' }}>{money(i.paid || 0, i.currency, { bare: true })}</td>
                    <td style={{ direction: 'ltr', color: i.remaining > 0 ? 'var(--danger)' : 'inherit' }}>{money(i.remaining || 0, i.currency, { bare: true })}</td>
                    <td><StatusBadge status={i.status} /></td>
                    <td>
                      <div className="row" style={{ gap: 4 }}>
                        <button className="btn btn-outline btn-sm" onClick={() => go('invoice-view', { id: i.id })}>عرض</button>
                        {isManager && i.status !== 'cancelled' && (
                          <>
                            <button className="btn btn-outline btn-sm" onClick={() => go('sale-new', { id: i.id })}>تعديل</button>
                            <button className="btn btn-outline btn-sm" onClick={() => { setCancelId(i.id); setReason('') }}>إلغاء</button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Modal open={!!cancelId} onClose={() => setCancelId(null)} title="إلغاء الفاتورة"
        footer={<>
          <button className="btn btn-brown" onClick={() => { cancelInvoice(cancelId, reason); setCancelId(null); onToast?.('تم إلغاء الفاتورة وإرجاع المخزون') }}>تأكيد الإلغاء</button>
          <button className="btn btn-outline" onClick={() => setCancelId(null)}>رجوع</button>
        </>}>
        <p className="small" style={{ marginBottom: 10 }}>سيتم إرجاع الكميات إلى المخزون، وتبقى الفاتورة في السجل بحالة «ملغاة».</p>
        <Field label="سبب الإلغاء">
          <input className="input" value={reason} onChange={(e) => setReason(e.target.value)} placeholder="مثال: خطأ في الإدخال" />
        </Field>
      </Modal>
    </div>
  )
}
