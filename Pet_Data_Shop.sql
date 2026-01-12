/* =========================================================
   PETSHOP DATABASE - VERSION: TÁCH CUSTOMER / STAFF
   - Customer: tự đăng ký (dbo.Customers) -> KHÔNG role
   - Staff: chỉ admin cấp (dbo.Staff) -> CÓ role (dbo.Roles)
   ========================================================= */

USE master;
GO
IF DB_ID('petshop') IS NOT NULL
BEGIN
    ALTER DATABASE petshop SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE petshop;
END
GO
CREATE DATABASE petshop;
GO
USE petshop;
GO

/* =========================================================
   1) ROLES (CHỈ DÙNG CHO STAFF)
   ========================================================= */
CREATE TABLE dbo.Roles (
    RoleID INT IDENTITY(1,1) PRIMARY KEY,
    RoleName VARCHAR(30) NOT NULL UNIQUE -- admin, staff
);
GO

INSERT INTO dbo.Roles(RoleName) VALUES ('admin'), ('staff');
GO

/* =========================================================
   2) CUSTOMERS (TỰ ĐĂNG KÝ - KHÔNG ROLE)
   ========================================================= */
CREATE TABLE dbo.Customers (
    CustomerID INT IDENTITY(1,1) PRIMARY KEY,
    Username VARCHAR(60) NOT NULL UNIQUE,
    PasswordHash VARCHAR(255) NOT NULL,
    Email VARCHAR(120) NULL UNIQUE,
    Phone VARCHAR(20) NULL,
    FullName NVARCHAR(120) NULL,
    IsActive BIT NOT NULL DEFAULT 1,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL
);
GO

/* =========================================================
   3) STAFF (ADMIN CẤP - CÓ ROLE)
   ========================================================= */
CREATE TABLE dbo.Staff (
    StaffID INT IDENTITY(1,1) PRIMARY KEY,
    RoleID INT NOT NULL, -- admin / staff
    Username VARCHAR(60) NOT NULL UNIQUE,
    PasswordHash VARCHAR(255) NOT NULL,
    Email VARCHAR(120) NULL UNIQUE,
    Phone VARCHAR(20) NULL,
    FullName NVARCHAR(120) NULL,

    HireDate DATE NULL,
    Bio NVARCHAR(300) NULL,

    IsActive BIT NOT NULL DEFAULT 1,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,

    CONSTRAINT FK_Staff_Roles FOREIGN KEY (RoleID) REFERENCES dbo.Roles(RoleID)
);
GO

/* =========================================================
   4) PRODUCT CATEGORIES / PRODUCTS
   ========================================================= */
