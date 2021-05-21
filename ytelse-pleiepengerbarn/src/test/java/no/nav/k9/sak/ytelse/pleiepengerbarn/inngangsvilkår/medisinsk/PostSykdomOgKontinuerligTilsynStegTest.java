package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

class PostSykdomOgKontinuerligTilsynStegTest {

    private BehandlingRepositoryProvider mockProvider = mock(BehandlingRepositoryProvider.class); // Brukes ikke, men kan ikke være null
    private PostSykdomOgKontinuerligTilsynSteg steg = new PostSykdomOgKontinuerligTilsynSteg(mockProvider, new VilkårsPerioderTilVurderingTjeneste() {
        @Override
        public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
            return null;
        }

        @Override
        public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId) {
            return null;
        }

        @Override
        public int maksMellomliggendePeriodeAvstand() {
            return 0;
        }

        @Override
        public KantIKantVurderer getKantIKantVurderer() {
            return new PåTversAvHelgErKantIKantVurderer();
        }
    });

    @Test
    void skal_justere_utfall_ved_perioder_med_avslag_på_medisinsk() {
        var builder = Vilkårene.builder();
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        var oppfyltPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(28), LocalDate.now());
        var avslåttPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(32), LocalDate.now().minusDays(29));
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(avslåttPeriode.getFomDato(), oppfyltPeriode.getTomDato());
        var perioderTilVurdering = List.of(periodeTilVurdering);

        builder.leggTilIkkeVurderteVilkår(perioderTilVurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
            VilkårType.MEDLEMSKAPSVILKÅRET,
            VilkårType.OPPTJENINGSPERIODEVILKÅR,
            VilkårType.OPPTJENINGSVILKÅRET);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(oppfyltPeriode)
            .medUtfall(Utfall.OPPFYLT))
            .leggTil(vilkårBuilder.hentBuilderFor(avslåttPeriode)
                .medUtfall(Utfall.IKKE_OPPFYLT));
        builder.leggTil(vilkårBuilder);

        var vilkårBuilder18år = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        builder.leggTil(vilkårBuilder18år);

        var resultatBuilder = steg.justerVilkårsperioderEtterSykdom(builder.build(), new TreeSet<>(perioderTilVurdering));

        var oppdaterteVilkår = resultatBuilder.build();

        assertThat(oppdaterteVilkår).isNotNull();

        for (Vilkår vilkår : oppdaterteVilkår.getVilkårene()) {
            if (VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).hasSize(2);
                assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode)).contains(oppfyltPeriode, avslåttPeriode);
            } else if (VilkårType.MEDISINSKEVILKÅR_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).isEmpty();
            } else {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().get(0).getPeriode()).isEqualTo(oppfyltPeriode);
            }
        }
    }

    @Test
    void skal_justere_utfall_ved_perioder_med_avslag_på_medisinsk_med_18årsvurdering() {
        var builder = Vilkårene.builder();
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        var oppfyltPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(28), LocalDate.now());
        var avslåttPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(32), LocalDate.now().minusDays(29));
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(avslåttPeriode.getFomDato(), oppfyltPeriode.getTomDato());
        var perioderTilVurdering = List.of(periodeTilVurdering);

        builder.leggTilIkkeVurderteVilkår(perioderTilVurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
            VilkårType.MEDLEMSKAPSVILKÅRET,
            VilkårType.OPPTJENINGSPERIODEVILKÅR,
            VilkårType.OPPTJENINGSVILKÅRET);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(oppfyltPeriode)
            .medUtfall(Utfall.OPPFYLT))
            .leggTil(vilkårBuilder.hentBuilderFor(avslåttPeriode)
                .medUtfall(Utfall.IKKE_OPPFYLT));
        builder.leggTil(vilkårBuilder);

        var vilkårBuilder18år = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        builder.leggTil(vilkårBuilder18år);

        var resultatBuilder = steg.justerVilkårsperioderEtterSykdom(builder.build(), new TreeSet<>(perioderTilVurdering));

        var oppdaterteVilkår = resultatBuilder.build();

        assertThat(oppdaterteVilkår).isNotNull();

        for (Vilkår vilkår : oppdaterteVilkår.getVilkårene()) {
            if (VilkårType.MEDISINSKEVILKÅR_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).hasSize(2);
                assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode)).contains(oppfyltPeriode, avslåttPeriode);
            } else if (VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).isEmpty();
            } else {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().get(0).getPeriode()).isEqualTo(oppfyltPeriode);
            }
        }
    }

    @Test
    void skal_legge_tilbake_periode_hvis_sykdom_godkjent_igjen() {
        var builder = Vilkårene.builder();
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        var oppfyltPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(28), LocalDate.now().minusDays(20));
        var avslåttPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(19), LocalDate.now().minusDays(15));
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(oppfyltPeriode.getFomDato(), avslåttPeriode.getTomDato());
        var perioderTilVurdering = List.of(periodeTilVurdering);

        builder.leggTilIkkeVurderteVilkår(List.of(oppfyltPeriode), VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
            VilkårType.MEDLEMSKAPSVILKÅRET,
            VilkårType.OPPTJENINGSPERIODEVILKÅR,
            VilkårType.OPPTJENINGSVILKÅRET);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periodeTilVurdering)
            .medUtfall(Utfall.OPPFYLT));
        builder.leggTil(vilkårBuilder);

        var vilkårBuilder18år = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        builder.leggTil(vilkårBuilder18år);

        var resultatBuilder = steg.justerVilkårsperioderEtterSykdom(builder.build(), new TreeSet<>(perioderTilVurdering));

        var oppdaterteVilkår = resultatBuilder.build();

        assertThat(oppdaterteVilkår).isNotNull();

        for (Vilkår vilkår : oppdaterteVilkår.getVilkårene()) {
            if (VilkårType.MEDISINSKEVILKÅR_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode)).contains(periodeTilVurdering);
            } else if (VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).isEmpty();
            } else {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().get(0).getPeriode()).isEqualTo(periodeTilVurdering);
            }
        }
    }

    @Test
    void skal_IKKE_justere_utfall_ved_fullstendig_avslag_på_medisinsk() {
        var builder = Vilkårene.builder();
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        var avslåttPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(32), LocalDate.now().minusDays(29));
        var periodeTilVurdering = avslåttPeriode;
        var perioderTilVurdering = List.of(periodeTilVurdering);

        builder.leggTilIkkeVurderteVilkår(perioderTilVurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
            VilkårType.MEDLEMSKAPSVILKÅRET,
            VilkårType.OPPTJENINGSPERIODEVILKÅR,
            VilkårType.OPPTJENINGSVILKÅRET);
        vilkårBuilder
            .leggTil(vilkårBuilder.hentBuilderFor(avslåttPeriode)
                .medUtfall(Utfall.IKKE_OPPFYLT));
        builder.leggTil(vilkårBuilder);

        var vilkårBuilder18år = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        builder.leggTil(vilkårBuilder18år);

        var resultatBuilder = steg.justerVilkårsperioderEtterSykdom(builder.build(), new TreeSet<>(perioderTilVurdering));

        var oppdaterteVilkår = resultatBuilder.build();

        assertThat(oppdaterteVilkår).isNotNull();

        for (Vilkår vilkår : oppdaterteVilkår.getVilkårene()) {
            if (VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode)).contains(avslåttPeriode);
            } else if (VilkårType.MEDISINSKEVILKÅR_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).isEmpty();
            } else {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().get(0).getPeriode()).isEqualTo(periodeTilVurdering);
            }
        }
    }

    @Test
    void skal_IKKE_justere_utfall_ved_fullstendig_innvilgselse_på_medisinsk() {
        var builder = Vilkårene.builder();
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        var oppfyltPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(28), LocalDate.now());
        var periodeTilVurdering = oppfyltPeriode;
        var perioderTilVurdering = List.of(periodeTilVurdering);

        builder.leggTilIkkeVurderteVilkår(perioderTilVurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
            VilkårType.MEDLEMSKAPSVILKÅRET,
            VilkårType.OPPTJENINGSPERIODEVILKÅR,
            VilkårType.OPPTJENINGSVILKÅRET);
        vilkårBuilder
            .leggTil(vilkårBuilder.hentBuilderFor(oppfyltPeriode)
                .medUtfall(Utfall.OPPFYLT));
        builder.leggTil(vilkårBuilder);

        var vilkårBuilder18år = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        builder.leggTil(vilkårBuilder18år);

        var resultatBuilder = steg.justerVilkårsperioderEtterSykdom(builder.build(), new TreeSet<>(perioderTilVurdering));

        var oppdaterteVilkår = resultatBuilder.build();

        assertThat(oppdaterteVilkår).isNotNull();

        for (Vilkår vilkår : oppdaterteVilkår.getVilkårene()) {
            if (VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode)).contains(oppfyltPeriode);
            } else if (VilkårType.MEDISINSKEVILKÅR_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).isEmpty();
            } else {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().get(0).getPeriode()).isEqualTo(periodeTilVurdering);
            }
        }
    }
}
