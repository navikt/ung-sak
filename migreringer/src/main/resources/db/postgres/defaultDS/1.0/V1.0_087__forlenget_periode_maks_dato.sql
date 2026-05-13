-- legger til kolonne for maks-dato for forlenget periode (sendes fra ung-deltakelse-opplyser).
-- nullable for bakoverkompatibilitet i overgangsperioden hvor registeret ennå ikke alltid sender feltet.
alter table ung_ungdomsprogram_forlenget_periode
    add column forlenget_periode_maks_dato date null;

comment on column ung_ungdomsprogram_forlenget_periode.forlenget_periode_maks_dato is
    'Siste mulige dato for forlenget periode i ungdomsprogrammet, slik den er beregnet av ung-deltakelse-opplyser. Brukes til å beregne tom-dato uten å materialisere en lukket programperiode.';

