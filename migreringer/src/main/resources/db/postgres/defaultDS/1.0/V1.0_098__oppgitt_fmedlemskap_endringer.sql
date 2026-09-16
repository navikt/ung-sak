alter table if exists oppgitt_fmedlemskap_bosted rename to oppgitt_fmedlemskap_utenlandsopphold;
alter sequence if exists seq_oppgitt_fmedlemskap_bosted rename to seq_oppgitt_fmedlemskap_utenlandsopphold;
alter index if exists idx_oppgitt_fmedlemskap_bosted_fmedlemskap rename to idx_oppgitt_fmedlemskap_utenlandsopphold_fmedlemskap;

comment on table oppgitt_fmedlemskap_utenlandsopphold is 'Enkeltopphold i utlandet innenfor en oppgitt forutgående medlemskapsperiode. Landkode er ISO 3166-1 alpha-3.';
comment on table oppgitt_fmedlemskap is 'Per-søknad oppgitt forutgående medlemskapsperiode med utenlandsopphold. Hver rad representerer data fra én søknad.';

-- nye kolonner
alter table if exists oppgitt_fmedlemskap add column if not exists har_bodd_i_norge boolean not null default false;
alter table if exists oppgitt_fmedlemskap add column if not exists har_jobbet_i_norge boolean;
alter table if exists oppgitt_fmedlemskap add column if not exists har_jobbet_utenfor_norge boolean;

-- backfill basert på om perioden har oppgitt utenlandsopphold
update oppgitt_fmedlemskap set har_bodd_i_norge = true, har_jobbet_utenfor_norge = false
where not exists (select 1 from oppgitt_fmedlemskap_utenlandsopphold u where u.oppgitt_fmedlemskap_id = oppgitt_fmedlemskap.id);

update oppgitt_fmedlemskap set har_bodd_i_norge = false, har_jobbet_i_norge = false
where exists (select 1 from oppgitt_fmedlemskap_utenlandsopphold u where u.oppgitt_fmedlemskap_id = oppgitt_fmedlemskap.id);

