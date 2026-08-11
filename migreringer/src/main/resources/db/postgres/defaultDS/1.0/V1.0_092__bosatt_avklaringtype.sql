alter table bosatt_periode_avklaring
    add column if not exists avklaringtype varchar(50) default 'AVSLAG';

update bosatt_periode_avklaring
    set avklaringtype = 'AVSLAG'
    where avklaringtype is null;

alter table bosatt_periode_avklaring
    alter column avklaringtype set not null;