CREATE TABLE dbo.Categories (
    CategoryID INT IDENTITY(1,1) PRIMARY KEY,
    Name NVARCHAR(80) NOT NULL UNIQUE,
    Description NVARCHAR(500) NULL,
    IsActive BIT NOT NULL DEFAULT 1,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

CREATE TABLE dbo.Products (
    ProductID INT IDENTITY(1,1) PRIMARY KEY,
    CategoryID INT NULL,
    Name NVARCHAR(150) NOT NULL,
    SKU VARCHAR(50) NULL UNIQUE,
    Price DECIMAL(12,2) NOT NULL CHECK (Price >= 0),
    SalePrice DECIMAL(12,2) NULL CHECK (SalePrice IS NULL OR SalePrice >= 0),
    DiscountPercent DECIMAL(5,2) NOT NULL DEFAULT 0 CHECK (DiscountPercent BETWEEN 0 AND 100),
    Stock INT NOT NULL DEFAULT 0 CHECK (Stock >= 0),
    ImageUrl VARCHAR(255) NULL,
    Description NVARCHAR(MAX) NULL,
    IsActive BIT NOT NULL DEFAULT 1,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_Products_Categories FOREIGN KEY (CategoryID) REFERENCES dbo.Categories(CategoryID)
);
GO

/* =========================================================
   5) PETS (GỘP PET BÁN + PET KHÁCH SỞ HỮU)
   - OwnerCustomerID NULL => đang bán (IsAvailable=1)
   - OwnerCustomerID NOT NULL => đã có chủ (IsAvailable=0)
   ========================================================= */
CREATE TABLE dbo.Pets (
    PetID INT IDENTITY(1,1) PRIMARY KEY,
    Name NVARCHAR(120) NOT NULL,
    PetType VARCHAR(20) NOT NULL CHECK (PetType IN ('dog','cat','other')),
    Breed NVARCHAR(80) NULL,
    Gender VARCHAR(10) NULL CHECK (Gender IN ('Male','Female')),
    AgeMonths INT NULL CHECK (AgeMonths >= 0),
    WeightKg DECIMAL(5,2) NULL CHECK (WeightKg >= 0),
    Color NVARCHAR(40) NULL,
    Note NVARCHAR(500) NULL,
    ImageUrl VARCHAR(255) NULL,
    Description NVARCHAR(MAX) NULL,

    Price DECIMAL(12,2) NULL CHECK (Price IS NULL OR Price >= 0),
    SalePrice DECIMAL(12,2) NULL CHECK (SalePrice IS NULL OR SalePrice >= 0),
    DiscountPercent DECIMAL(5,2) NULL DEFAULT 0 CHECK (DiscountPercent IS NULL OR DiscountPercent BETWEEN 0 AND 100),

    IsAvailable BIT NOT NULL DEFAULT 1,
    OwnerCustomerID INT NULL,
    PurchasedAt DATETIME2 NULL,

    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,

    CONSTRAINT FK_Pets_OwnerCustomer FOREIGN KEY (OwnerCustomerID) REFERENCES dbo.Customers(CustomerID),

    CONSTRAINT CK_Pets_Owner_Available CHECK (
        (OwnerCustomerID IS NULL AND IsAvailable = 1) OR
        (OwnerCustomerID IS NOT NULL AND IsAvailable = 0)
    ),

    CONSTRAINT CK_Pets_Price_WhenSale CHECK (
        (OwnerCustomerID IS NULL AND Price IS NOT NULL) OR
        (OwnerCustomerID IS NOT NULL)
    )
);
GO

/* =========================================================
   6) SERVICES / APPOINTMENTS / ASSIGNMENT / SCHEDULE
   ========================================================= */
CREATE TABLE dbo.Services (
    ServiceID INT IDENTITY(1,1) PRIMARY KEY,
    ServiceType VARCHAR(20) NOT NULL CHECK (ServiceType IN ('vaccination','boarding','hygiene','health_check')),
    Name NVARCHAR(120) NOT NULL,
    Description NVARCHAR(500) NULL,
    BasePrice DECIMAL(12,2) NOT NULL CHECK (BasePrice >= 0),
    DurationMinutes INT NOT NULL DEFAULT 30 CHECK (DurationMinutes > 0),
    IsActive BIT NOT NULL DEFAULT 1,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

CREATE TABLE dbo.Appointments (
    AppointmentID INT IDENTITY(1,1) PRIMARY KEY,
    AppointmentCode VARCHAR(30) NOT NULL UNIQUE,
    CustomerID INT NOT NULL,
    PetID INT NOT NULL,
    AppointmentDate DATETIME2 NOT NULL,
    Note NVARCHAR(255) NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (Status IN ('pending','confirmed','in_progress','done','canceled','no_show')),
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,

    CONSTRAINT FK_App_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID),
    CONSTRAINT FK_App_Pet FOREIGN KEY (PetID) REFERENCES dbo.Pets(PetID)
);
GO

CREATE TABLE dbo.AppointmentServices (
    AppointmentServiceID INT IDENTITY(1,1) PRIMARY KEY,
    AppointmentID INT NOT NULL,
    ServiceID INT NOT NULL,
    Price DECIMAL(12,2) NOT NULL CHECK (Price >= 0),
    Quantity INT NOT NULL DEFAULT 1 CHECK (Quantity > 0),
    LineTotal AS (Price * Quantity) PERSISTED,

    CONSTRAINT FK_AppSvc_App FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID),
    CONSTRAINT FK_AppSvc_Service FOREIGN KEY (ServiceID) REFERENCES dbo.Services(ServiceID),
    CONSTRAINT UQ_AppSvc UNIQUE (AppointmentID, ServiceID)
);
GO

CREATE TABLE dbo.StaffSchedules (
    ScheduleID INT IDENTITY(1,1) PRIMARY KEY,
    StaffID INT NOT NULL,
    WorkDate DATE NOT NULL,
    StartTime TIME(0) NOT NULL,
    EndTime TIME(0) NOT NULL,
    Note NVARCHAR(255) NULL,

    CONSTRAINT FK_Schedules_Staff FOREIGN KEY (StaffID) REFERENCES dbo.Staff(StaffID),
    CONSTRAINT CK_Schedule_Time CHECK (EndTime > StartTime),
    CONSTRAINT UQ_Staff_Schedule UNIQUE (StaffID, WorkDate, StartTime, EndTime)
);
GO

