package no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet;

import static no.nav.k9.felles.konfigurasjon.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramRegisterKlient.DeltakerOpplysningerDTO;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class UngdomsprogramTjenesteTest {

    private static final AktørId AKTØR = AktørId.dummy();
    private static final LocalDate FOM = LocalDate.of(2024, 12, 1);

    @Inject
    private EntityManager entityManager;
    @Inject
    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    private UngdomsprogramRegisterKlient registerKlient;
    private UngdomsprogramTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        registerKlient = mock(UngdomsprogramRegisterKlient.class);
        tjeneste = new UngdomsprogramTjeneste(registerKlient, ungdomsprogramPeriodeRepository);
    }

    @Test
    void første_gangs_utvidelse_klipper_åpen_periode_til_300_virkedager() {
        // Behandling trigget av utvidet kvote-hendelse, ingen tidligere utvidelse lagret
        var behandling = lagBehandling(BehandlingÅrsakType.RE_HENDELSE_UTVIDET_KVOTE_UNGDOMSPROGRAM);
        // Register sender åpen periode med utvidet kvote-flagg
        mockRegister(new DeltakerProgramOpplysningDTO(UUID.randomUUID(), "ident", FOM, TIDENES_ENDE, true));

        tjeneste.innhentOpplysninger(behandling);

        var perioder = hentLagredePerioder(behandling);
        assertThat(perioder).hasSize(1);
        var periode = perioder.get(0);
        assertThat(periode.getPeriode().getFomDato()).isEqualTo(FOM);
        // Skal være klippet til en konkret dato før TIDENES_ENDE
        assertThat(periode.getPeriode().getTomDato()).isBefore(TIDENES_ENDE);
        assertThat(periode.getPeriode().getTomDato()).isAfter(FOM.plusWeeks(52));
        assertThat(harUtvidetKvoteLagret(behandling)).isTrue();
    }

    @Test
    void opphør_etter_utvidelse_lagrer_kun_registerets_periode_uten_å_re_derive_utvidelsen() {
        // Pre-betingelse: behandling har allerede et grunnlag der utvidet kvote er materialisert
        // (simulerer at grunnlaget ble kopiert over fra forrige behandling som utvidet kvoten).
        var behandling = lagBehandling(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        var utvidetTom = LocalDate.of(2026, 1, 25);
        ungdomsprogramPeriodeRepository.lagre(
            behandling.getId(),
            List.of(new UngdomsprogramPeriode(FOM, utvidetTom)),
            true);

        // Register sender opphørt periode (klippet tom) med utvidet kvote-flagg
        var opphørTom = LocalDate.of(2026, 1, 15);
        mockRegister(new DeltakerProgramOpplysningDTO(UUID.randomUUID(), "ident", FOM, opphørTom, true));

        tjeneste.innhentOpplysninger(behandling);

        var perioder = hentLagredePerioder(behandling);
        // Skal lagre nøyaktig én periode lik registerets, IKKE legge på resterende kvote kant-i-kant
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getPeriode().getFomDato()).isEqualTo(FOM);
        assertThat(perioder.get(0).getPeriode().getTomDato()).isEqualTo(opphørTom);
        assertThat(harUtvidetKvoteLagret(behandling)).isTrue();
    }

    @Test
    void uten_utvidet_kvote_lagrer_registerets_periode_uendret() {
        var behandling = lagBehandling(null);
        var registerTom = LocalDate.of(2025, 11, 30);
        mockRegister(new DeltakerProgramOpplysningDTO(UUID.randomUUID(), "ident", FOM, registerTom, false));

        tjeneste.innhentOpplysninger(behandling);

        var perioder = hentLagredePerioder(behandling);
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getPeriode().getFomDato()).isEqualTo(FOM);
        assertThat(perioder.get(0).getPeriode().getTomDato()).isEqualTo(registerTom);
        assertThat(harUtvidetKvoteLagret(behandling)).isFalse();
    }

    @Test
    void tomme_register_opplysninger_lagrer_tomt_grunnlag_med_utvidet_kvote_flagg_fra_behandlingsårsak() {
        var behandling = lagBehandling(BehandlingÅrsakType.RE_HENDELSE_UTVIDET_KVOTE_UNGDOMSPROGRAM);
        when(registerKlient.hentForAktørId(anyString()))
            .thenReturn(new DeltakerOpplysningerDTO(List.of()));

        tjeneste.innhentOpplysninger(behandling);

        assertThat(hentLagredePerioder(behandling)).isEmpty();
        assertThat(harUtvidetKvoteLagret(behandling)).isTrue();
    }

    @Test
    void tilstøtende_register_segmenter_komprimeres_til_én_periode() {
        // Verifiserer at lagTimeline.compress() faktisk slår sammen kant-i-kant segmenter
        var behandling = lagBehandling(null);
        var midt = LocalDate.of(2025, 6, 30);
        var tom = LocalDate.of(2025, 11, 30);
        when(registerKlient.hentForAktørId(anyString()))
            .thenReturn(new DeltakerOpplysningerDTO(List.of(
                new DeltakerProgramOpplysningDTO(UUID.randomUUID(), "ident", FOM, midt, false),
                new DeltakerProgramOpplysningDTO(UUID.randomUUID(), "ident", midt.plusDays(1), tom, false)
            )));

        tjeneste.innhentOpplysninger(behandling);

        var perioder = hentLagredePerioder(behandling);
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getPeriode().getFomDato()).isEqualTo(FOM);
        assertThat(perioder.get(0).getPeriode().getTomDato()).isEqualTo(tom);
    }

    private Behandling lagBehandling(BehandlingÅrsakType årsak) {
        var scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.UNGDOMSYTELSE, AKTØR);
        if (årsak != null) {
            scenario.medBehandlingÅrsak(årsak);
        }
        var behandling = scenario.lagre(entityManager);
        scenario.lagreFagsak(behandlingRepositoryProvider);
        return behandling;
    }

    private void mockRegister(DeltakerProgramOpplysningDTO opplysning) {
        when(registerKlient.hentForAktørId(anyString()))
            .thenReturn(new DeltakerOpplysningerDTO(List.of(opplysning)));
    }

    private List<UngdomsprogramPeriode> hentLagredePerioder(Behandling behandling) {
        return ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId())
            .map(g -> g.getUngdomsprogramPerioder().getPerioder().stream().toList())
            .orElse(List.of());
    }

    private boolean harUtvidetKvoteLagret(Behandling behandling) {
        return ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId())
            .flatMap(g -> g.getUngdomsprogramUtvidetKvote())
            .map(k -> k.isHarUtvidetKvote())
            .orElse(false);
    }
}

