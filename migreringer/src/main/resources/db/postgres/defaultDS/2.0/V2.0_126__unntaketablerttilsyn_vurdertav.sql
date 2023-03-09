alter table psb_unntak_etablert_tilsyn_periode add column if not exists vurdert_av varchar(20);
alter table psb_unntak_etablert_tilsyn_periode add column if not exists vurdert_tid timestamp(3);
