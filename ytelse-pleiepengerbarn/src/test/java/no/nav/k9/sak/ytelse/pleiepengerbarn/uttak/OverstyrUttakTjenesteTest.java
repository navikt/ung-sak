package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrUttakRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrtUttakPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@CdiDbAwareTest
class OverstyrUttakTjenesteTest {

    @Inject
    private OverstyrUttakRepository overstyrUttakRepository;
    private OverstyrUttakTjeneste overstyrUttakTjeneste;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private FagsakRepository fagsakRepository;

    @BeforeEach
    void setUp() {
        overstyrUttakTjeneste = new OverstyrUttakTjeneste(null, overstyrUttakRepository, null, null);
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker("saksbehandler1");
    }

    @Test
    void skal_returnere_tom_tidslinje_for_førstegangsbehandling_uten_overstyring() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), AktørId.dummy(), null, new Saksnummer("ABC"), LocalDate.now(), LocalDate.now());
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        fagsakRepository.opprettNy(fagsak);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var tidslinje = overstyrUttakTjeneste.finnEndretTidslinjeFraOriginalBehandling(BehandlingReferanse.fra(behandling));

        assertThat(tidslinje.isEmpty()).isTrue();
    }

    @Test
    void skal_returnere_tidslinje_for_førstegangsbehandling_med_overstyring() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), AktørId.dummy(), null, new Saksnummer("ABC"), LocalDate.now(), LocalDate.now());
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        fagsakRepository.opprettNy(fagsak);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));


        var overstyrtUttakPeriode = new OverstyrtUttakPeriode(null, BigDecimal.TEN, Set.of(), "En begrunnelse");
        var overstyrtPeriode = new LocalDateInterval(LocalDate.now(), LocalDate.now().plusDays(2));
        overstyrUttakRepository.oppdaterOverstyringAvUttak(behandling.getId(), List.of(), new LocalDateTimeline<>(overstyrtPeriode, overstyrtUttakPeriode));

        var tidslinje = overstyrUttakTjeneste.finnEndretTidslinjeFraOriginalBehandling(BehandlingReferanse.fra(behandling));

        assertThat(tidslinje.isEmpty()).isFalse();
        assertThat(tidslinje.getLocalDateIntervals()).isEqualTo(Set.of(overstyrtPeriode));
    }


    @Test
    void skal_returnere_tidslinje_for_revurdering_med_endring_av_overstyring() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), AktørId.dummy(), null, new Saksnummer("ABC"), LocalDate.now(), LocalDate.now());
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        fagsakRepository.opprettNy(fagsak);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var overstyrtUttakPeriode = new OverstyrtUttakPeriode(null, BigDecimal.TEN, Set.of(), "En begrunnelse");
        var overstyrtPeriode = new LocalDateInterval(LocalDate.now(), LocalDate.now().plusDays(2));
        overstyrUttakRepository.oppdaterOverstyringAvUttak(behandling.getId(), List.of(), new LocalDateTimeline<>(overstyrtPeriode, overstyrtUttakPeriode));

        var revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));

        var revurdertOverstyrtUttakPeriode = new OverstyrtUttakPeriode(null, BigDecimal.valueOf(50), Set.of(), "En begrunnelse");
        overstyrUttakRepository.oppdaterOverstyringAvUttak(revurdering.getId(), List.of(), new LocalDateTimeline<>(overstyrtPeriode, revurdertOverstyrtUttakPeriode));

        var tidslinje = overstyrUttakTjeneste.finnEndretTidslinjeFraOriginalBehandling(BehandlingReferanse.fra(revurdering));

        assertThat(tidslinje.isEmpty()).isFalse();
        assertThat(tidslinje.getLocalDateIntervals()).isEqualTo(Set.of(overstyrtPeriode));
    }

    @Test
    void skal_returnere_tom_tidslinje_for_revurdering_uten_endring_av_overstyring() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), AktørId.dummy(), null, new Saksnummer("ABC"), LocalDate.now(), LocalDate.now());
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        fagsakRepository.opprettNy(fagsak);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var overstyrtUttakPeriode = new OverstyrtUttakPeriode(null, BigDecimal.TEN, Set.of(), "En begrunnelse");
        var overstyrtPeriode = new LocalDateInterval(LocalDate.now(), LocalDate.now().plusDays(2));
        overstyrUttakRepository.oppdaterOverstyringAvUttak(behandling.getId(), List.of(), new LocalDateTimeline<>(overstyrtPeriode, overstyrtUttakPeriode));

        var revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));

        var revurdertOverstyrtUttakPeriode = new OverstyrtUttakPeriode(null, BigDecimal.TEN, Set.of(), "En begrunnelse");
        overstyrUttakRepository.oppdaterOverstyringAvUttak(revurdering.getId(), List.of(), new LocalDateTimeline<>(overstyrtPeriode, revurdertOverstyrtUttakPeriode));

        var tidslinje = overstyrUttakTjeneste.finnEndretTidslinjeFraOriginalBehandling(BehandlingReferanse.fra(revurdering));

        assertThat(tidslinje.isEmpty()).isTrue();
    }

    @Test
    void skal_returnere_tidslinje_for_revurdering_med_endring_av_overstyring_uten_fullstendig_overlapp() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), AktørId.dummy(), null, new Saksnummer("ABC"), LocalDate.now(), LocalDate.now());
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        fagsakRepository.opprettNy(fagsak);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var overstyrtUttakPeriode = new OverstyrtUttakPeriode(null, BigDecimal.TEN, Set.of(), "En begrunnelse");
        var overstyrtPeriode = new LocalDateInterval(LocalDate.now(), LocalDate.now().plusDays(2));
        overstyrUttakRepository.oppdaterOverstyringAvUttak(behandling.getId(), List.of(), new LocalDateTimeline<>(overstyrtPeriode, overstyrtUttakPeriode));

        var revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));

        var revurdertOverstyrtUttakPeriode = new OverstyrtUttakPeriode(null, BigDecimal.valueOf(50), Set.of(), "En begrunnelse");
        var overstyrtPeriodeRevurdering = new LocalDateInterval(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        overstyrUttakRepository.oppdaterOverstyringAvUttak(revurdering.getId(), List.of(), new LocalDateTimeline<>(overstyrtPeriodeRevurdering, revurdertOverstyrtUttakPeriode));

        var tidslinje = overstyrUttakTjeneste.finnEndretTidslinjeFraOriginalBehandling(BehandlingReferanse.fra(revurdering));

        assertThat(tidslinje.isEmpty()).isFalse();
        assertThat(tidslinje.getLocalDateIntervals()).isEqualTo(Set.of(
            new LocalDateInterval(LocalDate.now(), LocalDate.now()),
            new LocalDateInterval(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)),
            new LocalDateInterval(LocalDate.now().plusDays(3), LocalDate.now().plusDays(3))));
    }

    @Test
    void skal_returnere_tidslinje_for_revurdering_uten_endring_av_overstyring_uten_fullstendig_overlapp() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), AktørId.dummy(), null, new Saksnummer("ABC"), LocalDate.now(), LocalDate.now());
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        fagsakRepository.opprettNy(fagsak);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var overstyrtUttakPeriode = new OverstyrtUttakPeriode(null, BigDecimal.TEN, Set.of(), "En begrunnelse");
        var overstyrtPeriode = new LocalDateInterval(LocalDate.now(), LocalDate.now().plusDays(2));
        overstyrUttakRepository.oppdaterOverstyringAvUttak(behandling.getId(), List.of(), new LocalDateTimeline<>(overstyrtPeriode, overstyrtUttakPeriode));

        var revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));

        var overstyrtPeriodeRevurdering = new LocalDateInterval(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        overstyrUttakRepository.oppdaterOverstyringAvUttak(revurdering.getId(), List.of(), new LocalDateTimeline<>(overstyrtPeriodeRevurdering, overstyrtUttakPeriode));

        var tidslinje = overstyrUttakTjeneste.finnEndretTidslinjeFraOriginalBehandling(BehandlingReferanse.fra(revurdering));

        assertThat(tidslinje.isEmpty()).isFalse();
        assertThat(tidslinje.getLocalDateIntervals()).isEqualTo(Set.of(
            new LocalDateInterval(LocalDate.now(), LocalDate.now()),
            new LocalDateInterval(LocalDate.now().plusDays(3), LocalDate.now().plusDays(3))));
    }

}
