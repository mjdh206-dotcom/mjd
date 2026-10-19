import { useMemo, useState } from 'react'
import { useStore } from '../store'
import { PageHead, Card, Empty, Seg, Avatar } from '../components'
import { Icon } from '../icons'

const ACTION_TONE = {
  'إنشاء': ['badge-green', 'plus'], 'تعديل': ['badge-blue', 'edit'], 'حذف': ['badge-red', 'trash'],
  'إلغاء': ['badge-red', 'x'], 'دفعة': ['badge-gold', 'cash'], 'جرد': ['badge-warn', 'clipboardCheck'],
  'تحميص': ['badge-warn', 'flame'],
}

export default function Audit() {
  const s = useStore()
  const [q, setQ] = useState('')
  const [action, setAction] = useState('all')

  const list = useMemo(() => s.audit.filter((a) => {
    if (action !== 'all' && a.action !== action) return false
    if (q && !`${a.user} ${a.entity} ${a.entityId} ${a.details}`.includes(q)) return false
    return true
  }), [s.audit, q, action])

  const grouped = useMemo(() => {
    const map = {}
    list.forEach((a) => {
      const day = new Date(a.at).toLocaleDateString('ar-SY', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
      if (!map[day]) map[day] = []
      map[day].push(a)
    })
    return Object.entries(map)
  }, [list])

  return (
    <div className="grid" style={{ gap: 16 }}>
      <PageHead title="سجل العمليات" subtitle="كل إنشاء وتعديل وإلغاء موثّق بالمستخدم والوقت — للمحاسبة والمراجعة" />

      <div className="grid g4">
        <div className="kpi"><div className="top"><span className="lbl">كل العمليات</span><span className="ic"><Icon name="history" /></span></div><div className="val">{s.audit.length}</div><div className="foot">محفوظة محلياً</div></div>
        <div className="kpi"><div className="top"><span className="lbl">عمليات اليوم</span><span className="ic"><Icon name="clock" /></span></div><div className="val">{s.audit.filter((a) => a.at.slice(0, 10) === new Date().toISOString().slice(0, 10)).length}</div><div className="foot">نشاط مباشر</div></div>
        <div className="kpi"><div className="top"><span className="lbl">فواتير</span><span className="ic"><Icon name="receipt" /></span></div><div className="val">{s.audit.filter((a) => a.entity === 'فاتورة').length}</div><div className="foot">إنشاء وتعديل وإلغاء</div></div>
        <div className="kpi"><div className="top"><span className="lbl">مستخدمون مسجّلون</span><span className="ic"><Icon name="users" /></span></div><div className="val">{new Set(s.audit.map((a) => a.user)).size}</div><div className="foot">من نفّذ العمليات</div></div>
      </div>

      <Card tight>
        <div className="row between" style={{ gap: 8 }}>
          <div className="field-icon" style={{ flex: 1, minWidth: 200 }}>
            <Icon name="search" />
            <input className="input" placeholder="ابحث باسم المستخدم أو التفاصيل…" value={q} onChange={(e) => setQ(e.target.value)} />
          </div>
          <Seg value={action} onChange={setAction} size="sm" options={[{ id: 'all', label: 'الكل' }, { id: 'إنشاء', label: 'إنشاء' }, { id: 'تعديل', label: 'تعديل' }, { id: 'إلغاء', label: 'إلغاء' }, { id: 'دفعة', label: 'دفعات' }]} />
        </div>
      </Card>

      {grouped.length === 0 ? <Card><Empty icon="history" title="لا عمليات مطابقة" /></Card> : grouped.map(([day, items]) => (
        <Card key={day} title={day} badge={items.length} icon="calendar">
          <div className="stack" style={{ gap: 0 }}>
            {items.map((a, idx) => {
              const [tone, ic] = ACTION_TONE[a.action] || ['badge-neutral', 'info']
              return (
                <div key={a.id} className="list-item" style={{ padding: '10px 0' }}>
                  <span className="ic-tile"><Icon name={ic} /></span>
                  <div className="txt">
                    <div className="t1">{a.action} {a.entity} {a.entityId && <span style={{ color: 'var(--muted)', fontWeight: 600 }}>· {a.entityId}</span>}</div>
                    <div className="t2">{a.details || '—'} · {new Date(a.at).toLocaleTimeString('ar-SY', { hour: '2-digit', minute: '2-digit' })}</div>
                  </div>
                  <div className="row" style={{ gap: 6 }}>
                    <span className={`badge ${tone}`}>{a.action}</span>
                    <span className="row" style={{ gap: 5 }}><Avatar name={a.user} size="sm" /><span className="tiny" style={{ fontWeight: 700 }}>{a.user}</span></span>
                  </div>
                </div>
              )
            })}
          </div>
        </Card>
      ))}
    </div>
  )
}
