package dataaccess;

import java.util.Arrays;
import java.util.List;

/**
 * The database schema as plain DDL. {@link Database} runs {@link #CREATE} on startup and
 * uses {@link #TABLES_CHILD_FIRST} to drop everything in dependency order on reset.
 * Dates are stored as ISO-8601 TEXT; the three inventory item lists collapse into one
 * {@code item} table with a {@code location_state} column.
 */
final class Schema {

    private Schema() {}

    static final List<String> CREATE = Arrays.asList(
        "CREATE TABLE IF NOT EXISTS category (" +
        "  name TEXT PRIMARY KEY" +
        ")",

        "CREATE TABLE IF NOT EXISTS shelf_location (" +
        "  id    INTEGER PRIMARY KEY," +
        "  label TEXT NOT NULL" +
        ")",

        "CREATE TABLE IF NOT EXISTS product (" +
        "  product_id             INTEGER PRIMARY KEY," +
        "  name                   TEXT NOT NULL," +
        "  manufacturer           TEXT NOT NULL," +
        "  category_name          TEXT NOT NULL REFERENCES category(name)," +
        "  sub_category           TEXT," +
        "  sub_sub_category       TEXT," +
        "  shelf_location_id      INTEGER REFERENCES shelf_location(id)," +
        "  supplier_id            INTEGER NOT NULL DEFAULT 0," +
        "  min_to_restock         INTEGER NOT NULL," +
        "  price_without_discount INTEGER NOT NULL" +
        ")",

        "CREATE TABLE IF NOT EXISTS item (" +
        "  item_id        INTEGER PRIMARY KEY," +
        "  product_id     INTEGER NOT NULL REFERENCES product(product_id)," +
        "  cost_price     INTEGER NOT NULL," +
        "  expiration     TEXT NOT NULL," +
        "  location_state TEXT NOT NULL" +          // STORAGE | SHELF | EXPIRED
        ")",

        "CREATE TABLE IF NOT EXISTS sold_item (" +
        "  item_id    INTEGER PRIMARY KEY," +
        "  sell_date  TEXT NOT NULL," +
        "  sell_price REAL NOT NULL," +
        "  cost_price INTEGER NOT NULL" +
        ")",

        "CREATE TABLE IF NOT EXISTS defect_item (" +
        "  item_id     INTEGER PRIMARY KEY," +
        "  cost_price  INTEGER NOT NULL," +
        "  expiration  TEXT," +
        "  reason      TEXT," +
        "  update_date TEXT NOT NULL" +
        ")",

        "CREATE TABLE IF NOT EXISTS discount (" +
        "  id            INTEGER PRIMARY KEY AUTOINCREMENT," +
        "  product_id    INTEGER REFERENCES product(product_id)," +
        "  category_name TEXT REFERENCES category(name)," +
        "  percentage    INTEGER NOT NULL," +
        "  start_date    TEXT NOT NULL," +
        "  end_date      TEXT NOT NULL" +
        ")",

        "CREATE TABLE IF NOT EXISTS orders (" +
        "  order_id    INTEGER PRIMARY KEY," +
        "  type        TEXT NOT NULL," +            // SHORTAGE | SCHEDULED | DISPATCHED (discriminator)
        "  supplier_id INTEGER NOT NULL," +
        "  status      TEXT NOT NULL," +            // CREATED | SENT | RECEIVED
        "  order_date  TEXT NOT NULL" +
        ")",

        "CREATE TABLE IF NOT EXISTS order_line (" +
        "  order_id   INTEGER NOT NULL REFERENCES orders(order_id)," +
        "  product_id INTEGER NOT NULL," +
        "  quantity   INTEGER NOT NULL," +
        "  unit_price INTEGER NOT NULL DEFAULT 0," +
        "  PRIMARY KEY (order_id, product_id)" +
        ")",

        "CREATE TABLE IF NOT EXISTS order_delivery_day (" +
        "  order_id INTEGER NOT NULL REFERENCES orders(order_id)," +
        "  day      INTEGER NOT NULL," +
        "  PRIMARY KEY (order_id, day)" +
        ")",

        "CREATE TABLE IF NOT EXISTS supplier (" +
        "  supplier_id INTEGER PRIMARY KEY," +
        "  name        TEXT," +
        "  address     TEXT," +
        "  phone       TEXT," +
        "  contact     TEXT" +
        ")",

        "CREATE TABLE IF NOT EXISTS supplier_delivery_day (" +
        "  supplier_id INTEGER NOT NULL REFERENCES supplier(supplier_id)," +
        "  day         INTEGER NOT NULL," +
        "  PRIMARY KEY (supplier_id, day)" +
        ")",

        "CREATE TABLE IF NOT EXISTS agreement (" +
        "  supplier_id     INTEGER NOT NULL REFERENCES supplier(supplier_id)," +
        "  product_name    TEXT NOT NULL," +    // product NAME is the universal cross-module identifier
        "  unit_price      INTEGER NOT NULL," +
        "  bulk_threshold  INTEGER NOT NULL," +
        "  bulk_unit_price INTEGER NOT NULL," +
        "  PRIMARY KEY (supplier_id, product_name)" +
        ")"
    );

    /** Child tables first so DROP respects foreign keys. */
    static final List<String> TABLES_CHILD_FIRST = Arrays.asList(
        "agreement", "supplier_delivery_day", "supplier",
        "order_delivery_day", "order_line", "orders",
        "discount", "defect_item", "sold_item", "item",
        "product", "shelf_location", "category"
    );
}
