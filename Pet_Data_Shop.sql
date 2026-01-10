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
    RoleID INT IDENTITY(1,1) PRIMARY KEY,
    RoleName VARCHAR(30) NOT NULL UNIQUE -- admin, staff, customer
);
GO
CREATE TABLE dbo.Users (
    UserID INT IDENTITY(1,1) PRIMARY KEY,
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
CREATE TABLE dbo.UserRoles (
    UserID INT NOT NULL,
    RoleID INT NOT NULL,
    PRIMARY KEY (UserID, RoleID),
    CONSTRAINT FK_UserRoles_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_UserRoles_Role FOREIGN KEY (RoleID) REFERENCES dbo.Roles(RoleID)
);
GO
/* Seed roles */
INSERT dbo.Roles(RoleName)
SELECT 'admin' WHERE NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE RoleName='admin');
INSERT dbo.Roles(RoleName)
SELECT 'staff' WHERE NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE RoleName='staff');
INSERT dbo.Roles(RoleName)
SELECT 'customer' WHERE NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE RoleName='customer');
GO
/* =========================================================
   2) STAFF
   ========================================================= */
CREATE TABLE dbo.Staff (
    StaffID INT PRIMARY KEY,
    HireDate DATE NULL,
    Bio NVARCHAR(300) NULL,
    CONSTRAINT FK_Staff_Users FOREIGN KEY (StaffID) REFERENCES dbo.Users(UserID)
);
GO
/* =========================================================
   3) PRODUCT CATEGORIES / PRODUCTS
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
   4) PETS (thống nhất PetForSale + CustomerPets)
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
    OwnerUserID INT NULL,
    PurchasedAt DATETIME2 NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_Pets_Owner FOREIGN KEY (OwnerUserID) REFERENCES dbo.Users(UserID),
    CONSTRAINT CK_Pets_Owner_Available CHECK (
        (OwnerUserID IS NULL AND IsAvailable = 1) OR
        (OwnerUserID IS NOT NULL AND IsAvailable = 0)
    ),
    CONSTRAINT CK_Pets_Price_WhenSale CHECK (
        (OwnerUserID IS NULL AND Price IS NOT NULL) OR
        (OwnerUserID IS NOT NULL)
    )
);
GO
/* =========================================================
   5) SERVICES / APPOINTMENTS / ASSIGNMENT / SCHEDULE
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
    UserID INT NOT NULL,
    PetID INT NOT NULL,
    AppointmentDate DATETIME2 NOT NULL,
    Note NVARCHAR(255) NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (Status IN ('pending','confirmed','in_progress','done','canceled','no_show')),
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_App_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID),
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
   6) LỊCH SỬ TIÊM VẮC-XIN & KIỂM TRA SỨC KHỎE
   ========================================================= */
CREATE TABLE dbo.PetVaccinations (
    VaccinationID INT IDENTITY(1,1) PRIMARY KEY,
    PetID INT NOT NULL,
    VaccineName NVARCHAR(100) NOT NULL,
    AdministeredDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    NextDueDate DATETIME2 NULL,
    AppointmentID INT NULL,
    PerformedBy INT NULL,
    Note NVARCHAR(500) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_PetVacc_Pet FOREIGN KEY (PetID) REFERENCES dbo.Pets(PetID),
    CONSTRAINT FK_PetVacc_App FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID) ON DELETE SET NULL,
    CONSTRAINT FK_PetVacc_Staff FOREIGN KEY (PerformedBy) REFERENCES dbo.Staff(StaffID)
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
    PerformedBy INT NULL,
    Note NVARCHAR(500) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Health_Pet FOREIGN KEY (PetID) REFERENCES dbo.Pets(PetID),
    CONSTRAINT FK_Health_App FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID) ON DELETE SET NULL,
    CONSTRAINT FK_Health_Staff FOREIGN KEY (PerformedBy) REFERENCES dbo.Staff(StaffID)
);
GO
/* =========================================================
   7) CART / CART ITEMS
   ========================================================= */
CREATE TABLE dbo.Carts (
    CartID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL UNIQUE,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_Carts_Users FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID)
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
   8) ORDERS / ORDER ITEMS
   ========================================================= */
CREATE TABLE dbo.Orders (
    OrderID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL,
    OrderCode VARCHAR(30) NOT NULL UNIQUE,
    ShipName NVARCHAR(120) NULL,
    ShipPhone VARCHAR(20) NULL,
    ShipAddress NVARCHAR(255) NULL,
    Note NVARCHAR(255) NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (Status IN ('pending','paid','processing','shipped','done','canceled','refunded')),
    Subtotal DECIMAL(12,2) NOT NULL DEFAULT 0,
    DiscountTotal DECIMAL(12,2) NOT NULL DEFAULT 0,
    ShippingFee DECIMAL(12,2) NOT NULL DEFAULT 0,
    TotalAmount DECIMAL(12,2) NOT NULL DEFAULT 0,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_Orders_Users FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID)
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
   9) PAYMENTS
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
   10) FEEDBACK & SERVICE REVIEWS
   ========================================================= */
CREATE TABLE dbo.Feedbacks (
    FeedbackID INT IDENTITY(1,1) PRIMARY KEY,
    FirstName NVARCHAR(100) NOT NULL,
    LastName NVARCHAR(100) NOT NULL,
    Email VARCHAR(255) NOT NULL,
    PhoneNumber VARCHAR(20) NULL,
    Subject NVARCHAR(100) NOT NULL,
    Message NVARCHAR(MAX) NOT NULL,
    UserID INT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Feedback_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE SET NULL
);
GO
CREATE TABLE dbo.ServiceReviews (
    ReviewID INT IDENTITY(1,1) PRIMARY KEY,
    AppointmentID INT NOT NULL,
    Rating INT NOT NULL CHECK (Rating BETWEEN 1 AND 5),
    Comment NVARCHAR(MAX) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UserID INT NOT NULL,
    CONSTRAINT FK_Review_App FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID),
    CONSTRAINT FK_Review_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID)
);
GO
/* =========================================================
   11) INDEXES
   ========================================================= */
CREATE INDEX IX_Products_Category ON dbo.Products(CategoryID);
CREATE INDEX IX_Pets_Owner ON dbo.Pets(OwnerUserID);
CREATE INDEX IX_Pets_Available ON dbo.Pets(IsAvailable) WHERE IsAvailable = 1;
CREATE INDEX IX_Orders_User ON dbo.Orders(UserID, CreatedAt);
CREATE INDEX IX_OrderItems_Order ON dbo.OrderItems(OrderID);
CREATE INDEX IX_Appointments_User ON dbo.Appointments(UserID, AppointmentDate);
CREATE INDEX IX_Appointments_Pet ON dbo.Appointments(PetID);
CREATE INDEX IX_Appointments_StatusDate ON dbo.Appointments(Status, AppointmentDate);
CREATE INDEX IX_Payments_TypeStatus ON dbo.Payments(PaymentType, Status, PaidAt);
GO
/* =========================================================
   12) SEED ADMIN
   ========================================================= */
IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE Username='admin@')
BEGIN
    INSERT INTO dbo.Users (Username, PasswordHash, Email, FullName)
    VALUES ('admin@', 'e10adc3949ba59abbe56e057f20f883e', 'admin123@gmail.com', N'Administrator');
    DECLARE @AdminID INT = SCOPE_IDENTITY();
    INSERT dbo.UserRoles(UserID, RoleID)
    SELECT @AdminID, RoleID FROM dbo.Roles WHERE RoleName='admin';
    INSERT dbo.Staff(StaffID) VALUES (@AdminID);
END
GO
