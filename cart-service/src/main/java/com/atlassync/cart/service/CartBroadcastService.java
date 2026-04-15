package com.atlassync.cart.service;

import com.atlassync.cart.dto.CartSnapshot;
import com.atlassync.cart.dto.CartUpdateEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class CartBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public CartBroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcast(String sessionId, String eventType, CartSnapshot snapshot) {
        CartUpdateEvent event = new CartUpdateEvent(sessionId, eventType, snapshot);
        messagingTemplate.convertAndSend("/topic/cart/" + sessionId, event);
    }
}
