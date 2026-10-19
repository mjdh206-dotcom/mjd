import { useMemo, useRef, useState } from 'react'
import { useStore, updateSettings, setTheme, setRate, rateNow, exportBackup, importBackup, resetDemo, ensureCatalog, resetMovements, resetSystem } from '../store'
import { CURRENCIES, money, rateToNumber, todayISO } from '../utils'
import { Card, Field, PageHead, Seg, Toggle, ConfirmDialog, Modal } from '../components'
import { Icon, BrandMark } from '../icons'

export default function Settings({ onToast }) {
  const s = useStore()
  const [tab, setTab] = useState('shop')
  const [form, setForm] = useState({
    shopName: s.settings.shopName, branch: s.settings.branch, phone: s.settings.phone,
    address: s.settings.address, invoiceFooter: s.settings.invoiceFooter,
    ozGrams: String(s.settings.ozGrams), defaultCurrency: s.settings.defaultCurrency,
  })
  const [rateInput, setRateInput] = useState(String(rateNow()))
  const [confirm, setConfirm] = useState(null)
  const [importData, setImportData] = useState(null)
  const fileRef = useRef(null)

  const saveShop = () => {
    updateSettings({
      shopName: form.shopName.trim() || 'محامص الحرمين', branch: form.branch, phone: form.phone, address: form.address,
      invoiceFooter: form.invoiceFooter, ozGrams: Math.max(1, Number(form.ozGrams) || 250), defaultCurrency: form.defaultCurrency,
    })
    onToast?.('تم حفظ بيانات المحل')
  }

  const storageSize = useMemo(() => {
    try { return (JSON.stringify(s).length / 1024).toFixed(1) } catch { return '—' }
  }, [s])

  const doImport = (text) => {
    try {
      importBackup(text)
      onToast?.('تمت استعادة النسخة الاحتياطية')
      setImportData(null)
    } catch (e) { onToast?.('ملف غير صالح أو تالف', 'error') }
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      <PageHead title="الإعدادات والنسخ الاحتياطي" subtitle="هوية المحل، إعدادات التشغيل، الحماية، والنسخ الاحتياطي الكامل" />

      <Seg block value={tab} onChange={setTab} options={[
        { id: 'shop', label: 'هوية المحل', icon: 'bank' }, { id: 'ops', label: 'التشغيل والأسعار', icon: 'sliders' },
        { id: 'security', label: 'الحماية والمظهر', icon: 'shield' }, { id: 'backup', label: 'النسخ الاحتياطي', icon: 'database' },
      ]} />

      {tab === 'shop' && (
        <div className="grid" style={{ gridTemplateColumns: 'minmax(0,1.4fr) minmax(0,1fr)', gap: 16, alignItems: 'start' }}>
          <Card title="بيانات المحل" icon="bank" subtitle="تظهر في أعلى كل شاشة وعلى كل فاتورة وإيصال">
            <div className="grid g2" style={{ gap: 10 }}>
              <Field label="اسم المحل" required><input className="input" value={form.shopName} onChange={(e) => setForm({ ...form, shopName: e.target.value })} /></Field>
              <Field label="الفرع"><input className="input" value={form.branch} onChange={(e) => setForm({ ...form, branch: e.target.value })} /></Field>
              <Field label="الهاتف"><input className="input" style={{ direction: 'ltr' }} value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></Field>
              <Field label="العنوان"><input className="input" value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} /></Field>
            </div>
            <Field label="عبارة أسفل الفاتورة" style={{ marginTop: 10 }}><input className="input" value={form.invoiceFooter} onChange={(e) => setForm({ ...form, invoiceFooter: e.target.value })} /></Field>
            <button className="btn btn-gold" style={{ marginTop: 12 }} onClick={saveShop}><Icon name="save" />حفظ البيانات</button>
          </Card>

          <Card title="معاينة الهوية" icon="star">
            <div style={{ background: 'linear-gradient(150deg,#332317,#1E140E)', borderRadius: 14, padding: 20, color: '#F9F0E0', textAlign: 'center' }}>
              <BrandMark size={60} style={{ margin: '0 auto' }} />
              <div style={{ fontFamily: 'Amiri,serif', fontSize: 25, marginTop: 8 }}>{form.shopName || 'محامص الحرمين'}</div>
              <div className="tiny" style={{ letterSpacing: 2.4, color: 'rgba(227,196,124,.65)', marginTop: 2 }}>MAHAMESS AL HARAMAYN</div>
              <div className="tiny" style={{ color: 'rgba(249,240,224,.5)', marginTop: 8 }}>{form.branch} · {form.phone}</div>
              <div className="tiny" style={{ color: 'rgba(249,240,224,.4)' }}>{form.address}</div>
            </div>
            <div className="tiny muted" style={{ marginTop: 10, lineHeight: 1.8 }}>الاسم والفرع يظهران في الشريط الجانبي وأعلى الشاشات، وعلى رأس الفاتورة المطبوعة والصورة والواتساب.</div>
          </Card>
        </div>
      )}

      {tab === 'ops' && (
        <div className="grid" style={{ gridTemplateColumns: 'minmax(0,1.3fr) minmax(0,1fr)', gap: 16, alignItems: 'start' }}>
          <Card title="إعدادات التشغيل" icon="sliders">
            <div className="stack">
              <div className="grid g2" style={{ gap: 10 }}>
                <Field label="وزن الأوقية (غرام)" hint="يُستخدم في تحويل الأوقية إلى غرام">
                  <input className="input" inputMode="numeric" value={form.ozGrams} onChange={(e) => setForm({ ...form, ozGrams: e.target.value })} />
                </Field>
                <Field label="العملة الافتراضية للفواتير">
                  <select className="select" value={form.defaultCurrency} onChange={(e) => setForm({ ...form, defaultCurrency: e.target.value })}>
                    <option value="SYP">ليرة سورية</option><option value="USD">دولار أمريكي</option>
                  </select>
                </Field>
              </div>
              <div className="sep" />
              <div className="row between">
                <div><div style={{ fontSize: 12.5, fontWeight: 700 }}>سعر صرف الدولار</div><div className="tiny muted">يُسجَّل مع كل فاتورة جديدة</div></div>
                <div className="row" style={{ gap: 6 }}>
                  <input className="input" inputMode="decimal" value={rateInput} onChange={(e) => setRateInput(e.target.value)} style={{ width: 120, direction: 'ltr', textAlign: 'right', height: 36 }} />
                  <button className="btn btn-gold btn-sm" onClick={() => { const n = Number(rateInput); if (n > 0) { setRate(n); onToast?.('تم تحديث السعر') } else onToast?.('سعر غير صالح', 'error') }}>تحديث</button>
                </div>
              </div>
              <div className="sep" />
              <div className="row between">
                <div><div style={{ fontSize: 12.5, fontWeight: 700 }}>الوضع الداكن</div><div className="tiny muted">مريح للعمل الليلي</div></div>
                <Toggle on={s.settings.theme === 'dark'} onChange={() => setTheme(s.settings.theme === 'dark' ? 'light' : 'dark')} />
              </div>
              <div className="row between">
                <div><div style={{ fontSize: 12.5, fontWeight: 700 }}>قفل التطبيق برقم سري</div><div className="tiny muted">يُطلب PIN عند كل فتح للتطبيق</div></div>
                <Toggle on={!!s.settings.pinEnabled} onChange={(v) => { updateSettings({ pinEnabled: v }); onToast?.(v ? 'تم تفعيل القفل' : 'تم تعطيل القفل') }} />
              </div>
              <button className="btn btn-gold" style={{ marginTop: 4 }} onClick={saveShop}><Icon name="save" />حفظ إعدادات التشغيل</button>
            </div>
          </Card>

          <Card title="معلومات النظام" icon="info">
            <div className="stack">
              <div className="row between"><span className="small muted">الإصدار</span><b className="small">2.1.0 Pro</b></div>
              <div className="row between"><span className="small muted">قاعدة البيانات</span><b className="small">محلية (بدون إنترنت)</b></div>
              <div className="row between"><span className="small muted">حجم البيانات</span><b className="small">{storageSize} ك.ب</b></div>
              <div className="row between"><span className="small muted">عدد الأصناف</span><b className="small">{s.items.length}</b></div>
              <div className="row between"><span className="small muted">عدد الفواتير</span><b className="small">{s.invoices.length}</b></div>
              <div className="row between"><span className="small muted">آخر نسخة احتياطية</span><b className="small">{s.settings.lastBackup ? new Date(s.settings.lastBackup).toLocaleDateString('ar-SY') : 'لم تُنشأ بعد'}</b></div>
              <div className="sep" />
              <div className="tiny muted" style={{ lineHeight: 1.9 }}>كل المبالغ تُخزَّن كأعداد صحيحة (بدون أخطاء Float)، وكل فاتورة تحتفظ بسعر صرفها وعملتها.</div>
            </div>
          </Card>
        </div>
      )}

      {tab === 'security' && (
        <div className="grid g2">
          <Card title="الحماية" icon="shield">
            <div className="stack">
              <div className="row between"><div><div style={{ fontSize: 12.5, fontWeight: 700 }}>قفل التطبيق</div><div className="tiny muted">طلب رقم سري عند كل دخول</div></div><Toggle on={!!s.settings.pinEnabled} onChange={(v) => updateSettings({ pinEnabled: v })} /></div>
              <div className="sep" />
              <div className="row between"><div><div style={{ fontSize: 12.5, fontWeight: 700 }}>الوضع الداكن</div><div className="tiny muted">واجهة داكنة أنيقة</div></div><Toggle on={s.settings.theme === 'dark'} onChange={() => setTheme(s.settings.theme === 'dark' ? 'light' : 'dark')} /></div>
              <div className="sep" />
              <div className="tiny muted" style={{ lineHeight: 1.9 }}>لتغيير أرقام الدخول، انتقل إلى شاشة <b>المستخدمون والصلاحيات</b>. يُنصح بتغيير الرقم الافتراضي 1234 فور بدء الاستخدام.</div>
              <button className="btn btn-ghost" style={{ marginTop: 4 }} onClick={() => onToast?.('يمكنك تغيير الأرقام من شاشة المستخدمين')}><Icon name="key" />إدارة أرقام الدخول</button>
            </div>
          </Card>
          <Card title="التنبيهات الذكية" icon="bell">
            <div className="stack">
              <div className="row between"><div><div style={{ fontSize: 12.5, fontWeight: 700 }}>تنبيه المخزون المنخفض</div><div className="tiny muted">عند الوصول لحد التنبيه</div></div><Toggle on onChange={() => onToast?.('التنبيهات مفعّلة دائماً')} /></div>
              <div className="row between"><div><div style={{ fontSize: 12.5, fontWeight: 700 }}>تنبيه الصلاحية</div><div className="tiny muted">قبل 30 يوماً من الانتهاء</div></div><Toggle on onChange={() => onToast?.('التنبيهات مفعّلة دائماً')} /></div>
              <div className="row between"><div><div style={{ fontSize: 12.5, fontWeight: 700 }}>تنبيه الديون المتأخرة</div><div className="tiny muted">بعد 30 يوماً من آخر فاتورة</div></div><Toggle on onChange={() => onToast?.('التنبيهات مفعّلة دائماً')} /></div>
              <div className="sep" />
              <div className="tiny muted" style={{ lineHeight: 1.9 }}>تظهر التنبيهات في جرس الشريط العلوي وفي لوحة التحكم مع روابط مباشرة للإجراء.</div>
            </div>
          </Card>
        </div>
      )}

      {tab === 'backup' && (
        <div className="grid" style={{ gridTemplateColumns: 'minmax(0,1.3fr) minmax(0,1fr)', gap: 16, alignItems: 'start' }}>
          <Card title="النسخ الاحتياطي والاستعادة" icon="database" subtitle="نسخة كاملة من المحل: الأصناف، الفواتير، الحسابات والإعدادات">
            <div className="grid g2" style={{ gap: 10 }}>
              <button className="btn btn-primary" style={{ height: 80, flexDirection: 'column', gap: 4 }} onClick={() => { exportBackup(); onToast?.('تم تنزيل النسخة الاحتياطية') }}>
                <Icon name="download" size={22} /><span>تنزيل نسخة احتياطية</span><span className="tiny" style={{ opacity: .6, fontWeight: 500 }}>ملف JSON كامل</span>
              </button>
              <button className="btn btn-ghost" style={{ height: 80, flexDirection: 'column', gap: 4 }} onClick={() => fileRef.current?.click()}>
                <Icon name="upload" size={22} /><span>استعادة نسخة</span><span className="tiny muted" style={{ fontWeight: 500 }}>سيُستبدل كل البيانات</span>
              </button>
            </div>
            <input ref={fileRef} type="file" accept=".json,application/json" style={{ display: 'none' }} onChange={(e) => {
              const file = e.target.files?.[0]
              if (!file) return
              const reader = new FileReader()
              reader.onload = () => setImportData(String(reader.result))
              reader.readAsText(file)
              e.target.value = ''
            }} />
            <div className="sep" />
            <div className="row between" style={{ marginBottom: 8 }}>
              <span className="small muted">آخر نسخة احتياطية</span>
              <span className="badge badge-neutral">{s.settings.lastBackup ? new Date(s.settings.lastBackup).toLocaleString('ar-SY') : 'لم تُنشأ بعد'}</span>
            </div>
            <div className="tiny muted" style={{ lineHeight: 1.9 }}>
              احتفظ بنسخة أسبوعياً على الأقل. يمكن استعادة النسخة على نفس الجهاز أو جهاز آخر يعمل بنفس التطبيق.
            </div>
          </Card>

          <Card title="بيانات النظام" icon="database" subtitle="تهيئة البداية، التصفير، والكتالوج الجاهز">
            <div className="stack">
              <div className="card tight" style={{ background: 'var(--gold-3)', borderColor: 'rgba(194,152,63,.3)' }}>
                <div style={{ fontSize: 12.5, fontWeight: 800, color: 'var(--gold-deep)' }}>كتالوج أصناف المحامص</div>
                <div className="tiny" style={{ color: 'var(--gold-deep)', opacity: .85, marginTop: 3, lineHeight: 1.7 }}>أكثر من 110 أصناف: مكسرات، بزور، مجففات، قهوة، كاندي وضيافة. الكميات والأسعار تبدأ صفراً.</div>
                <button className="btn btn-gold btn-sm" style={{ marginTop: 8 }} onClick={() => setConfirm('catalog')}><Icon name="package" />استعادة الأصناف الناقصة</button>
              </div>
              <div className="card tight" style={{ background: 'var(--surface-2)' }}>
                <div style={{ fontSize: 12.5, fontWeight: 800 }}>تصفير الحركات</div>
                <div className="tiny muted" style={{ marginTop: 3, lineHeight: 1.7 }}>يحذف الفواتير والحسابات والحركات، ويبقي الأصناف والزبائن والأسعار.</div>
                <button className="btn btn-ghost btn-sm" style={{ marginTop: 8 }} onClick={() => setConfirm('moves')}><Icon name="rotate" />تصفير الحركات</button>
              </div>
              <div className="card tight" style={{ background: 'var(--warn-soft)', borderColor: 'rgba(169,113,29,.22)' }}>
                <div style={{ fontSize: 12.5, fontWeight: 800, color: 'var(--warn)' }}>بيانات تجريبية</div>
                <div className="tiny" style={{ color: 'var(--warn)', opacity: .85, marginTop: 3, lineHeight: 1.7 }}>يملأ النظام بفواتير وأصناف وأسعار للتجربة والعرض فقط.</div>
                <button className="btn btn-ghost btn-sm" style={{ marginTop: 8 }} onClick={() => setConfirm('demo')}>تحميل البيانات التجريبية</button>
              </div>
              <div className="card tight" style={{ background: 'var(--danger-soft)', borderColor: 'rgba(178,59,59,.2)' }}>
                <div style={{ fontSize: 12.5, fontWeight: 800, color: 'var(--danger)' }}>تصفير النظام بالكامل</div>
                <div className="tiny" style={{ color: 'var(--danger)', opacity: .85, marginTop: 3, lineHeight: 1.7 }}>يرجع كل شيء إلى الصفر: أصناف بلا أسعار ولا كميات، ولا فواتير. تُحفظ إعدادات المحل والمستخدمون.</div>
                <button className="btn btn-danger btn-sm" style={{ marginTop: 8 }} onClick={() => setConfirm('clear')}>تصفير كل شيء</button>
              </div>
              <div className="tiny muted">يُنصح بتنزيل نسخة احتياطية قبل أي عملية حذف.</div>
            </div>
          </Card>
        </div>
      )}

      <ConfirmDialog open={confirm === 'demo'} onClose={() => setConfirm(null)} title="تحميل بيانات تجريبية" text="سيتم استبدال كل البيانات الحالية ببيانات تجريبية جاهزة. لا يمكن التراجع." confirmLabel="تحميل التجريبية" tone="primary" icon="rotate"
        onConfirm={() => { resetDemo(); onToast?.('تم تحميل البيانات التجريبية') }} />
      <ConfirmDialog open={confirm === 'catalog'} onClose={() => setConfirm(null)} title="استعادة كتالوج الأصناف" text="ستُضاف أصناف الكتالوج الناقصة فقط (بكميات وأسعار صفر) دون المساس بأصنافك وأسعارك الحالية." confirmLabel="إضافة الأصناف الناقصة" tone="primary" icon="package"
        onConfirm={() => { ensureCatalog(); onToast?.('تم تحديث كتالوج الأصناف') }} />

      {confirm === 'moves' && <MovesResetModal onClose={() => setConfirm(null)} onDone={(opts) => { resetMovements(opts); setConfirm(null); onToast?.('تم تصفير الحركات') }} />}
      {confirm === 'clear' && <FullResetModal onClose={() => setConfirm(null)} onDone={() => { resetSystem(); setConfirm(null); onToast?.('تم تصفير النظام بالكامل') }} />}

      {importData && (
        <Modal open onClose={() => setImportData(null)} title="استعادة نسخة احتياطية" subtitle="راجع الملف قبل الاستبدال" icon="upload" size="sm"
          footer={<>
            <button className="btn btn-gold" onClick={() => doImport(importData)}>استعادة الآن</button>
            <button className="btn btn-ghost" onClick={() => setImportData(null)}>إلغاء</button>
          </>}>
          <div className="stack">
            <div className="row" style={{ background: 'var(--warn-soft)', padding: 10, borderRadius: 9, gap: 7 }}>
              <Icon name="alert" size={16} style={{ color: 'var(--warn)' }} />
              <span className="tiny" style={{ color: 'var(--warn)', fontWeight: 700 }}>سيتم استبدال كل البيانات الحالية بمحتوى الملف.</span>
            </div>
            <div className="card tight" style={{ background: 'var(--surface-2)' }}>
              <div className="row between"><span className="tiny muted">حجم الملف</span><b className="tiny">{(importData.length / 1024).toFixed(1)} ك.ب</b></div>
              <div className="row between" style={{ marginTop: 4 }}><span className="tiny muted">نوع الملف</span><b className="tiny">نسخة محامص الحرمين</b></div>
            </div>
          </div>
        </Modal>
      )}
    </div>
  )
}


