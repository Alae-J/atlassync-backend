package com.atlassync.product.repository;

import com.atlassync.product.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcode(String barcode);

    List<Product> findByBarcodeIn(List<String> barcodes);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Product> searchByNameContainingIgnoreCase(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM Product p " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "AND p.aisleNumber = :aisle")
    List<Product> searchByNameAndAisle(@Param("query") String query,
                                       @Param("aisle") Integer aisle,
                                       Pageable pageable);

    List<Product> findByAisleNumber(Integer aisleNumber, Pageable pageable);
}
