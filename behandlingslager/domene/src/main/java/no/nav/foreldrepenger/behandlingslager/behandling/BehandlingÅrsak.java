package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.kodeverk.BehandlingÅrsakKodeverdiConverter;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;

@Entity(name = "BehandlingÅrsak")
@Table(name = "BEHANDLING_ARSAK")
public class BehandlingÅrsak extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING_ARSAK")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ManyToOne(optional = false)
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false)
    private Behandling behandling;

    @Convert(converter = BehandlingÅrsakKodeverdiConverter.class)
    @Column(name="behandling_arsak_type", nullable = false)
    private BehandlingÅrsakType behandlingÅrsakType = BehandlingÅrsakType.UDEFINERT;

    @ManyToOne
    @JoinColumn(name = "original_behandling_id", updatable = false)
    private Behandling originalBehandling;

    
    @Column(name = "manuelt_opprettet", nullable = false)
    private boolean manueltOpprettet = false;

    BehandlingÅrsak() {
        // for hibernate
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandling.getId();
    }

    public BehandlingÅrsakType getBehandlingÅrsakType() {
        return behandlingÅrsakType;
    }

    public Optional<Behandling> getOriginalBehandling() {
        return Optional.ofNullable(originalBehandling);
    }

    public boolean erManueltOpprettet() {
        return manueltOpprettet;
    }

    public static BehandlingÅrsak.Builder builder(BehandlingÅrsakType behandlingÅrsakType) {
        return new Builder(Arrays.asList(behandlingÅrsakType));
    }

    public static BehandlingÅrsak.Builder builder(List<BehandlingÅrsakType> behandlingÅrsakTyper) {
        return new Builder(behandlingÅrsakTyper);
    }

    void setBehandling(Behandling behandling) {
        this.behandling = behandling;
    }

    public static class Builder {

        private List<BehandlingÅrsakType> behandlingÅrsakTyper;
        private Behandling originalBehandling;
        private boolean manueltOpprettet;

        public Builder(List<BehandlingÅrsakType> behandlingÅrsakTyper) {
            Objects.requireNonNull(behandlingÅrsakTyper, "behandlingÅrsakTyper");
            this.behandlingÅrsakTyper = behandlingÅrsakTyper;
        }

        public Builder medOriginalBehandling(Behandling originalBehandling) {
            this.originalBehandling = originalBehandling;
            return this;
        }

        public Builder medManueltOpprettet(boolean manueltOpprettet) {
            this.manueltOpprettet = manueltOpprettet;
            return this;
        }

        public List<BehandlingÅrsak> buildFor(Behandling behandling) {
            Objects.requireNonNull(behandling, "behandling");
            List<BehandlingÅrsak> nyeÅrsaker = new ArrayList<>();
            for (BehandlingÅrsakType årsakType : this.behandlingÅrsakTyper) {
                // Tillater å oppdatere enkelte attributter. Kan derfor ikke bruke Hibernate + equals/hashcode til å håndtere insert vs update
                Optional<BehandlingÅrsak> eksisterende = behandling.getBehandlingÅrsaker().stream()
                    .filter(it -> it.getBehandlingÅrsakType().equals(årsakType))
                    .findFirst();
                if (eksisterende.isPresent()) {
                    // Oppdater eksisterende (UPDATE)
                    BehandlingÅrsak årsak = eksisterende.get();
                    if (this.originalBehandling != null) {
                        årsak.originalBehandling = this.originalBehandling;
                    }
                    årsak.manueltOpprettet = this.manueltOpprettet;
                } else {
                    // Opprett ny (INSERT)
                    BehandlingÅrsak behandlingÅrsak = new BehandlingÅrsak();
                    behandlingÅrsak.behandling = behandling;
                    behandlingÅrsak.behandlingÅrsakType = årsakType;
                    behandlingÅrsak.originalBehandling = this.originalBehandling;
                    behandlingÅrsak.manueltOpprettet = this.manueltOpprettet;
                    nyeÅrsaker.add(behandlingÅrsak);
                }
            }
            behandling.leggTilBehandlingÅrsaker(nyeÅrsaker);
            return behandling.getBehandlingÅrsaker();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BehandlingÅrsak that = (BehandlingÅrsak) o;

        return Objects.equals(behandlingÅrsakType, that.behandlingÅrsakType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingÅrsakType);
    }
}
