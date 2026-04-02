<<<<<<< HEAD
# second-round-assignm-final-20319-bhavesh
Final Project Assignment - This repository contains the complete final project code and documentation.
=======
# ShopEase E-Commerce API

A production-ready Spring Boot backend for an e-commerce system with JWT authentication, Stripe payment integration, and comprehensive API documentation.

## Features

- **Authentication & Authorization**
  - User registration and login with JWT tokens
  - BCrypt password hashing
  - Role-based access control (USER/ADMIN)
  - Secure endpoints with Spring Security

- **Product Management**
  - Full CRUD operations for products
  - Search, filter, and pagination
  - Category-based filtering
  - Price range filtering
  - Admin-only product management

- **Shopping Cart**
  - Add/update/remove items
  - Stock validation
  - User-specific cart isolation

- **Order Management**
  - Create orders from cart
  - Order history
  - Order status tracking
  - Admin order management

- **Payment Integration**
  - Stripe Checkout Sessions
  - Webhook handling
  - Payment status tracking
  - Refund support

## Tech Stack

- Java 17
- Spring Boot 3.2
- Spring Security 6
- Spring Data JPA
- MySQL 8
- JWT (jjwt 0.12)
- Stripe SDK
- Swagger/OpenAPI 3
- JUnit 5 & Mockito

## Prerequisites

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Stripe Account

## Quick Start

### 1. Clone and Configure

```bash
cd backend
```

### 2. Database Setup

```sql
CREATE DATABASE shopease_db;
CREATE USER 'shopease'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON shopease_db.* TO 'shopease'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Environment Variables

Create a `.env` file or set environment variables:

```bash
# Database
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=shopease_db
export DB_USERNAME=shopease
export DB_PASSWORD=your_password

# JWT
export JWT_SECRET=your_base64_encoded_secret_key_at_least_256_bits
export JWT_EXPIRATION=86400000

# Stripe
export STRIPE_API_KEY=sk_test_your_stripe_secret_key
export STRIPE_PUBLIC_KEY=pk_test_your_stripe_public_key
export STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret
```

### 4. Build and Run

```bash
mvn clean install
mvn spring-boot:run
```

### 5. Access the Application

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/api-docs

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/register | Register new user |
| POST | /api/auth/login | User login |
| GET | /api/auth/me | Get current user |
| POST | /api/auth/logout | Logout |

### Products
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | /api/products | List products | Public |
| GET | /api/products/{id} | Get product | Public |
| GET | /api/products/search | Search products | Public |
| POST | /api/products | Create product | Admin |
| PUT | /api/products/{id} | Update product | Admin |
| DELETE | /api/products/{id} | Delete product | Admin |

### Cart
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/cart | Get cart |
| POST | /api/cart/items | Add to cart |
| PUT | /api/cart/items/{productId} | Update item |
| DELETE | /api/cart/items/{productId} | Remove item |
| DELETE | /api/cart | Clear cart |

### Orders
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/orders | Create order |
| GET | /api/orders/{id} | Get order |
| GET | /api/orders/my-orders | Get user orders |
| POST | /api/orders/{id}/cancel | Cancel order |
| GET | /api/orders/admin/all | Get all orders (Admin) |

### Payments
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/payments/checkout | Create checkout |
| GET | /api/payments/status/{orderId} | Get status |
| GET | /api/payments/confirm | Confirm payment |
| POST | /api/payments/webhook | Stripe webhook |

## Sample API Requests

### Register User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "phone": "1234567890"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePass123!"
  }'
```

### Create Product (Admin)
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15 Pro",
    "description": "Latest Apple smartphone",
    "price": 999.99,
    "stockQuantity": 100,
    "category": "Electronics",
    "brand": "Apple",
    "imageUrl": "https://example.com/iphone.jpg"
  }'
```

### Add to Cart
```bash
curl -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 2
  }'
```

### Create Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "shippingAddress": "123 Main St",
    "shippingCity": "New York",
    "shippingState": "NY",
    "shippingZipCode": "10001",
    "shippingCountry": "USA"
  }'
```

## Testing

```bash
# Run all tests
mvn test

# Run with coverage report
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=AuthServiceTest
```

## Project Structure

```
src/main/java/com/shopease/
├── EcommerceApplication.java
├── config/
│   ├── OpenApiConfig.java
│   ├── SecurityConfig.java
│   └── StripeConfig.java
├── controller/
│   ├── AuthController.java
│   ├── CartController.java
│   ├── OrderController.java
│   ├── PaymentController.java
│   └── ProductController.java
├── dto/
│   ├── request/
│   └── response/
├── entity/
│   ├── User.java
│   ├── Product.java
│   ├── Cart.java
│   ├── CartItem.java
│   ├── Order.java
│   └── OrderItem.java
├── exception/
│   └── GlobalExceptionHandler.java
├── mapper/
├── repository/
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── CustomUserDetailsService.java
├── service/
│   ├── impl/
│   └── interfaces/
└── util/
```

## License

MIT License
>>>>>>> main
