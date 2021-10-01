alter table SYKDOM_DOKUMENT RENAME CONSTRAINT "fk_sykdom_vurdering_01" to "fk_sykdom_dokument_01";

alter table SYKDOM_DOKUMENT add constraint fk_sykdom_dokument_02 foreign key (behandling_uuid) references behandling(uuid);

alter table SYKDOM_DOKUMENT_INFORMASJON add constraint fk_sykdom_dokument_informasjon foreign key (sykdom_dokument_id) references sykdom_dokument(id)
