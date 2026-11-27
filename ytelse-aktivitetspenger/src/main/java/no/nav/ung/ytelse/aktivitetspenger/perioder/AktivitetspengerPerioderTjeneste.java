package no.nav.ung.ytelse.aktivitetspenger.perioder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.vilkår.VilkårTjeneste;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utleder innvilgede perioder med aktivitetspenger.
 * <p>
 * Grunnlaget er gjeldende vedtak per fagsak, dvs. siste avsluttede, ikke-henlagte ytelsesbehandling. Åpne behandlinger
 * tas ikke med. En periode regnes som innvilget når samlet vilkårsutfall er {@link Utfall#OPPFYLT}, uavhengig av dagsats.
 */
@ApplicationScoped
public class AktivitetspengerPerioderTjeneste {

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private VilkårTjeneste vilkårTjeneste;

    AktivitetspengerPerioderTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AktivitetspengerPerioderTjeneste(FagsakRepository fagsakRepository,
                                            BehandlingRepository behandlingRepository,
                                            VilkårTjeneste vilkårTjeneste) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.vilkårTjeneste = vilkårTjeneste;
    }

    /**
     * <pre>
     * Forespurt        |------------|
     * Innvilget   |---------|
     * Returnert   |---------|
     *
     * Forespurt                  |-----|
     * Innvilget   |---------|
     * Returnert   (ingen)
     * </pre>
     * Saksnumrene er fagsakene med innvilgede perioder i det som returneres, også når en fagsak bare er med fordi
     * perioden er slått sammen med en periode fra en annen fagsak.
     */
    public InnvilgedePerioder hentInnvilgedeOverlappendePerioder(Collection<AktørId> aktørIder, Periode forespurtPeriode) {
        Objects.requireNonNull(forespurtPeriode, "forespurtPeriode");
        Map<Saksnummer, LocalDateTimeline<Boolean>> innvilgetPerFagsak = aktørIder.stream()
            .flatMap(aktørId -> fagsakRepository.hentForBruker(aktørId, FagsakYtelseType.AKTIVITETSPENGER).stream())
            .collect(Collectors.toMap(Fagsak::getSaksnummer, this::innvilgetTidslinjeForGjeldendeVedtak, (a, b) -> a));

        var forespurt = new LocalDateInterval(forespurtPeriode.getFom(), forespurtPeriode.getTom());
        var returnert = innvilgetPerFagsak.values().stream()
            .reduce(LocalDateTimeline.<Boolean>empty(), (a, b) -> a.union(b, StandardCombinators::coalesceLeftHandSide))
            .compress()
            .getLocalDateIntervals().stream()
            .filter(interval -> interval.overlaps(forespurt))
            .toList();

        var saksnumre = innvilgetPerFagsak.entrySet().stream()
            .filter(e -> e.getValue().getLocalDateIntervals().stream().anyMatch(i -> returnert.stream().anyMatch(i::overlaps)))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        var perioder = returnert.stream()
            .map(interval -> new Periode(interval.getFomDato(), interval.getTomDato()))
            .toList();
        return new InnvilgedePerioder(perioder, saksnumre);
    }

    private LocalDateTimeline<Boolean> innvilgetTidslinjeForGjeldendeVedtak(Fagsak fagsak) {
        return behandlingRepository.finnSisteAvsluttedeIkkeHenlagteYtelsebehandling(fagsak.getId())
            .map(behandling -> vilkårTjeneste.samletVilkårsresultat(behandling.getId())
                .filterValue(utfall -> utfall.getSamletUtfall() == Utfall.OPPFYLT)
                .mapValue(utfall -> Boolean.TRUE))
            .orElse(LocalDateTimeline.empty());
    }
}
