create temporary table tmp_startdato_migrering (
    behandling_id bigint not null primary key,
    startdatoer_id bigint not null
) on commit drop;

insert into tmp_startdato_migrering (behandling_id, startdatoer_id)
select distinct a.behandling_id, nextval('seq_startdatoer')
from akt_soekt_periode a
where not exists (
    select 1
    from gr_startdato g
    where g.behandling_id = a.behandling_id
      and g.aktiv = true
);

insert into startdatoer (id)
select t.startdatoer_id
from tmp_startdato_migrering t;

insert into soekt_startdato (id, startdatoer_id, journalpost_id, startdato)
select nextval('seq_soekt_startdato'),
       t.startdatoer_id,
       a.journalpost_id,
       a.fom
from (
    select distinct on (a.behandling_id, a.fom)
           a.behandling_id,
           a.journalpost_id,
           a.fom
    from akt_soekt_periode a
    join tmp_startdato_migrering t on t.behandling_id = a.behandling_id
    order by a.behandling_id, a.fom, a.journalpost_id
) a
join tmp_startdato_migrering t on t.behandling_id = a.behandling_id;

insert into gr_startdato (id, behandling_id, relevante_startdatoer_id, oppgitte_startdatoer_id)
select nextval('seq_gr_startdato'),
       t.behandling_id,
       t.startdatoer_id,
       t.startdatoer_id
from tmp_startdato_migrering t;

