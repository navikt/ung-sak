package no.nav.folketrygdloven.beregningsgrunnlag.testutilities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Gradering;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class BeregningInntektsmeldingTestUtil {

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    BeregningInntektsmeldingTestUtil() {
        // for CDI
    }

    @Inject
    public BeregningInntektsmeldingTestUtil(InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    public Inntektsmelding opprettInntektsmelding(BehandlingReferanse behandlingReferanse, String orgNummer, LocalDate skjæringstidspunkt, BigDecimal refusjonskrav,
                                                  BigDecimal inntekt, LocalDateTime innsendingstidspunkt) {
        return opprettInntektsmelding(behandlingReferanse, orgNummer, null, skjæringstidspunkt, Collections.emptyList(), refusjonskrav, inntekt, Tid.TIDENES_ENDE,
            Collections.emptyList(), Collections.emptyList(), innsendingstidspunkt);
    }

    public Inntektsmelding opprettInntektsmelding(BehandlingReferanse behandlingReferanse, String orgNummer, LocalDate skjæringstidspunkt, BigDecimal refusjonskrav,
                                                  BigDecimal inntekt, LocalDate refusjonOpphørerFom, LocalDateTime innsendingstidspunkt) {
        return opprettInntektsmelding(behandlingReferanse, orgNummer, null, skjæringstidspunkt, Collections.emptyList(), refusjonskrav, inntekt, refusjonOpphørerFom,
            Collections.emptyList(), Collections.emptyList(), innsendingstidspunkt);
    }

    public Inntektsmelding opprettInntektsmelding(BehandlingReferanse behandlingReferanse, String orgnr, InternArbeidsforholdRef arbId, LocalDate skjæringstidspunktOpptjening,
                                                  Integer refusjon, LocalDateTime innsendingstidspunkt) {
        return opprettInntektsmelding(behandlingReferanse, orgnr, arbId, skjæringstidspunktOpptjening, Collections.emptyList(), refusjon, 10, innsendingstidspunkt);
    }

    public Inntektsmelding opprettInntektsmelding(BehandlingReferanse behandlingReferanse, String orgnr, InternArbeidsforholdRef arbId, LocalDate skjæringstidspunktOpptjening,
                                                  LocalDateTime innsendingstidspunkt) {
        return opprettInntektsmelding(behandlingReferanse, orgnr, arbId, skjæringstidspunktOpptjening, Collections.emptyList(), 0, 10, innsendingstidspunkt);
    }

    public Inntektsmelding opprettInntektsmelding(BehandlingReferanse behandlingReferanse, String orgnr, InternArbeidsforholdRef arbId, LocalDate skjæringstidspunktOpptjening,
                                                  List<Gradering> graderinger, LocalDateTime innsendingstidspunkt) {
        return opprettInntektsmelding(behandlingReferanse, orgnr, arbId, skjæringstidspunktOpptjening, graderinger, null, 10, innsendingstidspunkt);
    }

    public Inntektsmelding opprettInntektsmelding(BehandlingReferanse behandlingReferanse, String orgnr, InternArbeidsforholdRef arbId, LocalDate skjæringstidspunktOpptjening,
                                                  List<Gradering> graderinger, Integer refusjon, LocalDateTime innsendingstidspunkt) { // NOSONAR - brukes bare
                                                                                                                                       // til test
        BigDecimal refusjonEllerNull = refusjon != null ? BigDecimal.valueOf(refusjon) : null;
        return opprettInntektsmelding(behandlingReferanse, orgnr, arbId, skjæringstidspunktOpptjening, graderinger, refusjonEllerNull, BigDecimal.TEN, Tid.TIDENES_ENDE,
            Collections.emptyList(), Collections.emptyList(), innsendingstidspunkt);
    }

    public Inntektsmelding opprettInntektsmelding(BehandlingReferanse behandlingReferanse, String orgnr, InternArbeidsforholdRef arbId, LocalDate skjæringstidspunktOpptjening,
                                                  List<Gradering> graderinger, Integer refusjon, Integer inntekt, LocalDateTime innsendingstidspunkt) { // NOSONAR
                                                                                                                                                        // -
                                                                                                                                                        // brukes
                                                                                                                                                        // bare
                                                                                                                                                        // til
                                                                                                                                                        // test
        BigDecimal refusjonEllerNull = refusjon != null ? BigDecimal.valueOf(refusjon) : null;
        return opprettInntektsmelding(behandlingReferanse, orgnr, arbId, skjæringstidspunktOpptjening, graderinger, refusjonEllerNull, BigDecimal.valueOf(inntekt),
            Tid.TIDENES_ENDE, Collections.emptyList(), Collections.emptyList(), innsendingstidspunkt);
    }

    public Inntektsmelding opprettInntektsmelding(BehandlingReferanse behandlingReferanse, String orgnr, InternArbeidsforholdRef arbId, LocalDate skjæringstidspunktOpptjening,
                                                  List<Gradering> graderinger, // NOSONAR - brukes bare til test
                                                  Integer refusjon, LocalDate opphørsdatoRefusjon, LocalDateTime innsendingstidspunkt) { // NOSONAR - brukes
                                                                                                                                         // bare til test
        BigDecimal refusjonEllerNull = refusjon != null ? BigDecimal.valueOf(refusjon) : null;
        return opprettInntektsmelding(behandlingReferanse, orgnr, arbId, skjæringstidspunktOpptjening, graderinger, refusjonEllerNull, BigDecimal.valueOf(7_000),
            opphørsdatoRefusjon, Collections.emptyList(), Collections.emptyList(), innsendingstidspunkt);
    }

    public Inntektsmelding opprettInntektsmeldingMedNaturalYtelser(BehandlingReferanse behandlingReferanse, // NOSONAR - brukes bare til test
                                                                   String orgnr,
                                                                   LocalDate skjæringstidspunkt,
                                                                   BigDecimal inntektBeløp,
                                                                   BigDecimal refusjonskrav,
                                                                   LocalDate refusjonOpphørerDato,
                                                                   LocalDateTime innsendingstidspunkt,
                                                                   NaturalYtelse... naturalYtelser) {
        return opprettInntektsmelding(behandlingReferanse, orgnr, null, skjæringstidspunkt, Collections.emptyList(), refusjonskrav, inntektBeløp, refusjonOpphørerDato,
            Arrays.asList(naturalYtelser), Collections.emptyList(), innsendingstidspunkt);
    }

    public Inntektsmelding opprettInntektsmeldingMedEndringerIRefusjon(BehandlingReferanse behandlingReferanse, String orgnr, InternArbeidsforholdRef arbId,
                                                                       LocalDate skjæringstidspunkt, BigDecimal inntektBeløp, // NOSONAR - brukes bare til test
                                                                       BigDecimal refusjonskrav, LocalDate refusjonOpphørerDato, List<Refusjon> endringRefusjon,
                                                                       LocalDateTime innsendingstidspunkt) {
        return opprettInntektsmelding(behandlingReferanse, orgnr, arbId, skjæringstidspunkt, Collections.emptyList(), refusjonskrav, inntektBeløp, refusjonOpphørerDato,
            Collections.emptyList(), endringRefusjon, innsendingstidspunkt);
    }

    private Inntektsmelding opprettInntektsmelding(BehandlingReferanse behandlingReferanse, String orgnr, InternArbeidsforholdRef internReferanse,
                                                   LocalDate skjæringstidspunktOpptjening, List<Gradering> graderinger, // NOSONAR - brukes bare til test
                                                   BigDecimal refusjon, BigDecimal inntekt, LocalDate opphørsdatoRefusjon, List<NaturalYtelse> naturalYtelser,
                                                   List<Refusjon> endringRefusjon, LocalDateTime innsendingstidspunkt) {

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
        return opprettInntektsmelding(behandlingReferanse, arbeidsgiver, internReferanse, skjæringstidspunktOpptjening, graderinger, refusjon, inntekt,
            opphørsdatoRefusjon, naturalYtelser, endringRefusjon, innsendingstidspunkt);
    }

    public Inntektsmelding opprettInntektsmelding(BehandlingReferanse behandlingReferanse, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internReferanse,
                                                  LocalDate skjæringstidspunktOpptjening, List<Gradering> graderinger, // NOSONAR - brukes bare til test
                                                  BigDecimal refusjon, BigDecimal inntekt,
                                                  LocalDate opphørsdatoRefusjon,
                                                  List<NaturalYtelse> naturalYtelser,
                                                  List<Refusjon> endringRefusjon,
                                                  LocalDateTime innsendingstidspunkt) {

        InntektsmeldingBuilder inntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        inntektsmeldingBuilder.medStartDatoPermisjon(skjæringstidspunktOpptjening);
        inntektsmeldingBuilder.medBeløp(inntekt);
        inntektsmeldingBuilder.medInnsendingstidspunkt(innsendingstidspunkt);
        if (refusjon != null) {
            inntektsmeldingBuilder.medRefusjon(refusjon, opphørsdatoRefusjon);
        }
        inntektsmeldingBuilder.medArbeidsgiver(arbeidsgiver);
        inntektsmeldingBuilder.medArbeidsforholdId(internReferanse);
        naturalYtelser.forEach(inntektsmeldingBuilder::leggTil);
        graderinger.forEach(inntektsmeldingBuilder::leggTil);
        endringRefusjon.forEach(inntektsmeldingBuilder::leggTil);

        // FIXME: (FC) Skal ikke trenge å kalle denne her, men må sørge for at AksjonspunktUtledere får inntektsmeldinger som input i stedet for å slå opp først.
        inntektsmeldingTjeneste.lagreInntektsmelding(behandlingReferanse.getSaksnummer(), behandlingReferanse.getId(), inntektsmeldingBuilder);

        return inntektsmeldingBuilder.build(); // gir samme resultat for hvert kall til build.

    }
}