function MovesResetModal({ onClose, onDone }) {
  const [clearStock, setClearStock] = useState(true)
  return (
    <Modal open onClose={onClose} title="تصفير الحركات" subtitle="لا يمكن التراجع" icon="rotate" size="sm"
      footer={<>
        <button className="btn btn-danger" onClick={() => onDone({ clearStock })}>تصفير الحركات</button>
        <button className="btn btn-ghost" onClick={onClose}>إلغاء</button>
      </>}>
      <div className="stack">
        <p style={{ fontSize: 12.5, lineHeight: 1.9, color: 'var(--ink-2)' }}>
          سيتم حذف الفواتير والمشتريات والمصاريف والمسحوبات والدفعات وإقفالات الصندوق وسجل العمليات.
          تبقى الأصناف والزبائن والمستخدمون والإعدادات.
        </p>
        <div className="row between">
          <div><div style={{ fontSize: 12.5, fontWeight: 700 }}>تصفير كميات المخزون</div><div className="tiny muted">اجعل مخزون كل صنف صفراً</div></div>
          <Toggle on={clearStock} onChange={setClearStock} />
        </div>
      </div>
    </Modal>
  )
}

function FullResetModal({ onClose, onDone }) {
  const [text, setText] = useState('')
  const ready = text.trim() === 'تصفير'
  return (
    <Modal open onClose={onClose} title="تصفير النظام بالكامل" subtitle="عملية نهائية — اكتب كلمة التأكيد" icon="alert" size="sm"
      footer={<>
        <button className="btn btn-danger" disabled={!ready} onClick={onDone}><Icon name="trash" />تصفير نهائي</button>
        <button className="btn btn-ghost" onClick={onClose}>إلغاء</button>
      </>}>
      <div className="stack">
        <div className="row" style={{ background: 'var(--danger-soft)', padding: 10, borderRadius: 9, gap: 7 }}>
          <Icon name="alert" size={16} style={{ color: 'var(--danger)' }} />
          <span className="tiny" style={{ color: 'var(--danger)', fontWeight: 700 }}>سيُحذف كل شيء: الفواتير، الأصناف، الزبائن، الموردون، العمال، الكشوفات والحركات.</span>
        </div>
        <div className="tiny muted" style={{ lineHeight: 1.9 }}>
          سيعود النظام إلى حالة البداية: كتالوج الأصناف كاملاً بكميات وأسعار صفر، ولا فواتير ولا أرصدة.
          تُحفظ أسماء المحل والمستخدمون وأرقام الدخول وسعر الصرف.
        </div>
        <Field label="اكتب «تصفير» للتأكيد">
          <input className="input" value={text} onChange={(e) => setText(e.target.value)} placeholder="تصفير" autoFocus />
        </Field>
      </div>
    </Modal>
  )
}
