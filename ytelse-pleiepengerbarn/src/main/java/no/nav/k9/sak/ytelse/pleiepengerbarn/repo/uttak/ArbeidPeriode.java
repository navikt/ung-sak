package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.time.Duration;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakArbeidTypeKodeConverter;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@Entity(name = "PsbArbeidPeriode")
@Table(name = "UP_ARBEID_PERIODE")
@Immutable
public class ArbeidPeriode extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UP_ARBEID_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @Convert(converter = UttakArbeidTypeKodeConverter.class)
    @ChangeTracked
    @Column(name = "aktivitet_type", nullable = false, updatable = false)
    private UttakArbeidType aktivitetType;

    @ChangeTracked
    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @ChangeTracked
    @Embedded
    private InternArbeidsforholdRef arbeidsforholdRef;

    /**
     * prosent av normalt man kommer til å jobbe når man har denne ytelsen.
     */
    @ChangeTracked
    @Column(name = "faktisk_arbeid_per_dag", updatable = false)
    private Duration faktiskArbeidTimerPerDag;

    @ChangeTracked
    /* tid jobber normalt per uke (timer etc.) til vanlig (hvis man ikke hadde hatt denne ytelsen). */
    @Column(name = "normalt_arbeid_per_dag", updatable = false)
    private Duration jobberNormaltTimerPerDag;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    ArbeidPeriode() {
    }

    public ArbeidPeriode(DatoIntervallEntitet periode,
                         UttakArbeidType aktivitetType,
                         Arbeidsgiver arbeidsgiver,
                         InternArbeidsforholdRef arbeidsforholdRef,
                         Duration faktiskArbeidTimerPerDag,
                         Duration jobberNormaltTimerPerDag) {
        this.periode = periode;
        this.aktivitetType = aktivitetType;
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.faktiskArbeidTimerPerDag = faktiskArbeidTimerPerDag;
        this.jobberNormaltTimerPerDag = jobberNormaltTimerPerDag;
    }

    public ArbeidPeriode(ArbeidPeriode it) {
        this.periode = it.periode;
        this.aktivitetType = it.aktivitetType;
        this.arbeidsgiver = it.arbeidsgiver;
        this.arbeidsforholdRef = it.arbeidsforholdRef;
        this.faktiskArbeidTimerPerDag = it.faktiskArbeidTimerPerDag;
        this.jobberNormaltTimerPerDag = it.jobberNormaltTimerPerDag;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public UttakArbeidType getAktivitetType() {
        return aktivitetType;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public Duration getFaktiskArbeidTimerPerDag() {
        return faktiskArbeidTimerPerDag;
    }

    public Duration getJobberNormaltTimerPerDag() {
        return jobberNormaltTimerPerDag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArbeidPeriode that = (ArbeidPeriode) o;
        return Objects.equals(periode, that.periode) && aktivitetType == that.aktivitetType && Objects.equals(arbeidsgiver, that.arbeidsgiver) && Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef) && Objects.equals(faktiskArbeidTimerPerDag, that.faktiskArbeidTimerPerDag) && Objects.equals(jobberNormaltTimerPerDag, that.jobberNormaltTimerPerDag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, aktivitetType, arbeidsgiver, arbeidsforholdRef, faktiskArbeidTimerPerDag, jobberNormaltTimerPerDag);
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(aktivitetType, arbeidsgiver, arbeidsforholdRef, periode);
    }

    @Override
    public String toString() {
        return "ArbeidPeriode{" +
            "periode=" + periode +
            ", aktivitetType=" + aktivitetType +
            ", arbeidsgiver=" + arbeidsgiver +
            ", arbeidsforholdRef=" + arbeidsforholdRef +
            ", faktiskArbeidTimerPerDag=" + faktiskArbeidTimerPerDag +
            ", jobberNormaltTimerPerDag=" + jobberNormaltTimerPerDag +
            '}';
    }
}
