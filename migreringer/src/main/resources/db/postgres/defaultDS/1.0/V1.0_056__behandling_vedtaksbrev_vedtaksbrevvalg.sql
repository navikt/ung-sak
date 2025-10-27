-- kobler behandling_vedtaksbrev til vedtaksbrev_valg hvis den har brukt valg i bestillingen
alter table behandling_vedtaksbrev
    add column vedtaksbrev_valg_id bigint,
    add constraint fk_vedtaksbrev_valg
        foreign key (vedtaksbrev_valg_id)
            references vedtaksbrev_valg (id),
    add constraint unique_vedtaksbrev_valg_id unique (vedtaksbrev_valg_id),
    add constraint unique_brevbestilling_id unique (brevbestilling_id)
