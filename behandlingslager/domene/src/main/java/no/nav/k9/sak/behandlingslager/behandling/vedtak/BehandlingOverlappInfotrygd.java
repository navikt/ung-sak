package no.nav.k9.sak.behandlingslager.behandling.vedtak;

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
import no.nav.k9.sak.domene.typer.tid.ÅpenDatoIntervallEntitet;
import no.nav.k9.sak.typer.Saksnummer;

@Entity(name = "BehandlingOverlappInfotrygd")
@Table(name = "BEHANDLING_OVERLAPP_INFOTRYGD")
public class BehandlingOverlappInfotrygd extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEH_OVERLAPP_INFOTRYGD")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "saksnummer", column = @Column(name = "saksnummer")))
    private Saksnummer saksnummer;

    @Column(name = "BEHANDLING_ID", nullable = false, updatable = false)
    private Long behandlingId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "VL_FOM")),
        @AttributeOverride(name = "tomDato", column = @Column(name = "VL_TOM"))
    })
    private ÅpenDatoIntervallEntitet periodeVL;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "INFOTRYGD_FOM")),
        @AttributeOverride(name = "tomDato", column = @Column(name = "INFOTRYGD_TOM"))
    })
    private ÅpenDatoIntervallEntitet periodeInfotrygd;

    protected BehandlingOverlappInfotrygd() {
    }

    public Long getId() {
        return id;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public ÅpenDatoIntervallEntitet getPeriodeVL() {
        return periodeVL;
    }

    public ÅpenDatoIntervallEntitet getPeriodeInfotrygd() {
        return periodeInfotrygd;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof BehandlingOverlappInfotrygd)) {
            return false;
        }
        BehandlingOverlappInfotrygd that = (BehandlingOverlappInfotrygd) object;
        return Objects.equals(saksnummer, that.saksnummer) &&
            Objects.equals(behandlingId, that.behandlingId) &&
            Objects.equals(periodeVL, that.periodeVL) &&
            Objects.equals(periodeInfotrygd, that.periodeInfotrygd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saksnummer, behandlingId, periodeVL, periodeInfotrygd);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Saksnummer saksnummer;
        private Long behandlingId;
        private ÅpenDatoIntervallEntitet periodeVL;
        private ÅpenDatoIntervallEntitet periodeInfotrygd;

        public Builder medSaksnummer(Saksnummer saksnummer) {
            this.saksnummer = saksnummer;
            return this;
        }

        public Builder medBehandlingId(Long behandlingId) {
            this.behandlingId = behandlingId;
            return this;
        }

        public Builder medPeriodeVL(ÅpenDatoIntervallEntitet periodeVL) {
            this.periodeVL = periodeVL;
            return this;
        }

        public Builder medPeriodeInfotrygd(ÅpenDatoIntervallEntitet periodeInfotrygd) {
            this.periodeInfotrygd = periodeInfotrygd;
            return this;
        }

        public BehandlingOverlappInfotrygd build() {
            verifyStateForBuild();
            BehandlingOverlappInfotrygd behandlingOverlappInfotrygd = new BehandlingOverlappInfotrygd();
            behandlingOverlappInfotrygd.saksnummer = saksnummer;
            behandlingOverlappInfotrygd.behandlingId = behandlingId;
            behandlingOverlappInfotrygd.periodeVL = periodeVL;
            behandlingOverlappInfotrygd.periodeInfotrygd = periodeInfotrygd;
            return behandlingOverlappInfotrygd;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(saksnummer);
            Objects.requireNonNull(behandlingId);
            Objects.requireNonNull(periodeVL);
            Objects.requireNonNull(periodeInfotrygd);
            Objects.requireNonNull(saksnummer);
        }
    }
}
