package vn.edu.fpt.petworldplatform.UITest;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StaffManageUITest {

    private WebDriver driver;
    private WebDriverWait wait;

    private static Workbook workbook;
    private static Sheet sheet;
    private static int rowNum = 1;

    private String testStatus;
    private String failMessage;

    private static CellStyle headerStyle;
    private static CellStyle passedStyle;
    private static CellStyle failedStyle;

    @BeforeAll
    public static void setUpExcel() {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("Test Results");

        headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        passedStyle = workbook.createCellStyle();
        Font passedFont = workbook.createFont();
        passedFont.setColor(IndexedColors.GREEN.getIndex());
        passedFont.setBold(true);
        passedStyle.setFont(passedFont);

        failedStyle = workbook.createCellStyle();
        Font failedFont = workbook.createFont();
        failedFont.setColor(IndexedColors.RED.getIndex());
        failedFont.setBold(true);
        failedStyle.setFont(failedFont);

        Row headerRow = sheet.createRow(0);

        Cell cell0 = headerRow.createCell(0);
        cell0.setCellValue("Name Test Case");
        cell0.setCellStyle(headerStyle);

        Cell cell1 = headerRow.createCell(1);
        cell1.setCellValue("Status");
        cell1.setCellStyle(headerStyle);

        Cell cell2 = headerRow.createCell(2);
        cell2.setCellValue("Note");
        cell2.setCellStyle(headerStyle);
    }

    @BeforeEach
    public void setUp(TestInfo testInfo) {
        testStatus = "FAILED";
        failMessage = "Lỗi không xác định hoặc AssertionError";

        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        loginAsAdmin();
    }

    private void loginAsAdmin() {
        driver.get("http://localhost:8080/login");
        driver.findElement(By.name("username")).sendKeys("huytranAdmin");
        driver.findElement(By.name("password")).sendKeys("3d5febde");
        driver.findElement(By.xpath("//button[@type='submit']")).click();

        wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
    }

    @Test
    @Order(1)
    public void testShowStaffList() {
        driver.get("http://localhost:8080/admin/staff-manage");
        wait.until(ExpectedConditions.urlContains("/admin/staff-manage"));

        WebElement table = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
        assertTrue(table.isDisplayed(), "Bảng danh sách nhân viên không hiển thị!");

        java.util.List<WebElement> rows = driver.findElements(By.xpath("//table/tbody/tr"));
        assertTrue(rows.size() > 0, "Bảng không có dữ liệu nhân viên nào!");

        testStatus = "PASSED";
        failMessage = "Hoạt động chính xác";
    }


    @Test
    @Order(2)
    public void testCreateStaff_Success() {
        driver.get("http://localhost:8080/admin/staff-manage/create");

        driver.findElement(By.name("fullName")).sendKeys("Nhân Viên Hợp Lệ");
        driver.findElement(By.name("email")).sendKeys("valid.email@gmail.com");
        driver.findElement(By.name("username")).sendKeys("validUser123");
        driver.findElement(By.name("phone")).sendKeys("0988777666");

        Select roleSelect = new Select(driver.findElement(By.name("roleId")));
        roleSelect.selectByIndex(2);

        submitForm();

        wait.until(ExpectedConditions.urlContains("/admin/staff-manage"));
        assertTrue(driver.getCurrentUrl().endsWith("/admin/staff-manage"), "Thêm nhân viên thất bại!");

        testStatus = "PASSED";
        failMessage = "Thêm mới nhân viên thành công";
    }

    @Test
    @Order(3)
    public void testCreateStaff_EmptyAllFields() {
        driver.get("http://localhost:8080/admin/staff-manage/create");

        submitForm();

        assertTrue(driver.getCurrentUrl().contains("/create"), "Lỗi: Đã cho phép tạo nhân viên rỗng!");
        testStatus = "PASSED";
        failMessage = "Bắt lỗi để trống các trường (NotBlank, NotNull)";
    }


    @Test
    @Order(5)
    public void testCreateStaff_InvalidEmailFormat() {
        driver.get("http://localhost:8080/admin/staff-manage/create");

        fillBasicValidData();

        WebElement emailInput = driver.findElement(By.name("email"));
        emailInput.clear();
        emailInput.sendKeys("not-an-email-format");

        submitForm();

        assertTrue(driver.getCurrentUrl().contains("/create"), "Lỗi: Không bắt lỗi định dạng Email");
        testStatus = "PASSED";
        failMessage = "Bắt lỗi sai định dạng Email thành công";
    }



    @Test
    @Order(6)
    public void testUpdateStaffInfo() {
        driver.get("http://localhost:8080/admin/staff-manage");

        WebElement firstEditBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@href, '/admin/edit-staff/')]")
        ));
        firstEditBtn.click();

        wait.until(ExpectedConditions.urlContains("/edit-staff/"));

        WebElement phoneInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("phone")));
        phoneInput.clear();
        phoneInput.sendKeys("0123456789");

        submitForm();

        wait.until(ExpectedConditions.urlContains("/admin/staff-manage"));
        assertTrue(driver.getCurrentUrl().endsWith("/admin/staff-manage"), "Cập nhật nhân viên thất bại!");

        testStatus = "PASSED";
        failMessage = "Cập nhật thông tin nhân viên thành công";
    }

    @Test
    @Order(7)
    public void testDeleteStaff() {
        driver.get("http://localhost:8080/admin/staff-manage");

        WebElement firstDeleteBtn = wait.until(ExpectedConditions.elementToBeClickable(By.className("delete-staff-btn")));
        firstDeleteBtn.click();

        WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("transferDeleteModal")));
        assertTrue(modal.isDisplayed(), "Modal Xóa không hiện lên!");

        WebElement submitDeleteBtn = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("btnSubmitDelete")));
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("arguments[0].click();", submitDeleteBtn);

        wait.until(ExpectedConditions.urlContains("/admin/staff-manage"));
        assertTrue(driver.getCurrentUrl().contains("/admin/staff-manage"), "Lỗi khi xóa nhân viên!");

        testStatus = "PASSED";
        failMessage = "Xóa nhân viên thành công";
    }


    private void fillBasicValidData() {
        driver.findElement(By.name("fullName")).sendKeys("Test Validation");
        driver.findElement(By.name("email")).sendKeys("valid@gmail.com");
        driver.findElement(By.name("username")).sendKeys("validUsername");
        driver.findElement(By.name("phone")).sendKeys("0988777686");
        try {
            Select roleSelect = new Select(driver.findElement(By.name("roleId")));
            roleSelect.selectByIndex(2);
        } catch (Exception e) {
        }
    }

    private void submitForm() {
        WebElement submitBtn = driver.findElement(By.xpath("//button[@type='submit']"));
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("arguments[0].click();", submitBtn);
    }


    @AfterEach
    public void tearDown(TestInfo testInfo) throws InterruptedException {
        String testName = testInfo.getDisplayName();

        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(testName);

        Cell statusCell = row.createCell(1);
        statusCell.setCellValue(testStatus);
        if ("PASSED".equals(testStatus)) {
            statusCell.setCellStyle(passedStyle);
        } else {
            statusCell.setCellStyle(failedStyle);
        }

        row.createCell(2).setCellValue(failMessage);

        Thread.sleep(3000);
        if (driver != null) {
            driver.quit();
        }
    }

    @AfterAll
    public static void exportExcel() {
        try {
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);

            FileOutputStream out = new FileOutputStream("BaoCaoTest_StaffManage.xlsx");
            workbook.write(out);
            out.close();
            workbook.close();
            System.out.println("ĐÃ XUẤT FILE EXCEL THÀNH CÔNG: BaoCaoTest_StaffManage.xlsx");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}