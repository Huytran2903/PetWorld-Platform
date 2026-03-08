-- Insert test data for Pet Statistics Report
-- This script creates sample pets with different types, dates, and prices to test the statistics functionality

-- Clear existing data (optional - uncomment if you want to start fresh)
-- DELETE FROM Pets WHERE Name LIKE 'Test%';

-- Insert Service Pets (Owner is NOT NULL, Price is NULL)
-- Dogs for services
INSERT INTO Pets (Name, PetType, Breed, Gender, AgeMonths, WeightKg, Color, Note, Price, IsAvailable, CreatedAt, OwnerCustomerID)
VALUES 
('Test Dog Service 1', 'dog', 'Golden Retriever', 'Male', 12, 25.5, N'Golden', N'Service dog for grooming', NULL, 0, '2025-01-15 10:30:00', 1),
('Test Dog Service 2', 'dog', 'Poodle', 'Female', 8, 18.2, N'White', N'Service dog for training', NULL, 0, '2025-02-20 14:15:00', 2),
('Test Dog Service 3', 'dog', 'Bulldog', 'Male', 24, 15.8, N'Brown', N'Service dog for boarding', NULL, 0, '2025-03-10 09:45:00', 1),
('Test Dog Service 4', 'dog', 'Beagle', 'Female', 18, 12.3, N'Tricolor', N'Service dog for daycare', NULL, 0, '2025-04-05 16:20:00', 3);

-- Cats for services
INSERT INTO Pets (Name, PetType, Breed, Gender, AgeMonths, WeightKg, Color, Note, Price, IsAvailable, CreatedAt, OwnerCustomerID)
VALUES 
('Test Cat Service 1', 'cat', 'Persian', 'Female', 15, 4.2, N'White', N'Service cat for grooming', NULL, 0, '2025-01-25 11:00:00', 2),
('Test Cat Service 2', 'cat', 'Siamese', 'Male', 10, 3.8, N'Cream', N'Service cat for boarding', NULL, 0, '2025-03-15 13:30:00', 1),
('Test Cat Service 3', 'cat', 'Maine Coon', 'Female', 20, 6.5, N'Gray', N'Service cat for training', NULL, 0, '2025-05-08 10:15:00', 3);

-- Other pets for services
INSERT INTO Pets (Name, PetType, Breed, Gender, AgeMonths, WeightKg, Color, Note, Price, IsAvailable, CreatedAt, OwnerCustomerID)
VALUES 
('Test Rabbit Service 1', 'other', 'Holland Lop', 'Female', 6, 1.8, N'White', N'Service rabbit for boarding', NULL, 0, '2025-02-10 15:45:00', 2),
('Test Hamster Service 1', 'other', 'Syrian', 'Male', 4, 0.2, N'Golden', N'Service hamster for grooming', NULL, 0, '2025-04-12 12:00:00', 1);

-- Insert Sale Pets (Owner is NULL, Price is NOT NULL)
-- Dogs for sale
INSERT INTO Pets (Name, PetType, Breed, Gender, AgeMonths, WeightKg, Color, Note, Price, IsAvailable, CreatedAt, OwnerCustomerID)
VALUES 
('Test Dog Sale 1', 'dog', 'Labrador', 'Male', 3, 8.5, N'Black', N'Healthy puppy for sale', 2500000.00, 1, '2025-01-10 08:30:00', NULL),
('Test Dog Sale 2', 'dog', 'German Shepherd', 'Female', 4, 12.0, N'Brown', N'Trained puppy for sale', 3200000.00, 1, '2025-02-15 10:00:00', NULL),
('Test Dog Sale 3', 'dog', 'Husky', 'Male', 5, 14.2, N'Gray', N'Pure breed for sale', 4500000.00, 1, '2025-03-20 14:20:00', NULL),
('Test Dog Sale 4', 'dog', 'Corgi', 'Female', 2, 6.8, N'Tan', N'Cute puppy for sale', 2800000.00, 1, '2025-04-25 09:15:00', NULL),
('Test Dog Sale 5', 'dog', 'Shih Tzu', 'Male', 3, 5.5, N'White', N'Small breed for sale', 2200000.00, 1, '2025-05-15 11:30:00', NULL);

-- Cats for sale
INSERT INTO Pets (Name, PetType, Breed, Gender, AgeMonths, WeightKg, Color, Note, Price, IsAvailable, CreatedAt, OwnerCustomerID)
VALUES 
('Test Cat Sale 1', 'cat', 'British Shorthair', 'Male', 2, 3.2, N'Blue', N'Pure breed kitten for sale', 1800000.00, 1, '2025-01-20 12:45:00', NULL),
('Test Cat Sale 2', 'cat', 'Ragdoll', 'Female', 3, 4.5, N'Seal', N'Beautiful kitten for sale', 2500000.00, 1, '2025-03-10 15:30:00', NULL),
('Test Cat Sale 3', 'cat', 'Scottish Fold', 'Male', 4, 3.8, N'Cream', N'Unique breed for sale', 2800000.00, 1, '2025-05-05 10:00:00', NULL);

