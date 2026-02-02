package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.List;

import utils.ExcelWriters;

public class SearchResultsPage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    public SearchResultsPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        PageFactory.initElements(driver, this);
    }

    // --- Sort UI ---
    @FindBy(css = "button[name='sorting_selector']")
    private WebElement sortDropdown;

    @FindBy(xpath = "//label[.//text()='Top guest ratings']")
    private WebElement topGuestRatingsOption;

    @FindBy(xpath = "//button[normalize-space()='Apply' or contains(@data-testid,'apply')]")
    private WebElement applyButton;

    // --- Result cards ---
    private final By cardSelector = By.cssSelector("li[data-testid='accommodation-list-element']");
    private final By hotelNameInCard = By.cssSelector("span[itemprop='name']");
    private final By pricePrimaryInCard = By.cssSelector("div[itemprop='price']");
    private final By priceAltInCard = By.cssSelector("span[data-testid='recommended-price']");
    private final By ratingsCard = By.cssSelector("span[itemprop='ratingValue']");
    private final By nextPageButton = By.cssSelector("button[data-testid='next-result-page']");
    private final By topGuestRatings = By.xpath("//label[.//text()='Top guest ratings']");

    public void openSortDropdownOnly() {
        wait.until(ExpectedConditions.elementToBeClickable(sortDropdown)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(topGuestRatings));
    }

    public void selectTopGuestRatingsOnly() {
        wait.until(ExpectedConditions.elementToBeClickable(topGuestRatings)).click();
        try {
            wait.until(ExpectedConditions.elementToBeClickable(applyButton)).click();
        } catch (Exception ignored) {}
        waitUntilResultsPresent();
    }

    public void waitUntilResultsPresent() {
        wait.until(d -> !d.findElements(cardSelector).isEmpty());
    }

    /** Print to console and write to Excel (A..C) without gaps. */
    public void printHotelsAndWriteExcel(String excelPath, String sheetName) {
        waitUntilResultsPresent();
        try {
            ExcelWriters.ensureSheetWithHeaders(excelPath, sheetName);
        } catch (Exception e) {
            System.err.println("[Excel] ensureSheetWithHeaders failed: " + e.getMessage());
        }

        int total = driver.findElements(cardSelector).size();
        for (int i = 0; i < total; i++) {
            try {
                WebElement card = driver.findElements(cardSelector).get(i);
                String name   = safeGetText(card, hotelNameInCard, "Name not found");
                String price  = safeGetPrice(card);
                String rating = safeGetRatings(card, ratingsCard, "");

//                System.out.println("Hotel Name: " + name + " \n Price: " + price + " \n Rating: " + rating);

                try {
                    ExcelWriters.appendHotelRow(excelPath, sheetName, name, price, rating);
                } catch (Exception x) {
                    System.err.println("[Excel] appendHotelRow failed: " + x.getMessage());
                }
            } catch (StaleElementReferenceException ignored) {
                // retry next card
            } catch (IndexOutOfBoundsException ignored) {
                break;
            }
        }
    }
    /** Writes hotels for the first 'pages' pages (page 1 = current). */
    public void writeHotelsForFirstNPages(String excelPath, String sheetName, int pages) {
        if (pages < 1) pages = 1;

        for (int p = 1; p <= pages; p++) {
            if (p == 1) {
                // current page (already loaded and sorted)
                System.out.println("[Pagination] Writing page " + p);
                printHotelsAndWriteExcel(excelPath, sheetName);
                continue;
            }

            System.out.println("[Pagination] Navigating to page " + p);
            WebElement previousFirst = firstCardOrNull();

            if (!clickNextIfEnabled()) {
                System.out.println("[Pagination] Next button not available. Stopping at page " + (p - 1));
                break;
            }

            // wait for staleness of previous first (if any), then stabilize results
            if (previousFirst != null) {
                try { wait.until(ExpectedConditions.stalenessOf(previousFirst)); }
                catch (TimeoutException ignored) {}
            }
            waitUntilResultsPresent();

            System.out.println("[Pagination] Writing page " + p);
            printHotelsAndWriteExcel(excelPath, sheetName);
        }
    }

    /** Returns first card element if present, else null. */
    private WebElement firstCardOrNull() {
        List<WebElement> cards = driver.findElements(cardSelector);
        return cards.isEmpty() ? null : cards.get(0);
    }

    /** Clicks next if it is present and enabled; returns true if a click was performed. */
    private boolean clickNextIfEnabled() {
        WebElement btn;
        try {
            btn = wait.until(ExpectedConditions.presenceOfElementLocated(nextPageButton));
        } catch (TimeoutException e) {
            return false;
        }
        if (!isEnabled(btn)) return false;

        scrollIntoView(btn);
        try {
            wait.until(ExpectedConditions.elementToBeClickable(btn)).click();
            return true;
        } catch (Exception e) {
            // try JS click as fallback
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    /** Heuristic "enabled" check for the pagination button. */
    private boolean isEnabled(WebElement el) {
        if (el == null) return false;
        String disabled = attr(el, "disabled");
        String aria = attr(el, "aria-disabled");
        return el.isDisplayed() && el.isEnabled()
                && (disabled == null)
                && !"true".equalsIgnoreCase(aria);
    }

    private String attr(WebElement el, String name) {
        try { return el.getAttribute(name); } catch (Exception e) { return null; }
    }

    private void scrollIntoView(WebElement el) {
        try { ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el); }
        catch (Exception ignored) {}
    }

    // --- helpers ---
    private String safeGetPrice(WebElement card) {
        try {
            return extractCleanPrice(card.findElement(pricePrimaryInCard).getText());
        } catch (NoSuchElementException | StaleElementReferenceException e1) {
            try {
                return extractCleanPrice(card.findElement(priceAltInCard).getText());
            } catch (NoSuchElementException | StaleElementReferenceException e2) {
                return "Price not found";
            }
        }
    }

    private String safeGetText(WebElement scope, By by, String fallback) {
        try {
            return scope.findElement(by).getText();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return fallback;
        }
    }

    private String safeGetRatings(WebElement scope, By by, String fallback) {
        try {
            String txt = scope.findElement(by).getText();
            return (txt == null || txt.isBlank()) ? fallback : txt.trim();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return fallback;
        }
    }

    private String extractCleanPrice(String rawText) {
        if (rawText == null) return "Price not found";
        int index = rawText.indexOf("₹");
        if (index != -1) return rawText.substring(index).trim();
        return rawText.trim().isEmpty() ? "Price not found" : rawText.trim();
    }

    public int getResultCount() {
        return driver.findElements(cardSelector).size();
    }
}
