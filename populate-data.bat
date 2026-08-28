@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo   Populating Online Retail Microservices Data (cURL)
echo ===================================================
echo.

:: -----------------------------------------------------------------------------
:: 1. Add 5 Customers (Customer Service - Port 8081)
:: -----------------------------------------------------------------------------
echo [1/4] Adding 5 Customers...

echo Adding Alice Smith...
curl -s -X POST http://localhost:8081/customer -H "Content-Type: application/json" -d "{\"name\":\"Alice Smith\",\"email\":\"alice.smith@example.com\",\"phone\":\"0411111111\",\"contactMethod\":\"Email\",\"address\":{\"unitNumber\":12,\"streetNumber\":100,\"street\":\"George St\",\"suburb\":\"Sydney CBD\",\"city\":\"Sydney\",\"postcode\":2000,\"state\":\"NSW\",\"country\":\"Australia\"}}"
echo.

echo Adding Bob Johnson...
curl -s -X POST http://localhost:8081/customer -H "Content-Type: application/json" -d "{\"name\":\"Bob Johnson\",\"email\":\"bob.johnson@example.com\",\"phone\":\"0422222222\",\"contactMethod\":\"Phone\",\"address\":{\"streetNumber\":245,\"street\":\"Collins St\",\"suburb\":\"Melbourne CBD\",\"city\":\"Melbourne\",\"postcode\":3000,\"state\":\"VIC\",\"country\":\"Australia\"}}"
echo.

echo Adding Charlie Brown...
curl -s -X POST http://localhost:8081/customer -H "Content-Type: application/json" -d "{\"name\":\"Charlie Brown\",\"email\":\"charlie.brown@example.com\",\"phone\":\"0433333333\",\"contactMethod\":\"Email\",\"address\":{\"unitNumber\":3,\"streetNumber\":50,\"street\":\"Queen St\",\"suburb\":\"Brisbane City\",\"city\":\"Brisbane\",\"postcode\":4000,\"state\":\"QLD\",\"country\":\"Australia\"}}"
echo.

echo Adding Diana Prince...
curl -s -X POST http://localhost:8081/customer -H "Content-Type: application/json" -d "{\"name\":\"Diana Prince\",\"email\":\"diana.prince@example.com\",\"phone\":\"0444444444\",\"contactMethod\":\"Phone\",\"address\":{\"streetNumber\":88,\"street\":\"St Georges Terrace\",\"suburb\":\"Perth\",\"city\":\"Perth\",\"postcode\":6000,\"state\":\"WA\",\"country\":\"Australia\"}}"
echo.

echo Adding Evan Wright...
curl -s -X POST http://localhost:8081/customer -H "Content-Type: application/json" -d "{\"name\":\"Evan Wright\",\"email\":\"evan.wright@example.com\",\"phone\":\"0455555555\",\"contactMethod\":\"Email\",\"address\":{\"unitNumber\":10,\"streetNumber\":120,\"street\":\"King William St\",\"suburb\":\"Adelaide\",\"city\":\"Adelaide\",\"postcode\":5000,\"state\":\"SA\",\"country\":\"Australia\"}}"
echo.

:: -----------------------------------------------------------------------------
:: 2. Add Products for all 16 Categories (Product Service - Port 8082)
:: -----------------------------------------------------------------------------
echo.
echo [2/4] Adding Products across all 16 Categories...

:: Electronics
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Pro Wireless Headphones\",\"category\":\"Electronics\",\"price\":249.99,\"description\":\"Active noise cancelling with 30hr battery life\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Ultra 4K Smart Monitor 27in\",\"category\":\"Electronics\",\"price\":489.00,\"description\":\"IPS panel with USB-C 90W charging\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Compact Bluetooth Speaker\",\"category\":\"Electronics\",\"price\":79.50,\"description\":\"Waterproof IPX7 portable speaker\"}"
echo.

:: Appliances
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Automatic Espresso Machine\",\"category\":\"Appliances\",\"price\":699.00,\"description\":\"15-bar Italian pump with milk frother\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Digital Air Fryer XL\",\"category\":\"Appliances\",\"price\":149.95,\"description\":\"5.8L capacity with 8 presets\"}"
echo.

:: Furniture
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Ergonomic Mesh Office Chair\",\"category\":\"Furniture\",\"price\":320.00,\"description\":\"Adjustable lumbar support and 3D armrests\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Motorized Standing Desk 140cm\",\"category\":\"Furniture\",\"price\":550.00,\"description\":\"Dual motor electric height adjustable desk\"}"
echo.

