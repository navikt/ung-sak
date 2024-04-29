package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.IKKE_GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.MANGLER_VURDERING;
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
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertNødvendighet;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertNødvendighetHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

class VurderNødvendighetTidslinjeUtlederTest {

    private final VurderNødvendighetTidslinjeUtleder vurderNødvendighetTidslinjeUtleder = new VurderNødvendighetTidslinjeUtleder();

    private final LocalDate søknadsperiodeFom = LocalDate.now().minusWeeks(2);
    private final LocalDate søknadsperiodeTom = LocalDate.now();
    private final JournalpostId journalpost1 = new JournalpostId("234");
    private final JournalpostId journalpost2 = new JournalpostId("567");
    private LocalDateTimeline<Boolean> søknadsperiode;

    @BeforeEach
    void setup() {
        søknadsperiode = TidslinjeUtil.tilTidslinjeKomprimert(new TreeSet<>(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeTom))));
    }

    private void setupVilkårResultat(VilkårResultatBuilder vilkårResultatBuilder, VilkårType vilkårType, Utfall utfall, LocalDate fom, LocalDate tom) {
        vilkårResultatBuilder.leggTil(vilkårResultatBuilder.hentBuilderFor(vilkårType)
            .leggTil(new VilkårPeriodeBuilder().medUtfall(utfall).medPeriode(fom, tom)));
    }

    private Set<PerioderFraSøknad> setupEnkelKursperiode() {
        KursPeriode kursPeriode = lagKursperiode(søknadsperiodeFom, søknadsperiodeTom);
        return Set.of(setupPerioderFraSøknad(journalpost1, List.of(kursPeriode)));
    }

    private PerioderFraSøknad setupPerioderFraSøknad(JournalpostId journalpostId, List<KursPeriode> kursperioder) {
        return new PerioderFraSøknad(journalpostId,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            kursperioder);
    }

    private KursPeriode lagKursperiode(LocalDate fom, LocalDate tom) {
        return new KursPeriode(fom, tom, null, null, null, null, null);
    }

    private VurdertOpplæringGrunnlag setupVurderingsgrunnlag(List<VurdertNødvendighet> vurdertNødvendighet) {
        VurdertNødvendighetHolder vurdertNødvendighetHolder = new VurdertNødvendighetHolder(vurdertNødvendighet);
        return new VurdertOpplæringGrunnlag(1344L, null, vurdertNødvendighetHolder, null, null);
    }

    @Test
    void ingenVurdering() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();

        var vilkårResultatBuilder = new VilkårResultatBuilder();
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        var vilkårene = vilkårResultatBuilder.build();

        var resultat = vurderNødvendighetTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, null, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, MANGLER_VURDERING);
    }

    @Test
    void vurderingGodkjent() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertNødvendighet vurdertNødvendighet = new VurdertNødvendighet(journalpost1, true, "", "", LocalDateTime.now(), List.of());
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertNødvendighet));

        var vilkårResultatBuilder = new VilkårResultatBuilder();
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        var vilkårene = vilkårResultatBuilder.build();

        var resultat = vurderNødvendighetTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, GODKJENT);
    }

    @Test
    void vurderingIkkeGodkjent() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertNødvendighet vurdertNødvendighet = new VurdertNødvendighet(journalpost1, false, "", "", LocalDateTime.now(), List.of());
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertNødvendighet));

        var vilkårResultatBuilder = new VilkårResultatBuilder();
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        var vilkårene = vilkårResultatBuilder.build();

        var resultat = vurderNødvendighetTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, IKKE_GODKJENT);
    }

    @Test
    void vurderingMangler() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertNødvendighet vurdertNødvendighet = new VurdertNødvendighet(journalpost2, true, "", "", LocalDateTime.now(), List.of());
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertNødvendighet));

        var vilkårResultatBuilder = new VilkårResultatBuilder();
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        var vilkårene = vilkårResultatBuilder.build();

        var resultat = vurderNødvendighetTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, MANGLER_VURDERING);
    }

    @Test
    void vurderingManglerDelvis() {
        KursPeriode kursPeriode1 = lagKursperiode(søknadsperiodeFom, søknadsperiodeTom.minusDays(1));
        KursPeriode kursPeriode2 = lagKursperiode(søknadsperiodeTom, søknadsperiodeTom);
        PerioderFraSøknad perioderFraSøknad1 = setupPerioderFraSøknad(journalpost1, List.of(kursPeriode1));
        PerioderFraSøknad perioderFraSøknad2 = setupPerioderFraSøknad(journalpost2, List.of(kursPeriode2));
        VurdertNødvendighet vurdertNødvendighet = new VurdertNødvendighet(journalpost1, true, "", "", LocalDateTime.now(), List.of());
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertNødvendighet));

        var vilkårResultatBuilder = new VilkårResultatBuilder();
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        var vilkårene = vilkårResultatBuilder.build();

        var resultat = vurderNødvendighetTidslinjeUtleder.utled(vilkårene, Set.of(perioderFraSøknad1, perioderFraSøknad2), vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        var forventetGodkjentTidslinje = resultat.intersection(new LocalDateTimeline<>(søknadsperiodeFom, søknadsperiodeTom.minusDays(1), true));
        assertTidslinje(forventetGodkjentTidslinje, GODKJENT);
        assertTidslinje(resultat.disjoint(forventetGodkjentTidslinje), MANGLER_VURDERING);
    }

    @Test
    void ikkeGodkjentInstitusjon() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertNødvendighet vurdertNødvendighet = new VurdertNødvendighet(journalpost1, true, "", "", LocalDateTime.now(), List.of());
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertNødvendighet));

        var vilkårResultatBuilder = new VilkårResultatBuilder();
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.IKKE_OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        var vilkårene = vilkårResultatBuilder.build();

        var resultat = vurderNødvendighetTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertThat(resultat).isEmpty();
    }

    @Test
    void ikkeGodkjentSykdom() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertNødvendighet vurdertNødvendighet = new VurdertNødvendighet(journalpost1, true, "", "", LocalDateTime.now(), List.of());
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertNødvendighet));

        var vilkårResultatBuilder = new VilkårResultatBuilder();
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.LANGVARIG_SYKDOM, Utfall.IKKE_OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        var vilkårene = vilkårResultatBuilder.build();

        var resultat = vurderNødvendighetTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertThat(resultat).isEmpty();
    }

    @Test
    void ikkeGodkjentOpplæring() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertNødvendighet vurdertNødvendighet = new VurdertNødvendighet(journalpost1, true, "", "", LocalDateTime.now(), List.of());
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertNødvendighet));

        var vilkårResultatBuilder = new VilkårResultatBuilder();
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.IKKE_OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        var vilkårene = vilkårResultatBuilder.build();

        var resultat = vurderNødvendighetTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertThat(resultat).isEmpty();
    }

    @Test
    void delvisGodkjentOpplæring() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertNødvendighet vurdertNødvendighet = new VurdertNødvendighet(journalpost1, true, "", "", LocalDateTime.now(), List.of());
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertNødvendighet));

        var vilkårResultatBuilder = new VilkårResultatBuilder();
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.OPPFYLT, søknadsperiodeTom.minusWeeks(1).plusDays(1), søknadsperiodeTom);
        setupVilkårResultat(vilkårResultatBuilder, VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.IKKE_OPPFYLT, søknadsperiodeFom, søknadsperiodeTom.minusWeeks(1));
        var vilkårene = vilkårResultatBuilder.build();

        var resultat = vurderNødvendighetTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        var forventetGodkjentTidslinje = resultat.intersection(new LocalDateTimeline<>(søknadsperiodeTom.minusWeeks(1).plusDays(1), søknadsperiodeTom, true));
        assertTidslinje(forventetGodkjentTidslinje, GODKJENT);
        assertThat(resultat.disjoint(forventetGodkjentTidslinje)).isEmpty();
    }

    private void assertTidslinje(LocalDateTimeline<NødvendighetGodkjenningStatus> tidslinje, NødvendighetGodkjenningStatus forventetStatus) {
        assertThat(tidslinje).isNotEmpty();
        assertThat(tidslinje.disjoint(tidslinje.filterValue(v -> Objects.equals(v, forventetStatus)))).isEmpty();
    }
}
