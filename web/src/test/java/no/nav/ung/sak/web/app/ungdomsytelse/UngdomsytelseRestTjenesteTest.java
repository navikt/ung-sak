package no.nav.ung.sak.web.app.ungdomsytelse;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelsePeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static no.nav.ung.kodeverk.uttak.Tid.TIDENES_ENDE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class UngdomsytelseRestTjenesteTest {

    private UngdomsytelseRestTjeneste ungdomsytelseRestTjeneste;

    @Inject
    private EntityManager entityManager;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    @Inject
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        ungdomsytelseRestTjeneste = new UngdomsytelseRestTjeneste(
            behandlingRepository,
            ungdomsytelseGrunnlagRepository,
            new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository, new UngdomsytelseStartdatoRepository(entityManager)),
            tilkjentYtelseRepository,
            new MånedsvisTidslinjeUtleder(new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository, new UngdomsytelseStartdatoRepository(entityManager)), behandlingRepository)
        );


        behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
    }

    @Test
    void skal_få_null_for_ungdomsprograminfo_dersom_ungdomsprogramperioder_ikke_innhentet() {
        final var ungdomsprogramInformasjon = ungdomsytelseRestTjeneste.getUngdomsprogramInformasjon(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(ungdomsprogramInformasjon).isNull();
    }


    @Test
    void skal_utlede_maksdato_uten_opphørsdato() {
        // arrange
        final var fom = LocalDate.now().withDayOfYear(1);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(fom, TIDENES_ENDE)));

        // act
        final var ungdomsprogramInformasjon = ungdomsytelseRestTjeneste.getUngdomsprogramInformasjon(new BehandlingUuidDto(behandling.getUuid()));

        // assert
        final var forventetMaksdato = fom.plusWeeks(52);
        assertThat(ungdomsprogramInformasjon.maksdatoForDeltakelse()).isEqualTo(forventetMaksdato.minusDays(1));
        assertThat(ungdomsprogramInformasjon.opphørsdato()).isNull();
        assertThat(ungdomsprogramInformasjon.antallDagerTidligereUtbetalt()).isNull();
    }

    @Test
    void skal_utlede_maksdato_og_opphørsdato() {
        // arrange
        final var fom = LocalDate.now().withDayOfYear(1);
        final var opphørsdato = fom.plusDays(20);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(fom, opphørsdato)));

        // act
        final var ungdomsprogramInformasjon = ungdomsytelseRestTjeneste.getUngdomsprogramInformasjon(new BehandlingUuidDto(behandling.getUuid()));

        // assert
        final var forventetMaksdato = fom.plusWeeks(52);
        assertThat(ungdomsprogramInformasjon.maksdatoForDeltakelse()).isEqualTo(forventetMaksdato.minusDays(1));
        assertThat(ungdomsprogramInformasjon.opphørsdato()).isEqualTo(opphørsdato);
        assertThat(ungdomsprogramInformasjon.antallDagerTidligereUtbetalt()).isNull();
    }

    @Test
    void skal_utlede_antall_dager_brukt_fra_original_behandling() {
        // arrange
        final var fom = LocalDate.now().withDayOfYear(1);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(fom, TIDENES_ENDE)));
        tilkjentYtelseRepository.lagre(behandling.getId(), List.of(
                TilkjentYtelsePeriode.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.with(TemporalAdjusters.lastDayOfMonth())))
                    .medRedusertBeløp(BigDecimal.TEN)
                    .medDagsats(BigDecimal.TEN)
                    .medReduksjon(BigDecimal.ZERO)
                    .medUredusertBeløp(BigDecimal.TEN)
                    .build()
            ),
            List.of(),
            "test", "test");

        var revurdering = TestScenarioBuilder.builderMedSøknad()
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
            .lagre(entityManager);
        ungdomsprogramPeriodeRepository.lagre(revurdering.getId(),
            List.of(new UngdomsprogramPeriode(fom, TIDENES_ENDE)));


        // act
        final var ungdomsprogramInformasjon = ungdomsytelseRestTjeneste.getUngdomsprogramInformasjon(new BehandlingUuidDto(revurdering.getUuid()));

        // assert
        final var forventetMaksdato = fom.plusWeeks(52);
        assertThat(ungdomsprogramInformasjon.maksdatoForDeltakelse()).isEqualTo(forventetMaksdato.minusDays(1));
        assertThat(ungdomsprogramInformasjon.opphørsdato()).isNull();
        assertThat(ungdomsprogramInformasjon.antallDagerTidligereUtbetalt()).isEqualTo(23);
    }

}