CREATE TABLE dbo.AppointmentAssignments (
    AppointmentID INT NOT NULL,
    StaffID INT NOT NULL,
    AssignedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    PRIMARY KEY (AppointmentID, StaffID),

    CONSTRAINT FK_Assign_App FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID),
    CONSTRAINT FK_Assign_Staff FOREIGN KEY (StaffID) REFERENCES dbo.Staff(StaffID)
);
GO

/* =========================================================
   7) VACCINATION & HEALTH RECORDS
   ========================================================= */
CREATE TABLE dbo.PetVaccinations (
    VaccinationID INT IDENTITY(1,1) PRIMARY KEY,
    PetID INT NOT NULL,
    VaccineName NVARCHAR(100) NOT NULL,
    AdministeredDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    NextDueDate DATETIME2 NULL,

    AppointmentID INT NULL,
    PerformedByStaffID INT NULL,

    Note NVARCHAR(500) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_PetVacc_Pet FOREIGN KEY (PetID) REFERENCES dbo.Pets(PetID),
    CONSTRAINT FK_PetVacc_App FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID) ON DELETE SET NULL,
    CONSTRAINT FK_PetVacc_Staff FOREIGN KEY (PerformedByStaffID) REFERENCES dbo.Staff(StaffID)
);
GO

CREATE TABLE dbo.PetHealthRecords (
    HealthRecordID INT IDENTITY(1,1) PRIMARY KEY,
    PetID INT NOT NULL,
    CheckDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    WeightKg DECIMAL(5,2) NULL,
    Temperature DECIMAL(4,2) NULL,
    ConditionBefore NVARCHAR(500) NULL,
    ConditionAfter NVARCHAR(500) NULL,
    Findings NVARCHAR(MAX) NULL,
    Recommendations NVARCHAR(MAX) NULL,

    AppointmentID INT NULL,
    PerformedByStaffID INT NULL,

    Note NVARCHAR(500) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_Health_Pet FOREIGN KEY (PetID) REFERENCES dbo.Pets(PetID),
    CONSTRAINT FK_Health_App FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID) ON DELETE SET NULL,
    CONSTRAINT FK_Health_Staff FOREIGN KEY (PerformedByStaffID) REFERENCES dbo.Staff(StaffID)
);
GO

/* =========================================================
   8) CART / CART ITEMS (CHỈ CUSTOMER)
   ========================================================= */
CREATE TABLE dbo.Carts (
    CartID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID INT NOT NULL UNIQUE,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_Carts_Customers FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID)
);
GO

CREATE TABLE dbo.CartItems (
    CartItemID INT IDENTITY(1,1) PRIMARY KEY,
    CartID INT NOT NULL,
    ProductID INT NULL,
    PetID INT NULL,
    Quantity INT NOT NULL DEFAULT 1 CHECK (Quantity > 0),
    AddedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_CartItems_Carts FOREIGN KEY (CartID) REFERENCES dbo.Carts(CartID),
    CONSTRAINT FK_CartItems_Products FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID),
    CONSTRAINT FK_CartItems_Pets FOREIGN KEY (PetID) REFERENCES dbo.Pets(PetID),

    CONSTRAINT CK_CartItems_OnlyOneType CHECK (
        (CASE WHEN ProductID IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN PetID IS NULL THEN 0 ELSE 1 END) = 1
    )
);
GO

/* =========================================================
   9) ORDERS / ORDER ITEMS (CHỈ CUSTOMER)
   ========================================================= */
CREATE TABLE dbo.Orders (
    OrderID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID INT NOT NULL,
    OrderCode VARCHAR(30) NOT NULL UNIQUE,

    ShipName NVARCHAR(120) NULL,
    ShipPhone VARCHAR(20) NULL,
    ShipAddress NVARCHAR(255) NULL,
    Note NVARCHAR(255) NULL,

    Status VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (Status IN ('pending','paid','processing','shipped','done','canceled','refunded')),

    Subtotal DECIMAL(12,2) NOT NULL DEFAULT 0,
    DiscountTotal DECIMAL(12,2) NOT NULL DEFAULT 0,
    ShippingFee DECIMAL(12,2) NOT NULL DEFAULT 0,
    TotalAmount DECIMAL(12,2) NOT NULL DEFAULT 0,

    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,

    CONSTRAINT FK_Orders_Customers FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID)
);
GO

