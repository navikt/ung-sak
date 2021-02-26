alter table SO_SOEKNAD_ANGITT_PERSON drop column navn, drop column foedselsdato;

alter table FAGSAK add column RELATERT_PERSON_AKTOER_ID VARCHAR(50);

ALTER TABLE fagsak
    ADD CONSTRAINT unik_fagsak_3 EXCLUDE USING gist (
    periode WITH &&,
    bruker_aktoer_id WITH =,
    relatert_person_aktoer_id WITH =,
    ytelse_type WITH =)
    WHERE (relatert_person_aktoer_id IS NOT NULL AND periode IS NOT NULL);
