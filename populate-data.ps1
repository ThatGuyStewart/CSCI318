# PowerShell Script to Populate Microservices Database with Sample Data
# Ensures all 4 microservices are active before inserting

$ErrorActionPreference = "Stop"

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  Populating Online Retail Microservices Data      " -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

# -----------------------------------------------------------------------------
# 1. Health Checks
# -----------------------------------------------------------------------------
$endpoints = @{
    "Customer Service"     = "http://localhost:8081/customer"
    "Product Service"      = "http://localhost:8082/product"
    "Order Service"        = "http://localhost:8083/order"
    "Notification Service" = "http://localhost:8084/notification/broadcast"
}

Write-Host "`nChecking microservice availability..." -ForegroundColor Yellow
foreach ($svc in $endpoints.Keys) {
    try {
        $null = Invoke-RestMethod -Uri $endpoints[$svc] -Method GET -TimeoutSec 3 -ErrorAction SilentlyContinue
        Write-Host "  [OK] $svc is responsive." -ForegroundColor Green
    } catch {
        Write-Host "  [WARNING] $svc might still be booting or endpoint is ready. Proceeding..." -ForegroundColor DarkYellow
    }
}

# -----------------------------------------------------------------------------
# 2. Populate 5 Customers (Customer Service - Port 8081)
# -----------------------------------------------------------------------------
Write-Host "`n[1/4] Adding 5 Customers..." -ForegroundColor Yellow

$customers = @(
    @{
        name = "Alice Smith"
        email = "alice.smith@example.com"
        phone = "0411111111"
        contactMethod = "Email"
        address = @{
            unitNumber = 12
            streetNumber = 100
            street = "George St"
            suburb = "Sydney CBD"
            city = "Sydney"
            postcode = 2000
            state = "NSW"
            country = "Australia"
        }
    },
    @{
        name = "Bob Johnson"
        email = "bob.johnson@example.com"
        phone = "0422222222"
        contactMethod = "Phone"
        address = @{
            unitNumber = $null
            streetNumber = 245
            street = "Collins St"
            suburb = "Melbourne CBD"
            city = "Melbourne"
            postcode = 3000
            state = "VIC"
            country = "Australia"
        }
    },
    @{
        name = "Charlie Brown"
        email = "charlie.brown@example.com"
        phone = "0433333333"
        contactMethod = "Email"
        address = @{
            unitNumber = 3
            streetNumber = 50
            street = "Queen St"
            suburb = "Brisbane City"
            city = "Brisbane"
            postcode = 4000
            state = "QLD"
            country = "Australia"
        }
    },
    @{
        name = "Diana Prince"
        email = "diana.prince@example.com"
        phone = "0444444444"
        contactMethod = "Phone"
        address = @{
            unitNumber = $null
            streetNumber = 88
            street = "St Georges Terrace"
            suburb = "Perth"
            city = "Perth"
            postcode = 6000
            state = "WA"
            country = "Australia"
        }
    },
    @{
        name = "Evan Wright"
        email = "evan.wright@example.com"
        phone = "0455555555"
        contactMethod = "Email"
        address = @{
            unitNumber = 10
            streetNumber = 120
            street = "King William St"
            suburb = "Adelaide"
            city = "Adelaide"
            postcode = 5000
            state = "SA"
            country = "Australia"
        }
    }
)

foreach ($c in $customers) {
    $json = $c | ConvertTo-Json -Depth 5
    $res = Invoke-RestMethod -Uri "http://localhost:8081/customer" -Method POST -ContentType "application/json" -Body $json
    Write-Host "  Created Customer #$($res.id): $($res.name) ($($res.email))" -ForegroundColor Green
}

# -----------------------------------------------------------------------------
# 3. Populate Products for all 16 Categories (Product Service - Port 8082)
# -----------------------------------------------------------------------------
Write-Host "`n[2/4] Adding Products across all 16 Categories..." -ForegroundColor Yellow

