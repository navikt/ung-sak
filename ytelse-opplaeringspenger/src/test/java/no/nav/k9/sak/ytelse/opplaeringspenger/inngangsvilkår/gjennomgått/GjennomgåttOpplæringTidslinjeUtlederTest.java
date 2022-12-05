package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått;

import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.IKKE_GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.IKKE_GODKJENT_INSTITUSJON;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.IKKE_GODKJENT_REISETID;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.IKKE_GODKJENT_SYKDOMSVILKÅR;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.MANGLER_VURDERING;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

class GjennomgåttOpplæringTidslinjeUtlederTest {

    private final GjennomgåttOpplæringTidslinjeUtleder gjennomgåttOpplæringTidslinjeUtleder = new GjennomgåttOpplæringTidslinjeUtleder();

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
        KursPeriode kursPeriode = new KursPeriode(søknadsperiodeFom, søknadsperiodeTom, null, null, "institusjon", null, "beskrivelse");
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
        return new VurdertOpplæringGrunnlag(1341L, null, null, vurdertOpplæringPerioderHolder);
    }

    @Test
    void ingenVurdering() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, null, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, MANGLER_VURDERING);
    }

    @Test
    void vurderingGodkjent() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæringPeriode vurdertOpplæringPeriode = new VurdertOpplæringPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeTom), true, null, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæringPeriode));

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, GODKJENT);
    }

    @Test
    void vurderingIkkeGodkjent() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæringPeriode vurdertOpplæringPeriode = new VurdertOpplæringPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeTom), false, null, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæringPeriode));

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, IKKE_GODKJENT);
    }

    @Test
    void vurderingMangler() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of());

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, MANGLER_VURDERING);
    }

    @Test
    void vurderingManglerDelvis() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæringPeriode vurdertOpplæringPeriode = new VurdertOpplæringPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeTom.minusDays(1)), true, null, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæringPeriode));

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        var forventetGodkjentTidslinje = resultat.intersection(new LocalDateTimeline<>(søknadsperiodeFom, søknadsperiodeTom.minusDays(1), true));
        assertTidslinje(forventetGodkjentTidslinje, GODKJENT);
        assertTidslinje(resultat.disjoint(forventetGodkjentTidslinje), MANGLER_VURDERING);
    }

    @Test
    void ikkeGodkjentInstitusjon() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæringPeriode vurdertOpplæringPeriode = new VurdertOpplæringPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeTom), true, null, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæringPeriode));

        setupVilkårResultat(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.IKKE_OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, IKKE_GODKJENT_INSTITUSJON);
    }

    @Test
    void ikkeGodkjentSykdom() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæringPeriode vurdertOpplæringPeriode = new VurdertOpplæringPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeTom), true, null, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæringPeriode));

        setupVilkårResultat(VilkårType.LANGVARIG_SYKDOM, Utfall.IKKE_OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, IKKE_GODKJENT_SYKDOMSVILKÅR);
    }

    @Test
    void ikkeGodkjentReisetidOver1Dag() {
        KursPeriode kursPeriode = new KursPeriode(søknadsperiodeFom.plusDays(2), søknadsperiodeTom.minusDays(2),
            DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeFom.plusDays(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeTom.minusDays(1), søknadsperiodeTom),
            "", null, "");
        Set<PerioderFraSøknad> perioderFraSøknad = setupPerioderFraSøknad(List.of(kursPeriode));

        VurdertOpplæringPeriode vurdertOpplæringPeriode = new VurdertOpplæringPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom.plusDays(2), søknadsperiodeTom.minusDays(2)), true, null, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæringPeriode));

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        var forventetGodkjentTidslinje = resultat.intersection(new LocalDateTimeline<>(søknadsperiodeFom.plusDays(2), søknadsperiodeTom.minusDays(2), true));
        assertTidslinje(forventetGodkjentTidslinje, GODKJENT);
        assertTidslinje(resultat.disjoint(forventetGodkjentTidslinje), IKKE_GODKJENT_REISETID);
    }

    @Test
    void godkjentReisetidOver1Dag() {
        DatoIntervallEntitet reisetidTil = DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeFom.plusDays(1));
        DatoIntervallEntitet reisetidHjem = DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeTom.minusDays(1), søknadsperiodeTom);
        KursPeriode kursPeriode = new KursPeriode(søknadsperiodeFom.plusDays(2), søknadsperiodeTom.minusDays(2), reisetidTil, reisetidHjem, "", null, "");
        Set<PerioderFraSøknad> perioderFraSøknad = setupPerioderFraSøknad(List.of(kursPeriode));

        VurdertReisetid vurdertReisetid = new VurdertReisetid(reisetidTil, reisetidHjem, "");
        VurdertOpplæringPeriode vurdertOpplæringPeriode = new VurdertOpplæringPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom.plusDays(2), søknadsperiodeTom.minusDays(2)), true, vurdertReisetid, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæringPeriode));

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, GODKJENT);
    }

    @Test
    void reisetidOppTil1DagGodkjennesAutomatisk() {
        DatoIntervallEntitet reisetidTil = DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeFom);
        DatoIntervallEntitet reisetidHjem = DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeTom, søknadsperiodeTom);
        KursPeriode kursPeriode = new KursPeriode(søknadsperiodeFom.plusDays(1), søknadsperiodeTom.minusDays(1), reisetidTil, reisetidHjem, "", null, "");
        Set<PerioderFraSøknad> perioderFraSøknad = setupPerioderFraSøknad(List.of(kursPeriode));

        VurdertOpplæringPeriode vurdertOpplæringPeriode = new VurdertOpplæringPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom.plusDays(1), søknadsperiodeTom.minusDays(1)), true, null, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæringPeriode));

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = gjennomgåttOpplæringTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, GODKJENT);
    }

    private void assertTidslinje(LocalDateTimeline<OpplæringGodkjenningStatus> tidslinje, OpplæringGodkjenningStatus forventetStatus) {
        assertThat(tidslinje.disjoint(tidslinje.filterValue(v -> Objects.equals(v, forventetStatus)))).isEmpty();
    }
}
