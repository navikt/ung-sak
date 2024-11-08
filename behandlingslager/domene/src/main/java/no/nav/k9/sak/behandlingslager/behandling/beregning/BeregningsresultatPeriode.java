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

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
    private BeregningsresultatEntitet beregningsresultat;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsresultatPeriode", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @OrderBy("periode, arbeidsgiver.arbeidsgiverOrgnr, arbeidsgiver.arbeidsgiverAktørId, arbeidsforholdRef, aktivitetStatus, inntektskategori")

    private List<BeregningsresultatAndel> beregningsresultatAndelList = new ArrayList<>();
    @Column(name = "total_utbetalingsgrad_fra_uttak")
    private BigDecimal totalUtbetalingsgradFraUttak;

    @Column(name = "total_utbetalingsgrad_etter_reduksjon_ved_tilkommet_inntekt")
    private BigDecimal totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt;

    @Column(name = "reduksjonsfaktor_inaktiv_type_a")
    private BigDecimal reduksjonsfaktorInaktivTypeA;


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


    public BigDecimal getTotalUtbetalingsgradFraUttak() {
        return totalUtbetalingsgradFraUttak;
    }

    public BigDecimal getTotalUtbetalingsgradEtterReduksjonVedTilkommetInntekt() {
        return totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt;
    }

    public BigDecimal getReduksjonsfaktorInaktivTypeA() {
        return reduksjonsfaktorInaktivTypeA;
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
        return getClass().getSimpleName()
            + "<periode=" + periode
            + (totalUtbetalingsgradFraUttak != null ? ", totalUtbetalingsgradFraUttak=" + totalUtbetalingsgradFraUttak.toPlainString() : "")
            + (totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt != null ? ", totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt=" + totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt.toPlainString() : "")
            + (reduksjonsfaktorInaktivTypeA != null ? ", reduksjonsfaktorInaktivTypeA=" + reduksjonsfaktorInaktivTypeA.toPlainString() : "")
            + ", andeler=[" + beregningsresultatAndelList.size() + "]>";
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

        public Builder medTotalUtbetalingsgradFraUttak(BigDecimal totalUtbetalingsgradFraUttak) {
            beregningsresultatPeriodeMal.totalUtbetalingsgradFraUttak = totalUtbetalingsgradFraUttak;
            return this;
        }

        public Builder medTotalUtbetalingsgradEtterReduksjonVedTilkommetInntekt(BigDecimal totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt) {
            beregningsresultatPeriodeMal.totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt = totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt;
            return this;
        }

        public Builder medReduksjonsfaktorInaktivTypeA(BigDecimal reduksjonsfaktorInaktivTypeA) {
            beregningsresultatPeriodeMal.reduksjonsfaktorInaktivTypeA = reduksjonsfaktorInaktivTypeA;
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
