

-- 6LX4G

update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='6LX4G') and journalpost_id IN ('478272399');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='6LX4G' and b.id=1089718;


-- 6X3A8

update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='6X3A8') and journalpost_id IN ('479762644','479762770');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='6X3A8' and b.id=1034701;
            
            
-- 6XMAY

update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='6XMAY') and journalpost_id IN ('476701995');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='6XMAY' and b.id=1035165;


            
-- 7EACC

update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='7EACC') and journalpost_id IN ('474102271', '474102334', '474102398', '474102475', '474102530', '474102565');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='7EACC' and b.id=1051255;
                                
        
-- 7EVAi

update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='7EVAi') and journalpost_id IN ('476774783');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='7EVAi' and b.id=1051806;
        
      
      
-- 7EYDM

update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='7EYDM') and journalpost_id IN ('476820672');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='7EYDM' and b.id=1051793;
                
                
-- 7H2NG

update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='7H2NG') and journalpost_id IN ('474099494', '474099601', '474099681', '474099773', '474099814', '474099881', '474100000', '474100042', '474100263', '474100966', '474101015', '474101129', '474101761', '474101818');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='7H2NG' and b.id=1053790
;
       
       
-- 7HUW4

update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='7HUW4') and journalpost_id IN ('474466725');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='7HUW4' and b.id=1054558;
                
                
       
-- 7KZYY

update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='7KZYY') and journalpost_id IN ('478253213');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='7KZYY' and b.id=1058128;
           
           
-- 7M3JY

update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='7M3JY') and journalpost_id IN ('478367024', '478367052', '478367082', '478367101');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='7M3JY' and b.id=1064420;
                
                
-- 7TGBW


update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='7TGBW') and journalpost_id IN ('475010758', '475010763');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='7TGBW' and b.id=1074589;
   
-- 7TGVC


update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='7TGVC') and journalpost_id IN ('475243560', '475243591');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='7TGVC' and b.id=1074635;
   
-- 82XDo


update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='82XDo') and journalpost_id IN ('479771662');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='82XDo' and b.id=1084727;
   
   
-- 831NU


update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='831NU') and journalpost_id IN ('479780168', '479780196', '479780207', '479780212', '479780215', '479780220', '479780246');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='831NU' and b.id=1084946;
   
   
-- 84MKG


update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='84MKG') and journalpost_id IN ('479902168');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='84MKG' and b.id=1090181;
   
-- 84XDC


update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='84XDC') and journalpost_id IN ('479978125', '479978129');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='84XDC' and b.id=1092360;
   
   
-- 877NU


update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2015. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='877NU') and journalpost_id IN ('480275314');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='877NU' and b.id=1116092;
   
   
   
-- 88XQK


update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='88XQK') and journalpost_id IN ('480498260');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='88XQK' and b.id=1120143;


-- fiks fagsak periode for 2017 saker
update fagsak set periode='[2017-01-01, 2018-01-01)'
where saksnummer in ('7Yo28');
                
-- fiks fagsak periode for 2018 saker
update fagsak set periode='[2018-01-01, 2019-01-01)'
where saksnummer in ('7T784');

-- fiks fagsak perioder for 2019 saker 

update fagsak set periode='[2019-01-01, 2020-01-01)'
where saksnummer in ('7CR90', '7DB9U', '7EJR8', '7EKAo', '7ESWE', '7EZ86', '7FT4K', '7G97Q', '7GoHQ', '7GR8C', '7GRT6', '7GSi6', '7GTiA', '7H9HA', '7HCYA', '7HGVY', '7HJE8', '7HTDY', '7HV5U', '7M28Q', '7M2WC', '7M3D0', '7M3FS', '7T0XQ', '7T5YA', '7T784', '7T9XC', '7TAB8', '7TLT4', '7TP36', '7UCJM', '7Y0C2', '7Yo28', '837RA', '8AEA8', '8F732', '8HX5M', '8MEF6', '8MTFG', '7TSEM', '7Y29i', '7VR1o');


-- henlegg 7TSEM 
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.henleggBehandling',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
'fagsakId=' || b.fagsak_id || '
behandlingId=' || b.id || '
henleggesGrunn=HENLAGT_FEILOPPRETTET'
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='7TSEM' and b.id=1074832;
                
-- henlegg 7Y29i
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.henleggBehandling',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
'fagsakId=' || b.fagsak_id || '
behandlingId=' || b.id || '
henleggesGrunn=HENLAGT_FEILOPPRETTET'
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='7Y29i' and b.id=1078914;
 
-- henlegg 7T784
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.henleggBehandling',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
'fagsakId=' || b.fagsak_id || '
behandlingId=' || b.id || '
henleggesGrunn=HENLAGT_FEILOPPRETTET'
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='7T784' and b.id=1074358;
                
 
-- henlegg 7Yo28
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.henleggBehandling',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
'fagsakId=' || b.fagsak_id || '
behandlingId=' || b.id || '
henleggesGrunn=HENLAGT_FEILOPPRETTET'
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='7Yo28' and b.id=1079471;
    
-- unlink 2017 im fra 2019 sak
update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2017. Kan ikke kobles til 2019 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='7Y0C2') and journalpost_id IN ('478598342');

