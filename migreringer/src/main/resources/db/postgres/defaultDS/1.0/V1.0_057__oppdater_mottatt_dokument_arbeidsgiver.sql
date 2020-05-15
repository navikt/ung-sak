update mottatt_dokument m
set arbeidsgiver = v.orgnummer
from (
	select t.id, trim(both from substring(lo_get(payload)::text from '%<virksomhetsnummer>#_+#</virksomhetsnummer>%' for '#')) orgnummer from mottatt_dokument t
	where t.payload is not null and type='INNTEKTSMELDING' and arbeidsgiver is null
	) v
WHERE v.id = m.id and v.orgnummer is not null and m.arbeidsgiver is null
	;
	
update mottatt_dokument m
set arbeidsgiver = v.orgnummer
from (
	select t.id, trim(both from substring(lo_get(payload)::text from '%<arbeidsgiverFnr>#_+#</arbeidsgiverFnr>%' for '#')) orgnummer from mottatt_dokument t
	where t.payload is not null and type='INNTEKTSMELDING' and arbeidsgiver is null
	) v
WHERE v.id = m.id and v.orgnummer is not null and m.arbeidsgiver is null
	;