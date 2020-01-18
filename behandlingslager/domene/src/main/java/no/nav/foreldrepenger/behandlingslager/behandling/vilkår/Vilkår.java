package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;

@Entity(name = "Vilkar")
@Table(name = "VILKAR")
public class Vilkår extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VILKAR")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vilkar_resultat_id", nullable = false, updatable = false)
    private VilkårResultat vilkårResultat;

    @Convert(converter = VilkårType.KodeverdiConverter.class)
    @Column(name = "vilkar_type", nullable = false, updatable = false)
    private VilkårType vilkårType;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "vilkår")
    private List<VilkårPeriode> perioder = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Vilkår() {
        // for hibernate og builder
    }

    Vilkår(Vilkår v) {
        this.vilkårType = v.vilkårType;
        this.perioder = v.getPerioder().stream().map(VilkårPeriode::new).peek(vp -> vp.setVilkår(this)).collect(Collectors.toList());
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(getVilkårType());
    }

    public Long getId() {
        return id;
    }

    public VilkårType getVilkårType() {
        return vilkårType;
    }

    void setVilkårType(VilkårType vilkårType) {
        this.vilkårType = vilkårType;
    }

    void setVilkårResultat(VilkårResultat vilkårResultat) {
        this.vilkårResultat = vilkårResultat;
    }

    public List<VilkårPeriode> getPerioder() {
        return Collections.unmodifiableList(perioder);
    }

    void setPerioder(List<VilkårPeriode> perioder) {
        this.perioder = perioder
            .stream()
            .peek(it -> it.setVilkår(this))
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "Vilkår{" +
            "vilkårType=" + vilkårType +
            ", perioder=" + perioder +
            '}';
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Vilkår)) {
            return false;
        }
        Vilkår other = (Vilkår) object;
        return Objects.equals(getVilkårType(), other.getVilkårType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVilkårType());
    }
}
