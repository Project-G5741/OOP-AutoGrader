# Color Theory — Light & Dark Theme

> Recommended color system for modern dashboards such as Student Dashboard and Lecturer Dashboard.
>
> **Design principle:** Use blue as the primary color, teal/cyan as supporting colors, and keep dark purple to a minimum.

---

## 1. Primary — Blue

| Role | Light | Dark |
|---|---|---|
| Primary | `#2563EB` | `#60A5FA` |
| Primary Hover | `#1D4ED8` | `#93C5FD` |
| Primary Active | `#1E40AF` | `#3B82F6` |
| Primary Light | `#DBEAFE` | `#1E3A5F` |
| Primary Text | `#1E3A8A` | `#BFDBFE` |

**Recommended usage:**
- Primary buttons
- Links
- Active tabs
- Selected items
- Progress indicators
- Focus states

> **Recommendation:** Blue should be the main brand/interaction color.

---

## 2. Secondary — Teal

| Role | Light | Dark |
|---|---|---|
| Secondary | `#0D9488` | `#2DD4BF` |
| Secondary Hover | `#0F766E` | `#5EEAD4` |
| Secondary Light | `#CCFBF1` | `#134E4A` |
| Secondary Text | `#115E59` | `#99F6E4` |

**Recommended usage:**
- Completed states
- Secondary success indicators
- Statistics
- Highlights
- Icons
- Progress indicators

Teal gives the interface a technical and modern appearance without making the UI feel overly blue.

---

## 3. Success — Green

| Role | Light | Dark |
|---|---|---|
| Success | `#16A34A` | `#4ADE80` |
| Success Hover | `#15803D` | `#86EFAC` |
| Success Background | `#DCFCE7` | `#14532D` |
| Success Text | `#166534` | `#BBF7D0` |

**Use for:**
- Correct
- Passed
- Completed
- Successful submission
- Lab completed

Example:

**Light:** `#16A34A`  
**Dark:** `#4ADE80`

---

## 4. Error — Red

| Role | Light | Dark |
|---|---|---|
| Error | `#DC2626` | `#F87171` |
| Error Hover | `#B91C1C` | `#FCA5A5` |
| Error Background | `#FEE2E2` | `#450A0A` |
| Error Text | `#991B1B` | `#FECACA` |

**Use for:**
- Incorrect
- Failed
- Error
- Rejected
- Invalid

---

## 5. Warning — Amber

Amber is recommended instead of an overly bright orange.

| Role | Light | Dark |
|---|---|---|
| Warning | `#D97706` | `#FBBF24` |
| Warning Hover | `#B45309` | `#FCD34D` |
| Warning Background | `#FEF3C7` | `#451A03` |
| Warning Text | `#92400E` | `#FDE68A` |

**Use for:**
- Pending
- Need review
- Low score
- Waiting
- Attention required

---

## 6. Neutral / Background Colors

### Light Theme

| Element | HEX |
|---|---|
| App Background | `#F8FAFC` |
| Surface | `#FFFFFF` |
| Surface Secondary | `#F1F5F9` |
| Surface Tertiary | `#E2E8F0` |
| Border | `#CBD5E1` |
| Text Primary | `#0F172A` |
| Text Secondary | `#475569` |
| Text Muted | `#64748B` |
| Disabled | `#94A3B8` |

### Dark Theme

| Element | HEX |
|---|---|
| App Background | `#0F172A` |
| Surface | `#111827` |
| Surface Secondary | `#1E293B` |
| Surface Tertiary | `#334155` |
| Border | `#334155` |
| Text Primary | `#F8FAFC` |
| Text Secondary | `#CBD5E1` |
| Text Muted | `#94A3B8` |
| Disabled | `#64748B` |

### Dark Theme Guideline

Avoid using pure `#000000` as the main background.

A dark charcoal/navy background such as `#0F172A` provides a softer visual hierarchy and allows accent colors to stand out without becoming overly bright.

---

## 7. Purple — Limited Accent Only

Because the UI should avoid strong/dark purple, do **not** use colors such as:

```text
#4C1D95
#581C87
#6B21A8
#7E22CE
```

Instead, use lighter purple only as a secondary accent.

| Role | Light | Dark |
|---|---|---|
| Purple Accent | `#8B5CF6` | `#A78BFA` |
| Purple Light | `#EDE9FE` | `#312E81` |
| Purple Text | `#6D28D9` | `#DDD6FE` |

### Recommended Purple Usage

Keep purple to approximately **5–10% of the UI**.

Good use cases:
- AI-related features
- Special analytics
- Premium/special status
- Selected chart categories
- Special highlights

Avoid using purple for:
- The entire sidebar
- Primary buttons
- Main navigation
- Most dashboard cards

---

## 8. Chart Palette

For dashboards with charts, use a controlled palette instead of a rainbow-like UI.

### Light Theme

