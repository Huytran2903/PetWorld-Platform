/* =========================================================
   DATABASE: PetShopDB_Final
   Created: 2026-01-08
   Scope: Full Flow E-commerce + Booking Services
   Tables: 18 (under 20)
========================================================= */

USE master;
GO
IF DB_ID('PetShopDB') IS NOT NULL
BEGIN
    ALTER DATABASE PetShopDB SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE PetShopDB;
END
GO
CREATE DATABASE PetShopDB;
GO
USE PetShopDB;
GO

/* =========================================================
   1) ROLES / PERMISSIONS / USERS  (Feature 1, 10)
========================================================= */

CREATE TABLE dbo.Roles (
    RoleID      INT IDENTITY(1,1) PRIMARY KEY,
    RoleName    VARCHAR(30) NOT NULL UNIQUE,   -- 'Admin','Staff','Customer'
    IsActive    BIT NOT NULL DEFAULT 1
);
GO

-- Tối giản: không cần bảng Permissions riêng
-- PermissionCode là chuỗi: "MANAGE_PRODUCTS", "ASSIGN_STAFF", ...
CREATE TABLE dbo.RolePermissions (
    RoleID          INT NOT NULL,
    PermissionCode  VARCHAR(80) NOT NULL,
    CONSTRAINT PK_RolePermissions PRIMARY KEY (RoleID, PermissionCode),
    CONSTRAINT FK_RolePermissions_Roles FOREIGN KEY (RoleID) REFERENCES dbo.Roles(RoleID)
);
GO

CREATE TABLE dbo.Users (
    UserID          INT IDENTITY(1,1) PRIMARY KEY,
    RoleID          INT NOT NULL,
    Username        VARCHAR(60) NOT NULL UNIQUE,
    PasswordHash    VARCHAR(255) NOT NULL,   -- bcrypt/argon2/sha256...
    FullName        NVARCHAR(120) NOT NULL,
    Email           VARCHAR(120) NOT NULL UNIQUE,
    Phone           VARCHAR(20) NULL,
    Address         NVARCHAR(255) NULL,
    AvatarUrl       NVARCHAR(255) NULL,

    IsActive        BIT NOT NULL DEFAULT 1,
    CreatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt       DATETIME2 NULL,

    CONSTRAINT FK_Users_Roles FOREIGN KEY (RoleID) REFERENCES dbo.Roles(RoleID)
);
GO

CREATE INDEX IX_Users_RoleID ON dbo.Users(RoleID);
GO


/* =========================================================
   2) PET PROFILES (Feature 3)
========================================================= */

CREATE TABLE dbo.Pets (
    PetID       INT IDENTITY(1,1) PRIMARY KEY,
    OwnerUserID INT NOT NULL, -- Customer
    PetName     NVARCHAR(80) NOT NULL,
    Species     VARCHAR(20) NOT NULL CHECK (Species IN ('DOG','CAT','OTHER')),
    Breed       NVARCHAR(80) NULL,
    Gender      VARCHAR(10) NULL CHECK (Gender IN ('MALE','FEMALE','UNKNOWN')),
    BirthDate   DATE NULL,
    WeightKg    DECIMAL(6,2) NULL CHECK (WeightKg IS NULL OR WeightKg >= 0),
    MedicalNote NVARCHAR(500) NULL,

    IsActive    BIT NOT NULL DEFAULT 1,
    CreatedAt   DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_Pets_Users FOREIGN KEY (OwnerUserID) REFERENCES dbo.Users(UserID)
);
GO

CREATE INDEX IX_Pets_Owner ON dbo.Pets(OwnerUserID);
GO


/* =========================================================
   3) PRODUCTS / CART / ORDERS (Feature 2)
========================================================= */

CREATE TABLE dbo.Categories (
    CategoryID      INT IDENTITY(1,1) PRIMARY KEY,
    CategoryName    NVARCHAR(80) NOT NULL UNIQUE,
    Description     NVARCHAR(255) NULL,
    IsActive        BIT NOT NULL DEFAULT 1
);
GO

CREATE TABLE dbo.Products (
    ProductID       INT IDENTITY(1,1) PRIMARY KEY,
    CategoryID      INT NOT NULL,
    ProductName     NVARCHAR(160) NOT NULL,
    Description     NVARCHAR(MAX) NULL,

    Price           DECIMAL(12,2) NOT NULL CHECK (Price >= 0),
    StockQty        INT NOT NULL DEFAULT 0 CHECK (StockQty >= 0),
    ImageUrl        NVARCHAR(255) NULL,

    -- Pricing & Promotions (tối giản, không tạo bảng Promotion)
    DiscountPercent DECIMAL(5,2) NULL
        CHECK (DiscountPercent IS NULL OR (DiscountPercent >= 0 AND DiscountPercent <= 100)),
    DiscountStartAt DATETIME2 NULL,
    DiscountEndAt   DATETIME2 NULL,

    IsActive        BIT NOT NULL DEFAULT 1,
    CreatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt       DATETIME2 NULL,

    CONSTRAINT FK_Products_Categories FOREIGN KEY (CategoryID) REFERENCES dbo.Categories(CategoryID)
);
GO

