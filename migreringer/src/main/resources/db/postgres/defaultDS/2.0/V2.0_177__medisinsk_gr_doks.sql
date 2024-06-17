create table if not exists medisinsk_grunnlagsdata_sykdomsdokumenter
(
    medisinsk_grunnlagsdata_id        bigint                                 not null
        constraint fk_medisinsk_grunnlagsdata_dokumenter_01
            references medisinsk_grunnlagsdata,
    pleietrengende_sykdom_dokument_id bigint                                 not null
        constraint fk_medisinsk_grunnlagsdata_dokumenter_02
            references pleietrengende_sykdom_dokument,
    opprettet_av                      varchar(20)  default 'vl'              not null,
    opprettet_tid                     timestamp(3) default current_timestamp not null
);

create unique index medisinsk_grunnlagsdata_sykdomsdokumenter_unique_idx
    on medisinsk_grunnlagsdata_sykdomsdokumenter (medisinsk_grunnlagsdata_id, pleietrengende_sykdom_dokument_id);
