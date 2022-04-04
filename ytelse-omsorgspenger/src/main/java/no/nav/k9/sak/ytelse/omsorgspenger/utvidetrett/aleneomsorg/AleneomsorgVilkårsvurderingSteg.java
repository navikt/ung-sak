package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.MANUELL_VILKÅRSVURDERING;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_AO;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg.regelmodell.AleneomsorgVilkårGrunnlag;

@FagsakYtelseTypeRef(OMSORGSPENGER_AO)
@BehandlingStegRef(stegtype = MANUELL_VILKÅRSVURDERING)
@BehandlingTypeRef
@ApplicationScoped
public class AleneomsorgVilkårsvurderingSteg implements BehandlingSteg {

    private AksjonspunktDefinisjon aksjonspunktDef = AksjonspunktDefinisjon.VURDER_OMS_UTVIDET_RETT;
    private final VilkårType VILKÅRET = VilkårType.UTVIDETRETT;

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private SøknadRepository søknadRepository;
    private VilkårTjeneste vilkårTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private AleneomsorgTjeneste aleneomsorgTjeneste;
    private boolean automatiserVedtak;

    public AleneomsorgVilkårsvurderingSteg() {
        // CDO
    }

    @Inject
    public AleneomsorgVilkårsvurderingSteg(BehandlingRepository behandlingRepository,
                                           SøknadRepository søknadRepository,
                                           VilkårTjeneste vilkårTjeneste,
                                           VilkårResultatRepository vilkårResultatRepository,
                                           @FagsakYtelseTypeRef(OMSORGSPENGER_AO) VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                           AleneomsorgTjeneste aleneomsorgTjeneste,
                                           @KonfigVerdi(value = "OMP_DELT_BOSTED_RAMMEVEDTAK", defaultVerdi = "true") boolean automatiserVedtak) {
        this.behandlingRepository = behandlingRepository;
        this.søknadRepository = søknadRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.perioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.aleneomsorgTjeneste = aleneomsorgTjeneste;
        this.automatiserVedtak = automatiserVedtak;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (automatiserVedtak) {
            return utførStegAutomatisk(kontekst);
        }
        return utførStegManuelt(kontekst);
    }

    public BehandleStegResultat utførStegAutomatisk(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();
        var søknad = søknadRepository.hentSøknad(behandling);
        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        var vilkårTimeline = vilkårene.getVilkårTimeline(VILKÅRET);

        var søknadsperiode = søknad.getSøknadsperiode();
        var intersectTimeline = vilkårTimeline.intersection(new LocalDateInterval(søknadsperiode.getFomDato(), fagsak.getPeriode().getTomDato()));

        boolean noenAndreVilkårErHeltAvslått = vilkårTjeneste.erNoenVilkårHeltAvslått(behandlingId, VILKÅRET, intersectTimeline.getMinLocalDate(), intersectTimeline.getMaxLocalDate());
        if (noenAndreVilkårErHeltAvslått) {
            vilkårTjeneste.settVilkårutfallTilIkkeVurdert(behandlingId, VILKÅRET,
                new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(vilkårTimeline.getMinLocalDate(), vilkårTimeline.getMaxLocalDate()))));
            behandling.getAksjonspunktMedDefinisjonOptional(aksjonspunktDef).ifPresent(a -> a.avbryt());
            behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        Vilkår vilkår = vilkårene.getVilkår(VILKÅRET).orElseThrow();
        List<VilkårPeriode> ikkeVurdertePerioder = vilkår.getPerioder().stream().filter(v -> v.getUtfall() == Utfall.IKKE_VURDERT).toList();
        LocalDateTimeline<AleneomsorgVilkårGrunnlag> grunnlagsdata = aleneomsorgTjeneste.oversettSystemdataTilRegelModellGrunnlag(kontekst.getBehandlingId(), ikkeVurdertePerioder);
        List<VilkårData> vilkårData = aleneomsorgTjeneste.vurderPerioder(grunnlagsdata);
        Vilkårene oppdaterteVilkår = oppdaterVilkårene(vilkårene, vilkårData);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), oppdaterteVilkår);

        List<AksjonspunktDefinisjon> aksjonspunkter = vilkårData.stream().flatMap(vd -> vd.getApDefinisjoner().stream()).distinct().toList();
        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
    }

    private Vilkårene oppdaterVilkårene(Vilkårene vilkårene, List<VilkårData> vilkårData) {
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(perioderTilVurderingTjeneste.maksMellomliggendePeriodeAvstand());
        VilkårBuilder vilkårBuilder = builder.hentBuilderFor(VILKÅRET);

        for (VilkårData data : vilkårData) {
            oppdaterBehandlingMedVilkårresultat(data, vilkårBuilder);
        }

        builder.leggTil(vilkårBuilder);
        return builder.build();
    }

    private void oppdaterBehandlingMedVilkårresultat(VilkårData vilkårData, VilkårBuilder vilkårBuilder) {
        var periode = vilkårData.getPeriode();
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato())
            .medUtfall(vilkårData.getUtfallType())
            .medMerknadParametere(vilkårData.getMerknadParametere())
            .medRegelEvaluering(vilkårData.getRegelEvaluering())
            .medRegelInput(vilkårData.getRegelInput())
            .medAvslagsårsak(vilkårData.getAvslagsårsak())
            .medMerknad(vilkårData.getVilkårUtfallMerknad()));
    }

    public BehandleStegResultat utførStegManuelt(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();
        var søknad = søknadRepository.hentSøknad(behandling);
        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        var vilkårTimeline = vilkårene.getVilkårTimeline(VILKÅRET);

        var søknadsperiode = søknad.getSøknadsperiode();
        var intersectTimeline = vilkårTimeline.intersection(new LocalDateInterval(søknadsperiode.getFomDato(), fagsak.getPeriode().getTomDato()));

        if (vilkårTjeneste.erNoenVilkårHeltAvslått(behandlingId, VILKÅRET, intersectTimeline.getMinLocalDate(), intersectTimeline.getMaxLocalDate())) {
            vilkårTjeneste.settVilkårutfallTilIkkeVurdert(behandlingId, VILKÅRET,
                new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(vilkårTimeline.getMinLocalDate(), vilkårTimeline.getMaxLocalDate()))));
            behandling.getAksjonspunktMedDefinisjonOptional(aksjonspunktDef).ifPresent(a -> a.avbryt());
            behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        return BehandleStegResultat.utførtMedAksjonspunkter(List.of(aksjonspunktDef));

    }
}
