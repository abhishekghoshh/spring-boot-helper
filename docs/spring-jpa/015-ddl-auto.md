### spring.jpa.hibernate.ddl-auto

This property controls what Hibernate does to the database **schema** at application startup. It compares entity classes against the existing tables and decides what DDL to execute.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update   # one of: none, validate, update, create, create-drop
```

**All options explained**

| Value | What it does | SQL generated at startup | Data preserved? |
|---|---|---|---|
| `none` | Does absolutely nothing to the schema | None | Yes |
| `validate` | Compares entities against DB schema, throws exception if mismatch | None (read-only check) | Yes |
| `update` | Adds missing tables/columns, never drops anything | `ALTER TABLE ADD COLUMN ...`, `CREATE TABLE ...` | Yes |
| `create` | Drops all managed tables, then re-creates them | `DROP TABLE ...`, `CREATE TABLE ...` | **No — all data lost** |
| `create-drop` | Same as `create`, but also drops tables when app shuts down | `DROP TABLE` on startup + shutdown | **No — all data lost** |

**Detailed breakdown**

**1. `none`**

- Hibernate does not touch the schema at all.
- You manage schema externally via Flyway, Liquibase, or manual SQL.
- **Use in**: Production, always. Schema changes should be version-controlled migrations, not auto-generated.

```yaml
# Production
spring.jpa.hibernate.ddl-auto=none
# Combined with Flyway or Liquibase for migrations
```

**2. `validate`**

- Hibernate checks that every `@Entity`, `@Table`, `@Column` annotation matches the actual DB schema.
- If a table or column is missing, the app **fails to start** with `SchemaManagementException`.
- Does not modify the database.
- **Use in**: Staging/pre-production to catch mismatches between code and schema after a migration.

```text
Entity: @Column(name = "email") on User
DB:     users table has no "email" column

Result: App fails at startup → SchemaManagementException:
        "Schema-validation: missing column [email] in table [users]"
```

**3. `update`**

- Hibernate scans entities and adds any missing tables or columns.
- **Never drops** columns, tables, or constraints (even if you removed the field from the entity).
- May generate `ALTER TABLE ADD COLUMN`, `CREATE TABLE`, `CREATE INDEX`.
- **Use in**: Local development for quick iteration.

```text
You add a new field to User:
   private String phone;

At startup, Hibernate runs:
   ALTER TABLE users ADD COLUMN phone VARCHAR(255);

You REMOVE the phone field from User:
   Hibernate does NOTHING — the column stays in the DB (orphaned column)
```

**4. `create`**

- Drops all tables Hibernate manages, then re-creates them from scratch.
- **All data is destroyed** on every startup.
- **Use in**: Unit testing with in-memory databases (H2).

```text
Startup:
   DROP TABLE IF EXISTS users CASCADE;
   DROP TABLE IF EXISTS addresses CASCADE;
   CREATE TABLE users (id BIGINT PRIMARY KEY, name VARCHAR(255), ...);
   CREATE TABLE addresses (id BIGINT PRIMARY KEY, ...);
```

**5. `create-drop`**

- Same as `create`, plus drops all tables when the `SessionFactory` is closed (app shutdown).
- **Use in**: Integration tests where you need a clean database per test run.

```text
Startup:   DROP + CREATE all tables
Shutdown:  DROP all tables
```

**Real-life usage by environment**

```text
┌────────────────┬──────────────┐
│  Environment   │  ddl-auto    │
├────────────────┼──────────────┤
│  Local dev     │  update      │
│  Unit tests    │  create-drop │
│  Integration   │  create-drop │
│  Staging       │  validate    │
│  Production    │  none        │
└────────────────┴──────────────┘
```

---

