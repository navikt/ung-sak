package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon;

import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.InstitusjonGodkjenningStatus.GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.InstitusjonGodkjenningStatus.IKKE_GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.InstitusjonGodkjenningStatus.MANGLER_VURDERING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjonHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

public class VurderInstitusjonTidslinjeUtlederTest {

    private VurderInstitusjonTidslinjeUtleder vurderInstitusjonTidslinjeUtleder;
    private GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste;

    private final Long behandlingId = 1337L;
    private final LocalDate søknadsperiodeFom = LocalDate.now().minusMonths(2);
    private final LocalDate søknadsperiodeTom = LocalDate.now();
    private final JournalpostId journalpost1 = new JournalpostId("234");
    private final JournalpostId journalpost2 = new JournalpostId("567");
    private final String institusjon1 = "noe";
    private final String institusjon2 = "noe annet";
    private final UUID institusjon1Uuid = UUID.randomUUID();
    private final UUID institusjon2Uuid = UUID.randomUUID();
    private LocalDateTimeline<Boolean> søknadstidslinje;

    @BeforeEach
    public void setup() {
        godkjentOpplæringsinstitusjonTjeneste = mock(GodkjentOpplæringsinstitusjonTjeneste.class);
        vurderInstitusjonTidslinjeUtleder = new VurderInstitusjonTidslinjeUtleder(godkjentOpplæringsinstitusjonTjeneste);
        var søknadsperioder = new TreeSet<>(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeTom)));
        søknadstidslinje = TidslinjeUtil.tilTidslinjeKomprimert(søknadsperioder);
    }

    private Set<PerioderFraSøknad> setupEnkelKursperiode() {
        return setupEnkelKursperiode(null);
    }

    private Set<PerioderFraSøknad> setupEnkelKursperiode(UUID uuid) {
        KursPeriode kursPeriode = lagKursperiode(søknadsperiodeFom, søknadsperiodeTom, institusjon1, uuid);
        return setupKursperioder(journalpost1, List.of(kursPeriode));
    }

    private Set<PerioderFraSøknad> setupKursperioder(JournalpostId journalpostId, List<KursPeriode> kursperioder) {
        return Set.of(setupEnkelKursperiode(journalpostId, kursperioder));
    }

    private PerioderFraSøknad setupEnkelKursperiode(JournalpostId journalpostId, List<KursPeriode> kursperiode) {
        return new PerioderFraSøknad(journalpostId,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            kursperiode);
    }

    private KursPeriode lagKursperiode(LocalDate fom, LocalDate tom, String institusjon, UUID uuid) {
        return new KursPeriode(fom, tom, null, null, institusjon, uuid, "beskrivelse");
    }

    private VurdertOpplæringGrunnlag setupVurderingsgrunnlag(List<VurdertInstitusjon> vurderteInstitusjoner) {
        VurdertInstitusjonHolder vurdertInstitusjonHolder = new VurdertInstitusjonHolder(vurderteInstitusjoner);
        return new VurdertOpplæringGrunnlag(behandlingId, vurdertInstitusjonHolder, null, null, null);
    }

    private void setupGodkjentOpplæringsinstitusjonIRegister(String navn, LocalDate fom, LocalDate tom, UUID uuid) {
        GodkjentOpplæringsinstitusjon godkjentOpplæringsinstitusjon = new GodkjentOpplæringsinstitusjon(uuid, navn, fom, tom);
        when(godkjentOpplæringsinstitusjonTjeneste.hentMedUuid(uuid)).thenReturn(Optional.of(godkjentOpplæringsinstitusjon));
    }

    @Test
    public void institusjonLiggerIRegisteretAktivForHelePerioden() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode(institusjon1Uuid);
        setupGodkjentOpplæringsinstitusjonIRegister("noe", søknadsperiodeFom, søknadsperiodeTom, institusjon1Uuid);

        var resultat = vurderInstitusjonTidslinjeUtleder.utled(perioderFraSøknad, null, søknadstidslinje);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, GODKJENT);
    }

    @Test
    public void institusjonLiggerIkkeIRegisterEllerVurderingsgrunnlag() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();

        var resultat = vurderInstitusjonTidslinjeUtleder.utled(perioderFraSøknad, null, søknadstidslinje);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, MANGLER_VURDERING);
    }

    @Test
    public void institusjonLiggerIRegisteretInaktivForHelePerioden() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode(institusjon1Uuid);
        setupGodkjentOpplæringsinstitusjonIRegister(institusjon1, søknadsperiodeTom.plusDays(1), søknadsperiodeTom.plusDays(1), institusjon1Uuid);

        var resultat = vurderInstitusjonTidslinjeUtleder.utled(perioderFraSøknad, null, søknadstidslinje);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, MANGLER_VURDERING);
    }

    @Test
    public void institusjonLiggerIRegisteretAktivForHalvePerioden() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode(institusjon1Uuid);
        setupGodkjentOpplæringsinstitusjonIRegister(institusjon1, søknadsperiodeFom, søknadsperiodeFom.plusMonths(1), institusjon1Uuid);

        var resultat = vurderInstitusjonTidslinjeUtleder.utled(perioderFraSøknad, null, søknadstidslinje);
        assertThat(resultat).isNotNull();
        var forventetGodkjentTidslinje = resultat.intersection(new LocalDateTimeline<>(søknadsperiodeFom, søknadsperiodeFom.plusMonths(1), true));
        assertTidslinje(forventetGodkjentTidslinje, GODKJENT);
        assertTidslinje(resultat.disjoint(forventetGodkjentTidslinje), MANGLER_VURDERING);
    }

    @Test
    public void institusjonGodkjentIVurderingsgrunnlag() {
        Set<PerioderFraSøknad> perioderFraSøknad = setupEnkelKursperiode();
        VurdertInstitusjon vurdertInstitusjon = new VurdertInstitusjon(journalpost1, true, "begrunnelse");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertInstitusjon));

        var resultat = vurderInstitusjonTidslinjeUtleder.utled(perioderFraSøknad, vurdertOpplæringGrunnlag, søknadstidslinje);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, GODKJENT);
    }

    @Test
    public void enAvToinstitusjonerGodkjentIVurderingsgrunnlag() {
        KursPeriode kursPeriode1 = lagKursperiode(søknadsperiodeFom, søknadsperiodeFom.plusMonths(1), institusjon1, null);
        KursPeriode kursPeriode2 = lagKursperiode(søknadsperiodeFom.plusMonths(1).plusDays(1), søknadsperiodeTom, institusjon2, null);
        PerioderFraSøknad perioderFraSøknad1 = setupEnkelKursperiode(journalpost1, List.of(kursPeriode1));
        PerioderFraSøknad perioderFraSøknad2 = setupEnkelKursperiode(journalpost2, List.of(kursPeriode2));
        Set<PerioderFraSøknad> perioderFraSøknad = Set.of(perioderFraSøknad1, perioderFraSøknad2);
        VurdertInstitusjon vurdertInstitusjon1 = new VurdertInstitusjon(journalpost1, true, "ja");
        VurdertInstitusjon vurdertInstitusjon2 = new VurdertInstitusjon(journalpost2, false, "nei");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertInstitusjon1, vurdertInstitusjon2));

        var resultat = vurderInstitusjonTidslinjeUtleder.utled(perioderFraSøknad, vurdertOpplæringGrunnlag, søknadstidslinje);
        assertThat(resultat).isNotNull();
        var forventetGodkjentTidslinje = resultat.intersection(new LocalDateTimeline<>(søknadsperiodeFom, søknadsperiodeFom.plusMonths(1), true));
        assertTidslinje(forventetGodkjentTidslinje, GODKJENT);
        assertTidslinje(resultat.disjoint(forventetGodkjentTidslinje), IKKE_GODKJENT);
    }

    @Test
    public void enAvToinstitusjonerGodkjentIRegisteret() {
        KursPeriode kursPeriode1 = lagKursperiode(søknadsperiodeFom, søknadsperiodeFom.plusMonths(1), institusjon1, institusjon1Uuid);
        KursPeriode kursPeriode2 = lagKursperiode(søknadsperiodeFom.plusMonths(1).plusDays(1), søknadsperiodeTom, institusjon2, institusjon2Uuid);
        Set<PerioderFraSøknad> perioderFraSøknad = setupKursperioder(journalpost1, List.of(kursPeriode1, kursPeriode2));
        setupGodkjentOpplæringsinstitusjonIRegister(institusjon1, søknadsperiodeFom, søknadsperiodeTom, institusjon1Uuid);
        setupGodkjentOpplæringsinstitusjonIRegister(institusjon2, søknadsperiodeTom.plusDays(1), null, institusjon2Uuid);

        var resultat = vurderInstitusjonTidslinjeUtleder.utled(perioderFraSøknad, null, søknadstidslinje);
        assertThat(resultat).isNotNull();
        var forventetGodkjentTidslinje = resultat.intersection(new LocalDateTimeline<>(søknadsperiodeFom, søknadsperiodeFom.plusMonths(1), true));
        assertTidslinje(forventetGodkjentTidslinje, GODKJENT);
        assertTidslinje(resultat.disjoint(forventetGodkjentTidslinje), MANGLER_VURDERING);
    }

    @Test
    public void enInstitusjonGodkjentIRegisteretEnGodkjentIVurderingsgrunnlag() {
        KursPeriode kursPeriode1 = lagKursperiode(søknadsperiodeFom, søknadsperiodeFom.plusMonths(1), institusjon1, institusjon1Uuid);
        KursPeriode kursPeriode2 = lagKursperiode(søknadsperiodeFom.plusMonths(1).plusDays(1), søknadsperiodeTom, institusjon2, null);
        PerioderFraSøknad perioderFraSøknad1 = setupEnkelKursperiode(journalpost1, List.of(kursPeriode1));
        PerioderFraSøknad perioderFraSøknad2 = setupEnkelKursperiode(journalpost2, List.of(kursPeriode2));
        Set<PerioderFraSøknad> perioderFraSøknad = Set.of(perioderFraSøknad1, perioderFraSøknad2);
        setupGodkjentOpplæringsinstitusjonIRegister(institusjon1, søknadsperiodeFom, søknadsperiodeTom, institusjon1Uuid);
        VurdertInstitusjon vurdertInstitusjon = new VurdertInstitusjon(journalpost2, true, "ja");
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = setupVurderingsgrunnlag(List.of(vurdertInstitusjon));

        var resultat = vurderInstitusjonTidslinjeUtleder.utled(perioderFraSøknad, vurdertOpplæringGrunnlag, søknadstidslinje);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, GODKJENT);
    }

    private void assertTidslinje(LocalDateTimeline<InstitusjonGodkjenningStatus> tidslinje, InstitusjonGodkjenningStatus forventetStatus) {
        assertThat(tidslinje.disjoint(tidslinje.filterValue(v -> Objects.equals(v, forventetStatus)))).isEmpty();
    }
}
