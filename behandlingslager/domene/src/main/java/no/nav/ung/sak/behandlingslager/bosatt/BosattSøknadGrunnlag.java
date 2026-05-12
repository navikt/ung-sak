package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Aggregat for søknadbaserte bostedsopplysninger per behandling.
 * Uavhengig av vilkårsperioder – lagres additivt uten deaktivering.
 */
@Entity(name = "BosattSøknadGrunnlag")
@Table(name = "BOSATT_SOEKNAD_GRUNNLAG")
public class BosattSøknadGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSATT_SOEKNAD_GRUNNLAG")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @BatchSize(size = 20)
    @JoinColumn(name = "bosatt_soeknad_grunnlag_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BostedsinformasjonFraSøknad> informasjon = new LinkedHashSet<>();

    public BosattSøknadGrunnlag() {
        // Hibernate
    }

    public BosattSøknadGrunnlag(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        this.behandlingId = behandlingId;
    }

    void leggTilInformasjon(BostedsinformasjonFraSøknad info) {
        informasjon.removeIf(i -> i.getJournalpostId().equals(info.getJournalpostId()));
        informasjon.add(info);
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Set<BostedsinformasjonFraSøknad> getInformasjon() {
        return Collections.unmodifiableSet(informasjon);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BosattSøknadGrunnlag that)) return false;
        return Objects.equals(behandlingId, that.behandlingId)
            && Objects.equals(informasjon, that.informasjon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, informasjon);
    }
}
