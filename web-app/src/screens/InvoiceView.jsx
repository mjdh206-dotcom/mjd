import { useRef, useState } from 'react'
import { useStore } from '../store'
import { money, fmtDate, fmtQty, lineTotal, CURRENCIES, rateToNumber } from '../utils'
import { Card } from '../components'
import html2canvas from 'html2canvas'
import { printThermal } from '../thermal'

export default function InvoiceView({ id, onBack, onToast }) {
  const s = useStore()
  const inv = s.invoices.find((i) => i.id === id)
  const paperRef = useRef(null)
  const [busy, setBusy] = useState(false)
  if (!inv) return <Card>الفاتورة غير موجودة</Card>

  const customer = s.customers.find((c) => c.id === inv.partyId)
  const isManager = s.session?.role === 'manager'

  const shareText = () => {
    const lines = inv.lines.map((l) => `• ${l.name} — ${fmtQty(l)} × ${money(l.unitPrice, inv.currency)} = ${money(lineTotal(l), inv.currency)}`)
    return [
      `*${s.settings.shopName}*`,
      `فاتورة رقم: ${inv.no}`,
      `التاريخ: ${fmtDate(inv.date)}`,
      `الزبون: ${inv.partyName}`,
      '',
      ...lines,
      '',
      `المجموع: ${money(inv.subtotal, inv.currency)}`,
      inv.discount ? `الخصم: ${money(inv.discount, inv.currency)}` : '',
      `*الإجمالي: ${money(inv.total, inv.currency)}*`,
      `المدفوع: ${money(inv.paid, inv.currency)}`,
      `المتبقي: ${money(inv.remaining, inv.currency)}`,
      inv.currency === 'USD' ? `سعر الصرف: ${rateToNumber(inv.rate).toLocaleString()} ل.س` : '',
      '',
      s.settings.invoiceFooter,
      s.settings.phone ? `هاتف: ${s.settings.phone}` : '',
    ].filter(Boolean).join('\n')
  }

  const shareWhatsApp = () => {
    const text = encodeURIComponent(shareText())
    const phone = (customer?.phone || '').replace(/[^0-9]/g, '')
    window.open(phone ? `https://wa.me/${phone.startsWith('963') ? phone : '963' + phone.replace(/^0/, '')}?text=${text}` : `https://wa.me/?text=${text}`, '_blank')
  }

  const shareImage = async () => {
    if (!paperRef.current) return
    setBusy(true)
    try {
      const canvas = await html2canvas(paperRef.current, { scale: 2, backgroundColor: '#ffffff', useCORS: true })
      const blob = await new Promise((res) => canvas.toBlob(res, 'image/png'))
      const file = new File([blob], `فاتورة-${inv.no}.png`, { type: 'image/png' })
      if (navigator.canShare?.({ files: [file] })) await navigator.share({ files: [file], title: `فاتورة ${inv.no}` })
      else {
        const a = document.createElement('a')
        a.href = URL.createObjectURL(blob)
        a.download = `فاتورة-${inv.no}.png`
        a.click()
        onToast?.('تم تنزيل صورة الفاتورة')
      }
    } catch (e) { onToast?.('تعذر إنشاء الصورة') }
    setBusy(false)
  }

  const printThermalReceipt = async () => {
    const res = await printThermal({
      shopName: s.settings.shopName,
      phone: s.settings.phone,
      no: inv.no,
      date: inv.date,
      customer: inv.partyName,
      lines: inv.lines.map((l) => ({ name: l.name, qty: fmtQty(l), price: money(l.unitPrice, inv.currency, { bare: true }), total: money(lineTotal(l), inv.currency, { bare: true }) })),
      total: money(inv.total, inv.currency),
      paid: money(inv.paid, inv.currency),
      remaining: money(inv.remaining, inv.currency),
      currency: inv.currency,
      footer: s.settings.invoiceFooter,
    })
    onToast?.(res || 'تمت الطباعة')
  }

  return (
    <div className="grid" style={{ gap: 12 }}>
      <Card>
        <div className="row" style={{ gap: 6 }}>
          <button className="btn btn-outline btn-sm" onClick={onBack}>← رجوع</button>
          <button className="btn btn-brown btn-sm" onClick={() => window.print()}>🖨 طباعة / PDF</button>
          <button className="btn btn-gold btn-sm" onClick={shareWhatsApp}>واتساب</button>
          <button className="btn btn-outline btn-sm" onClick={shareImage} disabled={busy}>{busy ? '...' : '🖼 صورة'}</button>
          <button className="btn btn-outline btn-sm" onClick={printThermalReceipt}>🧾 طابعة حرارية</button>
        </div>
      </Card>

      <div ref={paperRef} className="invoice-paper" id="invoice-paper">
        <div className="invoice-head">
          <div style={{ fontSize: 11, letterSpacing: 2, color: '#B28704', fontWeight: 700 }}>MAHAMESS ALHARAMAYN</div>
          <h2 style={{ fontSize: 24 }}>{s.settings.shopName}</h2>
          <div style={{ fontSize: 11, color: '#8D6E63' }}>{s.settings.branch} — {s.settings.address}{s.settings.phone ? ` — ${s.settings.phone}` : ''}</div>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 10 }}>
          <div>
            <div><b>فاتورة رقم:</b> {inv.no}</div>
            <div><b>التاريخ:</b> {fmtDate(inv.date)}</div>
          </div>
          <div style={{ textAlign: 'left' }}>
            <div><b>الزبون:</b> {inv.partyName}</div>
            {customer?.phone && <div><b>الهاتف:</b> {customer.phone}</div>}
            <div><b>العملة:</b> {CURRENCIES[inv.currency].name}</div>
          </div>
        </div>

        <table style={{ fontSize: 12, marginBottom: 8 }}>
          <thead>
            <tr style={{ background: '#FFF8E1' }}>
              <th style={{ textAlign: 'right', padding: 6 }}>الصنف</th>
              <th style={{ padding: 6 }}>الكمية</th>
              <th style={{ padding: 6 }}>السعر</th>
              <th style={{ padding: 6 }}>المجموع</th>
            </tr>
          </thead>
          <tbody>
            {inv.lines.map((l, i) => (
              <tr key={i}>
                <td style={{ textAlign: 'right', padding: 6, borderBottom: '1px dashed #E0D7C8' }}>{l.name}</td>
                <td style={{ textAlign: 'center', padding: 6, borderBottom: '1px dashed #E0D7C8' }}>{fmtQty(l)}</td>
                <td style={{ textAlign: 'center', padding: 6, borderBottom: '1px dashed #E0D7C8', direction: 'ltr' }}>{money(l.unitPrice, inv.currency, { bare: true })}</td>
                <td style={{ textAlign: 'left', padding: 6, borderBottom: '1px dashed #E0D7C8', direction: 'ltr' }}>{money(lineTotal(l), inv.currency, { bare: true })}</td>
              </tr>
            ))}
          </tbody>
        </table>

        <div style={{ borderTop: '2px solid #D4A017', paddingTop: 8, fontSize: 12 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '2px 0' }}><span>المجموع الفرعي</span><span style={{ direction: 'ltr' }}>{money(inv.subtotal, inv.currency)}</span></div>
          {inv.discount > 0 && <div style={{ display: 'flex', justifyContent: 'space-between', padding: '2px 0', color: '#C62828' }}><span>الخصم</span><span style={{ direction: 'ltr' }}>- {money(inv.discount, inv.currency)}</span></div>}
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', fontSize: 16, fontWeight: 800, borderTop: '1px solid #E0D7C8', marginTop: 4 }}>
            <span>الإجمالي</span><span style={{ direction: 'ltr' }}>{money(inv.total, inv.currency)}</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '2px 0' }}><span>المدفوع ({inv.paidMethod === 'debt' ? 'دين' : inv.paidMethod === 'partial' ? 'دفعة جزئية' : 'نقدي'})</span><span style={{ direction: 'ltr' }}>{money(inv.paid, inv.currency)}</span></div>
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '2px 0', fontWeight: 700 }}><span>المتبقي</span><span style={{ direction: 'ltr' }}>{money(inv.remaining, inv.currency)}</span></div>
          {inv.currency === 'USD' && <div style={{ display: 'flex', justifyContent: 'space-between', padding: '2px 0', fontSize: 10, color: '#8D6E63' }}><span>سعر الصرف المسجّل</span><span style={{ direction: 'ltr' }}>{rateToNumber(inv.rate).toLocaleString()} ل.س / $</span></div>}
        </div>

        {inv.status === 'cancelled' && (
          <div style={{ marginTop: 10, padding: 8, border: '2px solid #C62828', color: '#C62828', textAlign: 'center', fontWeight: 800, transform: 'rotate(-4deg)' }}>
            ملغاة — {inv.cancelReason || ''}
          </div>
        )}

        <div style={{ marginTop: 12, textAlign: 'center', fontSize: 11, color: '#8D6E63', borderTop: '1px dashed #E0D7C8', paddingTop: 8 }}>
          <div>{s.settings.invoiceFooter}</div>
          <div style={{ marginTop: 3 }}>{s.settings.shopName} — {s.settings.phone}</div>
          <div style={{ marginTop: 3, fontSize: 9 }}>بُنيت الفاتورة بواسطة تطبيق محامص الحرمين</div>
        </div>
      </div>

      {isManager && (
        <Card title="تفاصيل للمدير">
          <div className="table-wrap">
            <table>
              <tbody>
                <tr><td>التكلفة</td><td style={{ direction: 'ltr' }}>{money(inv.cost, inv.currency)}</td><td>الربح</td><td style={{ direction: 'ltr', color: 'var(--success)', fontWeight: 800 }}>{money(inv.profit, inv.currency)}</td></tr>
                <tr><td>أنشأها</td><td>{inv.createdBy}</td><td>آخر تعديل</td><td>{inv.updatedAt ? fmtDate(inv.updatedAt) : '—'}</td></tr>
                <tr><td>سعر الصرف</td><td style={{ direction: 'ltr' }}>{rateToNumber(inv.rate).toLocaleString()}</td><td>الحالة</td><td>{inv.status === 'cancelled' ? 'ملغاة' : 'مكتملة'}</td></tr>
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  )
}