CREATE TABLE dbo.OrderItems (
    OrderItemID INT IDENTITY(1,1) PRIMARY KEY,
    OrderID INT NOT NULL,
    ProductID INT NULL,
    PetID INT NULL,

    ItemName NVARCHAR(150) NOT NULL,
    UnitPrice DECIMAL(12,2) NOT NULL CHECK (UnitPrice >= 0),
    Quantity INT NOT NULL DEFAULT 1 CHECK (Quantity > 0),
    LineTotal AS (UnitPrice * Quantity) PERSISTED,

    CONSTRAINT FK_OrderItems_Orders FOREIGN KEY (OrderID) REFERENCES dbo.Orders(OrderID),
    CONSTRAINT FK_OrderItems_Products FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID),
    CONSTRAINT FK_OrderItems_Pets FOREIGN KEY (PetID) REFERENCES dbo.Pets(PetID),

    CONSTRAINT CK_OrderItems_OnlyOneType CHECK (
        (CASE WHEN ProductID IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN PetID IS NULL THEN 0 ELSE 1 END) = 1
    )
);
GO

/* =========================================================
   10) PAYMENTS
   ========================================================= */
CREATE TABLE dbo.Payments (
    PaymentID INT IDENTITY(1,1) PRIMARY KEY,
    PaymentType VARCHAR(10) NOT NULL CHECK (PaymentType IN ('order','service')),
    OrderID INT NULL,
    AppointmentID INT NULL,

    Method VARCHAR(30) NOT NULL CHECK (Method IN ('cod','bank','momo','vnpay','paypal','cash','other')),
    Amount DECIMAL(12,2) NOT NULL CHECK (Amount >= 0),
    Status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (Status IN ('pending','success','failed','refunded')),
    PaidAt DATETIME2 NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_Payments_Orders FOREIGN KEY (OrderID) REFERENCES dbo.Orders(OrderID),
    CONSTRAINT FK_Payments_Appointments FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID),

    CONSTRAINT CK_Payments_OneTarget CHECK (
        (PaymentType='order' AND OrderID IS NOT NULL AND AppointmentID IS NULL) OR
        (PaymentType='service' AND AppointmentID IS NOT NULL AND OrderID IS NULL)
    )
);
GO

/* =========================================================
   11) FEEDBACKS (PRODUCT / SERVICE / GENERAL)
   - Product feedback: Customer + OrderItem
   - Service feedback: Customer + Appointment (+ Staff nếu muốn lưu)
   - General feedback: có thể là khách vãng lai (CustomerID NULL) => cần Email/Phone
   ========================================================= */
CREATE TABLE dbo.Feedbacks (
    FeedbackID INT IDENTITY(1,1) PRIMARY KEY,

    CustomerID INT NULL,  -- cho phép NULL nếu feedback general từ guest
    FeedbackType VARCHAR(10) NOT NULL CHECK (FeedbackType IN ('product','service','general')),

    OrderItemID INT NULL,      -- product feedback
    AppointmentID INT NULL,    -- service feedback
    StaffID INT NULL,          -- staff phục vụ (optional, có thể derive qua Assignment)

    Rating INT NULL CHECK (Rating BETWEEN 1 AND 5),
    Subject NVARCHAR(100) NULL,
    Comment NVARCHAR(MAX) NULL,

    Email VARCHAR(255) NULL,
    PhoneNumber VARCHAR(20) NULL,

    ImageUrls VARCHAR(MAX) NULL, -- JSON array / comma-separated
    Status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (Status IN ('pending','approved','rejected')),

    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,

    CONSTRAINT FK_Feedback_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID),
    CONSTRAINT FK_Feedback_OrderItem FOREIGN KEY (OrderItemID) REFERENCES dbo.OrderItems(OrderItemID),
    CONSTRAINT FK_Feedback_Appointment FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID),
    CONSTRAINT FK_Feedback_Staff FOREIGN KEY (StaffID) REFERENCES dbo.Staff(StaffID),

    CONSTRAINT CK_Feedback_Logic CHECK (
        -- product: bắt buộc Customer + OrderItem, không có appointment/staff
        (FeedbackType = 'product'
            AND CustomerID IS NOT NULL
            AND OrderItemID IS NOT NULL
            AND AppointmentID IS NULL
            AND StaffID IS NULL
            AND Rating IS NOT NULL
        )
        OR
        -- service: bắt buộc Customer + Appointment, staff optional
        (FeedbackType = 'service'
            AND CustomerID IS NOT NULL
            AND AppointmentID IS NOT NULL
            AND OrderItemID IS NULL
            AND Rating IS NOT NULL
        )
        OR
        -- general: không gắn order/appointment/staff, rating có thể NULL,
        -- nếu CustomerID NULL thì phải có ít nhất Email hoặc Phone
        (FeedbackType = 'general'
            AND OrderItemID IS NULL
            AND AppointmentID IS NULL
            AND StaffID IS NULL
            AND (
                CustomerID IS NOT NULL
                OR (Email IS NOT NULL OR PhoneNumber IS NOT NULL)
            )
        )
    )
);
GO

