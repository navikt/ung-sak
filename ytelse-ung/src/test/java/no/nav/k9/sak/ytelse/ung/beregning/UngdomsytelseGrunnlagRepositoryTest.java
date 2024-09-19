package no.nav.k9.sak.ytelse.ung.beregning;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class UngdomsytelseGrunnlagRepositoryTest {


    @Inject
    private EntityManager entityManager;

    private UngdomsytelseGrunnlagRepository repository;

    @BeforeEach
    void setUp() {
        repository = new UngdomsytelseGrunnlagRepository(entityManager);
    }

    @Test
    void skal_kunne_lagre_ned_grunnlag_og_hente_opp_grunnlag() {

        repository.lagre(1L, new UngdomsytelseSatsPerioder(List.of(
            new UngdomsytelseSatsPeriode(
                BigDecimal.TEN,
                DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()),
                BigDecimal.TEN,
                BigDecimal.TEN)
        )));

        var ungdomsytelseGrunnlag = repository.hentGrunnlag(1L);
        assertThat(ungdomsytelseGrunnlag.isPresent()).isTrue();
    }
}
