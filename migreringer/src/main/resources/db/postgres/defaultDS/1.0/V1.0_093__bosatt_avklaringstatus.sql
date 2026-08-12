alter table bosatt_periode_avklaring
    add column if not exists status varchar(50) default 'UNDER_ARBEID' not null;

alter table bosatt_periode_avklaring
    drop column if exists er_bosatt_i_trondheim;
