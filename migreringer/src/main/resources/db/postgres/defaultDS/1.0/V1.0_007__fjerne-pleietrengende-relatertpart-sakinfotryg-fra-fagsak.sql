alter table fagsak drop constraint unik_fagsak_1;
alter table fagsak drop constraint unik_fagsak_2;
alter table fagsak drop constraint unik_fagsak_3;
alter table fagsak add constraint unik_en_fagsak_pr_bruker_og_periode exclude using gist (ytelse_type with =, bruker_aktoer_id with =, periode with &&) where ((periode is not null));
alter table fagsak drop column pleietrengende_aktoer_id;
alter table fagsak drop column relatert_person_aktoer_id;
alter table fagsak drop column til_infotrygd;
