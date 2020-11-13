package no.nav.k9.sak.behandlingslager.behandling.beregning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.vladmihalcea.hibernate.type.range.Range;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.kodeverk.AktivitetStatusKodeverdiConverter;
import no.nav.k9.sak.behandlingslager.kodeverk.InntektskategoriKodeverdiConverter;
import no.nav.k9.sak.behandlingslager.kodeverk.OpptjeningAktivitetTypeKodeverdiConverter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Periode;

@Entity(name = "BeregningsresultatAndel")
@Table(name = "BR_ANDEL")
public class BeregningsresultatAndel extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BR_ANDEL")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    /** @deprecated fjernes og erstattes med {@link #beregningsresultat}. */
    @JsonBackReference
    @Deprecated(forRemoval = true)
    @ManyToOne(optional = false)
    @JoinColumn(name = "br_periode_id", nullable = false, updatable = false)
    private BeregningsresultatPeriode beregningsresultatPeriode;

    @JsonBackReference
    @ManyToOne(optional = false)
    @JoinColumn(name = "BEREGNINGSRESULTAT_ID", nullable = false /* , updatable = false */)
    private BeregningsresultatEntitet beregningsresultat;

    @Column(name = "bruker_er_mottaker", nullable = false)
    private Boolean brukerErMottaker = Boolean.FALSE;

    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @Embedded
    private InternArbeidsforholdRef arbeidsforholdRef;

    @Convert(converter = OpptjeningAktivitetTypeKodeverdiConverter.class)
    @Column(name = "arbeidsforhold_type", nullable = false)
    private OpptjeningAktivitetType arbeidsforholdType;

    @Column(name = "dagsats", nullable = false)
    private int dagsats;

    @Column(name = "stillingsprosent", nullable = false)
    private BigDecimal stillingsprosent;

    @Column(name = "utbetalingsgrad", nullable = false)
    private BigDecimal utbetalingsgrad;

    @Column(name = "dagsats_fra_bg", nullable = false)
    private int dagsatsFraBg;

    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "verdi", column = @Column(name = "feriepenger_beloep")))
    @ChangeTracked
    private Beløp feriepengerBeløp;

    @Convert(converter = AktivitetStatusKodeverdiConverter.class)
    @Column(name = "aktivitet_status", nullable = false)
    private AktivitetStatus aktivitetStatus;

    @Convert(converter = InntektskategoriKodeverdiConverter.class)
    @Column(name = "inntektskategori", nullable = false)
    private Inntektskategori inntektskategori;

    BeregningsresultatAndel() {
        //
    }

    /** @deprecated skal erstattes av en ren copy ctor uten BeregningsresultatPeriode (under) */
    @Deprecated(forRemoval = true)
    // copy ctor
    public BeregningsresultatAndel(BeregningsresultatAndel fraAndel, BeregningsresultatPeriode tilknyttPeriode) {

        this.beregningsresultatPeriode = Objects.requireNonNull(tilknyttPeriode, "tilknyttPeriode");
        tilknyttPeriode.addBeregningsresultatAndel(this);

        // avleder periode i onCreateMigrate
        // avleder beregningsresultat i onCreateMigrate

        this.brukerErMottaker = fraAndel.brukerErMottaker;
        this.arbeidsforholdRef = fraAndel.arbeidsforholdRef;
        this.arbeidsgiver = fraAndel.arbeidsgiver;
        this.arbeidsforholdType = fraAndel.arbeidsforholdType;
        this.dagsats = fraAndel.dagsats;
        this.stillingsprosent = fraAndel.stillingsprosent;
        this.utbetalingsgrad = fraAndel.utbetalingsgrad;
        this.dagsatsFraBg = fraAndel.dagsatsFraBg;
        this.feriepengerBeløp = fraAndel.feriepengerBeløp;
        this.aktivitetStatus = fraAndel.aktivitetStatus;
        this.inntektskategori = fraAndel.inntektskategori;

    }

    // copy ctor
    public BeregningsresultatAndel(BeregningsresultatAndel fraAndel) {
        // avleder periode i onCreateMigrate
        // avleder beregningsresultat i onCreateMigrate

        this.brukerErMottaker = fraAndel.brukerErMottaker;
        this.arbeidsforholdRef = fraAndel.arbeidsforholdRef;
        this.arbeidsgiver = fraAndel.arbeidsgiver;
        this.arbeidsforholdType = fraAndel.arbeidsforholdType;
        this.dagsats = fraAndel.dagsats;
        this.stillingsprosent = fraAndel.stillingsprosent;
        this.utbetalingsgrad = fraAndel.utbetalingsgrad;
        this.dagsatsFraBg = fraAndel.dagsatsFraBg;
        this.feriepengerBeløp = fraAndel.feriepengerBeløp;
        this.aktivitetStatus = fraAndel.aktivitetStatus;
        this.inntektskategori = fraAndel.inntektskategori;

    }

    // copy ctor (erstatter over)
    public BeregningsresultatAndel(BeregningsresultatAndel fraAndel, Periode tilknyttPeriode) {

        Objects.requireNonNull(tilknyttPeriode, "tilknyttPeriode");
        this.periode = DatoIntervallEntitet.fra(tilknyttPeriode).toRange();
        // avleder beregningsresultat i onCreateMigrate

        this.brukerErMottaker = fraAndel.brukerErMottaker;
        this.arbeidsforholdRef = fraAndel.arbeidsforholdRef;
        this.arbeidsgiver = fraAndel.arbeidsgiver;
        this.arbeidsforholdType = fraAndel.arbeidsforholdType;
        this.dagsats = fraAndel.dagsats;
        this.stillingsprosent = fraAndel.stillingsprosent;
        this.utbetalingsgrad = fraAndel.utbetalingsgrad;
        this.dagsatsFraBg = fraAndel.dagsatsFraBg;
        this.feriepengerBeløp = fraAndel.feriepengerBeløp;
        this.aktivitetStatus = fraAndel.aktivitetStatus;
        this.inntektskategori = fraAndel.inntektskategori;

    }

    /** @deprecated brukes til migrering (fjerning av BR_PERIODE, BR_FERIEPENGER_PR_AAR er gjort). */
    @Deprecated(forRemoval = true)
    @PrePersist
    protected void onCreateMigrate() {
        if (this.periode == null) {
            DatoIntervallEntitet datoIntervall = beregningsresultatPeriode.getPeriode();
            if (datoIntervall.getFomDato().getYear() != datoIntervall.getTomDato().getYear()) {
                throw new IllegalStateException(String.format("periode fom har forskjellig år fra tom: %s", datoIntervall));
            }
            this.periode = datoIntervall.toRange();
        }

        if (this.beregningsresultat == null) {
            this.beregningsresultat = beregningsresultatPeriode.getBeregningsresultat();
        }
    }

    /** @deprecated brukes til migrering (fjerning av BR_PERIODE, BR_FERIEPENGER_PR_AAR er gjort). */
    @Deprecated(forRemoval = true)
    @PreUpdate
    protected void onUpdateMigrate() {
        if (this.periode == null) {
            DatoIntervallEntitet datoIntervall = beregningsresultatPeriode.getPeriode();
            if (datoIntervall.getFomDato().getYear() != datoIntervall.getTomDato().getYear()) {
                throw new IllegalStateException(String.format("periode fom har forskjellig år fra tom: %s", datoIntervall));
            }
            this.periode = datoIntervall.toRange();
        }
        if (this.beregningsresultat == null) {
            this.beregningsresultat = beregningsresultatPeriode.getBeregningsresultat();
        }
    }

    public Long getId() {
        return id;
    }

    /** @deprecated bruk fom/tom i stedet. */
    @Deprecated(forRemoval = true)
    public BeregningsresultatPeriode getBeregningsresultatPeriode() {
        return beregningsresultatPeriode;
    }

    public boolean erBrukerMottaker() {
        return brukerErMottaker;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef != null ? arbeidsforholdRef : InternArbeidsforholdRef.nullRef();
    }

    public String getArbeidsforholdIdentifikator() {
        return arbeidsgiver == null ? null : arbeidsgiver.getIdentifikator();
    }

    public boolean erArbeidsgiverPrivatperson() {
        return getArbeidsgiver().map(Arbeidsgiver::erAktørId).orElse(false);
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    public boolean skalTilBrukerEllerPrivatperson() {
        return brukerErMottaker || this.erArbeidsgiverPrivatperson();
    }

    public OpptjeningAktivitetType getArbeidsforholdType() {
        return arbeidsforholdType;
    }

    public int getDagsats() {
        return dagsats;
    }

    public Beløp getFeriepengerÅrsbeløp() {
        return feriepengerBeløp;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public int getDagsatsFraBg() {
        return dagsatsFraBg;
    }

    public void setFeriepengerBeløp(Beløp feriepengerÅrsbeløp) {
        if (this.feriepengerBeløp != null) {
            throw new IllegalArgumentException("Kan ikke endre feriepengerBeløp");
        }
        this.feriepengerBeløp = feriepengerÅrsbeløp;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public DatoIntervallEntitet getPeriode() {
        return this.periode != null ? DatoIntervallEntitet.fra(periode) : beregningsresultatPeriode.getPeriode();
    }

    public LocalDate getFom() {
        return getPeriode().getFomDato();
    }

    public LocalDate getTom() {
        return getPeriode().getTomDato();
    }

    /**
     * Returnerer en aktivitetsnøkkel som brukes til å idetifisere resultatandeler i forskjellige behandlinger.
     * Ideelt sett trengs kun BeregningsresultatAktivitetsnøkkelV2, men det må undersøkes mer om BeregningsresultatAktivitetsnøkkel tjener en
     * unik funksjon.
     * Brukes til å identifisere endringsdato, så undersøk om man her kan bruke BeregningsresultatAktivitetsnøkkelV2 istedet.
     * 
     * @return Nøkkel med Aktivitetstatus, arbeidsgiver, inntektskategori, arbeidsforholdreferanse
     */
    public BeregningsresultatAktivitetsnøkkel getAktivitetsnøkkel() {
        return new BeregningsresultatAktivitetsnøkkel(this);
    }

    /**
     * Returnerer en aktivitetsnøkkel som kan brukes til å identifisere like andeler
     * men som ikke skiller på andeler hos samme arbeidsgiver på forskjellige arbeidsforhold.
     * 
     * @return Nøkkel med Aktivitetstatus og arbeidsgiver
     */
    public BeregningsresultatAktivitetsnøkkelV2 getAktivitetsnøkkelV2() {
        return new BeregningsresultatAktivitetsnøkkelV2(this);
    }

    @Override
    public String toString() {
        return "BeregningsresultatAndel{" +
            "brukerErMottaker=" + brukerErMottaker +
            ", arbeidsgiver=" + arbeidsgiver +
            ", arbeidsforholdRef=" + arbeidsforholdRef +
            ", arbeidsforholdType=" + arbeidsforholdType +
            ", dagsats=" + dagsats +
            ", stillingsprosent=" + stillingsprosent +
            ", utbetalingsgrad=" + utbetalingsgrad +
            ", dagsatsFraBg=" + dagsatsFraBg +
            ", aktivitetStatus=" + aktivitetStatus +
            ", inntektskategori=" + inntektskategori +
            ", feriepengerÅrsbeløp=" + feriepengerBeløp +
            '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof BeregningsresultatAndel)) {
            return false;
        }
        BeregningsresultatAndel other = (BeregningsresultatAndel) obj;
        return Objects.equals(this.getArbeidsgiver(), other.getArbeidsgiver())
            && Objects.equals(this.getArbeidsforholdRef(), other.getArbeidsforholdRef())
            && Objects.equals(this.getArbeidsforholdType(), other.getArbeidsforholdType())
            && Objects.equals(this.getInntektskategori(), other.getInntektskategori())
            && Objects.equals(this.erBrukerMottaker(), other.erBrukerMottaker())
            && Objects.equals(this.getDagsats(), other.getDagsats())
            && equals(this.getStillingsprosent(), other.getStillingsprosent())
            && equals(this.getUtbetalingsgrad(), other.getUtbetalingsgrad())
            && Objects.equals(this.getDagsatsFraBg(), other.getDagsatsFraBg())
            && Objects.equals(this.getFeriepengerÅrsbeløp(), other.getFeriepengerÅrsbeløp());
    }

    private boolean equals(BigDecimal a, BigDecimal b) {
        return (a == b) || (a != null && a.compareTo(b) == 0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(brukerErMottaker, arbeidsgiver, arbeidsforholdRef, arbeidsforholdType, dagsats, aktivitetStatus, dagsatsFraBg, stillingsprosent, utbetalingsgrad, inntektskategori,
            feriepengerBeløp);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BeregningsresultatAndel eksisterendeBeregningsresultatAndel) {
        return new Builder(eksisterendeBeregningsresultatAndel);
    }

    public static class Builder {

        private BeregningsresultatAndel mal;
        private Integer dagsats;
        private Integer dagsatsFraBg;

        public Builder() {
            mal = new BeregningsresultatAndel();
            mal.arbeidsforholdType = OpptjeningAktivitetType.UDEFINERT;
        }

        public Builder(BeregningsresultatAndel eksisterendeBeregningsresultatAndel) {
            mal = eksisterendeBeregningsresultatAndel;
        }

        public Builder medBrukerErMottaker(boolean brukerErMottaker) {
            mal.brukerErMottaker = brukerErMottaker;
            return this;
        }

        public Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            mal.arbeidsgiver = arbeidsgiver;
            return this;
        }

        /**
         * @deprecated Bruk {@link #medArbeidsforholdRef(InternArbeidsforholdRef)}
         */
        @Deprecated(forRemoval = true)
        public Builder medArbeidsforholdRef(String arbeidsforholdRef) {
            mal.arbeidsforholdRef = arbeidsforholdRef == null ? null : InternArbeidsforholdRef.ref(arbeidsforholdRef);
            return this;
        }

        public Builder medArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
            mal.arbeidsforholdRef = arbeidsforholdRef;
            return this;
        }

        public Builder medArbeidsforholdType(OpptjeningAktivitetType arbeidsforholdType) {
            mal.arbeidsforholdType = arbeidsforholdType;
            return this;
        }

        public Builder medDagsats(int dagsats) {
            this.dagsats = dagsats;
            mal.dagsats = dagsats;
            return this;
        }

        public Builder medStillingsprosent(BigDecimal stillingsprosent) {
            mal.stillingsprosent = stillingsprosent;
            return this;
        }

        public Builder medPeriode(DatoIntervallEntitet periode) {
            if (periode.getFomDato().getYear() != periode.getTomDato().getYear()) {
                throw new IllegalStateException(String.format("periode fom har forskjellig år fra tom: %s", periode));
            }
            mal.periode = periode.toRange();
            return this;
        }

        public Builder medPeriode(LocalDateInterval periode) {
            return this.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato()));
        }

        public Builder medFeriepengerÅrsbeløp(Beløp beløp) {
            mal.feriepengerBeløp = beløp;
            return this;
        }

        public Builder medUtbetalingsgrad(BigDecimal utbetalingsgrad) {
            mal.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        public Builder medDagsatsFraBg(int dagsatsFraBg) {
            this.dagsatsFraBg = dagsatsFraBg;
            mal.dagsatsFraBg = dagsatsFraBg;
            return this;
        }

        public Builder medAktivitetStatus(AktivitetStatus aktivitetStatus) {
            mal.aktivitetStatus = aktivitetStatus;
            return this;
        }

        public Builder medInntektskategori(Inntektskategori inntektskategori) {
            mal.inntektskategori = inntektskategori;
            return this;
        }

        public BeregningsresultatAndel buildFor(BeregningsresultatPeriode beregningsresultatPeriode) {
            mal.beregningsresultatPeriode = beregningsresultatPeriode;
            mal.periode = beregningsresultatPeriode.getPeriode().toRange();
            mal.beregningsresultat = beregningsresultatPeriode.getBeregningsresultat();
            verifyStateForBuild();
            mal.getBeregningsresultatPeriode().addBeregningsresultatAndel(mal);
            return mal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(mal.brukerErMottaker, "brukerErMottaker");
            Objects.requireNonNull(mal.stillingsprosent, "stillingsprosent");
            verifyUtbetalingsgrad(mal.utbetalingsgrad);
            Objects.requireNonNull(mal.inntektskategori, "inntektskategori");
            Objects.requireNonNull(dagsatsFraBg, "dagsatsFraBg");
            Objects.requireNonNull(dagsats, "dagsats");
            Objects.requireNonNull(mal.beregningsresultat, "beregningsresultat");
            Objects.requireNonNull(mal.periode, "periode");
            if (!mal.brukerErMottaker) {
                Objects.requireNonNull(mal.arbeidsgiver, "virksomhet");
            }
        }

        private void verifyUtbetalingsgrad(BigDecimal utbetalingsgrad) {
            Objects.requireNonNull(utbetalingsgrad, "uttaksgrad");
            boolean mellomGyldigIntervall = utbetalingsgrad.compareTo(BigDecimal.ZERO) >= 0 &&
                utbetalingsgrad.compareTo(BigDecimal.valueOf(100)) <= 0;
            if (!mellomGyldigIntervall) {
                throw new IllegalStateException("Utviklerfeil: Utbetalingsgrad må være mellom 0 og 100");
            }
        }

    }

}
