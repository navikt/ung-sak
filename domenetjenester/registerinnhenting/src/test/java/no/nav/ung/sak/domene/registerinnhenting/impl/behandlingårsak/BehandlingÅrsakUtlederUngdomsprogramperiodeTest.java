package no.nav.ung.sak.domene.registerinnhenting.impl.behandlingårsak;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class BehandlingÅrsakUtlederUngdomsprogramperiodeTest {

    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    private BehandlingÅrsakUtlederUngdomsprogramperiode behandlingÅrsakUtlederUngdomsprogramperiode;

    private Behandling behandling;

    @BeforeEach
    void setUp() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), LocalDate.now(), null);
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling.getId()));
        behandlingÅrsakUtlederUngdomsprogramperiode = new BehandlingÅrsakUtlederUngdomsprogramperiode(ungdomsprogramPeriodeRepository);
    }

    @Test
    void skal_ikke_utlede_årsak_for_periode_til_tidenes_ende() {
        // Arrange
        final var ungdomsprogramPeriodeGrunnlag = lagreGrunnlag(LocalDate.now(), AbstractLocalDateInterval.TIDENES_ENDE);
        final var ungdomsprogramPeriodeGrunnlag2 = lagreGrunnlag(LocalDate.now(), AbstractLocalDateInterval.TIDENES_ENDE);

        // Act
        final var behandlingsårsaker = behandlingÅrsakUtlederUngdomsprogramperiode.utledBehandlingÅrsaker(BehandlingReferanse.fra(behandling), ungdomsprogramPeriodeGrunnlag.getId(), ungdomsprogramPeriodeGrunnlag2.getId());

        // Assert
        assertThat(behandlingsårsaker.size()).isEqualTo(0);
    }

    @Test
    void skal_utlede_årsak_for_periode_med_ulik_startdato_og_lik_sluttdato() {
        // Arrange
        final var ungdomsprogramPeriodeGrunnlag = lagreGrunnlag(LocalDate.now().minusDays(1), LocalDate.now());
        final var ungdomsprogramPeriodeGrunnlag2 = lagreGrunnlag(LocalDate.now(), LocalDate.now());

        // Act
        final var behandlingsårsaker = behandlingÅrsakUtlederUngdomsprogramperiode.utledBehandlingÅrsaker(BehandlingReferanse.fra(behandling), ungdomsprogramPeriodeGrunnlag.getId(), ungdomsprogramPeriodeGrunnlag2.getId());

        // Assert
        assertThat(behandlingsårsaker.size()).isEqualTo(1);
    }

    @Test
    void skal_utlede_årsak_for_periode_med_ulik_startdato_og_ulik_sluttdato() {
        // Arrange
        final var ungdomsprogramPeriodeGrunnlag = lagreGrunnlag(LocalDate.now().minusDays(1), LocalDate.now());
        final var ungdomsprogramPeriodeGrunnlag2 = lagreGrunnlag(LocalDate.now(), LocalDate.now().plusDays(1));

        // Act
        final var behandlingsårsaker = behandlingÅrsakUtlederUngdomsprogramperiode.utledBehandlingÅrsaker(BehandlingReferanse.fra(behandling), ungdomsprogramPeriodeGrunnlag.getId(), ungdomsprogramPeriodeGrunnlag2.getId());

        // Assert
        assertThat(behandlingsårsaker.size()).isEqualTo(2);
    }

    @Test
    void skal_utlede_årsak_for_periode_med_lik_startdato_og_ulik_sluttdato() {
        // Arrange
        final var ungdomsprogramPeriodeGrunnlag = lagreGrunnlag(LocalDate.now().minusDays(1), LocalDate.now());
        final var ungdomsprogramPeriodeGrunnlag2 = lagreGrunnlag(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

        // Act
        final var behandlingsårsaker = behandlingÅrsakUtlederUngdomsprogramperiode.utledBehandlingÅrsaker(BehandlingReferanse.fra(behandling), ungdomsprogramPeriodeGrunnlag.getId(), ungdomsprogramPeriodeGrunnlag2.getId());

        // Assert
        assertThat(behandlingsårsaker.size()).isEqualTo(1);
    }


    private UngdomsprogramPeriodeGrunnlag lagreGrunnlag(LocalDate fom, LocalDate tom) {
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))));
        final var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).get();
        return ungdomsprogramPeriodeGrunnlag;
    }
}
