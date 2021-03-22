package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import java.time.LocalDate;
import java.util.NavigableSet;
import java.util.Set;
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
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
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
        var oppdatertResultatBuilder = justerVilkårsperioderEtterSykdom(vilkårene, perioder, perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VilkårType.MEDLEMSKAPSVILKÅRET));

        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), oppdatertResultatBuilder.build());

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    VilkårResultatBuilder justerVilkårsperioderEtterSykdom(Vilkårene vilkårene, NavigableSet<DatoIntervallEntitet> sykdomsPerioderTilVurdering, NavigableSet<DatoIntervallEntitet> medlemskapsPerioderTilVurdering) {
        var innvilgetePerioder = vilkårene.getVilkår(VILKÅRET).orElseThrow()
            .getPerioder()
            .stream()
            .filter(it -> sykdomsPerioderTilVurdering.stream().anyMatch(at -> at.overlapper(it.getPeriode())))
            .filter(it -> Utfall.OPPFYLT.equals(it.getUtfall()))
            .collect(Collectors.toSet());

        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene);

        justerPeriodeForMedlemskap(innvilgetePerioder, resultatBuilder, medlemskapsPerioderTilVurdering);
        justerPeriodeForOpptjeningOgBeregning(innvilgetePerioder, resultatBuilder);

        return resultatBuilder;
    }

    private void justerPeriodeForOpptjeningOgBeregning(Set<VilkårPeriode> innvilgetePerioder, VilkårResultatBuilder resultatBuilder) {
        if (innvilgetePerioder.isEmpty()) {
            return;
        }

        var fom = innvilgetePerioder.stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato).min(LocalDate::compareTo).orElseThrow();
        var tom = innvilgetePerioder.stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getTomDato).max(LocalDate::compareTo).orElseThrow();

        for (VilkårType vilkårType : Set.of(VilkårType.OPPTJENINGSPERIODEVILKÅR, VilkårType.OPPTJENINGSVILKÅRET, VilkårType.BEREGNINGSGRUNNLAGVILKÅR)) {
            var vilkårBuilder = resultatBuilder.hentBuilderFor(vilkårType);

            vilkårBuilder = vilkårBuilder.tilbakestill(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
            for (VilkårPeriode vilkårPeriode : innvilgetePerioder) {
                vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(vilkårPeriode.getPeriode()).medUtfall(Utfall.IKKE_VURDERT));
            }
            resultatBuilder.leggTil(vilkårBuilder);
        }
    }

    private void justerPeriodeForMedlemskap(Set<VilkårPeriode> innvilgetePerioder, VilkårResultatBuilder resultatBuilder, NavigableSet<DatoIntervallEntitet> perioder) {

        if (perioder.isEmpty()) {
            return;
        }

        if (perioder.size() > 1) {
            throw new IllegalStateException("Fant flere perioder med medlemskapsvurdering enn forventet");
        }

        var originFom = perioder.stream().map(DatoIntervallEntitet::getFomDato).min(LocalDate::compareTo).orElseThrow();
        var fom = innvilgetePerioder.stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato).min(LocalDate::compareTo).orElseThrow();

        if (fom.isAfter(originFom)) {

            var tom = perioder.stream().map(DatoIntervallEntitet::getTomDato).max(LocalDate::compareTo).orElseThrow();

            var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET);
            vilkårBuilder = vilkårBuilder.tilbakestill(DatoIntervallEntitet.fraOgMedTilOgMed(originFom, fom.minusDays(1)));
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(fom, tom));
            resultatBuilder.leggTil(vilkårBuilder);
        }
    }

}
