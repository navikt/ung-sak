package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.time.LocalDate;

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
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;

@Dependent
public class PeriodisertUtvidetRettIverksettTjeneste {

    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;
    private boolean brukPeriodisertRammevedtak;

    @Inject
    public PeriodisertUtvidetRettIverksettTjeneste(BehandlingRepository behandlingRepository,
                                                   VilkårResultatRepository vilkårResultatRepository,
                                                   @KonfigVerdi(value = "PERIODISERT_RAMMEVEDTAK", defaultVerdi = "false") boolean brukPeriodisertRammevedtak) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.brukPeriodisertRammevedtak = brukPeriodisertRammevedtak;
    }

    public LocalDateTimeline<Utfall> utfallSomErEndret(Behandling b) {
        if (!brukPeriodisertRammevedtak){
            throw new IllegalStateException("Forventet ikke å komme her uten at PERIODISERT_RAMMEVEDTAK er lansert");
        }
        LocalDateTimeline<Utfall> nyttUtfall = hentUtfallOgKombinerMedTidligereUtfall(b.getId());
        LocalDateTimeline<Utfall> tidligereUtfall = hentUtfallOgKombinerMedTidligereUtfall(b.getOriginalBehandlingId().orElse(null));
        return nyttUtfall.crossJoin(tidligereUtfall, this::endretUtfall).compress();
    }

    private LocalDateSegment<Utfall> endretUtfall(LocalDateInterval intervall, LocalDateSegment<Utfall> nyttUtfall, LocalDateSegment<Utfall> gammeltUtfall) {
        Utfall nyVerdi = nyttUtfall.getValue();
        Utfall gammelVerdi = gammeltUtfall != null ? gammeltUtfall.getValue() : Utfall.IKKE_OPPFYLT; //ikke-innvilget i gammel tidslinje har samme effekt som IKKE_OPPFYLT
        return nyVerdi != gammelVerdi ? new LocalDateSegment<>(intervall, nyVerdi) : null;
    }

    private LocalDateTimeline<Utfall> hentUtfallOgKombinerMedTidligereUtfall(Long behandlingId) {
        if (behandlingId == null) {
            return LocalDateTimeline.empty();
        }
        Long forrigeBehandligId = behandlingRepository.hentBehandling(behandlingId).getOriginalBehandlingId().orElse(null);
        LocalDateTimeline<Utfall> utfall = hentUtfall(behandlingId);
        LocalDateTimeline<Utfall> tidligereUtfall = hentUtfallOgKombinerMedTidligereUtfall(forrigeBehandligId);
        LocalDateTimeline<Utfall> modifisertEksisterendeTidslinje = flippTilAvslagEtter(tidligereUtfall, utfall.getMaxLocalDate());
        return utfall.crossJoin(modifisertEksisterendeTidslinje, StandardCombinators::coalesceLeftHandSide);
    }

    private LocalDateTimeline<Utfall> hentUtfall(Long behandlingId) {
        return vilkårResultatRepository.hentHvisEksisterer(behandlingId)
            .map(vr -> vr.getVilkårTimeline(VilkårType.UTVIDETRETT))
            .orElse(LocalDateTimeline.empty())
            .mapValue(VilkårPeriode::getUtfall)
            .compress();
    }

    private LocalDateTimeline<Utfall> flippTilAvslagEtter(LocalDateTimeline<Utfall> eksisterendeUtfall, LocalDate grensedato) {
        LocalDateTimeline<Utfall> flippetTilAvslag = eksisterendeUtfall.intersection(new LocalDateInterval(grensedato.plusDays(1), LocalDate.MAX)).mapValue(v -> Utfall.IKKE_OPPFYLT);
        return eksisterendeUtfall.crossJoin(flippetTilAvslag, StandardCombinators::coalesceRightHandSide);
    }
}
