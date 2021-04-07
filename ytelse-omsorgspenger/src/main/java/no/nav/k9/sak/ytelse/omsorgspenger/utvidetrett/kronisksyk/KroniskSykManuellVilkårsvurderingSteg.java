package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk;

import java.util.List;
import java.util.NavigableSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.VilkårTjeneste;

@FagsakYtelseTypeRef("OMP_KS")
@BehandlingStegRef(kode = "MANUELL_VILKÅRSVURDERING")
@BehandlingTypeRef
@ApplicationScoped
public class KroniskSykManuellVilkårsvurderingSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

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
        var vilkår = vilkårene.getVilkår(vilkårType);

        if (vilkårTjeneste.erNoenVilkårHeltAvslått(behandlingId, vilkårType, søknad.getMottattDato(), fagsak.getPeriode().getTomDato())) {
            behandling.getAksjonspunktMedDefinisjonOptional(aksjonspunktDef).ifPresent(a -> a.avbryt());
            behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        if (vilkår.get().getPerioder().stream().anyMatch(v -> v.getUtfall() == Utfall.IKKE_VURDERT)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(aksjonspunktDef));
        } else {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!BehandlingStegType.MANUELL_VILKÅRSVURDERING.equals(tilSteg)) {
            Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            var ref = BehandlingReferanse.fra(behandling);
            NavigableSet<DatoIntervallEntitet> perioderTilVurdering = vilkårTjeneste.utledPerioderTilVurdering(ref, vilkårType, false);
            vilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst, vilkårType, perioderTilVurdering);
        }
    }
}
