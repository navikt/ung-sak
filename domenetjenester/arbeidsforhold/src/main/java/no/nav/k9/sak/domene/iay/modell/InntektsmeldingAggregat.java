package no.nav.k9.sak.domene.iay.modell;

import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.IKKE_BRUK;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.domene.iay.inntektsmelding.InntektsmeldingErNyereVurderer;
import no.nav.k9.sak.typer.Arbeidsgiver;

public class InntektsmeldingAggregat {

    private static final Logger logger = LoggerFactory.getLogger(InntektsmeldingAggregat.class);

    @ChangeTracked
    private List<Inntektsmelding> inntektsmeldinger = new ArrayList<>();

    private ArbeidsforholdInformasjon arbeidsforholdInformasjon;

    InntektsmeldingAggregat() {
    }

    InntektsmeldingAggregat(InntektsmeldingAggregat inntektsmeldingAggregat) {
        this(inntektsmeldingAggregat.getAlleInntektsmeldinger());
    }

    public InntektsmeldingAggregat(Collection<Inntektsmelding> inntektsmeldinger) {
        this.inntektsmeldinger.addAll(inntektsmeldinger.stream().map(i -> {
            final Inntektsmelding inntektsmelding = new Inntektsmelding(i);
            return inntektsmelding;
        }).collect(Collectors.toList()));
    }

    /**
     * Alle gjeldende inntektsmeldinger i behandlingen (de som skal brukes)
     *
     * @return Liste med {@link Inntektsmelding}
     *         <p>
     *         Merk denne filtrerer inntektsmeldinger ifht hva som skal brukes.
     */
    public List<Inntektsmelding> getInntektsmeldingerSomSkalBrukes() {
        return inntektsmeldinger.stream().filter(this::skalBrukes).collect(Collectors.toUnmodifiableList());
    }

    /**
     * Get alle inntetksmeldinger (både de som skal brukes og ikke brukes).
     */
    public List<Inntektsmelding> getAlleInntektsmeldinger() {
        var ims = new ArrayList<>(inntektsmeldinger);
        Collections.sort(ims, Inntektsmelding.COMP_REKKEFØLGE);
        return Collections.unmodifiableList(ims);
    }

    private boolean skalBrukes(Inntektsmelding im) {
        return arbeidsforholdInformasjon == null || arbeidsforholdInformasjon.getOverstyringer()
            .stream()
            .noneMatch(ov -> erFjernet(im, ov));
    }

    private boolean erFjernet(Inntektsmelding im, ArbeidsforholdOverstyring ov) {
        return (ov.getArbeidsforholdRef().equals(im.getArbeidsforholdRef()))
            && ov.getArbeidsgiver().equals(im.getArbeidsgiver())
            && (Objects.equals(IKKE_BRUK, ov.getHandling())
                || Objects.equals(SLÅTT_SAMMEN_MED_ANNET, ov.getHandling())
                || ov.kreverIkkeInntektsmelding());
    }

    /**
     * Alle gjeldende inntektsmeldinger for en virksomhet i behandlingen.
     *
     * @return Liste med {@link Inntektsmelding}
     */
    public List<Inntektsmelding> getInntektsmeldingerFor(Arbeidsgiver arbeidsgiver) {
        return getInntektsmeldingerSomSkalBrukes().stream().filter(i -> i.getArbeidsgiver().equals(arbeidsgiver)).collect(Collectors.toList());
    }

    /**
     * Den persisterte inntektsmeldingen kan være av nyere dato, bestemmes av
     * innsendingstidspunkt på inntektsmeldingen.
     */
    public void leggTil(Inntektsmelding inntektsmelding) {

        boolean fjernet = inntektsmeldinger.removeIf(it -> skalFjerneInntektsmelding(it, inntektsmelding));

        if (fjernet || inntektsmeldinger.stream().noneMatch(it -> it.gjelderSammeArbeidsforhold(inntektsmelding))) {
            final Inntektsmelding entitet = inntektsmelding;
            inntektsmeldinger.add(entitet);
        }

        inntektsmeldinger.stream().filter(it -> it.gjelderSammeArbeidsforhold(inntektsmelding) && !fjernet).findFirst().ifPresent(
            e -> logger.info("Persistert inntektsmelding med journalpostid {} er nyere enn den mottatte med journalpostid {}. Ignoreres", e.getJournalpostId(), inntektsmelding.getJournalpostId()));
    }

    public void fjern(Inntektsmelding inntektsmelding) {
        inntektsmeldinger.remove(inntektsmelding);
    }

    private boolean skalFjerneInntektsmelding(Inntektsmelding gammel, Inntektsmelding ny) {
        return InntektsmeldingErNyereVurderer.erNyere(gammel, ny);
    }

    void taHensynTilBetraktninger(ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        this.arbeidsforholdInformasjon = arbeidsforholdInformasjon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof InntektsmeldingAggregat))
            return false;
        InntektsmeldingAggregat that = (InntektsmeldingAggregat) o;
        return Objects.equals(inntektsmeldinger, that.inntektsmeldinger);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inntektsmeldinger);
    }
}
