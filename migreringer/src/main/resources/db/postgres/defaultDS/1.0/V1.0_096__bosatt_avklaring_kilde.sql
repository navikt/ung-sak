-- Kilde til opplysningene bak bostedsavklaringen. Legges på begge variantene av avklaring.

alter table bosatt_periode_avklaring
    add column if not exists kilde varchar(100),
    add column if not exists kilde_fritekst varchar(1000);

alter table bosatt_periode_avklaring_foreslaatt
    add column if not exists kilde varchar(100),
    add column if not exists kilde_fritekst varchar(1000);

-- Kun for dev: eksisterende rader har ingen kilde registrert.
update bosatt_periode_avklaring set kilde = 'BRUKER' where kilde is null;
update bosatt_periode_avklaring_foreslaatt set kilde = 'BRUKER' where kilde is null;

alter table bosatt_periode_avklaring
    alter column kilde set not null;
alter table bosatt_periode_avklaring_foreslaatt
    alter column kilde set not null;

comment on column bosatt_periode_avklaring.kilde is 'Hvor saksbehandler har fått opplysningene fra, jf. BostedsavklaringKildeType.';
comment on column bosatt_periode_avklaring.kilde_fritekst is 'Fritekstbeskrivelse av kilde. Settes kun når kilde = ANNET.';
comment on column bosatt_periode_avklaring_foreslaatt.kilde is 'Hvor saksbehandler har fått opplysningene fra, jf. BostedsavklaringKildeType.';
comment on column bosatt_periode_avklaring_foreslaatt.kilde_fritekst is 'Fritekstbeskrivelse av kilde. Settes kun når kilde = ANNET.';
