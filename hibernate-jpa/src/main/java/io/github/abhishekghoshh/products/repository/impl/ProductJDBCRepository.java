package io.github.abhishekghoshh.products.repository.impl;

import io.github.abhishekghoshh.products.entity.Product;
import io.github.abhishekghoshh.products.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Qualifier("ProductJDBCRepository")
@AllArgsConstructor
public class ProductJDBCRepository implements ProductRepository {

    private static final Logger logger = LoggerFactory.getLogger(ProductJDBCRepository.class);

    private final DataSource dataSource;

    @Override
    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();
        String query = "SELECT * FROM products";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                products.add(mapToProduct(resultSet));
            }
        } catch (SQLException e) {
            logger.error("Error fetching products", e);
        }

        return products;
    }

    @Override
    public Optional<Product> findById(Long id) {
        String query = "SELECT * FROM products WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapToProduct(resultSet));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching products", e);
        }

        return Optional.empty();
    }

    @Override
    public Product save(Product product) {
        String query = "INSERT INTO products (name, description, price) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, product.getName());
            statement.setString(2, product.getDescription());
            statement.setDouble(3, product.getPrice());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    product.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving product", e);
        }

        return product;
    }

    @Override
    public void delete(Product product) {
        String query = "DELETE FROM products WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setLong(1, product.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting product", e);
        }
    }

    private Product mapToProduct(ResultSet resultSet) throws SQLException {
        return new Product(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getFloat("price")
        );
    }
}