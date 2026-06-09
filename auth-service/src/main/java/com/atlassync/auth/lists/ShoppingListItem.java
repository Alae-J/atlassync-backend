package com.atlassync.auth.lists;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shopping_list_items")
@IdClass(ShoppingListItemId.class)
@Getter
@Setter
@NoArgsConstructor
public class ShoppingListItem {

    @Id
    @Column(name = "list_id", nullable = false)
    private Long listId;

    @Id
    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(nullable = false, length = 64)
    private String barcode;

    @Column(nullable = false)
    private Integer qty;
}
