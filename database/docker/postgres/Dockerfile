# PostgreSQL 16 + PostGIS 3.4 (official postgis image) + pgvector.
# The postgis image is Debian-based and already has the PGDG apt repository,
# so pgvector installs as a plain package. Keeps PG major = 16 (ARCHITECTURE ADR 007).
FROM postgis/postgis:16-3.4

RUN apt-get update \
 && apt-get install -y --no-install-recommends postgresql-16-pgvector \
 && rm -rf /var/lib/apt/lists/*
