import { useMemo, useState } from 'react'
import { useStore, invoicesFor, cancelInvoice } from '../store'
import { money, fmtDate, dayOffset, downloadCSV, CURRENCIES } from '../utils'
import { Card, Empty, Modal, StatusBadge, Field, PageHead, Money, Seg } from '../components'
import { Icon } from '../icons'

export default function Invoices({ go, onToast }) {
  const s = useStore()
  const isManager = s.session?.role === 'manager'
  const [search, setSearch] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [status, setStatus] = useState('all')
  const [cancelId, setCancelId] = useState(null)
  const [reason, setReason] = useState('')
  const [quick, setQuick] = useState('30')

  const list = useMemo(() => invoicesFor('sale', { from, to, search, status }).filter((i) => !from && !to ? i.date >= dayOffset(365) : true),
    [s, from, to, search, status])
  const totals = useMemo(() => {
    const t = { SYP: 0, USD: 0, rem: { SYP: 0, USD: 0 }, count: 0 }
    list.filter((i) => i.status !== 'cancelled').forEach((i) => {
      t[i.currency] += i.total
      t.rem[i.currency] += i.remaining || 0
      t.count++
    })
    return t
  }, [list])

  const setRange = (id) => {
    setQuick(id)
    if (id === 'all') { setFrom(''); setTo(''); return }
    setFrom(dayOffset(Number(id)))
    setTo(dayOffset(0))
  }

  const exportList = () => {
    downloadCSV(`فواتير-المبيعات-${dayOffset(0)}.csv`, [
      ['الرقم', 'التاريخ', 'الزبون', 'العملة', 'الإجمالي', 'المدفوع', 'المتبقي', 'الحالة', 'المنشئ'],
      ...list.map((i) => [i.no, i.date, i.partyName, i.currency, i.total, i.paid, i.remaining, i.status === 'cancelled' ? 'ملغاة' : 'مكتملة', i.createdBy]),
    ])
    onToast?.('تم تصدير ملف CSV')
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      <PageHead title="فواتير المبيعات" subtitle="كل فاتورة بعملتها وسعر صرفها المسجّل — قابلة للبحث والتعديل والإلغاء"
        actions={<>
          <button className="btn btn-ghost" onClick={exportList}><Icon name="download" />تصدير CSV</button>
          <button className="btn btn-gold" onClick={() => go('sale-new')}><Icon name="plus" />فاتورة جديدة</button>
        </>} />

      <div className="grid g4">
        <div className="kpi gold"><div className="top"><span className="lbl">مبيعات الفترة — ليرة</span><span className="ic"><Icon name="cart" /></span></div><div className="val">{money(totals.SYP, 'SYP', { bare: true })}</div><div className="foot">صافي بعد إلغاء الملغاة</div></div>
        <div className="kpi"><div className="top"><span className="lbl">مبيعات الفترة — دولار</span><span className="ic"><Icon name="dollar" /></span></div><div className="val">{money(totals.USD, 'USD', { bare: true })}</div><div className="foot">منفصلة تماماً عن الليرة</div></div>
        <div className="kpi"><div className="top"><span className="lbl">الذمم المتبقية</span><span className="ic"><Icon name="wallet" /></span></div><div className="val">{money(totals.rem.SYP, 'SYP', { bare: true })}</div><div className="foot">+ {money(totals.rem.USD, 'USD')}</div></div>
        <div className="kpi"><div className="top"><span className="lbl">عدد الفواتير</span><span className="ic"><Icon name="receipt" /></span></div><div className="val">{list.length}</div><div className="foot">{list.filter((i) => i.status === 'cancelled').length} ملغاة</div></div>
      </div>

      <Card>
        <div className="row between" style={{ marginBottom: 12 }}>
          <div className="field-icon" style={{ flex: 1, minWidth: 220 }}>
            <Icon name="search" />
            <input className="input" placeholder="ابحث برقم الفاتورة أو اسم الزبون…" value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <Seg value={quick} onChange={setRange} options={[{ id: '7', label: '7 أيام' }, { id: '30', label: '30 يوم' }, { id: '90', label: '90 يوم' }, { id: 'all', label: 'الكل' }]} size="sm" />
        </div>
        <div className="grid g3" style={{ gap: 10 }}>
          <Field label="من تاريخ"><input type="date" className="input" value={from} onChange={(e) => { setFrom(e.target.value); setQuick('') }} /></Field>
          <Field label="إلى تاريخ"><input type="date" className="input" value={to} onChange={(e) => { setTo(e.target.value); setQuick('') }} /></Field>
          <Field label="الحالة">
            <select className="select" value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="all">كل الحالات</option><option value="completed">مكتملة</option><option value="cancelled">ملغاة</option>
            </select>
          </Field>
        </div>
      </Card>

      <Card flush>
        {list.length === 0 ? <Empty icon="receipt" title="لا توجد فواتير مطابقة" text="جرّب توسيع الفترة أو مسح الفلاتر." /> : (
          <>
            <div className="table-wrap desktop-table" style={{ border: 0, borderRadius: 0 }}>
              <table>
                <thead><tr><th>الفاتورة</th><th>التاريخ</th><th>الزبون</th><th>العملة</th><th className="num">الإجمالي</th><th className="num">المدفوع</th><th className="num">المتبقي</th><th>الحالة</th><th></th></tr></thead>
                <tbody>
                  {list.map((i) => (
                    <tr key={i.id}>
                      <td><b>{i.no}</b><div className="tiny muted">{i.lines?.length || 0} بند · {i.createdBy}</div></td>
                      <td>{fmtDate(i.date)}</td>
                      <td style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis' }}>{i.partyName}</td>
                      <td><span className="badge badge-neutral">{i.currency === 'SYP' ? 'ل.س' : '$'}</span></td>
                      <td className="num strong">{money(i.total, i.currency, { bare: true })}</td>
                      <td className="num">{money(i.paid || 0, i.currency, { bare: true })}</td>
                      <td className="num" style={{ color: i.remaining > 0 ? 'var(--danger)' : 'var(--muted)', fontWeight: 700 }}>{money(i.remaining || 0, i.currency, { bare: true })}</td>
                      <td><StatusBadge status={i.status} /></td>
                      <td>
                        <div className="row" style={{ gap: 4, flexWrap: 'nowrap' }}>
                          <button className="btn btn-ghost btn-icon" title="عرض" onClick={() => go('invoice-view', { id: i.id })}><Icon name="eye" /></button>
                          {isManager && i.status !== 'cancelled' && (
                            <>
                              <button className="btn btn-ghost btn-icon" title="تعديل" onClick={() => go('sale-new', { id: i.id })}><Icon name="edit" /></button>
                              <button className="btn btn-ghost btn-icon" title="إلغاء" onClick={() => { setCancelId(i.id); setReason('') }}><Icon name="x" /></button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="mobile-cards" style={{ padding: 12 }}>
              {list.map((i) => (
                <div className="m-card" key={i.id} onClick={() => go('invoice-view', { id: i.id })} style={{ cursor: 'pointer' }}>
                  <div className="top">
                    <div>
                      <div className="title">{i.no} <span className="tiny muted">· {i.currency === 'SYP' ? 'ل.س' : '$'}</span></div>
                      <div className="sub">{i.partyName} · {fmtDate(i.date)}</div>
                    </div>
                    <div style={{ textAlign: 'end' }}>
                      <div className="amount">{money(i.total, i.currency)}</div>
                      <span style={{ marginTop: 4, display: 'inline-block' }}><StatusBadge status={i.status} /></span>
                    </div>
                  </div>
                  <div className="foot">
                    <span>المتبقي: <b style={{ color: i.remaining > 0 ? 'var(--danger)' : 'var(--success)' }}>{money(i.remaining || 0, i.currency)}</b></span>
                    <span>{i.lines?.length} بند</span>
                  </div>
                  {isManager && i.status !== 'cancelled' && (
                    <div className="row" style={{ marginTop: 8, gap: 5 }}>
                      <button className="btn btn-ghost btn-sm" onClick={(e) => { e.stopPropagation(); go('sale-new', { id: i.id }) }}><Icon name="edit" />تعديل</button>
                      <button className="btn btn-ghost btn-sm" style={{ color: 'var(--danger)' }} onClick={(e) => { e.stopPropagation(); setCancelId(i.id); setReason('') }}><Icon name="x" />إلغاء</button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </>
        )}
      </Card>

      {cancelId && (
        <Modal open onClose={() => setCancelId(null)} title="إلغاء الفاتورة" subtitle="سيُعاد المخزون وتُسجَّل العملية في سجل العمليات" icon="alert" size="sm"
          footer={<>
            <button className="btn btn-danger" onClick={() => {
              if (!reason.trim()) return onToast?.('اذكر سبب الإلغاء', 'error')
              cancelInvoice(cancelId, reason.trim()); setCancelId(null); onToast?.('تم إلغاء الفاتورة')
            }}>تأكيد الإلغاء</button>
            <button className="btn btn-ghost" onClick={() => setCancelId(null)}>رجوع</button>
          </>}>
          <Field label="سبب الإلغاء" required>
            <textarea className="textarea" value={reason} onChange={(e) => setReason(e.target.value)} placeholder="مثال: خطأ في إدخال الكمية…" autoFocus />
          </Field>
          <div className="tiny" style={{ marginTop: 10, color: 'var(--danger)', background: 'var(--danger-soft)', padding: 9, borderRadius: 8 }}>
            لا يمكن التراجع عن الإلغاء، لكن تبقى الفاتورة محفوظة في السجل مع سببها.
          </div>
        </Modal>
      )}
    </div>
  )
}
