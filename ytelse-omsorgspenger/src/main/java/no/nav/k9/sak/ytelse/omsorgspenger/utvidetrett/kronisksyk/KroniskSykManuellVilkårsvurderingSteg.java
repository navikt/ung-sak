package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.MANUELL_VILKÅRSVURDERING;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.VilkårTjeneste;

@FagsakYtelseTypeRef(OMSORGSPENGER_KS)
@BehandlingStegRef(stegtype = MANUELL_VILKÅRSVURDERING)
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
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();

        var søknad = søknadRepository.hentSøknad(behandling);
        var vilkårene = vilkårTjeneste.hentVilkårResultat(behandlingId);

        var vilkårTimeline = vilkårene.getVilkårTimeline(vilkårType);
        var intersectTimeline = vilkårTimeline.intersection(new LocalDateInterval(søknad.getMottattDato(), fagsak.getPeriode().getTomDato()));

        if (vilkårTjeneste.erNoenVilkårHeltAvslått(behandlingId, vilkårType, intersectTimeline.getMinLocalDate(), intersectTimeline.getMaxLocalDate())) {
            vilkårTjeneste.settVilkårutfallTilIkkeVurdert(behandlingId, vilkårType,
                new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(vilkårTimeline.getMinLocalDate(), vilkårTimeline.getMaxLocalDate()))));
            behandling.getAksjonspunktMedDefinisjonOptional(aksjonspunktDef).ifPresent(a -> a.avbryt());
            behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        return BehandleStegResultat.utførtMedAksjonspunkter(List.of(aksjonspunktDef));

    }
}
