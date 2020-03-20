package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.medisinsk;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.pleiebehov.PleiebehovBuilder;
import no.nav.k9.sak.behandlingslager.behandling.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.behandlingslager.behandling.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.pleiebehov.Pleieperiode;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.RyddVilkårTyper;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkaar.VilkårData;
import no.nav.k9.sak.inngangsvilkaar.medisinsk.MedisinskVilkårTjeneste;
import no.nav.k9.sak.inngangsvilkaar.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.inngangsvilkaar.regelmodell.medisinsk.MedisinskVilkårResultat;
import no.nav.k9.sak.inngangsvilkaar.regelmodell.medisinsk.PleiePeriode;
import no.nav.k9.sak.inngangsvilkaar.regelmodell.medisinsk.Pleiegrad;

@BehandlingStegRef(kode = "VURDER_MEDISINSK")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderSykdomOgKontinuerligTilsynSteg implements BehandlingSteg {

    public static final VilkårType VILKÅRET = VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR;
    private BehandlingRepositoryProvider repositoryProvider;
    private PleiebehovResultatRepository resultatRepository;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private MedisinskVilkårTjeneste medisinskVilkårTjeneste;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    VurderSykdomOgKontinuerligTilsynSteg() {
        // CDI
    }

    @Inject
    public VurderSykdomOgKontinuerligTilsynSteg(BehandlingRepositoryProvider repositoryProvider,
                                                PleiebehovResultatRepository resultatRepository,
                                                VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                                MedisinskVilkårTjeneste medisinskVilkårTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.repositoryProvider = repositoryProvider;
        this.resultatRepository = resultatRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.medisinskVilkårTjeneste = medisinskVilkårTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        final var perioder = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VILKÅRET);
        final var vilkårData = medisinskVilkårTjeneste.vurderPerioder(kontekst, perioder);

        final var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        final var oppdaterteVilkår = oppdaterBehandlingMedVilkårresultat(vilkårData, vilkårene);

        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), oppdaterteVilkår);

        // Lagre resultatstruktur
        oppdaterResultatStruktur(kontekst, perioder, vilkårData);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void oppdaterResultatStruktur(BehandlingskontrollKontekst kontekst, Set<DatoIntervallEntitet> perioder, VilkårData vilkårData) {
        final var nåværendeResultat = resultatRepository.hentHvisEksisterer(kontekst.getBehandlingId());
        var builder = nåværendeResultat.map(PleiebehovResultat::getPleieperioder).map(PleiebehovBuilder::builder).orElse(PleiebehovBuilder.builder());
        final DatoIntervallEntitet periodeTilVurdering = utledPeriodeTilVurdering(perioder);
        builder.tilbakeStill(periodeTilVurdering);
        final var vilkårresultat = ((MedisinskVilkårResultat) vilkårData.getEkstraVilkårresultat());

        vilkårresultat.getPleieperioder().forEach(periode -> builder.leggTil(utledPeriode(periode)));
        resultatRepository.lagreOgFlush(behandlingRepository.hentBehandling(kontekst.getBehandlingId()), builder);
    }

    private Pleieperiode utledPeriode(PleiePeriode periode) {
        return new Pleieperiode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFraOgMed(), periode.getTilOgMed()), no.nav.k9.kodeverk.medisinsk.Pleiegrad.fraKode(periode.getGrad().name()));
    }

    private DatoIntervallEntitet utledPeriodeTilVurdering(Set<DatoIntervallEntitet> perioder) {
        var startDato = perioder.stream().map(DatoIntervallEntitet::getFomDato).min(LocalDate::compareTo).orElse(LocalDate.now());
        var sluttDato = perioder.stream().map(DatoIntervallEntitet::getTomDato).max(LocalDate::compareTo).orElse(LocalDate.now());

        return DatoIntervallEntitet.fraOgMedTilOgMed(startDato, sluttDato);
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

        if (vilkårData.getUtfallType().equals(Utfall.OPPFYLT)) {
            final var ekstraVilkårresultat = (MedisinskVilkårResultat) vilkårData.getEkstraVilkårresultat();
            ekstraVilkårresultat.getPleieperioder()
                .stream()
                .filter(it -> Pleiegrad.INGEN.equals(it.getGrad()))
                .forEach(it -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(it.getFraOgMed(), it.getTilOgMed())
                    .medUtfall(Utfall.IKKE_OPPFYLT)
                    .medMerknadParametere(vilkårData.getMerknadParametere())
                    .medRegelEvaluering(vilkårData.getRegelEvaluering())
                    .medRegelInput(vilkårData.getRegelInput())
                    .medAvslagsårsak(Avslagsårsak.IKKE_BEHOV_FOR_KONTINUERLIG_TILSYN_OG_PLEIE_PÅ_BAKGRUNN_AV_SYKDOM)
                    .medMerknad(vilkårData.getVilkårUtfallMerknad())));
        }
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
