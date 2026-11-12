# Color Theory — Light & Dark Theme

> Corporate EdTech **60-30-10 color system** for OOP AutoGrader (`frontend/src/theme/tokens.js`), calibrated for readability, WCAG AA contrast, zero gradients, and crisp Light/Dark separation.
>
> **60-30-10 hierarchy:** 60% architectural neutrals (canvas/cards/borders) · 30% Institutional Navy (`#1E3A5F` / `#23456E`) + Supporting Teal (`#0D9488`) · 10% Interactive Cobalt CTA (`#2563EB`), Academic Gold (`#D97706` / `#F59E0B`), and semantic indicators (Purple AI accent strictly ≤ 10%).

---

## 0. Institutional Brand Anchors — Navy & Gold

### Brand Navy (`--navy`)
| Role | Light | Dark |
|---|---|---|
| Navy | `#1E3A5F` | `#23456E` |
| Navy Hover | `#162C47` | `#2B5385` |
| Navy Light | `#E8EEF5` | `#162840` |
| Navy Text | `#162C47` | `#C7D9EE` |

**Use for:** Brand badge (`AppLogo`), Official lab badges (`LabPrefixBadge`), architectural section bars (`DashboardSection`, `Reports`), institutional KPI wells, navigation anchors.

### Academic Gold (`--gold`)
| Role | Light | Dark |
|---|---|---|
| Gold | `#D97706` | `#F59E0B` |
| Gold Hover | `#B45309` | `#D97706` |
| Gold Background | `#FEF3C7` | `#422006` |
| Gold Text | `#92400E` | `#FDE68A` |

**Use for:** Rank #1 podium crown (`RankingBoard`), flat `StudentStreakFlame` honors/streaks, selective recognition badges.

---

## 1. Primary — Interactive Cobalt

| Role | Light | Dark |
|---|---|---|
| Primary | `#2563EB` | `#2563EB` |
| Primary Hover | `#1D4ED8` | `#3B82F6` |
| Primary Active | `#1E40AF` | `#1D4ED8` |
| Primary Light | `#DBEAFE` | `#172B4D` |
| Primary Text | `#1E3A8A` | `#93C5FD` |

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
| Secondary | `#0D9488` | `#0D9488` |
| Secondary Hover | `#0F766E` | `#14B8A6` |
| Secondary Light | `#CCFBF1` | `#113A3A` |
| Secondary Text | `#115E59` | `#5EEAD4` |

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
| Success | `#16A34A` | `#16A34A` |
| Success Hover | `#15803D` | `#22C55E` |
| Success Background | `#DCFCE7` | `#143E29` |
| Success Text | `#166534` | `#86EFAC` |

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
| Error | `#DC2626` | `#EF4444` |
| Error Hover | `#B91C1C` | `#DC2626` |
| Error Background | `#FEE2E2` | `#451419` |
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
| Warning | `#D97706` | `#D97706` |
| Warning Hover | `#B45309` | `#F59E0B` |
| Warning Background | `#FEF3C7` | `#422408` |
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
| Subtle Border | `#E2E8F0` |
| Text Primary | `#0F172A` |
| Text Secondary | `#475569` |
| Text Muted | `#64748B` |
| Disabled | `#94A3B8` |

### Dark Theme

| Element | HEX |
|---|---|
| App Background | `#0B1220` |
| Surface | `#111A2E` |
| Surface Secondary | `#18243C` |
| Surface Tertiary | `#22304C` |
| Border | `#2A3956` |
| Subtle Border | `#1E2D47` |
| Text Primary | `#F8FAFC` |
| Text Secondary | `#CBD5E1` |
| Text Muted | `#94A3B8` |
| Disabled | `#64748B` |

### Dark Theme Guideline

Avoid pure `#000000` and avoid hiding borders or shadows in `.dark`. The calibrated deep-slate canvas (`#0B1220`) paired with `#111A2E` stage cards, visible hairline `#2A3956` borders, and `.shadow-stage-1/2/3` keeps cards crisply separated at single-level depth.

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
| Purple Accent | `#7C3AED` | `#8B5CF6` |
| Purple Light | `#EDE9FE` | `#2E1E59` |
| Purple Text | `#5B21B6` | `#DDD6FE` |

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
  /* Corporate EdTech Brand */
  --navy: #1E3A5F;
  --navy-hover: #162C47;
  --navy-light: #E8EEF5;
  --navy-text: #162C47;
  --gold: #D97706;
  --gold-hover: #B45309;
  --gold-bg: #FEF3C7;
  --gold-text: #92400E;

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
  --border-subtle: #E2E8F0;

  /* Text */
  --text-primary: #0F172A;
  --text-secondary: #475569;
  --text-muted: #64748B;
  --text-disabled: #94A3B8;

  /* Purple - limited accent */
  --purple: #7C3AED;
  --purple-light: #EDE9FE;
  --purple-text: #5B21B6;
}
```

---

## Dark Theme

```css
.dark {
  /* Corporate EdTech Brand */
  --navy: #23456E;
  --navy-hover: #2B5385;
  --navy-light: #162840;
  --navy-text: #C7D9EE;
  --gold: #F59E0B;
  --gold-hover: #D97706;
  --gold-bg: #422006;
  --gold-text: #FDE68A;

  /* Primary */
  --primary: #2563EB;
  --primary-hover: #3B82F6;
  --primary-active: #1D4ED8;
  --primary-light: #172B4D;
  --primary-text: #93C5FD;

  /* Secondary */
  --secondary: #0D9488;
  --secondary-hover: #14B8A6;
  --secondary-light: #113A3A;
  --secondary-text: #5EEAD4;

  /* Semantic */
  --success: #16A34A;
  --success-hover: #22C55E;
  --warning: #D97706;
  --warning-hover: #F59E0B;
  --error: #EF4444;
  --error-hover: #DC2626;
  --info: #0284C7;

  /* Background */
  --background: #0B1220;
  --surface: #111A2E;
  --surface-secondary: #18243C;
  --surface-tertiary: #22304C;

  /* Borders */
  --border: #2A3956;
  --border-subtle: #1E2D47;

  /* Text */
  --text-primary: #F8FAFC;
  --text-secondary: #CBD5E1;
  --text-muted: #94A3B8;
  --text-disabled: #64748B;

  /* Purple - limited accent */
  --purple: #8B5CF6;
  --purple-light: #2E1E59;
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
