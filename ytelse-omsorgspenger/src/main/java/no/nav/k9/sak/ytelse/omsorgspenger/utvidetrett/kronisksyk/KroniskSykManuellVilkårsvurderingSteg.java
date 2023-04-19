package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.MANUELL_VILKÅRSVURDERING;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS;

import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.VilkårTjeneste;

@FagsakYtelseTypeRef(OMSORGSPENGER_KS)
@BehandlingStegRef(value = MANUELL_VILKÅRSVURDERING)
@BehandlingTypeRef
@ApplicationScoped
public class KroniskSykManuellVilkårsvurderingSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;

    private AksjonspunktDefinisjon aksjonspunktDef = AksjonspunktDefinisjon.VURDER_OMS_UTVIDET_RETT;
    private VilkårType vilkårType = VilkårType.UTVIDETRETT;
    private SøknadRepository søknadRepository;

    private VilkårTjeneste vilkårTjeneste;

    public KroniskSykManuellVilkårsvurderingSteg() {
        // CDO
    }

    @Inject
    public KroniskSykManuellVilkårsvurderingSteg(BehandlingRepository behandlingRepository,
                                                 SøknadRepository søknadRepository,
                                                 VilkårTjeneste vilkårTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.søknadRepository = søknadRepository;
        this.vilkårTjeneste = vilkårTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();

        if (erNoeInnevilgetFor(behandlingId, VilkårType.OMSORGEN_FOR)) {
            //saksbehandler skal manuelt vurdere sykdom og sette dato for når vedtake gjelder fra
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(aksjonspunktDef));
        } else {
            //kan ikke innvilge når tidligere vilkår ikke er oppfylt, så saksbehandler trenger ikke å vurdere sykdom
            var vilkårene = vilkårTjeneste.hentVilkårResultat(behandlingId);
            NavigableSet<DatoIntervallEntitet> vilkårsperioder = new TreeSet<>(vilkårene.getVilkårTimeline(vilkårType).stream().map(segment -> DatoIntervallEntitet.fra(segment.getLocalDateInterval())).toList());
            vilkårTjeneste.settVilkårutfallTilIkkeVurdert(behandlingId, vilkårType, vilkårsperioder);
            var behandling = behandlingRepository.hentBehandling(behandlingId);
            behandling.getAksjonspunktMedDefinisjonOptional(aksjonspunktDef).ifPresent(Aksjonspunkt::avbryt);
            behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
    }

    boolean erNoeInnevilgetFor(Long behandlingId, VilkårType vilkårType) {
        Vilkårene vilkårene = vilkårTjeneste.hentVilkårResultat(behandlingId);
        return vilkårene.getVilkårTimeline(vilkårType).stream().anyMatch(segment -> segment.getValue().getUtfall() == Utfall.OPPFYLT);
    }
}