CREATE INDEX IX_Products_CategoryID ON dbo.Products(CategoryID);
CREATE INDEX IX_Products_ProductName ON dbo.Products(ProductName);
GO

-- 1 user có 1 cart ACTIVE (dễ demo addToCart/updateQty)
CREATE TABLE dbo.Carts (
    CartID      INT IDENTITY(1,1) PRIMARY KEY,
    UserID      INT NOT NULL,
    Status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (Status IN ('ACTIVE','ORDERED','ABANDONED')),
    CreatedAt   DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt   DATETIME2 NULL,

    CONSTRAINT FK_Carts_Users FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID)
);
GO

CREATE UNIQUE INDEX UX_Carts_ActivePerUser
ON dbo.Carts(UserID)
WHERE Status = 'ACTIVE';
GO

CREATE TABLE dbo.CartItems (
    CartID      INT NOT NULL,
    ProductID   INT NOT NULL,
    Quantity    INT NOT NULL CHECK (Quantity > 0),
    AddedAt     DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT PK_CartItems PRIMARY KEY (CartID, ProductID),
    CONSTRAINT FK_CartItems_Carts FOREIGN KEY (CartID) REFERENCES dbo.Carts(CartID),
    CONSTRAINT FK_CartItems_Products FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID)
);
GO

CREATE TABLE dbo.Orders (
    OrderID         INT IDENTITY(1,1) PRIMARY KEY,
    UserID          INT NOT NULL,  -- Customer

    ShipName        NVARCHAR(120) NOT NULL,
    ShipPhone       VARCHAR(20) NOT NULL,
    ShipAddress     NVARCHAR(255) NOT NULL,
    Note            NVARCHAR(255) NULL,

    PaymentMethod   VARCHAR(20) NOT NULL DEFAULT 'COD'
        CHECK (PaymentMethod IN ('COD','BANKING','WALLET')),
    PaymentStatus   VARCHAR(20) NOT NULL DEFAULT 'UNPAID'
        CHECK (PaymentStatus IN ('UNPAID','PAID','REFUNDED')),

    OrderStatus     VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (OrderStatus IN ('PENDING','CONFIRMED','PACKING','SHIPPING','COMPLETED','CANCELLED')),

    -- Tổng tiền nên lưu để báo cáo nhanh (app tính từ OrderItems)
    Subtotal        DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (Subtotal >= 0),
    DiscountTotal   DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (DiscountTotal >= 0),
    ShippingFee     DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (ShippingFee >= 0),
    GrandTotal      AS (Subtotal - DiscountTotal + ShippingFee) PERSISTED,

    CreatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt       DATETIME2 NULL,

    CONSTRAINT FK_Orders_Users FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID)
);
GO

CREATE INDEX IX_Orders_UserID_CreatedAt ON dbo.Orders(UserID, CreatedAt DESC);
GO

CREATE TABLE dbo.OrderItems (
    OrderItemID INT IDENTITY(1,1) PRIMARY KEY,
    OrderID     INT NOT NULL,
    ProductID   INT NOT NULL,
    Quantity    INT NOT NULL CHECK (Quantity > 0),

    -- Snapshot giá tại thời điểm mua
    UnitPrice   DECIMAL(12,2) NOT NULL CHECK (UnitPrice >= 0),
    DiscountPercentSnapshot DECIMAL(5,2) NULL
        CHECK (DiscountPercentSnapshot IS NULL OR (DiscountPercentSnapshot >= 0 AND DiscountPercentSnapshot <= 100)),

    LineTotal   AS (
        Quantity * (UnitPrice * (1 - ISNULL(DiscountPercentSnapshot, 0) / 100.0))
    ) PERSISTED,

    CONSTRAINT FK_OrderItems_Orders FOREIGN KEY (OrderID) REFERENCES dbo.Orders(OrderID),
    CONSTRAINT FK_OrderItems_Products FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID)
);
GO

CREATE INDEX IX_OrderItems_OrderID ON dbo.OrderItems(OrderID);
GO


