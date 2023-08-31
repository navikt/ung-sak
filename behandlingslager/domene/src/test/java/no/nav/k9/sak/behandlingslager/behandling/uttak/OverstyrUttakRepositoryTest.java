package no.nav.k9.sak.behandlingslager.behandling.uttak;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Saksnummer;

@CdiDbAwareTest
class OverstyrUttakRepositoryTest {

    @Inject
    BehandlingRepository behandlingRepository;
    @Inject
    FagsakRepository fagsakRepository;
    @Inject
    OverstyrUttakRepository overstyrUttakRepository;

    Arbeidsgiver arbeidsgiver1 = Arbeidsgiver.virksomhet("111111111");
    InternArbeidsforholdRef arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
    Arbeidsgiver arbeidsgiver2 = Arbeidsgiver.virksomhet("222222222");
    Arbeidsgiver arbeidsgiverPerson = Arbeidsgiver.person(new AktørId("3333333333333"));

    LocalDate dag1 = LocalDate.now();
    LocalDate dag2 = dag1.plusDays(1);
    LocalDate dag3 = dag1.plusDays(2);
    Long behandlingId;

    @BeforeEach
    void setUp() {
        Fagsak fagsak = lagFagsak();
        behandlingId = lagBehandling(fagsak);
    }

    @Test
    void skal_lagre_og_hente_overstyring() {
        LocalDateInterval periode1 = new LocalDateInterval(dag1, dag1);
        LocalDateInterval periode2 = new LocalDateInterval(dag2, dag2);
        OverstyrtUttakPeriode overstyrtUttakPeriodePeriode1 = new OverstyrtUttakPeriode(new BigDecimal("0.35"), Set.of());
        OverstyrtUttakPeriode overstyrtUttakPeriodePeriode2 = new OverstyrtUttakPeriode(new BigDecimal("0.35"), Set.of(new OverstyrtUttakUtbetalingsgrad(UttakArbeidType.ARBEIDSTAKER, arbeidsgiver1, arbeidsforholdRef, new BigDecimal("0.23"))));
        overstyrUttakRepository.leggTilOverstyring(behandlingId, periode1, overstyrtUttakPeriodePeriode1);
        overstyrUttakRepository.leggTilOverstyring(behandlingId, periode2, overstyrtUttakPeriodePeriode2);

        Assertions.assertThat(overstyrUttakRepository.harOverstyring(behandlingId)).isTrue();
        Assertions.assertThat(overstyrUttakRepository.hentOverstyrtUttak(behandlingId)).isEqualTo(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(periode1, overstyrtUttakPeriodePeriode1),
            new LocalDateSegment<>(periode2, overstyrtUttakPeriodePeriode2))
        ));

    }

    @Test
    void skal_overskrive_eksisterende_overstyring_for_perioden_for_ny_overstyring() {
        Assertions.assertThat(overstyrUttakRepository.harOverstyring(behandlingId)).isFalse();
        Assertions.assertThat(overstyrUttakRepository.hentOverstyrtUttak(behandlingId)).isEqualTo(LocalDateTimeline.empty());

        LocalDateInterval periodeHele = new LocalDateInterval(dag1, dag3);
        LocalDateInterval periode1 = new LocalDateInterval(dag1, dag1);
        LocalDateInterval periode2 = new LocalDateInterval(dag2, dag2);
        LocalDateInterval periode3 = new LocalDateInterval(dag3, dag3);
        OverstyrtUttakPeriode overstyrtUttakPeriodeOrginal = new OverstyrtUttakPeriode(new BigDecimal("0.35"), Set.of());
        OverstyrtUttakPeriode overstyrtUttakPeriodeNy = new OverstyrtUttakPeriode(new BigDecimal("0.30"), Set.of(new OverstyrtUttakUtbetalingsgrad(UttakArbeidType.ARBEIDSTAKER, arbeidsgiver1, arbeidsforholdRef, new BigDecimal("0.23"))));
        overstyrUttakRepository.leggTilOverstyring(behandlingId, periodeHele, overstyrtUttakPeriodeOrginal);
        overstyrUttakRepository.leggTilOverstyring(behandlingId, periode2, overstyrtUttakPeriodeNy);

        Assertions.assertThat(overstyrUttakRepository.harOverstyring(behandlingId)).isTrue();
        Assertions.assertThat(overstyrUttakRepository.hentOverstyrtUttak(behandlingId)).isEqualTo(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(periode1, overstyrtUttakPeriodeOrginal),
            new LocalDateSegment<>(periode2, overstyrtUttakPeriodeNy),
            new LocalDateSegment<>(periode3, overstyrtUttakPeriodeOrginal))
        ));
    }

    @Test
    void skal_overskrive_eksisterende_overstyringer_for_perioden_for_ny_overstyring() {
        Assertions.assertThat(overstyrUttakRepository.harOverstyring(behandlingId)).isFalse();
        Assertions.assertThat(overstyrUttakRepository.hentOverstyrtUttak(behandlingId)).isEqualTo(LocalDateTimeline.empty());

        LocalDateInterval periodeHele = new LocalDateInterval(dag1, dag3);
        LocalDateInterval periode1 = new LocalDateInterval(dag1, dag1);
        LocalDateInterval periode2 = new LocalDateInterval(dag2, dag2);
        LocalDateInterval periode3 = new LocalDateInterval(dag3, dag3);
        OverstyrtUttakPeriode overstyrtUttakPeriodeOrginal = new OverstyrtUttakPeriode(new BigDecimal("0.35"), Set.of());
        OverstyrtUttakPeriode overstyrtUttakPeriodeOrginal2 = new OverstyrtUttakPeriode(new BigDecimal("0.36"), Set.of());
        OverstyrtUttakPeriode overstyrtUttakPeriodeNy = new OverstyrtUttakPeriode(new BigDecimal("0.30"), Set.of(new OverstyrtUttakUtbetalingsgrad(UttakArbeidType.ARBEIDSTAKER, arbeidsgiver1, arbeidsforholdRef, new BigDecimal("0.23"))));
        overstyrUttakRepository.leggTilOverstyring(behandlingId, periode1, overstyrtUttakPeriodeOrginal);
        overstyrUttakRepository.leggTilOverstyring(behandlingId, periode2, overstyrtUttakPeriodeOrginal2);
        overstyrUttakRepository.leggTilOverstyring(behandlingId, periode3, overstyrtUttakPeriodeOrginal);
        overstyrUttakRepository.leggTilOverstyring(behandlingId, periodeHele, overstyrtUttakPeriodeNy);

        Assertions.assertThat(overstyrUttakRepository.harOverstyring(behandlingId)).isTrue();
        Assertions.assertThat(overstyrUttakRepository.hentOverstyrtUttak(behandlingId)).isEqualTo(new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeHele, overstyrtUttakPeriodeNy))));
    }

    private Long lagBehandling(Fagsak fagsak) {
        Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = builder.build();
        return behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }


    private Fagsak lagFagsak() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), new Saksnummer("AAAAA"), dag1, dag1);
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }
}
