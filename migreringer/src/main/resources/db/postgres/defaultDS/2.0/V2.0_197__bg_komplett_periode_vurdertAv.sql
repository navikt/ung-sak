alter table bg_komplett_periode add column if not exists vurdert_av varchar(20);
alter table bg_komplett_periode add column if not exists vurdert_tidspunkt timestamp(3);
