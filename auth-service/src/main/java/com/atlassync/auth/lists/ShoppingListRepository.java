package com.atlassync.auth.lists;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {

    List<ShoppingList> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<ShoppingList> findByIdAndUserId(Long id, Long userId);
}
