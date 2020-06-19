alter table MOTTATT_DOKUMENT add column status varchar(10);
alter table MOTTATT_DOKUMENT add column feilmelding TEXT;

update MOTTATT_DOKUMENT set status = 'UGYLDIG', feilmelding='Refusjon periode går over flere år'
where fagsak_id IN (select id from fagsak where saksnummer IN ('7oiS8', '7DYJC', '7KGWU', '7HZTW', '6VCFQ', '7GM96') and ytelse_type='OMP');


update prosess_task set status='KJOERT', blokkert_av=NULL where id in(
    2073495, 1890672, 1910231, 1816220, 1401313, 1809512) and status NOT IN ('KJOERT', 'FERDIG');

delete from fagsak_prosess_task where prosess_task_id in (
    2073495, 1890672, 1910231, 1816220, 1401313, 1809512) 
     and fagsak_id IN (select id from fagsak where saksnummer IN ('7oiS8', '7DYJC', '7KGWU', '7HZTW', '6VCFQ', '7GM96') and ytelse_type='OMP');

update prosess_task set status='KLAR', blokkert_av=NULL where blokkert_av in(
    2073495, 1890672, 1910231, 1816220, 1401313, 1809512) 
     and status NOT IN ('KJOERT', 'FERDIG');
     
 



