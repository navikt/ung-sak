-- Migrering: legg til fraflyttingsårsak og begrunnelse på bosatt_periode_avklaring
--
-- fraflyttings_aarsak: årsak til fraflytting (enum-verdi som varchar)
-- begrunnelse_ved_annet: fritekstvurdering fra saksbehandler, kun relevant ved årsak=ANNET

alter table bosatt_periode_avklaring
    add column fraflyttings_aarsak varchar(100),
    add column begrunnelse_ved_annet varchar(4000);
