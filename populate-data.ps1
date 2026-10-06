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
    "Order Service"        = "http://localhost:8083/order/event"
    "Notification Service" = "http://localhost:8084/notification"
}

Write-Host "`nChecking microservice availability..." -ForegroundColor Yellow
foreach ($svc in $endpoints.Keys) {
    try {
        $null = Invoke-RestMethod -Uri $endpoints[$svc] -Method GET -TimeoutSec 3 -ErrorAction SilentlyContinue
        Write-Host "  [OK] $svc is responsive." -ForegroundColor Green
    } catch {
        Write-Host "  [WARNING] $svc might still be booting or endpoint isn't ready. Proceeding..." -ForegroundColor DarkYellow
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
    Write-Host "  Created Customer #$($res.customerId): $($res.name) ($($res.email))" -ForegroundColor Green
}

# -----------------------------------------------------------------------------
# 3. Populate Products for all 16 Categories (Product Service - Port 8082)
# -----------------------------------------------------------------------------
Write-Host "`n[2/4] Adding Products across all 16 Categories..." -ForegroundColor Yellow

$products = @(
    # Electronics (10)
    @{ name = "Pro Wireless Headphones"; category = "Electronics"; price = 249.99; description = "Active noise cancelling with 30hr battery life" },
    @{ name = "Ultra 4K Smart Monitor 27in"; category = "Electronics"; price = 489.00; description = "IPS panel with USB-C 90W charging" },
    @{ name = "Compact Bluetooth Speaker"; category = "Electronics"; price = 79.50; description = "Waterproof IPX7 portable speaker" },
    @{ name = "Noise Cancelling Earbuds"; category = "Electronics"; price = 159.00; description = "True wireless earbuds with charging case" },
    @{ name = "Portable Power Bank 20000mAh"; category = "Electronics"; price = 69.00; description = "Fast charge USB-C / USB-A" },
    @{ name = "Smart Home Assistant"; category = "Electronics"; price = 129.00; description = "Voice assistant with smart home control" },
    @{ name = "27in Curved Gaming Monitor"; category = "Electronics"; price = 399.00; description = "144Hz VA panel with G-Sync compatible" },
    @{ name = "USB-C Multiport Dock"; category = "Electronics"; price = 99.00; description = "4K HDMI, Ethernet, USB-A, PD passthrough" },
    @{ name = "Action Camera 4K"; category = "Electronics"; price = 189.00; description = "Waterproof action cam with image stabilization" },
    @{ name = "Wireless Charging Pad"; category = "Electronics"; price = 39.00; description = "Fast Qi wireless charger pad" },

    # Appliances (10)
    @{ name = "Automatic Espresso Machine"; category = "Appliances"; price = 699.00; description = "15-bar Italian pump with milk frother" },
    @{ name = "Digital Air Fryer XL"; category = "Appliances"; price = 149.95; description = "5.8L capacity with 8 one-touch presets" },
    @{ name = "Robotic Vacuum Cleaner"; category = "Appliances"; price = 399.00; description = "Smart mapping and app control" },
    @{ name = "Smart Refrigerator Water Filter"; category = "Appliances"; price = 59.00; description = "Replacement filter for smart fridges" },
    @{ name = "Front Load Washing Machine"; category = "Appliances"; price = 799.00; description = "8kg smart front load with steam" },
    @{ name = "Convection Microwave Oven"; category = "Appliances"; price = 249.00; description = "900W with grill and convection" },
    @{ name = "Induction Cooktop Portable"; category = "Appliances"; price = 119.00; description = "Single zone induction with timer" },
    @{ name = "Upright Freezer 300L"; category = "Appliances"; price = 599.00; description = "Energy efficient chest/freezer" },
    @{ name = "Smart Thermostat"; category = "Appliances"; price = 199.00; description = "Wi-Fi programmable thermostat" },
    @{ name = "Clothes Steamer Handheld"; category = "Appliances"; price = 49.00; description = "Portable garment steamer" },

    # Furniture (10)
    @{ name = "Ergonomic Mesh Office Chair"; category = "Furniture"; price = 320.00; description = "Adjustable lumbar support and 3D armrests" },
    @{ name = "Motorized Standing Desk 140cm"; category = "Furniture"; price = 550.00; description = "Dual motor electric height adjustable desk" },
    @{ name = "3-Seater Fabric Sofa"; category = "Furniture"; price = 899.00; description = "Modern mid-century fabric sofa" },
    @{ name = "Solid Oak Coffee Table"; category = "Furniture"; price = 249.00; description = "Handcrafted solid wood table" },
    @{ name = "5-Shelf Bookshelf"; category = "Furniture"; price = 129.00; description = "Industrial style shelving unit" },
    @{ name = "Bedside Table with Drawer"; category = "Furniture"; price = 79.00; description = "Compact bedside storage unit" },
    @{ name = "Queen Mattress Medium Firm"; category = "Furniture"; price = 749.00; description = "Hybrid spring and memory foam" },
    @{ name = "Entertainment TV Unit 180cm"; category = "Furniture"; price = 399.00; description = "Low profile media console" },
    @{ name = "Set of 2 Bar Stools"; category = "Furniture"; price = 159.00; description = "Adjustable height bar stools" },
    @{ name = "6-Seater Dining Table Set"; category = "Furniture"; price = 699.00; description = "Dining table with 6 chairs" },

    # Kitchen (10)
    @{ name = "Japanese Damascus Chef Knife"; category = "Kitchen"; price = 129.00; description = "8-inch high carbon stainless steel blade" },
    @{ name = "Enameled Cast Iron Dutch Oven"; category = "Kitchen"; price = 180.00; description = "5.5 Quart heavy duty cooking pot" },
    @{ name = "Non-Stick Pan Set 3pc"; category = "Kitchen"; price = 89.00; description = "PFOA-free non-stick frying pans" },
    @{ name = "High-Speed Blender"; category = "Kitchen"; price = 149.00; description = "Smoothie and soup prep blender" },
    @{ name = "Food Processor 8cup"; category = "Kitchen"; price = 129.00; description = "Multi-function chopping and dough" },
    @{ name = "Stand Mixer 4.5L"; category = "Kitchen"; price = 299.00; description = "Planetary mixer with attachments" },
    @{ name = "Premium Bamboo Cutting Board Set"; category = "Kitchen"; price = 39.00; description = "3-piece board set with juice groove" },
    @{ name = "Knife Sharpener 3-Stage"; category = "Kitchen"; price = 29.00; description = "Electric sharpener for kitchen knives" },
    @{ name = "Stainless Measuring Set"; category = "Kitchen"; price = 19.00; description = "Measuring cups and spoons set" },
    @{ name = "Wall-Mounted Spice Rack"; category = "Kitchen"; price = 49.00; description = "12-jar spice organizer" },

    # Tools (10)
    @{ name = "20V Brushless Cordless Drill"; category = "Tools"; price = 159.00; description = "Includes 2 batteries and fast charger" },
    @{ name = "120-Piece Mechanics Tool Set"; category = "Tools"; price = 110.00; description = "Chrome vanadium steel socket set" },
    @{ name = "Circular Saw 7-1/4in"; category = "Tools"; price = 139.00; description = "High torque cutting saw" },
    @{ name = "Cordless Impact Driver"; category = "Tools"; price = 129.00; description = "Compact impact for screws and bolts" },
    @{ name = "Multi-Tool Oscillating"; category = "Tools"; price = 99.00; description = "Versatile cutting and sanding tool" },
    @{ name = "25m Tape Measure"; category = "Tools"; price = 24.00; description = "Auto-lock measuring tape" },
    @{ name = "Angle Grinder 125mm"; category = "Tools"; price = 79.00; description = "Cutting and grinding tool" },
    @{ name = "3m Aluminium Ladder"; category = "Tools"; price = 149.00; description = "Lightweight folding ladder" },
    @{ name = "Orbital Sander"; category = "Tools"; price = 59.00; description = "Random orbital finishing sander" },
    @{ name = "LED Work Light"; category = "Tools"; price = 39.00; description = "Portable rechargeable floodlight" },

    # Garden (10)
    @{ name = "Cordless Grass Trimmer"; category = "Garden"; price = 135.00; description = "Lightweight battery powered line trimmer" },
    @{ name = "Retractable Garden Hose Reel 25m"; category = "Garden"; price = 95.00; description = "Auto-rewind wall mounted water hose" },
    @{ name = "Electric Lawn Mower 40V"; category = "Garden"; price = 499.00; description = "Cordless mower with 40L catcher" },
    @{ name = "Garden Kneeler and Seat"; category = "Garden"; price = 39.00; description = "Foam kneeler with tool pouches" },
    @{ name = "Compost Bin 200L"; category = "Garden"; price = 89.00; description = "Rotating outdoor compost bin" },
    @{ name = "Pruning Shears Bypass"; category = "Garden"; price = 29.00; description = "Ergonomic cutting shears" },
    @{ name = "Patio Heater 3kW"; category = "Garden"; price = 179.00; description = "Gas patio heater with safety tilt" },
    @{ name = "Weatherproof Bird Feeder"; category = "Garden"; price = 24.00; description = "Hanging seed feeder" },
    @{ name = "Soil pH Meter"; category = "Garden"; price = 19.00; description = "Digital soil tester" },
    @{ name = "Set of 4 Outdoor Planters"; category = "Garden"; price = 69.00; description = "Durable resin planter set" },

    # Sports (10)
    @{ name = "Adjustable Dumbbell Pair 24kg"; category = "Sports"; price = 340.00; description = "Quick select weight dial from 2.5kg to 24kg" },
    @{ name = "High Density Yoga Mat 6mm"; category = "Sports"; price = 45.00; description = "Non-slip eco-friendly alignment mat" },
    @{ name = "Folding Treadmill"; category = "Sports"; price = 799.00; description = "Compact foldable treadmill with incline" },
    @{ name = "Official Size Soccer Ball"; category = "Sports"; price = 29.00; description = "Durable stitched training ball" },
    @{ name = "Pro Basketball"; category = "Sports"; price = 39.00; description = "Indoor/outdoor composite leather" },
    @{ name = "Tennis Racket Composite"; category = "Sports"; price = 89.00; description = "Lightweight graphite frame" },
    @{ name = "Running Shoes (Unisex)"; category = "Sports"; price = 129.00; description = "Cushioned road running shoe" },
    @{ name = "Swim Goggles Anti-Fog"; category = "Sports"; price = 19.00; description = "Comfort seal for long swims" },
    @{ name = "Indoor Exercise Bike"; category = "Sports"; price = 499.00; description = "Magnetic resistance stationary bike" },
    @{ name = "Resistance Bands Set"; category = "Sports"; price = 29.00; description = "5-level resistance loop bands" },

    # Toys (10)
    @{ name = "Modular Space Station Building Kit"; category = "Toys"; price = 89.90; description = "1050 pieces with astronaut minifigures" },
    @{ name = "Remote Control Off-Road Buggy"; category = "Toys"; price = 65.00; description = "High-speed 1:16 scale 4WD RC vehicle" },
    @{ name = "1000pc Jigsaw Puzzle"; category = "Toys"; price = 24.00; description = "Scenic landscape 1000-piece puzzle" },
    @{ name = "Strategic Board Game"; category = "Toys"; price = 49.00; description = "Family strategy board game for 2-6 players" },
    @{ name = "Educational Coding Robot"; category = "Toys"; price = 129.00; description = "STEM robot for kids with coding app" },
    @{ name = "Plush Teddy Bear Large"; category = "Toys"; price = 29.00; description = "Soft stuffed animal plush toy" },
    @{ name = "Doll Stroller"; category = "Toys"; price = 39.00; description = "Foldable stroller for dolls" },
    @{ name = "Wooden Train Set"; category = "Toys"; price = 59.00; description = "Classic wooden track with carriages" },
    @{ name = "Kids Art & Craft Set"; category = "Toys"; price = 34.00; description = "Paints, crayons, and paper set" },
    @{ name = "Science Lab Kit"; category = "Toys"; price = 44.00; description = "Beginner experiments with safe reagents" },

    # Automotive (10)
    @{ name = "Dual Lens 4K Dash Cam"; category = "Automotive"; price = 175.00; description = "Front and rear recording with Night Vision" },
    @{ name = "Portable Jump Starter 2000A"; category = "Automotive"; price = 119.00; description = "Heavy duty booster pack with power bank" },
    @{ name = "Electric Tyre Inflator"; category = "Automotive"; price = 49.00; description = "Cordless tyre pump with gauge" },
    @{ name = "12V Car Vacuum Cleaner"; category = "Automotive"; price = 39.00; description = "Portable hand vacuum for cars" },
    @{ name = "Universal Roof Rack"; category = "Automotive"; price = 199.00; description = "Crossbar set for most vehicles" },
    @{ name = "All-Weather Car Cover"; category = "Automotive"; price = 59.00; description = "Protective cover for sedans" },
    @{ name = "Seat Covers Full Set"; category = "Automotive"; price = 89.00; description = "Universal fit seat cover set" },
    @{ name = "Bluetooth OBD2 Adapter"; category = "Automotive"; price = 29.00; description = "Scan car diagnostics via app" },
    @{ name = "Portable GPS Navigator"; category = "Automotive"; price = 129.00; description = "7-inch GPS with lifetime maps" },
    @{ name = "Engine Coolant 4L"; category = "Automotive"; price = 29.00; description = "All-season antifreeze and coolant" },

    # Pets (10)
    @{ name = "Orthopedic Memory Foam Dog Bed"; category = "Pets"; price = 85.00; description = "Waterproof washable liner for large dogs" },
    @{ name = "Smart Automatic Pet Feeder"; category = "Pets"; price = 115.00; description = "Timed meal dispensing with voice recorder" },
    @{ name = "3-Level Cat Tree"; category = "Pets"; price = 129.00; description = "Scratching post with perches" },
    @{ name = "Hard Shell Pet Carrier"; category = "Pets"; price = 59.00; description = "Airline approved travel carrier" },
    @{ name = "LED Dog Collar"; category = "Pets"; price = 19.00; description = "Rechargeable light-up collar for night walks" },
    @{ name = "Pet Grooming Kit"; category = "Pets"; price = 39.00; description = "Brush, clippers, nail trimmer set" },
    @{ name = "Shampoo for Pets 500ml"; category = "Pets"; price = 14.00; description = "Hypoallergenic pet shampoo" },
    @{ name = "Dental Chews for Dogs"; category = "Pets"; price = 12.00; description = "Pack of 30 dental sticks" },
    @{ name = "Training Clicker"; category = "Pets"; price = 6.00; description = "Handheld dog training clicker" },
    @{ name = "Aquarium Filter 200L"; category = "Pets"; price = 79.00; description = "External canister filter for aquariums" },

    # Apparel (10)
    @{ name = "100% Merino Wool Thermal Crew"; category = "Apparel"; price = 95.00; description = "Breathable moisture wicking base layer" },
    @{ name = "Waterproof All-Weather Jacket"; category = "Apparel"; price = 160.00; description = "Breathable hooded rain shell" },
    @{ name = "Organic Cotton T-Shirt"; category = "Apparel"; price = 29.00; description = "Soft crew neck tee" },
    @{ name = "Slim Chino Pants"; category = "Apparel"; price = 59.00; description = "Smart casual chinos" },
    @{ name = "Running Shorts"; category = "Apparel"; price = 34.00; description = "Lightweight mesh running shorts" },
    @{ name = "Leather Belt"; category = "Apparel"; price = 39.00; description = "Genuine leather dress belt" },
    @{ name = "Wool Scarf"; category = "Apparel"; price = 49.00; description = "Warm winter scarf" },
    @{ name = "Everyday Sneakers"; category = "Apparel"; price = 89.00; description = "Casual lace-up sneakers" },
    @{ name = "Mens Swim Trunks"; category = "Apparel"; price = 39.00; description = "Quick-dry swim shorts" },
    @{ name = "Knitted Beanie"; category = "Apparel"; price = 19.00; description = "Warm stretch beanie hat" },

    # Beauty (10)
    @{ name = "Vitamin C Facial Radiance Serum"; category = "Beauty"; price = 52.00; description = "Antioxidant formula with Hyaluronic Acid" },
    @{ name = "Ionic Ceramic Hair Dryer"; category = "Beauty"; price = 110.00; description = "Fast drying salon grade 2200W motor" },
    @{ name = "Gentle Foaming Cleanser"; category = "Beauty"; price = 24.00; description = "Daily cleanser for all skin types" },
    @{ name = "Overnight Repair Night Cream"; category = "Beauty"; price = 59.00; description = "Restorative night-time moisturizer" },
    @{ name = "Sunscreen SPF50"; category = "Beauty"; price = 22.00; description = "Broad spectrum UV protection" },
    @{ name = "Volumizing Mascara"; category = "Beauty"; price = 19.00; description = "Smudge-proof lash volumizer" },
    @{ name = "Electric Toothbrush"; category = "Beauty"; price = 69.00; description = "Rechargeable toothbrush with timer" },
    @{ name = "Sheet Mask Set 5pc"; category = "Beauty"; price = 15.00; description = "Hydrating single-use masks" },
    @{ name = "Repair Hand Cream"; category = "Beauty"; price = 12.00; description = "Nourishing hand cream with shea" },
    @{ name = "Nail Care Kit"; category = "Beauty"; price = 18.00; description = "Files, clippers and polish" },

    # Grocery (20)
    @{ name = "Single Origin Ethiopian Coffee Beans 1kg"; category = "Grocery"; price = 42.00; description = "Medium roast whole bean specialty coffee" },
    @{ name = "Cold Pressed Organic Olive Oil 750ml"; category = "Grocery"; price = 24.50; description = "Extra virgin unrefined estate oil" },
    @{ name = "Fresh Milk 1L"; category = "Grocery"; price = 2.50; description = "Full cream fresh milk" },
    @{ name = "Free-Range Eggs 12 Pack"; category = "Grocery"; price = 6.50; description = "Large free-range eggs" },
    @{ name = "Brown Rice 1kg"; category = "Grocery"; price = 4.20; description = "Wholegrain brown rice" },
    @{ name = "Pasta Penne 500g"; category = "Grocery"; price = 1.80; description = "Durum wheat semolina pasta" },
    @{ name = "Tomato Pasta Sauce 700g"; category = "Grocery"; price = 3.00; description = "Rich Italian tomato sauce" },
    @{ name = "Canned Tuna in Springwater 185g"; category = "Grocery"; price = 1.90; description = "Sustainably sourced tuna" },
    @{ name = "Organic Granola 500g"; category = "Grocery"; price = 6.00; description = "Honey and oat granola" },
    @{ name = "Crunchy Peanut Butter 500g"; category = "Grocery"; price = 5.50; description = "No added sugar peanut butter" },
    @{ name = "Pure Australian Honey 350g"; category = "Grocery"; price = 8.00; description = "Raw multifloral honey" },
    @{ name = "White Sugar 1kg"; category = "Grocery"; price = 1.60; description = "Refined white sugar" },
    @{ name = "Plain Flour 1kg"; category = "Grocery"; price = 1.40; description = "All-purpose plain flour" },
    @{ name = "Black Tea 100g"; category = "Grocery"; price = 3.20; description = "Blend of Ceylon and Assam" },
    @{ name = "Sparkling Water 6 Pack 500ml"; category = "Grocery"; price = 5.00; description = "Lemon lime sparkling water" },
    @{ name = "Butter Salted 500g"; category = "Grocery"; price = 4.50; description = "Creamery salted butter" },
    @{ name = "Cheddar Cheese 250g"; category = "Grocery"; price = 5.50; description = "Mature cheddar block" },
    @{ name = "Bananas Bunch (1kg)"; category = "Grocery"; price = 3.00; description = "Fresh ripe bananas" },
    @{ name = "Gala Apples 1kg"; category = "Grocery"; price = 4.00; description = "Crisp fresh apples" },
    @{ name = "Raw Almonds 500g"; category = "Grocery"; price = 9.00; description = "Unsalted roasted almonds" },

    # Media (10)
    @{ name = "Wireless Multi-Room Audio Streamer"; category = "Media"; price = 189.00; description = "Hi-Res lossless audio streaming receiver" },
    @{ name = "Vinyl Turntable with Built-in Preamp"; category = "Media"; price = 220.00; description = "Belt drive with USB digital recording" },
    @{ name = "4K Blu-ray Player"; category = "Media"; price = 149.00; description = "HDR compatible Blu-ray player" },
    @{ name = "E-Reader 8GB"; category = "Media"; price = 129.00; description = "Glare-free e-ink display" },
    @{ name = "Classic Novels Box Set"; category = "Media"; price = 59.00; description = "10-book hardcover collection" },
    @{ name = "Projector Mini LED"; category = "Media"; price = 249.00; description = "Portable home cinema projector" },
    @{ name = "Streaming Stick 4K"; category = "Media"; price = 79.00; description = "Voice remote included" },
    @{ name = "Gaming Controller"; category = "Media"; price = 69.00; description = "Wireless Bluetooth gamepad" },
    @{ name = "Noise Isolating Headphones"; category = "Media"; price = 119.00; description = "Over-ear studio monitor headphones" },
    @{ name = "Portable Bluetooth Radio"; category = "Media"; price = 59.00; description = "DAB+ and FM radio with Bluetooth" },

    # Professional (10)
    @{ name = "Ultra-Fast Duplex Document Scanner"; category = "Professional"; price = 399.00; description = "50 sheet auto document feeder 40ppm" },
    @{ name = "Wireless Conference Speakerphone"; category = "Professional"; price = 145.00; description = "360-degree voice pickup with AI noise filter" },
    @{ name = "Ergonomic Keyboard"; category = "Professional"; price = 89.00; description = "Split ergonomic keyboard" },
    @{ name = "Business Laser Projector"; category = "Professional"; price = 999.00; description = "High-lumen projector for meeting rooms" },
    @{ name = "Monitor Arm Dual"; category = "Professional"; price = 129.00; description = "Adjustable dual monitor arm" },
    @{ name = "Label Printer"; category = "Professional"; price = 99.00; description = "Thermal label printer for shipping" },
    @{ name = "Network Attached Storage 2TB"; category = "Professional"; price = 249.00; description = "Backup NAS for small offices" },
    @{ name = "Mesh WiFi Pro"; category = "Professional"; price = 299.00; description = "Tri-band mesh WiFi system" },
    @{ name = "Time Clock Terminal"; category = "Professional"; price = 349.00; description = "Employee punch-in terminal" },
    @{ name = "Office Laminator"; category = "Professional"; price = 49.00; description = "A4 pouch laminator" },

    # Lifestyle (10)
    @{ name = "Vacuum Insulated Stainless Bottle 1L"; category = "Lifestyle"; price = 38.00; description = "Keeps drinks cold 24hrs / hot 12hrs" },
    @{ name = "Aromatherapy Ultrasonic Diffuser"; category = "Lifestyle"; price = 48.00; description = "Quiet wood grain cool mist humidifier" },
    @{ name = "Yoga Block"; category = "Lifestyle"; price = 14.00; description = "EVA foam yoga support block" },
    @{ name = "Universal Travel Adapter"; category = "Lifestyle"; price = 24.00; description = "All-in-one international adapter" },
    @{ name = "Picnic Blanket Large"; category = "Lifestyle"; price = 29.00; description = "Waterproof back picnic blanket" },
    @{ name = "Hammock Single"; category = "Lifestyle"; price = 34.00; description = "Portable camping hammock" },
    @{ name = "Reusable Shopping Bag Set"; category = "Lifestyle"; price = 12.00; description = "Foldable shopping bags 5-pack" },
    @{ name = "Smartwatch Replacement Band"; category = "Lifestyle"; price = 19.00; description = "Silicone band for popular smartwatches" },
    @{ name = "Polarized Sunglasses"; category = "Lifestyle"; price = 49.00; description = "UV400 protection polarized shades" },
    @{ name = "Insulated Lunch Box"; category = "Lifestyle"; price = 29.00; description = "Lunch cooler bag with divider" }
)

foreach ($p in $products) {
    $json = $p | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8082/product" -Method POST -ContentType "application/json" -Body $json
    Write-Host "  Created Product #$($res.productId): [$($res.category)] $($res.name) - `$$($res.price)" -ForegroundColor Green
}

# -----------------------------------------------------------------------------
# 4. Populate Shopping Baskets & Create Orders (Order Service - Port 8083)
# -----------------------------------------------------------------------------
Write-Host "`n[3/4] Adding Basket Items & Placing Orders..." -ForegroundColor Yellow

# Add items to Customer 1's basket
$basketItem1 = @{ productId = 1; quantity = 2 } | ConvertTo-Json
$basketItem2 = @{ productId = 4; quantity = 1 } | ConvertTo-Json
$null = Invoke-RestMethod -Uri "http://localhost:8081/customer/1/basket" -Method POST -ContentType "application/json" -Body $basketItem1
$null = Invoke-RestMethod -Uri "http://localhost:8081/customer/1/basket" -Method POST -ContentType "application/json" -Body $basketItem2
Write-Host "  Added items to Customer 1's basket." -ForegroundColor Green

# Order 1: Created from Customer 1's basket (omitting items in request)
$order1Payload = @{ customerId = 1 } | ConvertTo-Json
$order1 = Invoke-RestMethod -Uri "http://localhost:8083/order" -Method POST -ContentType "application/json" -Body $order1Payload
Write-Host "  Created Order #$($order1.orderId) from Customer 1's basket (Total: `$$($order1.total), Status: $($order1.status))" -ForegroundColor Green

# Order 2: Created with explicit items for Customer 2
$order2Payload = @{
    customerId = 2
    items = @(
        @{ productId = 6; quantity = 1 }
        @{ productId = 7; quantity = 1 }
    )
} | ConvertTo-Json -Depth 5
$order2 = Invoke-RestMethod -Uri "http://localhost:8083/order" -Method POST -ContentType "application/json" -Body $order2Payload
Write-Host "  Created Order #$($order2.orderId) for Customer 2 (Total: `$$($order2.total), Status: $($order2.status))" -ForegroundColor Green

# Update Order 2 status to InTransit
$statusUpdate = @{ status = "InTransit" } | ConvertTo-Json
$order2Updated = Invoke-RestMethod -Uri "http://localhost:8083/order/$($order2.orderId)/status" -Method PUT -ContentType "application/json" -Body $statusUpdate
Write-Host "  Updated Order #$($order2.orderId) status to $($order2Updated.status)" -ForegroundColor Green

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
$null = Invoke-RestMethod -Uri "http://localhost:8083/order/$($order3.orderId)/status" -Method PUT -ContentType "application/json" -Body $statusDelivered
Write-Host "  Created Order #$($order3.orderId) for Customer 3 (Status: Delivered)" -ForegroundColor Green

# Order 4: Created for Customer 4 and then Cancelled
$order4Payload = @{
    customerId = 4
    items = @(
        @{ productId = 14; quantity = 1 }
    )
} | ConvertTo-Json -Depth 5
$order4 = Invoke-RestMethod -Uri "http://localhost:8083/order" -Method POST -ContentType "application/json" -Body $order4Payload
$order4Cancelled = Invoke-RestMethod -Uri "http://localhost:8083/order/$($order4.orderId)/cancel" -Method POST
Write-Host "  Created and Cancelled Order #$($order4.orderId) for Customer 4 (Status: $($order4Cancelled.status))" -ForegroundColor Green

# -----------------------------------------------------------------------------
# 5. Populate Notifications (Notification Service - Port 8084)
# -----------------------------------------------------------------------------
Write-Host "`n[4/4] Sending Sample Notifications..." -ForegroundColor Yellow

# Direct by Customer ID
$notifCust1 = @{ message = "Exclusive VIP Reward: Enjoy 15% off your next purchase with code VIP15." } | ConvertTo-Json
$resN1 = Invoke-RestMethod -Uri "http://localhost:8084/notification/customer/1" -Method POST -ContentType "application/json" -Body $notifCust1
Write-Host "  Sent Notification #$($resN1.notificationId) to Customer 1 via $($resN1.type)" -ForegroundColor Green

# Direct by Customer Email
$notifEmail = @{ message = "Monthly Newsletter: Tech gadgets trends for Spring 2026." } | ConvertTo-Json
$resN2 = Invoke-RestMethod -Uri "http://localhost:8084/notification/customer/email/bob.johnson@example.com" -Method POST -ContentType "application/json" -Body $notifEmail
Write-Host "  Sent Notification #$($resN2.notificationId) to Bob Johnson via $($resN2.type)" -ForegroundColor Green

# Direct by Customer Phone
$notifPhone = @{ message = "Your security verification code is 829410." } | ConvertTo-Json
$resN3 = Invoke-RestMethod -Uri "http://localhost:8084/notification/customer/phone/0433333333" -Method POST -ContentType "application/json" -Body $notifPhone
Write-Host "  Sent Notification #$($resN3.notificationId) to Charlie Brown via $($resN3.type)" -ForegroundColor Green

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
