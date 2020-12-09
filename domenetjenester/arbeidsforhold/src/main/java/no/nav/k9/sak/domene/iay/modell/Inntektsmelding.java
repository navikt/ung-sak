package no.nav.k9.sak.domene.iay.modell;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsmeldingInnsendingsårsak;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;

public class Inntektsmelding implements IndexKey {

    public static final Comparator<? super Inntektsmelding> COMP_REKKEFØLGE = Comparator.comparing(Inntektsmelding::getKanalreferanse, Comparator.nullsLast(Comparator.naturalOrder()));

    @ChangeTracked
    private List<Gradering> graderinger = new ArrayList<>();

    @ChangeTracked
    private List<NaturalYtelse> naturalYtelser = new ArrayList<>();

    @ChangeTracked
    private List<UtsettelsePeriode> utsettelsePerioder = new ArrayList<>();

    @ChangeTracked
    private Arbeidsgiver arbeidsgiver;

    @ChangeTracked
    private InternArbeidsforholdRef arbeidsforholdRef;

    @ChangeTracked
    private LocalDate startDatoPermisjon;

    @ChangeTracked
    private List<PeriodeAndel> oppgittFravær = new ArrayList<>();

    private boolean nærRelasjon;

    /**
     * Journalpost referanse (Joark referanse).
     */
    private JournalpostId journalpostId;

    /**
     * Dato inntektsmelding mottatt.
     */
    private LocalDate mottattDato;

    @ChangeTracked
    private Beløp inntektBeløp;

    @ChangeTracked
    private Beløp refusjonBeløpPerMnd;

    @ChangeTracked
    private LocalDate refusjonOpphører;

    private LocalDateTime innsendingstidspunkt;

    @ChangeTracked
    private String kanalreferanse;

    private String kildesystem;

    @ChangeTracked
    private List<Refusjon> endringerRefusjon = new ArrayList<>();

    @ChangeTracked
    private InntektsmeldingInnsendingsårsak innsendingsårsak = InntektsmeldingInnsendingsårsak.UDEFINERT;

    private FagsakYtelseType ytelseType = FagsakYtelseType.UDEFINERT;

    Inntektsmelding() {
    }

    public Inntektsmelding(Inntektsmelding inntektsmelding) {
        this.ytelseType = inntektsmelding.getFagsakYtelseType();
        this.arbeidsgiver = inntektsmelding.getArbeidsgiver();
        this.arbeidsforholdRef = inntektsmelding.arbeidsforholdRef;
        this.startDatoPermisjon = inntektsmelding.startDatoPermisjon;
        this.nærRelasjon = inntektsmelding.getErNærRelasjon();
        this.journalpostId = inntektsmelding.getJournalpostId();
        this.inntektBeløp = inntektsmelding.getInntektBeløp();
        this.refusjonBeløpPerMnd = inntektsmelding.getRefusjonBeløpPerMnd();
        this.refusjonOpphører = inntektsmelding.getRefusjonOpphører();
        this.innsendingsårsak = inntektsmelding.getInntektsmeldingInnsendingsårsak();
        this.innsendingstidspunkt = inntektsmelding.getInnsendingstidspunkt();
        this.kanalreferanse = inntektsmelding.getKanalreferanse();
        this.kildesystem = inntektsmelding.getKildesystem();
        this.mottattDato = inntektsmelding.getMottattDato();
        this.graderinger = inntektsmelding.getGraderinger();
        this.naturalYtelser = inntektsmelding.getNaturalYtelser();
        this.utsettelsePerioder = inntektsmelding.getUtsettelsePerioder();
        this.endringerRefusjon = inntektsmelding.getEndringerRefusjon();
        this.oppgittFravær = inntektsmelding.getOppgittFravær();
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { arbeidsgiver, arbeidsforholdRef, journalpostId, kanalreferanse };
        return IndexKeyComposer.createKey(keyParts);
    }

    /**
     * Referanse til journalpost (i arkivsystem - Joark) der dokumentet ligger.
     *
     * @return {@link JournalpostId} som inneholder denne inntektsmeldingen.
     */
    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    void setJournalpostId(JournalpostId journalpostId) {
        Objects.requireNonNull(journalpostId);
        this.journalpostId = journalpostId;
    }

