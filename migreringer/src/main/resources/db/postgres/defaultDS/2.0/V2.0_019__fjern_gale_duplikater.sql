UPDATE SYKDOM_DOKUMENT_INFORMASJON d
SET duplikat_av_sykdom_dokument_id = null
WHERE d.duplikat_av_sykdom_dokument_id IS NOT NULL
  AND EXISTS (
    SELECT *
    FROM SYKDOM_VURDERING_VERSJON_DOKUMENT v
    WHERE v.sykdom_dokument_id = d.sykdom_dokument_id
  );