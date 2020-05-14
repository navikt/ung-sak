update mottatt_dokument set arbeidsgiver = trim(both from substring(lo_get(payload::oid)::text from '%<virksomhetsnummer>#"_+#"</virksomhetsnummer>%' for '#')) 
where payload is not null and arbeidsgiver is null and type='INNTEKTSMELDING';

update mottatt_dokument set arbeidsgiver = trim(both from substring(lo_get(payload::oid)::text from '%<arbeidsgiverFnr>#"_+#"</arbeidsgiverFnr>%' for '#')) 
where payload is not null and arbeidsgiver is null and type='INNTEKTSMELDING';
