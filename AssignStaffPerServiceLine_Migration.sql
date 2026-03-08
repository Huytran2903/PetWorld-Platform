/* =========================================================
   Migration: Assign staff per service line in appointment
   Date: 2026-03-03
   Safe to run multiple times (idempotent)
   ========================================================= */

-- 1) Ensure appointment status check-constraint supports checked_in
IF EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK__Appointme__Statu__7D439ABD'
      AND parent_object_id = OBJECT_ID('dbo.Appointments')
)
BEGIN
    ALTER TABLE dbo.Appointments
    DROP CONSTRAINT CK__Appointme__Statu__7D439ABD;
END
GO

IF EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK_Appointments_Status'
      AND parent_object_id = OBJECT_ID('dbo.Appointments')
)
BEGIN
    ALTER TABLE dbo.Appointments
    DROP CONSTRAINT CK_Appointments_Status;
END
GO

ALTER TABLE dbo.Appointments
ADD CONSTRAINT CK_Appointments_Status
CHECK (Status IN ('pending','confirmed','checked_in','in_progress','done','canceled','no_show'));
GO

-- 2) Add AssignedStaffID on AppointmentServices only if missing
IF COL_LENGTH('dbo.AppointmentServices', 'AssignedStaffID') IS NULL
BEGIN
    ALTER TABLE dbo.AppointmentServices
    ADD AssignedStaffID INT NULL;
END
GO

-- 3) Add FK only if missing
IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_AppSvc_AssignedStaff'
      AND parent_object_id = OBJECT_ID('dbo.AppointmentServices')
)
BEGIN
    ALTER TABLE dbo.AppointmentServices
    ADD CONSTRAINT FK_AppSvc_AssignedStaff
    FOREIGN KEY (AssignedStaffID) REFERENCES dbo.Staff(StaffID);
END
GO

-- 4) Add index only if missing
IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_AppSvc_AssignedStaff'
      AND object_id = OBJECT_ID('dbo.AppointmentServices')
)
BEGIN
    CREATE INDEX IX_AppSvc_AssignedStaff
    ON dbo.AppointmentServices(AssignedStaffID);
END
GO

-- 5) Optional backfill: copy appointment-level assignment to service lines
UPDATE s
SET s.AssignedStaffID = a.StaffID
FROM dbo.AppointmentServices s
JOIN dbo.Appointments a ON a.AppointmentID = s.AppointmentID
WHERE s.AssignedStaffID IS NULL
  AND a.StaffID IS NOT NULL;
GO

-- 6) Add ServiceStatus on AppointmentServices only if missing
IF COL_LENGTH('dbo.AppointmentServices', 'ServiceStatus') IS NULL
BEGIN
    ALTER TABLE dbo.AppointmentServices
    ADD ServiceStatus VARCHAR(20) NOT NULL CONSTRAINT DF_AppSvc_ServiceStatus DEFAULT 'pending';
END
GO

-- 7) Add check constraint for service status (drop old first if exists)
IF EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK_AppSvc_ServiceStatus'
      AND parent_object_id = OBJECT_ID('dbo.AppointmentServices')
)
BEGIN
    ALTER TABLE dbo.AppointmentServices
    DROP CONSTRAINT CK_AppSvc_ServiceStatus;
END
GO

ALTER TABLE dbo.AppointmentServices
ADD CONSTRAINT CK_AppSvc_ServiceStatus
CHECK (ServiceStatus IN ('pending','assigned','in_progress','done','canceled','no_show'));
GO

-- 8) Backfill existing rows by assignment
UPDATE dbo.AppointmentServices
SET ServiceStatus = CASE
    WHEN AssignedStaffID IS NULL THEN 'pending'
    ELSE 'assigned'
END
WHERE ServiceStatus IS NULL OR LTRIM(RTRIM(ServiceStatus)) = '';
GO

-- 9) Verification queries
SELECT name AS column_name
FROM sys.columns
WHERE object_id = OBJECT_ID('dbo.AppointmentServices')
  AND name = 'AssignedStaffID';

SELECT fk.name AS foreign_key_name
FROM sys.foreign_keys fk
WHERE fk.parent_object_id = OBJECT_ID('dbo.AppointmentServices')
  AND fk.name = 'FK_AppSvc_AssignedStaff';

SELECT i.name AS index_name
FROM sys.indexes i
WHERE i.object_id = OBJECT_ID('dbo.AppointmentServices')
  AND i.name = 'IX_AppSvc_AssignedStaff';
