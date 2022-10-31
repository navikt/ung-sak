package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.IKKE_GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.MANGLER_VURDERING;
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
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

class VurderNødvendighetTjenesteTest {

    private final VurderNødvendighetTjeneste vurderNødvendighetTjeneste = new VurderNødvendighetTjeneste();

    private final LocalDate søknadsperiodeFom = LocalDate.now().minusMonths(2);
    private final LocalDate søknadsperiodeTom = LocalDate.now();
    private final JournalpostId journalpost1 = new JournalpostId("234");
    private final JournalpostId journalpost2 = new JournalpostId("567");
    private LocalDateTimeline<Boolean> søknadsperiode;

    @BeforeEach
    void setup() {
        søknadsperiode = TidslinjeUtil.tilTidslinjeKomprimert(new TreeSet<>(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeTom))));
    }

    private Set<PerioderFraSøknad> setupEnkelKursperiode() {
        KursPeriode kursPeriode = new KursPeriode(søknadsperiodeFom, søknadsperiodeTom, "institusjon", "beskrivelse", søknadsperiodeFom, søknadsperiodeTom, null);
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

    private VurdertOpplæringGrunnlag setupVurderingsgrunnlag(List<VurdertOpplæring> vurdertOpplæring) {
        VurdertOpplæringHolder vurdertOpplæringHolder = new VurdertOpplæringHolder(vurdertOpplæring);
        return new VurdertOpplæringGrunnlag(1344L, null, vurdertOpplæringHolder);
    }

    @Test
    void ingenVurdering() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();

        var resultat = vurderNødvendighetTjeneste.hentTidslinjeTilVurderingMedNødvendighetsGodkjenning(perioderFraSøknad, null, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, MANGLER_VURDERING);
    }

    @Test
    void vurderingGodkjent() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(journalpost1, List.of(), true, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæring));

        var resultat = vurderNødvendighetTjeneste.hentTidslinjeTilVurderingMedNødvendighetsGodkjenning(perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, GODKJENT);
    }

    @Test
    void vurderingIkkeGodkjent() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(journalpost1, List.of(), false, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæring));

        var resultat = vurderNødvendighetTjeneste.hentTidslinjeTilVurderingMedNødvendighetsGodkjenning(perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, IKKE_GODKJENT);
    }

    @Test
    void vurderingMangler() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(journalpost2, List.of(), true, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæring));

        var resultat = vurderNødvendighetTjeneste.hentTidslinjeTilVurderingMedNødvendighetsGodkjenning(perioderFraSøknad, vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, MANGLER_VURDERING);
    }

    @Test
    void vurderingManglerDelvis() {
        KursPeriode kursPeriode1 = new KursPeriode(søknadsperiodeFom, søknadsperiodeTom.minusDays(1), "her", "beskrivelse", søknadsperiodeFom, søknadsperiodeFom.minusDays(1), null);
        KursPeriode kursPeriode2 = new KursPeriode(søknadsperiodeTom, søknadsperiodeTom, "der", "beskrivelse", søknadsperiodeTom, søknadsperiodeTom, null);
        PerioderFraSøknad perioderFraSøknad1 = setupPerioderFraSøknad(journalpost1, List.of(kursPeriode1));
        PerioderFraSøknad perioderFraSøknad2 = setupPerioderFraSøknad(journalpost2, List.of(kursPeriode2));
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(journalpost1, List.of(), true, "");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertOpplæring));

        var resultat = vurderNødvendighetTjeneste.hentTidslinjeTilVurderingMedNødvendighetsGodkjenning(Set.of(perioderFraSøknad1, perioderFraSøknad2), vurdertOpplæringGrunnlag, søknadsperiode);
        assertThat(resultat).isNotNull();
        var forventetGodkjentTidslinje = resultat.intersection(new LocalDateTimeline<>(søknadsperiodeFom, søknadsperiodeTom.minusDays(1), true));
        assertTidslinje(forventetGodkjentTidslinje, GODKJENT);
        assertTidslinje(resultat.disjoint(forventetGodkjentTidslinje), MANGLER_VURDERING);
    }

    private void assertTidslinje(LocalDateTimeline<NødvendighetGodkjenningStatus> tidslinje, NødvendighetGodkjenningStatus forventetStatus) {
        assertThat(tidslinje.disjoint(tidslinje.filterValue(v -> Objects.equals(v, forventetStatus)))).isEmpty();
    }
}
