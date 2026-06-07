package com.atlassync.product.service;

import com.atlassync.product.entity.Product;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a raw Open Food Facts {@code product} JSON node into a fully-populated
 * {@link Product} entity. The OFF payload is rich on labelling (allergens,
 * dietary flags, categories, nutrition) but bare on commerce — no price, no
 * stock, no aisle. This class fills those gaps with policies tuned for the
 * Moroccan store: MAD prices by aisle, stock default 100, RFID flag from
 * aisle (seafood + meat), aisle inferred from OFF category tags.
 */
@Component
public class ProductEnricher {

    private static final int DEFAULT_AISLE = 9;
    private static final BigDecimal DEFAULT_PRICE_MAD = new BigDecimal("20.00");
    private static final String CURRENCY_MAD = "MAD";

    /**
     * OFF category tag → aisle number. Order matters: the first key found in
     * the product's categories_tags wins, so list specific categories before
     * generic ones (e.g. en:yogurts before en:dairies before en:plant-based-foods).
     */
    private static final Map<String, Integer> AISLE_BY_CATEGORY = orderedMap(
            // Produce
            "en:fruits", 1,
            "en:vegetables", 1,
            "en:fresh-vegetables", 1,
            "en:fresh-fruits", 1,
            // Dairy
            "en:yogurts", 5,
            "en:cheeses", 5,
            "en:milks", 5,
            "en:eggs", 5,
            "en:dairies", 5,
            // Beverages
            "en:waters", 7,
            "en:juices", 7,
            "en:sodas", 7,
            "en:carbonated-drinks", 7,
            "en:teas", 7,
            "en:coffees", 7,
            "en:beverages", 7,
            // Bakery
            "en:breads", 8,
            "en:bakery-products", 8,
            "en:viennoiseries", 8,
            // Pantry
            "en:pastas", 9,
            "en:rices", 9,
            "en:cereals", 9,
            "en:olive-oils", 9,
            "en:oils", 9,
            "en:condiments", 9,
            "en:sauces", 9,
            "en:canned-foods", 9,
            // Seafood
            "en:seafood", 11,
            "en:fishes", 11,
            "en:fresh-fishes", 11,
            "en:shellfishes", 11,
            // Meat / Halal counter
            "en:meats", 12,
            "en:poultries", 12,
            "en:chickens", 12,
            "en:beefs", 12,
            // Snacks (lands as aisle 13, mobile renders any number)
            "en:biscuits", 13,
            "en:chocolates", 13,
            "en:cookies", 13,
            "en:snacks", 13,
            "en:confectioneries", 13,
            "en:cakes", 13,
            "en:sweet-snacks", 13
    );

    /** Per-aisle MAD price baseline. Demo-grade — realistic but not market-accurate. */
    private static final Map<Integer, BigDecimal> PRICE_BY_AISLE = Map.of(
            1, new BigDecimal("12.00"),   // Produce
            5, new BigDecimal("18.00"),   // Dairy
            7, new BigDecimal("10.00"),   // Beverages
            8, new BigDecimal("15.00"),   // Bakery
            9, new BigDecimal("28.00"),   // Pantry
            11, new BigDecimal("130.00"), // Seafood
            12, new BigDecimal("70.00"),  // Meat / Halal
            13, new BigDecimal("16.00")   // Snacks
    );

    /** OFF allergen tag → mobile-side allergen string from src/data/allergens.ts. */
    private static final Map<String, String> ALLERGEN_MAP = Map.ofEntries(
            Map.entry("en:gluten", "Gluten"),
            Map.entry("en:milk", "Milk"),
            Map.entry("en:eggs", "Eggs"),
            Map.entry("en:nuts", "Tree nuts"),
            Map.entry("en:tree-nuts", "Tree nuts"),
            Map.entry("en:peanuts", "Peanuts"),
            Map.entry("en:soybeans", "Soy"),
            Map.entry("en:fish", "Fish"),
            Map.entry("en:crustaceans", "Crustaceans"),
            Map.entry("en:molluscs", "Molluscs"),
            Map.entry("en:sesame-seeds", "Sesame"),
            Map.entry("en:mustard", "Mustard"),
            Map.entry("en:celery", "Celery"),
            Map.entry("en:sulphur-dioxide-and-sulphites", "Sulphites"),
            Map.entry("en:lupin", "Lupin")
    );

