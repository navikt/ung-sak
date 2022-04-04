package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_OMSORG_FOR;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.RyddVilkårTyper;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.hendelse.brukerdialoginnsyn.BrukerdialoginnsynService;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.OmsorgenForVilkårGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.RelasjonsRolle;

@BehandlingStegRef(stegtype = VURDER_OMSORG_FOR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@ApplicationScoped
public class VurderOmsorgenForSteg implements BehandlingSteg {

    public static final VilkårType VILKÅRET = VilkårType.OMSORGEN_FOR;
    private static final Logger log = LoggerFactory.getLogger(VurderOmsorgenForSteg.class);
    private BehandlingRepositoryProvider repositoryProvider;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private OmsorgenForTjeneste omsorgenForTjeneste;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private BrukerdialoginnsynService brukerdialoginnsynService;

    VurderOmsorgenForSteg() {
        // CDI
    }

    @Inject
    public VurderOmsorgenForSteg(BehandlingRepositoryProvider repositoryProvider,
                                 @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                 OmsorgenForTjeneste omsorgenForTjeneste,
                                 BrukerdialoginnsynService brukerdialoginnsynService) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.repositoryProvider = repositoryProvider;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.omsorgenForTjeneste = omsorgenForTjeneste;
        this.brukerdialoginnsynService = brukerdialoginnsynService;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        final var perioder = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VILKÅRET);
        final var samletOmsorgenForTidslinje = omsorgenForTjeneste.mapGrunnlag(kontekst, perioder);

        final var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        if (skalHaAksjonspunktGrunnetManuellRevurdering(samletOmsorgenForTidslinje, behandling) || harAksjonspunkt(samletOmsorgenForTidslinje, false)) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_OMSORGEN_FOR_V2)));
        } else if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_OMSORGEN_FOR_V2) && harIkkeLengerAksjonspunkt(behandling, samletOmsorgenForTidslinje)) {
            log.info("Har aksjonspunt for omsorgen for, men det er ikke relevant lenger");
            // Må manuelt avbryte pga konfig på aksjonspunktet hvis registerdata tilsier at det ikke er noen grunn til å
            // Manuelt avklare dette
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.VURDER_OMSORGEN_FOR_V2).avbryt();
            behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        }

        final var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        final boolean haddeOmsorgenForISistePeriode = harOmsorgenForISistePeriode(vilkårene);
        final List<VilkårData> vilkårData = omsorgenForTjeneste.vurderPerioder(kontekst, samletOmsorgenForTidslinje);

        final Vilkårene oppdaterteVilkår = oppdaterVilkårene(kontekst, vilkårene, vilkårData);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), oppdaterteVilkår);

        final boolean harOmsorgenForISistePeriode = harOmsorgenForISistePeriode(oppdaterteVilkår);
        if (haddeOmsorgenForISistePeriode != harOmsorgenForISistePeriode) {
            brukerdialoginnsynService.publiserOmsorgenForHendelse(behandling, harOmsorgenForISistePeriode);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean harOmsorgenForISistePeriode(Vilkårene vilkårene) {
        final Vilkår vilkår = vilkårene.getVilkår(VILKÅRET).orElseThrow();
        if (vilkår.getPerioder().isEmpty()) {
            return false;
        }
        final VilkårPeriode vilkårPeriode = vilkår.getPerioder().get(vilkår.getPerioder().size() - 1);
        return (vilkårPeriode.getUtfall() == Utfall.OPPFYLT);
    }


    private boolean harIkkeLengerAksjonspunkt(Behandling behandling, LocalDateTimeline<OmsorgenForVilkårGrunnlag> samletOmsorgenForTidslinje) {
        return !harAksjonspunkt(samletOmsorgenForTidslinje, behandling.erManueltOpprettet());
    }

    private boolean skalHaAksjonspunktGrunnetManuellRevurdering(LocalDateTimeline<OmsorgenForVilkårGrunnlag> samletOmsorgenForTidslinje, final Behandling behandling) {
        return behandling.erManueltOpprettet()
            && harAksjonspunkt(samletOmsorgenForTidslinje, true)
            && behandling.getAksjonspunkter().stream().noneMatch(a -> a.getAksjonspunktDefinisjon() == AksjonspunktDefinisjon.VURDER_OMSORGEN_FOR_V2);
    }

    private boolean harAksjonspunkt(LocalDateTimeline<OmsorgenForVilkårGrunnlag> samletOmsorgenForTidslinje, boolean medAlleGamleVurderingerPåNytt) {
        for (LocalDateSegment<OmsorgenForVilkårGrunnlag> s : samletOmsorgenForTidslinje.toSegments()) {
            final OmsorgenForVilkårGrunnlag grunnlag = s.getValue();
            if ((grunnlag.getErOmsorgsPerson() == null || medAlleGamleVurderingerPåNytt) && (
                grunnlag.getRelasjonMellomSøkerOgPleietrengende() == null
                    || grunnlag.getRelasjonMellomSøkerOgPleietrengende().getRelasjonsRolle() == null
                    || grunnlag.getRelasjonMellomSøkerOgPleietrengende().getRelasjonsRolle() != RelasjonsRolle.BARN)) {
                return true;
            }
        }
        return false;
    }

    private Vilkårene oppdaterVilkårene(BehandlingskontrollKontekst kontekst, Vilkårene vilkårene, final List<VilkårData> vilkårData) {
        final VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(perioderTilVurderingTjeneste.maksMellomliggendePeriodeAvstand());
        final VilkårBuilder vilkårBuilder = builder.hentBuilderFor(VILKÅRET);

        for (VilkårData data : vilkårData) {
            oppdaterBehandlingMedVilkårresultat(data, vilkårBuilder);
        }

        builder.leggTil(vilkårBuilder);
        return builder.build();
    }

    private void oppdaterBehandlingMedVilkårresultat(VilkårData vilkårData, VilkårBuilder vilkårBuilder) {
        final var periode = vilkårData.getPeriode();
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato())
            .medUtfall(vilkårData.getUtfallType())
            .medMerknadParametere(vilkårData.getMerknadParametere())
            .medRegelEvaluering(vilkårData.getRegelEvaluering())
            .medRegelInput(vilkårData.getRegelInput())
            .medAvslagsårsak(vilkårData.getAvslagsårsak())
            .medMerknad(vilkårData.getVilkårUtfallMerknad()));
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg, BehandlingStegType sisteSteg) {
        final var perioder = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VILKÅRET);
        perioder.forEach(periode -> {
            if (!erVilkårOverstyrt(kontekst.getBehandlingId(), periode.getFomDato(), periode.getTomDato())) {
                Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
                RyddVilkårTyper ryddVilkårTyper = new RyddVilkårTyper(modell, repositoryProvider, behandling, kontekst);
                ryddVilkårTyper.ryddVedTilbakeføring();
                behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
            }
        });
    }

    protected boolean erVilkårOverstyrt(Long behandlingId, LocalDate fom, LocalDate tom) {
        Optional<Vilkårene> resultatOpt = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        if (resultatOpt.isPresent()) {
            Vilkårene vilkårene = resultatOpt.get();
            return vilkårene.getVilkårene()
                .stream()
                .filter(vilkår -> VILKÅRET.equals(vilkår.getVilkårType()))
                .map(Vilkår::getPerioder)
                .flatMap(Collection::stream)
                .filter(it -> it.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)))
                .anyMatch(VilkårPeriode::getErOverstyrt);
        }
        return false;
    }
}
