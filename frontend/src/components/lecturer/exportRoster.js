import { collectIncorrectExportRows } from './ClassScoreBreakdown';
import { formatPercent, formatText } from '../../utils/formatters';

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

function escapeSvgText(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

export async function exportRowsAsExcel(rows, fileBase) {
  const XLSX = await import('xlsx');
  const ws = XLSX.utils.json_to_sheet(rows);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, 'Export');
  XLSX.writeFile(wb, `${fileBase}.xlsx`);
}

export async function exportRowsAsPdf(rows, title, fileBase) {
  const { jsPDF } = await import('jspdf');
  const doc = new jsPDF();
  doc.setFontSize(12);
  let y = 20;
  doc.text(title, 14, y);
  y += 10;
  rows.forEach((row) => {
    const line = Object.values(row).join(' | ');
    doc.text(line, 14, y);
    y += 8;
    if (y > 270) {
      doc.addPage();
      y = 20;
    }
  });
  doc.save(`${fileBase}.pdf`);
}

export function exportRowsAsSvg(rows, title, fileBase) {
  const lineHeight = 18;
  const height = Math.max(120, 40 + rows.length * lineHeight);
  const lines = [
    `<text x="20" y="28" font-size="16" font-family="Arial" font-weight="bold">${escapeSvgText(title)}</text>`,
    ...rows.map((row, index) => {
      const y = 52 + index * lineHeight;
      return `<text x="20" y="${y}" font-size="12" font-family="Arial">${escapeSvgText(Object.values(row).join(' | '))}</text>`;
    }),
  ];
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="800" height="${height}" viewBox="0 0 800 ${height}"><rect width="100%" height="100%" fill="#ffffff"/>${lines.join('')}</svg>`;
  downloadBlob(new Blob([svg], { type: 'image/svg+xml;charset=utf-8' }), `${fileBase}.svg`);
}

export async function exportDataset(format, { rows, title, fileBase }) {
  if (!rows.length) return;
  if (format === 'excel') {
    await exportRowsAsExcel(rows, fileBase);
    return;
  }
  if (format === 'pdf') {
    await exportRowsAsPdf(rows, title, fileBase);
    return;
  }
  if (format === 'svg') {
    exportRowsAsSvg(rows, title, fileBase);
  }
}

export async function exportChallengeBreakdown(format, { studentName, classData, fileBase }) {
  const rows = collectIncorrectExportRows(classData, studentName);
  if (!rows.length) {
    rows.push({
      'Student Name': studentName,
      'Incorrect Class': '—',
      'Incorrect Method': 'No incorrect methods found',
    });
  }
  await exportDataset(format, {
    rows,
    title: `Incorrect methods — ${studentName}`,
    fileBase,
  });
}

export async function exportRosterRows(format, { rows, labName, fileBase }) {
  await exportDataset(format, {
    rows,
    title: `Lab Student Roster — ${labName}`,
    fileBase: fileBase || `lab_${String(labName).replace(/\s+/g, '_')}_roster`,
  });
}

export function buildGradeOverviewExportRows({ labs, students }) {
  const labColumns = Array.isArray(labs) ? labs : [];
  const studentRows = Array.isArray(students) ? students : [];
  return studentRows.map((student) => {
    const row = {
      Student: formatText(student.studentName),
      IRN: formatText(student.irn),
      'Total Score': formatPercent(student.totalScore),
    };
    (student.labScores ?? []).forEach((score, index) => {
      const lab = labColumns[index];
      if (lab) {
        row[formatText(lab.labName)] = formatPercent(score);
      }
    });
    return row;
  });
}

export async function exportGradeOverview(format, { labs, students, fileBase }) {
  const rows = buildGradeOverviewExportRows({ labs, students });
  await exportDataset(format, {
    rows,
    title: 'Cross-lab Grade Overview',
    fileBase: fileBase || 'grade_overview_export',
  });
}
