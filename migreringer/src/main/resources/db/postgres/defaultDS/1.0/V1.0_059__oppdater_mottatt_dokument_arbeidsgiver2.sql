update mottatt_dokument m
set arbeidsgiver = substring(lo_get(payload)::text from '.*<virksomhetsnummer>\s*(\d+)\s*</virksomhetsnummer>.*')
WHERE m.payload is not null and m.type='INNTEKTSMELDING' and m.arbeidsgiver is null
	;
	
update mottatt_dokument m
set arbeidsgiver = substring(lo_get(payload)::text from '.*<arbeidsgiverFnr>\s*(\d+)\s*</arbeidsgiverFnr>.*')
WHERE m.payload is not null and m.type='INNTEKTSMELDING' and m.arbeidsgiver is null
	;