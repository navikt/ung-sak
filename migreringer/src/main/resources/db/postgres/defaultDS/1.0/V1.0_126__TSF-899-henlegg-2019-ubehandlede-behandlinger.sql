
-- markerer 2019 inntektsmeldinger somikke er behandlet for dodgy

update mottatt_dokument m
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select f.id from fagsak f 
 inner join behandling b on b.fagsak_id=f.id
  where f.saksnummer IN ('7CR90', '7DB9U', '7EJR8', '7EKAo', '7ESWE', '7EZ86', '7FT4K', '7G97Q', '7GoHQ', '7GR8C', '7GRT6', '7GSi6', '7GTiA', '7H9HA', '7HCYA', '7HGVY', '7HJE8', '7HTDY', '7HV5U', '7M28Q', '7M2WC', '7M3D0', '7M3FS', '7T0XQ', '7T5YA', '7T9XC', '7TAB8', '7TLT4', '7TP36', '7UCJM', '7Y0C2', '837RA', '8AEA8', '8F732', '8HX5M', '8MEF6', '8MTFG')
                 and b.id IN (1049756, 1050246, 1051369, 1051561, 1051680, 1051932, 1052591, 1052591, 1052968, 1053394, 1053758, 1053758, 1053758, 1053758, 1053758, 1053758, 1053758, 1053558, 1054267, 1053535, 1054809, 1054809, 1053968, 1054100, 1054219, 1088203, 1054515, 1054501, 1064388, 1064388, 1064388, 1064388, 1064398, 1064416, 1064418, 1074197, 1074402, 1074358, 1074358, 1074494, 1074494, 1074494, 1074494, 1074470, 1074470, 1074778, 1074825, 1074825, 1075439, 1078906, 1078906, 1079471, 1085129, 1126602, 1136834, 1143168, 1152119, 1153117)
                 and b.behandling_status IN ('OPPRE', 'UTRED')
                 )
 and (status is null or status!='UGYLDIG')
                 ;


-- henlegger saker som bare har 2019 inntektsmeldinger

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.henleggBehandling',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
'fagsakId=' || b.fagsak_id || '
behandlingId=' || b.id || '
henleggesGrunn=HENLAGT_FEILOPPRETTET'
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer IN ('7CR90', '7DB9U', '7EJR8', '7EKAo', '7ESWE', '7EZ86', '7FT4K', '7G97Q', '7GoHQ', '7GR8C', '7GRT6', '7GSi6', '7GTiA', '7H9HA', '7HCYA', '7HGVY', '7HJE8', '7HTDY', '7HV5U', '7M28Q', '7M2WC', '7M3D0', '7M3FS', '7T0XQ', '7T5YA', '7T9XC', '7TAB8', '7TLT4', '7TP36', '7UCJM', '7Y0C2', '837RA', '8AEA8', '8F732', '8HX5M', '8MEF6', '8MTFG')
                 and b.id IN (1049756, 1050246, 1051369, 1051561, 1051680, 1051932, 1052591, 1052591, 1052968, 1053394, 1053758, 1053758, 1053758, 1053758, 1053758, 1053758, 1053758, 1053558, 1054267, 1053535, 1054809, 1054809, 1053968, 1054100, 1054219, 1088203, 1054515, 1054501, 1064388, 1064388, 1064388, 1064388, 1064398, 1064416, 1064418, 1074197, 1074402, 1074358, 1074358, 1074494, 1074494, 1074494, 1074494, 1074470, 1074470, 1074778, 1074825, 1074825, 1075439, 1078906, 1078906, 1079471, 1085129, 1126602, 1136834, 1143168, 1152119, 1153117)
                 and b.behandling_status IN ('OPPRE', 'UTRED')
                 ;
