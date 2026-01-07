/* =========================================================
   0) CREATE DATABASE
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
   1) USERS / ROLES / AUTH
========================================================= */
CREATE TABLE dbo.Roles (
    RoleID      INT IDENTITY(1,1) PRIMARY KEY,
    RoleName    VARCHAR(30) NOT NULL UNIQUE   -- admin, staff, customer...
);
GO

CREATE TABLE dbo.Users (
    UserID          INT IDENTITY(1,1) PRIMARY KEY,
    Username        VARCHAR(60) NOT NULL UNIQUE,
    PasswordHash    VARCHAR(255) NOT NULL,        -- MD5/SHA/BCrypt tuỳ app
    Email           VARCHAR(120) NULL UNIQUE,
    Phone           VARCHAR(20) NULL,
    FullName        NVARCHAR(120) NULL,
    IsActive        BIT NOT NULL DEFAULT 1,
    CreatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt       DATETIME2 NULL
);
GO

CREATE TABLE dbo.UserRoles (
    UserID  INT NOT NULL,
    RoleID  INT NOT NULL,
    PRIMARY KEY (UserID, RoleID),
    CONSTRAINT FK_UserRoles_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_UserRoles_Role FOREIGN KEY (RoleID) REFERENCES dbo.Roles(RoleID)
);
GO

/* seed roles */
IF NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE RoleName='admin')   INSERT dbo.Roles(RoleName) VALUES ('admin');
IF NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE RoleName='staff')   INSERT dbo.Roles(RoleName) VALUES ('staff');
IF NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE RoleName='customer')INSERT dbo.Roles(RoleName) VALUES ('customer');
GO

/* =========================================================
   2) PRODUCT CATEGORIES / PRODUCTS
========================================================= */
CREATE TABLE dbo.Categories (
    CategoryID      INT IDENTITY(1,1) PRIMARY KEY,
    Name            NVARCHAR(80) NOT NULL UNIQUE,
    Description     NVARCHAR(500) NULL,
    IsActive        BIT NOT NULL DEFAULT 1,
    CreatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

CREATE TABLE dbo.Products (
    ProductID       INT IDENTITY(1,1) PRIMARY KEY,
    CategoryID      INT NULL,
    Name            NVARCHAR(150) NOT NULL,
    SKU             VARCHAR(50) NULL UNIQUE,
    Price           DECIMAL(12,2) NOT NULL CHECK (Price >= 0),
    SalePrice       DECIMAL(12,2) NULL CHECK (SalePrice IS NULL OR SalePrice >= 0),
    DiscountPercent DECIMAL(5,2) NOT NULL DEFAULT 0 CHECK (DiscountPercent >= 0 AND DiscountPercent <= 100),
    Stock           INT NOT NULL DEFAULT 0 CHECK (Stock >= 0),
    ImageUrl        VARCHAR(255) NULL,
    Description     NVARCHAR(MAX) NULL,
    IsActive        BIT NOT NULL DEFAULT 1,
    CreatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt       DATETIME2 NULL,
    CONSTRAINT FK_Products_Categories FOREIGN KEY (CategoryID) REFERENCES dbo.Categories(CategoryID)
);
GO

/* =========================================================
   3) PETS FOR SALE (thú cưng cửa hàng bán)
========================================================= */
CREATE TABLE dbo.PetForSale (
    PetForSaleID    INT IDENTITY(1,1) PRIMARY KEY,
    Name            NVARCHAR(120) NOT NULL,
    PetType         VARCHAR(20) NOT NULL CHECK (PetType IN ('dog','cat','other')),
    Breed           NVARCHAR(80) NULL,
    Gender          VARCHAR(10) NULL CHECK (Gender IN ('Male','Female')),
    AgeMonths       INT NULL CHECK (AgeMonths IS NULL OR AgeMonths >= 0),
    WeightKg        DECIMAL(5,2) NULL CHECK (WeightKg IS NULL OR WeightKg >= 0),
    Color           NVARCHAR(40) NULL,
    Vaccinated      BIT NOT NULL DEFAULT 0,
    Price           DECIMAL(12,2) NOT NULL CHECK (Price >= 0),
    SalePrice       DECIMAL(12,2) NULL CHECK (SalePrice IS NULL OR SalePrice >= 0),
    DiscountPercent DECIMAL(5,2) NOT NULL DEFAULT 0 CHECK (DiscountPercent >= 0 AND DiscountPercent <= 100),
    ImageUrl        VARCHAR(255) NULL,
    Description     NVARCHAR(MAX) NULL,
    IsAvailable     BIT NOT NULL DEFAULT 1,
    CreatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt       DATETIME2 NULL,
    Status AS (CASE WHEN IsAvailable=1 THEN 'Available' ELSE 'Not Available' END)
);
GO

/* =========================================================
   4) CUSTOMER PET PROFILES (thú cưng của khách để đặt dịch vụ)
========================================================= */
CREATE TABLE dbo.CustomerPets (
    CustomerPetID   INT IDENTITY(1,1) PRIMARY KEY,
    UserID          INT NOT NULL,  -- owner
    Name            NVARCHAR(120) NOT NULL,
    PetType         VARCHAR(20) NOT NULL CHECK (PetType IN ('dog','cat','other')),
    Breed           NVARCHAR(80) NULL,
    Gender          VARCHAR(10) NULL CHECK (Gender IN ('Male','Female')),
    AgeMonths       INT NULL CHECK (AgeMonths IS NULL OR AgeMonths >= 0),
    WeightKg        DECIMAL(5,2) NULL CHECK (WeightKg IS NULL OR WeightKg >= 0),
    Color           NVARCHAR(40) NULL,
    Vaccinated      BIT NOT NULL DEFAULT 0,
    Note            NVARCHAR(500) NULL,
    ImageUrl        VARCHAR(255) NULL,
    CreatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt       DATETIME2 NULL,
    CONSTRAINT FK_CustomerPets_Users FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID)
);
GO

