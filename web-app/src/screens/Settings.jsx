import { useRef, useState } from 'react'
import { useStore, updateSettings, exportBackup, importBackup, resetDemo, setRate, rateNow } from '../store'
import { rateToNumber, fmtDateTime, CURRENCIES } from '../utils'
import { Card, Field, Toggle } from '../components'

export default function Settings({ onToast }) {
  const s = useStore()
  const [f, setF] = useState({ ...s.settings, rate: rateToNumber(s.settings.rate) })
  const fileRef = useRef(null)
  const isManager = s.session?.role === 'manager'

  const save = () => {
    updateSettings({
      ...f,
      rate: Math.round(Number(f.rate) * 100),
      ozGrams: Number(f.ozGrams) || 250,
    })
    onToast?.('تم حفظ الإعدادات')
  }

  return (
    <div className="grid" style={{ gap: 12 }}>
      <Card title="بيانات المحل" badge="تظهر على كل شاشة وفاتورة">
        <div className="grid grid-2" style={{ gap: 10 }}>
          <Field label="اسم المحل"><input className="input" value={f.shopName} onChange={(e) => setF({ ...f, shopName: e.target.value })} /></Field>
          <Field label="الفرع"><input className="input" value={f.branch} onChange={(e) => setF({ ...f, branch: e.target.value })} /></Field>
          <Field label="الهاتف"><input className="input" style={{ direction: 'ltr' }} value={f.phone} onChange={(e) => setF({ ...f, phone: e.target.value })} /></Field>
          <Field label="العنوان"><input className="input" value={f.address} onChange={(e) => setF({ ...f, address: e.target.value })} /></Field>
          <Field label="عبارة أسفل الفاتورة"><input className="input" value={f.invoiceFooter} onChange={(e) => setF({ ...f, invoiceFooter: e.target.value })} /></Field>
          <Field label="وزن الأوقية (غرام)" hint="يُستخدم لتحويل الأوقية إلى غرام/كيلو">
            <input className="input" inputMode="numeric" value={f.ozGrams} onChange={(e) => setF({ ...f, ozGrams: e.target.value })} />
          </Field>
        </div>
      </Card>

      <div className="grid grid-2">
        <Card title="العملة وسعر الصرف">
          <div className="grid" style={{ gap: 10 }}>
            <Field label="عملة الفاتورة الافتراضية">
              <select className="select" value={f.defaultCurrency} onChange={(e) => setF({ ...f, defaultCurrency: e.target.value })}>
                <option value="SYP">ليرة سورية</option><option value="USD">دولار أمريكي</option>
              </select>
            </Field>
            <Field label="سعر صرف الدولار (ل.س)">
              <div className="row" style={{ flexWrap: 'nowrap' }}>
                <input className="input" inputMode="decimal" value={f.rate} onChange={(e) => setF({ ...f, rate: e.target.value })} />
                <button className="btn btn-outline btn-sm" style={{ whiteSpace: 'nowrap' }} onClick={() => { setRate(Number(f.rate)); onToast?.('تم تحديث سعر الصرف') }}>تحديث الآن</button>
              </div>
            </Field>
            <div className="small muted">السعر المحفوظ حالياً: <b>{rateNow().toLocaleString('en-US')} ل.س</b></div>
          </div>
        </Card>

        <Card title="المظهر والأمان">
          <div className="row" style={{ justifyContent: 'space-between', padding: '8px 0' }}>
            <div><div style={{ fontWeight: 700, fontSize: 13 }}>الوضع الداكن</div><div className="small muted">راحة للعين في الإضاءة المنخفضة</div></div>
            <Toggle on={f.theme === 'dark'} onChange={(v) => { setF({ ...f, theme: v ? 'dark' : 'light' }); updateSettings({ theme: v ? 'dark' : 'light' }) }} />
          </div>
          <div className="row" style={{ justifyContent: 'space-between', padding: '8px 0' }}>
            <div><div style={{ fontWeight: 700, fontSize: 13 }}>الرقم السري عند فتح التطبيق</div><div className="small muted">مطلوب في كل مرة</div></div>
            <Toggle on={f.pinEnabled} onChange={(v) => setF({ ...f, pinEnabled: v })} />
          </div>
          <Field label="الرقم السري للمدير" style={{ marginTop: 8 }}>
            <input className="input" style={{ direction: 'ltr' }} value={f.managerPin} onChange={(e) => setF({ ...f, managerPin: e.target.value })} maxLength={6} />
          </Field>
        </Card>
      </div>

      <div className="row">
        <button className="btn btn-gold" onClick={save}>💾 حفظ الإعدادات</button>
      </div>

      <Card title="النسخ الاحتياطي والاستعادة">
        <div className="grid grid-3" style={{ gap: 10 }}>
          <div className="card" style={{ background: 'var(--cream)' }}>
            <div style={{ fontWeight: 800, fontSize: 13 }}>📤 نسخة احتياطية يدوية</div>
            <p className="small muted" style={{ margin: '6px 0 10px' }}>ملف JSON يحتوي كل البيانات، يمكن حفظه على الجهاز أو رفعه إلى Google Drive.</p>
            <button className="btn btn-brown btn-sm" onClick={() => { exportBackup(); onToast?.('تم إنشاء النسخة الاحتياطية') }}>تنزيل النسخة</button>
          </div>
          <div className="card" style={{ background: 'var(--cream)' }}>
            <div style={{ fontWeight: 800, fontSize: 13 }}>📥 استعادة</div>
            <p className="small muted" style={{ margin: '6px 0 10px' }}>اختر ملف النسخة الاحتياطية لاستعادة البيانات.</p>
            <input ref={fileRef} type="file" accept="application/json" style={{ display: 'none' }}
              onChange={async (e) => {
                const file = e.target.files[0]
                if (!file) return
                try { importBackup(await file.text()); onToast?.('تمت استعادة البيانات') }
                catch (err) { onToast?.('ملف غير صالح') }
              }} />
            <button className="btn btn-outline btn-sm" onClick={() => fileRef.current?.click()}>اختيار ملف</button>
          </div>
          <div className="card" style={{ background: 'var(--cream)' }}>
            <div style={{ fontWeight: 800, fontSize: 13 }}>🔄 نسخ تلقائي</div>
            <p className="small muted" style={{ margin: '6px 0 10px' }}>آخر نسخة: {s.settings.lastBackup ? fmtDateTime(s.settings.lastBackup) : 'لا يوجد'}</p>
            <div className="row" style={{ justifyContent: 'space-between' }}>
              <span className="small">مفعّل</span>
              <Toggle on={f.autoBackup} onChange={(v) => { setF({ ...f, autoBackup: v }); updateSettings({ autoBackup: v }) }} />
            </div>
          </div>
        </div>
        <div className="sep" />
        <p className="small muted">
          في تطبيق أندرويد يتم النسخ التلقائي يومياً إلى مساحة التطبيق مع إمكانية التصدير إلى ملف على الجوال أو مشاركته مع Google Drive.
        </p>
      </Card>

      {isManager && (
        <Card title="منطقة الخطر">
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <div>
              <div style={{ fontWeight: 700, fontSize: 13 }}>إعادة البيانات التجريبية</div>
              <div className="small muted">يمسح كل التعديلات ويعيد البيانات الافتراضية</div>
            </div>
            <button className="btn btn-outline btn-sm" onClick={() => { if (confirm('إعادة تعيين كل البيانات إلى الوضع التجريبي؟')) { resetDemo(); onToast?.('تمت إعادة التعيين') } }}>إعادة تعيين</button>
          </div>
        </Card>
      )}

      <Card title="عن التطبيق">
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <div>
            <div style={{ fontWeight: 800 }}>محامص الحرمين — إدارة الفواتير والحسابات</div>
            <div className="small muted">الإصدار 1.0.0 • يعمل بدون إنترنت • بيانات محلية مشفّرة على الجهاز</div>
          </div>
          <span className="badge badge-gold">أندرويد 8+</span>
        </div>
      </Card>
    </div>
  )
}
