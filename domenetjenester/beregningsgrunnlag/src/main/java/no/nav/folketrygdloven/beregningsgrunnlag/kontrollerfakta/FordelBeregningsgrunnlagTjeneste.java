package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.BeregningInntektsmeldingTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering.Gradering;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.tid.ÅpenDatoIntervallEntitet;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

@ApplicationScoped
public class FordelBeregningsgrunnlagTjeneste {

    public enum VurderManuellBehandling {
        NYTT_ARBEIDSFORHOLD,
        FL_ELLER_SN_TILKOMMER,
        TOTALT_REFUSJONSKRAV_STØRRE_ENN_6G,
        REFUSJON_STØRRE_ENN_OPPGITT_INNTEKT_OG_HAR_AAP,
        GRADERT_ANDEL_SOM_VILLE_HA_BLITT_AVKORTET_TIL_0,
        FORESLÅTT_BG_PÅ_GRADERT_ANDEL_ER_0
    }

    @Inject
    public FordelBeregningsgrunnlagTjeneste() {
    }

    public static List<Gradering> hentGraderingerForAndelIPeriode(BeregningsgrunnlagPrStatusOgAndel andel, AktivitetGradering aktivitetGradering) {
        Optional<AndelGradering> graderingOpt = BeregningInntektsmeldingTjeneste.finnGraderingForAndel(andel, aktivitetGradering);
        if (graderingOpt.isPresent()) {
            AndelGradering andelGradering = graderingOpt.get();
            ÅpenDatoIntervallEntitet beregningsgrunnlagPeriode = andel.getBeregningsgrunnlagPeriode().getPeriode();
            return andelGradering.getGraderinger().stream()
                .filter(gradering -> gradering.getPeriode().overlapper(beregningsgrunnlagPeriode))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public static List<Gradering> hentGraderingerForAndel(BeregningsgrunnlagPrStatusOgAndel andel, AktivitetGradering aktivitetGradering) {
        Optional<AndelGradering> graderingOpt = BeregningInntektsmeldingTjeneste.finnGraderingForAndel(andel, aktivitetGradering);
        if (graderingOpt.isPresent()) {
            AndelGradering gradering = graderingOpt.get();
            return gradering.getGraderinger();
        }
        return Collections.emptyList();
    }

    private static List<BeregningsgrunnlagPeriode> finnPerioderMedGradering(BeregningsgrunnlagEntitet beregningsgrunnlag, AktivitetGradering aktivitetGradering) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .filter(periode -> harPeriodeGradering(periode, aktivitetGradering))
            .collect(Collectors.toList());
    }

    private static boolean harPeriodeGradering(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode, AktivitetGradering aktivitetGradering) {
        boolean harPeriodeGradering = aktivitetGradering.getAndelGradering().stream()
            .flatMap(gradering -> gradering.getGraderinger().stream())
            .anyMatch(gradering -> finnesOverlapp(beregningsgrunnlagPeriode, gradering.getPeriode()));
        return harPeriodeGradering;
    }

    private static boolean finnesOverlapp(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode, DatoIntervallEntitet intervall) {
        return beregningsgrunnlagPeriode.getPeriode().overlapper(intervall);
    }

    private static List<BeregningsgrunnlagPeriode> finnPerioderMedRefusjonskrav(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .filter(FordelBeregningsgrunnlagTjeneste::harPeriodeRefusjonskrav)
            .collect(Collectors.toList());
    }

    private static boolean harPeriodeRefusjonskrav(BeregningsgrunnlagPeriode periode) {
        return periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .anyMatch(andel -> andel.getBgAndelArbeidsforhold()
                .map(BGAndelArbeidsforhold::getRefusjonskravPrÅr)
                .filter(refusjon -> refusjon.compareTo(BigDecimal.ZERO) > 0)
                .isPresent());
    }

    /** Dersom returnerer {@link #vurderManuellBehandling(BehandlingReferanse, BeregningsgrunnlagEntitet, BeregningAktivitetAggregatEntitet)} så bør manuell behandling .... */
    public Optional<VurderManuellBehandling> vurderManuellBehandling(BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                                     BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                                                     AktivitetGradering aktivitetGradering,
                                                                     Collection<Inntektsmelding> inntektsmeldinger) {
        List<BeregningsgrunnlagPeriode> gradertePerioder = finnPerioderMedGradering(beregningsgrunnlag, aktivitetGradering);
        List<BeregningsgrunnlagPeriode> periodeMedRefusjonskrav = finnPerioderMedRefusjonskrav(beregningsgrunnlag);
        if (gradertePerioder.isEmpty() && periodeMedRefusjonskrav.isEmpty()) {
            return Optional.empty();
        }
        return vurderManuellBehandlingForGraderingEllerEndretRefusjon(beregningsgrunnlag, beregningAktivitetAggregat, aktivitetGradering, inntektsmeldinger);
    }

    private Optional<VurderManuellBehandling> vurderManuellBehandlingForGraderingEllerEndretRefusjon(BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                                                                     BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                                                                                     AktivitetGradering aktivitetGradering,
                                                                                                     Collection<Inntektsmelding> inntektsmeldinger) {
        for (BeregningsgrunnlagPeriode periode : beregningsgrunnlag.getBeregningsgrunnlagPerioder()) {
            Optional<VurderManuellBehandling> vurderManuell = vurderManuellBehandlingForPeriode(periode, beregningAktivitetAggregat,
                aktivitetGradering, inntektsmeldinger);
            if (vurderManuell.isPresent())
                return vurderManuell;
        }
        return Optional.empty();
    }

    private Optional<VurderManuellBehandling> vurderManuellBehandlingForPeriode(BeregningsgrunnlagPeriode periode,
                                                                                BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                                                                AktivitetGradering aktivitetGradering,
                                                                                Collection<Inntektsmelding> inntektsmeldinger) {
        boolean erTotaltRefusjonskravStørreEnnSeksG = BeregningInntektsmeldingTjeneste.erTotaltRefusjonskravStørreEnnEllerLikSeksG(periode, inntektsmeldinger);
        boolean harNoenAndelerMedAap = harNoenAndelerMedAAP(periode);

        for (BeregningsgrunnlagPrStatusOgAndel andel : periode.getBeregningsgrunnlagPrStatusOgAndelList()) {
            Optional<VurderManuellBehandling> vurderManuell = vurderManuellBehandlingForAndel(andel, aktivitetGradering, beregningAktivitetAggregat,
                erTotaltRefusjonskravStørreEnnSeksG,
                harNoenAndelerMedAap,
                inntektsmeldinger,
                periode.getPeriode());
            if (vurderManuell.isPresent()) {
                return vurderManuell;
            }
        }
        return Optional.empty();
    }

    /**
     * @deprecated TODO : refactor kode som kaller på denne metoden slik at det blir tydeligere hva som brukes her.
     */
    @Deprecated(forRemoval=true)
    public Optional<VurderManuellBehandling> vurderManuellBehandlingForAndel(BeregningsgrunnlagPeriode periode,
                                                                             BeregningsgrunnlagPrStatusOgAndel andel,
                                                                             AktivitetGradering aktivitetGradering,
                                                                             BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                                                             Collection<Inntektsmelding> inntektsmeldinger) {
        boolean erTotaltRefusjonskravStørreEnnSeksG = BeregningInntektsmeldingTjeneste.erTotaltRefusjonskravStørreEnnEllerLikSeksG(periode, inntektsmeldinger);
        boolean harNoenAndelerMedAap = harNoenAndelerMedAAP(periode);
        return vurderManuellBehandlingForAndel(andel, aktivitetGradering, beregningAktivitetAggregat,
            erTotaltRefusjonskravStørreEnnSeksG,
            harNoenAndelerMedAap,
            inntektsmeldinger,
            periode.getPeriode());
    }

    private Optional<VurderManuellBehandling> vurderManuellBehandlingForAndel(BeregningsgrunnlagPrStatusOgAndel andel,
                                                                              AktivitetGradering aktivitetGradering,
                                                                              BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                                                              boolean harTotaltRefusjonskravStørreEnn6G,
                                                                              boolean harNoenAndelerMedAap, Collection<Inntektsmelding> inntektsmeldinger, ÅpenDatoIntervallEntitet periode) {
        boolean harGraderingIBGPeriode = !hentGraderingerForAndelIPeriode(andel, aktivitetGradering).isEmpty();
        BigDecimal refusjonskravPrÅr = BeregningInntektsmeldingTjeneste.finnRefusjonskravPrÅrIPeriodeForAndel(andel, periode, inntektsmeldinger).orElse(BigDecimal.ZERO);

        boolean harRefusjonIPerioden = refusjonskravPrÅr.compareTo(BigDecimal.ZERO) != 0;

        if (!harGraderingIBGPeriode && !harRefusjonIPerioden) {
            return Optional.empty();
        }

        if (erNyFLSNAndel(andel, beregningAktivitetAggregat)) {
            return Optional.of(VurderManuellBehandling.FL_ELLER_SN_TILKOMMER);
        }

        boolean erNytt = erNyttArbeidsforhold(andel, beregningAktivitetAggregat);
        if (erNytt) {
            return Optional.of(VurderManuellBehandling.NYTT_ARBEIDSFORHOLD);
        }

        if (skalGraderePåAndelUtenBeregningsgrunnlag(andel, harGraderingIBGPeriode)) {
            return Optional.of(VurderManuellBehandling.FORESLÅTT_BG_PÅ_GRADERT_ANDEL_ER_0);
        }

        if (harGraderingUtenRefusjon(harGraderingIBGPeriode, harRefusjonIPerioden)) {
            if (harTotaltRefusjonskravStørreEnn6G) {
                return Optional.of(VurderManuellBehandling.TOTALT_REFUSJONSKRAV_STØRRE_ENN_6G);
            }
            if (gradertAndelVilleBlittAvkortet(andel)) {
                return Optional.of(VurderManuellBehandling.GRADERT_ANDEL_SOM_VILLE_HA_BLITT_AVKORTET_TIL_0);
            }
        }

        if (harAndelerMedAAPOgRefusjonOverstigerInntekt(andel, harNoenAndelerMedAap)) {
            return Optional.of(VurderManuellBehandling.REFUSJON_STØRRE_ENN_OPPGITT_INNTEKT_OG_HAR_AAP);
        }
        return Optional.empty();
    }

    private boolean harAndelerMedAAPOgRefusjonOverstigerInntekt(BeregningsgrunnlagPrStatusOgAndel andel, boolean harNoenAndelerMedAap) {
        BigDecimal refusjonskravPrÅr = andel.getBgAndelArbeidsforhold()
            .map(BGAndelArbeidsforhold::getRefusjonskravPrÅr)
            .orElse(BigDecimal.ZERO);
        return harNoenAndelerMedAap && harHøyereRefusjonEnnInntekt(refusjonskravPrÅr, andel);
    }

    private boolean harGraderingUtenRefusjon(boolean harGraderingIBGPeriode, boolean harRefusjonIPerioden) {
        return harGraderingIBGPeriode && !harRefusjonIPerioden;
    }

    private boolean skalGraderePåAndelUtenBeregningsgrunnlag(BeregningsgrunnlagPrStatusOgAndel andel, boolean harGraderingIBGPeriode) {
        boolean harIkkjeBeregningsgrunnlag = andel.getBruttoInkludertNaturalYtelser().compareTo(BigDecimal.ZERO) == 0;
        return harGraderingIBGPeriode && harIkkjeBeregningsgrunnlag;
    }

    private boolean gradertAndelVilleBlittAvkortet(BeregningsgrunnlagPrStatusOgAndel andel) {
        if (erStatusSomAvkortesVedATOver6G(andel)) {
            BigDecimal totaltBgFraArbeidstaker = andel.getBeregningsgrunnlagPeriode().getBeregningsgrunnlagPrStatusOgAndelList()
                .stream()
                .filter(a -> a.getAktivitetStatus().erArbeidstaker())
                .map(BeregningsgrunnlagPrStatusOgAndel::getBruttoInkludertNaturalYtelser)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
            BigDecimal seksG = andel.getBeregningsgrunnlagPeriode().getBeregningsgrunnlag().getGrunnbeløp().getVerdi().multiply(BigDecimal.valueOf(6));
            return totaltBgFraArbeidstaker.compareTo(seksG) > 0;
        }
        return false;
    }

    private boolean erStatusSomAvkortesVedATOver6G(BeregningsgrunnlagPrStatusOgAndel andel) {
        AktivitetStatus aktivitetStatus = andel.getAktivitetStatus();
        return !aktivitetStatus.erArbeidstaker();
    }

    private boolean erNyFLSNAndel(BeregningsgrunnlagPrStatusOgAndel andel, BeregningAktivitetAggregatEntitet beregningAktivitetAggregat) {
        LocalDate skjæringstidspunkt = andel.getBeregningsgrunnlagPeriode().getBeregningsgrunnlag().getSkjæringstidspunkt();
        if (andel.getAktivitetStatus().erFrilanser()) {
            return erNyAndelMedType(beregningAktivitetAggregat, skjæringstidspunkt, OpptjeningAktivitetType.FRILANS);
        }
        if (andel.getAktivitetStatus().erSelvstendigNæringsdrivende()) {
            return erNyAndelMedType(beregningAktivitetAggregat, skjæringstidspunkt, OpptjeningAktivitetType.NÆRING);
        }
        return false;
    }

    private boolean erNyAndelMedType(BeregningAktivitetAggregatEntitet beregningAktivitetAggregat, LocalDate skjæringstidspunkt, OpptjeningAktivitetType opptjeningAktivitetType) {
        return beregningAktivitetAggregat.getBeregningAktiviteter().stream()
            .filter(beregningAktivitet -> !beregningAktivitet.getPeriode().getTomDato().isBefore(skjæringstidspunkt.minusDays(1)))
            .noneMatch(
                beregningAktivitet -> opptjeningAktivitetType.equals(beregningAktivitet.getOpptjeningAktivitetType()));
    }

    private boolean harHøyereRefusjonEnnInntekt(BigDecimal refusjonskravPrÅr, BeregningsgrunnlagPrStatusOgAndel andel) {
            return harHøyereRefusjonEnnBeregningsgrunnlag(refusjonskravPrÅr, andel.getBruttoPrÅr());
    }

    private boolean harHøyereRefusjonEnnBeregningsgrunnlag(BigDecimal refusjonskravPrÅr, BigDecimal bruttoPrÅr) {
            return refusjonskravPrÅr.compareTo(bruttoPrÅr) > 0;
    }

    private boolean harNoenAndelerMedAAP(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        return beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .anyMatch(a -> AktivitetStatus.ARBEIDSAVKLARINGSPENGER.equals(a.getAktivitetStatus()));
    }

    public boolean erNyttArbeidsforhold(BeregningsgrunnlagPrStatusOgAndel andel, BeregningAktivitetAggregatEntitet beregningAktivitetAggregat) {
        if (!andel.getBgAndelArbeidsforhold().isPresent()) {
            return false;
        }
        LocalDate skjæringstidspunkt = andel.getBeregningsgrunnlagPeriode().getBeregningsgrunnlag().getSkjæringstidspunkt();
        BGAndelArbeidsforhold arbeidsforhold = andel.getBgAndelArbeidsforhold().get();
        var beregningAktiviteter = beregningAktivitetAggregat.getBeregningAktiviteter();
        return beregningAktiviteter.stream()
            .filter(beregningAktivitet -> !beregningAktivitet.getPeriode().getTomDato().isBefore(skjæringstidspunkt.minusDays(1)))
            .filter(beregningAktivitet -> !beregningAktivitet.getPeriode().getFomDato().isAfter(skjæringstidspunkt.minusDays(1)))
            .noneMatch(
                beregningAktivitet -> beregningAktivitet.gjelderFor(arbeidsforhold.getArbeidsgiver(), arbeidsforhold.getArbeidsforholdRef()));
    }

}