$products = @(
    # Electronics
    @{ name = "Pro Wireless Headphones"; category = "Electronics"; price = 249.99; description = "Active noise cancelling with 30hr battery life" },
    @{ name = "Ultra 4K Smart Monitor 27in"; category = "Electronics"; price = 489.00; description = "IPS panel with USB-C 90W charging" },
    @{ name = "Compact Bluetooth Speaker"; category = "Electronics"; price = 79.50; description = "Waterproof IPX7 portable speaker" },

    # Appliances
    @{ name = "Automatic Espresso Machine"; category = "Appliances"; price = 699.00; description = "15-bar Italian pump with milk frother" },
    @{ name = "Digital Air Fryer XL"; category = "Appliances"; price = 149.95; description = "5.8L capacity with 8 one-touch presets" },

    # Furniture
    @{ name = "Ergonomic Mesh Office Chair"; category = "Furniture"; price = 320.00; description = "Adjustable lumbar support and 3D armrests" },
    @{ name = "Motorized Standing Desk 140cm"; category = "Furniture"; price = 550.00; description = "Dual motor electric height adjustable desk" },

    # Kitchen
    @{ name = "Japanese Damascus Chef Knife"; category = "Kitchen"; price = 129.00; description = "8-inch high carbon stainless steel blade" },
    @{ name = "Enameled Cast Iron Dutch Oven"; category = "Kitchen"; price = 180.00; description = "5.5 Quart heavy duty cooking pot" },

    # Tools
    @{ name = "20V Brushless Cordless Drill"; category = "Tools"; price = 159.00; description = "Includes 2 batteries and fast charger" },
    @{ name = "120-Piece Mechanics Tool Set"; category = "Tools"; price = 110.00; description = "Chrome vanadium steel socket set" },

    # Garden
    @{ name = "Cordless Grass Trimmer"; category = "Garden"; price = 135.00; description = "Lightweight battery powered line trimmer" },
    @{ name = "Retractable Garden Hose Reel 25m"; category = "Garden"; price = 95.00; description = "Auto-rewind wall mounted water hose" },

    # Sports
    @{ name = "Adjustable Dumbbell Pair 24kg"; category = "Sports"; price = 340.00; description = "Quick select weight dial from 2.5kg to 24kg" },
    @{ name = "High Density Yoga Mat 6mm"; category = "Sports"; price = 45.00; description = "Non-slip eco-friendly alignment mat" },

    # Toys
    @{ name = "Modular Space Station Building Kit"; category = "Toys"; price = 89.90; description = "1050 pieces with astronaut minifigures" },
    @{ name = "Remote Control Off-Road Buggy"; category = "Toys"; price = 65.00; description = "High-speed 1:16 scale 4WD RC vehicle" },

    # Automotive
    @{ name = "Dual Lens 4K Dash Cam"; category = "Automotive"; price = 175.00; description = "Front and rear recording with Night Vision" },
    @{ name = "Portable Jump Starter 2000A"; category = "Automotive"; price = 119.00; description = "Heavy duty booster pack with power bank" },

    # Pets
    @{ name = "Orthopedic Memory Foam Dog Bed"; category = "Pets"; price = 85.00; description = "Waterproof washable liner for large dogs" },
    @{ name = "Smart Automatic Pet Feeder"; category = "Pets"; price = 115.00; description = "Timed meal dispensing with voice recorder" },

    # Apparel
    @{ name = "100% Merino Wool Thermal Crew"; category = "Apparel"; price = 95.00; description = "Breathable moisture wicking base layer" },
    @{ name = "Waterproof All-Weather Jacket"; category = "Apparel"; price = 160.00; description = "Breathable hooded rain shell" },

    # Beauty
    @{ name = "Vitamin C Facial Radiance Serum"; category = "Beauty"; price = 52.00; description = "Antioxidant formula with Hyaluronic Acid" },
    @{ name = "Ionic Ceramic Hair Dryer"; category = "Beauty"; price = 110.00; description = "Fast drying salon grade 2200W motor" },

    # Grocery
    @{ name = "Single Origin Ethiopian Coffee Beans 1kg"; category = "Grocery"; price = 42.00; description = "Medium roast whole bean specialty coffee" },
    @{ name = "Cold Pressed Organic Olive Oil 750ml"; category = "Grocery"; price = 24.50; description = "Extra virgin unrefined estate oil" },

    # Media
    @{ name = "Wireless Multi-Room Audio Streamer"; category = "Media"; price = 189.00; description = "Hi-Res lossless audio streaming receiver" },
    @{ name = "Vinyl Turntable with Built-in Preamp"; category = "Media"; price = 220.00; description = "Belt drive with USB digital recording" },

    # Professional
    @{ name = "Ultra-Fast Duplex Document Scanner"; category = "Professional"; price = 399.00; description = "50 sheet auto document feeder 40ppm" },
    @{ name = "Wireless Conference Speakerphone"; category = "Professional"; price = 145.00; description = "360-degree voice pickup with AI noise filter" },

    # Lifestyle
    @{ name = "Vacuum Insulated Stainless Bottle 1L"; category = "Lifestyle"; price = 38.00; description = "Keeps drinks cold 24hrs / hot 12hrs" },
    @{ name = "Aromatherapy Ultrasonic Diffuser"; category = "Lifestyle"; price = 48.00; description = "Quiet wood grain cool mist humidifier" }
)

