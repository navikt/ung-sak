package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import java.time.LocalDate;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
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

    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    PostSykdomOgKontinuerligTilsynSteg() {
        // CDI
    }

    @Inject
    public PostSykdomOgKontinuerligTilsynSteg(BehandlingRepositoryProvider repositoryProvider,
                                              @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        final var perioder = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VilkårType.OPPTJENINGSVILKÅRET); // OK
        final var perioderMedlemskap = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VilkårType.MEDLEMSKAPSVILKÅRET);

        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        var oppdatertResultatBuilder = justerVilkårsperioderEtterSykdom(vilkårene, perioder, perioderMedlemskap);

        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), oppdatertResultatBuilder.build());

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    VilkårResultatBuilder justerVilkårsperioderEtterSykdom(Vilkårene vilkårene, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, NavigableSet<DatoIntervallEntitet> medlemskapsPerioderTilVurdering) {
        var innvilgetePerioder = finnInnvilgedePerioder(vilkårene, perioderTilVurdering);

        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(perioderTilVurderingTjeneste.maksMellomliggendePeriodeAvstand());

        justerPeriodeForMedlemskap(innvilgetePerioder, resultatBuilder, medlemskapsPerioderTilVurdering);
        justerPeriodeForOpptjeningOgBeregning(innvilgetePerioder, resultatBuilder, perioderTilVurdering);

        return resultatBuilder;
    }

    private Set<VilkårPeriode> finnInnvilgedePerioder(Vilkårene vilkårene,
                                                      NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        var s1 = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR).orElseThrow().getPerioder().stream();
        var s2 = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_18_ÅR).orElseThrow().getPerioder().stream();
        return Stream.concat(s1, s2)
            .filter(it -> perioderTilVurdering.stream().anyMatch(at -> at.overlapper(it.getPeriode())))
            .filter(it -> Utfall.OPPFYLT.equals(it.getUtfall()))
            .collect(Collectors.toSet());
    }

    private void justerPeriodeForOpptjeningOgBeregning(Set<VilkårPeriode> innvilgetePerioder, VilkårResultatBuilder resultatBuilder, NavigableSet<DatoIntervallEntitet> sykdomsPerioderTilVurdering) {
        if (innvilgetePerioder.isEmpty()) {
            return;
        }

        for (VilkårType vilkårType : Set.of(VilkårType.OPPTJENINGSPERIODEVILKÅR, VilkårType.OPPTJENINGSVILKÅRET, VilkårType.BEREGNINGSGRUNNLAGVILKÅR)) {
            var vilkårBuilder = resultatBuilder.hentBuilderFor(vilkårType);
            for (DatoIntervallEntitet periodeTilVurdering : sykdomsPerioderTilVurdering) {
                if (harSkjæringstidspunktetBlittFlyttet(periodeTilVurdering, innvilgetePerioder)) {
                    var relevantInnvilgetPeriode = finnRelevantPeriode(periodeTilVurdering, innvilgetePerioder);

                    if (harSkjæringstidspunktetBlittFlyttetFremITid(periodeTilVurdering, relevantInnvilgetPeriode)) {
                        var periodeSomSkalTilbakestilles = regnUtPeriodeSomSkalTilbakestilles(periodeTilVurdering, relevantInnvilgetPeriode);
                        vilkårBuilder = vilkårBuilder.tilbakestill(periodeSomSkalTilbakestilles);
                    }
                    vilkårBuilder = vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(relevantInnvilgetPeriode)
                        .medPeriode(relevantInnvilgetPeriode)
                        .medUtfall(Utfall.IKKE_VURDERT));
                }
            }
            resultatBuilder.leggTil(vilkårBuilder);
        }
    }

    private boolean harSkjæringstidspunktetBlittFlyttetFremITid(DatoIntervallEntitet periodeTilVurdering, DatoIntervallEntitet relevantInnvilgetPeriode) {
        Objects.requireNonNull(periodeTilVurdering);
        Objects.requireNonNull(relevantInnvilgetPeriode);

        return periodeTilVurdering.getFomDato().isBefore(relevantInnvilgetPeriode.getFomDato());
    }

    private DatoIntervallEntitet regnUtPeriodeSomSkalTilbakestilles(DatoIntervallEntitet periodeTilVurdering, DatoIntervallEntitet relevantInnvilgetPeriode) {
        var originFom = periodeTilVurdering.getFomDato();
        var fom = relevantInnvilgetPeriode.getFomDato();

        return DatoIntervallEntitet.fraOgMedTilOgMed(originFom, fom.minusDays(1));
    }

    private boolean harSkjæringstidspunktetBlittFlyttet(DatoIntervallEntitet datoIntervallEntitet, Set<VilkårPeriode> innvilgetePerioder) {
        if (innvilgetePerioder.isEmpty()) {
            return false;
        }
        if (innvilgetePerioder.stream().noneMatch(it -> it.getPeriode().overlapper(datoIntervallEntitet))) {
            return false;
        }
        var relevantPeriode = finnRelevantPeriode(datoIntervallEntitet, innvilgetePerioder);

        return datoIntervallEntitet.getFomDato() != relevantPeriode.getFomDato();
    }

    public DatoIntervallEntitet finnRelevantPeriode(DatoIntervallEntitet datoIntervallEntitet, Set<VilkårPeriode> innvilgetePerioder) {
        var relevantePerioder = innvilgetePerioder.stream()
            .filter(it -> it.getPeriode().overlapper(datoIntervallEntitet))
            .collect(Collectors.toSet());
        if (relevantePerioder.size() == 1) {
            return relevantePerioder.stream().findFirst().map(VilkårPeriode::getPeriode).orElseThrow();
        } else {
            throw new IllegalStateException("Fant flere vilkårsperioder som overlapper.. " + relevantePerioder);
        }
    }

    private void justerPeriodeForMedlemskap(Set<VilkårPeriode> innvilgetePerioder, VilkårResultatBuilder resultatBuilder, NavigableSet<DatoIntervallEntitet> perioder) {

        if (perioder.isEmpty()) {
            return;
        }
        if (innvilgetePerioder.isEmpty()) {
            return;
        }

        if (perioder.size() > 1) {
            throw new IllegalStateException("Fant flere perioder med medlemskapsvurdering enn forventet");
        }

        var originFom = perioder.stream().map(DatoIntervallEntitet::getFomDato).min(LocalDate::compareTo).orElseThrow();
        var fom = innvilgetePerioder.stream().map(VilkårPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato).min(LocalDate::compareTo).orElseThrow();

        if (fom.isAfter(originFom)) {
            var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET);
            vilkårBuilder = vilkårBuilder.tilbakestill(DatoIntervallEntitet.fraOgMedTilOgMed(originFom, fom.minusDays(1)));
            resultatBuilder.leggTil(vilkårBuilder);
        } else if (fom.isBefore(originFom)) {
            var tom = perioder.stream().map(DatoIntervallEntitet::getTomDato).max(LocalDate::compareTo).orElseThrow();
            var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET);
            vilkårBuilder = vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(fom, tom)
                .medUtfall(Utfall.IKKE_VURDERT));
            resultatBuilder.leggTil(vilkårBuilder);
        }
    }

}
