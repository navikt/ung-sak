alter table omsorgen_for_periode add column if not exists vurdert_av varchar(20);
alter table omsorgen_for_periode add column if not exists vurdert_tid timestamp(3);
