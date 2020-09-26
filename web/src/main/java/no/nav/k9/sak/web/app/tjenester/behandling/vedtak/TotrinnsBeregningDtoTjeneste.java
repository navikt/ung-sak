package no.nav.k9.sak.web.app.tjenester.behandling.vedtak;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
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

@Dependent
public class TotrinnsBeregningDtoTjeneste {
    private static final AksjonspunktDefinisjon FAKTA_ATFL_SN = AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN;
    private static final AksjonspunktDefinisjon VARIG_ENDRET_SN = AksjonspunktDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE;
    private static final Set<AksjonspunktDefinisjon> HENT_GRUNNLAG_APDEFS = EnumSet.of(VARIG_ENDRET_SN, FAKTA_ATFL_SN);

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

        Map<BeregningsgrunnlagToTrinn, Beregningsgrunnlag> totrinnTilBeregningsgrunnlagMap = Collections.emptyMap();
        var apDef = aksjonspunkt.getAksjonspunktDefinisjon();
        if (HENT_GRUNNLAG_APDEFS.contains(apDef)) {
            totrinnTilBeregningsgrunnlagMap = hentBeregningsgrunnlag(ref, beregningsgrunnlagTotrinn);
        }

        for (var bgTotrinn : beregningsgrunnlagTotrinn) {
            Beregningsgrunnlag bg = totrinnTilBeregningsgrunnlagMap.get(bgTotrinn);
            TotrinnsBeregningDto dto = new TotrinnsBeregningDto();

            if (VARIG_ENDRET_SN.equals(apDef)) {
                dto.setFastsattVarigEndringNaering(erVarigEndringFastsattForSelvstendingNæringsdrivendeGittGrunnlag(bgTotrinn, bg));
            }

            if (FAKTA_ATFL_SN.equals(apDef)) {
                List<FaktaOmBeregningTilfelle> tilfeller = Objects.requireNonNull(bg, "Mangler beregningsgrunnlag for " + bgTotrinn).getFaktaOmBeregningTilfeller();
                dto.setFaktaOmBeregningTilfeller(tilfeller);
                dto.setSkjæringstidspunkt(bg.getSkjæringstidspunkt());
            }

            // TODO : er det verdt å returnere tom dto for andre tilfeller her?
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
