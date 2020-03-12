package no.nav.k9.sak.domene.uttak.repo;

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

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@Entity(name = "UttakPeriode")
@Table(name = "UT_UTTAK_PERIODE")
public class UttakPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UT_UTTAK_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
            @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @ManyToOne
    @JoinColumn(name = "uttak_id", nullable = false, updatable = false, unique = true)
    private Uttak uttak;

    @Column(name = "uttak_aktivitet_type", nullable = false, updatable = false)
    private UttakArbeidType aktivitetType;
    
    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @Embedded
    private InternArbeidsforholdRef arbeidsforholdRef;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    UttakPeriode() {
    }

    public UttakPeriode(LocalDate fom, LocalDate tom, UttakArbeidType aktivitetType) {
        this.aktivitetType = Objects.requireNonNull(aktivitetType, "aktivitetType");
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    public UttakPeriode(LocalDate fom, LocalDate tom, UttakArbeidType aktivitetType, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.aktivitetType = Objects.requireNonNull(aktivitetType, "aktivitetType");
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    public UttakPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setUttak(Uttak uttak) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof UttakPeriode))
            return false;
        UttakPeriode that = (UttakPeriode) o;
        return Objects.equals(periode, that.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "id=" + id +
            ", periode=" + periode +
            ", versjon=" + versjon +
            '>';
    }

}
