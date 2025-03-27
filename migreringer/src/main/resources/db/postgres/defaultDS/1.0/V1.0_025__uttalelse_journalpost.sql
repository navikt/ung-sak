alter table uttalelse add svar_journalpost_id varchar(20);
comment on column uttalelse.svar_journalpost_id is 'journalpost_id for svar på etterlysning';

update uttalelse set svar_journalpost_id = (select svar_journalpost_id from etterlysning where etterlysning.id = uttalelse.etterlysning_id);
alter table uttalelse alter column svar_journalpost_id set not null;
alter table etterlysning drop column svar_journalpost_id;
