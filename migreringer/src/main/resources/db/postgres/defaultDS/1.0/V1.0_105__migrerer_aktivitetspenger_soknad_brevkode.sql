update fordel_journalpost_mottatt
set brevkode = 'NAV 25-13.01'
where brevkode = 'FIXME_AKTIVITETSPENGER_BREVKODE';

update fordel_mottatt_melding
set brevkode = 'NAV 25-13.01'
where brevkode = 'FIXME_AKTIVITETSPENGER_BREVKODE';

update fordel_journalpost_innsending
set brevkode = 'NAV 25-13.01'
where brevkode = 'FIXME_AKTIVITETSPENGER_BREVKODE';
