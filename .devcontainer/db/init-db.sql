GRANT ALL PRIVILEGES ON DATABASE k9sak_unit TO k9sak_unit;
create extension if not exists btree_gist;

CREATE DATABASE k9sak;
CREATE USER k9sak WITH PASSWORD 'k9sak';
GRANT ALL PRIVILEGES ON DATABASE k9sak TO k9sak;

\connect k9sak k9sak;
create extension if not exists btree_gist;
