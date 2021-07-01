update MOTTATT_DOKUMENT
set status     = 'UGYLDIG',
    feilmelding='TSF-1845: Søknad må ha eksakt én (hoved)virksomhet. Størrelse var 2'
where journalpost_id IN (509825408)
  and status = 'MOTTATT'
;
