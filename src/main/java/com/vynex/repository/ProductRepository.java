package com.vynex.repository;

import com.vynex.model.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Raw JDBC repository for products and orders.
 * Creates both tables on startup if they don't exist.
 */
@Repository
public class ProductRepository {

    private final String dbUrl;

    public ProductRepository(@Value("${spring.datasource.url}") String dbUrl) {
        this.dbUrl = dbUrl;
        initTables();
    }

    private void initTables() {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            // Products table
            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "description TEXT," +
                    "category TEXT," +
                    "sellingPrice REAL," +
                    "manufacturingCost REAL," +
                    "discountPercent REAL," +
                    "stockQuantity INTEGER," +
                    "supplierName TEXT)");

            // Orders table — stores every customer purchase for analytics
            stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "productId INTEGER," +
                    "productName TEXT," +
                    "customerEmail TEXT," +
                    "quantity INTEGER," +
                    "deliveryAddress TEXT," +
                    "totalPrice REAL," +
                    "orderDate TEXT)");

            System.out.println("[JDBC] Products and Orders tables ready.");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialise product/order tables", e);
        }
    }

    // ── Save a new product ────────────────────────────────────────────────────
    public void saveProduct(Product p) {
        String sql = "INSERT INTO products " +
                "(name, description, category, sellingPrice, manufacturingCost, discountPercent, stockQuantity, supplierName) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getCategory());
            ps.setDouble(4, p.getSellingPrice() != null ? p.getSellingPrice() : 0);
            ps.setDouble(5, p.getManufacturingCost() != null ? p.getManufacturingCost() : 0);
            ps.setDouble(6, p.getDiscountPercent() != null ? p.getDiscountPercent() : 0);
            ps.setInt   (7, p.getStockQuantity() != null ? p.getStockQuantity() : 0);
            ps.setString(8, p.getSupplierName());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("DB error saving product", e);
        }
    }

    // ── Get all products ──────────────────────────────────────────────────────
    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE stockQuantity > 0";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Product p = new Product();
                p.setId(rs.getLong("id"));
                p.setName(rs.getString("name"));
                p.setDescription(rs.getString("description"));
                p.setCategory(rs.getString("category"));
                p.setSellingPrice(rs.getDouble("sellingPrice"));
                p.setManufacturingCost(rs.getDouble("manufacturingCost"));
                p.setDiscountPercent(rs.getDouble("discountPercent"));
                p.setStockQuantity(rs.getInt("stockQuantity"));
                p.setSupplierName(rs.getString("supplierName"));
                list.add(p);
            }

        } catch (SQLException e) {
            throw new RuntimeException("DB error fetching products", e);
        }
        return list;
    }

    // ── Save an order ─────────────────────────────────────────────────────────
    public void saveOrder(com.vynex.model.Order order) {
        String sql = "INSERT INTO orders " +
                "(productId, productName, customerEmail, quantity, deliveryAddress, totalPrice, orderDate) " +
                "VALUES (?, ?, ?, ?, ?, ?, datetime('now'))";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong  (1, order.getProductId());
            ps.setString(2, order.getProductName());
            ps.setString(3, order.getCustomerEmail());
            ps.setInt   (4, order.getQuantity());
            ps.setString(5, order.getDeliveryAddress());
            ps.setDouble(6, order.getTotalPrice());
            ps.executeUpdate();

            // Reduce stock quantity
            try (PreparedStatement upd = conn.prepareStatement(
                    "UPDATE products SET stockQuantity = stockQuantity - ? WHERE id = ?")) {
                upd.setInt (1, order.getQuantity());
                upd.setLong(2, order.getProductId());
                upd.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException("DB error saving order", e);
        }
    }

    // ── Business summary stats ────────────────────────────────────────────────
    public java.util.Map<String, Object> getBusinessSummary() {
        java.util.Map<String, Object> summary = new java.util.LinkedHashMap<>();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            // Total revenue from all orders
            ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(totalPrice), 0) FROM orders");
            double totalRevenue = rs.next() ? rs.getDouble(1) : 0;

            // Total manufacturing cost of sold items
            ResultSet rs2 = stmt.executeQuery(
                "SELECT COALESCE(SUM(o.quantity * p.manufacturingCost), 0) " +
                "FROM orders o JOIN products p ON o.productId = p.id");
            double totalCost = rs2.next() ? rs2.getDouble(1) : 0;

            double totalProfit = totalRevenue - totalCost;
            double totalLoss   = totalProfit < 0 ? Math.abs(totalProfit) : 0;

            // Growth rate: orders this month vs last month
            ResultSet rs3 = stmt.executeQuery(
                "SELECT COUNT(*) FROM orders WHERE strftime('%Y-%m', orderDate) = strftime('%Y-%m', 'now')");
            int thisMonth = rs3.next() ? rs3.getInt(1) : 0;

            ResultSet rs4 = stmt.executeQuery(
                "SELECT COUNT(*) FROM orders WHERE strftime('%Y-%m', orderDate) = strftime('%Y-%m', date('now', '-1 month'))");
            int lastMonth = rs4.next() ? rs4.getInt(1) : 0;

            double growthRate = lastMonth > 0 ? ((thisMonth - lastMonth) * 100.0 / lastMonth) : (thisMonth > 0 ? 100.0 : 0);

            // Best performing month
            ResultSet rs5 = stmt.executeQuery(
                "SELECT strftime('%Y-%m', orderDate) as month, SUM(totalPrice) as rev " +
                "FROM orders GROUP BY month ORDER BY rev DESC LIMIT 1");
            String bestMonth = rs5.next() ? rs5.getString("month") : "N/A";

            // Top product by revenue
            ResultSet rs6 = stmt.executeQuery(
                "SELECT productName, SUM(totalPrice) as rev FROM orders GROUP BY productName ORDER BY rev DESC LIMIT 1");
            String topProduct = rs6.next() ? rs6.getString("productName") : "N/A";

            // Total active products
            ResultSet rs7 = stmt.executeQuery("SELECT COUNT(*) FROM products WHERE stockQuantity > 0");
            int activeProducts = rs7.next() ? rs7.getInt(1) : 0;

            summary.put("totalRevenue",    totalRevenue);
            summary.put("totalProfit",     Math.max(totalProfit, 0));
            summary.put("totalLoss",       totalLoss);
            summary.put("growthRate",      Math.round(growthRate * 10.0) / 10.0);
            summary.put("bestMonth",       bestMonth);
            summary.put("topProduct",      topProduct);
            summary.put("activeProducts",  activeProducts);

        } catch (SQLException e) {
            throw new RuntimeException("DB error fetching summary", e);
        }
        return summary;
    }

    // ── Per-product analytics ─────────────────────────────────────────────────
    public java.util.List<java.util.Map<String, Object>> getProductStats() {
        java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();

        String sql = "SELECT p.id, p.name, p.sellingPrice, p.manufacturingCost, p.discountPercent, " +
                "COALESCE(SUM(o.quantity), 0) as totalSold, " +
                "COALESCE(SUM(o.totalPrice), 0) as revenue " +
                "FROM products p LEFT JOIN orders o ON p.id = o.productId " +
                "GROUP BY p.id ORDER BY revenue DESC";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                java.util.Map<String, Object> stat = new java.util.LinkedHashMap<>();
                double revenue  = rs.getDouble("revenue");
                int    sold     = rs.getInt("totalSold");
                double cost     = rs.getDouble("manufacturingCost") * sold;
                double profit   = revenue - cost;

                stat.put("id",          rs.getLong("id"));
                stat.put("name",        rs.getString("name"));
                stat.put("revenue",     Math.round(revenue * 100.0) / 100.0);
                stat.put("profit",      Math.round(Math.max(profit, 0) * 100.0) / 100.0);
                stat.put("loss",        Math.round(profit < 0 ? Math.abs(profit) : 0 * 100.0) / 100.0);
                stat.put("totalSold",   sold);
                stat.put("growth",      sold > 10 ? "High Growth" : sold > 3 ? "Stable" : "Needs Attention");
                stat.put("trend",       sold > 5 ? "Upward" : "Flat");
                list.add(stat);
            }

        } catch (SQLException e) {
            throw new RuntimeException("DB error fetching product stats", e);
        }
        return list;
    }

    // ── Orders per day (last 7 days) for line chart ───────────────────────────
    public java.util.List<java.util.Map<String, Object>> getOrdersLast7Days() {
        java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();

        String sql = "SELECT strftime('%d/%m', orderDate) as day, " +
                "COUNT(*) as orders, COALESCE(SUM(totalPrice), 0) as revenue " +
                "FROM orders " +
                "WHERE orderDate >= date('now', '-7 days') " +
                "GROUP BY day ORDER BY orderDate ASC";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                java.util.Map<String, Object> day = new java.util.LinkedHashMap<>();
                day.put("day",     rs.getString("day"));
                day.put("orders",  rs.getInt("orders"));
                day.put("revenue", rs.getDouble("revenue"));
                list.add(day);
            }

        } catch (SQLException e) {
            throw new RuntimeException("DB error fetching orders chart data", e);
        }
        return list;
    }
}
