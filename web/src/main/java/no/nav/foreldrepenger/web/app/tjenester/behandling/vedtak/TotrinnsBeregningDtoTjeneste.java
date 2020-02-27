package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.sak.kontrakt.vedtak.TotrinnsBeregningDto;

@ApplicationScoped
public class TotrinnsBeregningDtoTjeneste {
    private BeregningTjeneste tjeneste;

    protected TotrinnsBeregningDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public TotrinnsBeregningDtoTjeneste(BeregningTjeneste tjeneste) {
        this.tjeneste = tjeneste;
    }

    TotrinnsBeregningDto hentBeregningDto(Totrinnsvurdering aksjonspunkt,
                                                  Behandling behandling,
                                                  Optional<UUID> beregningsgrunnlagGrunnlagUuid) {
        TotrinnsBeregningDto dto = new TotrinnsBeregningDto();
        if (aksjonspunkt.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE)) {
            if (beregningsgrunnlagGrunnlagUuid.isPresent()) {
                dto.setFastsattVarigEndringNaering(erVarigEndringFastsattForSelvstendingNæringsdrivendeGittGrunnlag(beregningsgrunnlagGrunnlagUuid.get(), behandling.getId()));
            } else {
                dto.setFastsattVarigEndringNaering(erVarigEndringFastsattForSelvstendingNæringsdrivendeGittBehandlingId(behandling.getId()));
            }
        }
        if (AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN.equals(aksjonspunkt.getAksjonspunktDefinisjon())) {
            Beregningsgrunnlag bg = hentBeregningsgrunnlag(behandling, beregningsgrunnlagGrunnlagUuid);
            List<FaktaOmBeregningTilfelle> tilfeller = bg.getFaktaOmBeregningTilfeller();
            dto.setFaktaOmBeregningTilfeller(tilfeller);
        }
        return dto;
    }

    private Beregningsgrunnlag hentBeregningsgrunnlag(Behandling behandling, Optional<UUID> beregningsgrunnlagId) {
        if (beregningsgrunnlagId.isPresent()) {
            return tjeneste.hentBeregningsgrunnlagForId(beregningsgrunnlagId.get(), behandling.getId())
                .orElseThrow(() -> new IllegalStateException("Fant ikkje beregningsgrunnlag med id " + beregningsgrunnlagId.get()));
        } else {
            return tjeneste.hentFastsatt(behandling.getId()).orElseThrow();
        }
    }

    private boolean erVarigEndringFastsattForSelvstendingNæringsdrivendeGittBehandlingId(Long behandlingId) {
        Beregningsgrunnlag beregningsgrunnlag = tjeneste.hentFastsatt(behandlingId).orElseThrow();

        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .flatMap(bgps -> bgps.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .anyMatch(andel -> andel.getOverstyrtPrÅr() != null);
    }

    private boolean erVarigEndringFastsattForSelvstendingNæringsdrivendeGittGrunnlag(UUID beregningsgrunnlagGrunnlagId, Long behandlingId) {
        Beregningsgrunnlag beregningsgrunnlag = tjeneste.hentBeregningsgrunnlagForId(beregningsgrunnlagGrunnlagId, behandlingId)
            .orElseThrow(() ->
                new IllegalStateException("Fant ingen beregningsgrunnlag med id " + beregningsgrunnlagGrunnlagId.toString()));
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .flatMap(bgps -> bgps.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .anyMatch(andel -> andel.getOverstyrtPrÅr() != null);
    }
}
