package com.commercemesh.catalog.config;

import com.commercemesh.catalog.document.Category;
import com.commercemesh.catalog.document.Product;
import com.commercemesh.catalog.repository.CategoryRepository;
import com.commercemesh.catalog.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class DataInitializer {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public DataInitializer(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @PostConstruct
    public void init() {
        if (categoryRepository.count() > 0 || productRepository.count() > 0) {
            return;
        }

        // Seed 5 categories
        Category electronics = createCategory("Electronics", "electronics",
                "Electronic devices and gadgets", null, 1);
        Category clothing = createCategory("Clothing", "clothing",
                "Apparel and fashion items", null, 2);
        Category homeGarden = createCategory("Home & Garden", "home-garden",
                "Home improvement and garden supplies", null, 3);
        Category sports = createCategory("Sports", "sports",
                "Sports equipment and outdoor gear", null, 4);
        Category books = createCategory("Books", "books",
                "Books and educational materials", null, 5);

        electronics = categoryRepository.save(electronics);
        clothing = categoryRepository.save(clothing);
        homeGarden = categoryRepository.save(homeGarden);
        sports = categoryRepository.save(sports);
        books = categoryRepository.save(books);

        // Seed 10 products
        productRepository.save(createProduct(
                "Wireless Bluetooth Headphones",
                "Premium noise-cancelling wireless headphones with 30-hour battery life.",
                "SoundMax",
                "SKU-ELEC-001",
                new BigDecimal("79.99"),
                new BigDecimal("129.99"),
                electronics.getId(),
                List.of("electronics", "audio", "headphones"),
                Map.of("color", "Black", "batteryLife", "30 hours"),
                true, true
        ));

        productRepository.save(createProduct(
                "Smartphone 128GB",
                "Latest generation smartphone with stunning display and powerful camera system.",
                "TechPro",
                "SKU-ELEC-002",
                new BigDecimal("699.99"),
                new BigDecimal("799.99"),
                electronics.getId(),
                List.of("electronics", "smartphone", "mobile"),
                Map.of("color", "Midnight Blue", "storage", "128GB"),
                true, true
        ));

        productRepository.save(createProduct(
                "USB-C Charging Cable 2m",
                "Durable braided USB-C charging cable, 2 meters long with fast charging support.",
                "ChargePlus",
                "SKU-ELEC-003",
                new BigDecimal("12.99"),
                new BigDecimal("19.99"),
                electronics.getId(),
                List.of("electronics", "accessories", "cable"),
                Map.of("length", "2m", "connector", "USB-C"),
                true, false
        ));

        productRepository.save(createProduct(
                "Classic Fit Cotton T-Shirt",
                "Soft, breathable 100% organic cotton t-shirt available in multiple colors.",
                "EcoWear",
                "SKU-CLOTH-001",
                new BigDecimal("24.99"),
                new BigDecimal("34.99"),
                clothing.getId(),
                List.of("clothing", "t-shirt", "cotton", "men"),
                Map.of("color", "White", "size", "M", "material", "Organic Cotton"),
                true, true
        ));

        productRepository.save(createProduct(
                "Slim Fit Denim Jeans",
                "Modern slim-fit denim jeans with slight stretch for comfort.",
                "DenimCo",
                "SKU-CLOTH-002",
                new BigDecimal("59.99"),
                new BigDecimal("79.99"),
                clothing.getId(),
                List.of("clothing", "jeans", "denim", "men"),
                Map.of("color", "Dark Blue", "size", "32", "fit", "Slim"),
                true, false
        ));

        productRepository.save(createProduct(
                "Stainless Steel Water Bottle",
                "Double-wall insulated stainless steel water bottle, keeps drinks cold 24 hours.",
                "HydroMate",
                "SKU-HOME-001",
                new BigDecimal("29.99"),
                new BigDecimal("39.99"),
                homeGarden.getId(),
                List.of("home", "kitchen", "bottle", "eco-friendly"),
                Map.of("color", "Silver", "capacity", "750ml"),
                true, true
        ));

        productRepository.save(createProduct(
                "LED Desk Lamp with Wireless Charger",
                "Adjustable LED desk lamp with built-in wireless phone charger and 5 brightness levels.",
                "BrightHome",
                "SKU-HOME-002",
                new BigDecimal("45.99"),
                new BigDecimal("59.99"),
                homeGarden.getId(),
                List.of("home", "office", "lamp", "LED"),
                Map.of("color", "White", "brightnessLevels", "5"),
                true, false
        ));

        productRepository.save(createProduct(
                "Running Shoes - Ultra Boost",
                "Lightweight responsive running shoes with energy-return cushioning.",
                "StrideX",
                "SKU-SPORT-001",
                new BigDecimal("129.99"),
                new BigDecimal("159.99"),
                sports.getId(),
                List.of("sports", "running", "shoes", "footwear"),
                Map.of("color", "Black/Red", "size", "42", "type", "Running"),
                true, true
        ));

        productRepository.save(createProduct(
                "Yoga Mat Premium 6mm",
                "Non-slip premium yoga mat with alignment lines, 6mm thick for joint comfort.",
                "ZenFit",
                "SKU-SPORT-002",
                new BigDecimal("34.99"),
                new BigDecimal("49.99"),
                sports.getId(),
                List.of("sports", "yoga", "mat", "fitness"),
                Map.of("color", "Purple", "thickness", "6mm"),
                true, false
        ));

        productRepository.save(createProduct(
                "Clean Code: A Handbook of Agile Software Craftsmanship",
                "The definitive guide to writing code that is easy to read, maintain, and extend.",
                "Pearson",
                "SKU-BOOK-001",
                new BigDecimal("39.99"),
                new BigDecimal("49.99"),
                books.getId(),
                List.of("books", "programming", "software", "coding"),
                Map.of("author", "Robert C. Martin", "pages", "464", "format", "Paperback"),
                true, true
        ));
    }

    private Category createCategory(String name, String slug, String description, String parentId, int sortOrder) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(description);
        category.setParentId(parentId);
        category.setActive(true);
        category.setSortOrder(sortOrder);
        return category;
    }

    private Product createProduct(String name, String description, String brand, String sku,
                                   BigDecimal price, BigDecimal compareAtPrice, String categoryId,
                                   List<String> tags, Map<String, String> attributes,
                                   boolean isFeatured, boolean isActive) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setBrand(brand);
        product.setSku(sku);
        product.setPrice(price);
        product.setCompareAtPrice(compareAtPrice);
        product.setCurrency("USD");
        product.setCategoryId(categoryId);
        product.setTags(tags);
        product.setAttributes(attributes);
        product.setImages(List.of());
        product.setFeatured(isFeatured);
        product.setActive(isActive);
        return product;
    }
}
