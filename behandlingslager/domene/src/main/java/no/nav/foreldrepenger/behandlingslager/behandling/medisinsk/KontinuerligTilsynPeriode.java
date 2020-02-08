package no.nav.foreldrepenger.behandlingslager.behandling.medisinsk;

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

@Entity(name = "KontinuerligTilsynPeriode")
@Table(name = "MD_KONTINUERLIG_TILSYN_PERIODE")
public class KontinuerligTilsynPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MD_KONTINUERLIG_TILSYN_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @Column(name = "begrunnelse", nullable = false)
    private String begrunnelse;

    @Column(name = "grad", nullable = false)
    private int grad;

    @ManyToOne
    @JoinColumn(name = "kontinuerlig_tilsyn_id", nullable = false, updatable = false, unique = true)
    private KontinuerligTilsyn kontinuerligTilsyn;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    KontinuerligTilsynPeriode() {
    }

    public KontinuerligTilsynPeriode(DatoIntervallEntitet periode, String begrunnelse, int grad) {
        this.periode = periode;
        this.begrunnelse = begrunnelse;
        this.grad = validerGrad(grad);
    }

    KontinuerligTilsynPeriode(KontinuerligTilsynPeriode kontinuerligTilsynPeriode) {
        this.periode = kontinuerligTilsynPeriode.periode;
        this.begrunnelse = kontinuerligTilsynPeriode.begrunnelse;
        this.grad = kontinuerligTilsynPeriode.grad;
    }

    private int validerGrad(int grad) {
        if (grad < 0 || grad > 200) {
            throw new IllegalArgumentException("Grad overskrider gyldige verdier");
        }
        return grad;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setKontinuerligTilsyn(KontinuerligTilsyn kontinuerligTilsyn) {
        this.kontinuerligTilsyn = kontinuerligTilsyn;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public int getGrad() {
        return grad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KontinuerligTilsynPeriode that = (KontinuerligTilsynPeriode) o;
        return grad == that.grad &&
            Objects.equals(periode, that.periode) &&
            Objects.equals(begrunnelse, that.begrunnelse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, begrunnelse, grad);
    }

    @Override
    public String toString() {
        return "KontinuerligTilsynPeriode{" +
            "id=" + id +
            ", periode=" + periode +
            ", versjon=" + versjon +
            '}';
    }

}
