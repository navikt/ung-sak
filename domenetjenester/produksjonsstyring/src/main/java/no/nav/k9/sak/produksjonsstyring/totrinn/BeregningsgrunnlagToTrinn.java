package no.nav.k9.sak.produksjonsstyring.totrinn;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "BeregningsgrunnlagToTrinn")
@Table(name = "TT_BEREGNING")
@Immutable
public class BeregningsgrunnlagToTrinn extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TT_BEREGNING")
    private Long id;

    @Column(name = "skjaeringstidspunkt", nullable = false)
    private LocalDate skjæringstidspunkt;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsgrunnlagToTrinn", cascade = CascadeType.PERSIST)
    @BatchSize(size=20)
    private List<BeregningsgrunnlagToTrinnFaktaTilfelle> faktaOmBeregningTilfeller = new ArrayList<>();

    @Column(name = "fastsatt_varig_endring", nullable = false)
    private Boolean fastsattVarigEndring;


    public BeregningsgrunnlagToTrinn() {
    }

    public BeregningsgrunnlagToTrinn(LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }


    public BeregningsgrunnlagToTrinn(LocalDate skjæringstidspunkt, List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfelleList, Boolean fastsattVarigEndring) {
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.fastsattVarigEndring = fastsattVarigEndring;
        setFaktaOmBeregningTilfeller(faktaOmBeregningTilfelleList);
    }

    private void setFaktaOmBeregningTilfeller(List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfelleList) {
        this.faktaOmBeregningTilfeller = faktaOmBeregningTilfelleList.stream().map(BeregningsgrunnlagToTrinnFaktaTilfelle::new).collect(Collectors.toList());
        this.faktaOmBeregningTilfeller.forEach(beregningsgrunnlagToTrinnFaktaTilfelle -> beregningsgrunnlagToTrinnFaktaTilfelle.setBeregningsgrunnlagToTrinn(this));
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public List<FaktaOmBeregningTilfelle> getFaktaOmBeregningTilfeller() {
        return faktaOmBeregningTilfeller.stream()
            .map(BeregningsgrunnlagToTrinnFaktaTilfelle::getFaktaOmBeregningTilfelle)
            .collect(Collectors.toList());
    }

    public Boolean getFastsattVarigEndring() {
        return fastsattVarigEndring;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeregningsgrunnlagToTrinn that = (BeregningsgrunnlagToTrinn) o;
        return Objects.equals(skjæringstidspunkt, that.skjæringstidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skjæringstidspunkt);
    }

    @Override
    public String toString() {
        return "BeregningsgrunnlagToTrinn{" +
            "skjæringstidspunkt=" + skjæringstidspunkt +
            "faktaOmBeregnintTilfeller=" + faktaOmBeregningTilfeller +
            "fastsattVarigEndring=" + fastsattVarigEndring +
            '}';
    }
}
