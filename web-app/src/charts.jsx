import { useId } from 'react'

const smooth = (pts) => {
  if (pts.length < 2) return ''
  let d = `M ${pts[0][0]},${pts[0][1]}`
  for (let i = 0; i < pts.length - 1; i++) {
    const p0 = pts[i - 1] || pts[i]
    const p1 = pts[i]
    const p2 = pts[i + 1]
    const p3 = pts[i + 2] || p2
    const c1x = p1[0] + (p2[0] - p0[0]) / 6
    const c1y = p1[1] + (p2[1] - p0[1]) / 6
    const c2x = p2[0] - (p3[0] - p1[0]) / 6
    const c2y = p2[1] - (p3[1] - p1[1]) / 6
    d += ` C ${c1x},${c1y} ${c2x},${c2y} ${p2[0]},${p2[1]}`
  }
  return d
}

export function AreaChart({ data = [], height = 150, color = '#C2983F', labels = [], showGrid = true, compact = false }) {
  const id = useId().replace(/:/g, '')
  const W = 640
  const H = height
  const padX = 8
  const padY = compact ? 8 : 16
  const max = Math.max(...data, 1) * 1.12
  const min = Math.min(...data, 0)
  const pts = data.map((v, i) => [
    padX + (i * (W - padX * 2)) / Math.max(1, data.length - 1),
    H - padY - ((v - min) / Math.max(1, max - min)) * (H - padY * 2),
  ])
  const line = smooth(pts)
  const area = line ? `${line} L ${W - padX},${H - 2} L ${padX},${H - 2} Z` : ''
  const last = pts[pts.length - 1]
  return (
    <div className="chart-wrap">
      <svg viewBox={`0 0 ${W} ${H}`} width="100%" height={H} preserveAspectRatio="none" style={{ display: 'block', overflow: 'visible' }}>
        <defs>
          <linearGradient id={`ag-${id}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={color} stopOpacity=".30" />
            <stop offset="70%" stopColor={color} stopOpacity=".06" />
            <stop offset="100%" stopColor={color} stopOpacity="0" />
          </linearGradient>
        </defs>
        {showGrid && [0.25, 0.5, 0.75].map((f) => (
          <line key={f} x1="0" x2={W} y1={H * f} y2={H * f} stroke="currentColor" strokeOpacity=".07" strokeDasharray="3 6" />
        ))}
        {area && <path d={area} fill={`url(#ag-${id})`} />}
        {line && <path d={line} fill="none" stroke={color} strokeWidth="2.4" strokeLinecap="round" vectorEffect="non-scaling-stroke" />}
        {last && <circle cx={last[0]} cy={last[1]} r="4" fill={color} stroke="var(--surface)" strokeWidth="2.5" />}
      </svg>
      {labels.length > 0 && (
        <div className="chart-axis">
          {labels.map((l, i) => <span key={i}>{l}</span>)}
        </div>
      )}
    </div>
  )
}

export function Bars({ data = [], height = 160, color = '#C2983F', valueFmt = (v) => v, highlight = true }) {
  const max = Math.max(...data.map((d) => d.value), 1)
  const every = Math.max(1, Math.ceil(data.length / 12))
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', gap: data.length > 20 ? 2 : 6, height, paddingTop: 8 }}>
      {data.map((d, i) => {
        const h = Math.max(4, (d.value / max) * (height - 34))
        const isMax = highlight && d.value === max && max > 0
        return (
          <div key={i} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, minWidth: 0 }} title={`${d.label}: ${valueFmt(d.value)}`}>
            <div style={{ flex: 1, display: 'flex', alignItems: 'flex-end', width: '100%', justifyContent: 'center' }}>
              <div style={{
                width: '100%', maxWidth: 30, height: h, borderRadius: 7,
                background: isMax ? 'linear-gradient(180deg,#E3C47C,#C2983F)' : 'linear-gradient(180deg,rgba(194,152,63,.55),rgba(194,152,63,.22))',
                transition: 'height .5s cubic-bezier(.2,.8,.2,1)',
              }} />
            </div>
            <span style={{ fontSize: 9, color: 'var(--muted)', fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '100%', visibility: (i % every === 0 || i === data.length - 1) ? 'visible' : 'hidden' }}>{d.label}</span>
          </div>
        )
      })}
    </div>
  )
}

export function Donut({ segments = [], size = 132, thickness = 14, center, sub }) {
  const total = segments.reduce((a, b) => a + b.value, 0) || 1
  const r = (size - thickness) / 2
  const c = 2 * Math.PI * r
  let offset = 0
  return (
    <div style={{ position: 'relative', width: size, height: size, flex: '0 0 auto' }}>
      <svg width={size} height={size} style={{ transform: 'rotate(-90deg)' }}>
        <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="var(--surface-3)" strokeWidth={thickness} />
        {segments.map((s, i) => {
          const len = (s.value / total) * c
          const el = (
            <circle key={i} cx={size / 2} cy={size / 2} r={r} fill="none" stroke={s.color} strokeWidth={thickness}
              strokeDasharray={`${Math.max(0, len - 2)} ${c - Math.max(0, len - 2)}`} strokeDashoffset={-offset}
              strokeLinecap="round" style={{ transition: 'stroke-dasharray .5s' }} />
          )
          offset += len
          return el
        })}
      </svg>
      {(center || sub) && (
        <div style={{ position: 'absolute', inset: 0, display: 'grid', placeItems: 'center', textAlign: 'center', padding: 16 }}>
          <div>
            <div style={{ fontSize: 17, fontWeight: 800, lineHeight: 1.2 }}>{center}</div>
            {sub && <div style={{ fontSize: 9, color: 'var(--muted)', marginTop: 2 }}>{sub}</div>}
          </div>
        </div>
      )}
    </div>
  )
}

export function MiniSpark({ data = [], width = 90, height = 30, color = '#C2983F' }) {
  const max = Math.max(...data, 1)
  const min = Math.min(...data, 0)
  const pts = data.map((v, i) => [
    (i * width) / Math.max(1, data.length - 1),
    height - 3 - ((v - min) / Math.max(1, max - min)) * (height - 6),
  ])
  return (
    <svg width={width} height={height} style={{ overflow: 'visible' }}>
      <path d={smooth(pts)} fill="none" stroke={color} strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}
