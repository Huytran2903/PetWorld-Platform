/* =========================================================
   PETSHOP DATABASE - VERSION: FINAL FULL (1 TABLE PAYMENTS)
   - Customer: Table riêng, không Role
   - Staff: Table riêng, có Role (Admin/Staff)
   - Feedback: Có link tới Service cụ thể
   - FEATURE 10 (tối giản 2 bảng): SystemConfigs + AccessControl
   - FIX: Services.ServiceType có thêm 'health_check' (seed chạy được)
   - FIX: Orders.Status VARCHAR(20) + CHECK
   - FIX: SQL Server time columns dùng DATETIME2 (không dùng timestamp)
   - FIX: Orders - Payments quan hệ 1-1 (mỗi Order tối đa 1 Payment) bằng UNIQUE filtered index
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
    UpdatedAt DATETIME2 NULL,
	AuthProvider VARCHAR(20) DEFAULT 'LOCAL'
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
   FEATURE 10 – TỐI GIẢN 2 BẢNG (Admin)
   ========================================================= */
CREATE TABLE dbo.SystemConfigs (
    ConfigKey   VARCHAR(80)  NOT NULL PRIMARY KEY,
    ConfigValue NVARCHAR(MAX) NULL,
    DataType    VARCHAR(20)  NOT NULL DEFAULT 'string'
        CHECK (DataType IN ('string','int','decimal','bool','json')),
    UpdatedByStaffID INT NULL,
    UpdatedAt   DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_SystemConfigs_Staff FOREIGN KEY (UpdatedByStaffID) REFERENCES dbo.Staff(StaffID)
);
GO

CREATE TABLE dbo.AccessControl (
    RoleID INT NOT NULL,
    PermissionCode VARCHAR(120) NOT NULL,  -- vd: SYSTEM.CONFIG, SYSTEM.ACL
    IsAllowed BIT NOT NULL DEFAULT 1,
    GrantedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    PRIMARY KEY (RoleID, PermissionCode),
    CONSTRAINT FK_AccessControl_Role FOREIGN KEY (RoleID) REFERENCES dbo.Roles(RoleID)
);
GO

/* =========================================================
   4) CATEGORIES / PRODUCTS
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
/* Service Types: categorize services (Spa, Vaccination, Boarding, etc.) - Admin CRUD, soft delete */
CREATE TABLE dbo.ServiceTypes (
    ServiceTypeID INT IDENTITY(1,1) PRIMARY KEY,
    Name NVARCHAR(80) NOT NULL UNIQUE,
    Description NVARCHAR(500) NULL,
    IsActive BIT NOT NULL DEFAULT 1
);
GO

CREATE TABLE dbo.Services (
    ServiceID INT IDENTITY(1,1) PRIMARY KEY,
    ServiceType VARCHAR(20) NOT NULL,
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
    AppointmentCode VARCHAR(100) NOT NULL UNIQUE,
    CustomerID INT NOT NULL,
    PetID INT NOT NULL,
    StaffID INT NULL,
    AppointmentDate DATETIME2 NOT NULL,
    EndTime DATETIME2 NULL,
    Note NVARCHAR(255) NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (Status IN ('pending','confirmed','in_progress','done','canceled','no_show', 'check_in')),
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,
    RescheduledAt DATETIME2 NULL,
    PreviousAppointmentDate DATETIME2 NULL,
    CanceledAt DATETIME2 NULL,
    CancellationReason NVARCHAR(255) NULL,
    CONSTRAINT FK_App_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID),
    CONSTRAINT FK_App_Pet FOREIGN KEY (PetID) REFERENCES dbo.Pets(PetID),
    CONSTRAINT FK_App_Staff FOREIGN KEY (StaffID) REFERENCES dbo.Staff(StaffID)
);
GO

