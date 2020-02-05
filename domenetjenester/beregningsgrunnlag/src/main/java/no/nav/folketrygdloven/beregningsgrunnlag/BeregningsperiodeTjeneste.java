package no.nav.folketrygdloven.beregningsgrunnlag;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class BeregningsperiodeTjeneste {

    private int inntektRapporteringFristDag;

    BeregningsperiodeTjeneste() {
        // for CDI-proxy
    }

    @Inject
    public BeregningsperiodeTjeneste(@KonfigVerdi(value = "inntekt.rapportering.frist.dato", defaultVerdi = "5") int inntektRapporteringFristDagIMåneden) {
        this.inntektRapporteringFristDag = inntektRapporteringFristDagIMåneden;
    }

    public static DatoIntervallEntitet fastsettBeregningsperiodeForATFLAndeler(LocalDate skjæringstidspunkt) {
        LocalDate fom = skjæringstidspunkt.minusMonths(3).withDayOfMonth(1);
        LocalDate tom = skjæringstidspunkt.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    public static DatoIntervallEntitet fastsettBeregningsperiodeForSNAndeler(LocalDate skjæringstidspunkt) {
        LocalDate fom = skjæringstidspunkt.minusYears(3).withDayOfMonth(1);
        LocalDate tom = skjæringstidspunkt.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    boolean skalVentePåInnrapporteringAvInntekt(BeregningsgrunnlagEntitet beregningsgrunnlag, List<Arbeidsgiver> arbeidsgivere, LocalDate dagensDato) {
        Objects.requireNonNull(beregningsgrunnlag, "beregningsgrunnlag");
        if (!harAktivitetStatuserSomKanSettesPåVent(beregningsgrunnlag)) {
            return false;
        }
        LocalDate beregningsperiodeTom = hentBeregningsperiodeTomForATFL(beregningsgrunnlag);
        LocalDate originalFrist = beregningsperiodeTom.plusDays(inntektRapporteringFristDag);
        LocalDate fristMedHelligdagerInkl = BevegeligeHelligdagerUtil.hentFørsteVirkedagFraOgMed(originalFrist);
        if (dagensDato.isAfter(fristMedHelligdagerInkl)) {
            return false;
        }
        return !harMottattInntektsmeldingForAlleArbeidsforhold(beregningsgrunnlag, arbeidsgivere);
    }

    private boolean harAktivitetStatuserSomKanSettesPåVent(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .map(BeregningsgrunnlagPrStatusOgAndel::getAktivitetStatus)
            .anyMatch(status -> status.erArbeidstaker() || status.erFrilanser());
    }

    LocalDate utledBehandlingPåVentFrist(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        LocalDate beregningsperiodeTom = hentBeregningsperiodeTomForATFL(beregningsgrunnlag);
        LocalDate frist = beregningsperiodeTom.plusDays(inntektRapporteringFristDag);
        return BevegeligeHelligdagerUtil.hentFørsteVirkedagFraOgMed(frist).plusDays(1);
    }

    private boolean harMottattInntektsmeldingForAlleArbeidsforhold(BeregningsgrunnlagEntitet beregningsgrunnlag, List<Arbeidsgiver> arbeidsgivere) {
        boolean erFrilanser = beregningsgrunnlag.getAktivitetStatuser().stream()
            .map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus).anyMatch(AktivitetStatus::erFrilanser);
        if (erFrilanser) {
            return false;
        }
        return alleArbeidsforholdHarInntektsmelding(beregningsgrunnlag, arbeidsgivere);
    }

    private static LocalDate hentBeregningsperiodeTomForATFL(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.getAktivitetStatus().erArbeidstaker() || andel.getAktivitetStatus().erFrilanser())
            .map(BeregningsgrunnlagPrStatusOgAndel::getBeregningsperiodeTom)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Beregningsperiode skal være satt for arbeidstaker- og frilansandeler"));
    }

    private boolean alleArbeidsforholdHarInntektsmelding(BeregningsgrunnlagEntitet beregningsgrunnlag, List<Arbeidsgiver> arbeidsgivere) {
        return hentAlleArbeidsgiverePåGrunnlaget(beregningsgrunnlag)
            .filter(arbeidsgiver -> !OrgNummer.erKunstig(arbeidsgiver.getOrgnr())) //Arbeidsforhold er ikke lagt til av saksbehandler
            .allMatch(arbeidsgiver -> arbeidsgivere
                .stream()
                .anyMatch(v -> v.equals(arbeidsgiver)));
    }

    private static Stream<Arbeidsgiver> hentAlleArbeidsgiverePåGrunnlaget(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .map(BeregningsgrunnlagPrStatusOgAndel::getBgAndelArbeidsforhold)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(BGAndelArbeidsforhold::getArbeidsgiver);
    }


}
