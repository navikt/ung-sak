alter table olp_vurdert_institusjon add column vurdert_av VARCHAR(20) not null;
alter table olp_vurdert_institusjon add column vurdert_tid TIMESTAMP(3) not null;

alter table olp_vurdert_opplaering add column vurdert_av VARCHAR(20) not null;
alter table olp_vurdert_opplaering add column vurdert_tid TIMESTAMP(3) not null;

alter table olp_vurdert_opplaering_periode add column vurdert_av VARCHAR(20) not null;
alter table olp_vurdert_opplaering_periode add column vurdert_tid TIMESTAMP(3) not null;

alter table olp_vurdert_reisetid add column vurdert_av VARCHAR(20) not null;
alter table olp_vurdert_reisetid add column vurdert_tid TIMESTAMP(3) not null;

