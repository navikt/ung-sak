package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.aarskvantum.kontrakter.AleneOmOmsorgen;
import no.nav.k9.aarskvantum.kontrakter.MidlertidigAleneOmOmsorgen;
import no.nav.k9.aarskvantum.kontrakter.Rammevedtak;
import no.nav.k9.aarskvantum.kontrakter.UtvidetRett;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

@Dependent
public class VedtatteRammevedtakTjeneste {

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private AktørTjeneste aktørTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    public VedtatteRammevedtakTjeneste(BehandlingRepository behandlingRepository, FagsakRepository fagsakRepository, VilkårResultatRepository vilkårResultatRepository, AktørTjeneste aktørTjeneste, BehandlingVedtakRepository behandlingVedtakRepository) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.aktørTjeneste = aktørTjeneste;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
    }

    public InnvilgedeOgAvslåtteRammevedtak hentK9sakRammevedtak(AktørId søkerAktørId) {
        List<Fagsak> fagsaker = fagsakRepository.hentForBruker(søkerAktørId)
            .stream().filter(fagsak -> fagsak.getYtelseType().erRammevedtak())
            .toList();

        List<Behandling> behandlingerMedVedtak = fagsaker.stream()
            .flatMap(fagsak -> behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId()).stream())
            .filter(Behandling::erAvsluttet)
            .filter(behandling -> !behandling.erHenlagt())
            .sorted(Comparator.comparing(Behandling::getAvsluttetDato))
            .toList();

        List<Rammevedtak> innvilgede = new ArrayList<>();
        List<Rammevedtak> avslåtte = new ArrayList<>();
        for (Behandling behandling : behandlingerMedVedtak) {
            BehandlingVedtak vedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId()).orElseThrow(() -> new IllegalArgumentException("Fant ikke behandling vedtak på behandlingen " + behandling.getId()));
            for (LocalDateSegment<Utfall> resultatperiode : hentUtfall(behandling.getId()).compress()) {
                Rammevedtak v = mapTilÅrskvantumKontrakt(vedtak.getVedtakstidspunkt(), behandling.getFagsak(), resultatperiode.getFom(), resultatperiode.getTom());
                Utfall utfallForPeriode = resultatperiode.getValue();
                if (utfallForPeriode == Utfall.OPPFYLT) {
                    innvilgede.add(v);
                } else if (utfallForPeriode == Utfall.IKKE_OPPFYLT) {
                    avslåtte.add(v);
                } else {
                    throw new IllegalArgumentException("Ikke-støttet utfall: " + utfallForPeriode);
                }
            }
        }
        return new InnvilgedeOgAvslåtteRammevedtak(innvilgede, avslåtte);
    }

    public record InnvilgedeOgAvslåtteRammevedtak(List<Rammevedtak> innvilgede, List<Rammevedtak> avslåtte) {
    }


    private Rammevedtak mapTilÅrskvantumKontrakt(LocalDateTime vedtakTidspunkt, Fagsak fagsak, LocalDate fom, LocalDate tom) {
        return switch (fagsak.getYtelseType()) {
            case OMSORGSPENGER_KS ->
                new UtvidetRett(vedtakTidspunkt.toLocalDate(), Duration.ZERO, fom, tom, fnrForBarnet(fagsak));
            case OMSORGSPENGER_MA ->
                new MidlertidigAleneOmOmsorgen(vedtakTidspunkt.toLocalDate(), Duration.ZERO, fom, tom);
            case OMSORGSPENGER_AO ->
                new AleneOmOmsorgen(vedtakTidspunkt.toLocalDate(), Duration.ZERO, fom, tom, fnrForBarnet(fagsak));
            default -> throw new IllegalArgumentException("Ikke-støttet ytelsetype: " + fagsak.getYtelseType());
        };
    }

    private String fnrForBarnet(Fagsak fagsak) {
        return aktørTjeneste.hentPersonIdentForAktørId(fagsak.getPleietrengendeAktørId()).orElseThrow(() -> new IllegalArgumentException("Kan ikke finne FNR for barn")).getIdent();
    }

    private LocalDateTimeline<Utfall> hentUtfall(Long behandlingId) {
        Map<VilkårType, LocalDateTimeline<VilkårPeriode>> vilkårTidslinjer = vilkårResultatRepository.hent(behandlingId).getVilkårTidslinjer(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.MIN, LocalDate.MAX));
        LocalDateTimeline<Boolean> tidlinjeNoeInnvilget = harVilkårMedUtfall(VilkårType.UTVIDETRETT, vilkårTidslinjer, Utfall.OPPFYLT);
        LocalDateTimeline<Boolean> tidlinjeNoeAvslått = harMinstEtVilkårMedUtfall(vilkårTidslinjer, Utfall.IKKE_OPPFYLT);
        //samlet resultat er OPPFYLT hvis minst ett vilkår er OPPFYLT og ingen vilkår er IKKE_OPPFYLT.
        return tidlinjeNoeInnvilget.mapValue(v -> Utfall.OPPFYLT).crossJoin(tidlinjeNoeAvslått.mapValue(v -> Utfall.IKKE_OPPFYLT), StandardCombinators::coalesceRightHandSide);
    }

    private LocalDateTimeline<Boolean> harMinstEtVilkårMedUtfall(Map<VilkårType, LocalDateTimeline<VilkårPeriode>> vilkårTidslinjer, Utfall ønsketUtfall) {
        return vilkårTidslinjer.values().stream()
            .map(tidslinje -> tidslinje.filterValue(v -> v.getUtfall() == ønsketUtfall))
            .map(tidslinje -> tidslinje.mapValue(v -> true))
            .reduce((a, b) -> a.crossJoin(b, StandardCombinators::alwaysTrueForMatch))
            .orElse(LocalDateTimeline.empty());
    }

    private LocalDateTimeline<Boolean> harVilkårMedUtfall(VilkårType aktueltVilkår, Map<VilkårType, LocalDateTimeline<VilkårPeriode>> vilkårTidslinjer, Utfall ønsketUtfall) {
        return vilkårTidslinjer.entrySet().stream()
            .filter(e->e.getKey() == aktueltVilkår)
            .map(Map.Entry::getValue)
            .map(tidslinje -> tidslinje.filterValue(v -> v.getUtfall() == ønsketUtfall))
            .map(tidslinje -> tidslinje.mapValue(v -> true))
            .reduce((a, b) -> a.crossJoin(b, StandardCombinators::alwaysTrueForMatch))
            .orElse(LocalDateTimeline.empty());
    }
}
