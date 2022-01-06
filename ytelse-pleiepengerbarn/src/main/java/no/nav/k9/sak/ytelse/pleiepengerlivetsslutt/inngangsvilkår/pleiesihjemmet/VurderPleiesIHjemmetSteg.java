package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.medisinsk.Pleiegrad;
import no.nav.k9.kodeverk.vilkår.VilkårType;
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
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleiebehovBuilder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleieperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell.Pleielokasjon;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell.PleiesHjemmeVilkårResultat;

@BehandlingStegRef(kode = "VURDER_PLEIES_HJEMME")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PPN")
@ApplicationScoped
public class VurderPleiesIHjemmetSteg implements BehandlingSteg {

    public static final VilkårType VILKÅRET = VilkårType.PLEIES_I_HJEMMMET;
    private BehandlingRepositoryProvider repositoryProvider;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private PleiebehovResultatRepository resultatRepository;
    private PleiesHjemmeVilkårTjeneste pleiesHjemmeVilkårTjeneste = new PleiesHjemmeVilkårTjeneste();

    VurderPleiesIHjemmetSteg() {
        // CDI
    }

    @Inject
    public VurderPleiesIHjemmetSteg(BehandlingRepositoryProvider repositoryProvider,
                                    @FagsakYtelseTypeRef("PPN") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                    SykdomGrunnlagRepository sykdomGrunnlagRepository,
                                    PleiebehovResultatRepository resultatRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.repositoryProvider = repositoryProvider;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.resultatRepository = resultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var sykdomGrunnlagBehandling = sykdomGrunnlagRepository.hentGrunnlagForBehandling(behandling.getUuid()).orElseThrow();
        var perioder = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VILKÅRET);

        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårene);

        for (DatoIntervallEntitet periode : perioder) {
            var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VILKÅRET);
            var vilkårData = pleiesHjemmeVilkårTjeneste.vurderPerioder(periode, sykdomGrunnlagBehandling);

            oppdaterBehandlingMedVilkårresultat(vilkårData, vilkårBuilder, vilkårResultatBuilder);
            oppdaterResultatStruktur(kontekst, periode, vilkårData);
        }
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), vilkårResultatBuilder.build());

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void oppdaterResultatStruktur(BehandlingskontrollKontekst kontekst, DatoIntervallEntitet periodeTilVurdering, VilkårData vilkårData) {
        var nåværendeResultat = resultatRepository.hentHvisEksisterer(kontekst.getBehandlingId());
        var builder = nåværendeResultat.map(PleiebehovResultat::getPleieperioder).map(EtablertPleiebehovBuilder::builder).orElse(EtablertPleiebehovBuilder.builder());
        builder.tilbakeStill(periodeTilVurdering);
        final var vilkårresultat = ((PleiesHjemmeVilkårResultat) vilkårData.getEkstraVilkårresultat());

        vilkårresultat.getPleieperioder().forEach(periode -> {
            Pleiegrad pleiegrad = periode.getPleielokasjon() == Pleielokasjon.HJEMME ? Pleiegrad.TILSYN_LIVETS_SLUTT : Pleiegrad.INGEN;
            var etablertPleieperiode = new EtablertPleieperiode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFraOgMed(), periode.getTilOgMed()), pleiegrad);
            builder.leggTil(etablertPleieperiode);
        });
        resultatRepository.lagreOgFlush(kontekst.getBehandlingId(), builder);
    }

    private void oppdaterBehandlingMedVilkårresultat(VilkårData vilkårData, VilkårBuilder vilkårBuilder, VilkårResultatBuilder vilkårResultatBuilder) {
        final var periode = vilkårData.getPeriode();
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato())
            .medUtfall(vilkårData.getUtfallType())
            .medMerknadParametere(vilkårData.getMerknadParametere())
            .medRegelEvaluering(vilkårData.getRegelEvaluering())
            .medRegelInput(vilkårData.getRegelInput())
            .medAvslagsårsak(vilkårData.getAvslagsårsak())
            .medMerknad(vilkårData.getVilkårUtfallMerknad()));
        vilkårResultatBuilder.leggTil(vilkårBuilder);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg, BehandlingStegType sisteSteg) {
        final var perioder = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VILKÅRET);
        perioder.forEach(periode -> {
            if (!erVilkårOverstyrt(kontekst.getBehandlingId(), periode.getFomDato(), periode.getTomDato())) {
                Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
                RyddVilkårTyper ryddVilkårTyper = new RyddVilkårTyper(modell, repositoryProvider, behandling, kontekst);
                ryddVilkårTyper.ryddVedTilbakeføring();
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
