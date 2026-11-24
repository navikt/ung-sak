-- Maks én åpen kontrollbehandling (BT-012) per fagsak. Indeksen stopper to opprettelser som går samtidig.
-- Koden for behandlingstypen er foreløpig. Endres koden, må indeksen lages på nytt.
create unique index if not exists uidx_behandling_aapen_kontroll_per_fagsak
    on behandling (fagsak_id) where behandling_type = 'BT-012' and behandling_status <> 'AVSLU';
