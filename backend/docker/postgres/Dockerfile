# PostgreSQL 16 + PostGIS 3.4 (official postgis image) + pgvector.
# Keeps PG major = 16 (ARCHITECTURE ADR 007).
#
# pgvector is copied from the official pgvector image instead of being installed with apt:
# postgis/postgis:16-3.4 is based on Debian 11 (bullseye), whose PGDG apt repository
# (bullseye-pgdg) was removed from apt.postgresql.org after bullseye reached end of life,
# so `apt-get install postgresql-16-pgvector` no longer works on this base image.
FROM pgvector/pgvector:0.8.7-pg16 AS pgvector

FROM postgis/postgis:16-3.4

COPY --from=pgvector /usr/lib/postgresql/16/lib/vector.so /usr/lib/postgresql/16/lib/
COPY --from=pgvector /usr/share/postgresql/16/extension/vector* /usr/share/postgresql/16/extension/
