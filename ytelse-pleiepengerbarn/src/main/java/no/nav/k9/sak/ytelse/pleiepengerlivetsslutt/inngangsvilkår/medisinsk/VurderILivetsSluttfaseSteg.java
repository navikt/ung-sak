package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.RyddVilkårTyper;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingRepository;

@BehandlingStegRef(kode = "VURDER_MEDISINSK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PPN")
@ApplicationScoped
public class VurderILivetsSluttfaseSteg implements BehandlingSteg {

    private BehandlingRepositoryProvider repositoryProvider;
    private PleiebehovResultatRepository resultatRepository;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private MedisinskVilkårTjeneste medisinskVilkårTjeneste = new MedisinskVilkårTjeneste();
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomVurderingRepository sykdomVurderingRepository;

    VurderILivetsSluttfaseSteg() {
        // CDI
    }

    @Inject
    public VurderILivetsSluttfaseSteg(BehandlingRepositoryProvider repositoryProvider,
                                      PleiebehovResultatRepository resultatRepository,
                                      @FagsakYtelseTypeRef("PPN") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                      SykdomGrunnlagRepository sykdomGrunnlagRepository,
                                      SykdomVurderingRepository sykdomVurderingRepository) {
        this.resultatRepository = resultatRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.repositoryProvider = repositoryProvider;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var perioder = perioderTilVurderingTjeneste.utled(behandlingId, VilkårType.I_LIVETS_SLUTTFASE);

        final Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        final SykdomGrunnlagBehandling sykdomGrunnlagBehandling = opprettGrunnlag(perioder, behandling);

        var gjeldendeSykdomVurdering = sykdomVurderingRepository.getSisteVurderingstidslinjeFor(SykdomVurderingType.LIVETS_SLUTTFASE, behandling.getFagsak().getPleietrengendeAktørId());
        var sykdomVurderingSegmenter = gjeldendeSykdomVurdering.toSegments();

        var harVurdering = !sykdomVurderingSegmenter.isEmpty()
            && sykdomVurderingSegmenter
            .stream()
            .allMatch(sykdomVurderingSegment -> sykdomVurderingSegment.getValue() != null
                && Set.of(Resultat.OPPFYLT, Resultat.IKKE_OPPFYLT).contains(sykdomVurderingSegment.getValue().getResultat()));
        if (!harVurdering || behandling.erManueltOpprettet()) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING)));
        }

        var builder = Vilkårene.builderFraEksisterende(vilkårene);
        vurderVilkår(behandlingId, sykdomGrunnlagBehandling, builder, VilkårType.I_LIVETS_SLUTTFASE, perioder);
        vilkårResultatRepository.lagre(behandlingId, builder.build());

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private SykdomGrunnlagBehandling opprettGrunnlag(NavigableSet<DatoIntervallEntitet> perioderSamlet, final Behandling behandling) {
        return sykdomGrunnlagRepository.utledOgLagreGrunnlag(
            behandling.getFagsak().getSaksnummer(),
            behandling.getUuid(),
            behandling.getAktørId(),
            behandling.getFagsak().getPleietrengendeAktørId(),
            perioderSamlet.stream()
                .map(p -> new Periode(p.getFomDato(), p.getTomDato()))
                .collect(Collectors.toList()),
            List.of()
        );
    }

    private void vurderVilkår(Long behandlingId,
                              SykdomGrunnlagBehandling sykdomGrunnlagBehandling,
                              VilkårResultatBuilder builder,
                              VilkårType vilkåret,
                              NavigableSet<DatoIntervallEntitet> perioder) {

        var vilkårBuilder = builder.hentBuilderFor(vilkåret);
        for (DatoIntervallEntitet periode : perioder) {
            final var vilkårData = medisinskVilkårTjeneste.vurderPerioder(vilkåret, periode, sykdomGrunnlagBehandling);
            oppdaterBehandlingMedVilkårresultat(vilkårData, vilkårBuilder);
        }
        builder.leggTil(vilkårBuilder);
    }

    private void oppdaterBehandlingMedVilkårresultat(VilkårData vilkårData, VilkårBuilder vilkårBuilder) {

        final var periode = vilkårData.getPeriode();
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato())
            .medUtfall(vilkårData.getUtfallType())
            .medMerknadParametere(vilkårData.getMerknadParametere())
            .medRegelEvaluering(vilkårData.getRegelEvaluering())
            .medRegelInput(vilkårData.getRegelInput())
            .medAvslagsårsak(vilkårData.getAvslagsårsak())
            .medMerknad(vilkårData.getVilkårUtfallMerknad()));
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg, BehandlingStegType sisteSteg) {
        håndterHoppOverBakover(kontekst, modell, VilkårType.I_LIVETS_SLUTTFASE);
    }

    private void håndterHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, VilkårType vilkåret) {
        final var perioder = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), vilkåret);
        perioder.forEach(periode -> {
            if (!erVilkårOverstyrt(vilkåret, kontekst.getBehandlingId(), periode.getFomDato(), periode.getTomDato())) {
                Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
                RyddVilkårTyper ryddVilkårTyper = new RyddVilkårTyper(modell, repositoryProvider, behandling, kontekst);
                ryddVilkårTyper.ryddVedTilbakeføring();
                behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
            }
        });
    }

    protected boolean erVilkårOverstyrt(VilkårType vilkåret, Long behandlingId, LocalDate fom, LocalDate tom) {
        Optional<Vilkårene> resultatOpt = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        if (resultatOpt.isPresent()) {
            Vilkårene vilkårene = resultatOpt.get();
            return vilkårene.getVilkårene()
                .stream()
                .filter(vilkår -> vilkåret.equals(vilkår.getVilkårType()))
                .map(Vilkår::getPerioder)
                .flatMap(Collection::stream)
                .filter(it -> it.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)))
                .anyMatch(VilkårPeriode::getErOverstyrt);
        }
        return false;
    }
}
