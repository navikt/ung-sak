package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregat for søknadbaserte bostedsopplysninger per behandling.
 * Uavhengig av vilkårsperioder – lagres additivt uten deaktivering.
 */
@Entity(name = "BostedsinformasjonFraSøknadHolder")
@Table(name = "BOSATT_SOEKNAD_GRUNNLAG")
public class BostedsinformasjonFraSøknadHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSATT_SOEKNAD_GRUNNLAG")
    private Long id;

    @BatchSize(size = 20)
    @JoinColumn(name = "bosatt_soeknad_grunnlag_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BostedsinformasjonFraSøknad> informasjon = new LinkedHashSet<>();

    public BostedsinformasjonFraSøknadHolder() {
    }

    public BostedsinformasjonFraSøknadHolder(BostedsinformasjonFraSøknadHolder oppgittFraSøknad) {
        if (oppgittFraSøknad != null) {
            this.informasjon.addAll(oppgittFraSøknad.getInformasjon());
        }
    }

    void leggTilInformasjon(BostedsinformasjonFraSøknad info) {
        informasjon.removeIf(i -> i.getJournalpostId().equals(info.getJournalpostId()));
        informasjon.add(info);
    }

    public Long getId() {
        return id;
    }

    public Set<BostedsinformasjonFraSøknad> getInformasjon() {
        return Collections.unmodifiableSet(informasjon);
    }

    public Map<LocalDate, BostedsinformasjonFraSøknad> hentSomMap() {
        return informasjon.stream()
            .collect(Collectors.toMap(BostedsinformasjonFraSøknad::getFomDato, i -> i));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BostedsinformasjonFraSøknadHolder that)) return false;
        return Objects.equals(informasjon, that.informasjon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(informasjon);
    }
}
