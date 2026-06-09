package com.atlassync.auth.lists;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, ShoppingListItemId> {

    List<ShoppingListItem> findByListIdOrderByPosition(Long listId);

    @Transactional
    void deleteByListId(Long listId);
}
