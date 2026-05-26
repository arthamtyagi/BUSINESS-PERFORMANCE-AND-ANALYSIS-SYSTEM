package com.vynex.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.sql.*;
/**
 * DataSeeder — runs once on startup.
 * Inserts sample products if the products table is empty.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private final String dbUrl;

    public DataSeeder(@Value("${spring.datasource.url}") String dbUrl) {
        this.dbUrl = dbUrl;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        String sql = "INSERT INTO products (name, description, category, sellingPrice, manufacturingCost, discountPercent, stockQuantity, supplierName) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Object[][] products = {
            {"Vynex Analytics Pro",    "Advanced business analytics suite with real-time dashboards.",     "Software",    4999.00, 1200.00, 10.0, 50,  "Vynex Labs"},
            {"Smart Inventory Kit",    "Complete inventory tracking system for small businesses.",          "Hardware",    8499.00, 3500.00, 5.0,  30,  "TechSupply Co."},
            {"Data Insights Bundle",   "Monthly subscription for AI-powered sales and trend reports.",     "Subscription",1999.00, 400.00,  15.0, 100, "Vynex Labs"},
            {"Business Starter Pack",  "Everything a new business needs — analytics, CRM, and reports.",   "Bundle",      12999.00,5000.00, 8.0,  20,  "Vynex Partners"},
            {"Cloud Storage 1TB",      "Secure cloud storage for business documents and data backups.",    "Cloud",       2499.00, 800.00,  0.0,  200, "CloudBase Inc."},
            {"CRM Module",             "Customer relationship management tool integrated with Vynex.",     "Software",    3499.00, 900.00,  12.0, 75,  "Vynex Labs"},
            {"Sales Report Generator", "Automated weekly and monthly sales report generation tool.",       "Software",    1499.00, 300.00,  0.0,  150, "ReportWorks"},
            {"Employee Dashboard",     "Track employee performance, attendance and productivity metrics.", "HR Tools",    5999.00, 2000.00, 5.0,  40,  "HRTech Solutions"}
        };

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Object[] p : products) {
                ps.setString(1, (String)  p[0]);
                ps.setString(2, (String)  p[1]);
                ps.setString(3, (String)  p[2]);
                ps.setDouble(4, (Double)  p[3]);
                ps.setDouble(5, (Double)  p[4]);
                ps.setDouble(6, (Double)  p[5]);
                ps.setInt   (7, (Integer) p[6]);
                ps.setString(8, (String)  p[7]);
                ps.addBatch();
            }
            ps.executeBatch();
            System.out.println("[DataSeeder] Inserted " + products.length + " sample products.");
        }
    }
}
