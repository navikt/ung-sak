-- legger til kolonne for maks-dato for programperioden (260 virkedager standard, 300 ved forlenget periode).
-- sendes alltid fra ung-deltakelse-opplyser uavhengig av om har_forlenget_periode er true eller false.
alter table ung_ungdomsprogram_forlenget_periode
    add column periode_maks_dato date null;

comment on column ung_ungdomsprogram_forlenget_periode.periode_maks_dato is
    'Siste mulige dato for programperioden, beregnet av ung-deltakelse-opplyser. 260 virkedager fra startdato ved normal kvote, 300 ved forlenget periode. Brukes til å beregne tom-dato uten å materialisere en lukket programperiode.';

