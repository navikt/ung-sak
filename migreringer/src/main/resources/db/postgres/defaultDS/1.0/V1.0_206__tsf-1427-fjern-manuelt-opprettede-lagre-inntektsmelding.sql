update prosess_task set status='KJOERT', blokkert_av=NULL, siste_kjoering_feil_tekst='AVBRUTT - MANUELL FEILRETTING' where id in (13208254, 13208237) and status != 'FERDIG';
