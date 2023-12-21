package no.nav.k9.sak.behandlingslager.behandling.uttak;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.k9.kodeverk.behandling.BehandlingType;
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
    private BehandlingRepository behandlingRepository;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private OverstyrUttakRepository overstyrUttakRepository;

    private Arbeidsgiver arbeidsgiver1 = Arbeidsgiver.virksomhet("111111111");
    private InternArbeidsforholdRef arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
    private LocalDate dag1 = LocalDate.now();
    private LocalDate dag2 = dag1.plusDays(1);
    private LocalDate dag3 = dag1.plusDays(2);
    private Long originalBehandlingId;
    private Behandling originalBehandling;

    @BeforeEach
    void setUp() {
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker("saksbehandler1");

        Fagsak fagsak = lagFagsak();
        originalBehandlingId = lagBehandling(fagsak);
    }

    @Test
    void skal_ikke_ha_overstyring_i_utgangspunktet() {
        assertThat(overstyrUttakRepository.hentOverstyrtUttak(originalBehandlingId)).isEqualTo(LocalDateTimeline.empty());
    }

    @Test
    void skal_lage_kopi_av_eksisterende() {
        LocalDateInterval periode1 = new LocalDateInterval(dag1, dag1);
        LocalDateInterval periode2 = new LocalDateInterval(dag2, dag2);
        OverstyrtUttakPeriode overstyrtUttakPeriodePeriode1 = new OverstyrtUttakPeriode(null, new BigDecimal("0.35"), Set.of(), "begrunnelse");
        OverstyrtUttakPeriode overstyrtUttakPeriodePeriode2 = new OverstyrtUttakPeriode(null, new BigDecimal("0.35"), Set.of(new OverstyrtUttakUtbetalingsgrad(UttakArbeidType.ARBEIDSTAKER, arbeidsgiver1, arbeidsforholdRef, new BigDecimal("0.23"))), "begrunnelse");

        LocalDateTimeline<OverstyrtUttakPeriode> oppdateringer = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(periode1, overstyrtUttakPeriodePeriode1),
            new LocalDateSegment<>(periode2, overstyrtUttakPeriodePeriode2))
        );

        overstyrUttakRepository.oppdaterOverstyringAvUttak(originalBehandlingId, List.of(), oppdateringer);

        var revurderingBehandlingId = lagRevurdering(originalBehandling);

        overstyrUttakRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, revurderingBehandlingId);


        assertThat(overstyrUttakRepository.hentOverstyrtUttak(revurderingBehandlingId)).isEqualTo(oppdateringer);
    }


    @Test
    void skal_lagre_og_hente_overstyring() {
        LocalDateInterval periode1 = new LocalDateInterval(dag1, dag1);
        LocalDateInterval periode2 = new LocalDateInterval(dag2, dag2);
        OverstyrtUttakPeriode overstyrtUttakPeriodePeriode1 = new OverstyrtUttakPeriode(null, new BigDecimal("0.35"), Set.of(), "begrunnelse");
        OverstyrtUttakPeriode overstyrtUttakPeriodePeriode2 = new OverstyrtUttakPeriode(null, new BigDecimal("0.35"), Set.of(new OverstyrtUttakUtbetalingsgrad(UttakArbeidType.ARBEIDSTAKER, arbeidsgiver1, arbeidsforholdRef, new BigDecimal("0.23"))), "begrunnelse");

        LocalDateTimeline<OverstyrtUttakPeriode> oppdateringer = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(periode1, overstyrtUttakPeriodePeriode1),
            new LocalDateSegment<>(periode2, overstyrtUttakPeriodePeriode2))
        );

        overstyrUttakRepository.oppdaterOverstyringAvUttak(originalBehandlingId, List.of(), oppdateringer);

        assertThat(overstyrUttakRepository.hentOverstyrtUttak(originalBehandlingId)).isEqualTo(oppdateringer);
    }

    @Test
    void skal_legge_til_ikkeoverlappende_overstyring() {
        LocalDateInterval periode1 = new LocalDateInterval(dag1, dag1);
        OverstyrtUttakPeriode overstyrtUttakPeriodeOrginal = new OverstyrtUttakPeriode(null, new BigDecimal("0.35"), Set.of(), "begrunnelse");
        LocalDateTimeline<OverstyrtUttakPeriode> eksisterendeOverstyringer = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periode1, overstyrtUttakPeriodeOrginal)));
        overstyrUttakRepository.oppdaterOverstyringAvUttak(originalBehandlingId, List.of(), eksisterendeOverstyringer);

        //legger til ny periode
        LocalDateInterval periode2 = new LocalDateInterval(dag2, dag2);
        OverstyrtUttakPeriode overstyrtUttakPeriodeNy = new OverstyrtUttakPeriode(null, new BigDecimal("0.30"), Set.of(new OverstyrtUttakUtbetalingsgrad(UttakArbeidType.ARBEIDSTAKER, arbeidsgiver1, arbeidsforholdRef, new BigDecimal("0.23"))), "begrunnelse");
        LocalDateTimeline<OverstyrtUttakPeriode> oppdaterteOverstyringer = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periode2, overstyrtUttakPeriodeNy)));
        overstyrUttakRepository.oppdaterOverstyringAvUttak(originalBehandlingId, List.of(), oppdaterteOverstyringer);

        assertThat(overstyrUttakRepository.hentOverstyrtUttak(originalBehandlingId)).isEqualTo(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(periode1, overstyrtUttakPeriodeOrginal),
            new LocalDateSegment<>(periode2, overstyrtUttakPeriodeNy)
        )));
    }

    @Test
    void skal_overskrive_eksisterende_overstyring_for_perioden_for_ny_overstyring() {
        LocalDateInterval periodeHele = new LocalDateInterval(dag1, dag3);
        OverstyrtUttakPeriode overstyrtUttakPeriodeOrginal = new OverstyrtUttakPeriode(null, new BigDecimal("0.35"), Set.of(), "begrunnelse");
        LocalDateTimeline<OverstyrtUttakPeriode> eksisterendeOverstyringer = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeHele, overstyrtUttakPeriodeOrginal)));
        overstyrUttakRepository.oppdaterOverstyringAvUttak(originalBehandlingId, List.of(), eksisterendeOverstyringer);


        //overskriver deler av perioden
        LocalDateInterval periode2 = new LocalDateInterval(dag2, dag2);
        OverstyrtUttakPeriode overstyrtUttakPeriodeNy = new OverstyrtUttakPeriode(null, new BigDecimal("0.30"), Set.of(new OverstyrtUttakUtbetalingsgrad(UttakArbeidType.ARBEIDSTAKER, arbeidsgiver1, arbeidsforholdRef, new BigDecimal("0.23"))), "begrunnelse");
        LocalDateTimeline<OverstyrtUttakPeriode> oppdaterteOverstyringer = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periode2, overstyrtUttakPeriodeNy)));
        overstyrUttakRepository.oppdaterOverstyringAvUttak(originalBehandlingId, List.of(), oppdaterteOverstyringer);

        assertThat(overstyrUttakRepository.hentOverstyrtUttak(originalBehandlingId)).isEqualTo(eksisterendeOverstyringer.crossJoin(oppdaterteOverstyringer, StandardCombinators::coalesceRightHandSide));
    }

    @Test
    void skal_overskrive_eksisterende_overstyringer_for_perioden_for_ny_overstyring() {
        LocalDateInterval periodeHele = new LocalDateInterval(dag1, dag3);
        LocalDateInterval periode1 = new LocalDateInterval(dag1, dag1);
        LocalDateInterval periode2 = new LocalDateInterval(dag2, dag2);
        LocalDateInterval periode3 = new LocalDateInterval(dag3, dag3);
        OverstyrtUttakPeriode overstyrtUttakPeriodeOrginal = new OverstyrtUttakPeriode(null, new BigDecimal("0.35"), Set.of(), "begrunnelse");
        OverstyrtUttakPeriode overstyrtUttakPeriodeOrginal2 = new OverstyrtUttakPeriode(null, new BigDecimal("0.36"), Set.of(), "begrunnelse");
        OverstyrtUttakPeriode overstyrtUttakPeriodeNy = new OverstyrtUttakPeriode(null, new BigDecimal("0.30"), Set.of(new OverstyrtUttakUtbetalingsgrad(UttakArbeidType.ARBEIDSTAKER, arbeidsgiver1, arbeidsforholdRef, new BigDecimal("0.23"))), "begrunnelse");

        LocalDateTimeline<OverstyrtUttakPeriode> eksisterendeOverstyringer = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(periode1, overstyrtUttakPeriodeOrginal),
            new LocalDateSegment<>(periode2, overstyrtUttakPeriodeOrginal2),
            new LocalDateSegment<>(periode3, overstyrtUttakPeriodeOrginal))
        );
        overstyrUttakRepository.oppdaterOverstyringAvUttak(originalBehandlingId, List.of(), eksisterendeOverstyringer);


        //overskriver hele perioden
        LocalDateTimeline<OverstyrtUttakPeriode> oppdaterteOverstyringer = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeHele, overstyrtUttakPeriodeNy)));
        overstyrUttakRepository.oppdaterOverstyringAvUttak(originalBehandlingId, List.of(), oppdaterteOverstyringer);

        assertThat(overstyrUttakRepository.hentOverstyrtUttak(originalBehandlingId)).isEqualTo(oppdaterteOverstyringer);
    }

    @Test
    void skal_lagre_og_slette_med_id() {
        assertThat(overstyrUttakRepository.hentOverstyrtUttak(originalBehandlingId)).isEqualTo(LocalDateTimeline.empty());

        LocalDateInterval periode1 = new LocalDateInterval(dag1, dag1);
        LocalDateInterval periode2 = new LocalDateInterval(dag2, dag2);
        OverstyrtUttakPeriode overstyrtUttakPeriodePeriode1 = new OverstyrtUttakPeriode(null, new BigDecimal("0.35"), Set.of(), "begrunnelse");
        OverstyrtUttakPeriode overstyrtUttakPeriodePeriode2 = new OverstyrtUttakPeriode(null, new BigDecimal("0.35"), Set.of(new OverstyrtUttakUtbetalingsgrad(UttakArbeidType.ARBEIDSTAKER, arbeidsgiver1, arbeidsforholdRef, new BigDecimal("0.23"))), "begrunnelse");
        LocalDateTimeline<OverstyrtUttakPeriode> oppdateringer = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(periode1, overstyrtUttakPeriodePeriode1),
            new LocalDateSegment<>(periode2, overstyrtUttakPeriodePeriode2))
        );
        overstyrUttakRepository.oppdaterOverstyringAvUttak(originalBehandlingId, List.of(), oppdateringer);
        LocalDateTimeline<OverstyrtUttakPeriode> eksisterendeOverstyringer = overstyrUttakRepository.hentOverstyrtUttak(originalBehandlingId);

        //fjern periode1 gitt id
        Long idSomSlettes = eksisterendeOverstyringer.intersection(periode1).stream().toList().get(0).getValue().getId();
        overstyrUttakRepository.oppdaterOverstyringAvUttak(originalBehandlingId, List.of(idSomSlettes), LocalDateTimeline.empty());

        //kun periode 2 skal finnes
        assertThat(overstyrUttakRepository.hentOverstyrtUttak(originalBehandlingId)).isEqualTo(new LocalDateTimeline<>(periode2, overstyrtUttakPeriodePeriode2));
    }

    @Test
    void skal_oppdatere_periode_på_eksisterende_overstyring_gitt_id() {
        assertThat(overstyrUttakRepository.hentOverstyrtUttak(originalBehandlingId)).isEqualTo(LocalDateTimeline.empty());

        LocalDateInterval periodeOriginal = new LocalDateInterval(dag1, dag1);
        OverstyrtUttakPeriode overstyrtUttakPeriodePeriode1 = new OverstyrtUttakPeriode(null, new BigDecimal("0.35"), Set.of(), "begrunnelse");
        LocalDateTimeline<OverstyrtUttakPeriode> oppdateringer = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(periodeOriginal, overstyrtUttakPeriodePeriode1))
        );
        overstyrUttakRepository.oppdaterOverstyringAvUttak(originalBehandlingId, List.of(), oppdateringer);
        LocalDateTimeline<OverstyrtUttakPeriode> eksisterendeOverstyringer = overstyrUttakRepository.hentOverstyrtUttak(originalBehandlingId);

        //fjern periode1 gitt id
        LocalDateInterval periodeNy = new LocalDateInterval(dag2, dag2);
        Long id = eksisterendeOverstyringer.stream().toList().get(0).getValue().getId();
        OverstyrtUttakPeriode verdier = new OverstyrtUttakPeriode(id, new BigDecimal("0.35"), Set.of(), "begrunnelse");
        LocalDateTimeline<OverstyrtUttakPeriode> oppdateringer2 = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(periodeNy, verdier))
        );
        overstyrUttakRepository.oppdaterOverstyringAvUttak(originalBehandlingId, List.of(), oppdateringer2);

        //kun ny periode skal finnes
        assertThat(overstyrUttakRepository.hentOverstyrtUttak(originalBehandlingId)).isEqualTo(new LocalDateTimeline<>(periodeNy, verdier));
    }

    private Long lagBehandling(Fagsak fagsak) {
        Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        originalBehandling = builder.build();
        return behandlingRepository.lagre(originalBehandling, behandlingRepository.taSkriveLås(originalBehandling));
    }

    private Long lagRevurdering(Behandling forrige) {
        Behandling.Builder builder = Behandling.fraTidligereBehandling(forrige, BehandlingType.REVURDERING);
        Behandling behandling = builder.build();
        return behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }


    private Fagsak lagFagsak() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), new Saksnummer("AAAAA"), dag1, dag1);
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }
}
