import { useState } from 'react'
import { useStore, saveUser } from '../store'
import { Card, Empty, Field, Modal } from '../components'
import { fmtDateTime } from '../utils'

export default function Users({ onToast }) {
  const s = useStore()
  const [editing, setEditing] = useState(null)

  return (
    <div className="grid" style={{ gap: 12 }}>
      <Card>
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <div className="small muted">المدير: كل الصلاحيات • العامل: إنشاء فواتير فقط بدون أرباح أو تقارير</div>
          <button className="btn btn-gold btn-sm" onClick={() => setEditing({ name: '', role: 'worker', pin: '', active: true })}>＋ مستخدم</button>
        </div>
      </Card>

      <Card title="المستخدمون" badge={s.users.length}>
        {s.users.length === 0 ? <Empty icon="🔐" /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>الاسم</th><th>الدور</th><th>الصلاحيات</th><th>الرقم السري</th><th>الحالة</th><th></th></tr></thead>
              <tbody>
                {s.users.map((u) => (
                  <tr key={u.id}>
                    <td style={{ fontWeight: 700 }}>{u.name}</td>
                    <td><span className={`badge ${u.role === 'manager' ? 'badge-brown' : 'badge-gold'}`}>{u.role === 'manager' ? 'مدير' : 'عامل'}</span></td>
                    <td className="small">{u.role === 'manager' ? 'كل الصلاحيات + الأرباح + التقارير' : 'فواتير فقط — بدون أرباح أو تقارير'}</td>
                    <td style={{ direction: 'ltr' }}>{'•'.repeat(u.pin?.length || 0)}</td>
                    <td><span className={`badge ${u.active ? 'badge-green' : 'badge-red'}`}>{u.active ? 'نشط' : 'موقوف'}</span></td>
                    <td><button className="btn btn-outline btn-sm" onClick={() => setEditing({ ...u })}>تعديل</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Card title="آخر عمليات المستخدمين">
        <div className="table-wrap">
          <table>
            <thead><tr><th>الوقت</th><th>المستخدم</th><th>العملية</th><th>التفاصيل</th></tr></thead>
            <tbody>
              {s.audit.slice(0, 8).map((a) => (
                <tr key={a.id}><td className="small">{fmtDateTime(a.at)}</td><td>{a.user}</td><td>{a.action} {a.entity}</td><td className="small">{a.details}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      {editing && (
        <Modal open onClose={() => setEditing(null)} title={editing.id ? 'تعديل مستخدم' : 'مستخدم جديد'}
          footer={<button className="btn btn-gold" onClick={() => {
            if (!editing.name.trim() || !editing.pin) return
            saveUser(editing); setEditing(null); onToast?.('تم حفظ المستخدم')
          }}>حفظ</button>}>
          <div className="grid" style={{ gap: 10 }}>
            <Field label="الاسم"><input className="input" value={editing.name} onChange={(e) => setEditing({ ...editing, name: e.target.value })} /></Field>
            <Field label="الدور">
              <select className="select" value={editing.role} onChange={(e) => setEditing({ ...editing, role: e.target.value })}>
                <option value="manager">مدير</option>
                <option value="worker">عامل</option>
              </select>
            </Field>
            <Field label="الرقم السري (4 أرقام)">
              <input className="input" style={{ direction: 'ltr' }} maxLength={6} value={editing.pin} onChange={(e) => setEditing({ ...editing, pin: e.target.value.replace(/\D/g, '') })} />
            </Field>
            <div className="row" style={{ justifyContent: 'space-between' }}>
              <span className="small">الحساب نشط</span>
              <input type="checkbox" checked={!!editing.active} onChange={(e) => setEditing({ ...editing, active: e.target.checked })} style={{ width: 20, height: 20 }} />
            </div>
          </div>
        </Modal>
      )}
    </div>
  )
}
