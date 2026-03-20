/* =========================================================
   Migration: Add IsOneTimeVaccine flag on Services
   Date: 2026-03-18
   ========================================================= */

IF COL_LENGTH('dbo.Services', 'IsOneTimeVaccine') IS NULL
BEGIN
    ALTER TABLE dbo.Services
    ADD IsOneTimeVaccine BIT NOT NULL CONSTRAINT DF_Services_IsOneTimeVaccine DEFAULT 0;
END
GO

/* =========================================================
   Migration: Add Notifications table for customer alerts
   Date: 2026-03-19
   ========================================================= */

IF OBJECT_ID('dbo.Notifications', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Notifications (
        NotificationID INT IDENTITY(1,1) PRIMARY KEY,
        CustomerID INT NOT NULL,
        AppointmentID INT NULL,
        Title NVARCHAR(200) NOT NULL,
        Message NVARCHAR(MAX) NOT NULL,
        Type VARCHAR(50) NOT NULL,
        IsRead BIT NOT NULL CONSTRAINT DF_Notifications_IsRead DEFAULT 0,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Notifications_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT FK_Notifications_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID),
        CONSTRAINT FK_Notifications_Appointment FOREIGN KEY (AppointmentID) REFERENCES dbo.Appointments(AppointmentID)
    );

    CREATE INDEX IX_Notifications_Customer_CreatedAt
        ON dbo.Notifications(CustomerID, CreatedAt DESC);

    CREATE INDEX IX_Notifications_Customer_IsRead
        ON dbo.Notifications(CustomerID, IsRead);
END
GO

/* =========================================================
   Seed staff/admin accounts
   - admin: thai01 / 123456
   - staff: thai02 / 123456
   PasswordHash uses BCrypt to match Spring Security config.
   ========================================================= */

DECLARE @AdminRoleID INT = (SELECT TOP 1 RoleID FROM dbo.Roles WHERE RoleName = 'admin');
DECLARE @StaffRoleID INT = (SELECT TOP 1 RoleID FROM dbo.Roles WHERE RoleName = 'staff');
DECLARE @DefaultHash VARCHAR(255) = '$2a$10$mAmFNpItJZzLBtgxhoKEvOkJbc2WXWB2oh0sNjaBDZICaXJgzMI2O';

IF @AdminRoleID IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.Staff WHERE Username = 'thai01')
    BEGIN
        INSERT INTO dbo.Staff (RoleID, Username, PasswordHash, Email, Phone, FullName, HireDate, Bio, IsActive)
        VALUES (@AdminRoleID, 'thai01', @DefaultHash, 'thai01@petworld.local', '0900000001', N'Thai Admin', CAST(GETDATE() AS DATE), N'Admin account seed', 1);
    END
END

IF @StaffRoleID IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.Staff WHERE Username = 'thai02')
    BEGIN
        INSERT INTO dbo.Staff (RoleID, Username, PasswordHash, Email, Phone, FullName, HireDate, Bio, IsActive)
        VALUES (@StaffRoleID, 'thai02', @DefaultHash, 'thai02@petworld.local', '0900000002', N'Thai Staff', CAST(GETDATE() AS DATE), N'Staff account seed', 1);
    END
END
GO

