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
     totrinnsbehandling)
select nextval('seq_behandling_ansvarlig'),
       b.id,
       'HELE',
       0,
       ansvarlig_saksbehandler,
       ansvarlig_beslutter,
       behandlende_enhet,
       behandlende_enhet_navn,
       behandlende_enhet_arsak,
       totrinnsbehandling,id
from behandling b;

