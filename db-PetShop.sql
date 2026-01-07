-- =======================================================
--  DATABASE: petshop
-- =======================================================
CREATE DATABASE petshop;
USE petshop;

CREATE TABLE users (
    id INT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role VARCHAR(10) CHECK (role IN ('admin', 'user')) DEFAULT 'user',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
GO

CREATE TABLE categories (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description TEXT
);
GO

CREATE TABLE products (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    image VARCHAR(255),
    description TEXT,
    category_id INT,
    stock INT DEFAULT 0,
	salePrice DECIMAL(10,2) NULL,
	discount DECIMAL(5,2) DEFAULT 0,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);
GO


CREATE TABLE pet (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(10) CHECK (type IN ('dog', 'cat')) NOT NULL,
    age VARCHAR(50),
    gender VARCHAR(10) CHECK (gender IN ('Male', 'Female')),
    price DECIMAL(10,2),
    image VARCHAR(255),
    description TEXT,
    available BIT DEFAULT 1,
    weight DECIMAL(4,1),
    color VARCHAR(30),
    breed VARCHAR(50),
    vaccinated BIT DEFAULT 0,
	salePrice DECIMAL(10,2) NULL,
	discount DECIMAL(5,2) DEFAULT 0,
	status AS 
    (CASE WHEN available = 1 THEN 'Available' ELSE 'Not Available' END)

);


CREATE TABLE cart (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    pet_id INT NULL,
    product_id INT NULL,
    quantity INT DEFAULT 1,
    added_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (pet_id) REFERENCES pet(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
GO


CREATE TABLE orders (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT,
    total_price DECIMAL(10,2),
    status VARCHAR(10) CHECK (status IN ('pending', 'done', 'canceled')) DEFAULT 'pending',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
GO

CREATE TABLE order_items (
    order_id INT,
    product_type VARCHAR(10) CHECK (product_type IN ('product', 'pet')) DEFAULT 'product',
    product_id INT,
    quantity INT DEFAULT 1,
    price DECIMAL(10,2),
    PRIMARY KEY (order_id, product_id, product_type),
    FOREIGN KEY (order_id) REFERENCES orders(id)
);
GO
CREATE TABLE Feedbacks (
    Id INT PRIMARY KEY IDENTITY(1,1),
    
    FirstName NVARCHAR(100) NOT NULL,
    LastName NVARCHAR(100) NOT NULL,
    Email VARCHAR(255) NOT NULL, 
    PhoneNumber VARCHAR(20),
    [Subject] NVARCHAR(50) NOT NULL,
    [Message] NVARCHAR(MAX) NOT NULL, 
    
    User_id INT NULL,
    
    Created_At DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT FK_Feedback_User FOREIGN KEY (User_id) REFERENCES Users(Id) ON DELETE SET NULL
);

INSERT INTO users (username, password, email, role) VALUES 
('admin@', 'e10adc3949ba59abbe56e057f20f883e', 'admin123@gmail.com', 'admin')
