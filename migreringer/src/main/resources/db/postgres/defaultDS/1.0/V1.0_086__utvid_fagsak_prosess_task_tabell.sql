alter table fagsak_prosess_task add column TASK_TYPE VARCHAR(50);
create  index IDX_FAGSAK_PROSESS_TASK_5 on FAGSAK_PROSESS_TASK (TASK_TYPE);

alter table fagsak_prosess_task add constraint FK_FAGSAK_PROSESS_TASK_5 foreign key (TASK_TYPE) references PROSESS_TASK_TYPE(KODE); 

-- tbd utsetter oppdatering av data i fagsak_prosess_task til det skrives fra app (blir mindre aa oppdatere etterpa)