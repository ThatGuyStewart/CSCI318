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
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Noise Cancelling Earbuds\",\"category\":\"Electronics\",\"price\":159.00,\"description\":\"True wireless earbuds with charging case\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Portable Power Bank 20000mAh\",\"category\":\"Electronics\",\"price\":69.00,\"description\":\"Fast charge USB-C / USB-A\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Smart Home Assistant\",\"category\":\"Electronics\",\"price\":129.00,\"description\":\"Voice assistant with smart home control\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"27in Curved Gaming Monitor\",\"category\":\"Electronics\",\"price\":399.00,\"description\":\"144Hz VA panel with G-Sync compatible\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"USB-C Multiport Dock\",\"category\":\"Electronics\",\"price\":99.00,\"description\":\"4K HDMI, Ethernet, USB-A, PD passthrough\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Action Camera 4K\",\"category\":\"Electronics\",\"price\":189.00,\"description\":\"Waterproof action cam with image stabilization\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Wireless Charging Pad\",\"category\":\"Electronics\",\"price\":39.00,\"description\":\"Fast Qi wireless charger pad\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Wireless Multi-Room Audio Streamer\",\"category\":\"Electronics\",\"price\":189.00,\"description\":\"Hi-Res lossless audio streaming receiver\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Vinyl Turntable with Built-in Preamp\",\"category\":\"Electronics\",\"price\":220.00,\"description\":\"Belt drive with USB digital recording\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"4K Blu-ray Player\",\"category\":\"Electronics\",\"price\":149.00,\"description\":\"HDR compatible Blu-ray player\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Gaming Controller\",\"category\":\"Electronics\",\"price\":69.00,\"description\":\"Wireless Bluetooth gamepad\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Noise Isolating Headphones\",\"category\":\"Electronics\",\"price\":119.00,\"description\":\"Over-ear studio monitor headphones\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Portable Bluetooth Radio\",\"category\":\"Electronics\",\"price\":59.00,\"description\":\"DAB+ and FM radio with Bluetooth\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Projector Mini LED\",\"category\":\"Electronics\",\"price\":249.00,\"description\":\"Portable home cinema projector\"}"
echo.


:: Appliances
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Robotic Vacuum Cleaner\",\"category\":\"Appliances\",\"price\":399.00,\"description\":\"Smart mapping and app control\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Smart Refrigerator Water Filter\",\"category\":\"Appliances\",\"price\":59.00,\"description\":\"Replacement filter for smart fridges\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Front Load Washing Machine\",\"category\":\"Appliances\",\"price\":799.00,\"description\":\"8kg smart front load with steam\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Smart Thermostat\",\"category\":\"Appliances\",\"price\":199.00,\"description\":\"Wi-Fi programmable thermostat\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Clothes Steamer Handheld\",\"category\":\"Appliances\",\"price\":49.00,\"description\":\"Portable garment steamer\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Upright Freezer 300L\",\"category\":\"Appliances\",\"price\":599.00,\"description\":\"Energy efficient chest/freezer\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Induction Cooktop Portable\",\"category\":\"Appliances\",\"price\":119.00,\"description\":\"Single zone induction with timer\"}"
echo.


:: Furniture
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Ergonomic Mesh Office Chair\",\"category\":\"Furniture\",\"price\":320.00,\"description\":\"Adjustable lumbar support and 3D armrests\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Motorized Standing Desk 140cm\",\"category\":\"Furniture\",\"price\":550.00,\"description\":\"Dual motor electric height adjustable desk\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"3-Seater Fabric Sofa\",\"category\":\"Furniture\",\"price\":899.00,\"description\":\"Modern mid-century fabric sofa\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Solid Oak Coffee Table\",\"category\":\"Furniture\",\"price\":249.00,\"description\":\"Handcrafted solid wood table\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"5-Shelf Bookshelf\",\"category\":\"Furniture\",\"price\":129.00,\"description\":\"Industrial style shelving unit\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Bedside Table with Drawer\",\"category\":\"Furniture\",\"price\":79.00,\"description\":\"Compact bedside storage unit\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Queen Mattress Medium Firm\",\"category\":\"Furniture\",\"price\":749.00,\"description\":\"Hybrid spring and memory foam\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Entertainment TV Unit 180cm\",\"category\":\"Furniture\",\"price\":399.00,\"description\":\"Low profile media console\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Set of 2 Bar Stools\",\"category\":\"Furniture\",\"price\":159.00,\"description\":\"Adjustable height bar stools\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"6-Seater Dining Table Set\",\"category\":\"Furniture\",\"price\":699.00,\"description\":\"Dining table with 6 chairs\"}"
echo.