/* =========================================================
   12) INDEXES
   ========================================================= */
-- Products
CREATE INDEX IX_Products_Category ON dbo.Products(CategoryID);
CREATE INDEX IX_Products_Active ON dbo.Products(IsActive) WHERE IsActive = 1;
CREATE INDEX IX_Products_SKU ON dbo.Products(SKU);

-- Pets
CREATE INDEX IX_Pets_OwnerCustomer ON dbo.Pets(OwnerCustomerID);
CREATE INDEX IX_Pets_Available ON dbo.Pets(IsAvailable) WHERE IsAvailable = 1;
CREATE INDEX IX_Pets_Type ON dbo.Pets(PetType, IsAvailable);

-- Orders / OrderItems
CREATE INDEX IX_Orders_Customer ON dbo.Orders(CustomerID, CreatedAt DESC);
CREATE INDEX IX_Orders_Status ON dbo.Orders(Status, CreatedAt DESC);
CREATE INDEX IX_Orders_Code ON dbo.Orders(OrderCode);

CREATE INDEX IX_OrderItems_Order ON dbo.OrderItems(OrderID);
CREATE INDEX IX_OrderItems_Product ON dbo.OrderItems(ProductID);
CREATE INDEX IX_OrderItems_Pet ON dbo.OrderItems(PetID);

-- Appointments / Services
CREATE INDEX IX_Appointments_Customer ON dbo.Appointments(CustomerID, AppointmentDate DESC);
CREATE INDEX IX_Appointments_Pet ON dbo.Appointments(PetID, AppointmentDate DESC);
CREATE INDEX IX_Appointments_StatusDate ON dbo.Appointments(Status, AppointmentDate);
CREATE INDEX IX_Appointments_Code ON dbo.Appointments(AppointmentCode);

CREATE INDEX IX_AppSvc_Appointment ON dbo.AppointmentServices(AppointmentID);
CREATE INDEX IX_AppSvc_Service ON dbo.AppointmentServices(ServiceID);

-- Staff schedules / assignments
CREATE INDEX IX_Schedule_Staff ON dbo.StaffSchedules(StaffID, WorkDate);
CREATE INDEX IX_Assign_Appointment ON dbo.AppointmentAssignments(AppointmentID);
CREATE INDEX IX_Assign_Staff ON dbo.AppointmentAssignments(StaffID);

-- Payments
CREATE INDEX IX_Payments_TypeStatus ON dbo.Payments(PaymentType, Status, PaidAt);
CREATE INDEX IX_Payments_Order ON dbo.Payments(OrderID);
CREATE INDEX IX_Payments_Appointment ON dbo.Payments(AppointmentID);

-- Feedbacks
CREATE INDEX IX_Feedbacks_Type ON dbo.Feedbacks(FeedbackType, Status, CreatedAt DESC);
CREATE INDEX IX_Feedbacks_Customer ON dbo.Feedbacks(CustomerID, CreatedAt DESC);
CREATE INDEX IX_Feedbacks_OrderItem ON dbo.Feedbacks(OrderItemID);
CREATE INDEX IX_Feedbacks_Appointment ON dbo.Feedbacks(AppointmentID);
CREATE INDEX IX_Feedbacks_Staff ON dbo.Feedbacks(StaffID) WHERE StaffID IS NOT NULL;
CREATE INDEX IX_Feedbacks_Status ON dbo.Feedbacks(Status, CreatedAt DESC);

-- Vaccinations / Health
CREATE INDEX IX_PetVacc_Pet ON dbo.PetVaccinations(PetID, AdministeredDate DESC);
CREATE INDEX IX_PetVacc_NextDue ON dbo.PetVaccinations(NextDueDate) WHERE NextDueDate IS NOT NULL;
CREATE INDEX IX_Health_Pet ON dbo.PetHealthRecords(PetID, CheckDate DESC);

-- Cart
CREATE INDEX IX_CartItems_Cart ON dbo.CartItems(CartID);
GO

