alter table fagsak
    add column pleietrengende_aktoer_id VARCHAR(50),
    add column bruker_aktoer_id         VARCHAR(50);

create  index IDX_FAGSAK_8 on FAGSAK (bruker_aktoer_id);
create  index IDX_FAGSAK_9 on FAGSAK (pleietrengende_aktoer_id);

UPDATE fagsak
SET bruker_aktoer_id = b.aktoer_id
FROM bruker b
         INNER JOIN fagsak f on f.bruker_id = b.ID;

UPDATE fagsak
set pleietrengende_aktoer_id = p.aktoer_id
FROM md_pleietrengende p
         INNER JOIN gr_medisinsk gm on gm.pleietrengende_id = p.id
         INNER JOIN behandling b ON gm.behandling_id = b.id
         INNER JOIN fagsak f ON b.fagsak_id = f.id;

alter table fagsak
    alter column bruker_aktoer_id set not null;

alter table fagsak
    drop column bruker_id;

drop table bruker;

alter table gr_medisinsk
    drop column pleietrengende_id;

drop table md_pleietrengende;
