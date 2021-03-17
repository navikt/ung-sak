package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@BehandlingStegRef(kode = "POST_MEDISINSK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class PostSykdomOgKontinuerligTilsynSteg implements BehandlingSteg {

    public static final VilkårType VILKÅRET = VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR;
    private static final Logger log = LoggerFactory.getLogger(PostSykdomOgKontinuerligTilsynSteg.class);
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    PostSykdomOgKontinuerligTilsynSteg() {
        // CDI
    }

    @Inject
    public PostSykdomOgKontinuerligTilsynSteg(BehandlingRepositoryProvider repositoryProvider,
                                              @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        final var perioder = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VILKÅRET); // OK
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        var innvilgetePerioder = vilkårene.getVilkår(VILKÅRET).orElseThrow()
            .getPerioder()
            .stream()
            .filter(it -> perioder.stream().anyMatch(at -> at.overlapper(it.getPeriode())))
            .filter(it -> Utfall.OPPFYLT.equals(it.getUtfall()))
            .collect(Collectors.toSet());

        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
