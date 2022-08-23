package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess.steg;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_ALDERSVILKÅR_BARN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_AO;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aldersvilkår.AldersvilkårBarnTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aldersvilkår.regelmodell.AldersvilkårBarnVilkårGrunnlag;

@FagsakYtelseTypeRef(OMSORGSPENGER_AO)
@BehandlingStegRef(value = VURDER_ALDERSVILKÅR_BARN)
@BehandlingTypeRef
@ApplicationScoped
public class AldersvilkårBarnSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private AldersvilkårBarnTjeneste aldersvilkårBarnTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private boolean aldersvilkårLansert;

    public AldersvilkårBarnSteg() {
        //for CDI proxy
    }

    @Inject
    public AldersvilkårBarnSteg(BehandlingRepository behandlingRepository,
                                AldersvilkårBarnTjeneste aldersvilkårBarnTjeneste,
                                VilkårResultatRepository vilkårResultatRepository,
                                @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                @KonfigVerdi(value = "OMP_RAMMEVEDTAK_ALDERSVILKAAR", defaultVerdi = "true") boolean aldersvilkårLansert) {
        this.behandlingRepository = behandlingRepository;
        this.aldersvilkårBarnTjeneste = aldersvilkårBarnTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.aldersvilkårLansert = aldersvilkårLansert;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (!aldersvilkårLansert){
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        Long behandlingId = kontekst.getBehandlingId();
        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        Vilkår vilkår = vilkårene.getVilkår(VilkårType.ALDERSVILKÅR_BARN).orElseThrow();

        List<VilkårData> vilkårData = new ArrayList<>();
        for (AldersvilkårBarnVilkårGrunnlag grunnlag : aldersvilkårBarnTjeneste.oversettSystemdataTilRegelModellGrunnlag(behandlingId, vilkår)) {
            vilkårData.add(aldersvilkårBarnTjeneste.vurder(grunnlag));
        }
        Vilkårene oppdaterteVilkår = oppdaterVilkårene(vilkårene, vilkårData, behandlingId);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), oppdaterteVilkår);

        List<AksjonspunktDefinisjon> aksjonspunkter = vilkårData.stream().flatMap(vd -> vd.getApDefinisjoner().stream()).distinct().toList();
        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
    }

    private Vilkårene oppdaterVilkårene(Vilkårene vilkårene, List<VilkårData> vilkårData, long behandingId) {

        Behandling behandling = behandlingRepository.hentBehandling(behandingId);
        VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());

        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(perioderTilVurderingTjeneste.maksMellomliggendePeriodeAvstand());
        VilkårBuilder vilkårBuilder = builder.hentBuilderFor(VilkårType.ALDERSVILKÅR_BARN);

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

}
