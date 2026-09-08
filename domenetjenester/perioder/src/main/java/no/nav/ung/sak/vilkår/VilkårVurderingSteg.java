package no.nav.ung.sak.vilkår;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import no.nav.fpsak.tidsserie.LocalDateSegment;
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
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Et steg som utvider denne klassen har en vilkårsvurdering som avhenger av resultatet av tidligere vilkår.
 * Dersom tidligere vilkår, som det aktuelle vilkåret er avhengig av, har blitt avslått, skal utfallet av det aktuelle
 * vilkåret settes til uavklart.
 */
public abstract class VilkårVurderingSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(VilkårVurderingSteg.class);
    protected VilkårTjeneste vilkårTjeneste;
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
        var perioder = finnPerioderForVurderingAvVilkår(kontekst);
        var eksisterendeIkkeRelevantePerioder = finnEksisterendeIkkeRelevantePerioder(kontekst);
        var nyeIkkeRelevantPerioder = finnIkkeRelevantePerioder(kontekst, perioder);

        Vilkårene eksisterendeVilkår = vilkårResultatRepository.hentHvisEksisterer(kontekst.getBehandlingId()).orElseThrow();
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(eksisterendeVilkår);
        VilkårBuilder vilkårBuilder = builder.hentBuilderFor(getAktuellVilkårType());

        boolean noeEksisterendeIkkeRelevantErNåRelevant = !eksisterendeIkkeRelevantePerioder.disjoint(nyeIkkeRelevantPerioder).isEmpty();
        if (noeEksisterendeIkkeRelevantErNåRelevant) {
            //dette er for å håndtere tilfelle hvor saksbehandler først har satt en sluttdato for vilkår 1, så behandler
            //vilkår 2, for så å senere endre til en senere sluttdato i vilkår 1. Nå må perioden mellom ny og gammel sluttdato
            //også vurderes i vilkårene etter vilkår 1. Vi har ikke funksjonalitet for å vurdere bare denne nye perioden,
            //så vi tar hele den orginale perioden opp igjen til vurdering.
            vilkårBuilder.tilbakestill(TidslinjeUtil.tilDatoIntervallEntiteter(perioder)); //nødvendig for å få en sammenhengende periode
            for (LocalDateSegment<Boolean> segment : perioder.segmenter()) {
                vilkårBuilder.leggTil(new VilkårPeriodeBuilder()
                    .medPeriode(segment.getFom(), segment.getTom())
                    .medUtfall(Utfall.IKKE_VURDERT));
            }
            log.info("Tilbakestiller perioder til {} for {} siden noe som før var ikke-relevant nå må vurderes.", perioder, getAktuellVilkårType());
        }
        for (LocalDateSegment<?> segment : nyeIkkeRelevantPerioder.segmenter()) {
            vilkårBuilder.leggTil(new VilkårPeriodeBuilder()
                .medPeriode(segment.getFom(), segment.getTom())
                .medUtfall(Utfall.IKKE_RELEVANT));
            log.info("Setter {} til ikke-relevant for {} ", segment.getLocalDateInterval(), getAktuellVilkårType());
        }

        builder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), builder.build());

        vilkårTjeneste.nullstillBehandlingsresultat(kontekst);

        return utførResten(kontekst);
    }

    protected LocalDateTimeline<Boolean> finnPerioderSomSkalVurderes(BehandlingskontrollKontekst kontekst) {
        var perioder = finnPerioderForVurderingAvVilkår(kontekst);
        var ikkeRelevantPerioder = finnIkkeRelevantePerioder(kontekst, perioder);
        return perioder.disjoint(ikkeRelevantPerioder);
    }

    private LocalDateTimeline<?> finnEksisterendeIkkeRelevantePerioder(BehandlingskontrollKontekst kontekst) {
        LocalDateTimeline<VilkårPeriode> eksisterendeVilkårtidslinje = vilkårResultatRepository.hentHvisEksisterer(kontekst.getBehandlingId()).orElseThrow()
            .getVilkårTimeline(getAktuellVilkårType());
        return eksisterendeVilkårtidslinje.filterValue(v -> v.getUtfall() == Utfall.IKKE_RELEVANT);
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
