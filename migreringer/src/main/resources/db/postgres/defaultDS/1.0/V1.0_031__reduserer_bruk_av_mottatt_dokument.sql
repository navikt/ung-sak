alter table MOTTATT_DOKUMENT alter column type drop not null;

alter table MOTTATT_DOKUMENT 
 drop column elektronisk_registrert;
 
 alter table MOTTATT_DOKUMENT rename column "xml_payload" to "payload";
 
 alter table MOTTATT_DOKUMENT add column payload_type varchar(10);