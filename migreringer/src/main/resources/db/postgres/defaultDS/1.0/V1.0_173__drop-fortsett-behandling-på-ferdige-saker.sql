
delete from fagsak_prosess_task where
prosess_task_id in (
    select
        p.id
    from prosess_task p
    where 
        siste_kjoering_feil_tekst like '%Utvikler-feil - saken er ferdig behandlet, kan ikke oppdateres.%' 
        and p.status='FEILET'
        and task_type='behandlingskontroll.fortsettBehandling'
);

-- opphever veto
update prosess_task set status='KLAR'
  where 
     status='VETO'
     and blokkert_av in (
         select
            p.id
         from prosess_task p
         where 
             siste_kjoering_feil_tekst like '%Utvikler-feil - saken er ferdig behandlet, kan ikke oppdateres.%' 
             and p.status='FEILET'
             and task_type='behandlingskontroll.fortsettBehandling'
     );

update prosess_task set status='KJOERT'
  where 
     siste_kjoering_feil_tekst like '%Utvikler-feil - saken er ferdig behandlet, kan ikke oppdateres.%' 
     and status='FEILET'
     and task_type='behandlingskontroll.fortsettBehandling';
     
     