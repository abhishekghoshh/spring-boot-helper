# Structured Query Language(SQL)

## SQL Fundamentals

- What is SQL?
- SQL Standards
- SQL Dialects (PostgreSQL, MySQL, Oracle, SQL Server)
- SQL Statement Categories (DDL, DML, DQL, DCL, TCL)
- SQL Execution Order

## Database Objects

- Databases
- Schemas
- Tables
- Views
- Materialized Views (Overview)
- Sequences
- Indexes

## Data Types

- Numeric Types
- Character Types
- Boolean Type
- Date and Time Types
- UUID
- JSON / JSONB (Overview)
- Binary Data Types

## Data Definition Language (DDL)

- CREATE DATABASE
- CREATE SCHEMA
- CREATE TABLE
- ALTER TABLE
- DROP TABLE
- TRUNCATE TABLE
- RENAME TABLE
- COMMENT

## Constraints

- PRIMARY KEY
- FOREIGN KEY
- UNIQUE
- NOT NULL
- CHECK
- DEFAULT
- AUTO_INCREMENT / IDENTITY / SERIAL
- Generated Columns (Overview)

## Data Manipulation Language (DML)

- INSERT
- INSERT Multiple Rows
- INSERT ... SELECT
- UPDATE
- DELETE
- MERGE / UPSERT (Database Specific)
- RETURNING Clause (PostgreSQL)

## Basic Queries

- SELECT
- DISTINCT
- WHERE
- ORDER BY
- LIMIT
- OFFSET
- FETCH FIRST
- Aliases

## Filtering Data

- Comparison Operators
- Logical Operators
- BETWEEN
- IN
- NOT IN
- LIKE
- ILIKE (PostgreSQL)
- IS NULL
- IS NOT NULL
- EXISTS
- NOT EXISTS
- ANY
- ALL

## SQL Functions

- String Functions
- Numeric Functions
- Date Functions
- Time Functions
- Conversion Functions
- NULL Handling Functions
- Conditional Functions

## Aggregate Functions

- COUNT
- SUM
- AVG
- MIN
- MAX
- DISTINCT Aggregates

## GROUP BY

- GROUP BY
- HAVING
- Multiple Grouping Columns
- GROUP BY with Aggregate Functions

## Joins

- INNER JOIN
- LEFT JOIN
- RIGHT JOIN
- FULL OUTER JOIN
- CROSS JOIN
- SELF JOIN
- NATURAL JOIN (Concept)
- Join Execution Basics

## Set Operations

- UNION
- UNION ALL
- INTERSECT
- EXCEPT / MINUS

## Subqueries

- Scalar Subqueries
- Row Subqueries
- Table Subqueries
- Correlated Subqueries
- EXISTS vs IN

## Common Table Expressions (CTE)

- WITH Clause
- Recursive CTE (Overview)
- Multiple CTEs

## Window Functions

- OVER Clause
- PARTITION BY
- ORDER BY in Window Functions
- ROW_NUMBER
- RANK
- DENSE_RANK
- LEAD
- LAG
- FIRST_VALUE
- LAST_VALUE
- Running Totals

## Views

- Creating Views
- Updating Views
- View Limitations
- Materialized Views (Concept)

## Indexes

- CREATE INDEX
- DROP INDEX
- Composite Index
- Unique Index
- Partial Index (Overview)
- Functional Index (Overview)
- Index Usage
- When Not to Use Indexes

## Transactions

- BEGIN
- COMMIT
- ROLLBACK
- SAVEPOINT
- Transaction Isolation Levels
- Auto Commit
- Locking Basics

## Query Optimization

- Execution Plan (EXPLAIN)
- EXPLAIN ANALYZE
- Index Scan
- Sequential Scan
- Join Strategies
- Cost-Based Optimization
- Query Performance Tuning Basics

## SQL and Relationships

- One-to-One Queries
- One-to-Many Queries
- Many-to-Many Queries
- Junction Tables
- Foreign Key Navigation

## Data Integrity

- Cascading Deletes
- Cascading Updates
- Restrict
- No Action
- Set Null
- Set Default

## Stored Database Objects (Awareness)

- Stored Procedures (Overview)
- Functions
- Triggers (Overview)
- Sequences

## Pagination

- LIMIT / OFFSET
- Keyset Pagination (Concept)
- Cursor-Based Pagination (Concept)

## JSON Support (PostgreSQL Focus)

- JSON
- JSONB
- JSON Operators
- Querying JSON Fields
- Updating JSON Data

## SQL Features Used Frequently with Spring JPA

- CRUD Operations
- Parameterized Queries
- Named Parameters (Concept)
- Native SQL Queries
- Sorting
- Pagination
- Batch Inserts
- Batch Updates
- Batch Deletes
- EXISTS Queries
- CASE Expressions
- COALESCE
- NULLIF

## Advanced SQL Concepts (Recommended)

- Recursive Queries
- Lateral Joins (Overview)
- Common Performance Pitfalls
- N+1 Query Problem (SQL Perspective)
- Optimistic vs Pessimistic Locking (SQL Perspective)