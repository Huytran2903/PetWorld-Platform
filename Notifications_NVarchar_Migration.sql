-- Fix Vietnamese (Unicode) in notification titles: VARCHAR stores non-Unicode and shows ? for diacritics.
-- Run once on SQL Server if the table was created with VARCHAR for Title/Type.
-- Hibernate maps Message as NVARCHAR(MAX) already; Title/Type must be NVARCHAR too.

IF EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Notifications')
BEGIN
    ALTER TABLE Notifications ALTER COLUMN Title NVARCHAR(200) NOT NULL;
    ALTER TABLE Notifications ALTER COLUMN Type NVARCHAR(50) NOT NULL;
END

-- One-time data fix: legacy titles stored as VARCHAR/mojibake (e.g. D?ch v?, L?ch h?n) -> English
IF EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Notifications')
BEGIN
    UPDATE Notifications SET Title = N'Service completed'
    WHERE Type = N'appointment_done';

    UPDATE Notifications SET Title = N'Appointment confirmed'
    WHERE Type = N'appointment_confirmed';
END

ALTER TABLE system_configs WITH NOCHECK
ADD CONSTRAINT FK_SystemConfigs_Staff
FOREIGN KEY (updated_by_staff_id) REFERENCES Staff(StaffID); ê ô ô chạy thêm đoạn này trong db nữa nhe