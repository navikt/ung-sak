package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.IKKE_GJENNOMGÅTT_OPPLÆRING;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.IKKE_GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.IKKE_GODKJENT_INSTITUSJON;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.IKKE_GODKJENT_SYKDOMSVILKÅR;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.MANGLER_VURDERING;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

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
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

class VurderNødvendighetTidslinjeUtlederTest {

    private final VurderNødvendighetTidslinjeUtleder vurderNødvendighetTidslinjeUtleder = new VurderNødvendighetTidslinjeUtleder();

    private final LocalDate søknadsperiodeFom = LocalDate.now().minusWeeks(2);
    private final LocalDate søknadsperiodeTom = LocalDate.now();
    private final JournalpostId journalpost1 = new JournalpostId("234");
    private final JournalpostId journalpost2 = new JournalpostId("567");
    private LocalDateTimeline<Boolean> søknadsperiode;
    private final VilkårResultatBuilder vilkårResultatBuilder = new VilkårResultatBuilder();

    @BeforeEach
    void setup() {
        søknadsperiode = TidslinjeUtil.tilTidslinjeKomprimert(new TreeSet<>(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeTom))));
        setupVilkårResultat(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        setupVilkårResultat(VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
    }

    private void setupVilkårResultat(VilkårType vilkårType, Utfall utfall, LocalDate fom, LocalDate tom) {
        vilkårResultatBuilder.leggTil(vilkårResultatBuilder.hentBuilderFor(vilkårType)
            .leggTil(new VilkårPeriodeBuilder().medUtfall(utfall).medPeriode(fom, tom)));
    }

    private Set<PerioderFraSøknad> setupEnkelKursperiode() {
        KursPeriode kursPeriode = lagKursperiode(søknadsperiodeFom, søknadsperiodeTom, "institusjon", null);
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

    private KursPeriode lagKursperiode(LocalDate fom, LocalDate tom, String institusjon, UUID uuid) {
        return new KursPeriode(fom, tom, null, null, institusjon, uuid, "beskrivelse");
    }

    private VurdertOpplæringGrunnlag setupVurderingsgrunnlag(List<VurdertOpplæring> vurdertOpplæring) {
        VurdertOpplæringHolder vurdertOpplæringHolder = new VurdertOpplæringHolder(vurdertOpplæring);
        return new VurdertOpplæringGrunnlag(1344L, null, vurdertOpplæringHolder, null);
    }

    @Test
    void ingenVurdering() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = vurderNødvendighetTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, null, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, MANGLER_VURDERING);
    }

    @Test
    void vurderingGodkjent() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(journalpost1, true, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæring));

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = vurderNødvendighetTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, GODKJENT);
    }

    @Test
    void vurderingIkkeGodkjent() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(journalpost1, false, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæring));

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = vurderNødvendighetTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, IKKE_GODKJENT);
    }

    @Test
    void vurderingMangler() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(journalpost2, true, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæring));

        var vilkårene = vilkårResultatBuilder.build();

        var resultat = vurderNødvendighetTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, MANGLER_VURDERING);
    }

    @Test
    void vurderingManglerDelvis() {
        KursPeriode kursPeriode1 = lagKursperiode(søknadsperiodeFom, søknadsperiodeTom.minusDays(1), "her", null);
        KursPeriode kursPeriode2 = lagKursperiode(søknadsperiodeTom, søknadsperiodeTom, "der", null);
        PerioderFraSøknad perioderFraSøknad1 = setupPerioderFraSøknad(journalpost1, List.of(kursPeriode1));
        PerioderFraSøknad perioderFraSøknad2 = setupPerioderFraSøknad(journalpost2, List.of(kursPeriode2));
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(journalpost1, true, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæring));

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
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(journalpost1, true, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæring));

        setupVilkårResultat(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.IKKE_OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        var vilkårene = vilkårResultatBuilder.build();

        var resultat = vurderNødvendighetTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, IKKE_GODKJENT_INSTITUSJON);
    }

    @Test
    void ikkeGodkjentSykdom() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(journalpost1, true, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæring));

        setupVilkårResultat(VilkårType.LANGVARIG_SYKDOM, Utfall.IKKE_OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        var vilkårene = vilkårResultatBuilder.build();

        var resultat = vurderNødvendighetTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, IKKE_GODKJENT_SYKDOMSVILKÅR);
    }

    @Test
    void ikkeGodkjentOpplæring() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(journalpost1, true, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæring));

        setupVilkårResultat(VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.IKKE_OPPFYLT, søknadsperiodeFom, søknadsperiodeTom);
        var vilkårene = vilkårResultatBuilder.build();

        var resultat = vurderNødvendighetTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, IKKE_GJENNOMGÅTT_OPPLÆRING);
    }

    @Test
    void delvisGodkjentOpplæring() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(journalpost1, true, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæring));

        setupVilkårResultat(VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.IKKE_OPPFYLT, søknadsperiodeFom, søknadsperiodeTom.minusWeeks(1));
        var vilkårene = vilkårResultatBuilder.build();

        var resultat = vurderNødvendighetTidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        var forventetGodkjentTidslinje = resultat.intersection(new LocalDateTimeline<>(søknadsperiodeTom.minusWeeks(1).plusDays(1), søknadsperiodeTom, true));
        assertTidslinje(forventetGodkjentTidslinje, GODKJENT);
        assertTidslinje(resultat.disjoint(forventetGodkjentTidslinje), IKKE_GJENNOMGÅTT_OPPLÆRING);
    }

    private void assertTidslinje(LocalDateTimeline<NødvendighetGodkjenningStatus> tidslinje, NødvendighetGodkjenningStatus forventetStatus) {
        assertThat(tidslinje.disjoint(tidslinje.filterValue(v -> Objects.equals(v, forventetStatus)))).isEmpty();
    }
}
