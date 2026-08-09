package com.commercemesh.search.config;

import com.commercemesh.search.document.ProductDocument;
import com.commercemesh.search.repository.ProductSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ProductSearchRepository repository;

    public DataInitializer(ProductSearchRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (!repository.isEmpty()) {
            log.info("Search index already contains {} products. Skipping initialization.", repository.count());
            return;
        }

        log.info("Initializing search index with 15 sample products...");

        List<ProductDocument> sampleProducts = List.of(
                createProduct("Wireless Bluetooth Headphones", "Premium noise-cancelling over-ear headphones with 30-hour battery life and rich bass sound", "SoundMax", "Electronics",
                        new BigDecimal("149.99"), "https://images.example.com/headphones1.jpg", 4.5, 326, List.of("wireless", "bluetooth", "headphones", "noise-cancelling")),
                createProduct("Organic Green Tea Bags (100 Pack)", "Premium Japanese sencha green tea bags, rich in antioxidants, smooth taste", "TeaGarden", "Grocery",
                        new BigDecimal("12.99"), "https://images.example.com/greentea.jpg", 4.7, 1203, List.of("organic", "green-tea", "antioxidants")),
                createProduct("Running Shoes UltraFlex", "Lightweight mesh running shoes with responsive cushioning, ideal for long-distance running", "StridePro", "Footwear",
                        new BigDecimal("89.95"), "https://images.example.com/runningshoes.jpg", 4.3, 489, List.of("running", "shoes", "lightweight", "sports")),
                createProduct("Stainless Steel French Press", "Double-walled 34oz French press coffee maker with fine mesh filter", "BrewCraft", "Kitchen",
                        new BigDecimal("34.50"), "https://images.example.com/frenchpress.jpg", 4.6, 892, List.of("coffee", "french-press", "stainless-steel", "kitchen")),
                createProduct("Yoga Mat Premium 6mm", "Non-slip TPE eco-friendly yoga mat with carrying strap, perfect for home or studio", "ZenFit", "Sports",
                        new BigDecimal("26.99"), "https://images.example.com/yogamat.jpg", 4.4, 1567, List.of("yoga", "mat", "eco-friendly", "fitness")),
                createProduct("Smartphone 128GB Midnight Black", "6.7-inch AMOLED display, 50MP camera, 8GB RAM, 5G connectivity", "TechVibe", "Electronics",
                        new BigDecimal("699.00"), "https://images.example.com/smartphone.jpg", 4.8, 2341, List.of("smartphone", "5g", "android", "black")),
                createProduct("Ergonomic Office Chair", "Adjustable lumbar support mesh office chair with 3D armrests and tilt lock", "ComfortPlus", "Furniture",
                        new BigDecimal("249.99"), "https://images.example.com/chair.jpg", 4.2, 567, List.of("office", "chair", "ergonomic", "furniture")),
                createProduct("Cast Iron Skillet 12-inch", "Pre-seasoned cast iron skillet with helper handle, oven safe to 500°F", "IronChef", "Kitchen",
                        new BigDecimal("39.99"), "https://images.example.com/skillet.jpg", 4.7, 1923, List.of("cast-iron", "skillet", "cooking", "kitchen")),
                createProduct("LED Desk Lamp with USB Charging", "Adjustable brightness 5-mode LED desk lamp with touch control and USB port", "BrightSpace", "Office",
                        new BigDecimal("45.99"), "https://images.example.com/desklamp.jpg", 4.4, 876, List.of("led", "lamp", "desk", "usb")),
                createProduct("Organic Protein Powder Vanilla", "Plant-based protein powder with 20g protein per serving, no artificial sweeteners", "PureFuel", "Health",
                        new BigDecimal("42.00"), "https://images.example.com/protein.jpg", 4.1, 654, List.of("protein", "organic", "vegan", "vanilla", "supplement")),
                createProduct("Mechanical Keyboard RGB", "Cherry MX Blue switches, per-key RGB lighting, aluminum frame, programmable", "KeyMaster", "Electronics",
                        new BigDecimal("129.99"), "https://images.example.com/keyboard.jpg", 4.6, 1087, List.of("keyboard", "mechanical", "rgb", "gaming")),
                createProduct("Cotton Bed Sheet Set Queen", "400-thread count 100% Egyptian cotton sheet set, deep pockets, wrinkle-resistant", "DreamSoft", "Home",
                        new BigDecimal("59.99"), "https://images.example.com/sheets.jpg", 4.5, 2145, List.of("sheets", "cotton", "queen", "bedding")),
                createProduct("Bluetooth Portable Speaker", "Waterproof IPX7, 20W stereo sound, 12-hour battery, built-in microphone", "SoundMax", "Electronics",
                        new BigDecimal("79.99"), "https://images.example.com/speaker.jpg", 4.3, 765, List.of("speaker", "bluetooth", "portable", "waterproof")),
                createProduct("Instant Read Meat Thermometer", "3-second response waterproof digital thermometer with backlit display for BBQ cooking", "GrillPro", "Kitchen",
                        new BigDecimal("19.99"), "https://images.example.com/thermometer.jpg", 4.6, 1432, List.of("thermometer", "cooking", "bbq", "kitchen")),
                createProduct("Fitness Tracker Smart Band", "Heart rate monitor, SpO2 tracking, sleep analysis, 14-day battery, 5ATM waterproof", "FitBand", "Electronics",
                        new BigDecimal("54.99"), "https://images.example.com/fitnessband.jpg", 4.4, 892, List.of("fitness", "tracker", "smartwatch", "health"))
        );

        repository.saveAll(sampleProducts);
        log.info("Successfully initialized search index with {} sample products.", sampleProducts.size());
    }

    private ProductDocument createProduct(String name, String description, String brand, String category,
                                           BigDecimal price, String image, double rating, int reviews,
                                           List<String> tags) {
        String id = UUID.randomUUID().toString();
        return new ProductDocument(id, name, description, brand, category, price, image, rating, reviews, tags, true, Instant.now());
    }
}
