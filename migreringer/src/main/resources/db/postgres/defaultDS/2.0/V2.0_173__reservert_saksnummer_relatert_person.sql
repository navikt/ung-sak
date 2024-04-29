alter table reservert_saksnummer add column relatert_person_aktoer_id varchar(50);

create index IDX_RESERVERT_SAKSNUMMER_1 on RESERVERT_SAKSNUMMER (BRUKER_AKTOER_ID);
