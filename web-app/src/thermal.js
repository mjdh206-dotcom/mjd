// طباعة على طابعة حرارية بلوتوث (ESC/POS) من المتصفح — تُرسم الفاتورة كصورة لدعم العربية
const W = 384 // نقاط لطابعة 58mm

function buildCanvas(data) {
  const c = document.createElement('canvas')
  c.width = W
  c.height = 1200
  const ctx = c.getContext('2d')
  ctx.fillStyle = '#fff'
  ctx.fillRect(0, 0, W, c.height)
  ctx.fillStyle = '#000'

  let y = 16
  const center = (txt, font, gap = 6) => {
    ctx.font = font
    ctx.textAlign = 'center'
    ctx.direction = 'rtl'
    ctx.fillText(txt, W / 2, y)
    y += parseInt(font) + gap
  }
  const row = (right, left, font = '20px Tajawal, sans-serif', gap = 6) => {
    ctx.font = font
    ctx.textAlign = 'right'
    ctx.direction = 'rtl'
    ctx.fillText(right, W - 12, y)
    ctx.textAlign = 'left'
    ctx.direction = 'ltr'
    ctx.fillText(left, 12, y)
    y += parseInt(font) + gap
  }
  const line = () => {
    ctx.fillRect(10, y, W - 20, 1)
    y += 8
  }

  center(data.shopName, 'bold 30px Tajawal, sans-serif', 8)
  center('فاتورة مبيعات', '22px Tajawal, sans-serif', 8)
  if (data.phone) center(data.phone, '18px Tajawal, sans-serif')
  line()
  row(`رقم: ${data.no}`, '')
  row(`التاريخ: ${data.date}`, '')
  row(`الزبون: ${data.customer}`, '')
  line()
  ctx.font = '18px Tajawal, sans-serif'
  data.lines.forEach((l) => {
    ctx.textAlign = 'right'
    ctx.direction = 'rtl'
    ctx.fillText(l.name, W - 12, y)
    y += 22
    ctx.font = '17px Tajawal, sans-serif'
    ctx.textAlign = 'right'
    ctx.fillText(`${l.qty} × ${l.price}`, W - 12, y)
    ctx.textAlign = 'left'
    ctx.direction = 'ltr'
    ctx.fillText(l.total, 12, y)
    y += 22
    ctx.font = '18px Tajawal, sans-serif'
  })
  line()
  row('الإجمالي', data.total, 'bold 22px Tajawal, sans-serif')
  row('المدفوع', data.paid)
  row('المتبقي', data.remaining)
  line()
  center(data.footer || '', '16px Tajawal, sans-serif')
  center('شكراً لزيارتكم', '18px Tajawal, sans-serif')
  y += 30

  // قص الصورة على آخر سطر مستخدم
  const out = document.createElement('canvas')
  out.width = W
  out.height = Math.min(c.height, y)
  out.getContext('2d').drawImage(c, 0, 0)
  return out
}

function toRaster(canvas) {
  const ctx = canvas.getContext('2d')
  const { width, height } = canvas
  const img = ctx.getImageData(0, 0, width, height).data
  const bytesPerRow = Math.ceil(width / 8)
  const data = new Uint8Array(bytesPerRow * height)
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const i = (y * width + x) * 4
      const lum = (img[i] * 0.299 + img[i + 1] * 0.587 + img[i + 2] * 0.114)
      if (lum < 128) data[y * bytesPerRow + (x >> 3)] |= (0x80 >> (x & 7))
    }
  }
  const header = [0x1d, 0x76, 0x30, 0x00, bytesPerRow & 0xff, (bytesPerRow >> 8) & 0xff, height & 0xff, (height >> 8) & 0xff]
  const out = new Uint8Array(header.length + data.length)
  out.set(header, 0)
  out.set(data, header.length)
  return out
}

export async function printThermal(data) {
  if (!navigator.bluetooth) return 'المتصفح لا يدعم البلوتوث — استخدم تطبيق أندرويد للطباعة'
  try {
    const device = await navigator.bluetooth.requestDevice({
      acceptAllDevices: true,
      optionalServices: ['000018f0-0000-1000-8000-00805f9b34fb', '0000ff00-0000-1000-8000-00805f9b34fb', '49535343-fe7d-4ae5-8fa9-9fafd205e455'],
    })
    const server = await device.gatt.connect()
    const services = await server.getPrimaryServices()
    let characteristic = null
    for (const svc of services) {
      const chars = await svc.getCharacteristics()
      for (const ch of chars) {
        if (ch.properties.write || ch.properties.writeWithoutResponse) { characteristic = ch; break }
      }
      if (characteristic) break
    }
    if (!characteristic) return 'لم أجد منفذ الكتابة في الطابعة'
    const canvas = buildCanvas(data)
    const raster = toRaster(canvas)
    const init = new Uint8Array([0x1b, 0x40])
    const cut = new Uint8Array([0x1b, 0x64, 0x03, 0x1d, 0x56, 0x42, 0x00])
    const payload = new Uint8Array(init.length + raster.length + cut.length)
    payload.set(init, 0); payload.set(raster, init.length); payload.set(cut, init.length + raster.length)
    for (let i = 0; i < payload.length; i += 180) {
      await characteristic.writeValue(payload.slice(i, i + 180))
    }
    return 'تم إرسال الفاتورة إلى الطابعة'
  } catch (e) {
    if (e?.name === 'NotFoundError') return 'تم إلغاء اختيار الطابعة'
    return 'تعذر الاتصال بالطابعة'
  }
}