    /** OFF label tag → mobile-side dietary string from src/data/dietary.ts. */
    private static final Map<String, String> DIETARY_MAP = Map.ofEntries(
            Map.entry("en:vegan", "Vegan"),
            Map.entry("en:vegetarian", "Vegetarian"),
            Map.entry("en:gluten-free", "Gluten-free"),
            Map.entry("en:no-gluten", "Gluten-free"),
            Map.entry("en:organic", "Organic only"),
            Map.entry("en:fr:bio", "Organic only"),
            Map.entry("en:halal", "Halal"),
            Map.entry("en:lactose-free", "Lactose-free"),
            Map.entry("en:no-lactose", "Lactose-free"),
            Map.entry("en:sugar-free", "Sugar-free"),
            Map.entry("en:no-added-sugar", "Low sugar"),
            Map.entry("en:low-sugar", "Low sugar"),
            Map.entry("en:low-salt", "Low sodium"),
            Map.entry("en:low-sodium", "Low sodium")
    );

    public Product enrich(String barcode, JsonNode productNode) {
        Product product = new Product();
        product.setBarcode(barcode);
        product.setName(textOrDefault(productNode, "product_name", "Unknown product"));
        product.setBrand(textOrNull(productNode, "brands"));
        product.setImageUrl(textOrNull(productNode, "image_url"));
        product.setNutriscoreGrade(lowerTextOrNull(productNode, "nutriscore_grade"));
        product.setIngredientsText(textOrNull(productNode, "ingredients_text"));
        product.setCurrencyCode(CURRENCY_MAD);
        product.setStockQuantity(100);

        Integer aisle = inferAisle(productNode);
        product.setAisleNumber(aisle);
        product.setPrice(PRICE_BY_AISLE.getOrDefault(aisle, DEFAULT_PRICE_MAD));
        product.setRfidSecurityRequired(aisle != null && (aisle == 11 || aisle == 12));

        product.setAllergenCodes(parseAllergens(productNode));
        product.setNutriments(parseNutriments(productNode));
        product.setAttributes(buildAttributes(productNode));
        return product;
    }

    private Integer inferAisle(JsonNode productNode) {
        JsonNode tags = productNode.path("categories_tags");
        if (!tags.isArray()) return DEFAULT_AISLE;
        // Iterate in reverse so the most specific tag (last in OFF order) wins.
        for (int i = tags.size() - 1; i >= 0; i--) {
            String tag = tags.get(i).asText();
            Integer aisle = AISLE_BY_CATEGORY.get(tag);
            if (aisle != null) return aisle;
        }
        return DEFAULT_AISLE;
    }

    private String[] parseAllergens(JsonNode productNode) {
        JsonNode tags = productNode.path("allergens_tags");
        if (!tags.isArray() || tags.isEmpty()) return new String[0];
        List<String> mapped = new ArrayList<>(tags.size());
        for (JsonNode tag : tags) {
            String label = ALLERGEN_MAP.get(tag.asText());
            if (label != null && !mapped.contains(label)) mapped.add(label);
        }
        return mapped.toArray(String[]::new);
    }

    private Map<String, Object> buildAttributes(JsonNode productNode) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("unit", "ea");
        attrs.put("dietary", parseDietary(productNode));
        return attrs;
    }

    private List<String> parseDietary(JsonNode productNode) {
        List<String> matched = new ArrayList<>();
        collectDietary(productNode.path("labels_tags"), matched);
        // Fallback for products that declare vegan/vegetarian only in ingredients_analysis_tags
        collectDietary(productNode.path("ingredients_analysis_tags"), matched);
        return matched;
    }

    private void collectDietary(JsonNode tags, List<String> sink) {
        if (!tags.isArray()) return;
        for (JsonNode tag : tags) {
            String label = DIETARY_MAP.get(tag.asText());
            if (label != null && !sink.contains(label)) sink.add(label);
        }
    }

    private Map<String, Object> parseNutriments(JsonNode productNode) {
        JsonNode node = productNode.path("nutriments");
        if (node.isMissingNode() || !node.isObject()) return null;
        Map<String, Object> nutriments = new HashMap<>();
        nutriments.put("energy_kcal", node.path("energy-kcal_100g").asDouble(0));
        nutriments.put("fat", node.path("fat_100g").asDouble(0));
        nutriments.put("carbohydrates", node.path("carbohydrates_100g").asDouble(0));
        nutriments.put("proteins", node.path("proteins_100g").asDouble(0));
        nutriments.put("salt", node.path("salt_100g").asDouble(0));
        nutriments.put("sugars", node.path("sugars_100g").asDouble(0));
        return nutriments;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private String lowerTextOrNull(JsonNode node, String field) {
        String value = textOrNull(node, field);
        return value == null ? null : value.toLowerCase();
    }

    private String textOrDefault(JsonNode node, String field, String fallback) {
        String value = textOrNull(node, field);
        return value != null ? value : fallback;
    }

    private static Map<String, Integer> orderedMap(Object... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("expected key/value pairs");
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }
}
