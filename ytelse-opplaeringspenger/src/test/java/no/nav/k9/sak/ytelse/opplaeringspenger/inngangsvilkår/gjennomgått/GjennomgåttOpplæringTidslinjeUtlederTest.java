package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått;

import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.IKKE_GODKJENT_OPPLÆRING;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.IKKE_GODKJENT_REISETID;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.MANGLER_VURDERING_OPPLÆRING;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.MANGLER_VURDERING_REISETID;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPerioderHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetid;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetidHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

class GjennomgåttOpplæringTidslinjeUtlederTest {

    private final GjennomgåttOpplæringTidslinjeUtleder gjennomgåttOpplæringTidslinjeUtleder = new GjennomgåttOpplæringTidslinjeUtleder();

    private final LocalDateTime nå = LocalDateTime.now();
    private final LocalDate søknadsperiodeFom = LocalDate.now().minusWeeks(2);
    private final LocalDate søknadsperiodeTom = LocalDate.now();
    private final JournalpostId journalpostId = new JournalpostId("234");
    private LocalDateTimeline<Boolean> søknadsperiode;
    private final VilkårResultatBuilder vilkårResultatBuilder = new VilkårResultatBuilder();

    @BeforeEach
    void setup() {
        søknadsperiode = TidslinjeUtil.tilTidslinjeKomprimert(new TreeSet<>(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeTom))));
        setupVilkårResultat(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
    }

    private void setupVilkårResultat(VilkårType vilkårType, Utfall utfall, LocalDate fom, LocalDate tom) {
        vilkårResultatBuilder.leggTil(vilkårResultatBuilder.hentBuilderFor(vilkårType)
            .leggTil(new VilkårPeriodeBuilder().medUtfall(utfall).medPeriode(fom, tom)));
    }

    private Set<PerioderFraSøknad> setupEnkelKursperiode() {
        KursPeriode kursPeriode = new KursPeriode(søknadsperiodeFom, søknadsperiodeTom, null, null, "institusjon", null, null, null);
        return setupPerioderFraSøknad(List.of(kursPeriode));
    }

    private Set<PerioderFraSøknad> setupPerioderFraSøknad(List<KursPeriode> kursperioder) {
        return Set.of(new PerioderFraSøknad(journalpostId,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            kursperioder));
    }

    private VurdertOpplæringGrunnlag setupVurderingsgrunnlag(List<VurdertOpplæringPeriode> vurdertOpplæringPerioder) {
        VurdertOpplæringPerioderHolder vurdertOpplæringPerioderHolder = new VurdertOpplæringPerioderHolder(vurdertOpplæringPerioder);
        return new VurdertOpplæringGrunnlag(1341L, null, null, vurdertOpplæringPerioderHolder, null);
    }

    private VurdertOpplæringGrunnlag setupVurderingsgrunnlag(List<VurdertOpplæringPeriode> vurdertOpplæringPerioder, List<VurdertReisetid> vurdertReisetid) {
        VurdertOpplæringPerioderHolder vurdertOpplæringPerioderHolder = new VurdertOpplæringPerioderHolder(vurdertOpplæringPerioder);
        VurdertReisetidHolder vurdertReisetidHolder = new VurdertReisetidHolder(vurdertReisetid);
        return new VurdertOpplæringGrunnlag(1341L, null, null, vurdertOpplæringPerioderHolder, vurdertReisetidHolder);
    }

    @Test
    void ingenVurdering() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, null, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, MANGLER_VURDERING_OPPLÆRING);
    }

    @Test
    void vurderingGodkjent() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæringPeriode vurdertOpplæringPeriode = new VurdertOpplæringPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeTom), true, "", "", nå, List.of());
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæringPeriode));

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, GODKJENT);
    }

    @Test
    void vurderingIkkeGodkjent() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæringPeriode vurdertOpplæringPeriode = new VurdertOpplæringPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeTom), false, "", "", nå, List.of());
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæringPeriode));

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, IKKE_GODKJENT_OPPLÆRING);
    }

    @Test
    void vurderingMangler() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of());

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, MANGLER_VURDERING_OPPLÆRING);
    }

    @Test
    void vurderingManglerDelvis() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæringPeriode vurdertOpplæringPeriode = new VurdertOpplæringPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeTom.minusDays(1)), true, "", "", nå, List.of());
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæringPeriode));

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        var forventetGodkjentTidslinje = resultat.intersection(new LocalDateTimeline<>(søknadsperiodeFom, søknadsperiodeTom.minusDays(1), true));
        assertTidslinje(forventetGodkjentTidslinje, GODKJENT);
        assertTidslinje(resultat.disjoint(forventetGodkjentTidslinje), MANGLER_VURDERING_OPPLÆRING);
    }

    @Test
    void ikkeGodkjentInstitusjon() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæringPeriode vurdertOpplæringPeriode = new VurdertOpplæringPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeTom), true, "", "", nå, List.of());
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæringPeriode));

        setupVilkårResultat(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.IKKE_OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertThat(resultat).isEmpty();
    }

    @Test
    void ikkeGodkjentSykdom() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæringPeriode vurdertOpplæringPeriode = new VurdertOpplæringPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeTom), true, "", "", nå, List.of());
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæringPeriode));

        setupVilkårResultat(VilkårType.LANGVARIG_SYKDOM, Utfall.IKKE_OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertThat(resultat).isEmpty();
    }

    @Test
    void ikkeVurdertReisetidOver1Dag() {
        KursPeriode kursPeriode = new KursPeriode(søknadsperiodeFom.plusDays(2), søknadsperiodeTom.minusDays(2),
            DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeFom.plusDays(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeTom.minusDays(1), søknadsperiodeTom),
            "", null, null, null);
        Set<PerioderFraSøknad> perioderFraSøknad = setupPerioderFraSøknad(List.of(kursPeriode));

        VurdertOpplæringPeriode vurdertOpplæringPeriode = new VurdertOpplæringPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom.plusDays(2), søknadsperiodeTom.minusDays(2)), true, "", "", nå, List.of());
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæringPeriode));

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        var forventetGodkjentTidslinje = resultat.intersection(new LocalDateTimeline<>(søknadsperiodeFom.plusDays(2), søknadsperiodeTom.minusDays(2), true));
        assertTidslinje(forventetGodkjentTidslinje, GODKJENT);
        assertTidslinje(resultat.disjoint(forventetGodkjentTidslinje), MANGLER_VURDERING_REISETID);
    }

    @Test
    void vurdertReisetidOver1Dag() {
        DatoIntervallEntitet reisetidTil = DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeFom.plusDays(1));
        DatoIntervallEntitet reisetidHjem = DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeTom.minusDays(1), søknadsperiodeTom);
        DatoIntervallEntitet opplæringPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom.plusDays(2), søknadsperiodeTom.minusDays(2));
        KursPeriode kursPeriode = new KursPeriode(opplæringPeriode, reisetidTil, reisetidHjem, "", null, null, null);
        Set<PerioderFraSøknad> perioderFraSøknad = setupPerioderFraSøknad(List.of(kursPeriode));

        VurdertOpplæringPeriode vurdertOpplæringPeriode = new VurdertOpplæringPeriode(opplæringPeriode, true, "", "", nå, List.of());
        List<VurdertReisetid> vurdertReisetid = List.of(
            new VurdertReisetid(reisetidTil, true, "ja", "", nå),
            new VurdertReisetid(reisetidHjem, false, "nei", "", nå));
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæringPeriode), vurdertReisetid);

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        var forventetGodkjentTidslinje = resultat.intersection(new LocalDateTimeline<>(reisetidTil.getFomDato(), opplæringPeriode.getTomDato(), true));
        assertTidslinje(forventetGodkjentTidslinje, GODKJENT);
        assertTidslinje(resultat.disjoint(forventetGodkjentTidslinje), IKKE_GODKJENT_REISETID);
    }

    @Test
    void reisetidOppTil1DagGodkjennesAutomatisk() {
        DatoIntervallEntitet reisetidTil = DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeFom);
        DatoIntervallEntitet reisetidHjem = DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeTom, søknadsperiodeTom);
        KursPeriode kursPeriode = new KursPeriode(søknadsperiodeFom.plusDays(1), søknadsperiodeTom.minusDays(1), reisetidTil, reisetidHjem, "", null, null, null);
        Set<PerioderFraSøknad> perioderFraSøknad = setupPerioderFraSøknad(List.of(kursPeriode));

        VurdertOpplæringPeriode vurdertOpplæringPeriode = new VurdertOpplæringPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom.plusDays(1), søknadsperiodeTom.minusDays(1)), true, "", "", nå, List.of());
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæringPeriode));

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, GODKJENT);
    }

    private void assertTidslinje(LocalDateTimeline<OpplæringGodkjenningStatus> tidslinje, OpplæringGodkjenningStatus forventetStatus) {
        assertThat(tidslinje).isNotEmpty();
        assertThat(tidslinje.disjoint(tidslinje.filterValue(v -> Objects.equals(v, forventetStatus)))).isEmpty();
    }
}