-- Other pets for sale
INSERT INTO Pets (Name, PetType, Breed, Gender, AgeMonths, WeightKg, Color, Note, Price, IsAvailable, CreatedAt, OwnerCustomerID)
VALUES 
('Test Parrot Sale 1', 'other', 'Macaw', 'Male', 12, 1.2, N'Blue', N'Exotic bird for sale', 8500000.00, 1, '2025-02-28 16:00:00', NULL),
('Test Turtle Sale 1', 'other', 'Red Eared Slider', 'Female', 8, 0.8, N'Green', N'Aquatic turtle for sale', 450000.00, 1, '2025-04-18 13:15:00', NULL);

-- Insert Sold/Completed Pets (Owner is NULL, Price is NOT NULL, PurchasedAt is NOT NULL)
-- Dogs sold
INSERT INTO Pets (Name, PetType, Breed, Gender, AgeMonths, WeightKg, Color, Note, Price, IsAvailable, CreatedAt, PurchasedAt, OwnerCustomerID)
VALUES 
('Test Dog Sold 1', 'dog', 'Pomeranian', 'Female', 6, 3.5, N'Orange', N'Sold to happy family', 3500000.00, 0, '2025-01-05 09:00:00', '2025-01-25 14:30:00', NULL),
('Test Dog Sold 2', 'dog', 'Dachshund', 'Male', 8, 7.2, N'Brown', N'Sold to regular customer', 2800000.00, 0, '2025-02-12 11:20:00', '2025-03-05 10:15:00', NULL),
('Test Dog Sold 3', 'dog', 'Boxer', 'Male', 10, 18.5, N'Fawn', N'Sold to new owner', 3200000.00, 0, '2025-03-18 15:45:00', '2025-04-10 12:00:00', NULL);

-- Cats sold
INSERT INTO Pets (Name, PetType, Breed, Gender, AgeMonths, WeightKg, Color, Note, Price, IsAvailable, CreatedAt, PurchasedAt, OwnerCustomerID)
VALUES 
('Test Cat Sold 1', 'cat', 'Bengal', 'Male', 5, 5.2, N'Spotted', N'Sold to cat lover', 3200000.00, 0, '2025-01-15 10:30:00', '2025-02-20 16:00:00', NULL),
('Test Cat Sold 2', 'cat', 'Abyssinian', 'Female', 7, 4.0, N'Ruddy', N'Sold to experienced owner', 2200000.00, 0, '2025-04-02 13:15:00', '2025-04-25 11:45:00', NULL);

-- Other pets sold
INSERT INTO Pets (Name, PetType, Breed, Gender, AgeMonths, WeightKg, Color, Note, Price, IsAvailable, CreatedAt, PurchasedAt, OwnerCustomerID)
VALUES 
('Test Fish Sold 1', 'other', 'Koi', 'Male', 24, 2.5, N'Orange', N'Sold to pond owner', 1200000.00, 0, '2025-02-05 14:00:00', '2025-02-28 10:30:00', NULL);

-- Insert some 2026 data for testing date range filtering
INSERT INTO Pets (Name, PetType, Breed, Gender, AgeMonths, WeightKg, Color, Note, Price, IsAvailable, CreatedAt, OwnerCustomerID)
VALUES 
('Test Dog 2026 Service 1', 'dog', 'Cocker Spaniel', 'Female', 16, 11.2, N'Golden', N'Service dog for 2026', NULL, 0, '2026-01-08 12:30:00', 2),
('Test Cat 2026 Sale 1', 'cat', 'Siberian', 'Male', 3, 4.8, N'Gray', N'Sale cat for 2026', 2800000.00, 1, '2026-02-15 09:45:00', NULL),
('Test Dog 2026 Sold 1', 'dog', 'Mastiff', 'Male', 12, 35.0, N'Fawn', N'Sold in 2026', 5500000.00, 0, '2026-03-01 14:20:00', '2026-03-20 10:00:00', NULL);

-- Summary of test data:
-- Service Pets: 4 Dogs + 3 Cats + 2 Others = 9 total
-- Sale Pets: 5 Dogs + 3 Cats + 2 Others = 10 total  
-- Sold Pets: 3 Dogs + 2 Cats + 1 Other = 6 total
-- 2026 Data: 1 Service Dog + 1 Sale Cat + 1 Sold Dog = 3 total

-- Date distribution:
-- 2025: 9 + 10 + 6 = 25 pets
-- 2026: 3 pets
-- Total: 28 pets

-- This data will test:
-- 1. Service vs Sale breakdown (based on Owner and Price)
-- 2. Sold/Completed tracking (based on PurchasedAt)
-- 3. Species breakdown (Dog/Cat/Other)
-- 4. Date range filtering (2025 vs 2026)
-- 5. Grand total calculations
