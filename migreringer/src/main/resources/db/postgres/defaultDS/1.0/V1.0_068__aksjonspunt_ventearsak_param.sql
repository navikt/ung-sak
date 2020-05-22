alter table AKSJONSPUNKT add column vent_aarsak_variant varchar(200);

alter table AKSJONSPUNKT alter column vent_aarsak DROP NOT NULL;
