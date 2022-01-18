package no.nav.k9.sak.produksjonsstyring.totrinn;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Immutable;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.kodeverk.FaktaOmBeregningTilfelleKodeverdiConverter;

@Entity(name = "BeregningsgrunnlagToTrinnFaktaTilfelle")
@Table(name = "TT_BG_FAKTA_TILFELLE")
@Immutable
public class BeregningsgrunnlagToTrinnFaktaTilfelle extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TT_BEREGNING")
    private Long id;

    @Convert(converter = FaktaOmBeregningTilfelleKodeverdiConverter.class)
    @Column(name = "FAKTA_BEREGNING_TILFELLE", nullable = false, updatable = false)
    private FaktaOmBeregningTilfelle faktaOmBeregningTilfelle;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TT_BEREGNING_ID", nullable = false, updatable = false, unique = true)
    private BeregningsgrunnlagToTrinn beregningsgrunnlagToTrinn;

    public BeregningsgrunnlagToTrinnFaktaTilfelle() {
    }

    public BeregningsgrunnlagToTrinnFaktaTilfelle(FaktaOmBeregningTilfelle faktaOmBeregningTilfelle) {
        this.faktaOmBeregningTilfelle = faktaOmBeregningTilfelle;
    }

    void setBeregningsgrunnlagToTrinn(BeregningsgrunnlagToTrinn beregningsgrunnlagToTrinn) {
        this.beregningsgrunnlagToTrinn = beregningsgrunnlagToTrinn;
    }

    public FaktaOmBeregningTilfelle getFaktaOmBeregningTilfelle() {
        return faktaOmBeregningTilfelle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeregningsgrunnlagToTrinnFaktaTilfelle that = (BeregningsgrunnlagToTrinnFaktaTilfelle) o;
        return Objects.equals(faktaOmBeregningTilfelle, that.faktaOmBeregningTilfelle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(faktaOmBeregningTilfelle);
    }

    @Override
    public String toString() {
        return "BeregningsgrunnlagToTrinnFaktaTilfeller{" +
            "faktaOmBeregningTilfelle=" + faktaOmBeregningTilfelle +
            '}';
    }
}
