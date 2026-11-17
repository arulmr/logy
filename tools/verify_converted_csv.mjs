import fs from "node:fs/promises";
import { Workbook } from "@oai/artifact-tool";

const [inputPath, previewPath] = process.argv.slice(2);
if (!inputPath || !previewPath) throw new Error("Usage: verify_converted_csv.mjs <input.csv> <preview.png>");

const csvText = await fs.readFile(inputPath, "utf8");
const workbook = await Workbook.fromCSV(csvText, { sheetName: "LogyImport" });
const sheet = workbook.worksheets.getItem("LogyImport");
const used = sheet.getUsedRange();
used.format.font = { name: "Arial", size: 10 };
used.format.autofitColumns();
sheet.getRange("J1:J100").format.columnWidth = 34;
workbook.recalculate();

const table = await workbook.inspect({
  kind: "table",
  range: "LogyImport!A1:L100",
  include: "values,formulas",
  tableMaxRows: 8,
  tableMaxCols: 12,
  maxChars: 8000,
});
const errors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!",
  options: { useRegex: true, maxResults: 100 },
  summary: "converted CSV error scan",
});
const preview = await workbook.render({ sheetName: "LogyImport", range: "A1:L25", scale: 1, format: "png" });
await fs.writeFile(previewPath, new Uint8Array(await preview.arrayBuffer()));

console.log(table.ndjson);
console.log(errors.ndjson);
