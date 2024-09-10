package no.nav.k9.sak.behandlingslager.behandling.merknad;

import java.util.EnumSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity(name = "BehandlingMerknad")
@Table(name = "BEHANDLING_MERKNAD")
public class BehandlingMerknadEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING_MERKNAD")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "fritekst")
    private String fritekst;

    @Column(name = "hastesak", nullable = false)
    private boolean hastesak = false;

    BehandlingMerknadEntitet() {
    }

    public BehandlingMerknadEntitet(Long behandlingId, Set<BehandlingMerknadType> merknadTyper, String fritekst) {
        this.behandlingId = behandlingId;
        this.hastesak = merknadTyper.contains(BehandlingMerknadType.HASTESAK);
        this.fritekst = fritekst;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public Set<BehandlingMerknadType> getMerknadTyper() {
        Set<BehandlingMerknadType> merknadTyper = EnumSet.noneOf(BehandlingMerknadType.class);
        if (hastesak){
            merknadTyper.add(BehandlingMerknadType.HASTESAK);
        }
        return merknadTyper;
    }

    public String getFritekst() {
        return fritekst;
    }

    public void deaktiver(){
        aktiv = false;
    }
}
