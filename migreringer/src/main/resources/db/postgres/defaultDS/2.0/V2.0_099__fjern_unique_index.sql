DROP INDEX IF EXISTS UIDX_BG_PGI_PERIODE_01;
DROP INDEX IF EXISTS UIDX_BG_PGI_PERIODE_02;

CREATE INDEX if not exists UIDX_BG_PGI_PERIODE_01 ON BG_PGI_PERIODE (bg_pgi_id, skjaeringstidspunkt);
CREATE INDEX if not exists UIDX_BG_PGI_PERIODE_02 ON BG_PGI_PERIODE (bg_pgi_id, iay_referanse);
CREATE UNIQUE INDEX if not exists UIDX_BG_PGI_PERIODE_03 ON BG_PGI_PERIODE (bg_pgi_id, iay_referanse, skjaeringstidspunkt);
