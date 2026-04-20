create sequence seq_ung_ungdomsprogram_utvidet_kvote_id increment by 50 minvalue 1000000;

create table ung_ungdomsprogram_utvidet_kvote
(
    id                bigint                                 not null primary key,
    har_utvidet_kvote boolean      default false             not null,
    opprettet_av      varchar(20)  default 'VL'              not null,
    opprettet_tid     timestamp(3) default current_timestamp not null,
    endret_av         varchar(20),
    endret_tid        timestamp(3)
);

comment on table ung_ungdomsprogram_utvidet_kvote is 'Angir om bruker har utvidet kvote i ungdomsprogrammet.';

alter table ung_gr_ungdomsprogramperiode
    add column ung_ungdomsprogramp_utvidet_kvote_id bigint references ung_ungdomsprogram_utvidet_kvote (id);

create index idx_ung_gr_ungdomsprogramperiode_utvidet_kvote
    on ung_gr_ungdomsprogramperiode (ung_ungdomsprogramp_utvidet_kvote_id);

