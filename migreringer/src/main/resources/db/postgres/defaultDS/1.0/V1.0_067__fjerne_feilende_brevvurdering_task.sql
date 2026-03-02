-- oppdaterer tabellen med undertrykt resultat for sporing
insert into behandling_vedtaksbrev (id, behandling_id, fagsak_id, resultat_type, beskrivelse)
select nextval('seq_behandling_vedtaksbrev'), 3001901, 2001802, 'UNDERTRYKT', 'TSFF-2400'
from behandling
where id = 3001901 and fagsak_id = 2001802;

update prosess_task set status = 'FERDIG' where id = 1038154 and status = 'FEILET' and task_type = 'formidling.vedtak.brevvurdering'