:: Kitchen
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Japanese Damascus Chef Knife\",\"category\":\"Kitchen\",\"price\":129.00,\"description\":\"8-inch high carbon stainless steel blade\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Enameled Cast Iron Dutch Oven\",\"category\":\"Kitchen\",\"price\":180.00,\"description\":\"5.5 Quart heavy duty cooking pot\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Non-Stick Pan Set 3pc\",\"category\":\"Kitchen\",\"price\":89.00,\"description\":\"PFOA-free non-stick frying pans\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"High-Speed Blender\",\"category\":\"Kitchen\",\"price\":149.00,\"description\":\"Smoothie and soup prep blender\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Food Processor 8cup\",\"category\":\"Kitchen\",\"price\":129.00,\"description\":\"Multi-function chopping and dough\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Stand Mixer 4.5L\",\"category\":\"Kitchen\",\"price\":299.00,\"description\":\"Planetary mixer with attachments\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Premium Bamboo Cutting Board Set\",\"category\":\"Kitchen\",\"price\":39.00,\"description\":\"3-piece board set with juice groove\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Knife Sharpener 3-Stage\",\"category\":\"Kitchen\",\"price\":29.00,\"description\":\"Electric sharpener for kitchen knives\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Stainless Measuring Set\",\"category\":\"Kitchen\",\"price\":19.00,\"description\":\"Measuring cups and spoons set\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Wall-Mounted Spice Rack\",\"category\":\"Kitchen\",\"price\":49.00,\"description\":\"12-jar spice organizer\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Digital Air Fryer XL\",\"category\":\"Kitchen\",\"price\":149.95,\"description\":\"5.8L capacity with 8 one-touch presets\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Automatic Espresso Machine\",\"category\":\"Kitchen\",\"price\":699.00,\"description\":\"15-bar Italian pump with milk frother\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Convection Microwave Oven\",\"category\":\"Kitchen\",\"price\":249.00,\"description\":\"900W with grill and convection\"}"
echo.

:: Tools
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"20V Brushless Cordless Drill\",\"category\":\"Tools\",\"price\":159.00,\"description\":\"Includes 2 batteries and fast charger\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"120-Piece Mechanics Tool Set\",\"category\":\"Tools\",\"price\":110.00,\"description\":\"Chrome vanadium steel socket set\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Circular Saw 7-1/4in\",\"category\":\"Tools\",\"price\":139.00,\"description\":\"High torque cutting saw\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Cordless Impact Driver\",\"category\":\"Tools\",\"price\":129.00,\"description\":\"Compact impact for screws and bolts\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Multi-Tool Oscillating\",\"category\":\"Tools\",\"price\":99.00,\"description\":\"Versatile cutting and sanding tool\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"25m Tape Measure\",\"category\":\"Tools\",\"price\":24.00,\"description\":\"Auto-lock measuring tape\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Angle Grinder 125mm\",\"category\":\"Tools\",\"price\":79.00,\"description\":\"Cutting and grinding tool\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"3m Aluminium Ladder\",\"category\":\"Tools\",\"price\":149.00,\"description\":\"Lightweight folding ladder\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Orbital Sander\",\"category\":\"Tools\",\"price\":59.00,\"description\":\"Random orbital finishing sander\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"LED Work Light\",\"category\":\"Tools\",\"price\":39.00,\"description\":\"Portable rechargeable floodlight\"}"
echo.

