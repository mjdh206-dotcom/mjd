import { useState } from 'react'
import { useStore, savePreorder } from '../store'
import { money, parseAmount, fmtDate, todayISO, daysUntil, CURRENCIES } from '../utils'
import { Card, Empty, Field, Modal, StatusBadge, Stat } from '../components'

export default function Preorders({ onToast }) {
  const s = useStore()
  const [editing, setEditing] = useState(null)
  const pending = s.preorders.filter((p) => p.status === 'pending' || p.status === 'ready')
  const deposits = s.preorders.filter((p) => p.status !== 'cancelled').reduce((a, p) => a + p.deposit, 0)

  return (
    <div className="grid" style={{ gap: 12 }}>
      <div className="grid grid-3">
        <Stat icon="🎁" label="طلبات مفتوحة" value={String(pending.length)} tone="gold" />
        <Stat icon="📅" label="تسليم هذا الأسبوع" value={String(pending.filter((p) => { const d = daysUntil(p.deliveryDate); return d !== null && d >= 0 && d <= 7 }).length)} />
        <Stat icon="💵" label="إجمالي العربون" value={money(deposits, 'SYP', { bare: true })} sub="ل.س" />
      </div>

      <Card>
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <div className="small muted">حجز سلال الضيافة للمناسبات مع تاريخ تسليم وعربون ومتابعة الحالة</div>
          <button className="btn btn-gold btn-sm" onClick={() => setEditing({
            no: `PO-${String(s.preorders.length + 1).padStart(3, '0')}`, customerName: '', phone: '', date: todayISO(),
            deliveryDate: todayISO(), total: '', deposit: '', currency: 'SYP', notes: '', status: 'pending',
          })}>＋ طلب مسبق</button>
        </div>
      </Card>

      <Card title="الطلبات المسبقة" badge={s.preorders.length}>
        {s.preorders.length === 0 ? <Empty icon="🎁" /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>الرقم</th><th>الزبون</th><th>تاريخ الطلب</th><th>تاريخ التسليم</th><th>الإجمالي</th><th>العربون</th><th>المتبقي</th><th>الحالة</th><th></th></tr></thead>
              <tbody>
                {s.preorders.map((p) => {
                  const d = daysUntil(p.deliveryDate)
                  return (
                    <tr key={p.id}>
                      <td style={{ fontWeight: 800 }}>{p.no}</td>
                      <td>{p.customerName}<div className="small muted" style={{ direction: 'ltr' }}>{p.phone}</div></td>
                      <td className="small">{fmtDate(p.date)}</td>
                      <td className="small">
                        {fmtDate(p.deliveryDate)}
                        {p.status !== 'delivered' && d !== null && (
                          <div className={`small ${d < 0 ? '' : 'muted'}`} style={{ color: d < 0 ? 'var(--danger)' : undefined }}>{d < 0 ? `متأخر ${Math.abs(d)} يوم` : d === 0 ? 'اليوم' : `بعد ${d} يوم`}</div>
                        )}
                      </td>
                      <td style={{ direction: 'ltr' }}>{money(p.total, p.currency, { bare: true })}</td>
                      <td style={{ direction: 'ltr' }}>{money(p.deposit, p.currency, { bare: true })}</td>
                      <td style={{ direction: 'ltr', color: 'var(--danger)' }}>{money(p.total - p.deposit, p.currency, { bare: true })}</td>
                      <td><StatusBadge status={p.status} /></td>
                      <td>
                        <div className="row" style={{ gap: 4 }}>
                          <select className="select" style={{ width: 110, padding: '5px 6px', fontSize: 11 }} value={p.status}
                            onChange={(e) => { savePreorder({ ...p, status: e.target.value }); onToast?.('تم تحديث الحالة') }}>
                            <option value="pending">قيد الانتظار</option>
                            <option value="ready">جاهز</option>
                            <option value="delivered">تم التسليم</option>
                            <option value="cancelled">ملغى</option>
                          </select>
                          <button className="btn btn-outline btn-sm" onClick={() => setEditing({ ...p, total: p.total / Math.pow(10, CURRENCIES[p.currency].decimals), deposit: p.deposit / Math.pow(10, CURRENCIES[p.currency].decimals) })}>تعديل</button>
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
        <Modal open onClose={() => setEditing(null)} title={editing.id ? 'تعديل طلب مسبق' : 'طلب مسبق جديد'}
          footer={<button className="btn btn-gold" onClick={() => {
            if (!editing.customerName.trim()) return
            const cur = editing.currency
            savePreorder({
              ...editing,
              total: Math.round(parseAmount(editing.total) * Math.pow(10, CURRENCIES[cur].decimals)),
              deposit: Math.round(parseAmount(editing.deposit) * Math.pow(10, CURRENCIES[cur].decimals)),
            })
            setEditing(null); onToast?.('تم حفظ الطلب')
          }}>حفظ</button>}>
          <div className="grid" style={{ gap: 10 }}>
            <div className="grid grid-2" style={{ gap: 8 }}>
              <Field label="اسم الزبون"><input className="input" value={editing.customerName} onChange={(e) => setEditing({ ...editing, customerName: e.target.value })} /></Field>
              <Field label="الهاتف"><input className="input" style={{ direction: 'ltr' }} value={editing.phone} onChange={(e) => setEditing({ ...editing, phone: e.target.value })} /></Field>
              <Field label="تاريخ الطلب"><input type="date" className="input" value={editing.date} onChange={(e) => setEditing({ ...editing, date: e.target.value })} /></Field>
              <Field label="تاريخ التسليم"><input type="date" className="input" value={editing.deliveryDate} onChange={(e) => setEditing({ ...editing, deliveryDate: e.target.value })} /></Field>
              <Field label="الإجمالي"><input className="input" inputMode="decimal" value={editing.total} onChange={(e) => setEditing({ ...editing, total: e.target.value })} /></Field>
              <Field label="العربون"><input className="input" inputMode="decimal" value={editing.deposit} onChange={(e) => setEditing({ ...editing, deposit: e.target.value })} /></Field>
              <Field label="العملة"><select className="select" value={editing.currency} onChange={(e) => setEditing({ ...editing, currency: e.target.value })}><option value="SYP">ليرة</option><option value="USD">دولار</option></select></Field>
              <Field label="الحالة">
                <select className="select" value={editing.status} onChange={(e) => setEditing({ ...editing, status: e.target.value })}>
                  <option value="pending">قيد الانتظار</option><option value="ready">جاهز</option><option value="delivered">تم التسليم</option><option value="cancelled">ملغى</option>
                </select>
              </Field>
            </div>
            <Field label="تفاصيل الطلب"><textarea className="textarea" rows={2} value={editing.notes} onChange={(e) => setEditing({ ...editing, notes: e.target.value })} placeholder="مثال: 5 سلال ضيافة فاخرة لعرس" /></Field>
          </div>
        </Modal>
      )}
    </div>
  )
}
