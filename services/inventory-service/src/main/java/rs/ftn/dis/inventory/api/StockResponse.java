package rs.ftn.dis.inventory.api;

import rs.ftn.dis.inventory.domain.InventoryItem;

public record StockResponse(String productId, int quantity, int reserved, int available) {

    public static StockResponse from(InventoryItem item) {
        return new StockResponse(item.getProductId(), item.getQuantity(), item.getReserved(), item.getAvailable());
    }
}
