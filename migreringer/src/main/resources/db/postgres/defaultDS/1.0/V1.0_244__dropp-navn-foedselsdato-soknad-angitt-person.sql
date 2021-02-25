alter table SO_SOEKNAD_ANGITT_PERSON drop column navn, drop column foedselsdato;

alter table FAGSAK add column RELATERT_PERSON_AKTOER_ID VARCHAR(50);