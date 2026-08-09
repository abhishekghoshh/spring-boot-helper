package com.commercemesh.user.config;

import com.commercemesh.user.entity.Permission;
import com.commercemesh.user.entity.Role;
import com.commercemesh.user.repository.PermissionRepository;
import com.commercemesh.user.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Component
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public DataInitializer(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @PostConstruct
    @Transactional
    public void init() {
        if (roleRepository.count() > 0) {
            log.info("Roles already initialized, skipping seed data");
            return;
        }

        log.info("Seeding roles and permissions...");

        // Create permissions
        Permission userRead = createPermission("USER_READ", "View user information");
        Permission userWrite = createPermission("USER_WRITE", "Create and update user information");
        Permission productRead = createPermission("PRODUCT_READ", "View product information");
        Permission productWrite = createPermission("PRODUCT_WRITE", "Create and update products");
        Permission orderRead = createPermission("ORDER_READ", "View order information");
        Permission orderWrite = createPermission("ORDER_WRITE", "Create and update orders");
        Permission inventoryRead = createPermission("INVENTORY_READ", "View inventory information");
        Permission inventoryWrite = createPermission("INVENTORY_WRITE", "Update inventory levels");
        Permission roleManage = createPermission("ROLE_MANAGE", "Manage roles and permissions");
        Permission paymentRead = createPermission("PAYMENT_READ", "View payment information");
        Permission paymentWrite = createPermission("PAYMENT_WRITE", "Process payments");
        Permission reportRead = createPermission("REPORT_READ", "View reports and analytics");

        // SUPER_ADMIN: all permissions
        Role superAdmin = createRole("SUPER_ADMIN", "Full system access", Set.of(
                userRead, userWrite, productRead, productWrite,
                orderRead, orderWrite, inventoryRead, inventoryWrite,
                roleManage, paymentRead, paymentWrite, reportRead
        ));

        // ADMIN: most permissions except role management
        Role admin = createRole("ADMIN", "Administrative access", Set.of(
                userRead, userWrite, productRead, productWrite,
                orderRead, orderWrite, inventoryRead, inventoryWrite,
                paymentRead, paymentWrite, reportRead
        ));

        // PRODUCT_MANAGER: product and inventory
        createRole("PRODUCT_MANAGER", "Product and inventory management", Set.of(
                productRead, productWrite, inventoryRead, inventoryWrite
        ));

        // ORDER_MANAGER: orders and payments
        createRole("ORDER_MANAGER", "Order and payment management", Set.of(
                orderRead, orderWrite, paymentRead, paymentWrite
        ));

        // CUSTOMER_SUPPORT: read-only access to users, products, orders
        createRole("CUSTOMER_SUPPORT", "Customer support access", Set.of(
                userRead, productRead, orderRead, paymentRead
        ));

        // CUSTOMER: basic read access
        createRole("CUSTOMER", "Basic customer access", Set.of(
                productRead
        ));

        log.info("Seed data initialized successfully");
    }

    private Permission createPermission(String name, String description) {
        Permission permission = new Permission(name, description);
        return permissionRepository.save(permission);
    }

    private Role createRole(String name, String description, Set<Permission> permissions) {
        Role role = new Role(name, description);
        role.setPermissions(permissions);
        return roleRepository.save(role);
    }
}
