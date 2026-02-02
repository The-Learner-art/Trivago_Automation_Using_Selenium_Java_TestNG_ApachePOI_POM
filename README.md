# Trivago Hotel Search Automation Framework

## 📌 Project Overview
This project is an automated end-to-end (E2E) testing framework designed to validate hotel search functionality on Trivago. Built using **Selenium WebDriver** and **Java**, it follows the **Page Object Model (POM)** design pattern to ensure code reusability and maintainability.

The framework is data-driven, reading search criteria from an Excel file and exporting result data (Hotel Name, Price, Ratings) back into a detailed Excel report with automated screenshots for every test step.

## 🛠️ Tech Stack
* **Language:** Java
* **Automation:** Selenium WebDriver (4.x)
* **Test Framework:** TestNG
* **Build Tool:** Maven
* **Reporting:** Apache POI (Excel) & Custom Screenshot Utility

## 📂 Project Structure
Based on the project hierarchy:
* **`src/main/java/pages`**: Contains Page Objects (`HomePage.java`, `SearchResultsPage.java`).
* **`src/main/java/utils`**: Custom helpers for Excel operations (`ExcelWriters.java`) and capturing evidence (`ScreenshotUtil.java`).
* **`src/test/java/base`**: Contains `BaseTest.java` for browser setup and teardown.
* **`src/test/java/tests`**: The main execution logic in `E2EHotelSearchTest.java`.
* **`test-data/`**: Directory for input (`RunSet.xlsx`) and output (`CityResults.xlsx`).

## 🚀 Key Features
* **Data-Driven Testing:** Reads multiple test cases (City, Check-in, Check-out) from `RunSet.xlsx`.
* **Page Object Model:** Separates UI locators and page-specific actions for cleaner code.
* **Automated Excel Reporting:**
    * Creates a new sheet for every city searched.
    * Records Hotel Name, Price, and Ratings.
    * Automatically handles pagination to scrape multiple pages of results.
* **Visual Evidence:** Automatically captures screenshots at every milestone (Search, Date Selection, Sorting) and organizes them into city-specific folders under `target/screenshots/`.
* **Smart Synchronization:** Implements explicit and fluent waits to handle dynamic elements and loading states.

## 📋 Prerequisites
* Java JDK 17 or higher
* Maven installed
* Google Chrome, Edge, or Firefox

## ⚙️ How to Run
1. **Clone the Repository:**
   ```bash
   git clone [https://github.com/your-username/your-repository-name.git](https://github.com/your-username/your-repository-name.git)
2. **Prepare Test Data:**
   Open test-data/RunSet.xlsx and add your desired cities and dates (format: yyyy-MM-dd).
3. **Execute**:
   Run from your IDE (Eclipse or IntelliJ IDEA)
   [OR]
   ```bash
   mvn test
4. **View Results:**
   - Open ```test-data/CityResults.xlsx``` for the hotel data.
   - Check ```target/screenshots/``` for visual logs of the execution.

## 📝 Test Flow
1. Open Trivago: Navigates to the homepage.
2. Enter Destination: Types the city and selects the first suggestion.
3. Select Dates: Handles the calendar popover to select check-in and check-out dates.
4. Guest Adjustment: Modifies guest counts as required.
5. Sort Results: Opens the sort dropdown and applies the "Top guest ratings" filter.
6. Data Extraction: Scrapes the results across multiple pages and writes to Excel.