:: Garden
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Cordless Grass Trimmer\",\"category\":\"Garden\",\"price\":135.00,\"description\":\"Lightweight battery powered line trimmer\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Retractable Garden Hose Reel 25m\",\"category\":\"Garden\",\"price\":95.00,\"description\":\"Auto-rewind wall mounted water hose\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Electric Lawn Mower 40V\",\"category\":\"Garden\",\"price\":499.00,\"description\":\"Cordless mower with 40L catcher\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Garden Kneeler and Seat\",\"category\":\"Garden\",\"price\":39.00,\"description\":\"Foam kneeler with tool pouches\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Compost Bin 200L\",\"category\":\"Garden\",\"price\":89.00,\"description\":\"Rotating outdoor compost bin\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Pruning Shears Bypass\",\"category\":\"Garden\",\"price\":29.00,\"description\":\"Ergonomic cutting shears\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Patio Heater 3kW\",\"category\":\"Garden\",\"price\":179.00,\"description\":\"Gas patio heater with safety tilt\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Weatherproof Bird Feeder\",\"category\":\"Garden\",\"price\":24.00,\"description\":\"Hanging seed feeder\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Soil pH Meter\",\"category\":\"Garden\",\"price\":19.00,\"description\":\"Digital soil tester\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Set of 4 Outdoor Planters\",\"category\":\"Garden\",\"price\":69.00,\"description\":\"Durable resin planter set\"}"
echo.

:: Sports
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Adjustable Dumbbell Pair 24kg\",\"category\":\"Sports\",\"price\":340.00,\"description\":\"Quick select weight dial from 2.5kg to 24kg\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"High Density Yoga Mat 6mm\",\"category\":\"Sports\",\"price\":45.00,\"description\":\"Non-slip eco-friendly alignment mat\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Folding Treadmill\",\"category\":\"Sports\",\"price\":799.00,\"description\":\"Compact foldable treadmill with incline\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Official Size Soccer Ball\",\"category\":\"Sports\",\"price\":29.00,\"description\":\"Durable stitched training ball\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Pro Basketball\",\"category\":\"Sports\",\"price\":39.00,\"description\":\"Indoor/outdoor composite leather\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Tennis Racket Composite\",\"category\":\"Sports\",\"price\":89.00,\"description\":\"Lightweight graphite frame\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Running Shoes (Unisex)\",\"category\":\"Sports\",\"price\":129.00,\"description\":\"Cushioned road running shoe\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Swim Goggles Anti-Fog\",\"category\":\"Sports\",\"price\":19.00,\"description\":\"Comfort seal for long swims\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Indoor Exercise Bike\",\"category\":\"Sports\",\"price\":499.00,\"description\":\"Magnetic resistance stationary bike\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Resistance Bands Set\",\"category\":\"Sports\",\"price\":29.00,\"description\":\"5-level resistance loop bands\"}"
echo.

:: Toys
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Modular Space Station Building Kit\",\"category\":\"Toys\",\"price\":89.90,\"description\":\"1050 pieces with astronaut minifigures\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Remote Control Off-Road Buggy\",\"category\":\"Toys\",\"price\":65.00,\"description\":\"High-speed 1:16 scale 4WD RC vehicle\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"1000pc Jigsaw Puzzle\",\"category\":\"Toys\",\"price\":24.00,\"description\":\"Scenic landscape 1000-piece puzzle\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Strategic Board Game\",\"category\":\"Toys\",\"price\":49.00,\"description\":\"Family strategy board game for 2-6 players\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Educational Coding Robot\",\"category\":\"Toys\",\"price\":129.00,\"description\":\"STEM robot for kids with coding app\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Plush Teddy Bear Large\",\"category\":\"Toys\",\"price\":29.00,\"description\":\"Soft stuffed animal plush toy\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Doll Stroller\",\"category\":\"Toys\",\"price\":39.00,\"description\":\"Foldable stroller for dolls\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Wooden Train Set\",\"category\":\"Toys\",\"price\":59.00,\"description\":\"Classic wooden track with carriages\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Kids Art & Craft Set\",\"category\":\"Toys\",\"price\":34.00,\"description\":\"Paints, crayons, and paper set\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Science Lab Kit\",\"category\":\"Toys\",\"price\":44.00,\"description\":\"Beginner experiments with safe reagents\"}"
echo.