/* =========================================================
   5) CART / CART ITEMS (product OR pet-for-sale)
========================================================= */
CREATE TABLE dbo.Carts (
    CartID      INT IDENTITY(1,1) PRIMARY KEY,
    UserID      INT NOT NULL UNIQUE,   -- 1 user 1 cart
    CreatedAt   DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt   DATETIME2 NULL,
    CONSTRAINT FK_Carts_Users FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID)
);
GO

CREATE TABLE dbo.CartItems (
    CartItemID      INT IDENTITY(1,1) PRIMARY KEY,
    CartID          INT NOT NULL,
    ProductID       INT NULL,
    PetForSaleID    INT NULL,
    Quantity        INT NOT NULL DEFAULT 1 CHECK (Quantity > 0),
    AddedAt         DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_CartItems_Carts FOREIGN KEY (CartID) REFERENCES dbo.Carts(CartID),
    CONSTRAINT FK_CartItems_Products FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID),
    CONSTRAINT FK_CartItems_PetsForSale FOREIGN KEY (PetForSaleID) REFERENCES dbo.PetForSale(PetForSaleID),
    CONSTRAINT CK_CartItems_OnlyOneItemType CHECK (
        (CASE WHEN ProductID IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN PetForSaleID IS NULL THEN 0 ELSE 1 END) = 1
    )
);
GO

/* =========================================================
   6) ORDERS / ORDER ITEMS / PAYMENTS
========================================================= */
CREATE TABLE dbo.Orders (
    OrderID         INT IDENTITY(1,1) PRIMARY KEY,
    UserID          INT NOT NULL,
    OrderCode       VARCHAR(30) NOT NULL UNIQUE, -- e.g. ORD2026...
    ShipName        NVARCHAR(120) NULL,
    ShipPhone       VARCHAR(20) NULL,
    ShipAddress     NVARCHAR(255) NULL,
    Note            NVARCHAR(255) NULL,
    Status          VARCHAR(20) NOT NULL DEFAULT 'pending'
                    CHECK (Status IN ('pending','paid','processing','shipped','done','canceled','refunded')),
    Subtotal        DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (Subtotal >= 0),
    DiscountTotal   DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (DiscountTotal >= 0),
    ShippingFee     DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (ShippingFee >= 0),
    TotalAmount     DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (TotalAmount >= 0),
    CreatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt       DATETIME2 NULL,
    CONSTRAINT FK_Orders_Users FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID)
);
GO

