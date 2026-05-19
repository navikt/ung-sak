-- legger til kolonne for maks-dato for programperioden (260 virkedager standard, 300 ved forlenget periode).
-- sendes alltid fra ung-deltakelse-opplyser uavhengig av om har_forlenget_periode er true eller false.
-- renamer også tabell, sekvens og FK-kolonne til "maks_periode" da tabellen ikke nødvendigvis representerer en forlenget periode.
alter table if exists ung_ungdomsprogram_forlenget_periode
    add column periode_maks_dato date not null;

alter table if exists ung_ungdomsprogram_forlenget_periode rename to ung_ungdomsprogram_maks_periode;

alter sequence if exists seq_ung_ungdomsprogram_forlenget_periode_id rename to seq_ung_ungdomsprogram_maks_periode_id;

alter table if exists ung_gr_ungdomsprogramperiode
    rename column ung_ungdomsprogramp_forlenget_periode_id to ung_ungdomsprogram_maks_periode_id;

drop index if exists idx_ung_gr_ungdomsprogramperiode_forlenget_periode;
create index if not exists idx_ung_gr_ungdomsprogramperiode_maks_periode
    on ung_gr_ungdomsprogramperiode (ung_ungdomsprogram_maks_periode_id);

comment on table ung_ungdomsprogram_maks_periode is
    'Konfigurasjon for maksimal periodevarighet i ungdomsprogrammet, inkludert om bruker har forlenget periode (300 virkedager i stedet for 260).';
comment on column ung_ungdomsprogram_maks_periode.periode_maks_dato is
    'Siste mulige dato for programperioden, beregnet av ung-deltakelse-opplyser. 260 virkedager fra startdato ved normal kvote, 300 ved forlenget periode. Brukes til å beregne tom-dato uten å materialisere en lukket programperiode.';
