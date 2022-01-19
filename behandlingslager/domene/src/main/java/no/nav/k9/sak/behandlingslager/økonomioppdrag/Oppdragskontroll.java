package no.nav.k9.sak.behandlingslager.økonomioppdrag;

import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.typer.Saksnummer;

@Entity(name = "Oppdragskontroll")
@Table(name = "OPPDRAG_KONTROLL")
public class Oppdragskontroll extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OPPDRAG_KONTROLL")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    /**
     * Offisielt tildelt saksnummer fra GSAK.
     */
    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "saksnummer", column = @Column(name = "saksnummer", unique = true, nullable = false, updatable = false)))
    private Saksnummer saksnummer;

    
    @Column(name = "venter_kvittering", nullable = false)
    private Boolean venterKvittering = Boolean.TRUE;

    @Column(name = "prosess_task_id", nullable = false)
    private Long prosessTaskId;

    public Oppdragskontroll() {
        // default constructor
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public Boolean getVenterKvittering() {
        return venterKvittering;
    }

    public void setVenterKvittering(Boolean venterKvittering) {
        this.venterKvittering = venterKvittering;
    }

    public Long getProsessTaskId() {
        return prosessTaskId;
    }

    public long getVersjon() {
        return versjon;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Oppdragskontroll)) {
            return false;
        }
        Oppdragskontroll oppdragskontroll = (Oppdragskontroll) object;
        return Objects.equals(behandlingId, oppdragskontroll.getBehandlingId())
            && Objects.equals(saksnummer, oppdragskontroll.getSaksnummer())
            && Objects.equals(venterKvittering, oppdragskontroll.getVenterKvittering())
            && Objects.equals(prosessTaskId, oppdragskontroll.getProsessTaskId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, saksnummer, venterKvittering, prosessTaskId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
            (id != null ? "id=" + id + ", " : "") //$NON-NLS-1$ //$NON-NLS-2$
            + "behandlingId=" + behandlingId + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "saksnummer=" + saksnummer + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "venterKvittering=" + venterKvittering + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "prosessTaskId=" + prosessTaskId + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "opprettetTs=" + getOpprettetTidspunkt() //$NON-NLS-1$
            + ">"; //$NON-NLS-1$
    }
}
