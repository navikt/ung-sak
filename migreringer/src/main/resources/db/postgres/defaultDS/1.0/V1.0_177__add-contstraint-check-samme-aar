-- upper gir dato etter tom (alltid exclusive i db)
alter table br_andel add constraint chk_br_andel_samme_aar check (periode is null OR date_part('year'::text, lower(periode)) = date_part('year'::text, upper(periode) - interval '1 day'));

