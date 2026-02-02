
package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ExcelWriters {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Ensure sheet (City) exists with headers A1..C1. */
    public static void ensureSheetWithHeaders(String path, String sheetName) throws IOException {
        File file = new File(path);
        XSSFWorkbook wb;

        if (file.exists()) {
            try (FileInputStream fi = new FileInputStream(file)) {
                wb = new XSSFWorkbook(fi);
            }
        } else {
            wb = new XSSFWorkbook();
        }

        XSSFSheet sheet = wb.getSheet(sheetName);
        if (sheet == null) sheet = wb.createSheet(sheetName);

        Row header = sheet.getRow(0);
        if (header == null) header = sheet.createRow(0);

        setCellString(header, 0, "Hotel Name");
        setCellString(header, 1, "Price");
        setCellString(header, 2, "Ratings");

        CellStyle bold = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        bold.setFont(font);
        for (int c = 0; c <= 2; c++) {
            Cell cell = header.getCell(c);
            if (cell != null) cell.setCellStyle(bold);
            sheet.autoSizeColumn(c);
        }

        try (FileOutputStream fo = new FileOutputStream(file)) {
            wb.write(fo);
        }
        wb.close();
    }

    /** Clear data rows but keep header row (recreate sheet to avoid holes). */
    public static void clearDataKeepHeader(String path, String sheetName) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            ensureSheetWithHeaders(path, sheetName);
            return;
        }

        try (FileInputStream fi = new FileInputStream(file);
             XSSFWorkbook wb = new XSSFWorkbook(fi)) {

            int idx = wb.getSheetIndex(sheetName);
            XSSFSheet oldSheet = (idx >= 0) ? wb.getSheetAt(idx) : wb.createSheet(sheetName);

            Row header = oldSheet.getRow(0);
            String h0 = getCellText(header, 0);
            String h1 = getCellText(header, 1);
            String h2 = getCellText(header, 2);

            if (idx >= 0) wb.removeSheetAt(idx);
            else {
                int newIdx = wb.getSheetIndex(oldSheet);
                if (newIdx >= 0) wb.removeSheetAt(newIdx);
            }

            XSSFSheet sheet = wb.createSheet(sheetName);
            Row newHeader = sheet.createRow(0);
            setCellString(newHeader, 0, (h0 == null || h0.isBlank()) ? "Hotel Name" : h0);
            setCellString(newHeader, 1, (h1 == null || h1.isBlank()) ? "Price" : h1);
            setCellString(newHeader, 2, (h2 == null || h2.isBlank()) ? "Ratings" : h2);

            CellStyle bold = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            bold.setFont(font);
            for (int c = 0; c <= 2; c++) {
                Cell cell = newHeader.getCell(c);
                if (cell != null) cell.setCellStyle(bold);
                sheet.autoSizeColumn(c);
            }

            try (FileOutputStream fo = new FileOutputStream(file)) {
                wb.write(fo);
            }
        }
    }

    /** Write Check-in/Check-out on the right (header rows): F1/G1 and F2/G2. */
    public static void writeCheckInOutSideBlock(String path, String sheetName,
                                                LocalDate checkIn, LocalDate checkOut) throws IOException {

        try (FileInputStream fi = new FileInputStream(path);
             XSSFWorkbook wb = new XSSFWorkbook(fi)) {

            XSSFSheet sheet = wb.getSheet(sheetName);
            if (sheet == null) sheet = wb.createSheet(sheetName);

            Row r0 = sheet.getRow(0);
            if (r0 == null) r0 = sheet.createRow(0);
            setCellString(r0, 5, "Check-in");            // F1
            setCellString(r0, 6, checkIn.format(DATE_FMT));  // G1

            Row r1 = sheet.getRow(1);
            if (r1 == null) r1 = sheet.createRow(1);
            setCellString(r1, 5, "Check-out");           // F2
            setCellString(r1, 6, checkOut.format(DATE_FMT)); // G2

            sheet.autoSizeColumn(5);
            sheet.autoSizeColumn(6);

            try (FileOutputStream fo = new FileOutputStream(path)) {
                wb.write(fo);
            }
        }
    }

    /** Append hotel row to A..C without gaps (scan A..C only). */
    public static int appendHotelRow(String path, String sheetName,
                                     String hotelName, String price, String ratings) throws IOException {
        try (FileInputStream fi = new FileInputStream(path);
             XSSFWorkbook wb = new XSSFWorkbook(fi)) {

            XSSFSheet sheet = wb.getSheet(sheetName);
            if (sheet == null) sheet = wb.createSheet(sheetName);

            int writeIdx = findNextWriteRowAC(sheet);
            Row row = sheet.getRow(writeIdx);
            if (row == null) row = sheet.createRow(writeIdx);

            setCellString(row, 0, hotelName);
            setCellString(row, 1, price);
            setCellString(row, 2, ratings);

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);

            try (FileOutputStream fo = new FileOutputStream(path)) {
                wb.write(fo);
            }
            return writeIdx;
        }
    }

    // --------- helpers ---------

    private static Row getOrCreateRow(Sheet s, int rowIndex) {
        Row r = s.getRow(rowIndex);
        if (r == null) r = s.createRow(rowIndex);
        return r;
    }

    private static void setCellString(Row row, int col, String val) {
        Cell cell = row.getCell(col);
        if (cell == null) cell = row.createCell(col);
        cell.setCellValue(val == null ? "" : val);
    }

    private static String getCellText(Row row, int col) {
        if (row == null) return null;
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return new DataFormatter().formatCellValue(cell);
    }

    /** First empty row >= 1 considering only A..C */
    private static int findNextWriteRowAC(Sheet sheet) {
        int r = 1;
        while (true) {
            Row row = sheet.getRow(r);
            if (row == null || isRowACEmpty(row)) return r;
            r++;
        }
    }

    private static boolean isRowACEmpty(Row row) {
        DataFormatter fmt = new DataFormatter();
        for (int c = 0; c <= 2; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String txt = fmt.formatCellValue(cell);
                if (txt != null && !txt.isBlank()) return false;
            }
        }
        return true;
    }
}