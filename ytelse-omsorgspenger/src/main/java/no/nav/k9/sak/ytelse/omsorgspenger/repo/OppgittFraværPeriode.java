package no.nav.k9.sak.ytelse.omsorgspenger.repo;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@Entity(name = "OmsorgspengerFraværPeriode")
@Table(name = "OMP_OPPGITT_FRAVAER_PERIODE")
@Immutable
public class OppgittFraværPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OMP_OPPGITT_FRAVAER_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
            @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @ManyToOne
    @JoinColumn(name = "fravaer_id", nullable = false, updatable = false, unique = true)
    private OppgittFravær oppgittFravær;

    @Column(name = "aktivitet_type", nullable = false, updatable = false)
    private UttakArbeidType aktivitetType;

    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @Embedded
    private InternArbeidsforholdRef arbeidsforholdRef;

    /** tid oppgittFravær per dag. */
    @Column(name = "fravaer_per_dag")
    private Duration fraværPerDag;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    OppgittFraværPeriode() {
    }

    public OppgittFraværPeriode(LocalDate fom, LocalDate tom, UttakArbeidType aktivitetType, Duration fraværPerDag) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        this.aktivitetType = Objects.requireNonNull(aktivitetType, "aktivitetType");
        this.fraværPerDag = fraværPerDag;
    }

    public OppgittFraværPeriode(LocalDate fom, LocalDate tom, UttakArbeidType aktivitetType, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, Duration fraværPerDag) {
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.fraværPerDag = fraværPerDag;
        this.aktivitetType = Objects.requireNonNull(aktivitetType, "aktivitetType");
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);

    }

    public OppgittFraværPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setFravær(OppgittFravær uttak) {
        this.oppgittFravær = uttak;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public UttakArbeidType getAktivitetType() {
        return aktivitetType;
    }

    public Duration getFraværPerDag() {
        return fraværPerDag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof OppgittFraværPeriode))
            return false;
        OppgittFraværPeriode that = (OppgittFraværPeriode) o;
        return Objects.equals(periode, that.periode)
            && Objects.equals(aktivitetType, that.aktivitetType)
            && Objects.equals(arbeidsgiver, that.arbeidsgiver)
            && Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, aktivitetType, arbeidsgiver, arbeidsforholdRef);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "id=" + id +
            ", periode=" + periode +
            ", aktivitetType=" + arbeidsgiver +
            (arbeidsgiver != null ? ", arbeidsgiver=" + arbeidsgiver : "") +
            (arbeidsforholdRef != null ? ", arbeidsforholdRef=" + arbeidsforholdRef : "") +
            (fraværPerDag != null ? ", jobberNormaltPerUke=" + fraværPerDag : "") +
            ", versjon=" + versjon +
            '>';
    }

}
