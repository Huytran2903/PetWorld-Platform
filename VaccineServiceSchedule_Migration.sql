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

