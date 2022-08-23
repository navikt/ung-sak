package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov;

import java.util.Objects;

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
import no.nav.k9.kodeverk.medisinsk.Pleiegrad;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "Pleieperiode")
@Table(name = "PB_PLEIEPERIODE")
public class EtablertPleieperiode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PB_PLEIEPERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @Column(name = "pleiegrad", nullable = false)
    @Convert(converter = PleiegradTypeConverter.class)
    private Pleiegrad grad;

    @ManyToOne
    @JoinColumn(name = "pleieperioder_id", nullable = false, updatable = false, unique = true)
    private EtablertPleieperioder pleieperioder;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    EtablertPleieperiode() {
    }

    public EtablertPleieperiode(DatoIntervallEntitet periode, Pleiegrad grad) {
        this.periode = periode;
        this.grad = grad;
    }

    EtablertPleieperiode(EtablertPleieperiode kontinuerligTilsynPeriode) {
        this.periode = kontinuerligTilsynPeriode.periode;
        this.grad = kontinuerligTilsynPeriode.grad;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setPleieperioder(EtablertPleieperioder pleieperioder) {
        this.pleieperioder = pleieperioder;
    }

    public Pleiegrad getGrad() {
        return grad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EtablertPleieperiode that = (EtablertPleieperiode) o;
        return grad == that.grad &&
            Objects.equals(periode, that.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, grad);
    }

    @Override
    public String toString() {
        return "Pleieperiode{" +
            "id=" + id +
            ", periode=" + periode +
            ", grad=" + grad +
            ", versjon=" + versjon +
            '}';
    }

}
