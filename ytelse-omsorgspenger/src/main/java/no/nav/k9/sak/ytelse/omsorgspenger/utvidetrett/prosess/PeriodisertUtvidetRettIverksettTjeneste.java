package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.time.LocalDate;
import java.util.Map;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Dependent
public class PeriodisertUtvidetRettIverksettTjeneste {

    private VilkårResultatRepository vilkårResultatRepository;
    private boolean brukPeriodisertRammevedtak;

    @Inject
    public PeriodisertUtvidetRettIverksettTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                                   @KonfigVerdi(value = "PERIODISERT_RAMMEVEDTAK", defaultVerdi = "false") boolean brukPeriodisertRammevedtak) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.brukPeriodisertRammevedtak = brukPeriodisertRammevedtak;
    }

    public LocalDateTimeline<Utfall> utfallSomErEndret(Behandling b) {
        if (!brukPeriodisertRammevedtak) {
            throw new IllegalStateException("Forventet ikke å komme her uten at PERIODISERT_RAMMEVEDTAK er lansert");
        }
        LocalDateTimeline<Utfall> nyttUtfall = hentUtfall(b.getId());
        LocalDateTimeline<Utfall> tidligereUtfall = b.getOriginalBehandlingId().map(this::hentUtfall).orElse(LocalDateTimeline.empty());
        return nyttUtfall.combine(tidligereUtfall, this::endretUtfall, LocalDateTimeline.JoinStyle.LEFT_JOIN).compress(); //LEFT_JOIN siden vi kun er interessert i perioder påvirket av inneværende behandling
    }

    private LocalDateSegment<Utfall> endretUtfall(LocalDateInterval intervall, LocalDateSegment<Utfall> nyttUtfall, LocalDateSegment<Utfall> gammeltUtfall) {
        Utfall nyVerdi = nyttUtfall.getValue();
        Utfall gammelVerdi = gammeltUtfall != null ? gammeltUtfall.getValue() : Utfall.IKKE_OPPFYLT; //ikke-innvilget i gammel tidslinje har samme effekt som IKKE_OPPFYLT
        return nyVerdi != gammelVerdi ? new LocalDateSegment<>(intervall, nyVerdi) : null;
    }

    private LocalDateTimeline<Utfall> hentUtfall(Long behandlingId) {
        Map<VilkårType, LocalDateTimeline<VilkårPeriode>> vilkårTidslinjer = vilkårResultatRepository.hent(behandlingId).getVilkårTidslinjer(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.MIN, LocalDate.MAX));
        LocalDateTimeline<Boolean> tidlinjeNoeInnvilget = harMinstEtVilkårMedUtfall(vilkårTidslinjer, Utfall.OPPFYLT);
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


}
