package no.nav.foreldrepenger.behandlingslager.behandling.søknad;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity(name = "SøknadGrunnlag")
@Table(name = "GR_SOEKNAD")
class SøknadGrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_SOEKNAD")
    private Long id;

    @OneToOne
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Behandling behandling;

    @OneToOne
    @JoinColumn(name = "soeknad_id", nullable = false, updatable = false, unique = true)
    private SøknadEntitet søknad;

    
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    SøknadGrunnlagEntitet() {
    }

    SøknadGrunnlagEntitet(Behandling behandling, SøknadEntitet søknad) {
        this.behandling = behandling;
        this.søknad = søknad; // NOSONAR
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public SøknadEntitet getSøknad() {
        return søknad;
    }
}
