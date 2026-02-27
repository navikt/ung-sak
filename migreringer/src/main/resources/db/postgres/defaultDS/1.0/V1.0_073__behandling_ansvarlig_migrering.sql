insert into behandling_ansvarlig (
     id,
     behandling_id,
     behandling_del,
     versjon,
     ansvarlig_saksbehandler,
     ansvarlig_beslutter,
     behandlende_enhet,
     behandlende_enhet_navn,
     behandlende_enhet_arsak,
     totrinnsbehandling,
     opprettet_av,
     opprettet_tid)
select nextval('seq_behandling_ansvarlig'),
       b.id,
       'HELE',
       0,
       ansvarlig_saksbehandler,
       ansvarlig_beslutter,
       behandlende_enhet,
       behandlende_enhet_navn,
       behandlende_enhet_arsak,
       totrinnsbehandling,
       b.opprettet_av,
       b.opprettet_tid
from behandling b
join fagsak f on b.fagsak_id = f.id;

