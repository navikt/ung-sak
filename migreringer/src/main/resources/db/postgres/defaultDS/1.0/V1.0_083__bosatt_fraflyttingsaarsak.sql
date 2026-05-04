-- Migrering: legg til fraflyttingsårsak på bosatt_periode_avklaring
--
-- fraflyttings_aarsak: årsak til fraflytting (enum-verdi som varchar)

alter table bosatt_periode_avklaring
    add column fraflyttings_aarsak varchar(100);