```text
Blue      #2563EB
Teal      #0D9488
Green     #16A34A
Amber     #D97706
Red       #DC2626
Cyan      #0891B2
Purple    #8B5CF6
Slate     #64748B
```

### Dark Theme

```text
Blue      #60A5FA
Teal      #2DD4BF
Green     #4ADE80
Amber     #FBBF24
Red       #F87171
Cyan      #22D3EE
Purple    #A78BFA
Slate     #94A3B8
```

### Chart Guidelines

1. Use blue/teal as the most common chart colors.
2. Use green and red for positive/negative states.
3. Use amber for warnings or attention states.
4. Use purple sparingly.
5. Do not use many highly saturated colors at the same time.
6. Maintain consistent meaning across all charts.

---

## 9. Semantic Colors for Lecturer Dashboard

This mapping is recommended for grading and submission-related interfaces.

| Meaning | Light | Dark |
|---|---|---|
| Correct | `#16A34A` | `#4ADE80` |
| Incorrect | `#DC2626` | `#F87171` |
| Pending | `#D97706` | `#FBBF24` |
| Selected | `#2563EB` | `#60A5FA` |
| Info | `#0284C7` | `#38BDF8` |
| Completed | `#0D9488` | `#2DD4BF` |
| Neutral | `#64748B` | `#94A3B8` |

### Example Challenge Tabs

```text
Challenge Tabs
│
├── Overview       → Blue
├── Challenge 1    → Blue
├── Challenge 2    → Blue
│
├── Correct        → Green
├── Incorrect      → Red
└── Pending        → Amber
```

---

# 10. Recommended Complete Design Tokens

## Light Theme

```css
:root {
  /* Primary */
  --primary: #2563EB;
  --primary-hover: #1D4ED8;
  --primary-active: #1E40AF;
  --primary-light: #DBEAFE;
  --primary-text: #1E3A8A;

  /* Secondary */
  --secondary: #0D9488;
  --secondary-hover: #0F766E;
  --secondary-light: #CCFBF1;
  --secondary-text: #115E59;

  /* Semantic */
  --success: #16A34A;
  --success-hover: #15803D;
  --warning: #D97706;
  --warning-hover: #B45309;
  --error: #DC2626;
  --error-hover: #B91C1C;
  --info: #0284C7;

  /* Background */
  --background: #F8FAFC;
  --surface: #FFFFFF;
  --surface-secondary: #F1F5F9;
  --surface-tertiary: #E2E8F0;

  /* Borders */
  --border: #CBD5E1;

  /* Text */
  --text-primary: #0F172A;
  --text-secondary: #475569;
  --text-muted: #64748B;
  --text-disabled: #94A3B8;

  /* Purple - limited accent */
  --purple: #8B5CF6;
  --purple-light: #EDE9FE;
  --purple-text: #6D28D9;
}
```

---

## Dark Theme

```css
.dark {
  /* Primary */
  --primary: #60A5FA;
  --primary-hover: #93C5FD;
  --primary-active: #3B82F6;
  --primary-light: #1E3A5F;
  --primary-text: #BFDBFE;

  /* Secondary */
  --secondary: #2DD4BF;
  --secondary-hover: #5EEAD4;
  --secondary-light: #134E4A;
  --secondary-text: #99F6E4;

  /* Semantic */
  --success: #4ADE80;
  --success-hover: #86EFAC;
  --warning: #FBBF24;
  --warning-hover: #FCD34D;
  --error: #F87171;
  --error-hover: #FCA5A5;
  --info: #38BDF8;

  /* Background */
  --background: #0F172A;
  --surface: #111827;
  --surface-secondary: #1E293B;
  --surface-tertiary: #334155;

  /* Borders */
  --border: #334155;

  /* Text */
  --text-primary: #F8FAFC;
  --text-secondary: #CBD5E1;
  --text-muted: #94A3B8;
  --text-disabled: #64748B;

  /* Purple - limited accent */
  --purple: #A78BFA;
  --purple-light: #312E81;
  --purple-text: #DDD6FE;
}
```

---

# 11. Overall Color Strategy

| Color | Purpose | Priority |
|---|---|---|
| 🔵 Blue | Primary interaction / navigation | High |
| 🟢 Teal | Secondary / statistics / completion | High |
| 🟩 Green | Success / correct / completed | High |
| 🟥 Red | Error / incorrect / failed | High |
| 🟡 Amber | Warning / pending / attention | Medium |
| 🩵 Cyan | Information | Medium |
| 🟣 Purple | Special accent only | Low |
| ⚫ Slate | Background / text / borders | Essential |

## Final Recommendation

The overall visual hierarchy should follow:

**Blue → Primary**  
**Teal → Secondary/Data**  
**Green → Success**  
**Red → Error**  
**Amber → Warning**  
**Cyan → Information**  
**Purple → Limited Accent**

This creates a modern dashboard style while avoiding an overly purple interface.
