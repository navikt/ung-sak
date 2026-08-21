alter table bosatt_periode_avklaring
    add column if not exists avklaringtype varchar(50) default 'AVSLAG' not null;

alter table bosatt_periode_avklaring
    alter column avklaringtype drop default;