/* =========================================================
   13) SEED DATA (ADMIN + STAFF + CUSTOMER + CATEGORY + SERVICE)
   PasswordHash mẫu: MD5('123456') = e10adc3949ba59abbe56e057f20f883e
   ========================================================= */

-- Seed Admin (Staff có role admin)
DECLARE @AdminRoleID INT = (SELECT RoleID FROM dbo.Roles WHERE RoleName='admin');
INSERT INTO dbo.Staff(RoleID, Username, PasswordHash, Email, Phone, FullName, HireDate, Bio)
VALUES (@AdminRoleID, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 'admin@petshop.com', '0901234567', N'Quản trị viên', '2024-01-01', N'Quản trị hệ thống');

-- Seed Staff
DECLARE @StaffRoleID INT = (SELECT RoleID FROM dbo.Roles WHERE RoleName='staff');
INSERT INTO dbo.Staff(RoleID, Username, PasswordHash, Email, Phone, FullName, HireDate, Bio)
VALUES
(@StaffRoleID, 'staff01', 'e10adc3949ba59abbe56e057f20f883e', 'staff01@petshop.com', '0912345678', N'Nguyễn Văn A', '2024-01-15', N'Chuyên viên chăm sóc thú cưng, 3 năm kinh nghiệm'),
(@StaffRoleID, 'staff02', 'e10adc3949ba59abbe56e057f20f883e', 'staff02@petshop.com', '0923456789', N'Trần Thị B', '2024-02-01', N'Bác sĩ thú y, chuyên khoa nội'),
(@StaffRoleID, 'staff03', 'e10adc3949ba59abbe56e057f20f883e', 'staff03@petshop.com', '0934567890', N'Lê Văn C', '2024-03-10', N'Chuyên viên spa & grooming');

-- Seed Customers (tự đăng ký)
INSERT INTO dbo.Customers(Username, PasswordHash, Email, Phone, FullName, IsActive)
VALUES
('customer01', 'e10adc3949ba59abbe56e057f20f883e', 'customer01@gmail.com', '0945678901', N'Phạm Thị D', 1),
('customer02', 'e10adc3949ba59abbe56e057f20f883e', 'customer02@gmail.com', '0956789012', N'Hoàng Văn E', 1);

-- Seed Categories
INSERT INTO dbo.Categories(Name, Description) VALUES
(N'Thức ăn cho chó', N'Thức ăn dinh dưỡng dành cho chó các loại'),
(N'Thức ăn cho mèo', N'Thức ăn dinh dưỡng dành cho mèo các loại'),
(N'Phụ kiện thú cưng', N'Đồ chơi, vòng cổ, quần áo cho thú cưng'),
(N'Vệ sinh & chăm sóc', N'Sản phẩm tắm rửa, vệ sinh cho thú cưng'),
(N'Sức khỏe', N'Vitamin, thuốc bổ, sản phẩm chăm sóc sức khỏe');

-- Seed Services
INSERT INTO dbo.Services(ServiceType, Name, Description, BasePrice, DurationMinutes) VALUES
('vaccination', N'Tiêm phòng dại', N'Tiêm phòng bệnh dại cho chó mèo', 150000, 30),
('vaccination', N'Tiêm phòng 5 bệnh', N'Tiêm phòng 5 bệnh cơ bản cho chó', 200000, 30),
('vaccination', N'Tiêm phòng 7 bệnh', N'Tiêm phòng 7 bệnh toàn diện cho chó', 250000, 30),
('hygiene', N'Tắm spa cơ bản', N'Tắm, vệ sinh, cắt móng cơ bản', 100000, 60),
('hygiene', N'Tắm spa cao cấp', N'Tắm spa + massage + làm đẹp lông', 200000, 90),
('hygiene', N'Cắt tỉa lông chuyên nghiệp', N'Cắt tỉa, tạo kiểu lông theo yêu cầu', 150000, 60),
('health_check', N'Khám sức khỏe tổng quát', N'Kiểm tra sức khỏe toàn diện', 300000, 45),
('health_check', N'Khám bệnh chuyên khoa', N'Khám và tư vấn bệnh chuyên sâu', 500000, 60),
('boarding', N'Giữ thú cưng theo ngày', N'Dịch vụ giữ thú cưng chuyên nghiệp', 80000, 1440),
('boarding', N'Giữ thú cưng theo tuần', N'Dịch vụ giữ thú cưng theo tuần', 500000, 10080);
GO
