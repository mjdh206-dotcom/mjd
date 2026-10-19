import { useMemo, useState } from 'react'
import { useStore } from '../store'
import { fmtDateTime } from '../utils'
import { Card, Empty, Field } from '../components'

export default function Audit() {
  const s = useStore()
  const [q, setQ] = useState('')
  const [action, setAction] = useState('all')

  const list = useMemo(() => s.audit.filter((a) => {
    if (action !== 'all' && a.action !== action) return false
    if (q && !`${a.user} ${a.entity} ${a.entityId} ${a.details}`.includes(q)) return false
    return true
  }), [s.audit, q, action])

  const actions = [...new Set(s.audit.map((a) => a.action))]

  return (
    <div className="grid" style={{ gap: 12 }}>
      <Card>
        <div className="grid" style={{ gridTemplateColumns: '2fr 1fr', gap: 8 }}>
          <Field label="بحث في السجل"><input className="input" value={q} onChange={(e) => setQ(e.target.value)} placeholder="اسم المستخدم، رقم الفاتورة، التفاصيل..." /></Field>
          <Field label="نوع العملية">
            <select className="select" value={action} onChange={(e) => setAction(e.target.value)}>
              <option value="all">الكل</option>
              {actions.map((a) => <option key={a} value={a}>{a}</option>)}
            </select>
          </Field>
        </div>
        <p className="small muted" style={{ marginTop: 6 }}>كل إنشاء أو تعديل أو إلغاء يُسجَّل تلقائياً مع المستخدم والتاريخ.</p>
      </Card>

      <Card title="سجل العمليات" badge={list.length}>
        {list.length === 0 ? <Empty icon="📜" /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>الوقت</th><th>المستخدم</th><th>العملية</th><th>السجل</th><th>المرجع</th><th>التفاصيل</th></tr></thead>
              <tbody>
                {list.slice(0, 300).map((a) => (
                  <tr key={a.id}>
                    <td className="small" style={{ whiteSpace: 'nowrap' }}>{fmtDateTime(a.at)}</td>
                    <td>{a.user}</td>
                    <td>
                      <span className={`badge ${a.action === 'حذف' || a.action === 'إلغاء' ? 'badge-red' : a.action === 'تعديل' ? 'badge-gold' : 'badge-green'}`}>{a.action}</span>
                    </td>
                    <td>{a.entity}</td>
                    <td style={{ direction: 'ltr' }}>{a.entityId}</td>
                    <td className="small">{a.details}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  )
}