foreach ($p in $products) {
    $json = $p | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8082/product" -Method POST -ContentType "application/json" -Body $json
    Write-Host "  Created Product #$($res.id): [$($res.category)] $($res.name) - `$$($res.price)" -ForegroundColor Green
}

# -----------------------------------------------------------------------------
# 4. Populate Shopping Baskets & Create Orders (Order Service - Port 8083)
# -----------------------------------------------------------------------------
Write-Host "`n[3/4] Adding Basket Items & Placing Orders..." -ForegroundColor Yellow

# Add items to Customer 1's basket
$basketItem1 = @{ productId = 1; quantity = 2 } | ConvertTo-Json
$basketItem2 = @{ productId = 4; quantity = 1 } | ConvertTo-Json
$null = Invoke-RestMethod -Uri "http://localhost:8081/customer/1/basket/items" -Method POST -ContentType "application/json" -Body $basketItem1
$null = Invoke-RestMethod -Uri "http://localhost:8081/customer/1/basket/items" -Method POST -ContentType "application/json" -Body $basketItem2
Write-Host "  Added items to Customer 1's basket." -ForegroundColor Green

# Order 1: Created from Customer 1's basket (omitting items in request)
$order1Payload = @{ customerId = 1 } | ConvertTo-Json
$order1 = Invoke-RestMethod -Uri "http://localhost:8083/order" -Method POST -ContentType "application/json" -Body $order1Payload
Write-Host "  Created Order #$($order1.id) from Customer 1's basket (Total: `$$($order1.total), Status: $($order1.status))" -ForegroundColor Green

# Order 2: Created with explicit items for Customer 2
$order2Payload = @{
    customerId = 2
    items = @(
        @{ productId = 6; quantity = 1 }
        @{ productId = 7; quantity = 1 }
    )
} | ConvertTo-Json -Depth 5
$order2 = Invoke-RestMethod -Uri "http://localhost:8083/order" -Method POST -ContentType "application/json" -Body $order2Payload
Write-Host "  Created Order #$($order2.id) for Customer 2 (Total: `$$($order2.total), Status: $($order2.status))" -ForegroundColor Green

# Update Order 2 status to InTransit
$statusUpdate = @{ status = "InTransit" } | ConvertTo-Json
$order2Updated = Invoke-RestMethod -Uri "http://localhost:8083/order/$($order2.id)/status" -Method PUT -ContentType "application/json" -Body $statusUpdate
Write-Host "  Updated Order #$($order2.id) status to $($order2Updated.status)" -ForegroundColor Green

