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

    // --- BƯỚC 1: CÀI ĐẶT TRƯỚC KHI TEST ---
    @BeforeEach
    public void setUp() {
        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        // Gọi hàm đăng nhập trước để có quyền truy cập vào các trang /admin
        loginAsAdmin();
    }

    // Hàm hỗ trợ đăng nhập (Chạy ngầm trước mỗi bài test)
    private void loginAsAdmin() {
        driver.get("http://localhost:8080/login");
        driver.findElement(By.name("username")).sendKeys("huytran29");
        driver.findElement(By.name("password")).sendKeys("Huy@29032005");
        driver.findElement(By.xpath("//button[@type='submit']")).click();

        // Chờ đến khi vào được dashboard
        wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
    }

    // --- BƯỚC 1.5: TEST CHỨC NĂNG HIỂN THỊ DANH SÁCH & PHÂN TRANG ---
    @Test
    @Order(1)
    public void testShowStaffList() {
        // 1. Truy cập vào trang quản lý nhân viên
        driver.get("http://localhost:8080/admin/staff-manage");

        // 2. Chờ trang tải xong (URL phải đúng)
        wait.until(ExpectedConditions.urlContains("/admin/staff-manage"));

        // 3. Kiểm tra xem Bảng dữ liệu (Table) có xuất hiện không
        // Giả sử thẻ <table> của bạn có class "table" hoặc id cụ thể. Ở đây mình tìm thẻ <table> chung.
        WebElement table = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
        assertTrue(table.isDisplayed(), "Lỗi: Bảng danh sách nhân viên không hiển thị!");

        // 4. Kiểm tra xem bảng có dữ liệu không (Đếm số thẻ <tr> trong <tbody>)
        java.util.List<WebElement> rows = driver.findElements(By.xpath("//table/tbody/tr"));
        assertTrue(rows.size() > 0, "Lỗi: Bảng không có dữ liệu nhân viên nào!");

        // Theo code controller của bạn, size mặc định là 5, nên số dòng không được vượt quá 5
        assertTrue(rows.size() <= 5, "Lỗi: Phân trang sai, hiển thị quá 5 người 1 trang!");

        // 5. (Tùy chọn) Kiểm tra xem cụm phân trang (Pagination) có hiển thị không
        try {
            WebElement pagination = driver.findElement(By.className("pagination"));
            assertTrue(pagination.isDisplayed(), "Thanh phân trang đang hoạt động tốt.");
        } catch (Exception e) {
            System.out.println("Không tìm thấy thanh phân trang (Có thể do tổng số nhân viên <= 5 nên không hiện).");
        }
    }

    // --- BƯỚC 2: TEST CHỨC NĂNG TÌM KIẾM ---
    @Test
    @Order(2)
    public void testSearchStaff() {
        driver.get("http://localhost:8080/admin/staff-manage");

        // Tìm ô nhập từ khóa
        WebElement searchInput = driver.findElement(By.name("keyword"));

        // Gõ từ khóa VÀ nhấn phím ENTER ngay lập tức
        searchInput.sendKeys("Huy Tran");
        searchInput.sendKeys(Keys.ENTER); // Lệnh giả lập phím Enter

        // Kiểm tra xem URL có chứa từ khóa không (chứng tỏ đã search)
        wait.until(ExpectedConditions.urlContains("keyword="));
        assertTrue(driver.getCurrentUrl().contains("keyword="));
    }

    // --- BƯỚC 3: TEST CHỨC NĂNG THÊM NHÂN VIÊN ---
    @Test
    @Order(3)
    public void testCreateNewStaff() {
        driver.get("http://localhost:8080/admin/staff-manage/create");

        // Bắt các ô nhập liệu
        driver.findElement(By.name("fullName")).sendKeys("Nhân Viên Auto Test");
        driver.findElement(By.name("email")).sendKeys("autotest@gmail.com");
        driver.findElement(By.name("username")).sendKeys("hihihaha");
        driver.findElement(By.name("phone")).sendKeys("0999888777");

        Select roleSelect = new Select(driver.findElement(By.name("roleId")));
        roleSelect.selectByIndex(2);

        // TÌM VÀ CLICK NÚT LƯU
        WebElement submitBtn = driver.findElement(By.xpath("//button[@type='submit']"));
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("arguments[0].click();", submitBtn);

        // Chờ kết quả
        wait.until(ExpectedConditions.urlContains("/admin/staff-manage"));
        assertTrue(driver.getCurrentUrl().endsWith("/admin/staff-manage"));
    }

    // --- BƯỚC 4: TEST MODAL XÓA & BÀN GIAO CÔNG VIỆC ---
    @Test
    @Order(4)
    public void testDeleteAndTransferStaff() {
        driver.get("http://localhost:8080/admin/staff-manage");

        // 1. Tìm cái nút Xóa (hình thùng rác) của nhân viên đầu tiên trong bảng và click
        WebElement firstDeleteBtn = wait.until(ExpectedConditions.elementToBeClickable(By.className("delete-staff-btn")));
        firstDeleteBtn.click();

        // 2. Chờ cho cái Modal (Form màu vàng/xám) hiện lên
        WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("transferDeleteModal")));
        assertTrue(modal.isDisplayed(), "Lỗi: Modal Xóa không hiện lên!");

        // 3. Giả sử form vàng hiện lên, ta chọn người nhận bàn giao
        try {
            Select transferSelect = new Select(driver.findElement(By.name("transferStaffId")));
            transferSelect.selectByIndex(1); // Chọn người nhận việc
        } catch (Exception e) {
            System.out.println("Nhân viên này không có việc pending, form xám hiện lên (Bỏ qua bước chọn người).");
        }

        // 4. Bấm nút Xác nhận Xóa
        WebElement submitDeleteBtn = driver.findElement(By.id("btnSubmitDelete")); // Đảm bảo nút trong HTML có id này
        submitDeleteBtn.click();

        // 5. Kiểm tra kết quả redirect
        wait.until(ExpectedConditions.urlContains("/admin/staff-manage"));
        assertTrue(driver.getCurrentUrl().contains("/admin/staff-manage"));
    }

    // --- BƯỚC 5: DỌN DẸP TRÌNH DUYỆT ---
    @AfterEach
    public void tearDown() throws InterruptedException {
        Thread.sleep(2000); // Ngừng 2s để bạn nhìn thấy thao tác cuối
        if (driver != null) {
            driver.quit();
        }
    }
}
