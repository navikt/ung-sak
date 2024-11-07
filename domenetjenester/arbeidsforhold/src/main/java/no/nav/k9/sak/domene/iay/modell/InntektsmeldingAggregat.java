package no.nav.k9.sak.domene.iay.modell;

import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.IKKE_BRUK;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

public class InntektsmeldingAggregat {

    private static final Logger logger = LoggerFactory.getLogger(InntektsmeldingAggregat.class);

    @ChangeTracked
    private final Set<Inntektsmelding> inntektsmeldinger = new LinkedHashSet<>();

    private ArbeidsforholdInformasjon arbeidsforholdInformasjon;

    InntektsmeldingAggregat() {
    }

    InntektsmeldingAggregat(InntektsmeldingAggregat inntektsmeldingAggregat) {
        this(inntektsmeldingAggregat.getAlleInntektsmeldinger());
    }

    public InntektsmeldingAggregat(Collection<Inntektsmelding> inntektsmeldinger) {
        this.inntektsmeldinger.addAll(inntektsmeldinger.stream().map(Inntektsmelding::new).toList());
    }

    /**
     * Alle gjeldende inntektsmeldinger i behandlingen (de som skal brukes)
     *
     * @return Liste med {@link Inntektsmelding}
     *         <p>
     *         Merk denne filtrerer inntektsmeldinger ifht hva som skal brukes.
     */
    public List<Inntektsmelding> getInntektsmeldingerSomSkalBrukes() {
        return inntektsmeldinger.stream().filter(this::skalBrukes).toList();
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
            || ov.kreverIkkeInntektsmelding());
    }

    /**
     * Den persisterte inntektsmeldingen kan være av nyere dato, bestemmes av
     * innsendingstidspunkt på inntektsmeldingen.
     */
    public void leggTil(Inntektsmelding inntektsmelding) {

        boolean fjernet = inntektsmeldinger.removeIf(it -> skalFjerneInntektsmelding(it, inntektsmelding));

        if (fjernet || inntektsmeldinger.stream().noneMatch(it -> it.gjelderSammeArbeidsforhold(inntektsmelding))) {
            inntektsmeldinger.add(inntektsmelding);
        }

        inntektsmeldinger.stream().filter(it -> it.gjelderSammeArbeidsforhold(inntektsmelding) && !fjernet).findFirst().ifPresent(
            e -> logger.info("Persistert inntektsmelding med journalpostid {} er nyere enn den mottatte med journalpostid {}. Ignoreres", e.getJournalpostId(), inntektsmelding.getJournalpostId()));
    }

    public void fjern(Inntektsmelding inntektsmelding) {
        inntektsmeldinger.remove(inntektsmelding);
    }

    private boolean skalFjerneInntektsmelding(Inntektsmelding gammel, Inntektsmelding ny) {
        return ny.erNyereEnn(gammel);
    }

    void taHensynTilBetraktninger(ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        this.arbeidsforholdInformasjon = arbeidsforholdInformasjon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        return o instanceof InntektsmeldingAggregat that
            && Objects.equals(inntektsmeldinger, that.inntektsmeldinger);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inntektsmeldinger);
    }
}
