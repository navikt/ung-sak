package no.nav.k9.sak.domene.uttak.repo;

import java.math.BigDecimal;
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
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;

import org.hibernate.annotations.Immutable;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@Entity(name = "UttakAktivitetPeriode")
@Table(name = "UT_UTTAK_AKTIVITET_PERIODE")
@Immutable
public class UttakAktivitetPeriode extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UT_UTTAK_AKTIVITET_PERIODE")
    private Long id;

    @ChangeTracked
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
            @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @ManyToOne
    @JoinColumn(name = "aktivitet_id", nullable = false, updatable = false, unique = true)
    private UttakAktivitet uttak;

    @ChangeTracked
    @Column(name = "aktivitet_type", nullable = false, updatable = false)
    private UttakArbeidType aktivitetType;

    @ChangeTracked
    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @ChangeTracked
    @Embedded
    private InternArbeidsforholdRef arbeidsforholdRef;

    /** prosent av normalt man kommer til å jobbe når man har denne ytelsen. */
    @ChangeTracked
    @Column(name = "skal_jobbe_prosent", updatable = false)
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal skalJobbeProsent;

    @ChangeTracked
    /** tid jobber normalt per uke (timer etc.) til vanlig (hvis man ikke hadde hatt denne ytelsen). */
    @Column(name = "jobber_normalt_per_uke")
    private Duration jobberNormaltPerUke;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    UttakAktivitetPeriode() {
    }

    public UttakAktivitetPeriode(LocalDate fom, LocalDate tom, UttakArbeidType aktivitetType, Duration jobberNormaltPerUke, BigDecimal skalJobbeProsent) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        this.aktivitetType = Objects.requireNonNull(aktivitetType, "aktivitetType");
        this.jobberNormaltPerUke = jobberNormaltPerUke;
        this.skalJobbeProsent = skalJobbeProsent;
        validerTilstand();
    }

    public UttakAktivitetPeriode(LocalDate fom, LocalDate tom, UttakArbeidType aktivitetType, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, Duration jobberNormaltPerUke,
                                 BigDecimal skalJobbeProsent) {
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.jobberNormaltPerUke = jobberNormaltPerUke;
        this.aktivitetType = Objects.requireNonNull(aktivitetType, "aktivitetType");
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);

        // sett og valider til slutt
        this.skalJobbeProsent = skalJobbeProsent;
        validerTilstand();
    }

    public UttakAktivitetPeriode(UttakArbeidType aktivitetType, DatoIntervallEntitet periode) {
        this.periode = Objects.requireNonNull(periode, "periode");
        this.aktivitetType = Objects.requireNonNull(aktivitetType, "aktivitetType");
    }
    
    public UttakAktivitetPeriode(UttakArbeidType aktivitetType, LocalDate fom, LocalDate tom) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        this.aktivitetType = Objects.requireNonNull(aktivitetType, "aktivitetType");
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(periode);
    }
    
    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setUttak(UttakAktivitet uttak) {
        this.uttak = uttak;
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

    public BigDecimal getSkalJobbeProsent() {
        return skalJobbeProsent;
    }

    public Duration getJobberNormaltPerUke() {
        return jobberNormaltPerUke;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof UttakAktivitetPeriode))
            return false;
        UttakAktivitetPeriode that = (UttakAktivitetPeriode) o;
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
            (skalJobbeProsent != null ? ", skalJobbe=" + skalJobbeProsent + "%" : "") +
            (jobberNormaltPerUke != null ? ", jobberNormaltPerUke=" + jobberNormaltPerUke : "") +
            ", versjon=" + versjon +
            '>';
    }

    private void validerTilstand() {
        if (skalJobbeProsent != null) {
            if (skalJobbeProsent.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("skalJobbeProsent [" + skalJobbeProsent + "]kan ikke være <0 for periode= " + periode + ", aktivitetType=" + aktivitetType);
            } else if (skalJobbeProsent.compareTo(BigDecimal.valueOf(100L)) > 0) {
                throw new IllegalArgumentException("skalJobbeProsent [" + skalJobbeProsent + "]kan ikke være >100 for periode= " + periode + ", aktivitetType=" + aktivitetType);
            } else if (skalJobbeProsent.compareTo(BigDecimal.ZERO) != 0) {
                if (this.jobberNormaltPerUke == null) {
                    // må ha satt jobberNormaltPerUke
                    throw new IllegalArgumentException("Kan ikke jobberNormaltPerUke==null når skalJobbeProsent=" + skalJobbeProsent + " i periode=" + periode);
                }
            }
        }
    }

}
