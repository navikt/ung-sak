--se TSF-3238
update fagsak set periode='[2020-01-01, 2022-12-31]' where saksnummer ='D7276';

update vr_vilkar_periode
set fom = '2020-01-01', tom = '2022-12-31'
where id in (
    select  vvp.id
    from fagsak f
             join behandling b on b.fagsak_id = f.id
             join RS_VILKARS_RESULTAT rvr on rvr.behandling_id = b.id
             join VR_VILKAR_RESULTAT vvr on rvr.vilkarene_id = vvr.id
             join vr_vilkar vv on vvr.id = vv.vilkar_resultat_id
             join vr_vilkar_periode vvp on vv.id = vvp.vilkar_id
    where saksnummer = 'D7276'
)
