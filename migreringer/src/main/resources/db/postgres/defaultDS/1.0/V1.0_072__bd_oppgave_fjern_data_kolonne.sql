-- Fjerner jsonb data-kolonne fra BD_OPPGAVE.
-- Strukturert oppgavedata lagres nå i egne BD_OPPGAVE_DATA_*-tabeller (se V1.0_071),
-- og referansen holdes via oppgave_data_id / type-kolonnen (@Any-mapping).

alter table BD_OPPGAVE
    drop column data;