CREATE TABLE dbo.AppointmentServices (
    AppointmentServiceID INT IDENTITY(1,1) PRIMARY KEY,
    AppointmentID INT NOT NULL,
    ServiceID INT NOT NULL,
    AssignedStaffID INT NULL,
    Price DECIMAL(12,2) NOT NULL CHECK (Price >= 0),
    Quantity INT NOT NULL DEFAULT 1 CHECK (Quantity > 0),
    ServiceStatus VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (ServiceStatus IN ('pending','assigned','in_progress','done','canceled','no_show')),
    LineTotal AS (Price * Quantity) PERSISTED,
    CONSTRAINT FK_AppSvc_App FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID),
    CONSTRAINT FK_AppSvc_Service FOREIGN KEY (ServiceID) REFERENCES dbo.Services(ServiceID),
    CONSTRAINT FK_AppSvc_AssignedStaff FOREIGN KEY (AssignedStaffID) REFERENCES dbo.Staff(StaffID),
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
    AppointmentServiceID INT NULL,
    PerformedByStaffID INT NULL,
    Note NVARCHAR(500) NULL,
    WarningFlag BIT NOT NULL DEFAULT 0,
    IsDraft BIT NOT NULL DEFAULT 0,
    IsDeleted BIT NOT NULL DEFAULT 0,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_Health_Pet FOREIGN KEY (PetID) REFERENCES dbo.Pets(PetID),
    CONSTRAINT FK_Health_App FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID) ON DELETE SET NULL,
    CONSTRAINT FK_Health_AppSvc FOREIGN KEY (AppointmentServiceID) REFERENCES dbo.AppointmentServices(AppointmentServiceID) ON DELETE SET NULL,
    CONSTRAINT FK_Health_Staff FOREIGN KEY (PerformedByStaffID) REFERENCES dbo.Staff(StaffID)
);
GO

CREATE TABLE dbo.PetHealthPhotos (
    HealthPhotoID INT IDENTITY(1,1) PRIMARY KEY,
    HealthRecordID INT NOT NULL,
    ImageUrl VARCHAR(500) NOT NULL,
    CapturedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_HealthPhoto_Record FOREIGN KEY (HealthRecordID) REFERENCES dbo.PetHealthRecords(HealthRecordID) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.ServiceNotes (
    ServiceNoteID INT IDENTITY(1,1) PRIMARY KEY,
    AppointmentID INT NOT NULL,
    AppointmentServiceID INT NOT NULL,
    StaffID INT NOT NULL,
    Note NVARCHAR(MAX) NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'done'
        CHECK (Status IN ('draft','done')),
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_ServiceNote_Appointment FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID) ON DELETE CASCADE,
    CONSTRAINT FK_ServiceNote_ServiceLine FOREIGN KEY (AppointmentServiceID) REFERENCES dbo.AppointmentServices(AppointmentServiceID) ON DELETE CASCADE,
    CONSTRAINT FK_ServiceNote_Staff FOREIGN KEY (StaffID) REFERENCES dbo.Staff(StaffID)
);
GO

CREATE TABLE dbo.ServiceNotePhotos (
    ServiceNotePhotoID INT IDENTITY(1,1) PRIMARY KEY,
    ServiceNoteID INT NOT NULL,
    ImageUrl VARCHAR(500) NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_ServiceNotePhoto_Note FOREIGN KEY (ServiceNoteID) REFERENCES dbo.ServiceNotes(ServiceNoteID) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.AppointmentSummaries (
    SummaryID INT IDENTITY(1,1) PRIMARY KEY,
    AppointmentID INT NOT NULL UNIQUE,
    WeightKg DECIMAL(5,2) NULL,
    Temperature DECIMAL(4,2) NULL,
    ConditionBefore NVARCHAR(500) NULL,
    ConditionAfter NVARCHAR(500) NULL,
    Findings NVARCHAR(MAX) NULL,
    Recommendations NVARCHAR(MAX) NULL,
    Note NVARCHAR(500) NULL,
    WarningFlag BIT NOT NULL DEFAULT 0,
    SummaryByStaffID INT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_Summary_Appointment FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID) ON DELETE CASCADE,
    CONSTRAINT FK_Summary_Staff FOREIGN KEY (SummaryByStaffID) REFERENCES dbo.Staff(StaffID)
);
GO

