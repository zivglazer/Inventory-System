# README
# Inventory and Procurement Management System

A Java inventory and supplier procurement management system, developed as part of the Analysis and Design of Software Systems course at Ben Gurion University of the Negev. The system manages products, stock levels, suppliers and purchase agreements, and automatically issues purchase orders when inventory falls below defined minimum levels. It was built through a full analysis and design phase, including UML modeling and formal system contracts, before any code was written.

## Tech Stack

* **Java** for the domain model, services and application logic
* **SQL (SQLite via JDBC)** for persistent storage of inventory, suppliers and orders
* **UML** for use case, class, sequence and activity modeling
* **Object Oriented Design** with layered architecture, DAO and DTO patterns, and controller based domain logic
* **JUnit** for automated testing across all layers

## Architecture

The system follows a strict layered architecture in which every layer depends only on the layer beneath it.

**Presentation Layer**
Console based menus for product management, category management, inventory counting, reports and orders. Contains no business rules and communicates with the system exclusively through the service layer.

**Service Layer**
Defines the public API of the system. `InventoryService`, `OrderService` and `SupplierService` expose the operations described in the system contracts, while `IntegrationService` orchestrates the cross module workflows that span inventory, orders and suppliers. Every operation returns a uniform `Response` object carrying either a result or a failure message, so errors never propagate as exceptions to the caller.

**Domain Layer**
Holds the business rules and the object model: products, items, categories, shelf locations, discounts, suppliers, agreements and the order hierarchy (scheduled orders, shortage orders and dispatched orders). Domain objects own their own rules, for example a product computes its own replenishment quantity when it drops below its minimum level.

**Data Access Layer**
DAO classes per entity, DTO objects for row level transfer and mappers that translate between records and domain objects. A single shared JDBC connection, a bounded identity cache per table and transactional multi table operations keep the database as the system of record while limiting memory use.

## Features

* **Automated shortage detection.** The system continuously identifies every product whose available stock falls below its minimum level and issues purchase orders for the exact replenishment quantity required.
* **Automatic supplier selection.** Shortage orders are grouped by supplier and routed to the cheapest supplier for each product based on active supply agreements and quantity discounts.
* **Scheduled procurement workflow.** Periodic order templates are stored per supplier and delivery day. A daily background scheduler converts every template due for the next delivery day into a real dispatched order without user intervention.
* **Inventory management.** Products, categories and subcategories, item level tracking, warehouse and store shelf locations, expiry dates and defective item reporting.
* **Supplier and agreement management.** Suppliers, delivery days, supply agreements, per product pricing and quantity based discounts.
* **Reporting.** Inventory reports by category, expired item reports, defect reports and order reports.
* **Expected inventory.** Open shortage orders are taken into account so the same product is not ordered twice while a delivery is already on its way.
* **Persistence.** All data is stored in a relational database and survives restarts, with an optional preloaded sample dataset for demonstration.

## Design and Documentation

The full design was produced and reviewed before implementation, and the code was written to match it. All artifacts are available under `docs/`.

* **Requirements analysis** capturing the functional requirements the system implements
* **Use Case Diagrams** and detailed use case scenarios for the procurement flows
* **Class Diagram** covering the complete domain model and the relationships between layers
* **Sequence Diagrams** for the key operations, including `addScheduledOrder`, `addProductToOrder` and `addShortageOrder`
* **Activity Diagrams** for the periodic ordering flow and the shortage ordering flow
* **System Contracts** specifying preconditions, postconditions and responsibilities for every core operation

## Testing

The system is covered by an automated test suite that runs against an isolated in memory database, so tests are repeatable and independent of the production data file.

* **Unit tests** for domain rules such as stock thresholds, replenishment quantities, discount calculation and order state transitions
* **Service tests** verifying the service layer API, including validation and failure responses
* **Integration tests** covering the cross module workflows: shortage detection to purchase order, scheduled template to dispatched order, and supplier selection by price
* **System tests** driving the application through the presentation layer to validate complete end to end scenarios

## How to Run

> Placeholder: build and run instructions to be added.

```bash
# Requirements: Java (JDK 8 or later)

# Run the packaged application
java -jar release/adss2025_v02.jar
```

On startup the application offers a live mode and a test mode preloaded with sample data.

## Repository Structure

```
dev/src/presentation    Console user interface
dev/src/service         Service layer and system API
dev/src/domain          Domain model and business logic
dev/src/dataaccess      DAO, DTO, mappers, schema and caching
dev/test                Unit, service, integration and system tests
docs                    Requirements, contracts and UML documentation
release                 Packaged application and database file
```

## Author

Developed by Ziv Glazer as part of a three person team for the Analysis and Design of Software Systems course, Ben Gurion University of the Negev.


- **draw.io** — תרשים Use Case ותרשים מחלקות (class diagram),תרשימי רצף (sequence diagrams)

## מבנה ההגשה

```
docs/
├── cases-use/
│   ├── use-case-diagram.pdf        # תרשים Use Case (סעיף 1.1)
│   ├── use-case-diagram.xml        # XML של תרשים Use Case
│   ├── use-case-e.pdf              # תרחיש e: יצירת הזמנה תקופתית מספק
│   ├── use-case-f.pdf              # תרחיש f: הוצאת הזמנה מספק עקב חוסר
│   ├── e-main.pdf                  # Activity Diagram — תרחיש e
│   └── f-main.pdf                  # Activity Diagram — תרחיש f
├── contracts/
│   ├── contract-addScheduledOrder.pdf     # חוזה 1: יצירת תבנית הזמנה תקופתית
│   ├── contract-addProductToOrder.pdf     # חוזה 2: הוספת מוצר להזמנה תקופתית
│   └── contract-addInventoryOrders.pdf    # חוזה 3: יצירת הזמנות רכש עקב חוסר
├── diagrams-sequence/
│   ├── seq-diagram-addScheduledOrder.pdf  # תרשים רצף — addScheduledOrder
│   ├── seq-diagram-addProductToOrder.pdf  # תרשים רצף — addProductToOrder
│   └── seq-diagram-addInventoryOrders.pdf # תרשים רצף — addInventoryOrders
├── class-diagram.pdf               # תרשים מחלקות מעודכן
├── class-diagram.xml               # XML תרשים מחלקות (draw.io)
├── requirements.pdf                # מסמך דרישות מעודכן
└── README.md                       # קובץ זה
