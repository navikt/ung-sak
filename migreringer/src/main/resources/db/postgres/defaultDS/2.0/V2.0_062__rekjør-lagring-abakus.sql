-- Rekjør lagring av IM som feilaktig ble satt til FERDIG før lagring, AJMS6
update prosess_task
set blokkert_av = null,
    status      ='KLAR'
where status = 'FERDIG'
  and task_type = 'lagre.inntektsmeldinger.til.abakus'
  and id = 20512883;