/* =========================================================
   4) SERVICES / APPOINTMENTS (Feature 4, 5, 6)
   B1/B2/B3/B4 Flow
========================================================= */

CREATE TABLE dbo.ServiceTypes (
    ServiceTypeID    INT IDENTITY(1,1) PRIMARY KEY,
    TypeCode         VARCHAR(30) NOT NULL UNIQUE, -- 'vaccination','boarding','hygiene'
    TypeName         NVARCHAR(80) NOT NULL,       -- hiển thị UI
    Description      NVARCHAR(255) NULL,
    IsActive         BIT NOT NULL DEFAULT 1
);
GO

CREATE TABLE dbo.Services (
    ServiceID        INT IDENTITY(1,1) PRIMARY KEY,
    ServiceTypeID    INT NOT NULL,
    ServiceName      NVARCHAR(120) NOT NULL,
    Description      NVARCHAR(255) NULL,

    Price            DECIMAL(12,2) NOT NULL CHECK (Price >= 0),

    -- Quan trọng cho boarding theo ngày/đêm hoặc spa theo session
    Unit             VARCHAR(20) NOT NULL DEFAULT 'SESSION'
        CHECK (Unit IN ('SESSION','DAY','NIGHT','ITEM','HOUR')),

    DurationMins      INT NULL, -- optional cho xếp lịch vaccine/grooming
    IsActive          BIT NOT NULL DEFAULT 1,

    CreatedAt         DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt         DATETIME2 NULL,

    CONSTRAINT FK_Services_ServiceTypes FOREIGN KEY (ServiceTypeID) REFERENCES dbo.ServiceTypes(ServiceTypeID)
);
GO

CREATE INDEX IX_Services_Type ON dbo.Services(ServiceTypeID);
GO

-- Appointment: 1 lần đặt lịch
-- - Vaccine/Grooming: StartAt quan trọng, EndAt có thể NULL
-- - Boarding: StartAt=checkin, EndAt=checkout (hoặc EndAt NULL và tính theo Unit+Quantity)
CREATE TABLE dbo.Appointments (
    AppointmentID        INT IDENTITY(1,1) PRIMARY KEY,
    CustomerUserID       INT NOT NULL,
    PetID                INT NOT NULL,

    AssignedStaffID      INT NULL, -- Admin assign
    StartAt              DATETIME2 NOT NULL,
    EndAt                DATETIME2 NULL,

    Status               VARCHAR(20) NOT NULL DEFAULT 'BOOKED'
        CHECK (Status IN ('BOOKED','ASSIGNED','IN_PROGRESS','COMPLETED','CANCELLED')),

    CustomerNote         NVARCHAR(255) NULL,
    StaffNote            NVARCHAR(255) NULL,

    -- Tổng tiền booking (app tính từ AppointmentServices)
    TotalAmount          DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (TotalAmount >= 0),

    CreatedAt            DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt            DATETIME2 NULL,

    CONSTRAINT FK_Appointments_Customer FOREIGN KEY (CustomerUserID) REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_Appointments_Pets FOREIGN KEY (PetID) REFERENCES dbo.Pets(PetID),
    CONSTRAINT FK_Appointments_Staff FOREIGN KEY (AssignedStaffID) REFERENCES dbo.Users(UserID)
);
GO

CREATE INDEX IX_Appointments_Customer_StartAt ON dbo.Appointments(CustomerUserID, StartAt DESC);
CREATE INDEX IX_Appointments_Staff_StartAt ON dbo.Appointments(AssignedStaffID, StartAt DESC);
GO

-- Many-to-many: 1 appointment chọn nhiều services 
CREATE TABLE dbo.AppointmentServices (
    AppointmentID      INT NOT NULL,
    ServiceID          INT NOT NULL,
    Quantity           INT NOT NULL DEFAULT 1 CHECK (Quantity > 0),
    UnitPriceSnapshot  DECIMAL(12,2) NOT NULL CHECK (UnitPriceSnapshot >= 0),
    LineTotal          AS (Quantity * UnitPriceSnapshot) PERSISTED,

    CONSTRAINT PK_AppointmentServices PRIMARY KEY (AppointmentID, ServiceID),
    CONSTRAINT FK_AS_Appointments FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID),
    CONSTRAINT FK_AS_Services FOREIGN KEY (ServiceID) REFERENCES dbo.Services(ServiceID)
);
GO