:: Automotive
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Dual Lens 4K Dash Cam\",\"category\":\"Automotive\",\"price\":175.00,\"description\":\"Front and rear recording with Night Vision\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Portable Jump Starter 2000A\",\"category\":\"Automotive\",\"price\":119.00,\"description\":\"Heavy duty booster pack with power bank\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Electric Tyre Inflator\",\"category\":\"Automotive\",\"price\":49.00,\"description\":\"Cordless tyre pump with gauge\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"12V Car Vacuum Cleaner\",\"category\":\"Automotive\",\"price\":39.00,\"description\":\"Portable hand vacuum for cars\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Universal Roof Rack\",\"category\":\"Automotive\",\"price\":199.00,\"description\":\"Crossbar set for most vehicles\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"All-Weather Car Cover\",\"category\":\"Automotive\",\"price\":59.00,\"description\":\"Protective cover for sedans\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Seat Covers Full Set\",\"category\":\"Automotive\",\"price\":89.00,\"description\":\"Universal fit seat cover set\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Bluetooth OBD2 Adapter\",\"category\":\"Automotive\",\"price\":29.00,\"description\":\"Scan car diagnostics via app\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Portable GPS Navigator\",\"category\":\"Automotive\",\"price\":129.00,\"description\":\"7-inch GPS with lifetime maps\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Engine Coolant 4L\",\"category\":\"Automotive\",\"price\":29.00,\"description\":\"All-season antifreeze and coolant\"}"
echo.

:: Pets
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Orthopedic Memory Foam Dog Bed\",\"category\":\"Pets\",\"price\":85.00,\"description\":\"Waterproof washable liner for large dogs\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Smart Automatic Pet Feeder\",\"category\":\"Pets\",\"price\":115.00,\"description\":\"Timed meal dispensing with voice recorder\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"3-Level Cat Tree\",\"category\":\"Pets\",\"price\":129.00,\"description\":\"Scratching post with perches\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Hard Shell Pet Carrier\",\"category\":\"Pets\",\"price\":59.00,\"description\":\"Airline approved travel carrier\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"LED Dog Collar\",\"category\":\"Pets\",\"price\":19.00,\"description\":\"Rechargeable light-up collar for night walks\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Pet Grooming Kit\",\"category\":\"Pets\",\"price\":39.00,\"description\":\"Brush, clippers, nail trimmer set\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Shampoo for Pets 500ml\",\"category\":\"Pets\",\"price\":14.00,\"description\":\"Hypoallergenic pet shampoo\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Dental Chews for Dogs\",\"category\":\"Pets\",\"price\":12.00,\"description\":\"Pack of 30 dental sticks\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Training Clicker\",\"category\":\"Pets\",\"price\":6.00,\"description\":\"Handheld dog training clicker\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Aquarium Filter 200L\",\"category\":\"Pets\",\"price\":79.00,\"description\":\"External canister filter for aquariums\"}"
echo.

:: Apparel
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"100% Merino Wool Thermal Crew\",\"category\":\"Apparel\",\"price\":95.00,\"description\":\"Breathable moisture wicking base layer\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Waterproof All-Weather Jacket\",\"category\":\"Apparel\",\"price\":160.00,\"description\":\"Breathable hooded rain shell\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Organic Cotton T-Shirt\",\"category\":\"Apparel\",\"price\":29.00,\"description\":\"Soft crew neck tee\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Slim Chino Pants\",\"category\":\"Apparel\",\"price\":59.00,\"description\":\"Smart casual chinos\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Running Shorts\",\"category\":\"Apparel\",\"price\":34.00,\"description\":\"Lightweight mesh running shorts\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Leather Belt\",\"category\":\"Apparel\",\"price\":39.00,\"description\":\"Genuine leather dress belt\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Wool Scarf\",\"category\":\"Apparel\",\"price\":49.00,\"description\":\"Warm winter scarf\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Everyday Sneakers\",\"category\":\"Apparel\",\"price\":89.00,\"description\":\"Casual lace-up sneakers\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Mens Swim Trunks\",\"category\":\"Apparel\",\"price\":39.00,\"description\":\"Quick-dry swim shorts\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Knitted Beanie\",\"category\":\"Apparel\",\"price\":19.00,\"description\":\"Warm stretch beanie hat\"}"
echo.

