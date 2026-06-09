package com.atlassync.auth.lists;

public class ShoppingListNotFoundException extends RuntimeException {

    public ShoppingListNotFoundException(Long listId) {
        super("Shopping list not found: " + listId);
    }
}
