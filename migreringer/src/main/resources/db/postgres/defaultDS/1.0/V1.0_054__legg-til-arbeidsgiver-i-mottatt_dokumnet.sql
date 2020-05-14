alter table mottatt_dokument 
	add column arbeidsgiver varchar(50),
	add column versjon bigint default 0 not null;

update mottatt_dokument set arbeidsgiver = trim(both from substring(lo_get(payload)::text from '%<virksomhetsnummer>#"_+#"</virksomhetsnummer>%' for '#')) 
where payload is not null and arbeidsgiver is null and type='INNTEKTSMELDING';

update mottatt_dokument set arbeidsgiver = trim(both from substring(lo_get(payload)::text from '%<arbeidsgiverFnr>#"_+#"</arbeidsgiverFnr>%' for '#')) 
where payload is not null and arbeidsgiver is null and type='INNTEKTSMELDING';
