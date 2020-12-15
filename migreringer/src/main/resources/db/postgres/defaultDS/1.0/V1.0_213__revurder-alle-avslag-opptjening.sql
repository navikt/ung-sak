INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_payload)
SELECT nextval('seq_prosess_task'),
       'forvaltning.opprettManuellRevurdering',
       nextval('seq_prosess_task_gruppe'),
       null,
       f.saksnummer
FROM (
         SELECT DISTINCT b.fagsak_id
         FROM VR_VILKAR_PERIODE p INNER JOIN VR_VILKAR v ON (
                 v.id = p.vilkar_id
             ) INNER JOIN VR_VILKAR_RESULTAT r ON (
                 r.id = v.vilkar_resultat_id
             ) INNER JOIN RS_VILKARS_RESULTAT r2 ON (
                     r2.vilkarene_id = r.id
                 AND r2.aktiv = true
             ) INNER JOIN Behandling b ON (
                 b.id = r2.behandling_id
             )
         WHERE v.vilkar_type = 'FP_VK_23'
           AND p.utfall = 'IKKE_OPPFYLT'
           AND NOT EXISTS (
                 SELECT *
                 FROM Behandling b2
                 WHERE b2.id != b.id
                   AND b2.opprettet_dato > b.opprettet_dato
                   AND b2.fagsak_id = b.fagsak_id
             )
     ) b INNER JOIN Fagsak f ON (
            f.id = b.fagsak_id
        AND f.ytelse_type = 'OMP'
    )
;
