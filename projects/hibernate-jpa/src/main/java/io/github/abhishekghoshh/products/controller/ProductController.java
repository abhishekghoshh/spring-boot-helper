package io.github.abhishekghoshh.products.controller;

import io.github.abhishekghoshh.core.dto.SuccessDTO;
import io.github.abhishekghoshh.products.dto.ProductDTO;
import io.github.abhishekghoshh.products.service.ProductService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController()
@RequestMapping("/api/products")
@AllArgsConstructor
public class ProductController {

    private final ProductService productService;



    @GetMapping
    public List<ProductDTO> getAllProducts() {
        return productService.getAll();
    }

    @GetMapping("/pageable")
    public Page<ProductDTO> getAllPageableProducts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return productService.getAll(page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) throws Exception {
        Optional<ProductDTO> product = productService.getById(id);
        return product.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ProductDTO createProduct(@RequestBody ProductDTO productDTO) {
        return productService.save(productDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @RequestBody ProductDTO productDTO) throws Exception {
        ProductDTO updatedProduct = productService.updateById(id, productDTO);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessDTO> deleteProduct(@PathVariable Long id) throws Exception {
        productService.deleteById(id);
        return ResponseEntity.accepted()
                .body(new SuccessDTO("Successfully deleted"));
    }
}
