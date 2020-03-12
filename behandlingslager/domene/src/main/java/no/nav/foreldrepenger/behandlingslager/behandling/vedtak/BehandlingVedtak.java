package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.kodeverk.IverksettingStatusKodeverdiConverter;
import no.nav.k9.kodeverk.vedtak.IverksettingStatus;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;

@Entity(name = "BehandlingVedtak")
@Table(name = "BEHANDLING_VEDTAK")
public class BehandlingVedtak extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING_VEDTAK")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "VEDTAK_DATO", nullable = false)
    private LocalDateTime vedtakstidspunkt;

    @Column(name = "ANSVARLIG_SAKSBEHANDLER", nullable = false)
    private String ansvarligSaksbehandler;

    @Convert(converter = VedtakResultatTypeKodeverdiConverter.class)
    @Column(name = "vedtak_resultat_type", nullable = false)
    private VedtakResultatType vedtakResultatType = VedtakResultatType.UDEFINERT;

    @Column(name = "behandling_id", updatable = false, nullable = false, unique =true)
    private Long behandlingId;

    /**
     * Hvorvidt vedtaket er et "beslutningsvedtak". Et beslutningsvedtak er et vedtak med samme utfall som forrige vedtak.
     *
     * @see https://jira.adeo.no/browse/BEGREP-2012
     */
    @Column(name = "BESLUTNING", nullable = false)
    private boolean beslutningsvedtak;

    @Convert(converter = IverksettingStatusKodeverdiConverter.class)
    @Column(name = "iverksetting_status", nullable = false)
    private IverksettingStatus iverksettingStatus = IverksettingStatus.UDEFINERT;

    private BehandlingVedtak() {
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getVedtaksdato() {
        return vedtakstidspunkt.toLocalDate();
    }

    public LocalDateTime getVedtakstidspunkt() {
        return vedtakstidspunkt;
    }

    public String getAnsvarligSaksbehandler() {
        return ansvarligSaksbehandler;
    }

    public VedtakResultatType getVedtakResultatType() {
        return Objects.equals(VedtakResultatType.UDEFINERT, vedtakResultatType) ? null : vedtakResultatType;
    }

    public Boolean isBeslutningsvedtak() {
        return beslutningsvedtak;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof BehandlingVedtak)) {
            return false;
        }
        BehandlingVedtak vedtak = (BehandlingVedtak) object;
        return Objects.equals(vedtakstidspunkt, vedtak.getVedtakstidspunkt())
            && Objects.equals(ansvarligSaksbehandler, vedtak.getAnsvarligSaksbehandler())
            && Objects.equals(getVedtakResultatType(), vedtak.getVedtakResultatType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(vedtakstidspunkt, ansvarligSaksbehandler, getVedtakResultatType());
    }

    public IverksettingStatus getIverksettingStatus() {
        return Objects.equals(IverksettingStatus.UDEFINERT, iverksettingStatus) ? null : iverksettingStatus;
    }

    public void setIverksettingStatus(IverksettingStatus iverksettingStatus) {
        this.iverksettingStatus = iverksettingStatus == null ? IverksettingStatus.UDEFINERT : iverksettingStatus;
    }

    public static class Builder {
        private LocalDateTime vedtakstidspunkt = LocalDateTime.now();
        private String ansvarligSaksbehandler;
        private VedtakResultatType vedtakResultatType = VedtakResultatType.INNVILGET;
        private IverksettingStatus iverksettingStatus = IverksettingStatus.IKKE_IVERKSATT;
        private boolean beslutning = false;
        private Long behandlingId;

        public Builder(Long behandlingId) {
            this.behandlingId = behandlingId;
        }

        public Builder medVedtakstidspunkt(LocalDateTime vedtakstidspunkt) {
            this.vedtakstidspunkt = vedtakstidspunkt;
            return this;
        }

        public Builder medAnsvarligSaksbehandler(String ansvarligSaksbehandler) {
            this.ansvarligSaksbehandler = ansvarligSaksbehandler;
            return this;
        }

        public Builder medVedtakResultatType(VedtakResultatType vedtakResultatType) {
            this.vedtakResultatType = vedtakResultatType;
            return this;
        }

        public Builder medIverksettingStatus(IverksettingStatus iverksettingStatus) {
            this.iverksettingStatus = iverksettingStatus;
            return this;
        }

        public Builder medBeslutning(boolean beslutning) {
            this.beslutning = beslutning;
            return this;
        }
        
        public Builder medBehandling(Long behandlingId) {
            this.behandlingId = behandlingId;
            return this;
        }

        public BehandlingVedtak build() {
            verifyStateForBuild();
            BehandlingVedtak vedtak = new BehandlingVedtak();
            vedtak.behandlingId = Objects.requireNonNull(behandlingId, "behandlingId");
            vedtak.vedtakstidspunkt = Objects.requireNonNull(vedtakstidspunkt, "vedtakstidspunkt");
            vedtak.ansvarligSaksbehandler = ansvarligSaksbehandler;
            vedtak.vedtakResultatType = vedtakResultatType;
            vedtak.beslutningsvedtak = beslutning;
            vedtak.iverksettingStatus = Objects.requireNonNull(iverksettingStatus, "iverksettingStatus");
            return vedtak;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(vedtakstidspunkt, "vedtaksdato");
            Objects.requireNonNull(ansvarligSaksbehandler, "ansvarligSaksbehandler");
            Objects.requireNonNull(vedtakResultatType, "vedtakResultatType");
        }
    }

    public static Builder builder(Long behandlingId) {
        return new Builder(behandlingId);
    }
    
    public static Builder builder() {
        return new Builder(null);
    }

}
