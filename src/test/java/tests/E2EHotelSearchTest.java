
package tests;

import base.BaseTest;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pages.HomePage;
import pages.SearchResultsPage;
import utils.ExcelWriters;
import utils.ScreenshotUtil;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.*;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 - Reads ALL valid rows from test-data/TestData.xlsx / SearchRuns
 - Each row => City, CheckIn(yyyy-MM-dd), CheckOut(yyyy-MM-dd)
 - Each row performs full E2E automation in a clean browser session
 - Excel output: each city gets its own sheet in CityResults.xlsx
 - Screenshots saved under: target/screenshots/
 **/

public class E2EHotelSearchTest extends BaseTest {

    private static final String INPUT_FILE  = "RunSet.xlsx";
    private static final String INPUT_SHEET = "SearchRuns";
    private static final String OUTPUT_FILE = "CityResults.xlsx";

    // --------------------- DATA PROVIDER --------------------- //
    @DataProvider(name = "searchData")
    public Object[][] searchData() throws Exception {

        Path input = Path.of(System.getProperty("user.dir"), "test-data", INPUT_FILE);

        if (!Files.exists(input)) {
            throw new SkipException("Input Excel not found at " + input);
        }

        List<Object[]> validRows = new ArrayList<>();

        try (FileInputStream fi = new FileInputStream(input.toFile());
             XSSFWorkbook wb = new XSSFWorkbook(fi)) {

            XSSFSheet ws = wb.getSheet(INPUT_SHEET);
            if (ws == null) {
                throw new SkipException("Sheet '" + INPUT_SHEET + "' not found in " + input);
            }

            DataFormatter fmt = new DataFormatter();
            int last = ws.getLastRowNum();

            for (int r = 1; r <= last; r++) { // row=1 => Excel row 2
                XSSFRow row = ws.getRow(r);
                if (row == null) continue;

                String city        = fmt.formatCellValue(row.getCell(0));
                String checkInStr  = fmt.formatCellValue(row.getCell(1));
                String checkOutStr = fmt.formatCellValue(row.getCell(2));

                if (isBlank(city) || isBlank(checkInStr) || isBlank(checkOutStr))
                    continue;

                // Validate date format
                LocalDate ci, co;
                try {
                    ci = LocalDate.parse(checkInStr.trim());
                    co = LocalDate.parse(checkOutStr.trim());
                } catch (DateTimeParseException e) {
                    continue; // skip invalid date format
                }

                if (!co.isAfter(ci)) continue; // skip invalid date order

                validRows.add(new Object[]{
                        city.trim(),
                        ci.toString(),
                        co.toString()
                });
            }
        }

        if (validRows.isEmpty()) {
            throw new SkipException("No valid rows found in " + INPUT_FILE + " / " + INPUT_SHEET);
        }

        System.out.println("==================== DATA PROVIDER ====================");
        System.out.println("Total valid rows found: " + validRows.size());
        for (Object[] arr : validRows) {
            System.out.println("ROW: " + Arrays.toString(arr));
        }
        System.out.println("========================================================\n");

        return validRows.toArray(new Object[0][]);
    }

    // --------------------- SINGLE E2E FLOW PER ROW --------------------- //
    @Test(dataProvider = "searchData")
    public void e2e_flow_excel(String city, String checkInStr, String checkOutStr) throws Exception {

        LocalDate checkIn  = LocalDate.parse(checkInStr);
        LocalDate checkOut = LocalDate.parse(checkOutStr);

        // 1) Open Trivago
        driver.get("https://trivago.in/");
        ScreenshotUtil.takeScreenshot(driver, "01_open_Trivago", "Info");

        HomePage home = new HomePage(driver);
        SearchResultsPage results = new SearchResultsPage(driver);

        // 2) City
        home.enterDestination(city);
        ScreenshotUtil.takeScreenshot(driver, "02_city_" + city, "Info");

        // 3) Dates
        home.selectDateRange(checkIn, checkOut);
        ScreenshotUtil.takeScreenshot(driver, "03_dates_In/Out for " + city, "Info");

        // 4) Guests
        home.adjustGuests();
        ScreenshotUtil.takeScreenshot(driver, "04_guests_set_to_1 for " + city, "Info");

        // 5) Sort dropdown open
        results.openSortDropdownOnly();
        ScreenshotUtil.takeScreenshot(driver, "05_sort_open_dropdown for " + city, "Info");

        // 6) Select Top Guest Ratings
        results.selectTopGuestRatingsOnly();
        ScreenshotUtil.takeScreenshot(driver, "06_sorted_Top_Guest_Ratings for " + city, "Info");

        // 7) Validate results exist
        results.waitUntilResultsPresent();
        int count = results.getResultCount();
        Assert.assertTrue(count > 0, "Expected > 0 results; actual: " + count);

        // 8) Excel output (each city has its own sheet)
        Path output = Path.of(System.getProperty("user.dir"), "test-data", OUTPUT_FILE);

        try {
            ExcelWriters.ensureSheetWithHeaders(output.toString(), city);
            ExcelWriters.clearDataKeepHeader(output.toString(), city);
            ExcelWriters.writeCheckInOutSideBlock(output.toString(), city, checkIn, checkOut);
        } catch (Exception e) {
            System.err.println("[Excel] Failed writing header/check-in block: " + e.getMessage());
        }

        // AFTER: write first 3 pages (only first page screenshots are already taken)
        results.writeHotelsForFirstNPages(output.toString(), city, 2);

    }

    // ----------------------------- HELPERS ----------------------------- //
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
