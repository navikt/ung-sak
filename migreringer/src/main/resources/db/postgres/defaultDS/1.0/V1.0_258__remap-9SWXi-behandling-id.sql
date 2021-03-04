-- fix up mottatt_dokument inntektsmeldinger for 9SWXi
update mottatt_dokument set behandling_id=1350797
where journalpost_id in ('490674017', '490674020') and behandling_id=1291654
and exists (select 1 from behandling where id=1350797 and behandling_status='UTRED');