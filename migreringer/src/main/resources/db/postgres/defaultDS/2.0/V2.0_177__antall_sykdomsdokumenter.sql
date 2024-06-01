alter table medisinsk_grunnlagsdata add column if not exists antall_sykdomsdokumenter bigint;

--TODO endre til update, kanskje lage uttrekk først
with antall_mg as (select grm.pleietrengende_person_id as id, count(*) as antall1 from medisinsk_grunnlagsdata mg
    inner join gr_medisinsk grm on (mg.id = grm.medisinsk_grunnlagsdata_id)
    inner join pleietrengende_sykdom ps on (ps.pleietrengende_person_id = grm.pleietrengende_person_id)
    inner join pleietrengende_sykdom_dokument dokument on (dokument.pleietrengende_sykdom_id = ps.id)
    where dokument.opprettet_tid <= mg.opprettet_tid
    group by grm.pleietrengende_person_id),
    antall_pleietrengende as  (select grm.pleietrengende_person_id as id, count(*) as antall2 from medisinsk_grunnlagsdata mg
    inner join gr_medisinsk grm on (mg.id = grm.medisinsk_grunnlagsdata_id)
    inner join pleietrengende_sykdom ps on (ps.pleietrengende_person_id = grm.pleietrengende_person_id)
    inner join pleietrengende_sykdom_dokument dokument on (dokument.pleietrengende_sykdom_id = ps.id)
   group by grm.pleietrengende_person_id)
select * from antall_mg join antall_pleietrengende on (antall_mg.id = antall_pleietrengende.id) where antall1 <> antall2
