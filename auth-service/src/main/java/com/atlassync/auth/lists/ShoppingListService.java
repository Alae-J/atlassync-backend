package com.atlassync.auth.lists;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ShoppingListService {

    private final ShoppingListRepository listRepo;
    private final ShoppingListItemRepository itemRepo;

    public ShoppingListService(ShoppingListRepository listRepo,
                               ShoppingListItemRepository itemRepo) {
        this.listRepo = listRepo;
        this.itemRepo = itemRepo;
    }

    // ── Queries ─────────────────────────────────────────────────────────────

    public List<ListSummary> listForUser(Long userId) {
        return listRepo.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(list -> {
                    int count = itemRepo.findByListIdOrderByPosition(list.getId()).size();
                    return new ListSummary(list.getId(), list.getName(), count, null, list.getUpdatedAt());
                })
                .toList();
    }

    public ListDetail getForUser(Long userId, Long listId) {
        ShoppingList list = listRepo.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ShoppingListNotFoundException(listId));
        List<ListItemDto> items = toItemDtos(itemRepo.findByListIdOrderByPosition(listId));
        return new ListDetail(list.getId(), list.getName(), items, list.getCreatedAt(), list.getUpdatedAt());
    }

    // ── Mutations ────────────────────────────────────────────────────────────

    @Transactional
    public ListDetail create(Long userId, CreateListRequest req) {
        ShoppingList list = new ShoppingList();
        list.setUserId(userId);
        list.setName(req.name().strip());
        list = listRepo.save(list);

        List<ShoppingListItem> saved = insertItems(list.getId(),
                req.items() != null ? req.items() : List.of());

        log.info("[lists] created listId={} userId={} items={}", list.getId(), userId, saved.size());
        return toDetail(list, saved);
    }

    @Transactional
    public ListDetail update(Long userId, Long listId, UpdateListRequest req) {
        ShoppingList list = listRepo.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ShoppingListNotFoundException(listId));

        if (req.name() != null) {
            list.setName(req.name().strip());
        }

        List<ShoppingListItem> items;
        if (req.items() != null) {
            itemRepo.deleteByListId(listId);
            items = insertItems(listId, req.items());
        } else {
            items = itemRepo.findByListIdOrderByPosition(listId);
        }

        list = listRepo.save(list);
        log.info("[lists] updated listId={} userId={}", listId, userId);
        return toDetail(list, items);
    }

    @Transactional
    public void delete(Long userId, Long listId) {
        ShoppingList list = listRepo.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ShoppingListNotFoundException(listId));
        listRepo.delete(list);
        log.info("[lists] deleted listId={} userId={}", listId, userId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private List<ShoppingListItem> insertItems(Long listId, List<ListItemDto> dtos) {
        List<ShoppingListItem> entities = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            ListItemDto dto = dtos.get(i);
            ShoppingListItem item = new ShoppingListItem();
            item.setListId(listId);
            item.setPosition(i);
            item.setBarcode(dto.barcode());
            item.setQty(dto.qty());
            entities.add(item);
        }
        return itemRepo.saveAll(entities);
    }

    private List<ListItemDto> toItemDtos(List<ShoppingListItem> items) {
        return items.stream()
                .map(i -> new ListItemDto(i.getBarcode(), i.getQty()))
                .toList();
    }

    private ListDetail toDetail(ShoppingList list, List<ShoppingListItem> items) {
        return new ListDetail(
                list.getId(),
                list.getName(),
                toItemDtos(items),
                list.getCreatedAt(),
                list.getUpdatedAt()
        );
    }
}