:: Beauty
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Vitamin C Facial Radiance Serum\",\"category\":\"Beauty\",\"price\":52.00,\"description\":\"Antioxidant formula with Hyaluronic Acid\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Ionic Ceramic Hair Dryer\",\"category\":\"Beauty\",\"price\":110.00,\"description\":\"Fast drying salon grade 2200W motor\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Gentle Foaming Cleanser\",\"category\":\"Beauty\",\"price\":24.00,\"description\":\"Daily cleanser for all skin types\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Overnight Repair Night Cream\",\"category\":\"Beauty\",\"price\":59.00,\"description\":\"Restorative night-time moisturizer\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Sunscreen SPF50\",\"category\":\"Beauty\",\"price\":22.00,\"description\":\"Broad spectrum UV protection\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Volumizing Mascara\",\"category\":\"Beauty\",\"price\":19.00,\"description\":\"Smudge-proof lash volumizer\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Electric Toothbrush\",\"category\":\"Beauty\",\"price\":69.00,\"description\":\"Rechargeable toothbrush with timer\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Sheet Mask Set 5pc\",\"category\":\"Beauty\",\"price\":15.00,\"description\":\"Hydrating single-use masks\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Repair Hand Cream\",\"category\":\"Beauty\",\"price\":12.00,\"description\":\"Nourishing hand cream with shea\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Nail Care Kit\",\"category\":\"Beauty\",\"price\":18.00,\"description\":\"Files, clippers and polish\"}"
echo.

:: Grocery
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Single Origin Ethiopian Coffee Beans 1kg\",\"category\":\"Grocery\",\"price\":42.00,\"description\":\"Medium roast whole bean specialty coffee\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Cold Pressed Organic Olive Oil 750ml\",\"category\":\"Grocery\",\"price\":24.50,\"description\":\"Extra virgin unrefined estate oil\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Fresh Milk 1L\",\"category\":\"Grocery\",\"price\":2.50,\"description\":\"Full cream fresh milk\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Free-Range Eggs 12 Pack\",\"category\":\"Grocery\",\"price\":6.50,\"description\":\"Large free-range eggs\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Brown Rice 1kg\",\"category\":\"Grocery\",\"price\":4.20,\"description\":\"Wholegrain brown rice\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Pasta Penne 500g\",\"category\":\"Grocery\",\"price\":1.80,\"description\":\"Durum wheat semolina pasta\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Tomato Pasta Sauce 700g\",\"category\":\"Grocery\",\"price\":3.00,\"description\":\"Rich Italian tomato sauce\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Canned Tuna in Springwater 185g\",\"category\":\"Grocery\",\"price\":1.90,\"description\":\"Sustainably sourced tuna\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Organic Granola 500g\",\"category\":\"Grocery\",\"price\":6.00,\"description\":\"Honey and oat granola\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Crunchy Peanut Butter 500g\",\"category\":\"Grocery\",\"price\":5.50,\"description\":\"No added sugar peanut butter\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Pure Australian Honey 350g\",\"category\":\"Grocery\",\"price\":8.00,\"description\":\"Raw multifloral honey\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"White Sugar 1kg\",\"category\":\"Grocery\",\"price\":1.60,\"description\":\"Refined white sugar\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Plain Flour 1kg\",\"category\":\"Grocery\",\"price\":1.40,\"description\":\"All-purpose plain flour\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Black Tea 100g\",\"category\":\"Grocery\",\"price\":3.20,\"description\":\"Blend of Ceylon and Assam\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Sparkling Water 6 Pack 500ml\",\"category\":\"Grocery\",\"price\":5.00,\"description\":\"Lemon lime sparkling water\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Butter Salted 500g\",\"category\":\"Grocery\",\"price\":4.50,\"description\":\"Creamery salted butter\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Cheddar Cheese 250g\",\"category\":\"Grocery\",\"price\":5.50,\"description\":\"Mature cheddar block\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Bananas Bunch (1kg)\",\"category\":\"Grocery\",\"price\":3.00,\"description\":\"Fresh ripe bananas\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Gala Apples 1kg\",\"category\":\"Grocery\",\"price\":4.00,\"description\":\"Crisp fresh apples\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Raw Almonds 500g\",\"category\":\"Grocery\",\"price\":9.00,\"description\":\"Unsalted roasted almonds\"}"
echo.

