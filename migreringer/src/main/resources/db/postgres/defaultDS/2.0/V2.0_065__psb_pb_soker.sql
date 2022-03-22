create table if not exists PSB_PB_SAK
(
    ID BIGINT NOT NULL
);

create UNIQUE index PK_PSB_PB_SAK on PSB_PB_SAK(ID);
alter table PSB_PB_SAK add constraint FK_PSB_PB_SAK_1 foreign key (ID) references FAGSAK(ID);