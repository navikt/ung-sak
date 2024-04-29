alter table medlemskap_vurdering_lopende add column if not exists vurdert_av varchar(20);
alter table medlemskap_vurdering_lopende add column if not exists vurdert_tid timestamp(3);
