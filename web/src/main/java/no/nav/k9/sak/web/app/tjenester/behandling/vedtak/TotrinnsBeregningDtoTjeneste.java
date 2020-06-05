package no.nav.k9.sak.web.app.tjenester.behandling.vedtak;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.kontrakt.vedtak.TotrinnsBeregningDto;
import no.nav.k9.sak.produksjonsstyring.totrinn.BeregningsgrunnlagToTrinn;
import no.nav.k9.sak.produksjonsstyring.totrinn.Totrinnsvurdering;

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

    List<TotrinnsBeregningDto> hentBeregningDto(Totrinnsvurdering aksjonspunkt,
                                                Behandling behandling,
                                                List<BeregningsgrunnlagToTrinn> beregningsgrunnlagGrunnlagUuid) {
        if (beregningsgrunnlagGrunnlagUuid.isEmpty()) {
            return null;
        }
        var ref = BehandlingReferanse.fra(behandling);
        var dtoer = new ArrayList<TotrinnsBeregningDto>();
        for (BeregningsgrunnlagToTrinn beregningsgrunnlagToTrinn : beregningsgrunnlagGrunnlagUuid) {
            TotrinnsBeregningDto dto = new TotrinnsBeregningDto();
            if (aksjonspunkt.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE)) {
                dto.setFastsattVarigEndringNaering(erVarigEndringFastsattForSelvstendingNæringsdrivendeGittGrunnlag(ref, beregningsgrunnlagToTrinn));
            }
            if (AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN.equals(aksjonspunkt.getAksjonspunktDefinisjon())) {
                Beregningsgrunnlag bg = hentBeregningsgrunnlag(ref, beregningsgrunnlagToTrinn);
                List<FaktaOmBeregningTilfelle> tilfeller = bg.getFaktaOmBeregningTilfeller();
                dto.setFaktaOmBeregningTilfeller(tilfeller);
                dto.setSkjæringstidspunkt(bg.getSkjæringstidspunkt());
            }
            dtoer.add(dto);
        }
        return dtoer;
    }

    private Beregningsgrunnlag hentBeregningsgrunnlag(BehandlingReferanse referanse, BeregningsgrunnlagToTrinn beregningsgrunnlagId) {
        return tjeneste.hentGrunnlag(referanse, beregningsgrunnlagId.getSkjæringstidspunkt())
            .flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag)
            .orElseThrow(() -> new IllegalStateException("Fant ikkje beregningsgrunnlag med id " + beregningsgrunnlagId));
    }

    private boolean erVarigEndringFastsattForSelvstendingNæringsdrivendeGittGrunnlag(BehandlingReferanse referanse, BeregningsgrunnlagToTrinn beregningsgrunnlagGrunnlagId) {
        Beregningsgrunnlag beregningsgrunnlag = tjeneste.hentGrunnlag(referanse, beregningsgrunnlagGrunnlagId.getSkjæringstidspunkt())
            .flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag)
            .orElseThrow(() ->
                new IllegalStateException("Fant ingen beregningsgrunnlag med id " + beregningsgrunnlagGrunnlagId.toString()));
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .flatMap(bgps -> bgps.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .anyMatch(andel -> andel.getOverstyrtPrÅr() != null);
    }
}
