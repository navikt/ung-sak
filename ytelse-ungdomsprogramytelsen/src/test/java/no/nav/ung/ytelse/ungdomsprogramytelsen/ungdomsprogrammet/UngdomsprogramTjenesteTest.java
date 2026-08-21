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

import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramMaksPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
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
    void åpen_periode_med_forlenget_periode_bevares_og_maks_dato_lagres() {
        // Registeret sender periodeMaksDato – åpen periode skal bevares, ikke klippes
        var maksDato = LocalDate.of(2026, 2, 27); // fredag
        var behandling = lagBehandling(BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM);
        // Register sender åpen periode med forlenget periode-flagg
        mockRegister(new DeltakerProgramOpplysningDTO(UUID.randomUUID(), "ident", FOM, TIDENES_ENDE, true, maksDato));
        tjeneste.innhentOpplysninger(behandling);

        var perioder = hentLagredePerioder(behandling);
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getPeriode().getFomDato()).isEqualTo(FOM);
        // Åpen periode skal IKKE klippes
        assertThat(perioder.get(0).getPeriode().getTomDato()).isEqualTo(TIDENES_ENDE);
        assertThat(hentMaksDato(behandling)).contains(maksDato);
        assertThat(harForlengetPeriodeLagret(behandling)).isTrue();
    }

    @Test
    void opphør_etter_utvidelse_lagrer_kun_registerets_periode_uten_å_re_derive_utvidelsen() {
        // Pre-betingelse: behandling har allerede et grunnlag der forlenget periode er materialisert
        // (simulerer at grunnlaget ble kopiert over fra forrige behandling som forlenget perioden).
        var behandling = lagBehandling(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        var utvidetTom = LocalDate.of(2026, 1, 25);
        var maksDato = LocalDate.of(2026, 2, 27);
        ungdomsprogramPeriodeRepository.lagre(
            behandling.getId(),
            List.of(new UngdomsprogramPeriode(FOM, utvidetTom)),
            true,
            maksDato);

        // Register sender opphørt periode (klippet tom) med flagg for forlenget periode, uten periodeMaksDato
        var opphørTom = LocalDate.of(2026, 1, 15);
        mockRegister(new DeltakerProgramOpplysningDTO(UUID.randomUUID(), "ident", FOM, opphørTom, true, null));

        tjeneste.innhentOpplysninger(behandling);

        var perioder = hentLagredePerioder(behandling);
        // Skal lagre nøyaktig én periode lik registerets, IKKE legge på resterende dager kant-i-kant
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getPeriode().getFomDato()).isEqualTo(FOM);
        assertThat(perioder.get(0).getPeriode().getTomDato()).isEqualTo(opphørTom);
        // Eksisterende maks-periode bevares selv om register ikke sender periodeMaksDato ved opphør
        assertThat(harForlengetPeriodeLagret(behandling)).isTrue();
        assertThat(hentMaksDato(behandling)).contains(maksDato);
    }

    @Test
    void uten_forlenget_periode_lagrer_registerets_periode_uendret() {
        var behandling = lagBehandling(null);
        var registerTom = LocalDate.of(2025, 11, 30);
        mockRegister(new DeltakerProgramOpplysningDTO(UUID.randomUUID(), "ident", FOM, registerTom, false, null));

        tjeneste.innhentOpplysninger(behandling);

        var perioder = hentLagredePerioder(behandling);
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getPeriode().getFomDato()).isEqualTo(FOM);
        assertThat(perioder.get(0).getPeriode().getTomDato()).isEqualTo(registerTom);
        assertThat(harForlengetPeriodeLagret(behandling)).isFalse();
    }

    @Test
    void tomme_register_opplysninger_lagrer_tomt_grunnlag_uten_forlenget_periode() {
        var behandling = lagBehandling(null);
        when(registerKlient.hentForAktørId(anyString()))
            .thenReturn(new DeltakerOpplysningerDTO(List.of()));

        tjeneste.innhentOpplysninger(behandling);

        assertThat(hentLagredePerioder(behandling)).isEmpty();
        assertThat(harForlengetPeriodeLagret(behandling)).isFalse();
    }

    @Test
    // Verifiserer at lagTimeline.compress() faktisk slår sammen kant-i-kant segmenter
    void tilstøtende_register_segmenter_komprimeres_til_én_periode() {
        var behandling = lagBehandling(null);
        var midt = LocalDate.of(2025, 6, 30);
        var tom = LocalDate.of(2025, 11, 30);
        when(registerKlient.hentForAktørId(anyString()))
            .thenReturn(new DeltakerOpplysningerDTO(List.of(
                new DeltakerProgramOpplysningDTO(UUID.randomUUID(), "ident", FOM, midt, false, null),
                new DeltakerProgramOpplysningDTO(UUID.randomUUID(), "ident", midt.plusDays(1), tom, false, null)
            )));

        tjeneste.innhentOpplysninger(behandling);

        var perioder = hentLagredePerioder(behandling);
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getPeriode().getFomDato()).isEqualTo(FOM);
        assertThat(perioder.get(0).getPeriode().getTomDato()).isEqualTo(tom);
    }

    @Test
    void klippet_periode_fra_register_lagres_uendret_selv_med_maks_dato() {
        // Registeret sender klippet periode (opphør) + maks-dato – klippet periode skal beholdes uendret
        var maksDato = LocalDate.of(2026, 2, 27);
        var opphørTom = LocalDate.of(2026, 1, 15);
        var behandling = lagBehandling(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        mockRegister(new DeltakerProgramOpplysningDTO(UUID.randomUUID(), "ident", FOM, opphørTom, true, maksDato));

        tjeneste.innhentOpplysninger(behandling);

        var perioder = hentLagredePerioder(behandling);
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getPeriode().getTomDato()).isEqualTo(opphørTom);
        assertThat(hentMaksDato(behandling)).contains(maksDato);
    }

    private java.util.Optional<LocalDate> hentMaksDato(Behandling behandling) {
        return ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId())
            .flatMap(g -> g.getPeriodeMaksDato());
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

    private boolean harForlengetPeriodeLagret(Behandling behandling) {
        return ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId())
            .flatMap(UngdomsprogramPeriodeGrunnlag::getUngdomsprogramMaksPeriode)
            .map(UngdomsprogramMaksPeriode::harForlengetPeriode)
            .orElse(false);
    }
}
