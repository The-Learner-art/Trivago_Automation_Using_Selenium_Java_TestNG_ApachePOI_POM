
package utils;

import org.openqa.selenium.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotUtil {

    private static final String ROOT_DIR = "target";
    private static final String SCREENSHOTS_DIR = "screenshots";

    public static String takeScreenshot(WebDriver driver, String fileBase, String status) {
        if (driver == null) return null;

        String safeBase   = sanitize(fileBase);
        String safeStatus = sanitize(status);

        // EXTRACT CITY NAME → last part after underscore
        // Example: 02_city_Mumbai_Info  → parts = ["02","city","Mumbai","Info"]
        String city = extractCityFromBase(safeBase);

        // Create a folder per city
        Path dir = Paths.get(System.getProperty("user.dir"), ROOT_DIR, SCREENSHOTS_DIR, city);

        String fileName = safeBase + "_" + safeStatus + ".png";
        Path dest = dir.resolve(fileName);

        try {
            Files.createDirectories(dir);
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[Screenshot] " + dest.toAbsolutePath());
            return dest.toAbsolutePath().toString();
        } catch (WebDriverException | IOException e) {
            System.err.println("[Screenshot][Error] " + e.getMessage());
            return null;
        }
    }

    private static String sanitize(String name) {
        if (name == null) return "shot";
        return name.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    private static String extractCityFromBase(String base) {
        try {
            String[] parts = base.split("_");
            if (parts.length >= 3) {
                // Last meaningful part is usually the city (before status)
                // Eg: "05_sort_open_Vijayawada" → last = "Vijayawada"
                return parts[parts.length - 1];
            }
        } catch (Exception ignored) {}

        return "UnknownCity"; // fallback
    }
}
