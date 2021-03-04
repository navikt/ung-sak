package no.nav.k9.sak.domene.iay.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.arbeidsforhold.InntektsmeldingInnsendingsårsak;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.vedtak.konfig.Tid;

public class InntektsmeldingBuilder {

    public static final Comparator<? super InntektsmeldingBuilder> COMP_REKKEFØLGE = Comparator.comparing(InntektsmeldingBuilder::getKanalreferanse, Comparator.nullsLast(Comparator.naturalOrder()));
    private static final Logger log = LoggerFactory.getLogger(InntektsmeldingBuilder.class);
    private final Inntektsmelding kladd;
    private EksternArbeidsforholdRef eksternArbeidsforholdId;
    private boolean erBygget;

    InntektsmeldingBuilder(Inntektsmelding kladd) {
        this.kladd = kladd;
    }

    public static InntektsmeldingBuilder builder() {
        return new InntektsmeldingBuilder(new Inntektsmelding());
    }

    public static InntektsmeldingBuilder kopi(Inntektsmelding inntektsmelding) {
        return new InntektsmeldingBuilder(new Inntektsmelding(inntektsmelding));
    }

    public Inntektsmelding build() {
        return build(false);
    }

    public Inntektsmelding build(boolean ignore) { // NOSONAR
        var internRef = getInternArbeidsforholdRef();
        Objects.requireNonNull(kladd.getKanalreferanse(), "kanalreferanse er ikke satt");
        if (internRef.isPresent() && !ignore) {
            // magic - hvis har ekstern referanse må også intern referanse være spesifikk
            if ((eksternArbeidsforholdId != null && eksternArbeidsforholdId.gjelderForSpesifiktArbeidsforhold()) && internRef.get().getReferanse() == null) {
                throw new IllegalArgumentException(
                    "Begge referanser må gjelde spesifikke arbeidsforhold. " + " Ekstern: " + eksternArbeidsforholdId + ", Intern: " + internRef);
            }
        }

        new ValiderInntektsmelding().valider(this);

        erBygget = true; // Kan ikke bygge mer med samme builder, vil bare returnere samme kladd.
        return kladd;
    }

    Inntektsmelding getKladd() {
        precondition();
        return kladd;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return kladd.getArbeidsgiver();
    }

    public LocalDateTime getInnsendingstidspunkt() {
        return kladd.getInnsendingstidspunkt();
    }

    public String getKildesystem() {
        return kladd.getKildesystem();
    }

    public String getKanalreferanse() {
        return kladd.getKanalreferanse();
    }

    public Optional<EksternArbeidsforholdRef> getEksternArbeidsforholdRef() {
        return Optional.ofNullable(eksternArbeidsforholdId);
    }

    public Optional<InternArbeidsforholdRef> getInternArbeidsforholdRef() {
        return Optional.ofNullable(kladd.getArbeidsforholdRef());
    }

    public InntektsmeldingBuilder leggTil(Gradering gradering) {
        precondition();
        kladd.leggTil(gradering);
        return this;
    }

    public InntektsmeldingBuilder leggTil(NaturalYtelse naturalYtelse) {
        precondition();
        kladd.leggTil(naturalYtelse);
        return this;
    }

    public InntektsmeldingBuilder leggTilFravær(PeriodeAndel fravær) {
        precondition();
        kladd.leggTilFravær(fravær);
        return this;
    }

    public InntektsmeldingBuilder leggTil(Refusjon refusjon) {
        precondition();
        kladd.leggTil(refusjon);
        return this;
    }

    public InntektsmeldingBuilder leggTil(UtsettelsePeriode utsettelsePeriode) {
        precondition();
        kladd.leggTil(utsettelsePeriode);
        return this;
    }

    public InntektsmeldingBuilder medArbeidsforholdId(EksternArbeidsforholdRef arbeidsforholdId) {
        precondition();
        this.eksternArbeidsforholdId = arbeidsforholdId;
        kladd.setEksternArbeidsforholdRef(arbeidsforholdId);
        return this;
    }

    public InntektsmeldingBuilder medArbeidsforholdId(InternArbeidsforholdRef arbeidsforholdId) {
        precondition();
        if (arbeidsforholdId != null) {
            // magic - hvis har ekstern referanse må også intern referanse være spesifikk
            if (arbeidsforholdId.getReferanse() == null && eksternArbeidsforholdId != null && eksternArbeidsforholdId.gjelderForSpesifiktArbeidsforhold()) {
                throw new IllegalArgumentException(
                    "Begge referanser gjelde spesifikke arbeidsforhold. " + " Ekstern: " + eksternArbeidsforholdId + ", Intern: " + arbeidsforholdId);
            }
            kladd.setArbeidsforholdId(arbeidsforholdId);
        }
        return this;
    }

