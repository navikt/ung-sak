package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjonPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjonRepository;

@ApplicationScoped
public class GodkjentOpplæringsinstitusjonTjeneste {

    private GodkjentOpplæringsinstitusjonRepository repository;

    public GodkjentOpplæringsinstitusjonTjeneste() {
    }

    @Inject
    public GodkjentOpplæringsinstitusjonTjeneste(GodkjentOpplæringsinstitusjonRepository repository) {
        this.repository = repository;
    }

    public Optional<GodkjentOpplæringsinstitusjon> hentMedUuid(UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }

        return repository.hentMedUuid(uuid);
    }

    public Optional<GodkjentOpplæringsinstitusjon> hentAktivMedUuid(UUID uuid, Periode aktivPeriode) {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(aktivPeriode);

        Optional<GodkjentOpplæringsinstitusjon> institusjon = repository.hentMedUuid(uuid);
        if (institusjon.isPresent() && erAktiv(institusjon.get(), aktivPeriode)) {
            return institusjon;
        }
        return Optional.empty();
    }

    public Optional<GodkjentOpplæringsinstitusjon> hentAktivMedUuid(UUID uuid, LocalDateTimeline<?> aktivTidslinje) {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(aktivTidslinje);

        Optional<GodkjentOpplæringsinstitusjon> institusjon = repository.hentMedUuid(uuid);
        if (institusjon.isPresent() && erAktiv(institusjon.get(), aktivTidslinje)) {
            return institusjon;
        }
        return Optional.empty();
    }

    public List<GodkjentOpplæringsinstitusjon> hentAlle() {
        return repository.hentAlle();
    }

    public List<GodkjentOpplæringsinstitusjon> hentAktive(Periode aktivPeriode) {
        return repository.hentAlle().stream()
            .filter(institusjon -> erAktiv(institusjon, aktivPeriode))
            .collect(Collectors.toList());
    }

    private boolean erAktiv(GodkjentOpplæringsinstitusjon godkjentOpplæringsInstitusjon, Periode periode) {
        var tidslinje = new LocalDateTimeline<>(periode.getFom(), periode.getTom(), true);
        return erAktiv(godkjentOpplæringsInstitusjon, tidslinje);
    }

    private boolean erAktiv(GodkjentOpplæringsinstitusjon godkjentOpplæringsInstitusjon, LocalDateTimeline<?> tidslinje) {
        var aktivTidslinje = godkjentOpplæringsInstitusjon.getTidslinje();
        return tidslinje.disjoint(aktivTidslinje).isEmpty();
    }
}
