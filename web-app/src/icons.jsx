/* أيقونات SVG خفيفة — خط 1.7، بلا مكتبات خارجية */
const P = {
  dashboard: <><rect x="3" y="3" width="7.5" height="7.5" rx="2" /><rect x="13.5" y="3" width="7.5" height="7.5" rx="2" /><rect x="3" y="13.5" width="7.5" height="7.5" rx="2" /><rect x="13.5" y="13.5" width="7.5" height="7.5" rx="2" /></>,
  receipt: <><path d="M5 3.5h14a1 1 0 0 1 1 1V21l-2.7-1.8L14.6 21l-2.6-1.8L9.4 21l-2.7-1.8L4 21V4.5a1 1 0 0 1 1-1Z" /><path d="M8 8h8M8 12h8M8 16h5" /></>,
  cart: <><circle cx="9" cy="20" r="1.4" /><circle cx="18" cy="20" r="1.4" /><path d="M3 3h2.2l2.3 12.2a2 2 0 0 0 2 1.6h8.7a2 2 0 0 0 2-1.6L21 7H6" /></>,
  package: <><path d="m12 3 8.5 4.8v8.4L12 21l-8.5-4.8V7.8L12 3Z" /><path d="M3.8 7.5 12 12l8.2-4.5M12 12v9" /><path d="m7.9 5 8.2 4.6" /></>,
  users: <><path d="M16 21v-1.8a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4V21" /><circle cx="9" cy="7" r="3.4" /><path d="M22 21v-1.8a4 4 0 0 0-3-3.8M16.5 3.7a3.4 3.4 0 0 1 0 6.6" /></>,
  truck: <><path d="M3 6.5h10.5v10H3zM13.5 10H18l3 3v3.5h-7.5" /><circle cx="7" cy="18" r="1.8" /><circle cx="17.5" cy="18" r="1.8" /></>,
  wallet: <><path d="M20 8V6.5a1.5 1.5 0 0 0-1.5-1.5h-14A1.5 1.5 0 0 0 3 6.5v11A1.5 1.5 0 0 0 4.5 19h15a1.5 1.5 0 0 0 1.5-1.5V9.5A1.5 1.5 0 0 0 19.5 8H4" /><path d="M16.5 13.5h.01" /><path d="M3 9h17" /></>,
  bank: <><path d="m3 9 9-5.5L21 9" /><path d="M4.5 10.5v7M9 10.5v7M15 10.5v7M19.5 10.5v7M2.5 20.5h19" /></>,
  employee: <><circle cx="12" cy="8" r="3.6" /><path d="M5 21v-1.4a5.4 5.4 0 0 1 5.4-5.4h3.2a5.4 5.4 0 0 1 5.4 5.4V21" /><path d="m16.5 4.5 1.3 1.3 2.4-2.4" /></>,
  chart: <><path d="M3 3v18h18" /><path d="M7.5 15.5v-4M12 15.5V7M16.5 15.5v-6.5" /></>,
  shield: <><path d="M12 22s8-3.6 8-9.5V5.4L12 2.5 4 5.4v7.1C4 18.4 12 22 12 22Z" /><path d="m9 12 2 2 4-4" /></>,
  history: <><path d="M3 12a9 9 0 1 0 2.6-6.3L3 8" /><path d="M3 3v5h5" /><path d="M12 7.5V12l3 1.8" /></>,
  settings: <><circle cx="12" cy="12" r="3.2" /><path d="M19.4 14.5a1.7 1.7 0 0 0 .34 1.87l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.7 1.7 0 0 0-1.87-.34 1.7 1.7 0 0 0-1 1.55V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.55 1.7 1.7 0 0 0-1.88.34l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.7 1.7 0 0 0 .34-1.87 1.7 1.7 0 0 0-1.55-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.55-1.1 1.7 1.7 0 0 0-.34-1.88l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.7 1.7 0 0 0 1.87.34h.01a1.7 1.7 0 0 0 1-1.55V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.55 1.7 1.7 0 0 0 1.87-.34l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.7 1.7 0 0 0-.34 1.87v.01a1.7 1.7 0 0 0 1.55 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1Z" /></>,
  search: <><circle cx="11" cy="11" r="7.2" /><path d="m20 20-3.6-3.6" /></>,
  bell: <><path d="M18 8.5a6 6 0 1 0-12 0c0 6.5-2.5 7.5-2.5 7.5h17S18 15 18 8.5" /><path d="M13.7 20a2 2 0 0 1-3.4 0" /></>,
  sun: <><circle cx="12" cy="12" r="4.2" /><path d="M12 1.8v2.4M12 19.8v2.4M4.2 4.2l1.7 1.7M18.1 18.1l1.7 1.7M1.8 12h2.4M19.8 12h2.4M4.2 19.8l1.7-1.7M18.1 5.9l1.7-1.7" /></>,
  moon: <><path d="M20.5 14.5A8.5 8.5 0 1 1 9.5 3.5a7 7 0 0 0 11 11Z" /></>,
  plus: <><path d="M12 5v14M5 12h14" /></>,
  minus: <><path d="M5 12h14" /></>,
  x: <><path d="m6 6 12 12M18 6 6 18" /></>,
  check: <><path d="m4.5 12.5 5 5 10-11" /></>,
  checkCircle: <><circle cx="12" cy="12" r="9" /><path d="m8 12.2 2.7 2.8 5.5-6" /></>,
  chevronDown: <><path d="m6 9 6 6 6-6" /></>,
  chevronLeft: <><path d="m15 6-6 6 6 6" /></>,
  chevronRight: <><path d="m9 6 6 6-6 6" /></>,
  arrowLeft: <><path d="M20 12H4M10 6l-6 6 6 6" /></>,
  arrowRight: <><path d="M4 12h16M14 6l6 6-6 6" /></>,
  arrowUp: <><path d="M12 20V4M6 10l6-6 6 6" /></>,
  arrowDown: <><path d="M12 4v16M6 14l6 6 6-6" /></>,
  trendUp: <><path d="m3 17 6-6 4 4 8-8" /><path d="M15 7h6v6" /></>,
  trendDown: <><path d="m3 7 6 6 4-4 8 8" /><path d="M15 17h6v-6" /></>,
  printer: <><path d="M6.5 9V3.5h11V9" /><rect x="3" y="9" width="18" height="8" rx="2" /><path d="M6.5 14h11v6.5h-11z" /><path d="M17.5 12h.01" /></>,
  share: <><circle cx="18" cy="5" r="2.6" /><circle cx="6" cy="12" r="2.6" /><circle cx="18" cy="19" r="2.6" /><path d="m8.3 10.7 7.4-4.3M8.3 13.3l7.4 4.3" /></>,
  whatsapp: <><path d="M20.5 11.6a8.4 8.4 0 0 1-12.4 7.4L3.5 20.5l1.5-4.5a8.4 8.4 0 1 1 15.5-4.4Z" /><path d="M8.5 7.8c.3-.1.7 0 .9.3l1 1.7c.1.3.1.6-.1.8l-.6.7c.5 1.1 1.4 2 2.5 2.5l.7-.6c.2-.2.5-.2.8-.1l1.7 1c.3.2.4.6.3.9l-.3 1c-.2.5-.8.8-1.4.7-3.2-.6-5.7-3.1-6.3-6.3-.1-.6.2-1.2.7-1.4Z" /></>,
  download: <><path d="M12 3v12M7 10l5 5 5-5" /><path d="M4 20h16" /></>,
  upload: <><path d="M12 16V4M7 9l5-5 5 5" /><path d="M4 20h16" /></>,
  edit: <><path d="M12 20h9" /><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" /></>,
  trash: <><path d="M3.5 6.5h17M9 6.5V4.2c0-.6.5-1.2 1.2-1.2h3.6c.7 0 1.2.6 1.2 1.2v2.3M5.5 6.5 6.6 20a1.4 1.4 0 0 0 1.4 1.3h8a1.4 1.4 0 0 0 1.4-1.3l1.1-13.5M10 10.5v6.5M14 10.5v6.5" /></>,
  trashSmall: <><path d="M4 7h16M9 7V5h6v2M6 7l1 13h10l1-13M10 11v5M14 11v5" /></>,
  qr: <><rect x="3" y="3" width="6.5" height="6.5" rx="1" /><rect x="14.5" y="3" width="6.5" height="6.5" rx="1" /><rect x="3" y="14.5" width="6.5" height="6.5" rx="1" /><path d="M14.5 14.5h2.5v2.5h-2.5zM19 14.5h2M14.5 19v2M18 18h3v3h-3z" /></>,
  barcode: <><path d="M4 6v12M7 6v12M10 6v8M13 6v12M16 6v8M19 6v12M21 6v12" /></>,
  mic: <><rect x="9" y="2.5" width="6" height="11" rx="3" /><path d="M5.5 10.5a6.5 6.5 0 0 0 13 0M12 17v4M9 21h6" /></>,
  scan: <><path d="M4 9V6.5A1.5 1.5 0 0 1 5.5 5H8M16 5h2.5A1.5 1.5 0 0 1 20 6.5V9M20 15v2.5a1.5 1.5 0 0 1-1.5 1.5H16M8 19H5.5A1.5 1.5 0 0 1 4 17.5V15M3.5 12h17" /></>,
  card: <><rect x="2.5" y="5" width="19" height="14" rx="2.5" /><path d="M2.5 9.5h19M6.5 15h4" /></>,
  cash: <><rect x="2.5" y="6" width="19" height="12" rx="2" /><circle cx="12" cy="12" r="2.6" /><path d="M6 9.5v5M18 9.5v5" /></>,
  coins: <><ellipse cx="9" cy="6.5" rx="5.5" ry="2.7" /><path d="M3.5 6.5v4c0 1.5 2.5 2.7 5.5 2.7s5.5-1.2 5.5-2.7v-4" /><path d="M3.5 10.5v4c0 1.5 2.5 2.7 5.5 2.7 1 0 2-.15 2.8-.4" /><ellipse cx="16" cy="14" rx="4.8" ry="2.4" /><path d="M11.2 14v4c0 1.4 2.1 2.5 4.8 2.5s4.8-1.1 4.8-2.5v-4" /></>,
  dollar: <><path d="M12 2v20M17 5.5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" /></>,
  calendar: <><rect x="3" y="5" width="18" height="16" rx="2.5" /><path d="M8 3v4M16 3v4M3 10h18" /></>,
  alert: <><path d="M10.3 3.8 2.4 17.4A2 2 0 0 0 4.1 20h15.8a2 2 0 0 0 1.7-2.6L13.7 3.8a2 2 0 0 0-3.4 0Z" /><path d="M12 9v4M12 16.5h.01" /></>,
  clock: <><circle cx="12" cy="12" r="9" /><path d="M12 6.5V12l3.5 2" /></>,
  filter: <><path d="M3 5h18l-7 8.2v5.3l-4 2v-7.3Z" /></>,
  refresh: <><path d="M20.5 11a8.5 8.5 0 0 0-15-4.5L3 9" /><path d="M3 4v5h5" /><path d="M3.5 13a8.5 8.5 0 0 0 15 4.5L21 15" /><path d="M21 20v-5h-5" /></>,
  menu: <><path d="M4 6.5h16M4 12h16M4 17.5h16" /></>,
  logout: <><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><path d="m16 17 5-5-5-5M21 12H9" /></>,
  sparkles: <><path d="m12 3 1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9L12 3Z" /><path d="M19 16.5 20 19l2.5 1-2.5 1-1 2.5-1-2.5-2.5-1 2.5-1 1-2.5Z" /><path d="M5 2.5 5.8 4.5 7.8 5.3 5.8 6.1 5 8.1 4.2 6.1 2.2 5.3 4.2 4.5Z" /></>,
  percent: <><path d="M19 5 5 19" /><circle cx="7" cy="7" r="2.5" /><circle cx="17" cy="17" r="2.5" /></>,
  tag: <><path d="M20.6 13.4 12 22 2 12V2h10l8.6 8.6a2 2 0 0 1 0 2.8Z" /><circle cx="7" cy="7" r="1.4" /></>,
  basket: <><path d="M4 9.5h16l-1.6 9a2 2 0 0 1-2 1.7H7.6a2 2 0 0 1-2-1.7Z" /><path d="m8 9.5 4-6.5 4 6.5" /><path d="M9.5 13v3.5M14.5 13v3.5" /></>,
  coffee: <><path d="M4 8h13v7a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5Z" /><path d="M17 9h1.8a2.7 2.7 0 0 1 0 5.4H17" /><path d="M7 2.5c-.6 1 .6 1.8 0 3M11 2.5c-.6 1 .6 1.8 0 3" /></>,
  candy: <><circle cx="12" cy="12" r="4.2" /><path d="M8.3 9.5 4 7.5l1 4-1 4 4.3-2M15.7 9.5 20 7.5l-1 4 1 4-4.3-2" /></>,
  peanut: <><path d="M8.3 14.5c-2.2-2.2-3.2-4-2.4-6.1.7-1.8 2.8-2.7 4.6-2 .9.4 1.5 1.1 2.3 1.9 1.6 1.6 2.4 1.2 3.3 2.3 1.4 1.7 1 4.4-.8 5.8-1.9 1.5-4.7 1.4-7-.2-1-.7-1.7-1-2-1.7Z" /><path d="M9.5 12.5c1-.4 2-1.2 2.7-2.3" /></>,
  gift: <><rect x="3" y="8.5" width="18" height="4.5" rx="1" /><path d="M4.5 13v8h15v-8M12 8.5V21" /><path d="M12 8.5C10 8.5 7.5 7.8 7.5 5.8A2.3 2.3 0 0 1 12 5a2.3 2.3 0 0 1 4.5.8c0 2-2.5 2.7-4.5 2.7Z" /></>,
  scale: <><path d="M12 3v18M7 21h10M5 7h14" /><path d="m5 7-2.5 6a3.2 3.2 0 0 0 5 0Z" /><path d="m19 7-2.5 6a3.2 3.2 0 0 0 5 0Z" /></>,
  box: <><rect x="3" y="7" width="18" height="14" rx="1.5" /><path d="M3 11h18M12 7v14M7.5 7V4h9v3" /></>,
  warehouse: <><path d="m3 21 V9l9-5 9 5v12" /><path d="M3 21h18M7 21v-7h10v7M7 17.5h10" /></>,
  clipboard: <><rect x="5" y="4" width="14" height="17" rx="2" /><path d="M9 4V2.8h6V4M9 9h6M9 13h6M9 17h4" /></>,
  book: <><path d="M4 4.5A2.5 2.5 0 0 1 6.5 2H20v17.5H6.5A2.5 2.5 0 0 0 4 22Z" /><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" /><path d="M8 6h8M8 10h5" /></>,
  sliders: <><path d="M3 7h5M12 7h9M3 17h9M16 17h5" /><circle cx="10" cy="7" r="2.2" /><circle cx="14" cy="17" r="2.2" /></>,
  database: <><ellipse cx="12" cy="5.5" rx="8" ry="3.2" /><path d="M4 5.5v13c0 1.8 3.6 3.2 8 3.2s8-1.4 8-3.2v-13" /><path d="M4 12c0 1.8 3.6 3.2 8 3.2s8-1.4 8-3.2" /></>,
  key: <><circle cx="8" cy="15.5" r="4.5" /><path d="m11.5 12 9-9M17 6.5l2.5 2.5M14.5 9l2.5 2.5" /></>,
  lock: <><rect x="4" y="10" width="16" height="11" rx="2.5" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /><path d="M12 14.5v2.5" /></>,
  eye: <><path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z" /><circle cx="12" cy="12" r="3" /></>,
  eyeOff: <><path d="M3 3 21 21" /><path d="M10.5 6.2A9.8 9.8 0 0 1 12 6c6 0 9.5 6 9.5 6a17 17 0 0 1-3 3.6M6.5 7.6A17 17 0 0 0 2.5 12S6 18 12 18a9.5 9.5 0 0 0 4.2-1" /><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" /></>,
  info: <><circle cx="12" cy="12" r="9" /><path d="M12 11v5M12 7.8h.01" /></>,
  file: <><path d="M14 2.5H6.5a2 2 0 0 0-2 2v15a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2V8Z" /><path d="M14 2.5V8h5M8.5 12h7M8.5 16h5" /></>,
  image: <><rect x="3" y="3" width="18" height="18" rx="2.5" /><circle cx="8.5" cy="8.5" r="1.7" /><path d="m21 15-5-5L5 21" /></>,
  send: <><path d="m22 2-7 20-4-9-9-4Z" /><path d="M22 2 11 13" /></>,
  phone: <><path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2 4.2 2 2 0 0 1 4 2h3a2 2 0 0 1 2 1.7c.1 1 .4 2 .7 2.9a2 2 0 0 1-.5 2.1L8 10a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c1 .3 1.9.6 2.9.7A2 2 0 0 1 22 17Z" /></>,
  pin: <><path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 0 1 16 0Z" /><circle cx="12" cy="10" r="2.8" /></>,
  more: <><circle cx="5" cy="12" r="1.3" /><circle cx="12" cy="12" r="1.3" /><circle cx="19" cy="12" r="1.3" /></>,
  save: <><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2Z" /><path d="M17 21v-8H7v8M7 3v5h8" /></>,
  undo: <><path d="M3 7v6h6" /><path d="M3.5 13a9 9 0 1 0 2.2-6.4L3 9" /></>,
  flame: <><path d="M12 22c4.4 0 7-3 7-6.5 0-3.5-2-5.5-3.5-7C14 7 13 5 13.5 2.5 10.5 4 9 7 9 9c-1-.4-2-1.4-2.5-2.5C5.5 8.5 5 10.7 5 12.5 5 18 7.6 22 12 22Z" /><path d="M12 22c1.9 0 3.2-1.4 3.2-3.2 0-1.8-1.2-2.8-2-4.3-.8 1-1.4 1.8-2.4 2.6-.7.6-1 1.3-1 2.2 0 1.5 1 2.7 2.2 2.7Z" /></>,
  pie: <><path d="M21.2 15.9A10 10 0 1 1 8.1 2.8" /><path d="M22 12A10 10 0 0 0 12 2v10Z" /></>,
  line: <><path d="M3 3v18h18" /><path d="m6 15 4-4 3 3 6-7" /></>,
  swap: <><path d="M7 4 3 8l4 4M3 8h14a4 4 0 0 1 0 8h-1" /><path d="m17 20 4-4-4-4M21 16H7a4 4 0 0 1 0-8h1" /></>,
  list: <><path d="M8 6h13M8 12h13M8 18h13M3.5 6h.01M3.5 12h.01M3.5 18h.01" /></>,
  grid: <><rect x="3" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="3" width="7" height="7" rx="1.5" /><rect x="3" y="14" width="7" height="7" rx="1.5" /><rect x="14" y="14" width="7" height="7" rx="1.5" /></>,
  user: <><circle cx="12" cy="8" r="4" /><path d="M4.5 21a7.5 7.5 0 0 1 15 0" /></>,
  star: <><path d="m12 2.8 2.9 5.9 6.5.9-4.7 4.6 1.1 6.4-5.8-3-5.8 3 1.1-6.4L2.6 9.6l6.5-.9Z" /></>,
  globe: <><circle cx="12" cy="12" r="9" /><path d="M3 12h18M12 3a15 15 0 0 1 0 18 15 15 0 0 1 0-18Z" /></>,
  command: <><path d="M9 6a3 3 0 1 0-3 3h12a3 3 0 1 0-3-3v12a3 3 0 1 0 3-3H6a3 3 0 1 0 3 3Z" /></>,
  bag: <><path d="M6 7.5h12l1.4 12.2a1.5 1.5 0 0 1-1.5 1.7H6.1a1.5 1.5 0 0 1-1.5-1.7Z" /><path d="M8.5 10V6a3.5 3.5 0 0 1 7 0v4" /></>,
  scale2: <><path d="M5 21h14M12 3v18M5 7h14" /><path d="m7 7-2 5h4Z" /><path d="m17 7-2 5h4Z" /></>,
  verify: <><path d="m12 2.5 2.5 1.9 3.1-.1 1 3 2.5 1.9-1 3 1 3-2.5 1.9-1 3-3.1-.1L12 21.5l-2.5-1.9-3.1.1-1-3L2.9 15l1-3-1-3 2.5-1.9 1-3 3.1.1Z" /><path d="m8.5 12 2.4 2.4 4.6-4.8" /></>,
  clipboardCheck: <><rect x="5" y="4" width="14" height="17" rx="2" /><path d="M9 4V2.8h6V4" /><path d="m8.5 12.5 2.2 2.2 4.3-4.5" /></>,
  rotate: <><path d="M3 12a9 9 0 1 0 3-6.7L3 8" /><path d="M3 3v5h5" /></>,
  arrowLeftRight: <><path d="M4 7h15l-3-3M20 17H5l3 3" /></>,
  sliders2: <><path d="M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3M1 14h6M9 8h6M17 16h6" /></>,
}

