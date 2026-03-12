package no.nav.ung.ytelse.aktivitetspenger.beregning;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.behandling.aktivitetspenger.AktivitetspengerTestScenarioBuilder;
import no.nav.ung.sak.typer.Beløp;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.BeregningInput;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class AktivitetspengerBeregningsgrunnlagRepositoryTest {

    @Inject
    private EntityManager entityManager;

    private AktivitetspengerBeregningsgrunnlagRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AktivitetspengerBeregningsgrunnlagRepository(entityManager);
    }

    @Test
    void aktivitetspengergrunnlag_skal_kunne_gjenbruke_tidligere_bg_og_kunne_ha_flere_bg() {
        Behandling behandling1 = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        {
            var bg1 = lagBeregningsgrunnlag(LocalDate.of(2024, 1, 1));
            repository.lagreBeregningsgrunnlag(behandling1.getId(), bg1);
            entityManager.flush();
        }

        Behandling behandling2 = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        {
            // Gjenbruker beregningsgrunnlag fra behandling1
            var aktivitetspengerGrunnlag1 = repository.hentGrunnlag(behandling1.getId()).orElseThrow();
            var lagretBg1 = aktivitetspengerGrunnlag1.getBeregningsgrunnlag().getFirst();
            repository.lagreBeregningsgrunnlag(behandling2.getId(), lagretBg1);
            entityManager.flush();

            var aktivitetspengerGrunnlag2 = repository.hentGrunnlag(behandling2.getId()).orElseThrow();
            assertThat(aktivitetspengerGrunnlag2.getBeregningsgrunnlag()).hasSize(1);
            assertThat(aktivitetspengerGrunnlag2.getBeregningsgrunnlag().getFirst()).isEqualTo(aktivitetspengerGrunnlag1.getBeregningsgrunnlag().getFirst());
        }

        Behandling behandling3 = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        {
            // Gjenbruker bg1 og lagrer nytt beregningsgrunnlag bg2
            var lagretBg1 = repository.hentGrunnlag(behandling1.getId()).orElseThrow().getBeregningsgrunnlag().getFirst();
            repository.lagreBeregningsgrunnlag(behandling3.getId(), lagretBg1);

            var bg2 = lagBeregningsgrunnlag(LocalDate.of(2024, 2, 1));
            repository.lagreBeregningsgrunnlag(behandling3.getId(), bg2);
        }

        var bgIderBehandling1 = bgIderForBehandling(behandling1.getId());
        var bgIderBehandling2 = bgIderForBehandling(behandling2.getId());
        assertThat(bgIderBehandling1).isEqualTo(bgIderBehandling2);

        var grunnlagBehandling3 = repository.hentGrunnlag(behandling3.getId()).orElseThrow();
        assertThat(grunnlagBehandling3.getBeregningsgrunnlag()).hasSize(2);
    }

    @Test
    void skal_ikke_kunne_legge_til_to_beregningsgrunnlag_med_samme_skjæringstidspunkt() {
        Behandling behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);

        var bg1 = lagBeregningsgrunnlag(LocalDate.of(2024, 1, 1));
        repository.lagreBeregningsgrunnlag(behandling.getId(), bg1);
        entityManager.flush();

        var bg2 = lagBeregningsgrunnlag(LocalDate.of(2024, 1, 1));
        repository.lagreBeregningsgrunnlag(behandling.getId(), bg2);
        entityManager.flush();

        var grunnlag = repository.hentGrunnlag(behandling.getId()).orElseThrow();
        assertThat(grunnlag.getBeregningsgrunnlag()).hasSize(1);
    }

    private static Beregningsgrunnlag lagBeregningsgrunnlag(LocalDate skjæringstidspunkt) {
        var sisteLignedeÅr = Year.of(skjæringstidspunkt.minusYears(1).getYear());
        var input = new BeregningInput(
            new Beløp(BigDecimal.valueOf(500_000)),
            new Beløp(BigDecimal.valueOf(480_000)),
            new Beløp(BigDecimal.valueOf(460_000)),
            skjæringstidspunkt,
            sisteLignedeÅr
        );
        return new Beregningsgrunnlag(
            input,
            BigDecimal.valueOf(500_000),
            BigDecimal.valueOf(480_000),
            BigDecimal.valueOf(500_000),
            BigDecimal.valueOf(330_000),
            "{}"
        );
    }

    @SuppressWarnings("unchecked")
    private List<Long> bgIderForBehandling(Long behandlingId) {
        return entityManager.createNativeQuery(
                "SELECT k.beregningsgrunnlag_id FROM BEREGNINGSGRUNNLAG_KOBLING k " +
                "JOIN AVP_GR_BEREGNINGSGRUNNLAG g ON g.id = k.avp_gr_beregningsgrunnlag_id " +
                "WHERE g.behandling_id = :behandlingId AND g.aktiv = true ")
            .setParameter("behandlingId", behandlingId)
            .getResultList();
    }
}
