package no.nav.ung.sak.behandlingslager.behandling;

import java.util.Objects;

import org.hibernate.annotations.FilterDef;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.behandling.BehandlingStegStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.behandlingslager.kodeverk.BehandlingStegStatusKodeverdiConverter;
import no.nav.ung.sak.behandlingslager.kodeverk.BehandlingStegTypeKodeverdiConverter;

@Entity(name = "BehandlingStegTilstand")
@Table(name = "BEHANDLING_STEG_TILSTAND")
@FilterDef(
    name="kunAktiveBehandlingTilstander"
)
public class BehandlingStegTilstand extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING_STEG_TILSTAND")
    private Long id;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Convert(converter = BehandlingStegTypeKodeverdiConverter.class)
    @Column(name = "behandling_steg", nullable = false, updatable = false)
    private BehandlingStegType behandlingSteg;

    @Convert(converter = BehandlingStegStatusKodeverdiConverter.class)
    @Column(name = "behandling_steg_status", nullable = false)
    private BehandlingStegStatus behandlingStegStatus = BehandlingStegStatus.UDEFINERT;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public BehandlingStegTilstand(BehandlingStegType behandlingSteg) {
        this(behandlingSteg, BehandlingStegStatus.UDEFINERT);
    }

    public BehandlingStegTilstand(BehandlingStegType behandlingSteg, BehandlingStegStatus stegStatus) {
        this.behandlingSteg = behandlingSteg;
        this.setBehandlingStegStatus(stegStatus);
    }

    BehandlingStegTilstand() {
        // for hibernate
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BehandlingStegTilstand)) {
            return false;
        }
        BehandlingStegTilstand that = (BehandlingStegTilstand) o;
        return Objects.equals(getBehandlingSteg(), that.getBehandlingSteg()) &&
            Objects.equals(getBehandlingStegStatus(), that.getBehandlingStegStatus());
    }

    public BehandlingStegType getBehandlingSteg() {
        return behandlingSteg;
    }

    public BehandlingStegStatus getBehandlingStegStatus() {
        return Objects.equals(BehandlingStegStatus.UDEFINERT, behandlingStegStatus) ? null : behandlingStegStatus;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { behandlingSteg, behandlingStegStatus };
        return IndexKeyComposer.createKey(keyParts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBehandlingSteg(), getBehandlingStegStatus());
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public void deaktiver() {
        this.aktiv = false;  // kan kun endre fra true til false
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<steg=" + getBehandlingSteg()
            + ", stegStatus=" + getBehandlingStegStatus()
            + ", aktiv=" + aktiv
            + ">";
    }

    /**
     * Set BehandlingStegStatus direkte. Kun for invortes bruk.
     *
     * @param behandlingStegStatus - ny status
     */
    void setBehandlingStegStatus(BehandlingStegStatus behandlingStegStatus) {
        this.behandlingStegStatus = behandlingStegStatus == null ? BehandlingStegStatus.UDEFINERT : behandlingStegStatus;
    }
}
