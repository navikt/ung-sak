alter table olp_vurdert_institusjon add column if not exists vurdert_av VARCHAR(20) default 'VL' not null;
alter table olp_vurdert_institusjon add column if not exists vurdert_tid TIMESTAMP(3) default CURRENT_TIMESTAMP not null;
alter table olp_vurdert_institusjon alter column vurdert_av drop default;
alter table olp_vurdert_institusjon alter column vurdert_tid drop default;

alter table olp_vurdert_opplaering add column if not exists vurdert_av VARCHAR(20) default 'VL' not null;
alter table olp_vurdert_opplaering add column if not exists vurdert_tid TIMESTAMP(3) default CURRENT_TIMESTAMP not null;
alter table olp_vurdert_opplaering alter column vurdert_av drop default;
alter table olp_vurdert_opplaering alter column vurdert_tid drop default;

alter table olp_vurdert_opplaering_periode add column if not exists vurdert_av VARCHAR(20) default 'VL' not null;
alter table olp_vurdert_opplaering_periode add column if not exists vurdert_tid TIMESTAMP(3) default CURRENT_TIMESTAMP not null;
alter table olp_vurdert_opplaering_periode alter column vurdert_av drop default;
alter table olp_vurdert_opplaering_periode alter column vurdert_tid drop default;

alter table olp_vurdert_reisetid add column if not exists vurdert_av VARCHAR(20) default 'VL' not null;
alter table olp_vurdert_reisetid add column if not exists vurdert_tid TIMESTAMP(3) default CURRENT_TIMESTAMP not null;
alter table olp_vurdert_reisetid alter column vurdert_av drop default;
alter table olp_vurdert_reisetid alter column vurdert_tid drop default;
