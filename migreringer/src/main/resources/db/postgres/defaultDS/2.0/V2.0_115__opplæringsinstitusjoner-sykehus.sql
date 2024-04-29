insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), 'bcbc78d9-465b-48c6-913a-bf9b1eeb0faf', 'Stavanger Universitetssykehus', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'Stavanger Universitetssykehus'), '01.01.2000', '01.01.2100', null, null);

insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), '8671b60e-fd47-4097-b005-c376ea0fa240', 'St. Olavs Hospital', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'St. Olavs Hospital'), '01.01.2000', '01.01.2100', null, null);

insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), '053a0d6d-37bc-4618-8dff-b6321e927534', 'Sunnaas sykehus', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'Sunnaas sykehus'), '01.01.2000', '01.01.2100', null, null);

insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), 'e381480a-a0e8-4199-b963-82ef5ebbb9f3', 'Sykehus Asker/Bærum', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'Sykehus Asker/Bærum'), '01.01.2000', '01.01.2100', null, null);

insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), '503c6d3d-6c97-4de6-9ce8-5d3fbc45488c', 'Sykehuset Buskerud (Drammen sykehus)', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'Sykehuset Buskerud (Drammen sykehus)'), '01.01.2000', '01.01.2100', null, null);

insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), 'b2c5b913-40ed-4557-93cc-08f525bee5a9', 'Sykehuset i Vestfold', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'Sykehuset i Vestfold'), '01.01.2000', '01.01.2100', null, null);

insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), '45ac3eda-6e3e-45f5-99d3-670fbc9aaf52', 'Sykehuset Innlandet Elverum', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'Sykehuset Innlandet Elverum'), '01.01.2000', '01.01.2100', null, null);

insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), 'd0cd1cca-261a-48b2-bf03-65cfad35a810', 'Sykehuset Innlandet Gjøvik', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'Sykehuset Innlandet Gjøvik'), '01.01.2000', '01.01.2100', null, null);

insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), 'b0e72be4-ac00-4dda-bfb6-1f19e2435d47', 'Sykehuset Innlandet Hamar', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'Sykehuset Innlandet Hamar'), '01.01.2000', '01.01.2100', null, null);

insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), 'b0a41fdf-05a9-4998-8415-8503b6beb70b', 'Sykehuset Innlandet Kongsvinger', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'Sykehuset Innlandet Kongsvinger'), '01.01.2000', '01.01.2100', null, null);

insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), '739ba29f-eaf9-46f9-8e15-e1e48e030211', 'Sykehuset Innlandet Lillehammer', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'Sykehuset Innlandet Lillehammer'), '01.01.2000', '01.01.2100', null, null);

insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), '2c8424c3-8210-45ed-a982-3938a8afedc3', 'Sykehuset Innlandet Tynset', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'Sykehuset Innlandet Tynset'), '01.01.2000', '01.01.2100', null, null);

insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), 'a97e9ab3-1c88-446a-9f44-eed058becff1', 'Sykehuset Telemark', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'Sykehuset Telemark'), '01.01.2000', '01.01.2100', null, null);

insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), 'ea2cb9d3-add5-41fc-b16f-947a2855f6d2', 'Sykehuset Østfold', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'Sykehuset Østfold'), '01.01.2000', '01.01.2100', null, null);

insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), '8a839290-d91b-423c-82f7-4b69559791d5', 'Sørlandet sykehus, Arendal', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'Sørlandet sykehus, Arendal'), '01.01.2000', '01.01.2100', null, null);

insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), '4c10bc2d-2f2b-47a8-ab3b-64bdb5b12e50', 'Sørlandet sykehus, Farsund', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'Sørlandet sykehus, Farsund'), '01.01.2000', '01.01.2100', null, null);

insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), '829dd08f-a6f2-4d42-a318-127057dad1fb', 'Sørlandet sykehus, Kristiansand', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'Sørlandet sykehus, Kristiansand'), '01.01.2000', '01.01.2100', null, null);

insert into godkjente_opplaeringsinstitusjoner (id, uuid, navn, endret_av, endret_tid) VALUES (nextval('seq_godkjente_opplaeringsinstitusjoner'), '877cc841-a837-4619-8f53-7ee29775de66', 'Sørlandet sykehus, Mandal', null, null);
insert into godkjent_opplaeringsinstitusjon_periode (id, institusjon_id, fom, tom, endret_av, endret_tid) VALUES (nextval('seq_godkjent_opplaeringsinstitusjon_periode'), (select id from godkjente_opplaeringsinstitusjoner where navn = 'Sørlandet sykehus, Mandal'), '01.01.2000', '01.01.2100', null, null);
