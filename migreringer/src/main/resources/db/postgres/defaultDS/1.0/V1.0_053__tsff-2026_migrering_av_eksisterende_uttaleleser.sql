alter table gr_uttalelse
drop constraint "gr_uttalelse_uttalelser_id_fkey";
insert into gr_uttalelse (id, behandling_id, uttalelser_id)
select nextval('SEQ_GR_UTTALELSE'), e.behandling_id, nextval('SEQ_UTTALELSER') from (
    select distinct etterlysning.behandling_id
    from etterlysning
    where not exists(select 1
        from gr_uttalelse gu
        where gu.behandling_id = etterlysning.behandling_id
        and gu.aktiv = true)
        and uttalelse_id is not null
) e;


insert into uttalelser (id)
select uttalelser_id from (
    select uttalelser_id
    from gr_uttalelse
    where not exists(select 1
        from uttalelser u
        where u.id = uttalelser_id)
) e;

alter table gr_uttalelse
    add constraint "gr_uttalelse_uttalelser_id_fkey" foreign key (uttalelser_id) references uttalelser (id);


insert into uttalelse_v2 (id, grunnlag_ref, svar_journalpost_id, uttalelser_id, endring_type, fom, tom, har_uttalelse, begrunnelse)
select nextval('SEQ_UTTALELSE_V2'), e.grunnlag_ref, u.svar_journalpost_id, gu.uttalelser_id, 'ENDRET_INNTEKT', e.fom, e.tom, u.har_uttalelse, u.uttalelse_begrunnelse
from uttalelse u inner join etterlysning e on u.id = e.uttalelse_id inner join gr_uttalelse gu on e.behandling_id = gu.behandling_id
where not exists(select 1 from gr_uttalelse gu2 inner join uttalelse_v2 uv2inner on gu2.uttalelser_id=uv2inner.uttalelser_id
                 where uv2inner.svar_journalpost_id = u.svar_journalpost_id and gu2.behandling_id = e.behandling_id and gu2.aktiv=true)
  and gu.aktiv=true
  and e.type = 'UTTALELSE_KONTROLL_INNTEKT';

insert into uttalelse_v2 (id, grunnlag_ref, svar_journalpost_id, uttalelser_id, endring_type, fom, tom, har_uttalelse, begrunnelse)
select nextval('SEQ_UTTALELSE_V2'), e.grunnlag_ref, u.svar_journalpost_id, gu.uttalelser_id, 'ENDRET_STARTDATO', e.fom, e.tom, u.har_uttalelse, u.uttalelse_begrunnelse
from uttalelse u inner join etterlysning e on u.id = e.uttalelse_id inner join gr_uttalelse gu on e.behandling_id = gu.behandling_id
where not exists(select 1 from gr_uttalelse gu2 inner join uttalelse_v2 uv2inner on gu2.uttalelser_id=uv2inner.uttalelser_id
                 where uv2inner.svar_journalpost_id = u.svar_journalpost_id and gu2.behandling_id = e.behandling_id and gu2.aktiv=true)
  and gu.aktiv=true
  and e.type = 'UTTALELSE_ENDRET_STARTDATO';

insert into uttalelse_v2 (id, grunnlag_ref, svar_journalpost_id, uttalelser_id, endring_type, fom, tom, har_uttalelse, begrunnelse)
select nextval('SEQ_UTTALELSE_V2'), e.grunnlag_ref, u.svar_journalpost_id, gu.uttalelser_id, 'ENDRET_SLUTTDATO', e.fom, e.tom, u.har_uttalelse, u.uttalelse_begrunnelse
from uttalelse u inner join etterlysning e on u.id = e.uttalelse_id inner join gr_uttalelse gu on e.behandling_id = gu.behandling_id
where not exists(select 1 from gr_uttalelse gu2 inner join uttalelse_v2 uv2inner on gu2.uttalelser_id=uv2inner.uttalelser_id
                 where uv2inner.svar_journalpost_id = u.svar_journalpost_id and gu2.behandling_id = e.behandling_id and gu2.aktiv=true)
  and gu.aktiv=true
  and e.type = 'UTTALELSE_ENDRET_SLUTTDATO';


