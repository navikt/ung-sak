ALTER TABLE MOTTATT_DOKUMENT
    ALTER COLUMN dokument_kategori DROP NOT NULL;

update MOTTATT_DOKUMENT set type='INNTEKTSMELDING'; --alle innslag hittil er inntektsmelinger, så ok.

-- etterfyll placeholdere for mottatte FRISINN søknader (mangler payload for disse innti videre, men ok - soerger for at dup sjekk blir grei)
insert into mottatt_dokument (id, journalpost_id, type, opprettet_av, fagsak_id, mottatt_dato, mottatt_tidspunkt)
select nextval('seq_mottatt_dokument'), hl.journalpost_id, 'INNTEKTKOMP_FRILANS', 'k9-sak-migrering', h.fagsak_id, date_trunc('day', hl.opprettet_tid)::date, hl.opprettet_tid
from HISTORIKKINNSLAG_DOK_LINK hl
inner join HISTORIKKINNSLAG h on h.id = hl.historikkinnslag_id
inner join FAGSAK f on f.id=h.fagsak_id
where hl.link_tekst='Søknad' and f.ytelse_type='FRISINN';

    
alter table MOTTATT_DOKUMENT alter column payload TYPE oid using (payload::oid);