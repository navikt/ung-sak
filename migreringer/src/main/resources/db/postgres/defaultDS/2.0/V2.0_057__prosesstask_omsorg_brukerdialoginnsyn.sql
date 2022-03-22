Insert into PROSESS_TASK_TYPE
(KODE,NAVN,FEIL_MAKS_FORSOEK,FEIL_SEK_MELLOM_FORSOEK,FEILHANDTERING_ALGORITME,BESKRIVELSE,CRON_EXPRESSION)
values
('brukerdialoginnsyn.publiserJson','Publiserer JSON-hengelse til brukerdialoginnsyn',1,60,'DEFAULT','Publiserer JSON-hengelse til brukerdialoginnsyn',null) ON CONFLICT DO NOTHING;
