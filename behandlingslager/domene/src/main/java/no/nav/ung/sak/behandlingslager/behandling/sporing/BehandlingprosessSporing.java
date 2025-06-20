package no.nav.ung.sak.behandlingslager.behandling.sporing;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

@Entity(name = "BehandlingprosessSporing")
@Table(name = "BEHANDLINGPROSESS_SPORING")
public class BehandlingprosessSporing extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLINGPROSESS_SPORING")
    private Long id;

    @Column(name = "behandling_id", updatable = false, nullable = false)
    private Long behandlingId;


    @Column(name = "prosess_input", updatable = false, nullable = false)
    private String input;

    @Column(name = "prosess_resultat", updatable = false, nullable = false)
    private String resultat;

    @Column(name = "prosess_identifikator", updatable = false, nullable = false)
    private String prosessIdentifikator;


    public BehandlingprosessSporing(Long behandlingId, String input, String resultat, String prosessIdentifikator) {
        this.behandlingId = behandlingId;
        this.input = input;
        this.resultat = resultat;
        this.prosessIdentifikator = prosessIdentifikator;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public String getInput() {
        return input;
    }

    public String getResultat() {
        return resultat;
    }

    public String getProsessIdentifikator() {
        return prosessIdentifikator;
    }
}
