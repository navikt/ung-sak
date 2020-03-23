CREATE DATABASE k9sak_unit;
CREATE USER k9sak_unit WITH PASSWORD 'k9sak_unit';
GRANT ALL PRIVILEGES ON DATABASE k9sak_unit TO k9sak_unit;
ALTER DATABASE k9sak_unit SET timezone TO 'Europe/Oslo';

\connect k9sak_unit;
create extension if not exists btree_gist;

