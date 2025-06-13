-- ubrukte felter
alter table brevbestilling
    drop column saksnummer,
    drop column dokumentdata;

-- forberedelse migrere brevbestilling_behandling tabellen
alter table brevbestilling
    add behandling_id bigint,
    add fagsak_id bigint,
    add vedtaksbrev boolean;

comment on column brevbestilling.vedtaksbrev is 'er dokumentet et vedtaksbrev - kun tillatt med ett slik per behandling';

drop index if exists idx_brevbestilling_behandling_id;
create index idx_brevbestilling_behandling_id
    on brevbestilling (behandling_id);

update brevbestilling
set behandling_id = bb.behandling_id,
    vedtaksbrev   = bb.vedtaksbrev
from brevbestilling_behandling bb
where brevbestilling.id = bb.brevbestilling_id;

update brevbestilling
set fagsak_id = b.fagsak_id
from behandling b
where behandling_id = b.id;

alter table brevbestilling
    alter column behandling_id set not null,
    alter column fagsak_id set not null,
    alter column vedtaksbrev set not null;

create unique index uidx_brevbestilling_behandling_vedtak_mal
    on brevbestilling (behandling_id, dokumentmal_type)
    where vedtaksbrev = true and aktiv = true;

create index idx_brevbestilling_aktiv on brevbestilling (aktiv);

drop table brevbestilling_behandling