export function Icon({ name, size = 18, strokeWidth = 1.75, className = '', style }) {
  const path = P[name] || P.info
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} fill="none" stroke="currentColor"
      strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}
      style={style} aria-hidden="true">{path}</svg>
  )
}

export const categoryIcon = (cat) => ({ nuts: 'peanut', hospitality: 'gift', candy: 'candy', coffee: 'coffee' }[cat] || 'package')

export function BrandMark({ size = 46, className = '', dark = false }) {
  return (
    <svg viewBox="0 0 64 64" width={size} height={size} className={className} aria-hidden="true">
      <defs>
        <linearGradient id="bm-gold" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#EFD89F" />
          <stop offset=".48" stopColor="#CA9F49" />
          <stop offset="1" stopColor="#9A722A" />
        </linearGradient>
        <linearGradient id="bm-dark" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#37251A" />
          <stop offset="1" stopColor="#1C120C" />
        </linearGradient>
      </defs>
      <rect x="1" y="1" width="62" height="62" rx="17" fill={dark ? 'url(#bm-dark)' : 'url(#bm-gold)'} stroke={dark ? '#CA9F49' : 'rgba(42,27,18,.35)'} strokeWidth="1" />
      <rect x="5.5" y="5.5" width="53" height="53" rx="13" fill="none" stroke={dark ? 'rgba(202,159,73,.55)' : 'rgba(42,27,18,.30)'} strokeWidth="1" />
      <g fill="none" stroke={dark ? '#E7CB86' : '#2A1B12'} strokeLinecap="round">
        <path d="M32 13.5c-6.6 4.6-9.8 10.2-9.8 17 0 6.9 3.2 12.4 9.8 17 6.6-4.6 9.8-10.1 9.8-17 0-6.8-3.2-12.4-9.8-17Z" strokeWidth="1.9" />
        <path d="M32 18.4c-3.1 5.9-3.1 19.3 0 25.2" strokeWidth="1.55" />
        <path d="M27.5 21.5c1.3 1.4 1.6 3.2 1.2 4.9M36.5 21.5c-1.3 1.4-1.6 3.2-1.2 4.9" strokeWidth="1.3" opacity=".7" />
      </g>
      <circle cx="32" cy="9" r="1.1" fill={dark ? '#E7CB86' : '#2A1B12'} />
      <circle cx="32" cy="55" r="1.1" fill={dark ? '#E7CB86' : '#2A1B12'} />
    </svg>
  )
}

export function ReceiptSeal({ size = 44 }) {
  return <BrandMark size={size} />
}
