package com.commercesphere.inventory.service;
import com.commercesphere.inventory.document.Warehouse;
import com.commercesphere.inventory.dto.WarehouseDto;
import com.commercesphere.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class WarehouseService {
    private final WarehouseRepository repo;
    public WarehouseService(WarehouseRepository repo) { this.repo = repo; }
    public List<WarehouseDto> getAll() { return repo.findAll().stream().map(this::toDto).toList(); }
    public WarehouseDto create(WarehouseDto dto) {
        Warehouse w = Warehouse.builder().name(dto.name()).code(dto.code())
                .address(new Warehouse.Address(dto.street(), dto.city(), dto.state(), dto.zipCode(), dto.country()))
                .active(dto.active()).build();
        return toDto(repo.save(w));
    }
    private WarehouseDto toDto(Warehouse w) { return new WarehouseDto(w.getId(), w.getName(), w.getCode(), w.getAddress().getStreet(), w.getAddress().getCity(), w.getAddress().getState(), w.getAddress().getZipCode(), w.getAddress().getCountry(), w.isActive()); }
}
