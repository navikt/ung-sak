package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.metrikker.bigquery.tabeller.ungdomsprogram.DagerIProgrammetRecord;
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;
import no.nav.ung.sak.typer.AktørId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet.fraOgMedTilOgMed;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class specifically for testing the dagerIProgrammet functionality,
 * including the weekday calculation logic that excludes weekends.
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class DagerIProgrammetTest {

    public static final LocalDate DAGENS_DATO = LocalDate.of(2025, 8, 27);
    private static final LocalDate TIDENES_ENDE = LocalDate.of(9999, 12, 31);

    @Inject
    private EntityManager entityManager;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private SaksnummerRepository saksnummerRepository;

    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    private AntallDagerStatistikk statistikkRepository;

    @BeforeEach
    void setUp() {
        statistikkRepository = new AntallDagerStatistikk(entityManager);
    }

    @Test
    void skal_telle_antall_dager_til_dagens_dato() {
        lagFagsakOgBehandlingMedUngdomsprogram(fraOgMedTilOgMed(DAGENS_DATO.minusDays(10), TIDENES_ENDE));

        var dagerIProgrammet = statistikkRepository.dagerIProgrammet(DAGENS_DATO);

        assertThat(dagerIProgrammet.size()).isEqualTo(1);
        DagerIProgrammetRecord next = dagerIProgrammet.iterator().next();
        assertThat(next.dagerIProgrammet()).isEqualTo(7);
        assertThat(next.antall().compareTo(BigDecimal.ONE)).isEqualTo(0);
    }

    @Test
    void skal_ikke_telle_antall_dager_dersom_ikke_startet_enda() {
        lagFagsakOgBehandlingMedUngdomsprogram(fraOgMedTilOgMed(DAGENS_DATO.plusDays(1), TIDENES_ENDE));

        var dagerIProgrammet = statistikkRepository.dagerIProgrammet(DAGENS_DATO);

        assertThat(dagerIProgrammet.size()).isEqualTo(0);
    }

    @Test
    void skal_telle_antall_dager_til_sluttdato_dersom_før_dagens_dato() {
        lagFagsakOgBehandlingMedUngdomsprogram(fraOgMedTilOgMed(DAGENS_DATO.minusDays(10), DAGENS_DATO.minusDays(2)));

        var dagerIProgrammet = statistikkRepository.dagerIProgrammet(DAGENS_DATO);

        assertThat(dagerIProgrammet.size()).isEqualTo(1);
        DagerIProgrammetRecord next = dagerIProgrammet.iterator().next();
        assertThat(next.dagerIProgrammet()).isEqualTo(6);
        assertThat(next.antall().compareTo(BigDecimal.ONE)).isEqualTo(0);
    }

    @Test
    void skal_telle_antall_dager_for_flere_perioder() {
        lagFagsakOgBehandlingMedUngdomsprogram(
            fraOgMedTilOgMed(DAGENS_DATO.minusDays(20), DAGENS_DATO.minusDays(10)),
            fraOgMedTilOgMed(DAGENS_DATO.minusDays(2), DAGENS_DATO.minusDays(2)));

        var dagerIProgrammet = statistikkRepository.dagerIProgrammet(DAGENS_DATO);

        assertThat(dagerIProgrammet.size()).isEqualTo(1);
        DagerIProgrammetRecord next = dagerIProgrammet.iterator().next();
        assertThat(next.dagerIProgrammet()).isEqualTo(8);
        assertThat(next.antall().compareTo(BigDecimal.ONE)).isEqualTo(0);
    }

    @Test
    void skal_telle_antall_dager_for_flere_perioder_og_flere_fagsaker() {
        lagFagsakOgBehandlingMedUngdomsprogram(
            fraOgMedTilOgMed(DAGENS_DATO.minusDays(20), DAGENS_DATO.minusDays(10)),
            fraOgMedTilOgMed(DAGENS_DATO.minusDays(2), DAGENS_DATO.minusDays(2)));

        lagFagsakOgBehandlingMedUngdomsprogram(
            fraOgMedTilOgMed(DAGENS_DATO.minusDays(20), DAGENS_DATO.minusDays(10)),
            fraOgMedTilOgMed(DAGENS_DATO.minusDays(2), DAGENS_DATO.minusDays(2)));

        lagFagsakOgBehandlingMedUngdomsprogram(
            fraOgMedTilOgMed(DAGENS_DATO.minusDays(20), DAGENS_DATO.minusDays(10)),
            fraOgMedTilOgMed(DAGENS_DATO.minusDays(2), DAGENS_DATO.minusDays(2)));

        lagFagsakOgBehandlingMedUngdomsprogram(
            fraOgMedTilOgMed(DAGENS_DATO.minusDays(20), TIDENES_ENDE));

        var dagerIProgrammet = statistikkRepository.dagerIProgrammet(DAGENS_DATO);

        assertThat(dagerIProgrammet.size()).isEqualTo(2);
        Iterator<DagerIProgrammetRecord> iterator = dagerIProgrammet.iterator();
        DagerIProgrammetRecord e1 = iterator.next();
        assertThat(e1.dagerIProgrammet()).isEqualTo(8);
        assertThat(e1.antall()).isEqualByComparingTo(BigDecimal.valueOf(3));

        DagerIProgrammetRecord e2 = iterator.next();
        assertThat(e2.dagerIProgrammet()).isEqualTo(14);
        assertThat(e2.antall()).isEqualByComparingTo(BigDecimal.valueOf(1));
    }

    private void lagFagsakOgBehandlingMedUngdomsprogram(DatoIntervallEntitet... perioder) {
        var fagsak = byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.LØPENDE);
        lagreFagsaker(List.of(fagsak));

        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        lagreBehandling(behandling);

        // Save all periods in a single call to ensure they're all in the same active grunnlag
        List<UngdomsprogramPeriode> ungdomsprogramPerioder = Arrays.stream(perioder)
            .map(UngdomsprogramPeriode::new)
            .toList();
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), ungdomsprogramPerioder);
    }

    private Fagsak byggFagsak(FagsakYtelseType ytelseType, FagsakStatus fagsakStatus) {
        return FagsakBuilder.nyFagsak(ytelseType)
            .medBruker(AktørId.dummy())
            .medStatus(fagsakStatus)
            .build();
    }

    private void lagreFagsaker(List<Fagsak> fagsaker) {
        for (Fagsak fagsak : fagsaker) {
            fagsakRepository.opprettNy(fagsak);
        }
    }

    private void lagreBehandling(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }
}
