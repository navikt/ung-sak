package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import java.time.LocalDate;
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
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.BatchSize;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;

@Entity(name = "Vilkar")
@Table(name = "VR_VILKAR")
public class Vilkår extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VILKAR")
    private Long id;

    @Convert(converter = VilkårTypeKodeverdiConverter.class)
    @Column(name = "vilkar_type", nullable = false, updatable = false)
    private VilkårType vilkårType;

    @OneToMany(cascade = { CascadeType.ALL }, orphanRemoval = true)
    @JoinColumn(name = "vilkar_id", nullable = false)
    @OrderBy(value = "periode.fomDato asc nulls first")
    @BatchSize(size = 20)
    private List<VilkårPeriode> perioder = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    /**
     * Merk - denne er kun for builder - bygger en ugyldig Vilkår (uten vilkårtype).
     * 
     * @deprecated bør ikke brukes av builder og gjøres private
     */
    @Deprecated
    Vilkår() {
        // for hibernate og builder
    }

    Vilkår(Vilkår v) {
        this(v.getVilkårType());
        this.perioder = v.getPerioder().stream().map(VilkårPeriode::new).collect(Collectors.toList());
    }

    Vilkår(VilkårType vilkårType) {
        this.vilkårType = Objects.requireNonNull(vilkårType, "vilkårType");
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { getVilkårType() };
        return IndexKeyComposer.createKey(keyParts);
    }

    public Long getId() {
        return id;
    }

    public VilkårType getVilkårType() {
        return vilkårType;
    }

    void setVilkårType(VilkårType vilkårType) {
        this.vilkårType = Objects.requireNonNull(vilkårType, "vilkårType");
    }

    public List<VilkårPeriode> getPerioder() {
        return Collections.unmodifiableList(perioder);
    }

    void setPerioder(List<VilkårPeriode> perioder) {
        this.perioder.clear();
        this.perioder.addAll(perioder.stream().map(VilkårPeriode::new).collect(Collectors.toList()));
    }

    public VilkårPeriode finnPeriodeForSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        return this.perioder.stream().filter(it -> it.getSkjæringstidspunkt().equals(skjæringstidspunkt)).findFirst()
            .orElseThrow(() -> new IllegalStateException("Fant ikke vilkårsperiode for skjæringstidspunkt " + skjæringstidspunkt));
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
