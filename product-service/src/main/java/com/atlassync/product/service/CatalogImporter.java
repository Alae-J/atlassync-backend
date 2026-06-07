package com.atlassync.product.service;

import com.atlassync.product.entity.Product;
import com.atlassync.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * Pulls a curated list of real product barcodes from Open Food Facts on first
 * boot so the catalog feels like a real supermarket rather than 14 hand-rolled
 * demo items. Internationally-sold-in-Morocco brands picked for OFF coverage
 * (Nutella, Évian, Coca, Barilla, Kit Kat etc.) plus a handful of Moroccan
 * brands — OFF coverage on local brands is patchy, so we fail-soft on misses.
 *
 * <p>Runs in a background thread off ApplicationReadyEvent so app startup
 * isn't blocked by ~25 sequential HTTP calls. Skips any barcode already in
 * the DB, so this is safe to leave enabled across restarts.
 */
@Component
public class CatalogImporter {

    private static final Logger log = LoggerFactory.getLogger(CatalogImporter.class);

    /**
     * Curated barcodes. Mixes well-indexed international brands you'd find on
     * any Moroccan supermarket shelf with a few local brands. OFF misses
     * (barcode unknown to OFF) get logged at debug and skipped.
     */
    private static final List<String> CURATED_BARCODES = List.of(
            // Spreads / sweets
            "3017620422003",   // Nutella 400g
            "7622210449283",   // Oreo
            "8000500037560",   // Kinder Bueno
            "4008400404127",   // Ferrero Rocher T16
            "7613036930635",   // KitKat 4-finger
            // Beverages
            "5449000000996",   // Coca-Cola 33cl
            "5449000054227",   // Coca-Cola Zero 33cl
            "3068320115008",   // Évian 1.5L
            "3274080005003",   // Évian 33cl
            "7613034626844",   // Lipton Ice Tea
            // Pantry
            "8076809513753",   // Barilla spaghetti n.5
            "8076809529433",   // Barilla penne rigate
            "3017800238400",   // Lustucru pasta
            // Dairy
            "3033710065608",   // President butter
            "3228857000852",   // Harrys bread (in dairy aisle in some stores)
            "3175680011480",   // Carrefour milk
            // Snacks
            "5410908035915",   // Lay's Classic
            "7622210992338",   // Belvita
            "5000159484695",   // Mars bar
            "5000159461122",   // Twix
            // Moroccan brands (OFF coverage patchy — fail-soft on misses)
            "6111035000379",   // Centrale Danone
            "6111245000016",   // Aiguebelle chocolate
            "6111021000174",   // Bahia couscous
            "6111250000018",   // Bimo
            "6111000200013"    // Sidi Ali water
    );

    private final ProductRepository productRepository;
    private final OpenFoodFactsClient openFoodFactsClient;
    private final boolean enabled;

    public CatalogImporter(ProductRepository productRepository,
                           OpenFoodFactsClient openFoodFactsClient,
                           @Value("${catalog.import-on-boot:true}") boolean enabled) {
        this.productRepository = productRepository;
        this.openFoodFactsClient = openFoodFactsClient;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void importOnStartup() {
        if (!enabled) {
            log.info("Catalog importer disabled — skipping OFF seed.");
            return;
        }
        // Background thread so app accepts traffic immediately. The catalog
        // simply fills in over the next ~30s.
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "catalog-importer");
            t.setDaemon(true);
            return t;
        }).execute(this::runImport);
    }

    private void runImport() {
        log.info("Catalog importer starting · {} curated barcodes", CURATED_BARCODES.size());
        int imported = 0, skipped = 0, missed = 0;
        for (String barcode : CURATED_BARCODES) {
            if (productRepository.findByBarcode(barcode).isPresent()) {
                skipped++;
                continue;
            }
            Optional<Product> fetched = openFoodFactsClient.fetchProduct(barcode);
            if (fetched.isEmpty()) {
                missed++;
                continue;
            }
            try {
                productRepository.save(fetched.get());
                imported++;
            } catch (Exception e) {
                log.warn("Failed to save imported product {}: {}", barcode, e.getMessage());
            }
        }
        log.info("Catalog importer done · imported={} skipped={} missed={}",
                imported, skipped, missed);
    }
}