    /**
     * Virksomheten som har sendt inn inntektsmeldingen
     *
     * @return {@link Virksomhet}
     */
    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    void setArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public InntektsmeldingInnsendingsårsak getInntektsmeldingInnsendingsårsak() {
        return innsendingsårsak;
    }

    void setInntektsmeldingInnsendingsårsak(InntektsmeldingInnsendingsårsak innsendingsårsak) {
        this.innsendingsårsak = innsendingsårsak;
    }

    public LocalDateTime getInnsendingstidspunkt() {
        return innsendingstidspunkt;
    }

    void setInnsendingstidspunkt(LocalDateTime innsendingstidspunkt) {
        this.innsendingstidspunkt = innsendingstidspunkt;
    }

    /**
     * Kanalreferanse, arkivnummer fra Altinn?
     *
     * @return kanalreferanse
     */
    public String getKanalreferanse() {
        return kanalreferanse;
    }

    void setKanalreferanse(String kanalreferanse) {
        this.kanalreferanse = kanalreferanse;
    }

    public String getKildesystem() {
        return kildesystem;
    }

    void setKildesystem(String kildesystem) {
        this.kildesystem = kildesystem;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    void setOmsorgspengerFravær(List<PeriodeAndel> input) {
        this.oppgittFravær = input == null ? Collections.emptyList() : new ArrayList<PeriodeAndel>(input);
    }

    /**
     * Liste over perioder med graderinger
     *
     * @return {@link Gradering}
     */
    public List<Gradering> getGraderinger() {
        return Collections.unmodifiableList(graderinger);
    }

    public List<PeriodeAndel> getOppgittFravær() {
        return Collections.unmodifiableList(oppgittFravær);
    }

    /** alle perioder har 0 timer. */
    public boolean harKunKorreksjon() {
        return !oppgittFravær.isEmpty()
            && oppgittFravær.stream().allMatch(f -> f.getVarighetPerDag() != null && f.getVarighetPerDag().isZero());
    }

    /**
     * Liste over naturalytelser
     *
     * @return {@link NaturalYtelse}
     */
    public List<NaturalYtelse> getNaturalYtelser() {
        return Collections.unmodifiableList(naturalYtelser);
    }

    /**
     * Liste over utsettelse perioder
     *
     * @return {@link UtsettelsePeriode}
     */
    public List<UtsettelsePeriode> getUtsettelsePerioder() {
        return Collections.unmodifiableList(utsettelsePerioder);
    }

    /**
     * Arbeidsgivers arbeidsforhold referanse
     *
     * @return {@link ArbeidsforholdRef}
     */
    public InternArbeidsforholdRef getArbeidsforholdRef() {
        // Returnere NULL OBJECT slik at vi alltid har en ref (selv om den inneholder null).
        // gjør enkelte sammenligninger (eks. gjelderFor) enklere.
        return arbeidsforholdRef != null ? arbeidsforholdRef : InternArbeidsforholdRef.nullRef();
    }

    /**
     * Gjelder for et spesifikt arbeidsforhold
     *
     * @return {@link Boolean}
     */
    public boolean gjelderForEtSpesifiktArbeidsforhold() {
        return getArbeidsforholdRef() != null && getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold();
    }

    public boolean gjelderSammeArbeidsforhold(Inntektsmelding annen) {
        return getArbeidsgiver().equals(annen.getArbeidsgiver())
            && (getArbeidsforholdRef() == null || annen.getArbeidsforholdRef() == null
                || (getArbeidsforholdRef() != null && getArbeidsforholdRef().gjelderFor(annen.getArbeidsforholdRef())));
    }

    /**
     * Setter intern arbeidsdforhold Id for inntektsmelding
     *
     * @param arbeidsforholdRef Intern arbeidsforhold id
     */
    void setArbeidsforholdId(InternArbeidsforholdRef arbeidsforholdRef) {
        this.arbeidsforholdRef = arbeidsforholdRef != null && !InternArbeidsforholdRef.nullRef().equals(arbeidsforholdRef) ? arbeidsforholdRef : null;
    }

    /**
     * Startdato for permisjonen
     *
     * @return {@link LocalDate}
     */
    public Optional<LocalDate> getStartDatoPermisjon() {
        return Optional.ofNullable(startDatoPermisjon);
    }

    void setStartDatoPermisjon(LocalDate startDatoPermisjon) {
        this.startDatoPermisjon = startDatoPermisjon;
    }

    /**
     * Er det nær relasjon mellom søker og arbeidsgiver
     *
     * @return {@link Boolean}
     */
    public boolean getErNærRelasjon() {
        return nærRelasjon;
    }

    void setNærRelasjon(boolean nærRelasjon) {
        this.nærRelasjon = nærRelasjon;
    }

    /**
     * Oppgitt årsinntekt fra arbeidsgiver
     *
     * @return {@link Beløp}
     */
    public Beløp getInntektBeløp() {
        return inntektBeløp;
    }

    void setInntektBeløp(Beløp inntektBeløp) {
        this.inntektBeløp = inntektBeløp;
    }

    /**
     * Beløpet arbeidsgiver ønsker refundert
     *
     * @return {@link Beløp}
     */
    public Beløp getRefusjonBeløpPerMnd() {
        return refusjonBeløpPerMnd;
    }

    void setRefusjonBeløpPerMnd(Beløp refusjonBeløpPerMnd) {
        this.refusjonBeløpPerMnd = refusjonBeløpPerMnd;
    }

    /**
     * Dersom refusjonen opphører i stønadsperioden angis siste dag det søkes om refusjon for.
     *
     * @return {@link LocalDate}
     */
    public LocalDate getRefusjonOpphører() {
        return refusjonOpphører;
    }

    void setRefusjonOpphører(LocalDate refusjonOpphører) {
        this.refusjonOpphører = refusjonOpphører;
    }

    /**
     * Liste over endringer i refusjonsbeløp
     *
     * @return {@Link Refusjon}
     */
    public List<Refusjon> getEndringerRefusjon() {
        return Collections.unmodifiableList(endringerRefusjon);
    }

    void leggTilFravær(PeriodeAndel fravær) {
        this.oppgittFravær.add(fravær);
    }

    void leggTil(Gradering gradering) {
        this.graderinger.add(gradering);
    }

    void leggTil(NaturalYtelse naturalYtelse) {
        this.naturalYtelser.add(naturalYtelse);
    }

    void leggTil(UtsettelsePeriode utsettelsePeriode) {
        this.utsettelsePerioder.add(utsettelsePeriode);
    }

    void leggTil(Refusjon refusjon) {
        this.endringerRefusjon.add(refusjon);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof Inntektsmelding)) {
            return false;
        }
        Inntektsmelding entitet = (Inntektsmelding) o;
        return Objects.equals(getArbeidsgiver(), entitet.getArbeidsgiver())
            && Objects.equals(getArbeidsforholdRef(), entitet.getArbeidsforholdRef())
            && Objects.equals(getKanalreferanse(), entitet.getKanalreferanse())
            && Objects.equals(getJournalpostId(), entitet.getJournalpostId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getArbeidsgiver(), getArbeidsforholdRef(), getKanalreferanse(), getJournalpostId());
    }

    @Override
    public String toString() {
        return "InntektsmeldingEntitet{" +
            "virksomhet=" + arbeidsgiver +
            ", arbeidsforholdId='" + arbeidsforholdRef + '\'' +
            ", startDatoPermisjon=" + startDatoPermisjon +
            ", nærRelasjon=" + nærRelasjon +
            ", journalpostId=" + journalpostId +
            ", inntektBeløp=" + inntektBeløp +
            ", refusjonBeløpPerMnd=" + refusjonBeløpPerMnd +
            ", refusjonOpphører=" + refusjonOpphører +
            ", innsendingsårsak= " + innsendingsårsak +
            ", innsendingstidspunkt= " + innsendingstidspunkt +
            ", mottattDato = " + mottattDato +
            '}';
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return ytelseType;
    }

    void setFagsakYtelseType(FagsakYtelseType fagsakYtelseType) {
        this.ytelseType = fagsakYtelseType;
    }

    public boolean erNyereEnn(Inntektsmelding andre) {
        return gjelderSammeArbeidsforhold(andre) && COMP_REKKEFØLGE.compare(this, andre) > 0;
    }

    public boolean harRefusjonskrav() {
        return getRefusjonBeløpPerMnd() != null || getRefusjonOpphører() != null || !getEndringerRefusjon().isEmpty();
    }

    public boolean harFravær() {
        return !getOppgittFravær().isEmpty();
    }
}
