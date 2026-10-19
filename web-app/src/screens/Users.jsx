import { useState } from 'react'
import { useStore, saveUser } from '../store'
import { PageHead, Card, Field, Modal, Avatar, Seg, Toggle, Empty, ConfirmDialog } from '../components'
import { Icon } from '../icons'
import { fmtDateTime } from '../utils'

export default function Users({ onToast }) {
  const s = useStore()
  const [editing, setEditing] = useState(null)
  const [reveal, setReveal] = useState({})

  const openNew = () => setEditing({ name: '', role: 'worker', pin: '', active: true })

  const save = () => {
    if (!editing.name.trim()) return onToast?.('أدخل اسم المستخدم', 'error')
    if (!/^\d{4,6}$/.test(String(editing.pin))) return onToast?.('الرقم السري 4 إلى 6 أرقام', 'error')
    if (s.users.some((u) => u.pin === editing.pin && u.id !== editing.id)) return onToast?.('الرقم السري مستخدم لحساب آخر', 'error')
    saveUser(editing)
    setEditing(null)
    onToast?.('تم حفظ المستخدم')
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      <PageHead title="المستخدمون والصلاحيات" subtitle="المدير يرى الأرباح والتقارير والإعدادات — العامل يُنشئ الفواتير فقط"
        actions={<button className="btn btn-gold" onClick={openNew}><Icon name="plus" />مستخدم جديد</button>} />

      <div className="grid g4">
        <div className="kpi"><div className="top"><span className="lbl">المستخدمون</span><span className="ic"><Icon name="users" /></span></div><div className="val">{s.users.length}</div><div className="foot">{s.users.filter((u) => u.active).length} نشط</div></div>
        <div className="kpi gold"><div className="top"><span className="lbl">المديرون</span><span className="ic"><Icon name="shield" /></span></div><div className="val">{s.users.filter((u) => u.role === 'manager').length}</div><div className="foot">صلاحيات كاملة</div></div>
        <div className="kpi"><div className="top"><span className="lbl">العمال</span><span className="ic"><Icon name="employee" /></span></div><div className="val">{s.users.filter((u) => u.role === 'worker').length}</div><div className="foot">فواتير ومبيعات</div></div>
        <div className="kpi"><div className="top"><span className="lbl">حماية الدخول</span><span className="ic"><Icon name="lock" /></span></div><div className="val" style={{ fontSize: 17 }}>{s.settings.pinEnabled ? 'مفعّلة' : 'معطّلة'}</div><div className="foot">PIN عند فتح التطبيق</div></div>
      </div>

      <div className="grid g2">
        {s.users.map((u) => (
          <Card key={u.id}>
            <div className="row between" style={{ marginBottom: 12 }}>
              <div className="row" style={{ gap: 10 }}>
                <Avatar name={u.name} size="lg" />
                <div>
                  <div style={{ fontSize: 14, fontWeight: 800 }}>{u.name}</div>
                  <div className="tiny muted">{u.role === 'manager' ? 'مدير المحل' : 'عامل مبيعات'} · {u.active ? 'نشط' : 'موقوف'}</div>
                </div>
              </div>
              <span className={`badge ${u.role === 'manager' ? 'badge-gold' : 'badge-neutral'}`}>{u.role === 'manager' ? 'مدير' : 'عامل'}</span>
            </div>
            <div className="row between" style={{ background: 'var(--surface-2)', padding: '8px 10px', borderRadius: 9 }}>
              <span className="tiny muted">الرقم السري</span>
              <div className="row" style={{ gap: 6 }}>
                <b style={{ direction: 'ltr', letterSpacing: 2, fontSize: 12 }}>{reveal[u.id] ? u.pin : '••••'}</b>
                <button className="btn btn-ghost btn-icon" style={{ width: 26, height: 26 }} onClick={() => setReveal({ ...reveal, [u.id]: !reveal[u.id] })}>
                  <Icon name={reveal[u.id] ? 'eyeOff' : 'eye'} size={13} />
                </button>
              </div>
            </div>
            <div className="row" style={{ marginTop: 10, gap: 5 }}>
              <button className="btn btn-ghost btn-sm" onClick={() => setEditing({ ...u })}><Icon name="edit" />تعديل</button>
              <button className={`btn btn-sm ${u.active ? 'btn-ghost' : 'btn-gold'}`} onClick={() => {
                if (u.role === 'manager' && u.active && s.users.filter((x) => x.role === 'manager' && x.active).length <= 1) return onToast?.('لا يمكن إيقاف آخر مدير', 'error')
                saveUser({ ...u, active: !u.active }); onToast?.(u.active ? 'تم إيقاف المستخدم' : 'تم تنشيط المستخدم')
              }}><Icon name={u.active ? 'x' : 'check'} />{u.active ? 'إيقاف' : 'تنشيط'}</button>
            </div>
          </Card>
        ))}
      </div>

      <Card title="صلاحيات كل دور" icon="shield">
        <div className="table-wrap">
          <table>
            <thead><tr><th>الصلاحية</th><th>المدير</th><th>العامل</th></tr></thead>
            <tbody>
              {[['إنشاء فواتير ومبيعات', true, true], ['الاطلاع على الأصناف والمخزون', true, true], ['كشوف حساب الزبائن وتسجيل الدفعات', true, true], ['رؤية الأرباح والتكلفة', true, false], ['المشتريات والموردون', true, false], ['المصاريف والصندوق والمسحوبات', true, false], ['تعديل وإلغاء الفواتير', true, false], ['إدارة المستخدمين والإعدادات', true, false], ['النسخ الاحتياطي والاستعادة', true, false]].map(([label, m, w]) => (
                <tr key={label}><td>{label}</td><td><span className={`badge ${m ? 'badge-green' : 'badge-red'}`}><Icon name={m ? 'check' : 'x'} size={10} />{m ? 'مسموح' : 'ممنوع'}</span></td><td><span className={`badge ${w ? 'badge-green' : 'badge-red'}`}><Icon name={w ? 'check' : 'x'} size={10} />{w ? 'مسموح' : 'ممنوع'}</span></td></tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      {editing && (
        <Modal open onClose={() => setEditing(null)} title={editing.id ? 'تعديل مستخدم' : 'مستخدم جديد'} icon="user" size="sm"
          footer={<><button className="btn btn-gold" onClick={save}>حفظ</button><button className="btn btn-ghost" onClick={() => setEditing(null)}>إلغاء</button></>}>
          <div className="stack">
            <Field label="الاسم" required><input className="input" value={editing.name} onChange={(e) => setEditing({ ...editing, name: e.target.value })} autoFocus /></Field>
            <Field label="الدور">
              <Seg block value={editing.role} onChange={(v) => setEditing({ ...editing, role: v })} options={[{ id: 'manager', label: 'مدير — كل الصلاحيات' }, { id: 'worker', label: 'عامل — فواتير فقط' }]} />
            </Field>
            <Field label="الرقم السري" hint="من 4 إلى 6 أرقام — يُستخدم للدخول إلى التطبيق">
              <input className="input" inputMode="numeric" value={editing.pin} onChange={(e) => setEditing({ ...editing, pin: e.target.value.replace(/\D/g, '').slice(0, 6) })} style={{ direction: 'ltr', letterSpacing: 6, textAlign: 'center', fontSize: 18, fontWeight: 800 }} />
            </Field>
            <Toggle on={!!editing.active} onChange={(v) => setEditing({ ...editing, active: v })} label="حساب نشط" hint="الحساب الموقوف لا يستطيع الدخول" />
            {editing.id && <div className="tiny muted">أُنشئ في {fmtDateTime(editing.createdAt || new Date().toISOString())}</div>}
          </div>
        </Modal>
      )}
    </div>
  )
}
