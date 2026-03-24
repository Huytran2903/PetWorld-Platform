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

    @BeforeAll
    public static void setUpExcel() {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("Test Results");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name Test Case");
        headerRow.createCell(1).setCellValue("Status");
        headerRow.createCell(2).setCellValue("Note");
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
            assertTrue(rows.size() <= 5, "Phân trang sai, hiển thị quá 5 người 1 trang!");
        try {
            WebElement pagination = driver.findElement(By.className("pagination"));
            assertTrue(pagination.isDisplayed(), "Thanh phân trang đang hoạt động tốt.");
        } catch (Exception e) {
            System.out.println("Không tìm thấy thanh phân trang.");
        }

        testStatus = "PASSED";
        failMessage = "Hoạt động chính xác";
    }

    @Test
    @Order(2)
    public void testSearchStaff() {
        driver.get("http://localhost:8080/admin/staff-manage");
        WebElement searchInput = driver.findElement(By.name("keyword"));
        searchInput.sendKeys("Huy Tran");
        searchInput.sendKeys(Keys.ENTER);

        wait.until(ExpectedConditions.urlContains("keyword="));
        assertTrue(driver.getCurrentUrl().contains("keyword="));

        testStatus = "PASSED";
        failMessage = "Tìm kiếm chính xác";
    }

    @Test
    @Order(3)
    public void testCreateNewStaff() {
        driver.get("http://localhost:8080/admin/staff-manage/create");

        driver.findElement(By.name("fullName")).sendKeys("Nhân Viên Auto Test");
        driver.findElement(By.name("email")).sendKeys("autotest@gmail.com");
        driver.findElement(By.name("username")).sendKeys("hihihaha");
        driver.findElement(By.name("phone")).sendKeys("0999888777");

        Select roleSelect = new Select(driver.findElement(By.name("roleId")));
        roleSelect.selectByIndex(2);

        WebElement submitBtn = driver.findElement(By.xpath("//button[@type='submit']"));
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("arguments[0].click();", submitBtn);

        wait.until(ExpectedConditions.urlContains("/admin/staff-manage"));
        assertTrue(driver.getCurrentUrl().endsWith("/admin/staff-manage"));

        testStatus = "PASSED";
        failMessage = "Thêm mới nhân viên thành công";
    }

    @Test
    @Order(4)
    public void testDeleteAndTransferStaff() {
        driver.get("http://localhost:8080/admin/staff-manage");

        WebElement firstDeleteBtn = wait.until(ExpectedConditions.elementToBeClickable(By.className("delete-staff-btn")));
        firstDeleteBtn.click();

        WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("transferDeleteModal")));
        assertTrue(modal.isDisplayed(), "Modal Xóa không hiện lên!");

        try {
            Select transferSelect = new Select(driver.findElement(By.name("transferStaffId")));
            transferSelect.selectByIndex(1);
        } catch (Exception e) {
            System.out.println("Nhân viên này không có việc pending");
        }

        WebElement submitDeleteBtn = driver.findElement(By.id("btnSubmitDelete"));
        submitDeleteBtn.click();

        wait.until(ExpectedConditions.urlContains("/admin/staff-manage"));
        assertTrue(driver.getCurrentUrl().contains("/admin/staff-manage"));

        testStatus = "PASSED";
        failMessage = "Xóa và bàn giao hoàn tất";
    }


    @AfterEach
    public void tearDown(TestInfo testInfo) throws InterruptedException {
        String testName = testInfo.getDisplayName();

        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(testName);
        row.createCell(1).setCellValue(testStatus);
        row.createCell(2).setCellValue(failMessage);

        Thread.sleep(5000);
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