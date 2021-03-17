alter table SO_SOEKNAD add column journalpost_id varchar(20);

create index IDX_SO_SOEKNAD_12 on SO_SOEKNAD(journalpost_id);

alter table SO_SOEKNAD add column soeknad_id varchar(100);
create index IDX_SO_SOEKNAD_13 on SO_SOEKNAD(soeknad_id);
