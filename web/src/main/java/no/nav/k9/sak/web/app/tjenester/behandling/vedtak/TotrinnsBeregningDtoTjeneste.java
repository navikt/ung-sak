package no.nav.k9.sak.web.app.tjenester.behandling.vedtak;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
                                                List<BeregningsgrunnlagToTrinn> beregningsgrunnlagTotrinn) {
        if (beregningsgrunnlagTotrinn.isEmpty()) {
            return null;
        }
        var ref = BehandlingReferanse.fra(behandling);
        var dtoer = new ArrayList<TotrinnsBeregningDto>();

        var totrinnTilBeregningsgrunnlagMap = hentBeregningsgrunnlag(ref, beregningsgrunnlagTotrinn);

        for (var bgTotrinn : beregningsgrunnlagTotrinn) {
            Beregningsgrunnlag bg = totrinnTilBeregningsgrunnlagMap.get(bgTotrinn);
            TotrinnsBeregningDto dto = new TotrinnsBeregningDto();
            if (aksjonspunkt.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE)) {
                dto.setFastsattVarigEndringNaering(erVarigEndringFastsattForSelvstendingNæringsdrivendeGittGrunnlag(bgTotrinn, bg));
            }
            if (AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN.equals(aksjonspunkt.getAksjonspunktDefinisjon())) {
                List<FaktaOmBeregningTilfelle> tilfeller = bg.getFaktaOmBeregningTilfeller();
                dto.setFaktaOmBeregningTilfeller(tilfeller);
                dto.setSkjæringstidspunkt(bg.getSkjæringstidspunkt());
            }
            dtoer.add(dto);
        }
        return dtoer;
    }

    private Map<BeregningsgrunnlagToTrinn, Beregningsgrunnlag> hentBeregningsgrunnlag(BehandlingReferanse ref, List<BeregningsgrunnlagToTrinn> beregningsgrunnlagTotrinn) {
        var skjæringstidspunkter = beregningsgrunnlagTotrinn.stream().collect(Collectors.toMap(v -> v.getSkjæringstidspunkt(), v -> v));

        var beregningsgrunnlag = tjeneste.hentGrunnlag(ref, skjæringstidspunkter.keySet())
            .stream()
            .map(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(bg -> skjæringstidspunkter.get(bg.getSkjæringstidspunkt()), bg -> bg));
        return beregningsgrunnlag;
    }

    private boolean erVarigEndringFastsattForSelvstendingNæringsdrivendeGittGrunnlag(BeregningsgrunnlagToTrinn bgTotrinn, Beregningsgrunnlag beregningsgrunnlag) {
        Objects.requireNonNull(beregningsgrunnlag, "Fant ingen beregningsgrunnlag for " + bgTotrinn);

        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .flatMap(bgps -> bgps.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .anyMatch(andel -> andel.getOverstyrtPrÅr() != null);
    }
}
