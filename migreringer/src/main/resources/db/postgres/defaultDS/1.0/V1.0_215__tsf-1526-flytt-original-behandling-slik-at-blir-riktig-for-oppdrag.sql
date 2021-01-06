-- korrigerer 8ViQM for å linke riktig ifht ha k9-oppdrag kjenner til
update behandling set original_behandling_id=1179106   
where id = 1308326 and behandling_status='UTRED';


-- korrigerer 8HREo for å linke riktig ifht hva k9-oppdrag kjenner til
update behandling set original_behandling_id=1142945
where id = 1308327 and behandling_status='UTRED';
