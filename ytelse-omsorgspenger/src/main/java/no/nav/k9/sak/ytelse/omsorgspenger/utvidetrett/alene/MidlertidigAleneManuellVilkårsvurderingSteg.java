package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.alene;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.VilkårTjeneste;

@FagsakYtelseTypeRef("OMP_MA")
@BehandlingStegRef(kode = "MANUELL_VILKÅRSVURDERING")
@BehandlingTypeRef
@ApplicationScoped
public class MidlertidigAleneManuellVilkårsvurderingSteg implements BehandlingSteg {

    private AksjonspunktDefinisjon aksjonspunktDef = AksjonspunktDefinisjon.VURDER_OMS_UTVIDET_RETT;
    private final VilkårType vilkårType = VilkårType.UTVIDETRETT;

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private SøknadRepository søknadRepository;
    private VilkårTjeneste vilkårTjeneste;

    public MidlertidigAleneManuellVilkårsvurderingSteg() {
        // CDO
    }

    @Inject
    public MidlertidigAleneManuellVilkårsvurderingSteg(BehandlingRepository behandlingRepository,
                                                       SøknadRepository søknadRepository,
                                                       VilkårTjeneste vilkårTjeneste,
                                                       VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.søknadRepository = søknadRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();
        var søknad = søknadRepository.hentSøknad(behandling);
        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        var vilkårTimeline = vilkårene.getVilkårTimeline(vilkårType);

        var søknadsperiode = søknad.getSøknadsperiode();
        var intersectTimeline = vilkårTimeline.intersection(new LocalDateInterval(søknadsperiode.getFomDato(), fagsak.getPeriode().getTomDato()));

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