:: Media
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"E-Reader 8GB\",\"category\":\"Media\",\"price\":129.00,\"description\":\"Glare-free e-ink display\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Classic Novels Box Set\",\"category\":\"Media\",\"price\":59.00,\"description\":\"10-book hardcover collection\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Streaming Stick 4K\",\"category\":\"Media\",\"price\":79.00,\"description\":\"Voice remote included\"}"
echo.

:: Professional
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Ultra-Fast Duplex Document Scanner\",\"category\":\"Professional\",\"price\":399.00,\"description\":\"50 sheet auto document feeder 40ppm\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Wireless Conference Speakerphone\",\"category\":\"Professional\",\"price\":145.00,\"description\":\"360-degree voice pickup with AI noise filter\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Ergonomic Keyboard\",\"category\":\"Professional\",\"price\":89.00,\"description\":\"Split ergonomic keyboard\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Business Laser Projector\",\"category\":\"Professional\",\"price\":999.00,\"description\":\"High-lumen projector for meeting rooms\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Monitor Arm Dual\",\"category\":\"Professional\",\"price\":129.00,\"description\":\"Adjustable dual monitor arm\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Label Printer\",\"category\":\"Professional\",\"price\":99.00,\"description\":\"Thermal label printer for shipping\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Network Attached Storage 2TB\",\"category\":\"Professional\",\"price\":249.00,\"description\":\"Backup NAS for small offices\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Mesh WiFi Pro\",\"category\":\"Professional\",\"price\":299.00,\"description\":\"Tri-band mesh WiFi system\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Time Clock Terminal\",\"category\":\"Professional\",\"price\":349.00,\"description\":\"Employee punch-in terminal\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Office Laminator\",\"category\":\"Professional\",\"price\":49.00,\"description\":\"A4 pouch laminator\"}"
echo.

:: Lifestyle
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Vacuum Insulated Stainless Bottle 1L\",\"category\":\"Lifestyle\",\"price\":38.00,\"description\":\"Keeps drinks cold 24hrs / hot 12hrs\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Aromatherapy Ultrasonic Diffuser\",\"category\":\"Lifestyle\",\"price\":48.00,\"description\":\"Quiet wood grain cool mist humidifier\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Yoga Block\",\"category\":\"Lifestyle\",\"price\":14.00,\"description\":\"EVA foam yoga support block\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Universal Travel Adapter\",\"category\":\"Lifestyle\",\"price\":24.00,\"description\":\"All-in-one international adapter\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Picnic Blanket Large\",\"category\":\"Lifestyle\",\"price\":29.00,\"description\":\"Waterproof back picnic blanket\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Hammock Single\",\"category\":\"Lifestyle\",\"price\":34.00,\"description\":\"Portable camping hammock\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Reusable Shopping Bag Set\",\"category\":\"Lifestyle\",\"price\":12.00,\"description\":\"Foldable shopping bags 5-pack\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Smartwatch Replacement Band\",\"category\":\"Lifestyle\",\"price\":19.00,\"description\":\"Silicone band for popular smartwatches\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Polarized Sunglasses\",\"category\":\"Lifestyle\",\"price\":49.00,\"description\":\"UV400 protection polarized shades\"}"
echo.
curl -s -X POST http://localhost:8082/product -H "Content-Type: application/json" -d "{\"name\":\"Insulated Lunch Box\",\"category\":\"Lifestyle\",\"price\":29.00,\"description\":\"Lunch cooler bag with divider\"}"
echo.

:: -----------------------------------------------------------------------------
:: 3. Populate Shopping Basket & Create Orders (Order Service - Port 8083)
:: -----------------------------------------------------------------------------
echo.
echo [3/4] Adding Basket Items & Placing Orders...

:: Add products to Customer 1's basket
curl -s -X POST http://localhost:8081/customer/1/basket -H "Content-Type: application/json" -d "{\"productId\":1,\"quantity\":2}"
echo.
curl -s -X POST http://localhost:8081/customer/1/basket -H "Content-Type: application/json" -d "{\"productId\":4,\"quantity\":1}"
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
