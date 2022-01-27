package no.nav.k9.sak.ytelse.omsorgspenger.repo;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;

import org.hibernate.annotations.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.SøknadÅrsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.FraværÅrsakKodeConverter;
import no.nav.k9.sak.domene.uttak.repo.SøknadÅrsakKodeConverter;
import no.nav.k9.sak.domene.uttak.repo.UttakArbeidTypeKodeConverter;
import no.nav.k9.sak.perioder.VurdertSøktPeriode.SøktPeriodeData;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;

@Entity(name = "OmsorgspengerFraværPeriode")
@Table(name = "OMP_OPPGITT_FRAVAER_PERIODE")
@Immutable
public class OppgittFraværPeriode extends BaseEntitet implements IndexKey, SøktPeriodeData {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OMP_OPPGITT_FRAVAER_PERIODE")
    private Long id;

    @ChangeTracked
    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @ManyToOne
    @JoinColumn(name = "fravaer_id", nullable = false, updatable = false, unique = true)
    @JsonIgnore //må ha denne for å unngå sirkulær traversering fra Jackson - ved serialisering av regelsporing
    private OppgittFravær oppgittFravær;

    @Convert(converter = UttakArbeidTypeKodeConverter.class)
    @ChangeTracked
    @Column(name = "aktivitet", nullable = false, updatable = false)
    private UttakArbeidType aktivitet;

    @Convert(converter = FraværÅrsakKodeConverter.class)
    @ChangeTracked
    @Column(name = "fravaer_arsak", nullable = false, updatable = false)
    private FraværÅrsak fraværÅrsak;

    @Convert(converter = SøknadÅrsakKodeConverter.class)
    @ChangeTracked
    @Column(name = "soknad_arsak", nullable = false, updatable = false)
    private SøknadÅrsak søknadÅrsak;

    @ChangeTracked
    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @ChangeTracked
    @Embedded
    private InternArbeidsforholdRef arbeidsforholdRef;

    /**
     * tid oppgittFravær per dag. Hvis ikke oppgitt antas hele dagen å telle med.
     */
    @ChangeTracked
    @Column(name = "fravaer_per_dag", nullable = true)
    private Duration fraværPerDag;

    @Embedded
    @ChangeTracked
    @AttributeOverrides(@AttributeOverride(name = "verdi", column = @Column(name = "refusjonsbeloep")))
    private Beløp refusjonsbeløp;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    OppgittFraværPeriode() {
    }

    // Kun for test
    public OppgittFraværPeriode(LocalDate fom, LocalDate tom, UttakArbeidType aktivitetType, Duration fraværPerDag) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        this.aktivitet = Objects.requireNonNull(aktivitetType, "aktivitetType");
        this.fraværPerDag = fraværPerDag;
    }

    public OppgittFraværPeriode(JournalpostId journalpostId, LocalDate fom, LocalDate tom, UttakArbeidType aktivitetType, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, Duration fraværPerDag, Beløp refusjonsbeløp, FraværÅrsak fraværÅrsak, SøknadÅrsak søknadÅrsak) {
        this.journalpostId = journalpostId;
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.fraværPerDag = fraværPerDag;
        this.aktivitet = Objects.requireNonNull(aktivitetType, "aktivitetType");
        this.søknadÅrsak = søknadÅrsak;
        this.refusjonsbeløp = refusjonsbeløp;
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        this.fraværÅrsak = fraværÅrsak;
    }

    public OppgittFraværPeriode(LocalDate fom, LocalDate tom, OppgittFraværPeriode fra) {
        this(fra.getJournalpostId(), fom, tom, fra.getAktivitetType(), fra.getArbeidsgiver(), fra.getArbeidsforholdRef(), fra.getFraværPerDag(), fra.getRefusjonsbeløp(), fra.getFraværÅrsak(), fra.getSøknadÅrsak());
    }

    public OppgittFraværPeriode(OppgittFraværPeriode periode) {
        this.journalpostId = periode.journalpostId;
        this.arbeidsgiver = periode.arbeidsgiver;
        this.arbeidsforholdRef = periode.arbeidsforholdRef;
        this.fraværPerDag = periode.fraværPerDag;
        this.aktivitet = Objects.requireNonNull(periode.aktivitet, "aktivitetType");
        this.periode = periode.periode;
        this.refusjonsbeløp = periode.refusjonsbeløp;
        this.fraværÅrsak = periode.fraværÅrsak;
        this.søknadÅrsak = periode.søknadÅrsak;
    }


    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(journalpostId, periode, aktivitet, arbeidsgiver, arbeidsforholdRef);
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public LocalDate getFom() {
        return getPeriode().getFomDato();
    }

    public LocalDate getTom() {
        return getPeriode().getTomDato();
    }

    void setFravær(OppgittFravær uttak) {
        this.oppgittFravær = uttak;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef == null ? InternArbeidsforholdRef.nullRef() : arbeidsforholdRef;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public UttakArbeidType getAktivitetType() {
        return aktivitet;
    }

    public Duration getFraværPerDag() {
        return fraværPerDag;
    }

    public FraværÅrsak getFraværÅrsak() {
        return fraværÅrsak;
    }

    public SøknadÅrsak getSøknadÅrsak() {
        return søknadÅrsak;
    }

    public Beløp getRefusjonsbeløp() {
        return refusjonsbeløp;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V getPayload() {
        // et lite objekt med periode dataene (ikke periode, ikke journalpostid) til sammenligning
        return (V) Arrays.asList(fraværPerDag, aktivitet, fraværÅrsak, arbeidsgiver, arbeidsforholdRef);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof OppgittFraværPeriode))
            return false;
        OppgittFraværPeriode that = (OppgittFraværPeriode) o;
        return Objects.equals(periode, that.periode)
            && Objects.equals(journalpostId, that.journalpostId)
            && Objects.equals(fraværPerDag, that.fraværPerDag)
            && Objects.equals(aktivitet, that.aktivitet)
            && Objects.equals(fraværÅrsak, that.fraværÅrsak)
            && Objects.equals(arbeidsgiver, that.arbeidsgiver)
            && Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, journalpostId, fraværPerDag, aktivitet, fraværÅrsak, arbeidsgiver, arbeidsforholdRef);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "id=" + id +
            ", periode=" + periode +
            ", journalpostId=" + journalpostId +
            ", aktivitetType=" + aktivitet +
            ", fraværÅrsak=" + fraværÅrsak +
            ", søknadÅrsak=" + søknadÅrsak +
            (arbeidsgiver != null ? ", arbeidsgiver=" + arbeidsgiver : "") +
            (arbeidsforholdRef != null ? ", arbeidsforholdRef=" + arbeidsforholdRef : "") +
            (fraværPerDag != null ? ", fraværPerDag=" + fraværPerDag : "") +
            ", versjon=" + versjon +
            '>';
    }
}
