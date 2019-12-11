package no.nav.folketrygdloven.beregningsgrunnlag;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;

public class RyddBeregningsgrunnlag {

    private final BehandlingskontrollKontekst kontekst;
    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    RyddBeregningsgrunnlag(BeregningsgrunnlagRepository beregningsgrunnlagRepository, BehandlingskontrollKontekst kontekst) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.kontekst = kontekst;
    }

    public void ryddFastsettSkjæringstidspunktVedTilbakeføring() {
        beregningsgrunnlagRepository.deaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId());
    }

    public void gjenopprettOppdatertBeregningsgrunnlag() {
        beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }

    public void gjenopprettFastsattBeregningAktivitetBeregningsgrunnlag() {
        boolean bgReaktivert = beregningsgrunnlagRepository
            .reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        if (!bgReaktivert) {
            gjenopprettFørsteBeregningsgrunnlag();
        }
    }

    public void ryddForeslåBeregningsgrunnlagVedTilbakeføring() {
        beregningsgrunnlagRepository
            .reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.FORESLÅTT);
    }


    private void gjenopprettFørsteBeregningsgrunnlag() {
        beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.OPPRETTET);
    }

    public void ryddFordelBeregningsgrunnlagVedTilbakeføring(boolean harAksjonspunktSomErUtførtIUtgang) {
        if (harAksjonspunktSomErUtførtIUtgang) {
            if (beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.FASTSATT_INN).isPresent()) {
                beregningsgrunnlagRepository
                    .reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.FASTSATT_INN);
            } else {
                beregningsgrunnlagRepository
                    .reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);
            }
        } else {
            beregningsgrunnlagRepository
                .reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);
        }
    }
}