:: Kitchen
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Japanese Damascus Chef Knife\",\"category\":\"Kitchen\",\"price\":129.00,\"description\":\"8-inch high carbon stainless steel blade\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Enameled Cast Iron Dutch Oven\",\"category\":\"Kitchen\",\"price\":180.00,\"description\":\"5.5 Quart heavy duty cooking pot\"}"
echo.

:: Tools
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"20V Brushless Cordless Drill\",\"category\":\"Tools\",\"price\":159.00,\"description\":\"Includes 2 batteries and fast charger\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"120-Piece Mechanics Tool Set\",\"category\":\"Tools\",\"price\":110.00,\"description\":\"Chrome vanadium steel socket set\"}"
echo.

:: Garden
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Cordless Grass Trimmer\",\"category\":\"Garden\",\"price\":135.00,\"description\":\"Lightweight battery powered line trimmer\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Retractable Garden Hose Reel 25m\",\"category\":\"Garden\",\"price\":95.00,\"description\":\"Auto-rewind wall mounted water hose\"}"
echo.

:: Sports
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Adjustable Dumbbell Pair 24kg\",\"category\":\"Sports\",\"price\":340.00,\"description\":\"Quick select weight dial from 2.5kg to 24kg\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"High Density Yoga Mat 6mm\",\"category\":\"Sports\",\"price\":45.00,\"description\":\"Non-slip eco-friendly alignment mat\"}"
echo.

:: Toys
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Modular Space Station Building Kit\",\"category\":\"Toys\",\"price\":89.90,\"description\":\"1050 pieces with astronaut minifigures\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Remote Control Off-Road Buggy\",\"category\":\"Toys\",\"price\":65.00,\"description\":\"High-speed 1:16 scale 4WD RC vehicle\"}"
echo.

:: Automotive
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Dual Lens 4K Dash Cam\",\"category\":\"Automotive\",\"price\":175.00,\"description\":\"Front and rear recording with Night Vision\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Portable Jump Starter 2000A\",\"category\":\"Automotive\",\"price\":119.00,\"description\":\"Heavy duty booster pack with power bank\"}"
echo.

:: Pets
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Orthopedic Memory Foam Dog Bed\",\"category\":\"Pets\",\"price\":85.00,\"description\":\"Waterproof washable liner for large dogs\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Smart Automatic Pet Feeder\",\"category\":\"Pets\",\"price\":115.00,\"description\":\"Timed meal dispensing with voice recorder\"}"
echo.

:: Apparel
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"100% Merino Wool Thermal Crew\",\"category\":\"Apparel\",\"price\":95.00,\"description\":\"Breathable moisture wicking base layer\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Waterproof All-Weather Jacket\",\"category\":\"Apparel\",\"price\":160.00,\"description\":\"Breathable hooded rain shell\"}"
echo.

:: Beauty
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Vitamin C Facial Radiance Serum\",\"category\":\"Beauty\",\"price\":52.00,\"description\":\"Antioxidant formula with Hyaluronic Acid\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Ionic Ceramic Hair Dryer\",\"category\":\"Beauty\",\"price\":110.00,\"description\":\"Fast drying salon grade 2200W motor\"}"
echo.

:: Grocery
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Single Origin Ethiopian Coffee Beans 1kg\",\"category\":\"Grocery\",\"price\":42.00,\"description\":\"Medium roast whole bean specialty coffee\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Cold Pressed Organic Olive Oil 750ml\",\"category\":\"Grocery\",\"price\":24.50,\"description\":\"Extra virgin unrefined estate oil\"}"
echo.

:: Media
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Wireless Multi-Room Audio Streamer\",\"category\":\"Media\",\"price\":189.00,\"description\":\"Hi-Res lossless audio streaming receiver\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Vinyl Turntable with Built-in Preamp\",\"category\":\"Media\",\"price\":220.00,\"description\":\"Belt drive with USB digital recording\"}"
echo.

:: Professional
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Ultra-Fast Duplex Document Scanner\",\"category\":\"Professional\",\"price\":399.00,\"description\":\"50 sheet auto document feeder 40ppm\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Wireless Conference Speakerphone\",\"category\":\"Professional\",\"price\":145.00,\"description\":\"360-degree voice pickup with AI noise filter\"}"
echo.

