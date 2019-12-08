package no.nav.folketrygdloven.beregningsgrunnlag.input;

import static java.util.Collections.emptyList;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.AktørId;

class InntektsmeldingFilter {

    private InntektArbeidYtelseGrunnlag iayGrunnlag;

    InntektsmeldingFilter(InntektArbeidYtelseGrunnlag iayGrunnlag) {
        this.iayGrunnlag = iayGrunnlag;
    }

    /**
     * Henter alle inntektsmeldinger for beregning
     * Tar hensyn til inaktive arbeidsforhold, dvs. fjerner de
     * inntektsmeldingene som er koblet til inaktive arbeidsforhold.
     * Spesial håndtering i forbindelse med beregning.
     *
     * @param ref {@link BehandlingReferanse}
     * @param skjæringstidspunktForOpptjening datoen arbeidsforhold må inkludere eller starte etter for å bli regnet som aktive
     * @return Liste med inntektsmeldinger {@link Inntektsmelding}
     */
    List<Inntektsmelding> hentInntektsmeldingerBeregning(BehandlingReferanse ref, LocalDate skjæringstidspunktForOpptjening) {
        AktørId aktørId = ref.getAktørId();
        LocalDate skjæringstidspunktMinusEnDag = skjæringstidspunktForOpptjening.minusDays(1);
        List<Inntektsmelding> inntektsmeldinger = iayGrunnlag.getInntektsmeldinger().map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .orElse(emptyList());

        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(aktørId));
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getYrkesaktiviteter();

        // kan ikke filtrere når det ikke finnes yrkesaktiviteter
        if (yrkesaktiviteter.isEmpty()) {
            return inntektsmeldinger;
        }
        return filtrerVekkInntektsmeldingPåInaktiveArbeidsforhold(filter, yrkesaktiviteter, inntektsmeldinger, skjæringstidspunktMinusEnDag);
    }

    /**
     * Filtrer vekk inntektsmeldinger som er knyttet til et arbeidsforhold som har en tom dato som slutter før STP.
     */
    private List<Inntektsmelding> filtrerVekkInntektsmeldingPåInaktiveArbeidsforhold(YrkesaktivitetFilter filter, Collection<Yrkesaktivitet> yrkesaktiviteter,
                                                                                     Collection<Inntektsmelding> inntektsmeldinger,
                                                                                     LocalDate skjæringstidspunktet) {
        List<Inntektsmelding> resultat = new ArrayList<>();

        inntektsmeldinger.forEach(im -> {
            boolean skalLeggeTil = yrkesaktiviteter.stream()
                .anyMatch(y -> {
                    boolean gjelderFor = y.gjelderFor(im.getArbeidsgiver(), im.getArbeidsforholdRef());
                    var ansettelsesPerioder = filter.getAnsettelsesPerioder(y);
                    return gjelderFor && ansettelsesPerioder.stream()
                        .anyMatch(ap -> ap.getPeriode().inkluderer(skjæringstidspunktet) || ap.getPeriode().getFomDato().isAfter(skjæringstidspunktet));
                });
            if (skalLeggeTil) {
                resultat.add(im);
            }
        });
        return Collections.unmodifiableList(resultat);
    }
}
