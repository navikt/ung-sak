drop table GR_MEDLEMSKAP_VILKAR_PERIODE cascade;

create unique index UIDX_BR_RESULTAT_BEHANDLING_99 ON BR_RESULTAT_BEHANDLING (behandling_id) where (aktiv=true);
create unique index UIDX_GR_BEREGNINGSGRUNNLAG_99 ON GR_BEREGNINGSGRUNNLAG (behandling_id) where (aktiv=true);
create unique index UIDX_GR_MEDLEMSKAP_99 ON GR_MEDLEMSKAP (behandling_id) where (aktiv=true);
create unique index UIDX_GR_PERSONOPPLYSNING_99 ON GR_PERSONOPPLYSNING (behandling_id) where (aktiv=true);
create unique index UIDX_GR_SOEKNAD_99 ON GR_SOEKNAD (behandling_id) where (aktiv=true);
create unique index UIDX_OPPTJENING_99 ON OPPTJENING (behandling_id) where (aktiv=true);
create unique index UIDX_TILBAKEKREVING_INNTREKK_99 ON TILBAKEKREVING_INNTREKK (behandling_id) where (aktiv=true);
create unique index UIDX_TILBAKEKREVING_VALG_99 ON TILBAKEKREVING_VALG (behandling_id) where (aktiv=true);
create unique index UIDX_TOTRINNRESULTATGRUNNLAG_99 ON TOTRINNRESULTATGRUNNLAG (behandling_id) where (aktiv=true);
create unique index UIDX_TOTRINNSVURDERING_99 ON TOTRINNSVURDERING (behandling_id, aksjonspunkt_def) where (aktiv=true);
create unique index UIDX_UTTAK_RESULTAT_99 ON UTTAK_RESULTAT (behandling_resultat_id) where (aktiv=true);

alter table behandling_resultat drop column avslag_arsak;