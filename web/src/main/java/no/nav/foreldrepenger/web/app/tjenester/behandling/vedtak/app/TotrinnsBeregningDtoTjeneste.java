package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnsBeregningDto;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;

@ApplicationScoped
public class TotrinnsBeregningDtoTjeneste {
    private HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    protected TotrinnsBeregningDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public TotrinnsBeregningDtoTjeneste(HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
    }

    TotrinnsBeregningDto hentBeregningDto(Totrinnsvurdering aksjonspunkt,
                                                  Behandling behandling,
                                                  Optional<Long> beregningsgrunnlagId) {
        TotrinnsBeregningDto dto = new TotrinnsBeregningDto();
        if (aksjonspunkt.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE)) {
            if (beregningsgrunnlagId.isPresent()) {
                dto.setFastsattVarigEndringNaering(erVarigEndringFastsattForSelvstendingNæringsdrivendeGittGrunnlag(beregningsgrunnlagId.get()));
            } else {
                dto.setFastsattVarigEndringNaering(erVarigEndringFastsattForSelvstendingNæringsdrivendeGittBehandlingId(behandling.getId()));
            }
        }
        if (AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN.equals(aksjonspunkt.getAksjonspunktDefinisjon())) {
            BeregningsgrunnlagEntitet bg = hentBeregningsgrunnlag(behandling, beregningsgrunnlagId);
            List<FaktaOmBeregningTilfelle> tilfeller = bg.getFaktaOmBeregningTilfeller();
            dto.setFaktaOmBeregningTilfeller(tilfeller);
        }
        return dto;
    }

    private BeregningsgrunnlagEntitet hentBeregningsgrunnlag(Behandling behandling, Optional<Long> beregningsgrunnlagId) {
        if (beregningsgrunnlagId.isPresent()) {
            return beregningsgrunnlagTjeneste.hentBeregningsgrunnlagForId(beregningsgrunnlagId.get())
                .orElseThrow(() -> new IllegalStateException("Fant ikkje beregningsgrunnlag med id " + beregningsgrunnlagId.get()));
        } else {
            return beregningsgrunnlagTjeneste.hentBeregningsgrunnlagAggregatForBehandling(behandling.getId());
        }
    }

    private boolean erVarigEndringFastsattForSelvstendingNæringsdrivendeGittBehandlingId(Long behandlingId) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagAggregatForBehandling(behandlingId);

        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .flatMap(bgps -> bgps.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .anyMatch(andel -> andel.getOverstyrtPrÅr() != null);
    }

    private boolean erVarigEndringFastsattForSelvstendingNæringsdrivendeGittGrunnlag(Long beregningsgrunnlagId) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagForId(beregningsgrunnlagId)
            .orElseThrow(() ->
                new IllegalStateException("Fant ingen beregningsgrunnlag med id " + beregningsgrunnlagId.toString()));
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .flatMap(bgps -> bgps.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .anyMatch(andel -> andel.getOverstyrtPrÅr() != null);
    }
}
