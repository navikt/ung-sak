alter table mottatt_dokument add column if not exists kildesystem varchar(100);
alter table mottatt_dokument add column if not exists innsendingstidspunkt timestamp(3) without time zone;

update mottatt_dokument m set
 kildesystem = v.systemnavn,
 innsendingstidspunkt = to_timestamp(case when v.innsendingstidspunkt like '%+%' then v.innsendingstidspunkt else v.innsendingstidspunkt || '+00:00' end, 'YYYY-MM-DDThh24:MI:SS.US+TZH') at time zone 'UTC'
 
from (
  select
   regexp_replace(regexp_replace(substring( convert_from(lo_get(payload), 'UTF8') from '<(?:\w+:innsendingstidspunkt\s*[^>]*|innsendingstidspunkt)>\s*(.+)\s*</(?:\w+:innsendingstidspunkt\s*[^>]*|innsendingstidspunkt)>'), '(\.\d{1,6})(\d+)', '\1'), 'Z', '+00:00') as innsendingstidspunkt,
   substring(convert_from(lo_get(payload), 'UTF8') from '<(?:\w+:systemnavn\s*[^>]*|systemnavn)>\s*(.+)\s*</(?:\w+:systemnavn\s*[^>]*|systemnavn)>') as systemnavn,
   m2.id
   from mottatt_dokument m2
   where (innsendingstidspunkt is null or kildesystem is null)
     AND type='INNTEKTSMELDING' AND payload IS NOT NULL
   
) v
where (m.innsendingstidspunkt is null or m.kildesystem is null)
 AND m.type='INNTEKTSMELDING' AND m.payload IS NOT NULL AND v.id=m.id
;


create index idx_mottatt_dokument_11 on mottatt_dokument(kildesystem);
create index idx_mottatt_dokument_12 on mottatt_dokument(innsendingstidspunkt);

