package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess.steg;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_OMSORG_FOR;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_AO;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_MA;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.omsorgenfor.OmsorgenForTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.omsorgenfor.regelmodell.OmsorgenForVilkårGrunnlag;

@BehandlingStegRef(value = VURDER_OMSORG_FOR)
@FagsakYtelseTypeRef(OMSORGSPENGER_KS)
@FagsakYtelseTypeRef(OMSORGSPENGER_MA)
@FagsakYtelseTypeRef(OMSORGSPENGER_AO)
@BehandlingTypeRef
@ApplicationScoped
public class VurderOmsorgenForSteg implements BehandlingSteg {

    public static final VilkårType VILKÅRET = VilkårType.OMSORGEN_FOR;
    public static final AksjonspunktDefinisjon AKTUELT_AKSJONSPUNKT = AksjonspunktDefinisjon.VURDER_OMSORGEN_FOR;

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private SøknadRepository søknadRepository;
    private VilkårTjeneste vilkårTjeneste;
    private OmsorgenForTjeneste omsorgenForTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    VurderOmsorgenForSteg() {
        //
    }

    @Inject
    public VurderOmsorgenForSteg(BehandlingRepository behandlingRepository,
                                 VilkårResultatRepository vilkårResultatRepository,
                                 SøknadRepository søknadRepository, VilkårTjeneste vilkårTjeneste,
                                 OmsorgenForTjeneste omsorgenForTjeneste,
                                 @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {

        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.søknadRepository = søknadRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.omsorgenForTjeneste = omsorgenForTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Fagsak fagsak = behandling.getFagsak();
        var søknad = søknadRepository.hentSøknad(behandling);
        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var vilkårTimeline = vilkårene.getVilkårTimeline(VILKÅRET);
        var søknadsperiode = søknad.getSøknadsperiode();


        //når søker om KS etter året barnet er 18 år (men før 18+3år), blir hele utledet søknadperiode utenfor fagsakens periode
        boolean ksSøktEtter18År = fagsak.getYtelseType() == OMSORGSPENGER_KS && søknadsperiode.getFomDato().isAfter(fagsak.getPeriode().getTomDato());

        LocalDateTimeline<VilkårPeriode> intersectTimeline = ksSøktEtter18År
            ? LocalDateTimeline.empty()
            : vilkårTimeline.intersection(new LocalDateInterval(søknadsperiode.getFomDato(), fagsak.getPeriode().getTomDato()));

        boolean noenAndreVilkårErHeltAvslått = !intersectTimeline.isEmpty() && vilkårTjeneste.erNoenVilkårHeltAvslått(behandlingId, VILKÅRET, intersectTimeline.getMinLocalDate(), intersectTimeline.getMaxLocalDate());
        if (noenAndreVilkårErHeltAvslått) {
            vilkårTjeneste.settVilkårutfallTilIkkeVurdert(behandlingId, VILKÅRET, new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(vilkårTimeline.getMinLocalDate(), vilkårTimeline.getMaxLocalDate()))));
            behandling.getAksjonspunktMedDefinisjonOptional(AKTUELT_AKSJONSPUNKT).ifPresent(Aksjonspunkt::avbryt);
            behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        if (behandling.erManueltOpprettet() || behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_ALDERSVILKÅR_BARN)) {
            //skal alltid ha aksjonspunktet ved manuell revurdering slik at saksbehandler kan korrigere
            //og også hvis aldersvilkåret ble gjort manuelt (har aksjonspunkt)
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_OMSORGEN_FOR));
        }
        FagsakYtelseType fagsakYtelseType = behandling.getFagsakYtelseType();
        if (fagsakYtelseType == OMSORGSPENGER_AO || fagsakYtelseType == OMSORGSPENGER_KS) {
            return utførStegAutomatisk(kontekst);
        }
        return utførStegManuelt(kontekst);
    }

    public BehandleStegResultat utførStegAutomatisk(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var vilkår = vilkårene.getVilkår(VilkårType.OMSORGEN_FOR).orElseThrow();
        List<VilkårPeriode> ikkeVurdertePerioder = vilkår.getPerioder().stream().filter(v -> v.getUtfall() == Utfall.IKKE_VURDERT).toList();
        LocalDateTimeline<OmsorgenForVilkårGrunnlag> grunnlagsdata = omsorgenForTjeneste.oversettSystemdataTilRegelModellGrunnlag(kontekst.getBehandlingId(), ikkeVurdertePerioder);
        List<VilkårData> vilkårData = omsorgenForTjeneste.vurderPerioder(grunnlagsdata);
        Vilkårene oppdaterteVilkår = oppdaterVilkårene(kontekst, vilkårene, vilkårData);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), oppdaterteVilkår);

        List<AksjonspunktDefinisjon> aksjonspunkter = vilkårData.stream().flatMap(vd -> vd.getApDefinisjoner().stream()).distinct().toList();
        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
    }

    private Vilkårene oppdaterVilkårene(BehandlingskontrollKontekst kontekst, Vilkårene vilkårene, List<VilkårData> vilkårData) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
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
        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var vilkår = vilkårene.getVilkår(VilkårType.OMSORGEN_FOR).orElseThrow();
        if (vilkår.getPerioder().stream().anyMatch(v -> v.getUtfall() == Utfall.IKKE_VURDERT)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_OMSORGEN_FOR));
        } else {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

    }
}
