alter table behandling_steg_tilstand add column AKTIV BOOLEAN DEFAULT false NOT NULL;

create unique index UIDX_BEHANDLING_STEG_TILSTAND_01 ON BEHANDLING_STEG_TILSTAND (BEHANDLING_ID, BEHANDLING_STEG) WHERE (AKTIV=TRUE);

-- fiks sett aktive tilstander
update behandling_steg_tilstand bst 
set aktiv = true 
where 
ROW(bst.opprettet_tid, coalesce(bst.endret_tid, CURRENT_TIMESTAMP)) 
IN (select max(bst2.opprettet_tid), coalesce(max(bst2.endret_tid), CURRENT_TIMESTAMP) from behandling_steg_tilstand bst2 where bst2.behandling_id = bst.behandling_id group by bst2.opprettet_tid, bst2.endret_tid);

