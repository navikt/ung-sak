package no.nav.ung.sak.vilkår;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

import java.util.Set;

/**
 * Et steg som utvider denne klassen har en vilkårsvurdering som avhenger av resultatet av tidligere vilkår.
 * Dersom tidligere vilkår, som det aktuelle vilkåret er avhengig av, har blitt avslått, skal utfallet av det aktuelle
 * vilkåret settes til uavklart.
 */
public abstract class VilkårVurderingSteg implements BehandlingSteg {

    private VilkårTjeneste vilkårTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    protected BehandlingRepository behandlingRepository;
    protected Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;

    protected VilkårVurderingSteg() {
    }

    protected VilkårVurderingSteg(VilkårResultatRepository vilkårResultatRepository,
                                  VilkårTjeneste vilkårTjeneste,
                                  BehandlingRepository behandlingRepository,
                                  @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        // Henter avslåtte perioder
        var perioder = finnPerioderForVurderingAvVilkår(kontekst);
        var ikkeRelevantPerioder = finnIkkeRelevantePerioder(kontekst, perioder);
        vilkårTjeneste.nullstillBehandlingsresultat(kontekst);
        vilkårResultatRepository.settUtfallForPeriode(kontekst.getBehandlingId(), getAktuellVilkårType(), ikkeRelevantPerioder, Utfall.IKKE_RELEVANT);
        return utførResten(kontekst);
    }

    protected LocalDateTimeline<Boolean> finnPerioderSomSkalVurderes(BehandlingskontrollKontekst kontekst) {
        var perioder = finnPerioderForVurderingAvVilkår(kontekst);
        var ikkeRelevantPerioder = finnIkkeRelevantePerioder(kontekst, perioder);
        return perioder.disjoint(ikkeRelevantPerioder);
    }

    private LocalDateTimeline<?> finnIkkeRelevantePerioder(BehandlingskontrollKontekst kontekst, LocalDateTimeline<?> perioder) {
        final var vilkår = vilkårTjeneste.hentVilkårResultat(kontekst.getBehandlingId());
        final var avslåttTidslinjeMedTilleggsPerioder = finnTidslinjeForAvslåtteAvhengigheter(kontekst, vilkår);
        return perioder.intersection(avslåttTidslinjeMedTilleggsPerioder);
    }

    private LocalDateTimeline<Boolean> finnPerioderForVurderingAvVilkår(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var perioderTilVurdering = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandling.getFagsakYtelseType(), behandling.getType())
            .utled(kontekst.getBehandlingId(), getAktuellVilkårType());
        return TidslinjeUtil.tilTidslinje(perioderTilVurdering);
    }

    private LocalDateTimeline<Boolean> finnTidslinjeForAvslåtteAvhengigheter(BehandlingskontrollKontekst kontekst, Vilkårene vilkår) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        final var avslåttTidslinje = vilkår.getVilkårene().stream().filter(v -> getVilkårAvhengigheter(behandling.getFagsakYtelseType(), behandling.getType()).contains(v.getVilkårType()))
            .flatMap(v -> v.getPerioder().stream())
            .filter(p -> p.getGjeldendeUtfall().equals(Utfall.IKKE_OPPFYLT))
            .map(p -> new LocalDateTimeline<>(p.getFom(), p.getTom(), true))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
        return avslåttTidslinje;
    }

    public abstract BehandleStegResultat utførResten(BehandlingskontrollKontekst kontekst);

    public abstract VilkårType getAktuellVilkårType();

    /**
     * Hent vilkår som, dersom de ikke er innvilget, skal markere overlappende perioder for det aktuelle vilkåret under
     * vurdering som ikke relevant
     * <p>
     * Default implentasjon henter alle vilkårtyper fra steg som er før getAktuelLVilkårType
     */
    public abstract Set<VilkårType> getVilkårAvhengigheter(FagsakYtelseType ytelseType, BehandlingType behandlingType);


    ;

}
