alter table if exists oppgitt_fmedlemskap_bosted rename to oppgitt_fmedlemskap_utenlandsopphold;
alter sequence if exists seq_oppgitt_fmedlemskap_bosted rename to seq_oppgitt_fmedlemskap_utenlandsopphold;
alter index if exists idx_oppgitt_fmedlemskap_bosted_fmedlemskap rename to idx_oppgitt_fmedlemskap_utenlandsopphold_fmedlemskap;

comment on table oppgitt_fmedlemskap_utenlandsopphold is 'Enkeltopphold i utlandet innenfor en oppgitt forutgående medlemskapsperiode. Landkode er ISO 3166-1 alpha-3.';
comment on table oppgitt_fmedlemskap is 'Per-søknad oppgitt forutgående medlemskapsperiode med utenlandsopphold. Hver rad representerer data fra én søknad.';
