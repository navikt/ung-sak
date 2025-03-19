create table etterlysning
(
    id                      bigint not null primary key,
    behandling_id           bigint not null references behandling(id),
    fom                     date,
    tom                     date,
    grunnlag_ref            uuid not null,
    ekstern_ref             uuid not null,
    frist                   timestamp,
    status                  varchar(100) not null,
    type                    varchar(100) not null,
    opprettet_tid           timestamp default CURRENT_TIMESTAMP not null,
    opprettet_av            varchar(20) not null default 'VL',
    endret_av               varchar(20),
    endret_tid              timestamp
);

create index idx_etterlysning_behandling_id
    on etterlysning (behandling_id);

create index idx_etterlysning_behandling_id_type
    on etterlysning (behandling_id, type);

create index idx_etterlysning_behandling_id_skal_avbrytes
    on etterlysning (behandling_id) where status = 'SKAL_AVBRYTES';

create index idx_etterlysning_behandling_id_type_opprettet
    on etterlysning (behandling_id, type) where status = 'OPPRETTET';

create sequence if not exists seq_etterlysning increment by 50 minvalue 1000000;

comment on table etterlysning is 'Inneholder etterlysninger av bekreftelser for endret registeropplysning.';
comment on column etterlysning.id is 'Primary Key. Unik identifikator for etterlysning.';
comment on column etterlysning.behandling_id is 'Referanse til behandling.';
comment on column etterlysning.grunnlag_ref is 'Referanse til registergrunnlaget';
comment on column etterlysning.ekstern_ref is 'Ekstern referanse for etterlysning';
comment on column etterlysning.fom is 'Fom-dato for etterlysning';
comment on column etterlysning.tom is 'Tom-dato for etterlysning';
comment on column etterlysning.frist is 'Frist for å bekrefte etterlysning';
comment on column etterlysning.type is 'Type etterlysning';
comment on column etterlysning.status is 'Status på etterlysning';
comment on column etterlysning.opprettet_tid is 'Tidspunkt for når ytelsen ble opprettet.';
comment on column etterlysning.endret_tid is 'Tidspunkt for når ytelsen sist ble endret.';
