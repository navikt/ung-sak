package no.nav.k9.sak.behandlingslager.behandling.beregning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "BeregningsresultatPeriode")
@Table(name = "BR_PERIODE")
@DynamicInsert
@DynamicUpdate
public class BeregningsresultatPeriode extends BaseEntitet {

    private static final Comparator<BeregningsresultatAndel> COMP_BEREGININGSRESULTAT_ANDEL = Comparator
        .comparing((BeregningsresultatAndel ba) -> ba.getArbeidsforholdIdentifikator(), Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(ba -> ba.getArbeidsforholdRef().getReferanse(), Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(ba -> ba.getAktivitetStatus(), Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(ba -> ba.getInntektskategori(), Comparator.nullsLast(Comparator.naturalOrder()));
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BR_PERIODE")
    private Long id;
    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;
    @ManyToOne(optional = false)
    @JoinColumn(name = "BEREGNINGSRESULTAT_FP_ID", nullable = false, updatable = false)
    @JsonBackReference
    private BeregningsresultatEntitet beregningsresultat;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsresultatPeriode", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @OrderBy("arbeidsgiver.arbeidsgiverOrgnr, arbeidsgiver.arbeidsgiverAktørId, arbeidsforholdRef, aktivitetStatus, inntektskategori")
    private List<BeregningsresultatAndel> beregningsresultatAndelList = new ArrayList<>();
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fomDato", column = @Column(name = "br_periode_fom")),
            @AttributeOverride(name = "tomDato", column = @Column(name = "br_periode_tom"))
    })
    private DatoIntervallEntitet periode;

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BeregningsresultatPeriode eksisterendeBeregningsresultatPeriode) {
        return new Builder(eksisterendeBeregningsresultatPeriode);
    }

    public Long getId() {
        return id;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public LocalDate getBeregningsresultatPeriodeFom() {
        return periode.getFomDato();
    }

    public LocalDate getBeregningsresultatPeriodeTom() {
        return periode.getTomDato();
    }

    public List<BeregningsresultatAndel> getBeregningsresultatAndelList() {
        return Collections.unmodifiableList(beregningsresultatAndelList)
            .stream()
            .sorted(COMP_BEREGININGSRESULTAT_ANDEL)
            .collect(Collectors.toList());
    }

    public Optional<BigDecimal> getLavestUtbetalingsgrad() {
        return getBeregningsresultatAndelList().stream()
            .filter(a -> a.getDagsats() > 0)
            .map(BeregningsresultatAndel::getUtbetalingsgrad)
            .min(Comparator.naturalOrder());
    }

    public BeregningsresultatEntitet getBeregningsresultat() {
        return beregningsresultat;
    }

    void addBeregningsresultatAndel(BeregningsresultatAndel beregningsresultatAndel) {
        Objects.requireNonNull(beregningsresultatAndel, "beregningsresultatAndel");
        if (!beregningsresultatAndelList.contains(beregningsresultatAndel)) { // NOSONAR Class defines List based fields but uses them like Sets: Ingening å tjene på å bytte til Set ettersom det er
                                                                              // små lister
            beregningsresultatAndelList.add(beregningsresultatAndel);
        }
    }

    public int getDagsats() {
        return getBeregningsresultatAndelList().stream()
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof BeregningsresultatPeriode)) {
            return false;
        }
        BeregningsresultatPeriode other = (BeregningsresultatPeriode) obj;
        return Objects.equals(this.getBeregningsresultatPeriodeFom(), other.getBeregningsresultatPeriodeFom())
            && Objects.equals(this.getBeregningsresultatPeriodeTom(), other.getBeregningsresultatPeriodeTom());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<periode=" + periode + ", andeler=[" + beregningsresultatAndelList.size() + "]>";
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode);
    }

    public static class Builder {
        private BeregningsresultatPeriode beregningsresultatPeriodeMal;

        public Builder() {
            beregningsresultatPeriodeMal = new BeregningsresultatPeriode();
        }

        public Builder(BeregningsresultatPeriode eksisterendeBeregningsresultatPeriode) {
            beregningsresultatPeriodeMal = eksisterendeBeregningsresultatPeriode;
        }

        public Builder medBeregningsresultatPeriodeFomOgTom(LocalDate beregningsresultatPeriodeFom, LocalDate beregningsresultatPeriodeTom) {
            beregningsresultatPeriodeMal.periode = DatoIntervallEntitet.fraOgMedTilOgMed(beregningsresultatPeriodeFom, beregningsresultatPeriodeTom);
            return this;
        }

        public BeregningsresultatPeriode build(BeregningsresultatEntitet beregningsresultat) {
            beregningsresultatPeriodeMal.beregningsresultat = beregningsresultat;
            verifyStateForBuild();
            beregningsresultatPeriodeMal.beregningsresultat.addBeregningsresultatPeriode(beregningsresultatPeriodeMal);
            return beregningsresultatPeriodeMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(beregningsresultatPeriodeMal.beregningsresultatAndelList, "beregningsresultatAndeler");
            Objects.requireNonNull(beregningsresultatPeriodeMal.beregningsresultat, "beregningsresultat");
            Objects.requireNonNull(beregningsresultatPeriodeMal.periode, "beregningsresultatPeriodePeriode");
            Objects.requireNonNull(beregningsresultatPeriodeMal.periode.getFomDato(), "beregningsresultaPeriodeFom");
            Objects.requireNonNull(beregningsresultatPeriodeMal.periode.getTomDato(), "beregningsresultaPeriodeTom");
        }
    }

}
