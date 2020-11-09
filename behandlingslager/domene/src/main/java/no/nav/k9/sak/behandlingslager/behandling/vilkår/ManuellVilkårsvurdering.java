package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import static java.util.Objects.requireNonNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "ManuellVilkårsvurdering")
@Table(name = "RS_MAN_VILKARSVURDERING")
public class ManuellVilkårsvurdering extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RS_MAN_VILKARSVURDERING")
    private Long id;

    @Column(name = "fritekst", nullable = false)
    private String fritekst;

    protected ManuellVilkårsvurdering() {
    }

    public ManuellVilkårsvurdering(String fritekst) {
        this.fritekst = requireNonNull(fritekst, "fritekst er påkrevd, men var null");
    }

    public String getFritekst() {
        return fritekst;
    }

    @Override
    public String toString() {
        return "VilkårsvurderingFritekst{" +
            "id=" + id +
            ", fritekst='" + fritekst + '\'' +
            '}';
    }
}
