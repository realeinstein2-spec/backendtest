# MakersHub Database ER Diagram & Relationships

## Entities

```
users ||--o| factories : owns
users ||--o{ job_listings : posts
users ||--o{ orders : sme
users ||--o{ orders : factory
users ||--o{ reviews : reviewer
users ||--o{ reviews : reviewed
users ||--o{ messages : sends
users ||--o{ disputes : raised_by
users ||--o{ disputes : assigned_admin
users ||--o{ audit_logs : actor

factories ||--o{ factory_sectors : has
factories ||--o{ bids : submits
factories ||--o| featured_listings : featured

job_listings ||--o{ job_attachments : has
job_listings ||--o{ bids : receives
job_listings ||--o| orders : converted_to

bids ||--o| orders : awarded
orders ||--o| escrow_transactions : has
orders ||--o| reviews : reviewed
orders ||--o| disputes : disputed
orders ||--o{ messages : contains
```

## Relationship Rules

| Parent | Child | Cardinality | On Delete |
|--------|-------|-------------|-----------|
| users | factories | 1:1 | CASCADE |
| users | job_listings | 1:N | CASCADE |
| users | orders (sme) | 1:N | CASCADE |
| users | orders (factory) | 1:N | CASCADE |
| factories | bids | 1:N | CASCADE |
| job_listings | bids | 1:N | CASCADE |
| job_listings | orders | 1:1 | CASCADE |
| bids | orders | 1:1 | CASCADE |
| orders | escrow_transactions | 1:N | CASCADE |
| orders | reviews | 1:1 | CASCADE |
| orders | disputes | 1:1 | CASCADE |
| orders | messages | 1:N | CASCADE |

## Soft Delete

All business tables include `deleted_at`. Records are never hard-deleted by application code; queries filter `deleted_at IS NULL`.

## Spatial Indexing

`factories.gps_coordinates` uses PostGIS `GEOMETRY(POINT, 4326)` with a GiST index for proximity matching.
