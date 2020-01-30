package no.nav.foreldrepenger.inngangsvilkaar.perioder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.Fordeling;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingRepository;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
public class PerioderTilVurderingTjeneste {

    private FordelingRepository fordelingRepository;

    PerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public PerioderTilVurderingTjeneste(FordelingRepository fordelingRepository) {
        this.fordelingRepository = fordelingRepository;
    }

    public Set<DatoIntervallEntitet> utled(Long behandlingId) {
        final var fordeling = fordelingRepository.hentHvisEksisterer(behandlingId);

        if (fordeling.isEmpty()) {
            return Set.of();
        } else {
            final var perioder = fordeling.map(Fordeling::getPerioder).orElse(Collections.emptySet());
            final var fom = perioder.stream()
                .map(FordelingPeriode::getPeriode)
                .map(DatoIntervallEntitet::getFomDato)
                .min(LocalDate::compareTo)
                .orElseThrow();
            final var tom = perioder.stream()
                .map(FordelingPeriode::getPeriode)
                .map(DatoIntervallEntitet::getTomDato)
                .max(LocalDate::compareTo)
                .orElseThrow();

            return Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        }
    }
}
