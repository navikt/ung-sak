package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;

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
        return vilkårResultatRepository.hentHvisEksisterer(behandlingId)
            .map(vr -> vr.getVilkårTimeline(VilkårType.UTVIDETRETT))
            .orElse(LocalDateTimeline.empty())
            .mapValue(VilkårPeriode::getUtfall)
            .compress();
    }

}