CREATE TABLE dbo.OrderItems (
    OrderItemID     INT IDENTITY(1,1) PRIMARY KEY,
    OrderID         INT NOT NULL,
    ProductID       INT NULL,
    PetForSaleID    INT NULL,
    ItemName        NVARCHAR(150) NOT NULL,            -- snapshot name
    UnitPrice       DECIMAL(12,2) NOT NULL CHECK (UnitPrice >= 0), -- snapshot price
    Quantity        INT NOT NULL DEFAULT 1 CHECK (Quantity > 0),
    LineTotal       AS (UnitPrice * Quantity) PERSISTED,
    CONSTRAINT FK_OrderItems_Orders FOREIGN KEY (OrderID) REFERENCES dbo.Orders(OrderID),
    CONSTRAINT FK_OrderItems_Products FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID),
    CONSTRAINT FK_OrderItems_PetsForSale FOREIGN KEY (PetForSaleID) REFERENCES dbo.PetForSale(PetForSaleID),
    CONSTRAINT CK_OrderItems_OnlyOneItemType CHECK (
        (CASE WHEN ProductID IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN PetForSaleID IS NULL THEN 0 ELSE 1 END) = 1
    )
);
GO

CREATE TABLE dbo.Payments (
    PaymentID       INT IDENTITY(1,1) PRIMARY KEY,
    OrderID         INT NOT NULL,
    Method          VARCHAR(30) NOT NULL CHECK (Method IN ('cod','bank','momo','vnpay','paypal','other')),
    Amount          DECIMAL(12,2) NOT NULL CHECK (Amount >= 0),
    Status          VARCHAR(20) NOT NULL DEFAULT 'pending'
                    CHECK (Status IN ('pending','success','failed','refunded')),
    PaidAt          DATETIME2 NULL,
    CreatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Payments_Orders FOREIGN KEY (OrderID) REFERENCES dbo.Orders(OrderID)
);
GO

/* =========================================================
   7) SERVICES / APPOINTMENTS (đặt lịch chăm sóc)
========================================================= */
CREATE TABLE dbo.Services (
    ServiceID       INT IDENTITY(1,1) PRIMARY KEY,
    Name            NVARCHAR(120) NOT NULL,
    Description     NVARCHAR(500) NULL,
    BasePrice       DECIMAL(12,2) NOT NULL CHECK (BasePrice >= 0),
    DurationMinutes INT NOT NULL DEFAULT 30 CHECK (DurationMinutes > 0),
    IsActive        BIT NOT NULL DEFAULT 1,
    CreatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

CREATE TABLE dbo.Appointments (
    AppointmentID   INT IDENTITY(1,1) PRIMARY KEY,
    AppointmentCode VARCHAR(30) NOT NULL UNIQUE,
    UserID          INT NOT NULL,              -- customer
    CustomerPetID   INT NOT NULL,              -- pet của khách
    AppointmentDate DATETIME2 NOT NULL,        -- start datetime
    Note            NVARCHAR(255) NULL,
    Status          VARCHAR(20) NOT NULL DEFAULT 'pending'
                    CHECK (Status IN ('pending','confirmed','in_progress','done','canceled','no_show')),
    CreatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt       DATETIME2 NULL,
    CONSTRAINT FK_Appointments_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_Appointments_CustomerPets FOREIGN KEY (CustomerPetID) REFERENCES dbo.CustomerPets(CustomerPetID)
);
GO

CREATE TABLE dbo.AppointmentServices (
    AppointmentServiceID INT IDENTITY(1,1) PRIMARY KEY,
    AppointmentID   INT NOT NULL,
    ServiceID       INT NOT NULL,
    Price           DECIMAL(12,2) NOT NULL CHECK (Price >= 0),  -- snapshot
    Quantity        INT NOT NULL DEFAULT 1 CHECK (Quantity > 0),
    LineTotal       AS (Price * Quantity) PERSISTED,
    CONSTRAINT FK_AppSvc_App FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID),
    CONSTRAINT FK_AppSvc_Service FOREIGN KEY (ServiceID) REFERENCES dbo.Services(ServiceID),
    CONSTRAINT UQ_AppSvc UNIQUE (AppointmentID, ServiceID)
);
GO

