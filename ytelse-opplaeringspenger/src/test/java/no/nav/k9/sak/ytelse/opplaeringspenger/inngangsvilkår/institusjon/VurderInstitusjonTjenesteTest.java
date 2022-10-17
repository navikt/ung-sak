package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon;

import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.InstitusjonGodkjenningStatus.GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.InstitusjonGodkjenningStatus.IKKE_GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.InstitusjonGodkjenningStatus.MANGLER_VURDERING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.GodkjentOpplæringsinstitusjonTjeneste;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjonHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;

public class VurderInstitusjonTjenesteTest {

    private VurderInstitusjonTjeneste vurderInstitusjonTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private VurdertOpplæringRepository vurdertOpplæringRepository;
    private GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;

    private final Long behandlingId = 1337L;
    private final LocalDate søknadsperiodeFom = LocalDate.now().minusMonths(2);
    private final LocalDate søknadsperiodeTom = LocalDate.now();
    private final PerioderFraSøknad perioderFraSøknad = mock(PerioderFraSøknad.class);
    private final String institusjon1 = "institusjon1";
    private final String institusjon2 = "institusjon2";
    private final UUID institusjon1Uuid = UUID.randomUUID();
    private final UUID institusjon2Uuid = UUID.randomUUID();

    @BeforeEach
    public void setup() {
        perioderTilVurderingTjeneste = mock(VilkårsPerioderTilVurderingTjeneste.class);
        vurdertOpplæringRepository = mock(VurdertOpplæringRepository.class);
        godkjentOpplæringsinstitusjonTjeneste = mock(GodkjentOpplæringsinstitusjonTjeneste.class);
        uttakPerioderGrunnlagRepository = mock(UttakPerioderGrunnlagRepository.class);
        vurderInstitusjonTjeneste = new VurderInstitusjonTjeneste(perioderTilVurderingTjeneste, vurdertOpplæringRepository, godkjentOpplæringsinstitusjonTjeneste, uttakPerioderGrunnlagRepository);

        when(perioderTilVurderingTjeneste.utled(behandlingId, VilkårType.NØDVENDIG_OPPLÆRING)).thenReturn(new TreeSet<>(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiodeFom, søknadsperiodeTom))));

        UttaksPerioderGrunnlag uttaksPerioderGrunnlag = mock(UttaksPerioderGrunnlag.class);
        when(uttakPerioderGrunnlagRepository.hentGrunnlag(behandlingId)).thenReturn(Optional.of(uttaksPerioderGrunnlag));
        UttakPerioderHolder uttakPerioderHolder = mock(UttakPerioderHolder.class);
        when(uttaksPerioderGrunnlag.getRelevantSøknadsperioder()).thenReturn(uttakPerioderHolder);
        when(uttakPerioderHolder.getPerioderFraSøknadene()).thenReturn(new LinkedHashSet<>(List.of(perioderFraSøknad)));
    }

    @Test
    public void institusjonLiggerIRegisteretAktivForHelePerioden() {
        setupEnkelKursperiode(institusjon1Uuid);
        setupGodkjentOpplæringsinstitusjonIRegister(institusjon1, søknadsperiodeFom, søknadsperiodeTom, institusjon1Uuid);

        var resultat = vurderInstitusjonTjeneste.hentTidslinjeTilVurderingMedInstitusjonsGodkjenning(behandlingId);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, GODKJENT);
    }

    @Test
    public void institusjonLiggerIkkeIRegisterEllerVurderingsgrunnlag() {
        setupEnkelKursperiode();

        var resultat = vurderInstitusjonTjeneste.hentTidslinjeTilVurderingMedInstitusjonsGodkjenning(behandlingId);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, MANGLER_VURDERING);
    }

    @Test
    public void institusjonLiggerIRegisteretInaktivForHelePerioden() {
        setupEnkelKursperiode(institusjon1Uuid);
        setupGodkjentOpplæringsinstitusjonIRegister(institusjon1, søknadsperiodeTom.plusDays(1), søknadsperiodeTom.plusDays(1), institusjon1Uuid);

        var resultat = vurderInstitusjonTjeneste.hentTidslinjeTilVurderingMedInstitusjonsGodkjenning(behandlingId);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, IKKE_GODKJENT);
    }

    @Test
    public void institusjonLiggerIRegisteretAktivForHalvePerioden() {
        setupEnkelKursperiode(institusjon1Uuid);
        setupGodkjentOpplæringsinstitusjonIRegister(institusjon1, søknadsperiodeFom, søknadsperiodeFom.plusMonths(1), institusjon1Uuid);

        var resultat = vurderInstitusjonTjeneste.hentTidslinjeTilVurderingMedInstitusjonsGodkjenning(behandlingId);
        assertThat(resultat).isNotNull();
        var forventetGodkjentTidslinje = resultat.intersection(new LocalDateTimeline<>(søknadsperiodeFom, søknadsperiodeFom.plusMonths(1), true));
        assertTidslinje(forventetGodkjentTidslinje, GODKJENT);
        assertTidslinje(resultat.disjoint(forventetGodkjentTidslinje), IKKE_GODKJENT);
    }

    @Test
    public void institusjonGodkjentIVurderingsgrunnlag() {
        setupEnkelKursperiode();
        VurdertInstitusjon vurdertInstitusjon = new VurdertInstitusjon(institusjon1, true, "begrunnelse");
        setupVurderingsgrunnlag(List.of(vurdertInstitusjon));

        var resultat = vurderInstitusjonTjeneste.hentTidslinjeTilVurderingMedInstitusjonsGodkjenning(behandlingId);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, GODKJENT);
    }

    @Test
    public void enAvToinstitusjonerGodkjentIVurderingsgrunnlag() {
        KursPeriode kursPeriode1 = new KursPeriode(søknadsperiodeFom, søknadsperiodeFom.plusMonths(1), institusjon1, "beskrivelse", søknadsperiodeFom, søknadsperiodeFom.plusMonths(1), null);
        KursPeriode kursPeriode2 = new KursPeriode(søknadsperiodeFom.plusMonths(1).plusDays(1), søknadsperiodeTom, institusjon2, "beskrivelse", søknadsperiodeFom.plusMonths(1).plusDays(1), søknadsperiodeTom, null);
        setupKursperioder(List.of(kursPeriode1, kursPeriode2));
        VurdertInstitusjon vurdertInstitusjon1 = new VurdertInstitusjon(institusjon1, true, "ja");
        VurdertInstitusjon vurdertInstitusjon2 = new VurdertInstitusjon(institusjon2, false, "nei");
        setupVurderingsgrunnlag(List.of(vurdertInstitusjon1, vurdertInstitusjon2));

        var resultat = vurderInstitusjonTjeneste.hentTidslinjeTilVurderingMedInstitusjonsGodkjenning(behandlingId);
        assertThat(resultat).isNotNull();
        var forventetGodkjentTidslinje = resultat.intersection(new LocalDateTimeline<>(søknadsperiodeFom, søknadsperiodeFom.plusMonths(1), true));
        assertTidslinje(forventetGodkjentTidslinje, GODKJENT);
        assertTidslinje(resultat.disjoint(forventetGodkjentTidslinje), IKKE_GODKJENT);
    }

    @Test
    public void enAvToinstitusjonerGodkjentIRegisteret() {
        KursPeriode kursPeriode1 = new KursPeriode(søknadsperiodeFom, søknadsperiodeFom.plusMonths(1), institusjon1, "beskrivelse", søknadsperiodeFom, søknadsperiodeFom.plusMonths(1), institusjon1Uuid);
        KursPeriode kursPeriode2 = new KursPeriode(søknadsperiodeFom.plusMonths(1).plusDays(1), søknadsperiodeTom, institusjon2, "beskrivelse", søknadsperiodeFom.plusMonths(1).plusDays(1), søknadsperiodeTom, institusjon2Uuid);
        setupKursperioder(List.of(kursPeriode1, kursPeriode2));
        setupGodkjentOpplæringsinstitusjonIRegister(institusjon1, søknadsperiodeFom, søknadsperiodeTom, institusjon1Uuid);
        setupGodkjentOpplæringsinstitusjonIRegister(institusjon2, søknadsperiodeTom.plusDays(1), null, institusjon2Uuid);

        var resultat = vurderInstitusjonTjeneste.hentTidslinjeTilVurderingMedInstitusjonsGodkjenning(behandlingId);
        assertThat(resultat).isNotNull();
        var forventetGodkjentTidslinje = resultat.intersection(new LocalDateTimeline<>(søknadsperiodeFom, søknadsperiodeFom.plusMonths(1), true));
        assertTidslinje(forventetGodkjentTidslinje, GODKJENT);
        assertTidslinje(resultat.disjoint(forventetGodkjentTidslinje), IKKE_GODKJENT);
    }

    @Test
    public void enInstitusjonGodkjentIRegisteretEnGodkjentIVurderingsgrunnlag() {
        KursPeriode kursPeriode1 = new KursPeriode(søknadsperiodeFom, søknadsperiodeFom.plusMonths(1), institusjon1, "beskrivelse", søknadsperiodeFom, søknadsperiodeFom.plusMonths(1), institusjon1Uuid);
        KursPeriode kursPeriode2 = new KursPeriode(søknadsperiodeFom.plusMonths(1).plusDays(1), søknadsperiodeTom, institusjon2, "beskrivelse", søknadsperiodeFom.plusMonths(1).plusDays(1), søknadsperiodeTom, null);
        setupKursperioder(List.of(kursPeriode1, kursPeriode2));
        setupGodkjentOpplæringsinstitusjonIRegister(institusjon1, søknadsperiodeFom, søknadsperiodeTom, institusjon1Uuid);
        VurdertInstitusjon vurdertInstitusjon = new VurdertInstitusjon(institusjon2, true, "ja");
        setupVurderingsgrunnlag(List.of(vurdertInstitusjon));

        var resultat = vurderInstitusjonTjeneste.hentTidslinjeTilVurderingMedInstitusjonsGodkjenning(behandlingId);
        assertThat(resultat).isNotNull();
        assertTidslinje(resultat, GODKJENT);
    }

    private void assertTidslinje(LocalDateTimeline<InstitusjonGodkjenningStatus> tidslinje, InstitusjonGodkjenningStatus forventetStatus) {
        assertThat(tidslinje.disjoint(tidslinje.filterValue(v -> Objects.equals(v, forventetStatus)))).isEmpty();
    }

    private void setupEnkelKursperiode() {
        setupEnkelKursperiode(null);
    }

    private void setupEnkelKursperiode(UUID uuid) {
        KursPeriode kursPeriode = new KursPeriode(søknadsperiodeFom, søknadsperiodeTom, institusjon1, "beskrivelse", søknadsperiodeFom, søknadsperiodeTom, uuid);
        setupKursperioder(List.of(kursPeriode));
    }

    private void setupKursperioder(List<KursPeriode> kursperioder) {
        when(perioderFraSøknad.getKurs()).thenReturn(new LinkedHashSet<>(kursperioder));
    }

    private void setupVurderingsgrunnlag(List<VurdertInstitusjon> vurderteInstitusjoner) {
        VurdertInstitusjonHolder vurdertInstitusjonHolder = new VurdertInstitusjonHolder(vurderteInstitusjoner);
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = new VurdertOpplæringGrunnlag(behandlingId, vurdertInstitusjonHolder, null, "");
        when(vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandlingId)).thenReturn(Optional.of(vurdertOpplæringGrunnlag));
    }

    private void setupGodkjentOpplæringsinstitusjonIRegister(String navn, LocalDate fom, LocalDate tom, UUID uuid) {
        GodkjentOpplæringsinstitusjon godkjentOpplæringsinstitusjon = new GodkjentOpplæringsinstitusjon(uuid, navn, fom, tom);
        when(godkjentOpplæringsinstitusjonTjeneste.hentMedUuid(uuid)).thenReturn(Optional.of(godkjentOpplæringsinstitusjon));
    }
}
