package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess.steg;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
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
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.omsorgenfor.OmsorgenForTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.omsorgenfor.regelmodell.OmsorgenForVilkårGrunnlag;

@BehandlingStegRef(kode = "VURDER_OMSORG_FOR")
@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
@FagsakYtelseTypeRef("OMP_AO")
@BehandlingTypeRef
@ApplicationScoped
public class VurderOmsorgenForSteg implements BehandlingSteg {

    public static final VilkårType VILKÅRET = VilkårType.OMSORGEN_FOR;

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private OmsorgenForTjeneste omsorgenForTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    private boolean automatiserVedtak;

    VurderOmsorgenForSteg() {
        //
    }

    @Inject
    public VurderOmsorgenForSteg(BehandlingRepository behandlingRepository,
                                 VilkårResultatRepository vilkårResultatRepository,
                                 OmsorgenForTjeneste omsorgenForTjeneste,
                                 @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                 @KonfigVerdi(value = "OMP_DELT_BOSTED_RAMMEVEDTAK", defaultVerdi = "true") boolean automatiserVedtak) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.omsorgenForTjeneste = omsorgenForTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.automatiserVedtak = automatiserVedtak;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (automatiserVedtak) {
            return utførStegDelautomatisk(kontekst);
        }
        return utførStegHelmanuelt(kontekst);
    }

    public BehandleStegResultat utførStegDelautomatisk(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var vilkår = vilkårene.getVilkår(VilkårType.OMSORGEN_FOR).orElseThrow();
        if (vilkår.getPerioder().stream().anyMatch(v -> v.getUtfall() == Utfall.IKKE_VURDERT)) {
            LocalDateTimeline<OmsorgenForVilkårGrunnlag> grunnlagsdata = omsorgenForTjeneste.oversettSystemdataTilRegelModellGrunnlag(kontekst.getBehandlingId(), vilkår.getPerioder());
            List<VilkårData> vilkårData = omsorgenForTjeneste.vurderPerioder(grunnlagsdata);
            Vilkårene oppdaterteVilkår = oppdaterVilkårene(kontekst, vilkårene, vilkårData);
            vilkårResultatRepository.lagre(kontekst.getBehandlingId(), oppdaterteVilkår);

            //regelsett er ikke komplett (f.eks fosterbarn), saksbehandler skal kunne innvilge perioder som avslås her
            boolean altInnvilget = vilkårData.stream().allMatch(vp -> vp.getUtfallType() == Utfall.OPPFYLT);
            return altInnvilget
                ? BehandleStegResultat.utførtUtenAksjonspunkter()
                : BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_OMSORGEN_FOR));
        } else {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
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

    public BehandleStegResultat utførStegHelmanuelt(BehandlingskontrollKontekst kontekst) {
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
