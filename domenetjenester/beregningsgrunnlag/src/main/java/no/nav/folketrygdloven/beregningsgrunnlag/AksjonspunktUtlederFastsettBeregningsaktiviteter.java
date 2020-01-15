package no.nav.folketrygdloven.beregningsgrunnlag;

import static java.util.Collections.emptyList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktDefinisjon;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningVenteårsak;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;

public abstract class AksjonspunktUtlederFastsettBeregningsaktiviteter {

    private AvklarAktiviteterTjeneste avklarAktiviteterTjeneste;
    private BeregningsperiodeTjeneste beregningsperiodeTjeneste;

    protected AksjonspunktUtlederFastsettBeregningsaktiviteter() {
        // For CDI
    }

    public AksjonspunktUtlederFastsettBeregningsaktiviteter(AvklarAktiviteterTjeneste avklarAktiviteterTjeneste,
                                                            BeregningsperiodeTjeneste beregningsperiodeTjeneste) {
        this.avklarAktiviteterTjeneste = avklarAktiviteterTjeneste;
        this.beregningsperiodeTjeneste = beregningsperiodeTjeneste;
    }

    public abstract List<BeregningAksjonspunktResultat> utledAksjonspunkterFor(BehandlingReferanse behandlingReferanse,
                                                                      BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                                      BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                                                      boolean erOverstyrt,
                                                                      BeregningsgrunnlagInput bgInput);

    protected List<BeregningAksjonspunktResultat> utledAksjonspunkterForFelles(BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                                               BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                                                               Collection<Inntektsmelding> inntektsmeldinger,
                                                                               Optional<AktørYtelse> aktørYtelse,
                                                                               boolean erOverstyrt) {
        List<Arbeidsgiver> arbeidsgivere = inntektsmeldinger.stream().map(Inntektsmelding::getArbeidsgiver).collect(Collectors.toList());
        if (beregningsperiodeTjeneste.skalVentePåInnrapporteringAvInntekt(beregningsgrunnlag, arbeidsgivere, LocalDate.now())) {
            return List.of(opprettSettPåVentAutopunktForVentPåRapportering(beregningsgrunnlag));
        }
        Optional<LocalDate> ventPåMeldekortFrist = AutopunktUtlederFastsettBeregningsaktiviteterTjeneste.skalVenteTilDatoPåMeldekortAAPellerDP(aktørYtelse, beregningsgrunnlag, LocalDate.now());
        if (ventPåMeldekortFrist.isPresent()) {
            return List.of(opprettSettPåVentAutopunktMeldekort(ventPåMeldekortFrist.get()));
        }
        if (erOverstyrt) {
            return emptyList();
        }

        if (avklarAktiviteterTjeneste.skalAvklareAktiviteter(beregningsgrunnlag, beregningAktivitetAggregat, aktørYtelse)) {
            return List.of(BeregningAksjonspunktResultat.opprettFor(BeregningAksjonspunktDefinisjon.AVKLAR_AKTIVITETER));
        }
        return emptyList();
    }

    private BeregningAksjonspunktResultat opprettSettPåVentAutopunktForVentPåRapportering(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        LocalDate fristDato = beregningsperiodeTjeneste.utledBehandlingPåVentFrist(beregningsgrunnlag);
        return autopunkt(BeregningAksjonspunktDefinisjon.AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST, BeregningVenteårsak.VENT_INNTEKT_RAPPORTERINGSFRIST, fristDato);
    }

    private BeregningAksjonspunktResultat opprettSettPåVentAutopunktMeldekort(LocalDate fristDato) {
        return autopunkt(BeregningAksjonspunktDefinisjon.AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT, BeregningVenteårsak.VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT, fristDato);
    }

    protected BeregningAksjonspunktResultat autopunkt(BeregningAksjonspunktDefinisjon apDef, BeregningVenteårsak venteårsak, LocalDate settPåVentTom) {
        return BeregningAksjonspunktResultat.opprettMedFristFor(apDef, venteårsak, LocalDateTime.of(settPåVentTom, LocalTime.MIDNIGHT));
    }
}
