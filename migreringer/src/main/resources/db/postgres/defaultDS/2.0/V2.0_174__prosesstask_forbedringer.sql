--fjerne unødvendig indekser
drop index idx_prosess_task_1;
drop index idx_prosess_task_2;
drop index idx_prosess_task_3;
drop index idx_prosess_task_5;

-- brukte tidligere task_type som gruppering hvis gruppering ikke var satt
update prosess_task set task_gruppe = task_type where task_gruppe is null;
--krev at task_gruppe settes ved bruk
alter table prosess_task ALTER COLUMN task_gruppe SET NOT NULL;

--unngå at vi får negative tall i sekvens, ville gitt merkelig sortering p.t.
alter table prosess_task_partition_default add constraint ikke_negativ_sekvens check (
    not starts_with(task_sekvens, '-')
    );

create index idx_prosess_task_8 on prosess_task_partition_default (task_gruppe, length(task_sekvens), task_sekvens)
    where status in ('FEILET', 'KLAR', 'VENTER_SVAR', 'SUSPENDERT', 'VETO');

--hyppig endringer på tasker, bør ikke ha default fillfactor (100)
alter table prosess_task_partition_default set (fillfactor = 40);