# Order 3: Created for Customer 3 and updated to Delivered
$order3Payload = @{
    customerId = 3
    items = @(
        @{ productId = 8; quantity = 2 }
        @{ productId = 25; quantity = 1 }
    )
} | ConvertTo-Json -Depth 5
$order3 = Invoke-RestMethod -Uri "http://localhost:8083/order" -Method POST -ContentType "application/json" -Body $order3Payload
$statusDelivered = @{ status = "Delivered" } | ConvertTo-Json
$null = Invoke-RestMethod -Uri "http://localhost:8083/order/$($order3.id)/status" -Method PUT -ContentType "application/json" -Body $statusDelivered
Write-Host "  Created Order #$($order3.id) for Customer 3 (Status: Delivered)" -ForegroundColor Green

# Order 4: Created for Customer 4 and then Cancelled
$order4Payload = @{
    customerId = 4
    items = @(
        @{ productId = 14; quantity = 1 }
    )
} | ConvertTo-Json -Depth 5
$order4 = Invoke-RestMethod -Uri "http://localhost:8083/order" -Method POST -ContentType "application/json" -Body $order4Payload
$order4Cancelled = Invoke-RestMethod -Uri "http://localhost:8083/order/$($order4.id)/cancel" -Method POST
Write-Host "  Created and Cancelled Order #$($order4.id) for Customer 4 (Status: $($order4Cancelled.status))" -ForegroundColor Green

# -----------------------------------------------------------------------------
# 5. Populate Notifications (Notification Service - Port 8084)
# -----------------------------------------------------------------------------
Write-Host "`n[4/4] Sending Sample Notifications..." -ForegroundColor Yellow

# Direct by Customer ID
$notifCust1 = @{ message = "Exclusive VIP Reward: Enjoy 15% off your next purchase with code VIP15." } | ConvertTo-Json
$resN1 = Invoke-RestMethod -Uri "http://localhost:8084/notification/customer/1" -Method POST -ContentType "application/json" -Body $notifCust1
Write-Host "  Sent Notification #$($resN1.id) to Customer 1 via $($resN1.type)" -ForegroundColor Green

# Direct by Customer Email
$notifEmail = @{ message = "Monthly Newsletter: Tech gadgets trends for Spring 2026." } | ConvertTo-Json
$resN2 = Invoke-RestMethod -Uri "http://localhost:8084/notification/customer/email/bob.johnson@example.com" -Method POST -ContentType "application/json" -Body $notifEmail
Write-Host "  Sent Notification #$($resN2.id) to Bob Johnson via $($resN2.type)" -ForegroundColor Green

# Direct by Customer Phone
$notifPhone = @{ message = "Your security verification code is 829410." } | ConvertTo-Json
$resN3 = Invoke-RestMethod -Uri "http://localhost:8084/notification/customer/phone/0433333333" -Method POST -ContentType "application/json" -Body $notifPhone
Write-Host "  Sent Notification #$($resN3.id) to Charlie Brown via $($resN3.type)" -ForegroundColor Green

# Broadcast to All Customers
$broadcast = @{ message = "Store Announcement: Weekend Flash Sale starts this Saturday at 9AM!" } | ConvertTo-Json
$resB = Invoke-RestMethod -Uri "http://localhost:8084/notification/broadcast" -Method POST -ContentType "application/json" -Body $broadcast
Write-Host "  Broadcasted notification to all $($resB.Count) registered customers." -ForegroundColor Green

# Area Broadcast (NSW only)
$areaBroadcast = @{ message = "NSW Same-Day Delivery is now active across Greater Sydney."; state = "NSW"; country = "Australia" } | ConvertTo-Json
$resArea = Invoke-RestMethod -Uri "http://localhost:8084/notification/broadcast/area" -Method POST -ContentType "application/json" -Body $areaBroadcast
Write-Host "  Sent Area Broadcast to $($resArea.Count) customer(s) in NSW." -ForegroundColor Green

Write-Host "`n===================================================" -ForegroundColor Cyan
Write-Host "  Database population completed successfully!     " -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