/* staff scheduling & assignment */
CREATE TABLE dbo.StaffProfiles (
    StaffID     INT PRIMARY KEY,              -- = UserID
    HireDate    DATE NULL,
    Bio         NVARCHAR(300) NULL,
    CONSTRAINT FK_StaffProfiles_Users FOREIGN KEY (StaffID) REFERENCES dbo.Users(UserID)
);
GO

CREATE TABLE dbo.StaffSchedules (
    ScheduleID  INT IDENTITY(1,1) PRIMARY KEY,
    StaffID     INT NOT NULL,
    WorkDate    DATE NOT NULL,
    StartTime   TIME(0) NOT NULL,
    EndTime     TIME(0) NOT NULL,
    Note        NVARCHAR(255) NULL,
    CONSTRAINT FK_Schedules_Staff FOREIGN KEY (StaffID) REFERENCES dbo.StaffProfiles(StaffID),
    CONSTRAINT CK_Schedule_Time CHECK (EndTime > StartTime),
    CONSTRAINT UQ_Staff_WorkDate UNIQUE (StaffID, WorkDate, StartTime, EndTime)
);
GO

CREATE TABLE dbo.AppointmentAssignments (
    AppointmentID   INT NOT NULL,
    StaffID         INT NOT NULL,
    AssignedAt      DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    PRIMARY KEY (AppointmentID, StaffID),
    CONSTRAINT FK_Assign_App FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID),
    CONSTRAINT FK_Assign_Staff FOREIGN KEY (StaffID) REFERENCES dbo.StaffProfiles(StaffID)
);
GO

/* Appointment payment (dịch vụ cũng có thể thanh toán) */
CREATE TABLE dbo.ServicePayments (
    ServicePaymentID INT IDENTITY(1,1) PRIMARY KEY,
    AppointmentID    INT NOT NULL,
    Method           VARCHAR(30) NOT NULL CHECK (Method IN ('cash','bank','momo','vnpay','other')),
    Amount           DECIMAL(12,2) NOT NULL CHECK (Amount >= 0),
    Status           VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (Status IN ('pending','success','failed','refunded')),
    PaidAt           DATETIME2 NULL,
    CreatedAt        DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_ServicePayments_App FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID)
);
GO

/* =========================================================
   8) FEEDBACKS / CONTACT
========================================================= */
CREATE TABLE dbo.Feedbacks (
    FeedbackID   INT IDENTITY(1,1) PRIMARY KEY,
    FirstName    NVARCHAR(100) NOT NULL,
    LastName     NVARCHAR(100) NOT NULL,
    Email        VARCHAR(255) NOT NULL,
    PhoneNumber  VARCHAR(20) NULL,
    Subject      NVARCHAR(100) NOT NULL,
    Message      NVARCHAR(MAX) NOT NULL,
    UserID       INT NULL,
    CreatedAt    DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Feedback_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE SET NULL
);
GO

/* =========================================================
   9) INDEXES (cơ bản để chạy nhanh)
========================================================= */
CREATE INDEX IX_Products_Category ON dbo.Products(CategoryID);
CREATE INDEX IX_PetForSale_Available ON dbo.PetForSale(IsAvailable);
CREATE INDEX IX_Order_User ON dbo.Orders(UserID, CreatedAt);
CREATE INDEX IX_OrderItems_Order ON dbo.OrderItems(OrderID);
CREATE INDEX IX_Appointments_User ON dbo.Appointments(UserID, AppointmentDate);
CREATE INDEX IX_Appointments_StatusDate ON dbo.Appointments(Status, AppointmentDate);
GO

/* =========================================================
   10) SEED ADMIN USER (theo dữ liệu bạn đưa)
========================================================= */
IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE Username='admin@')
BEGIN
    INSERT INTO dbo.Users (Username, PasswordHash, Email, FullName)
    VALUES ('admin@', 'e10adc3949ba59abbe56e057f20f883e', 'admin123@gmail.com', N'Administrator');

    DECLARE @AdminID INT = SCOPE_IDENTITY();
    INSERT dbo.UserRoles(UserID, RoleID)
    SELECT @AdminID, RoleID FROM dbo.Roles WHERE RoleName='admin';
END
GO
