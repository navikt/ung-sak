package no.nav.k9.sak.behandlingslager.behandling.søknad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "SøknadGrunnlag")
@Table(name = "GR_SOEKNAD")
class SøknadGrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_SOEKNAD")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

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

    SøknadGrunnlagEntitet(Long behandlingId, SøknadEntitet søknad) {
        this.behandlingId = behandlingId;
        this.søknad = søknad; // NOSONAR
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public SøknadEntitet getSøknad() {
        return søknad;
    }
}
