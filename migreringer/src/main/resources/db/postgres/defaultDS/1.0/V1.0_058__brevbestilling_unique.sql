drop index if exists uidx_brevbestilling_behandling_vedtak_mal;

create unique index uidx_brevbestilling_behandling_vedtak_mal
    on brevbestilling (behandling_id, dokumentmal_type)
    where vedtaksbrev = true and aktiv = true and dokumentmal_type != 'MANUELL';