:: Lifestyle
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Vacuum Insulated Stainless Bottle 1L\",\"category\":\"Lifestyle\",\"price\":38.00,\"description\":\"Keeps drinks cold 24hrs / hot 12hrs\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Aromatherapy Ultrasonic Diffuser\",\"category\":\"Lifestyle\",\"price\":48.00,\"description\":\"Quiet wood grain cool mist humidifier\"}"
echo.

:: -----------------------------------------------------------------------------
:: 3. Populate Shopping Basket & Create Orders (Order Service - Port 8083)
:: -----------------------------------------------------------------------------
echo.
echo [3/4] Adding Basket Items & Placing Orders...

:: Add products to Customer 1's basket
curl -s -X POST http://localhost:8081/customer/1/basket/items -H "Content-Type: application/json" -d "{\"productId\":1,\"quantity\":2}"
echo.
curl -s -X POST http://localhost:8081/customer/1/basket/items -H "Content-Type: application/json" -d "{\"productId\":4,\"quantity\":1}"
echo.

:: Order 1: From Customer 1's basket
echo Placing Order 1 from Customer 1's Basket...
curl -s -X POST http://localhost:8083/order -H "Content-Type: application/json" -d "{\"customerId\":1}"
echo.

:: Order 2: Explicit items for Customer 2
echo Placing Order 2 for Customer 2...
curl -s -X POST http://localhost:8083/order -H "Content-Type: application/json" -d "{\"customerId\":2,\"items\":[{\"productId\":6,\"quantity\":1},{\"productId\":7,\"quantity\":1}]}"
echo.

:: Update Order 2 status to InTransit
echo Updating Order 2 status to InTransit...
curl -s -X PUT http://localhost:8083/order/2/status -H "Content-Type: application/json" -d "{\"status\":\"InTransit\"}"
echo.

:: Order 3: Explicit items for Customer 3
echo Placing Order 3 for Customer 3...
curl -s -X POST http://localhost:8083/order -H "Content-Type: application/json" -d "{\"customerId\":3,\"items\":[{\"productId\":8,\"quantity\":2},{\"productId\":25,\"quantity\":1}]}"
echo.

:: Update Order 3 status to Delivered
echo Updating Order 3 status to Delivered...
curl -s -X PUT http://localhost:8083/order/3/status -H "Content-Type: application/json" -d "{\"status\":\"Delivered\"}"
echo.

:: Order 4: Explicit items for Customer 4 and then Cancel
echo Placing Order 4 for Customer 4...
curl -s -X POST http://localhost:8083/order -H "Content-Type: application/json" -d "{\"customerId\":4,\"items\":[{\"productId\":14,\"quantity\":1}]}"
echo.
echo Cancelling Order 4...
curl -s -X POST http://localhost:8083/order/4/cancel
echo.

:: -----------------------------------------------------------------------------
:: 4. Populate Notifications (Notification Service - Port 8084)
:: -----------------------------------------------------------------------------
echo.
echo [4/4] Sending Sample Notifications...

:: Direct notification by Customer ID
echo Sending notification to Customer 1 by ID...
curl -s -X POST http://localhost:8084/notification/customer/1 -H "Content-Type: application/json" -d "{\"message\":\"Exclusive VIP Reward: Enjoy 15%% off your next purchase with code VIP15.\"}"
echo.

:: Direct notification by Email
echo Sending notification to Bob Johnson by Email...
curl -s -X POST http://localhost:8084/notification/customer/email/bob.johnson@example.com -H "Content-Type: application/json" -d "{\"message\":\"Monthly Newsletter: Tech gadgets trends for Spring 2026.\"}"
echo.

:: Direct notification by Phone
echo Sending notification to Charlie Brown by Phone...
curl -s -X POST http://localhost:8084/notification/customer/phone/0433333333 -H "Content-Type: application/json" -d "{\"message\":\"Your security verification code is 829410.\"}"
echo.

:: Broadcast to All Customers
echo Broadcasting notification to all customers...
curl -s -X POST http://localhost:8084/notification/broadcast -H "Content-Type: application/json" -d "{\"message\":\"Store Announcement: Weekend Flash Sale starts this Saturday at 9AM!\"}"
echo.

:: Area Broadcast (NSW)
echo Broadcasting notification to NSW customers...
curl -s -X POST http://localhost:8084/notification/broadcast/area -H "Content-Type: application/json" -d "{\"message\":\"NSW Same-Day Delivery is now active across Greater Sydney.\",\"state\":\"NSW\",\"country\":\"Australia\"}"
echo.

echo.
echo ===================================================
echo   Database population completed successfully!
echo ===================================================
pause
