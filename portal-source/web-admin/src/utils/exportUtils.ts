/**
 * CSV Export Utility with UTF-8 BOM encoding.
 * Ensures Bengali script and Unicode characters render correctly in Microsoft Excel, LibreOffice, and Google Sheets.
 */

export interface ExportColumn<T> {
  key: keyof T | string;
  header: string;
  transform?: (val: unknown, item: T) => string;
}

export interface TabularCsvOptions {
  filename: string;
  headers: string[];
  rows: Array<Array<string | number | boolean | null | undefined>>;
}

function downloadCsvString(filename: string, csvLines: string[]): void {
  // Prepend \uFEFF (UTF-8 Byte Order Mark) so Excel recognizes UTF-8 Bengali encoding natively
  const csvContent = '\uFEFF' + csvLines.join('\r\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  const cleanName = filename.endsWith('.csv')
    ? filename
    : `${filename}_${new Date().toISOString().slice(0, 10)}.csv`;

  link.setAttribute('href', url);
  link.setAttribute('download', cleanName);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export function exportToCsv(options: TabularCsvOptions): void;
export function exportToCsv<T extends Record<string, unknown>>(
  filename: string,
  columns: ExportColumn<T>[],
  data: T[]
): void;
export function exportToCsv<T extends Record<string, unknown>>(
  filenameOrOptions: string | TabularCsvOptions,
  columns?: ExportColumn<T>[],
  data?: T[]
): void {
  if (typeof filenameOrOptions === 'object') {
    const { filename, headers, rows } = filenameOrOptions;
    if (!rows || rows.length === 0) return;

    const headerRow = headers
      .map((h) => `"${String(h ?? '').replace(/"/g, '""')}"`)
      .join(',');
    const bodyRows = rows.map((row) =>
      row.map((cell) => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(',')
    );
    downloadCsvString(filename, [headerRow, ...bodyRows]);
    return;
  }

  if (!data || data.length === 0 || !columns) return;

  const headerRow = columns
    .map((col) => `"${String(col.header).replace(/"/g, '""')}"`)
    .join(',');

  const rows = data.map((item) =>
    columns
      .map((col) => {
        const rawVal = item[col.key as keyof T];
        const valStr = col.transform ? col.transform(rawVal, item) : String(rawVal ?? '');
        return `"${valStr.replace(/"/g, '""')}"`;
      })
      .join(',')
  );

  downloadCsvString(filenameOrOptions, [headerRow, ...rows]);
}
