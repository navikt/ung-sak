package no.nav.ung.ytelse.ungdomsprogramytelsen.beregnytelse.gregulering;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlag;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UngdomsytelseKandidatForGReguleringUtlederTest {

    @Test
    void skal_utlede_avvikende_periode_og_ignorere_match_i_samme_periode() {
        var grunnlagRepository = mock(UngdomsytelseGrunnlagRepository.class);
        var grunnlag = mock(UngdomsytelseGrunnlag.class);
        var behandling = mock(Behandling.class);

        var fom = LocalDate.of(2024, 4, 20);
        var tom = LocalDate.of(2024, 5, 10);
        var satser = new UngdomsytelseSatser(BigDecimal.ONE, BigDecimal.valueOf(118620), BigDecimal.ONE, UngdomsytelseSatsType.LAV, 0, 0);

        when(behandling.getId()).thenReturn(1L);
        when(grunnlagRepository.hentGrunnlag(behandling.getId())).thenReturn(Optional.of(grunnlag));
        when(grunnlag.getSatsTidslinje()).thenReturn(new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom, tom, satser))));

        var utleder = new UngdomsytelseKandidatForGReguleringUtleder(grunnlagRepository);

        var resultat = utleder.utledPerioderForGRegulering(behandling, DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));

        assertThat(resultat).containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2024, 5, 1), tom));
    }
}