-- Staff execution report: gộp spa + healthcheck + post-condition
CREATE TABLE dbo.AppointmentReports (
    AppointmentID        INT PRIMARY KEY,   -- 1 appointment có tối đa 1 report
    PerformedByStaffID   INT NOT NULL,

    BasicHealthCheckNote NVARCHAR(500) NULL,
    SpaServiceNote       NVARCHAR(500) NULL,
    PostServiceCondition NVARCHAR(500) NULL,
    ResultSummary        NVARCHAR(800) NULL,

    CreatedAt            DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt            DATETIME2 NULL,

    CONSTRAINT FK_Reports_Appointments FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID),
    CONSTRAINT FK_Reports_Staff FOREIGN KEY (PerformedByStaffID) REFERENCES dbo.Users(UserID)
);
GO


/* =========================================================
   5) FEEDBACK / NOTIFICATIONS / SETTINGS (Feature 7,8,10)
========================================================= */

-- Gộp review dịch vụ + feedback chung
CREATE TABLE dbo.Feedback (
    FeedbackID      INT IDENTITY(1,1) PRIMARY KEY,
    CustomerUserID  INT NOT NULL,
    AppointmentID   INT NULL, -- NULL = feedback chung; có ID = review dịch vụ

    Type            VARCHAR(20) NOT NULL
        CHECK (Type IN ('SERVICE_REVIEW','CUSTOMER_FEEDBACK')),

    Rating          INT NULL CHECK (Rating IS NULL OR (Rating >= 1 AND Rating <= 5)),
    Title           NVARCHAR(120) NULL,
    Content         NVARCHAR(800) NOT NULL,

    Status          VARCHAR(20) NOT NULL DEFAULT 'VISIBLE'
        CHECK (Status IN ('VISIBLE','HIDDEN','DELETED')),

    CreatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_Feedback_Users FOREIGN KEY (CustomerUserID) REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_Feedback_Appointments FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID)
);
GO

CREATE INDEX IX_Feedback_Appointment ON dbo.Feedback(AppointmentID);
GO

CREATE TABLE dbo.Notifications (
    NotificationID  INT IDENTITY(1,1) PRIMARY KEY,
    UserID          INT NOT NULL,

    Type            VARCHAR(30) NOT NULL
        CHECK (Type IN ('SERVICE_REMINDER','STAFF_ASSIGNMENT','SERVICE_STATUS_UPDATE','ORDER_STATUS_UPDATE','SYSTEM')),

    Title           NVARCHAR(120) NOT NULL,
    Message         NVARCHAR(500) NOT NULL,

    IsRead          BIT NOT NULL DEFAULT 0,
    SentAt          DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    ReadAt          DATETIME2 NULL,

    RefTable        VARCHAR(30) NULL, -- 'Appointments','Orders',...
    RefID           INT NULL,

    CONSTRAINT FK_Notifications_Users FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID)
);
GO

CREATE INDEX IX_Notifications_User_Read ON dbo.Notifications(UserID, IsRead, SentAt DESC);
GO

-- System configuration (Admin chỉnh rule chung)
CREATE TABLE dbo.SystemSettings (
    SettingKey      VARCHAR(80) PRIMARY KEY,
    SettingValue    NVARCHAR(400) NOT NULL,
    UpdatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO


/* =========================================================
   6) SEED DATA 
========================================================= */

INSERT INTO dbo.Roles(RoleName) VALUES ('Admin'), ('Staff'), ('Customer');
GO

-- Map route:
-- /services?type=vaccination
-- /services?type=boarding
-- /services?type=hygiene
INSERT INTO dbo.ServiceTypes(TypeCode, TypeName, Description) VALUES
('vaccination', N'Vaccination', N'Tiêm ngừa cho thú cưng'),
('boarding',    N'Pet Boarding', N'Giữ thú cưng theo ngày/đêm'),
('hygiene',     N'Grooming / Hygiene', N'Vệ sinh, tắm, cắt tỉa, combo');
GO

-- Sample services
INSERT INTO dbo.Services(ServiceTypeID, ServiceName, Price, Unit, DurationMins, Description) VALUES
(1, N'Vaccine 7 bệnh (Chó)', 150000, 'SESSION', 30, N'Tiêm vaccine tổng hợp'),
(1, N'Vaccine Dại (Mèo)',    100000, 'SESSION', 20, N'Tiêm phòng dại'),

(2, N'Chuồng tiêu chuẩn (<5kg)', 200000, 'NIGHT', NULL, N'Tính theo đêm'),
(2, N'Phòng VIP (Camera 24/7)',  500000, 'NIGHT', NULL, N'Tính theo đêm'),

(3, N'Combo Tắm + Cắt tỉa', 300000, 'SESSION', 60, N'Combo vệ sinh cơ bản'),
(3, N'Cạo vôi răng',        150000, 'SESSION', 30, N'Vệ sinh răng miệng');
GO