/* =========================================================
   8) CART / CART ITEMS
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
   9) ORDERS / ORDER ITEMS
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
   10) PAYMENTS (CHỈ 1 BẢNG)
   - Quan hệ 1-1 với Orders bằng UNIQUE filtered index (OrderID không lặp)
   - Tương tự cho Appointments (nếu bạn muốn 1-1)
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

-- 1-1: mỗi Order tối đa 1 Payment
CREATE UNIQUE INDEX UX_Payments_Order_1_1
ON dbo.Payments(OrderID)
WHERE PaymentType='order' AND OrderID IS NOT NULL;
GO

-- (tuỳ chọn nhưng nên có) 1-1: mỗi Appointment tối đa 1 Payment
CREATE UNIQUE INDEX UX_Payments_Appointment_1_1
ON dbo.Payments(AppointmentID)
WHERE PaymentType='service' AND AppointmentID IS NOT NULL;
GO

/* =========================================================
   11) FEEDBACKS (PRODUCT / SERVICE / GENERAL)
   ========================================================= */
CREATE TABLE dbo.Feedbacks (
    FeedbackID INT IDENTITY(1,1) PRIMARY KEY,

    CustomerID INT NULL,
    FeedbackType VARCHAR(10) NOT NULL CHECK (FeedbackType IN ('product','service','general')),

    OrderItemID INT NULL,
    AppointmentID INT NULL,
    ServiceID INT NULL,
    StaffID INT NULL,

    Rating INT NULL CHECK (Rating BETWEEN 1 AND 5),
    Subject NVARCHAR(100) NULL,
    Comment NVARCHAR(MAX) NULL,

    Email VARCHAR(255) NULL,
    PhoneNumber VARCHAR(20) NULL,

    ImageUrls VARCHAR(MAX) NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (Status IN ('pending','approved','rejected')),

    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,

    CONSTRAINT FK_Feedback_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID),
    CONSTRAINT FK_Feedback_OrderItem FOREIGN KEY (OrderItemID) REFERENCES dbo.OrderItems(OrderItemID),
    CONSTRAINT FK_Feedback_Appointment FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID),
    CONSTRAINT FK_Feedback_Service FOREIGN KEY (ServiceID) REFERENCES dbo.Services(ServiceID),
    CONSTRAINT FK_Feedback_Staff FOREIGN KEY (StaffID) REFERENCES dbo.Staff(StaffID),

    CONSTRAINT CK_Feedback_Logic CHECK (
        (FeedbackType = 'product'
            AND CustomerID IS NOT NULL
            AND OrderItemID IS NOT NULL
            AND AppointmentID IS NULL
            AND ServiceID IS NULL
            AND StaffID IS NULL
            AND Rating IS NOT NULL
        )
        OR
        (FeedbackType = 'service'
            AND CustomerID IS NOT NULL
            AND AppointmentID IS NOT NULL
            AND OrderItemID IS NULL
            AND Rating IS NOT NULL
        )
        OR
        (FeedbackType = 'general'
            AND OrderItemID IS NULL
            AND AppointmentID IS NULL
            AND ServiceID IS NULL
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
   12) Verification-Tokens(for email)
   ========================================================= */
CREATE TABLE dbo.verification_tokens (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    token VARCHAR(255) NOT NULL,
    expiry_date DATETIME2 NOT NULL,
    customer_id INT NOT NULL UNIQUE, 
    
   
    CONSTRAINT FK_VerificationToken_Customer 
    FOREIGN KEY (customer_id) REFERENCES dbo.Customers(CustomerID) 
    ON DELETE CASCADE 
);
GO

/* =========================================================
   13) INDEXES
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
CREATE INDEX IX_AppSvc_AssignedStaff ON dbo.AppointmentServices(AssignedStaffID) WHERE AssignedStaffID IS NOT NULL;
CREATE INDEX IX_AppSvc_Status ON dbo.AppointmentServices(ServiceStatus);

-- Staff schedules / assignments
CREATE INDEX IX_Schedule_Staff ON dbo.StaffSchedules(StaffID, WorkDate);
CREATE INDEX IX_Assign_Appointment ON dbo.AppointmentAssignments(AppointmentID);
CREATE INDEX IX_Assign_Staff ON dbo.AppointmentAssignments(StaffID);

-- Payments
CREATE INDEX IX_Payments_TypeStatusPaidAt ON dbo.Payments(PaymentType, Status, PaidAt);
CREATE INDEX IX_Payments_Order ON dbo.Payments(OrderID) WHERE OrderID IS NOT NULL;
CREATE INDEX IX_Payments_Appointment ON dbo.Payments(AppointmentID) WHERE AppointmentID IS NOT NULL;

-- Feedbacks
CREATE INDEX IX_Feedbacks_Type ON dbo.Feedbacks(FeedbackType, Status, CreatedAt DESC);
CREATE INDEX IX_Feedbacks_Customer ON dbo.Feedbacks(CustomerID, CreatedAt DESC);
CREATE INDEX IX_Feedbacks_OrderItem ON dbo.Feedbacks(OrderItemID);
CREATE INDEX IX_Feedbacks_Appointment ON dbo.Feedbacks(AppointmentID);
CREATE INDEX IX_Feedbacks_Service ON dbo.Feedbacks(ServiceID);
CREATE INDEX IX_Feedbacks_Staff ON dbo.Feedbacks(StaffID) WHERE StaffID IS NOT NULL;
CREATE INDEX IX_Feedbacks_Status ON dbo.Feedbacks(Status, CreatedAt DESC);

-- Vaccinations / Health
CREATE INDEX IX_PetVacc_Pet ON dbo.PetVaccinations(PetID, AdministeredDate DESC);
CREATE INDEX IX_PetVacc_NextDue ON dbo.PetVaccinations(NextDueDate) WHERE NextDueDate IS NOT NULL;
CREATE INDEX IX_Health_Pet ON dbo.PetHealthRecords(PetID, CheckDate DESC);
CREATE INDEX IX_Health_Appointment ON dbo.PetHealthRecords(AppointmentID) WHERE AppointmentID IS NOT NULL;
CREATE INDEX IX_Health_ServiceLine ON dbo.PetHealthRecords(AppointmentServiceID) WHERE AppointmentServiceID IS NOT NULL;
CREATE INDEX IX_ServiceNotes_Appointment ON dbo.ServiceNotes(AppointmentID);
CREATE INDEX IX_ServiceNotes_ServiceLine ON dbo.ServiceNotes(AppointmentServiceID);
CREATE INDEX IX_ServiceNotes_Staff ON dbo.ServiceNotes(StaffID);
CREATE INDEX IX_ServiceNotePhotos_Note ON dbo.ServiceNotePhotos(ServiceNoteID);
CREATE INDEX IX_Summaries_Appointment ON dbo.AppointmentSummaries(AppointmentID);

-- Cart
CREATE INDEX IX_CartItems_Cart ON dbo.CartItems(CartID);

-- Feature 10 minimal
CREATE INDEX IX_SystemConfigs_UpdatedAt ON dbo.SystemConfigs(UpdatedAt DESC);
GO

/* =========================================================
   13) SEED DATA
   PasswordHash mẫu: MD5('12345r6') = e10adc3949ba59abbe56e057f20f883e
   ========================================================= */

-- Seed Admin (Staff role admin)
DECLARE @AdminRoleID INT = (SELECT RoleID FROM dbo.Roles WHERE RoleName='admin');
INSERT INTO dbo.Staff(RoleID, Username, PasswordHash, Email, Phone, FullName, HireDate, Bio)
VALUES (@AdminRoleID, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 'admin@petshop.com', '0901234567', N'System Administrator', '2024-01-01', N'System administration');

-- Seed Staff
DECLARE @StaffRoleID INT = (SELECT RoleID FROM dbo.Roles WHERE RoleName='staff');
INSERT INTO dbo.Staff(RoleID, Username, PasswordHash, Email, Phone, FullName, HireDate, Bio)
VALUES
(@StaffRoleID, 'staff01', 'e10adc3949ba59abbe56e057f20f883e', 'staff01@petshop.com', '0912345678', N'Nguyen Van A', '2024-01-15', N'Pet care specialist, three years of experience'),
(@StaffRoleID, 'staff02', 'e10adc3949ba59abbe56e057f20f883e', 'staff02@petshop.com', '0923456789', N'Tran Thi B', '2024-02-01', N'Veterinarian, internal medicine specialist'),
(@StaffRoleID, 'staff03', 'e10adc3949ba59abbe56e057f20f883e', 'staff03@petshop.com', '0934567890', N'Le Van C', '2024-03-10', N'Spa and grooming specialist');

-- Seed Customers

-- Seed Categoriesr
INSERT INTO dbo.Categories(Name, Description) VALUES
(N'Dog Food', N'Nutritious food for dogs of all kinds'),
(N'Cat Food', N'Nutritious food for cats of all kinds'),
(N'Pet Accessories', N'Toys, collars, and clothes for pets'),
(N'Hygiene & Care', N'Bathing and hygiene products for pets'),
(N'Health', N'Vitamins, supplements, and health care products');

-- Seed Service Types (Name matches Services.ServiceType for BR-21 dependency check; used as filters in booking)
INSERT INTO dbo.ServiceTypes(Name, Description, IsActive) VALUES
('vaccination', N'Vaccination and immunization services', 1),
('boarding', N'Pet boarding and daycare', 1),
('hygiene', N'Bathing, grooming, and hygiene care', 1),
('health_check', N'General health check-up and consultation', 1);

-- Seed Services
INSERT INTO dbo.Services(ServiceType, Name, Description, BasePrice, DurationMinutes) VALUES
('vaccination', N'Rabies Vaccination', N'Rabies vaccination for dogs and cats', 150000, 30),
('vaccination', N'Five-in-One Vaccination', N'Basic five-disease vaccination for dogs', 200000, 30),
('vaccination', N'Seven-in-One Vaccination', N'Comprehensive seven-disease vaccination for dogs', 250000, 30),
('hygiene', N'Basic Spa Bath', N'Bathing, hygiene, and basic nail trimming', 100000, 60),
('hygiene', N'Premium Spa Bath', N'Spa bath + massage + coat grooming', 200000, 90),
('hygiene', N'Professional Grooming', N'Grooming and styling on request', 150000, 60),
('health_check', N'General Health Check-up', N'Comprehensive health examination', 300000, 45),
('health_check', N'Specialist Consultation', N'In-depth examination and specialized consultation', 500000, 60),
('boarding', N'Daily Pet Boarding', N'Professional pet boarding service', 80000, 1440),
('boarding', N'Weekly Pet Boarding', N'Weekly pet boarding service', 500000, 10080);

-- Seed FEATURE 10 (tối giản)
INSERT INTO dbo.SystemConfigs(ConfigKey, ConfigValue, DataType) VALUES
('SITE_NAME', N'PetShop', 'string'),
('MAINTENANCE_MODE', N'false', 'bool');

INSERT INTO dbo.AccessControl(RoleID, PermissionCode, IsAllowed) VALUES
(@AdminRoleID, 'SYSTEM.CONFIG', 1),
(@AdminRoleID, 'SYSTEM.ACL', 1),
(@StaffRoleID, 'SYSTEM.CONFIG', 0),
(@StaffRoleID, 'SYSTEM.ACL', 0);
GO

-- Drop old constraint
ALTER TABLE dbo.Appointments
DROP CONSTRAINT CK__Appointme__Statu__7D439ABD;

-- Recreate with checked_in added
ALTER TABLE dbo.Appointments
ADD CONSTRAINT CK_Appointments_Status
CHECK (Status IN ('pending','confirmed','checked_in','in_progress','done','canceled','no_show'));
