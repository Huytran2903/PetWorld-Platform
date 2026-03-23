package vn.edu.fpt.petworldplatform.UITest;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StaffManageUITest {

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    public void setUp() {
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

        // Chờ đến khi vào được dashboard
        wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
    }

    // --- CHỨC NĂNG HIỂN THỊ DANH SÁCH & PHÂN TRANG ---
    @Test
    @Order(1)
    public void testShowStaffList() {
        // 1. Truy cập vào trang quản lý nhân viên
        driver.get("http://localhost:8080/admin/staff-manage");

        wait.until(ExpectedConditions.urlContains("/admin/staff-manage"));

        WebElement table = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
        assertTrue(table.isDisplayed(), "Lỗi: Bảng danh sách nhân viên không hiển thị!");

        java.util.List<WebElement> rows = driver.findElements(By.xpath("//table/tbody/tr"));
        assertTrue(rows.size() > 0, "Lỗi: Bảng không có dữ liệu nhân viên nào!");

        assertTrue(rows.size() <= 5, "Lỗi: Phân trang sai, hiển thị quá 5 người 1 trang!");

        try {
            WebElement pagination = driver.findElement(By.className("pagination"));
            assertTrue(pagination.isDisplayed(), "Thanh phân trang đang hoạt động tốt.");
        } catch (Exception e) {
            System.out.println("Không tìm thấy thanh phân trang (Có thể do tổng số nhân viên <= 5 nên không hiện).");
        }
    }

    // --- CHỨC NĂNG TÌM KIẾM ---
    @Test
    @Order(2)
    public void testSearchStaff() {
        driver.get("http://localhost:8080/admin/staff-manage");

        WebElement searchInput = driver.findElement(By.name("keyword"));

        searchInput.sendKeys("Huy Tran");
        searchInput.sendKeys(Keys.ENTER);

        wait.until(ExpectedConditions.urlContains("keyword="));
        assertTrue(driver.getCurrentUrl().contains("keyword="));
    }

    // --- CHỨC NĂNG THÊM NHÂN VIÊN ---
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
    }

    // --- MODAL XÓA & BÀN GIAO CÔNG VIỆC ---
    @Test
    @Order(4)
    public void testDeleteAndTransferStaff() {
        driver.get("http://localhost:8080/admin/staff-manage");

        WebElement firstDeleteBtn = wait.until(ExpectedConditions.elementToBeClickable(By.className("delete-staff-btn")));
        firstDeleteBtn.click();

        WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("transferDeleteModal")));
        assertTrue(modal.isDisplayed(), "Lỗi: Modal Xóa không hiện lên!");

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
    }

    // --- DỌN DẸP ---
    @AfterEach
    public void tearDown() throws InterruptedException {
        Thread.sleep(2000);
        if (driver != null) {
            driver.quit();
        }
    }
}
