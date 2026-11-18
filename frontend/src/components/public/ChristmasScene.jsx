export default function ChristmasScene() {
  return <svg className="christmas-scene" viewBox="0 0 560 520" role="img" aria-label="Cây thông Giáng sinh bên những cuốn sách và món quà tri thức">
    <defs>
      <linearGradient id="winter-sky" x2="1" y2="1"><stop stopColor="#edf5f2" /><stop offset="1" stopColor="#fff1e4" /></linearGradient>
      <linearGradient id="fir-leaf" x2="1" y2="1"><stop stopColor="#296252" /><stop offset="1" stopColor="#17473d" /></linearGradient>
      <filter id="winter-shadow" x="-30%" y="-30%" width="160%" height="170%"><feDropShadow dy="12" stdDeviation="12" floodColor="#17304a" floodOpacity=".12" /></filter>
    </defs>
    <circle cx="280" cy="248" r="213" fill="url(#winter-sky)" />
    <circle cx="280" cy="248" r="228" fill="none" stroke="#bda77b" strokeOpacity=".3" strokeDasharray="3 10" />
    <g fill="#cba65e"><path d="m103 103 4 11 11 4-11 4-4 11-4-11-11-4 11-4Z" /><path d="m445 166 3 8 8 3-8 3-3 8-3-8-8-3 8-3Z" /><path d="m410 74 3 8 8 3-8 3-3 8-3-8-8-3 8-3Z" /></g>
    <g fill="#fff"><circle cx="150" cy="180" r="4" /><circle cx="416" cy="285" r="4" /><circle cx="359" cy="107" r="3" /><circle cx="115" cy="290" r="3" /><circle cx="470" cy="226" r="3" /></g>
    <ellipse cx="280" cy="443" rx="218" ry="24" fill="#17304a" opacity=".06" />
    <g filter="url(#winter-shadow)">
      <rect x="285" y="359" width="24" height="76" rx="6" fill="#8c6550" />
      <path d="m297 107-78 121h34l-63 87h34l-66 92h278l-66-92h34l-63-87h34Z" fill="url(#fir-leaf)" />
      <path d="M245 211q53 25 105 0M218 294q80 35 159 0M190 380q108 35 213 0" fill="none" stroke="#d4b576" strokeWidth="3" strokeLinecap="round" />
      <g fill="#f4cd7d"><circle cx="251" cy="217" r="4" /><circle cx="295" cy="225" r="4" /><circle cx="339" cy="216" r="4" /><circle cx="233" cy="301" r="4" /><circle cx="287" cy="312" r="4" /><circle cx="352" cy="306" r="4" /><circle cx="217" cy="387" r="4" /><circle cx="282" cy="397" r="4" /><circle cx="375" cy="390" r="4" /></g>
      <g fill="#e84b4f"><circle cx="279" cy="191" r="9" /><circle cx="325" cy="266" r="10" /><circle cx="258" cy="351" r="11" /><circle cx="359" cy="358" r="9" /></g>
      <g fill="#efdbb6"><circle cx="275" cy="280" r="7" /><circle cx="313" cy="365" r="7" /><circle cx="318" cy="198" r="6" /></g>
      <path d="m297 69 9 20 22 2-17 15 5 22-19-11-19 11 5-22-17-15 22-2Z" fill="#d6ac57" stroke="#f8e2af" strokeWidth="3" />
      <g transform="rotate(-8 145 377)">
        <rect x="79" y="292" width="113" height="143" rx="10" fill="#17304a" />
        <rect x="89" y="292" width="3" height="143" fill="#fff" opacity=".18" />
        <rect x="100" y="315" width="72" height="92" rx="5" fill="#fff8ed" />
        <text x="136" y="356" textAnchor="middle" fill="#17304a" fontSize="29" fontWeight="700">あ</text>
        <text x="136" y="383" textAnchor="middle" fill="#9d7554" fontSize="10" letterSpacing="2">LEARN</text>
      </g>
      <rect x="91" y="425" width="146" height="18" rx="5" fill="#e84b4f" /><path d="M104 432h118" stroke="#fff8ed" strokeWidth="3" />
      <rect x="107" y="443" width="147" height="14" rx="4" fill="#c6a16c" />
      <rect x="369" y="391" width="81" height="62" rx="6" fill="#e84b4f" />
      <rect x="365" y="384" width="89" height="16" rx="4" fill="#f2696c" />
      <path d="M410 385v68" stroke="#f6db9c" strokeWidth="13" />
      <path d="M409 384c-40-2-31-31-13-18 7 5 13 18 13 18Zm1 0c40-2 31-31 13-18-7 5-13 18-13 18Z" fill="none" stroke="#d9b56b" strokeWidth="5" />
      <rect x="324" y="421" width="49" height="37" rx="4" fill="#ecd7ad" /><path d="M347 421v37M324 432h49" stroke="#fff8ed" strokeWidth="7" />
    </g>
    <g transform="rotate(10 447 115)"><rect x="404" y="98" width="85" height="40" rx="20" fill="#fff" stroke="#e5ddd1" /><text x="446" y="123" textAnchor="middle" fill="#296252" fontSize="15" fontWeight="700">你好 ✧</text></g>
    <g transform="rotate(-9 100 226)"><rect x="48" y="207" width="100" height="39" rx="20" fill="#fff" stroke="#e5ddd1" /><text x="98" y="232" textAnchor="middle" fill="#17304a" fontSize="14" fontWeight="700">Bonjour!</text></g>
  </svg>;
}
