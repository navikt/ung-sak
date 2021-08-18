UPDATE PSB_UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE u1
SET aktiv = true
WHERE NOT EXISTS (
    SELECT *
    FROM PSB_UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE u2
    WHERE u2.pleietrengende_aktoer_id = u1.pleietrengende_aktoer_id
      AND u2.aktiv = true
  ) AND u1.opprettet_tid = (
    SELECT MAX(u2.opprettet_tid)
    FROM PSB_UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE u2
    WHERE u2.pleietrengende_aktoer_id = u1.pleietrengende_aktoer_id
  );