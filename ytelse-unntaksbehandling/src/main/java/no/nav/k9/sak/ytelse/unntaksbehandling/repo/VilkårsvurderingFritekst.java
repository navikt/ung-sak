package no.nav.k9.sak.ytelse.unntaksbehandling.repo;

import static java.util.Objects.requireNonNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "ManuellVilkårsvurderingFritekst")
@Table(name = "MAN_VV_FRITEKST")
public class VilkårsvurderingFritekst extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MAN_VV_FRITEKST")
    private Long id;

    @Column(name = "fritekst", nullable = false)
    private String fritekst;

    protected VilkårsvurderingFritekst() {
    }

    public VilkårsvurderingFritekst(String fritekst) {
        requireNonNull(fritekst, "fritekst kan ikke være null");
        this.fritekst = fritekst;
    }

    public String getFritekst() {
        return fritekst;
    }

}
