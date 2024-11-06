package no.nav.k9.sak.ytelse.ung.beregning.barnetillegg;

import static no.nav.fpsak.tidsserie.LocalDateInterval.TIDENES_ENDE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class LagAntallBarnTidslinjeTest {

    @Inject
    private EntityManager entityManager;
    private LagAntallBarnTidslinje lagAntallBarnTidslinje;
    private final HentFødselOgDød hentFødselOgDød = mock(HentFødselOgDød.class);
    private PersonopplysningRepository personopplysningRepository;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        personopplysningRepository = new PersonopplysningRepository(entityManager);
        lagAntallBarnTidslinje = new LagAntallBarnTidslinje(personopplysningRepository, hentFødselOgDød);
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        TestScenarioBuilder førstegangScenario = TestScenarioBuilder.builderMedSøknad();
        behandling = førstegangScenario.lagre(repositoryProvider);
    }

    @Test
    void skal_finne_ingen_barn() {
        var personInformasjonBuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);
        PersonInformasjonBuilder.PersonopplysningBuilder builder = personInformasjonBuilder.getPersonopplysningBuilder(behandling.getAktørId());
        builder.medFødselsdato(LocalDate.now().minusYears(18));
        personInformasjonBuilder.leggTil(builder);
        personopplysningRepository.lagre(behandling.getId(), personInformasjonBuilder);

        var antallBarnTidslinje = lagAntallBarnTidslinje.lagAntallBarnTidslinje(BehandlingReferanse.fra(behandling));

        assertThat(antallBarnTidslinje.isEmpty()).isTrue();
    }

    @Test
    void skal_finne_et_barn() {
        var personInformasjonBuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);
        PersonInformasjonBuilder.PersonopplysningBuilder builder = personInformasjonBuilder.getPersonopplysningBuilder(behandling.getAktørId());
        builder.medFødselsdato(LocalDate.now().minusYears(18));
        personInformasjonBuilder.leggTil(builder);
        var barnAktørId = AktørId.dummy();
        var fødselsdatoBarn = LocalDate.now().minusYears(1);
        leggTilBarn(personInformasjonBuilder, builder, barnAktørId, fødselsdatoBarn, null);

        var antallBarnTidslinje = lagAntallBarnTidslinje.lagAntallBarnTidslinje(BehandlingReferanse.fra(behandling));

        assertThat(antallBarnTidslinje.isEmpty()).isFalse();
        var segments = antallBarnTidslinje.toSegments();
        assertThat(segments.size()).isEqualTo(1);
        var segment = segments.first();
        assertThat(segment.getFom()).isEqualTo(fødselsdatoBarn);
        assertThat(segment.getTom()).isEqualTo(TIDENES_ENDE);
        assertThat(segment.getValue()).isEqualTo(1);
    }

    @Test
    void skal_finne_et_barn_med_dødsdato() {
        var personInformasjonBuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);
        PersonInformasjonBuilder.PersonopplysningBuilder builder = personInformasjonBuilder.getPersonopplysningBuilder(behandling.getAktørId());
        builder.medFødselsdato(LocalDate.now().minusYears(18));
        personInformasjonBuilder.leggTil(builder);
        var barnAktørId = AktørId.dummy();
        var fødselsdatoBarn = LocalDate.now().minusYears(1);
        leggTilBarn(personInformasjonBuilder, builder, barnAktørId, fødselsdatoBarn, fødselsdatoBarn.plusMonths(5));

        var antallBarnTidslinje = lagAntallBarnTidslinje.lagAntallBarnTidslinje(BehandlingReferanse.fra(behandling));

        assertThat(antallBarnTidslinje.isEmpty()).isFalse();
        var segments = antallBarnTidslinje.toSegments();
        assertThat(segments.size()).isEqualTo(1);
        var segment = segments.first();
        assertThat(segment.getFom()).isEqualTo(fødselsdatoBarn);
        assertThat(segment.getTom()).isEqualTo(fødselsdatoBarn.plusMonths(5));
        assertThat(segment.getValue()).isEqualTo(1);
    }

    @Test
    void skal_finne_to_barn() {
        var personInformasjonBuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);
        PersonInformasjonBuilder.PersonopplysningBuilder builder = personInformasjonBuilder.getPersonopplysningBuilder(behandling.getAktørId());
        builder.medFødselsdato(LocalDate.now().minusYears(18));
        personInformasjonBuilder.leggTil(builder);
        var barnAktørId = AktørId.dummy();
        var fødselsdatoBarn = LocalDate.now().minusYears(1);
        var dødsdatoBarn1 = fødselsdatoBarn.plusMonths(5);
        leggTilBarn(personInformasjonBuilder, builder, barnAktørId, fødselsdatoBarn, dødsdatoBarn1);
        var barnAktørId2 = AktørId.dummy();
        var fødselsdatoBarn2 = LocalDate.now().minusYears(2);
        leggTilBarn(personInformasjonBuilder, builder, barnAktørId2, fødselsdatoBarn2, null);

        var antallBarnTidslinje = lagAntallBarnTidslinje.lagAntallBarnTidslinje(BehandlingReferanse.fra(behandling));

        assertThat(antallBarnTidslinje.isEmpty()).isFalse();
        var segments = antallBarnTidslinje.toSegments();
        assertThat(segments.size()).isEqualTo(3);
        var iterator = segments.iterator();
        var segment1 = iterator.next();
        assertThat(segment1.getFom()).isEqualTo(fødselsdatoBarn2);
        assertThat(segment1.getTom()).isEqualTo(fødselsdatoBarn.minusDays(1));
        assertThat(segment1.getValue()).isEqualTo(1);

        var segment2 = iterator.next();
        assertThat(segment2.getFom()).isEqualTo(fødselsdatoBarn);
        assertThat(segment2.getTom()).isEqualTo(dødsdatoBarn1);
        assertThat(segment2.getValue()).isEqualTo(2);

        var segment3 = iterator.next();
        assertThat(segment3.getFom()).isEqualTo(dødsdatoBarn1.plusDays(1));
        assertThat(segment3.getTom()).isEqualTo(TIDENES_ENDE);
        assertThat(segment3.getValue()).isEqualTo(1);
    }

    private void leggTilBarn(PersonInformasjonBuilder personInformasjonBuilder, PersonInformasjonBuilder.PersonopplysningBuilder builder, AktørId barnAktørId, LocalDate fødselsdatoBarn, LocalDate dødsdato) {
        personInformasjonBuilder.leggTil(personInformasjonBuilder.getRelasjonBuilder(behandling.getAktørId(), barnAktørId, RelasjonsRolleType.BARN));
        personopplysningRepository.lagre(behandling.getId(), personInformasjonBuilder);
        var fødselOgDødInfo = new HentFødselOgDød.FødselOgDødInfo(barnAktørId, fødselsdatoBarn, dødsdato);
        when(hentFødselOgDød.hentFødselOgDødInfo(eq(barnAktørId))).thenReturn(fødselOgDødInfo);
    }


}