    /**
     * @deprecated bruk eksplisitt Intern/Ekstern arbeidforhold Id.
     */
    @Deprecated(forRemoval = true)
    public InntektsmeldingBuilder medArbeidsforholdId(String arbeidsforholdId) {
        precondition();
        return medArbeidsforholdId(arbeidsforholdId == null ? null : InternArbeidsforholdRef.ref(arbeidsforholdId));
    }

    public InntektsmeldingBuilder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        precondition();
        kladd.setArbeidsgiver(arbeidsgiver);
        return this;
    }

    public InntektsmeldingBuilder medBeløp(BigDecimal verdi) {
        precondition();
        kladd.setInntektBeløp(verdi == null ? null : new Beløp(verdi));
        return this;
    }

    public InntektsmeldingBuilder medInnsendingstidspunkt(LocalDateTime innsendingstidspunkt) {
        precondition();
        Objects.requireNonNull(innsendingstidspunkt, "innsendingstidspunkt");
        kladd.setInnsendingstidspunkt(innsendingstidspunkt);
        return this;
    }

    public InntektsmeldingBuilder medInntektsmeldingaarsak(InntektsmeldingInnsendingsårsak inntektsmeldingInnsendingsårsak) {
        precondition();
        kladd.setInntektsmeldingInnsendingsårsak(inntektsmeldingInnsendingsårsak);
        return this;
    }

    public InntektsmeldingBuilder medOppgittFravær(List<PeriodeAndel> fravær) {
        precondition();
        kladd.setOmsorgspengerFravær(fravær);
        return this;
    }

    public InntektsmeldingBuilder medInntektsmeldingaarsak(String inntektsmeldingInnsendingsårsak) {
        precondition();
        return medInntektsmeldingaarsak(inntektsmeldingInnsendingsårsak == null ? null : InntektsmeldingInnsendingsårsak.fraKode(inntektsmeldingInnsendingsårsak));
    }

    public InntektsmeldingBuilder medJournalpostId(JournalpostId id) {
        precondition();
        kladd.setJournalpostId(id);
        return this;
    }

    public InntektsmeldingBuilder medJournalpostId(String id) {
        precondition();
        return medJournalpostId(new JournalpostId(id));
    }

    public InntektsmeldingBuilder medKanalreferanse(String kanalreferanse) {
        precondition();
        kladd.setKanalreferanse(kanalreferanse);
        return this;
    }

    public InntektsmeldingBuilder medKildesystem(String kildesystem) {
        precondition();
        kladd.setKildesystem(kildesystem);
        return this;
    }

    public InntektsmeldingBuilder medMottattDato(LocalDate mottattDato) {
        precondition();
        kladd.setMottattDato(Objects.requireNonNull(mottattDato, "mottattDato"));
        return this;
    }

    public InntektsmeldingBuilder medNærRelasjon(boolean nærRelasjon) {
        precondition();
        kladd.setNærRelasjon(nærRelasjon);
        return this;
    }

    public InntektsmeldingBuilder medRefusjon(BigDecimal verdi) {
        precondition();
        kladd.setRefusjonBeløpPerMnd(verdi == null ? null : new Beløp(verdi));
        kladd.setRefusjonOpphører(Tid.TIDENES_ENDE);
        return this;
    }

    public InntektsmeldingBuilder medRefusjon(BigDecimal verdi, LocalDate opphører) {
        precondition();
        kladd.setRefusjonBeløpPerMnd(verdi == null ? null : new Beløp(verdi));
        kladd.setRefusjonOpphører(opphører);
        return this;
    }

    public InntektsmeldingBuilder medStartDatoPermisjon(LocalDate startPermisjon) {
        precondition();
        kladd.setStartDatoPermisjon(startPermisjon);
        return this;
    }

    public InntektsmeldingBuilder medYtelse(FagsakYtelseType ytelse) {
        precondition();
        kladd.setFagsakYtelseType(ytelse);
        return this;
    }

    private void precondition() {
        if (erBygget) {
            throw new IllegalStateException("Inntektsmelding objekt er allerede bygget, kan ikke modifisere nå. Returnerer kun : " + kladd);
        }
    }

}
