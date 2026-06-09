package com.atlassync.auth.lists;

import java.io.Serializable;
import java.util.Objects;

public class ShoppingListItemId implements Serializable {

    private Long listId;
    private Integer position;

    public ShoppingListItemId() {}

    public ShoppingListItemId(Long listId, Integer position) {
        this.listId = listId;
        this.position = position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShoppingListItemId that)) return false;
        return Objects.equals(listId, that.listId) && Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listId, position);
    }
}
