package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.medisinsk;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.RyddVilkårTyper;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.medisinsk.OmsorgenForTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.vilkår.VilkårType;

@BehandlingStegRef(kode = "VURDER_OMSORG_FOR")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderOmsorgenForSteg implements BehandlingSteg {

    public static final VilkårType VILKÅRET = VilkårType.OMSORGEN_FOR;
    private BehandlingRepositoryProvider repositoryProvider;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private OmsorgenForTjeneste omsorgenForTjeneste;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    VurderOmsorgenForSteg() {
        // CDI
    }

    @Inject
    public VurderOmsorgenForSteg(BehandlingRepositoryProvider repositoryProvider,
                                 VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                 OmsorgenForTjeneste omsorgenForTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.repositoryProvider = repositoryProvider;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.omsorgenForTjeneste = omsorgenForTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        final var perioder = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VILKÅRET);

        final var vilkårData = omsorgenForTjeneste.vurderPerioder(kontekst, perioder);

        final var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        final var oppdaterteVilkår = oppdaterBehandlingMedVilkårresultat(vilkårData, vilkårene);

        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), oppdaterteVilkår);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private Vilkårene oppdaterBehandlingMedVilkårresultat(VilkårData vilkårData, Vilkårene vilkårene) {
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);

        final var vilkårBuilder = builder.hentBuilderFor(vilkårData.getVilkårType());
        final var periode = vilkårData.getPeriode();
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato())
            .medUtfall(vilkårData.getUtfallType())
            .medMerknadParametere(vilkårData.getMerknadParametere())
            .medRegelEvaluering(vilkårData.getRegelEvaluering())
            .medRegelInput(vilkårData.getRegelInput())
            .medAvslagsårsak(vilkårData.getAvslagsårsak())
            .medMerknad(vilkårData.getVilkårUtfallMerknad()));

        builder.leggTil(vilkårBuilder);

        return builder.build();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg, BehandlingStegType sisteSteg) {
        final var perioder = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VILKÅRET);
        perioder.forEach(periode -> {
            if (!erVilkårOverstyrt(kontekst.getBehandlingId(), periode.getFomDato(), periode.getTomDato())) {
                Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
                RyddVilkårTyper ryddVilkårTyper = new RyddVilkårTyper(modell, repositoryProvider, behandling, kontekst);
                ryddVilkårTyper.ryddVedTilbakeføring(List.of(VILKÅRET));
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
