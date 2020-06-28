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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

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

    @ManyToOne(optional = false)
    @JoinColumn(name = "vilkar_resultat_id", nullable = false, updatable = false)
    private Vilkårene vilkårene;

    @Convert(converter = VilkårTypeKodeverdiConverter.class)
    @Column(name = "vilkar_type", nullable = false, updatable = false)
    private VilkårType vilkårType;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE}, mappedBy = "vilkår")
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
        Object[] keyParts = {getVilkårType()};
        return IndexKeyComposer.createKey(keyParts);
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

    void setVilkårene(Vilkårene vilkårene) {
        this.vilkårene = vilkårene;
    }

    public List<VilkårPeriode> getPerioder() {
        return Collections.unmodifiableList(perioder);
    }

    void setPerioder(List<VilkårPeriode> perioder) {
        this.perioder.clear();
        this.perioder.addAll(perioder
            .stream()
            .peek(it -> it.setVilkår(this))
            .collect(Collectors.toList()));
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
