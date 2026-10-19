import { useEffect, useRef, useState } from 'react'
import { useStore, cancelInvoice } from '../store'
import { money, fmtDate, fmtQty, lineTotal, CURRENCIES, rateToNumber, saveBlob, openPrintWindow, openExternal } from '../utils'
import { Card, Modal, Field, StatusBadge, PageHead } from '../components'
import { Icon, BrandMark } from '../icons'
import html2canvas from 'html2canvas'
import { printThermal } from '../thermal'
import QRCode from 'qrcode'

export default function InvoiceView({ id, onBack, onToast, go }) {
  const s = useStore()
  const inv = s.invoices.find((i) => i.id === id)
  const paperRef = useRef(null)
  const [busy, setBusy] = useState(false)
  const [qr, setQr] = useState('')
  const [cancelOpen, setCancelOpen] = useState(false)
  const [reason, setReason] = useState('')

  useEffect(() => {
    if (!inv) return
    QRCode.toDataURL([
      'MAHAMESS ALHARAMAYN',
      `INV:${inv.no}`,
      `DATE:${inv.date}`,
      `TOTAL:${inv.total}/${inv.currency}`,
      `RATE:${rateToNumber(inv.rate)}`,
      `CUSTOMER:${inv.partyName}`,
    ].join('|'), { margin: 1, width: 160, color: { dark: '#33221A', light: '#FFFFFF' } }).then(setQr).catch(() => {})
  }, [inv?.id])

  if (!inv) return <Card><div className="empty"><b>الفاتورة غير موجودة</b><p>ربما حُذفت أو أن الرابط غير صحيح.</p><button className="btn btn-gold" onClick={onBack}>رجوع للفواتير</button></div></Card>
  const customer = s.customers.find((c) => c.id === inv.partyId)
  const isManager = s.session?.role === 'manager'
  const isPurchase = inv.type === 'purchase'

  const shareText = () => {
    const lines = inv.lines.map((l) => `• ${l.name} — ${fmtQty(l)} × ${money(l.unitPrice, inv.currency)} = ${money(lineTotal(l), inv.currency)}`)
    return [
      `*${s.settings.shopName}*`,
      `${isPurchase ? 'فاتورة شراء' : 'فاتورة مبيعات'} رقم: ${inv.no}`,
      `التاريخ: ${fmtDate(inv.date)}`,
      `${isPurchase ? 'المورد' : 'الزبون'}: ${inv.partyName}`,
      '',
      ...lines, '',
      `المجموع: ${money(inv.subtotal, inv.currency)}`,
      inv.discount ? `الخصم: ${money(inv.discount, inv.currency)}` : '',
      `*الإجمالي: ${money(inv.total, inv.currency)}*`,
      `المدفوع: ${money(inv.paid, inv.currency)}`,
      `المتبقي: ${money(inv.remaining, inv.currency)}`,
      `سعر الصرف المسجّل: ${rateToNumber(inv.rate).toLocaleString('en-US')} ل.س / $`,
      '', s.settings.invoiceFooter,
      s.settings.phone ? `هاتف: ${s.settings.phone}` : '',
    ].filter(Boolean).join('\n')
  }

  const shareWhatsApp = () => {
    const text = encodeURIComponent(shareText())
    const phone = (customer?.phone || '').replace(/\D/g, '')
    openExternal(phone ? `https://wa.me/${phone.startsWith('963') ? phone : '963' + phone.replace(/^0/, '')}?text=${text}` : `https://wa.me/?text=${text}`)
  }

  const shareImage = async () => {
    if (!paperRef.current) return
    setBusy(true)
    try {
      const canvas = await html2canvas(paperRef.current, { scale: 2, backgroundColor: '#FFFDF8', useCORS: true })
      const blob = await new Promise((res) => canvas.toBlob(res, 'image/png'))
      const file = new File([blob], `فاتورة-${inv.no}.png`, { type: 'image/png' })
      if (window.AndroidApp?.saveBase64) {
        await saveBlob(blob, `فاتورة-${inv.no}.png`)
        onToast?.('تم حفظ صورة الفاتورة في التنزيلات')
      } else if (navigator.canShare?.({ files: [file] })) await navigator.share({ files: [file], title: `فاتورة ${inv.no}`, text: shareText() })
      else {
        const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = `فاتورة-${inv.no}.png`; a.click()
        onToast?.('تم تنزيل صورة الفاتورة')
      }
    } catch { onToast?.('تعذر إنشاء الصورة', 'error') }
    setBusy(false)
  }

  const printReceipt = () => {
    const rows = inv.lines.map((l) => `<tr><td>${l.name}</td><td>${fmtQty(l)}</td><td>${money(l.unitPrice, inv.currency, { bare: true })}</td><td>${money(lineTotal(l), inv.currency, { bare: true })}</td></tr>`).join('')
    const disc = inv.discount > 0 ? `<div class="row"><span>الخصم</span><span>- ${money(inv.discount, inv.currency)}</span></div>` : ''
    const html = `<!doctype html><html dir="rtl" lang="ar"><head><meta charset="utf-8"><title>${inv.no}</title>
      <style>
      @page{size:A5;margin:8mm} body{font-family:Tahoma,Arial,sans-serif;color:#2b1c12;margin:0}
      .wrap{max-width:150mm;margin:0 auto;border:1px solid #e4d8c4;padding:8mm}
      .head{text-align:center;border-bottom:2px solid #c2983f;padding-bottom:6mm}
      .head h1{font-family:'Times New Roman',serif;font-size:26px;margin:0;color:#33221A}
      .en{font-size:8px;letter-spacing:3px;color:#b08a3e}
      .muted{color:#8b7967;font-size:10px}
      .meta{display:flex;justify-content:space-between;gap:10px;font-size:10.5px;padding:5mm 0;border-bottom:1px dashed #dfd2bc}
      table{width:100%;border-collapse:collapse;font-size:10.5px;margin-top:3mm}
      th{text-align:right;background:#f8f1e2;padding:2.5mm;border-bottom:1px solid #dfd2bc;font-size:9.5px;color:#8b7967}
      td{padding:2.5mm;border-bottom:1px dashed #ede3d3}
      th:last-child,td:last-child{text-align:left}
      .tot{border-top:2px solid #c2983f;margin-top:4mm;padding-top:3mm;font-size:10.5px}
      .row{display:flex;justify-content:space-between;padding:1mm 0}
      .grand{font-size:16px;font-weight:bold;border-top:1px solid #e7dcc8;margin-top:2mm;padding-top:2mm}
      .grand b{color:#8c6724;font-size:20px}
      .foot{text-align:center;font-size:9px;color:#8b7967;border-top:1px dashed #dfd2bc;margin-top:6mm;padding-top:3mm;line-height:1.9}
      .qr{text-align:center;margin-top:3mm}.qr img{width:22mm;height:22mm}
      .stamp{border:2px solid #b23b3b;color:#b23b3b;display:inline-block;padding:1mm 4mm;transform:rotate(-6deg);font-weight:bold;letter-spacing:2px}
      </style></head><body><div class="wrap">
      <div class="head"><div class="en">MAHAMESS AL HARAMAYN</div><h1>${s.settings.shopName}</h1>
      <div class="muted">${s.settings.branch || ''} · ${s.settings.address || ''} ${s.settings.phone ? '· ' + s.settings.phone : ''}</div></div>
      <div class="meta"><div><b>${isPurchase ? 'فاتورة شراء' : 'فاتورة مبيعات'}:</b> ${inv.no}<br><b>التاريخ:</b> ${fmtDate(inv.date)}<br><b>العملة:</b> ${CURRENCIES[inv.currency].name}</div>
      <div style="text-align:left"><b>${isPurchase ? 'المورد' : 'الزبون'}:</b> ${inv.partyName}<br>${customer?.phone ? `<b>الهاتف:</b> ${customer.phone}<br>` : ''}<b>سعر الصرف:</b> ${rateToNumber(inv.rate).toLocaleString('en-US')} ل.س / $</div></div>
      <table><thead><tr><th>الصنف</th><th>الكمية</th><th>السعر</th><th>المجموع</th></tr></thead><tbody>${rows}</tbody></table>
      <div class="tot"><div class="row"><span>المجموع الفرعي</span><span>${money(inv.subtotal, inv.currency)}</span></div>${disc}
      <div class="row grand"><span>الإجمالي</span><b>${money(inv.total, inv.currency)}</b></div>
      <div class="row"><span>المدفوع</span><span>${money(inv.paid, inv.currency)}</span></div>
      <div class="row"><span>المتبقي</span><span>${money(inv.remaining, inv.currency)}</span></div></div>
      ${inv.status === 'cancelled' ? `<div style="text-align:center;margin-top:4mm"><span class="stamp">ملغاة — ${inv.cancelReason || ''}</span></div>` : ''}
      <div class="foot">${s.settings.invoiceFooter}<br>${s.settings.shopName} · ${s.settings.phone || ''}</div>
      ${qr ? `<div class="qr"><img src="${qr}"><div class="muted">امسح للتحقق من الفاتورة</div></div>` : ''}
      </div><script>window.onload=()=>window.print()</script></body></html>`
    if (!openPrintWindow(html)) onToast?.('تعذر فتح نافذة الطباعة', 'error')
  }

  const thermal = async () => {
    const res = await printThermal({
      shopName: s.settings.shopName, phone: s.settings.phone, no: inv.no, date: inv.date,
      customer: inv.partyName, currency: CURRENCIES[inv.currency].name,
      lines: inv.lines.map((l) => ({ name: l.name, qty: fmtQty(l), price: money(l.unitPrice, inv.currency, { bare: true }), total: money(lineTotal(l), inv.currency, { bare: true }) })),
      total: money(inv.total, inv.currency), paid: money(inv.paid, inv.currency), remaining: money(inv.remaining, inv.currency),
      footer: s.settings.invoiceFooter,
    })
    onToast?.(res || 'تمت الطباعة')
  }

  const doCancel = () => {
    if (!reason.trim()) return onToast?.('اذكر سبب الإلغاء', 'error')
    cancelInvoice(inv.id, reason.trim())
    setCancelOpen(false)
    onToast?.('تم إلغاء الفاتورة وإرجاع المخزون')
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      <div className="row between no-print">
        <button className="btn btn-ghost" onClick={onBack}><Icon name="arrowRight" />رجوع للفواتير</button>
        <div className="row" style={{ gap: 6 }}>
          <button className="btn btn-ghost btn-sm" onClick={shareImage} disabled={busy}><Icon name="image" />{busy ? '...' : 'صورة'}</button>
          <button className="btn btn-ghost btn-sm" onClick={thermal}><Icon name="printer" />حرارية</button>
          <button className="btn btn-ghost btn-sm" onClick={shareWhatsApp}><Icon name="whatsapp" />واتساب</button>
          <button className="btn btn-primary" onClick={printReceipt}><Icon name="printer" />طباعة / PDF</button>
        </div>
      </div>

      <div className="grid" style={{ gridTemplateColumns: 'minmax(0,1.2fr) minmax(0,.8fr)', gap: 16, alignItems: 'start' }} id="inv-grid">
        <div className="receipt-wrap">
          <div className="receipt" ref={paperRef}>
            <div className="r-head">
              <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 8 }}><BrandMark size={46} dark /></div>
              <div className="en">MAHAMESS AL HARAMAYN</div>
              <h2>{s.settings.shopName}</h2>
              <p>{s.settings.branch}{s.settings.address ? ` — ${s.settings.address}` : ''}{s.settings.phone ? ` — ${s.settings.phone}` : ''}</p>
              <div style={{ marginTop: 8, display: 'inline-flex', gap: 6 }}>
                <span className="badge badge-gold">{isPurchase ? 'فاتورة شراء' : 'فاتورة مبيعات'}</span>
                <StatusBadge status={inv.status} />
              </div>
            </div>

            <div className="r-meta">
              <div>
                <div><b>رقم الفاتورة:</b> {inv.no}</div>
                <div><b>التاريخ:</b> {fmtDate(inv.date)}</div>
                <div><b>العملة:</b> {CURRENCIES[inv.currency].name}</div>
              </div>
              <div style={{ textAlign: 'left' }}>
                <div><b>{isPurchase ? 'المورد' : 'الزبون'}:</b> {inv.partyName}</div>
                {customer?.phone && <div><b>الهاتف:</b> <span dir="ltr">{customer.phone}</span></div>}
                <div><b>سعر الصرف:</b> <span dir="ltr">{rateToNumber(inv.rate).toLocaleString('en-US')} ل.س / $</span></div>
              </div>
            </div>

            <table className="r-items">
              <thead><tr><th>الصنف</th><th>الكمية</th><th>السعر</th><th>المجموع</th></tr></thead>
              <tbody>
                {inv.lines.map((l, i) => (
                  <tr key={i}>
                    <td>{l.name}</td>
                    <td>{fmtQty(l)}</td>
                    <td>{money(l.unitPrice, inv.currency, { bare: true })}</td>
                    <td>{money(lineTotal(l), inv.currency, { bare: true })}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div className="r-tot">
              <div className="tr"><span>المجموع الفرعي</span><b>{money(inv.subtotal, inv.currency)}</b></div>
              {inv.discount > 0 && <div className="tr" style={{ color: '#B23B3B' }}><span>الخصم</span><b>- {money(inv.discount, inv.currency)}</b></div>}
              <div className="tr grand"><span>الإجمالي</span><b>{money(inv.total, inv.currency)}</b></div>
              <div className="tr"><span>المدفوع {inv.paidMethod === 'debt' ? '(دين)' : inv.paidMethod === 'partial' ? '(جزئي)' : '(نقدي)'}</span><b>{money(inv.paid, inv.currency)}</b></div>
              <div className="tr" style={{ fontWeight: 700 }}><span>المتبقي</span><b>{money(inv.remaining, inv.currency)}</b></div>
              {inv.change > 0 && <div className="tr"><span>الباقي للزبون</span><b>{money(inv.change, inv.paidCurrency || inv.currency)}</b></div>}
            </div>

            {inv.status === 'cancelled' && <div className="r-stamp">ملغاة</div>}

            <div className="r-foot">
              <div style={{ fontWeight: 700, color: '#8C6724' }}>{s.settings.invoiceFooter}</div>
              <div>{s.settings.shopName}{s.settings.phone ? ` — ${s.settings.phone}` : ''}</div>
              {qr && <div className="r-qr"><img src={qr} alt="QR الفاتورة" /><div style={{ fontSize: 8, marginTop: 3 }}>امسح للتحقق</div></div>}
              <div style={{ marginTop: 4 }}>فاتورة إلكترونية صادرة من نظام محامص الحرمين — v2.0</div>
            </div>
          </div>

          <div className="row center no-print" style={{ justifyContent: 'center', marginTop: 12, gap: 6 }}>
            {isManager && inv.status !== 'cancelled' && (
              <>
                <button className="btn btn-ghost btn-sm" onClick={() => go('sale-new', { id: inv.id })}><Icon name="edit" />تعديل الفاتورة</button>
                <button className="btn btn-danger btn-sm" onClick={() => { setCancelOpen(true); setReason('') }}><Icon name="x" />إلغاء الفاتورة</button>
              </>
            )}
            <button className="btn btn-ghost btn-sm" onClick={() => go('sale-new')}><Icon name="plus" />فاتورة جديدة</button>
          </div>
        </div>

        <div className="grid no-print" style={{ gap: 16 }}>
          {isManager && (
            <Card title="تفاصيل الإدارة" icon="shield" subtitle="تظهر للمدير فقط">
              <div className="grid g2" style={{ gap: 8 }}>
                <div className="card tight" style={{ background: 'var(--surface-2)' }}><div className="tiny muted">التكلفة</div><b>{money(inv.cost, inv.currency)}</b></div>
                <div className="card tight" style={{ background: 'var(--success-soft)' }}><div className="tiny muted">الربح</div><b style={{ color: 'var(--success)' }}>{money(inv.profit, inv.currency)}</b></div>
                <div className="card tight" style={{ background: 'var(--gold-3)' }}><div className="tiny muted">هامش الربح</div><b style={{ color: 'var(--gold-deep)' }}>{inv.total ? ((inv.profit / inv.total) * 100).toFixed(1) : 0}%</b></div>
                <div className="card tight" style={{ background: 'var(--surface-2)' }}><div className="tiny muted">عدد البنود</div><b>{inv.lines.length}</b></div>
              </div>
              <div className="sep" />
              <div className="kv"><span>أنشأها</span><b>{inv.createdBy}</b></div>
              <div className="kv"><span>وقت الإنشاء</span><b>{new Date(inv.createdAt).toLocaleString('ar-SY', { dateStyle: 'short', timeStyle: 'short' })}</b></div>
              {inv.updatedAt && <div className="kv"><span>آخر تعديل</span><b>{new Date(inv.updatedAt).toLocaleString('ar-SY', { dateStyle: 'short', timeStyle: 'short' })}</b></div>}
              <div className="kv"><span>المدفوع بعملة</span><b>{CURRENCIES[inv.paidCurrency || inv.currency].name}</b></div>
            </Card>
          )}

          <Card title="الخط الزمني" icon="history">
            <div className="stack" style={{ gap: 0 }}>
              <div className="list-item">
                <span className="ic-tile" style={{ background: 'var(--success-soft)', color: 'var(--success)', border: 0 }}><Icon name="plus" /></span>
                <div className="txt"><div className="t1">إنشاء الفاتورة</div><div className="t2">{new Date(inv.createdAt).toLocaleString('ar-SY')} · {inv.createdBy}</div></div>
              </div>
              {inv.updatedAt && (
                <div className="list-item">
                  <span className="ic-tile" style={{ background: 'var(--info-soft)', color: 'var(--info)', border: 0 }}><Icon name="edit" /></span>
                  <div className="txt"><div className="t1">آخر تعديل</div><div className="t2">{new Date(inv.updatedAt).toLocaleString('ar-SY')}</div></div>
                </div>
              )}
              {inv.status === 'cancelled' && (
                <div className="list-item">
                  <span className="ic-tile" style={{ background: 'var(--danger-soft)', color: 'var(--danger)', border: 0 }}><Icon name="x" /></span>
                  <div className="txt"><div className="t1">إلغاء الفاتورة</div><div className="t2">{inv.cancelledAt ? new Date(inv.cancelledAt).toLocaleString('ar-SY') : ''} · {inv.cancelledBy}</div></div>
                </div>
              )}
            </div>
            {inv.status === 'cancelled' && <div className="tiny" style={{ marginTop: 10, color: 'var(--danger)', background: 'var(--danger-soft)', padding: 9, borderRadius: 8 }}>سبب الإلغاء: {inv.cancelReason || 'غير محدد'}</div>}
          </Card>

          <Card title="مشاركة سريعة" icon="share" subtitle="أرسل الفاتورة كما هي">
            <div className="stack" style={{ gap: 6 }}>
              <button className="btn btn-ghost btn-block" style={{ justifyContent: 'space-between' }} onClick={shareWhatsApp}><span className="row" style={{ gap: 7 }}><Icon name="whatsapp" />إرسال عبر واتساب</span><Icon name="chevronLeft" size={14} /></button>
              <button className="btn btn-ghost btn-block" style={{ justifyContent: 'space-between' }} onClick={shareImage}><span className="row" style={{ gap: 7 }}><Icon name="image" />تنزيل صورة الفاتورة</span><Icon name="chevronLeft" size={14} /></button>
              <button className="btn btn-ghost btn-block" style={{ justifyContent: 'space-between' }} onClick={thermal}><span className="row" style={{ gap: 7 }}><Icon name="printer" />طابعة حرارية بلوتوث</span><Icon name="chevronLeft" size={14} /></button>
            </div>
            <div className="tiny muted" style={{ marginTop: 8, lineHeight: 1.8 }}>الطباعة الحرارية تتطلب متصفح Chrome على أندرويد مع طابعة ESC/POS.</div>
          </Card>
        </div>
      </div>

      <style>{`@media(max-width:980px){#inv-grid{grid-template-columns:1fr!important}}`}</style>

      <Modal open={cancelOpen} onClose={() => setCancelOpen(false)} title="إلغاء الفاتورة" subtitle={`${inv.no} — ${money(inv.total, inv.currency)}`} icon="alert" size="sm"
        footer={<>
          <button className="btn btn-danger" onClick={doCancel}>تأكيد الإلغاء</button>
          <button className="btn btn-ghost" onClick={() => setCancelOpen(false)}>رجوع</button>
        </>}>
        <Field label="سبب الإلغاء" required>
          <textarea className="textarea" value={reason} onChange={(e) => setReason(e.target.value)} placeholder="مثال: خطأ في الكمية…" autoFocus />
        </Field>
        <div className="tiny" style={{ marginTop: 10, color: 'var(--danger)', background: 'var(--danger-soft)', padding: 9, borderRadius: 8 }}>
          سيُعاد المخزون وتُسجَّل العملية في سجل العمليات باسمك.
        </div>
      </Modal>
    </div>
  )
}
