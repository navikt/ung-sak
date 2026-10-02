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
import no.nav.ung.sak.vilkår.VilkårTjeneste;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Utleder innvilgede perioder med aktivitetspenger for eksterne konsumenter (tilleggsstønader).
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
     * Returnerer innvilgede perioder som overlapper forespurt periode. Periodene kuttes ikke ved forespurt fom/tom.
     */
    public List<Periode> hentInnvilgedePerioder(Collection<AktørId> aktørIder, Periode forespurtPeriode) {
        Objects.requireNonNull(forespurtPeriode, "forespurtPeriode");
        var innvilget = aktørIder.stream()
            .flatMap(aktørId -> fagsakRepository.hentForBruker(aktørId, FagsakYtelseType.AKTIVITETSPENGER).stream())
            .map(this::innvilgetTidslinjeForGjeldendeVedtak)
            .reduce(LocalDateTimeline.<Boolean>empty(), (a, b) -> a.union(b, StandardCombinators::coalesceLeftHandSide))
            .compress();

        var forespurt = new LocalDateInterval(forespurtPeriode.getFom(), forespurtPeriode.getTom());
        return innvilget.getLocalDateIntervals().stream()
            .filter(interval -> interval.overlaps(forespurt))
            .map(interval -> new Periode(interval.getFomDato(), interval.getTomDato()))
            .toList();
    }

    private LocalDateTimeline<Boolean> innvilgetTidslinjeForGjeldendeVedtak(Fagsak fagsak) {
        return behandlingRepository.finnSisteAvsluttedeIkkeHenlagteYtelsebehandling(fagsak.getId())
            .map(behandling -> vilkårTjeneste.samletVilkårsresultat(behandling.getId())
                .filterValue(utfall -> utfall.getSamletUtfall() == Utfall.OPPFYLT)
                .mapValue(utfall -> Boolean.TRUE))
            .orElse(LocalDateTimeline.empty());
    }
}
