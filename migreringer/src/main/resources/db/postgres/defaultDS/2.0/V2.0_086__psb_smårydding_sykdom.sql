alter table SYKDOM_GRUNNLAG_BEHANDLING add constraint FK_SYKDOM_GRUNNLAG_BEHANDLING_04 foreign key (behandling_uuid) references behandling(uuid);
