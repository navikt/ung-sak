package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.innhold.TomVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.regler.BehandlingVedtaksbrevResultat;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.Vedtaksbrev;
import no.nav.ung.sak.formidling.vedtak.regler.YtelseVedtaksbrevRegler;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.UngTestScenario;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.BrevTestUtils;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.*;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Test av regler for hvilke vedtaksbrev skal tilbys og egenskaper for redigering og hindring.
 * <p>
 * For rene automatiske brev så er testene som sjekker brevinnhold dekkende. Denne testen sjekker unntak fra de testene
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class YtelseVedtaksbrevReglerTest {

    @Inject
    private EntityManager entityManager;
    private UngTestRepositories ungTestRepositories;

    @Inject
    @Any
    private YtelseVedtaksbrevRegler vedtaksbrevRegler;


    @BeforeEach
    void setup() {
        ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
    }


    @Test
    void skal_ikke_redigere_brev_uten_aksjonspunkt() {
        var behandling = lagBehandling(EndringInntektScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1)));

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isTrue();
        assertThat(totalresultater.vedtaksbrevResultater()).hasSize(1);

        var regelResulat = totalresultater.vedtaksbrevResultater().getFirst();

        assertFullAutomatiskBrev(regelResulat, DokumentMalType.ENDRING_INNTEKT, EndringInntektReduksjonInnholdBygger.class);
    }

    @Test
    void skal_gi_ingen_brev_ved_full_ungdomsprogram_med_ingen_rapportert_inntekt_uten_ap() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var behandling = lagBehandling(EndringInntektScenarioer.endring0KrInntekt_19år(fom));

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isFalse();
        assertThat(totalresultater.ingenBrevResultater()).hasSize(1);

        var regelResulat = totalresultater.ingenBrevResultater().getFirst();
        assertThat(regelResulat.ingenBrevÅrsakType()).isEqualTo(IngenBrevÅrsakType.IKKE_RELEVANT);

        assertThat(regelResulat.forklaring()).containsIgnoringCase("ingen brev");

    }

    @Test
    void skal_gi_ingen_brev_ved_kontroll_siste_måned_ingen_rapportert_inntekt_uten_ap() {
        LocalDate fom = LocalDate.of(2026, 1, 1);
        int inntektSisteMåned = 0;
        var behandling = lagBehandling(EndringInntektScenarioer.endringInntektSisteMåned_10dager(fom, inntektSisteMåned));

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isFalse();
        assertThat(totalresultater.ingenBrevResultater()).hasSize(1);

        var regelResulat = totalresultater.ingenBrevResultater().getFirst();
        assertThat(regelResulat.ingenBrevÅrsakType()).isEqualTo(IngenBrevÅrsakType.IKKE_RELEVANT);

        assertThat(regelResulat.forklaring()).containsIgnoringCase("ingen brev");

    }

    @Test
    void skal_gi_redigerbar_brev_ved_avkortning_men_fastsatt_til_0_kr_og_register_er_0kr() {
        UngTestScenario ungTestGrunnlag = EndringInntektScenarioer.endring10000KrInntekt0KrRegisterInntekt_0krFastsatt(LocalDate.of(2024, 12, 1));
        var behandling = EndringInntektScenarioer.lagBehandlingMedAksjonspunktKontrollerInntekt(ungTestGrunnlag, ungTestRepositories);

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isTrue();
        assertThat(totalresultater.vedtaksbrevResultater()).hasSize(1);

        var regelResulat = totalresultater.vedtaksbrevResultater().getFirst();

        assertRedigerbarBrev(regelResulat, DokumentMalType.ENDRING_INNTEKT_UTEN_REDUKSJON, EndringInntektUtenReduksjonInnholdBygger.class);
    }

    @Test
    void skal_gi_ingen_brev_ved_dødsfall_av_barn_hendelse() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var behandling = lagBehandling(EndringBarnetilleggScenarioer.endringDødsfall(fom, fom.plusDays(4)));

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isFalse();

        assertThat(totalresultater.ingenBrevResultater()).hasSize(1);

        var regelResulat = totalresultater.ingenBrevResultater().getFirst();
        assertThat(regelResulat.ingenBrevÅrsakType()).isEqualTo(IngenBrevÅrsakType.IKKE_IMPLEMENTERT);

        assertThat(regelResulat.forklaring()).containsIgnoringCase("ingen brev");

    }

    @Test
    void skal_gi_ingen_brev_ved_dødsfall_av_barn_under_førstegangsbehandling() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var behandling = lagBehandling(FørstegangsbehandlingScenarioer.innvilget19årMedDødsfallBarn15DagerEtterStartdato(fom));

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isFalse();

        assertThat(totalresultater.ingenBrevResultater()).hasSize(1);

        var regelResulat = totalresultater.ingenBrevResultater().getFirst();
        assertThat(regelResulat.ingenBrevÅrsakType()).isEqualTo(IngenBrevÅrsakType.IKKE_IMPLEMENTERT);

        assertThat(regelResulat.forklaring()).containsIgnoringCase("ingen brev");

    }

    @Test
    void skal_gi_tomt_brev_som_må_redigeres_ved_avslag_aldersvilkår() {
        LocalDate fom = LocalDate.of(2025, 8, 1);
        UngTestScenario ungTestGrunnlag = AvslagScenarioer.avslagAlder(fom);
        var behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medUngTestGrunnlag(ungTestGrunnlag)
            .buildOgLagreMedUng(ungTestRepositories);

        behandling.avsluttBehandling();

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isTrue();
        assertThat(totalresultater.vedtaksbrevResultater()).hasSize(1);

        var regelResulat = totalresultater.vedtaksbrevResultater().getFirst();

        var egenskaper = regelResulat.vedtaksbrevEgenskaper();
        assertThat(regelResulat.vedtaksbrevBygger()).isInstanceOf(TomVedtaksbrevInnholdBygger.class);
        assertThat(regelResulat.dokumentMalType()).isEqualTo(DokumentMalType.MANUELT_VEDTAK_DOK);
        assertThat(egenskaper.kanHindre()).isTrue();
        assertThat(egenskaper.kanOverstyreHindre()).isTrue();
        assertThat(egenskaper.kanRedigere()).isTrue();
        assertThat(egenskaper.kanOverstyreRediger()).isTrue();
        assertThat(regelResulat.forklaring()).contains("avslag");

    }

    @Test
    void skal_ikke_kunne_redigere_eller_hindre_automatisk_brev_for_andre_totrinn_ap_enn_kontroller_inntekt() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        // Bruker aksjonspunkt med totrinn for å trigge redigering av brev

        var behandling = BrevScenarioerUtils.lagAvsluttetBehandlingMedAP(
          EndringHøySatsScenarioer.endring25År(fom.minusYears(25)), ungTestRepositories,
            AksjonspunktDefinisjon.VURDER_TILBAKETREKK
        );

        behandling.avsluttBehandling();

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isTrue();
        assertThat(totalresultater.vedtaksbrevResultater()).hasSize(1);

        var regelResulat = totalresultater.vedtaksbrevResultater().getFirst();

        assertFullAutomatiskBrev(regelResulat, DokumentMalType.ENDRING_HØY_SATS, EndringHøySatsInnholdBygger.class);
        assertThat(regelResulat.forklaring()).contains("høy sats");

    }

    /**
     * RE_ENDRET_SATS brukes for å manuelt trigge førstegansbehandling etter en feilretting.
     */
    @Test
    void skal_gi_førstegangsinnvilgelse_ved_manuell_re_endret_sats() {
        UngTestScenario grunnlag = FørstegangsbehandlingScenarioer.endret_sats(LocalDate.of(2024, 12, 1));

        var behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medManuellOpprettet()
            .medUngTestGrunnlag(grunnlag).buildOgLagreMedUng(ungTestRepositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isTrue();
        assertThat(totalresultater.vedtaksbrevResultater()).hasSize(1);

        var regelResulat = totalresultater.vedtaksbrevResultater().getFirst();

        assertFullAutomatiskBrev(regelResulat, DokumentMalType.INNVILGELSE_DOK, FørstegangsInnvilgelseInnholdBygger.class);
    }


    @Test
    void skal_lage_to_brev_ved_kontroller_inntekt_og_andre_hendelser() {
        LocalDate fom = LocalDate.of(2025, 8, 1);
        var behandling = EndringInntektScenarioer.lagBehandlingMedAksjonspunktKontrollerInntekt(KombinasjonScenarioer.kombinasjon_endringMedInntektOgFødselAvBarn(fom), ungTestRepositories);
        behandling.avsluttBehandling();

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isTrue();
        assertThat(totalresultater.vedtaksbrevResultater()).hasSize(2);

        var inntektResultat = totalresultater.vedtaksbrevResultater().stream()
            .filter(resultat -> resultat.dokumentMalType().equals(DokumentMalType.ENDRING_INNTEKT))
            .findFirst()
            .orElseThrow();

        assertRedigerbarBrev(inntektResultat, DokumentMalType.ENDRING_INNTEKT, EndringInntektReduksjonInnholdBygger.class);

        assertThat(inntektResultat.forklaring()).contains(AksjonspunktDefinisjon.KONTROLLER_INNTEKT.getKode());

        var barnetilleggResultat = totalresultater.vedtaksbrevResultater().stream()
            .filter(resultat -> resultat.dokumentMalType().equals(DokumentMalType.ENDRING_BARNETILLEGG))
            .findFirst()
            .orElseThrow();

        assertFullAutomatiskBrev(barnetilleggResultat, DokumentMalType.ENDRING_BARNETILLEGG, EndringBarnetilleggInnholdBygger.class);

        assertThat(barnetilleggResultat.forklaring()).doesNotContain(AksjonspunktDefinisjon.KONTROLLER_INNTEKT.getKode());
        assertThat(barnetilleggResultat.forklaring()).contains("barn");
    }

    @Test
    void skal_gi_forlenget_brev_selv_om_inntektskontroll_gir_ingen_brev() {
        LocalDate fom = LocalDate.of(2025, 1, 1);
        LocalDate opprinneligSluttdato = fom.plusWeeks(52).minusDays(1);
        LocalDate nySluttdato = opprinneligSluttdato.plusDays(28);

        var behandling = lagBehandling(KombinasjonScenarioer.kombinasjon_forlengetPeriodeOgKontrollInntektFullUtbetaling(fom, opprinneligSluttdato, nySluttdato));

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isTrue();
        assertThat(totalresultater.vedtaksbrevResultater()).hasSize(1);

        var forlengetResultat = totalresultater.vedtaksbrevResultater().stream()
            .filter(resultat -> resultat.dokumentMalType().equals(DokumentMalType.FORLENGET_PERIODE))
            .findFirst()
            .orElseThrow();

        assertFullAutomatiskBrev(forlengetResultat, DokumentMalType.FORLENGET_PERIODE, ForlengetPeriodeInnholdBygger.class);
    }

    @Test
    void skal_gi_to_brev_ved_forlenget_periode_og_kontroll_inntekt_med_reduksjon() {
        LocalDate fom = LocalDate.of(2025, 1, 1);
        LocalDate opprinneligSluttdato = fom.plusWeeks(52).minusDays(1);
        LocalDate nySluttdato = opprinneligSluttdato.plusDays(28);

        var behandling = lagBehandling(KombinasjonScenarioer.kombinasjon_forlengetPeriodeOgKontrollInntektMedReduksjon(fom, opprinneligSluttdato, nySluttdato));

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isTrue();
        assertThat(totalresultater.vedtaksbrevResultater()).hasSize(2);

        var forlengetResultat = totalresultater.vedtaksbrevResultater().stream()
            .filter(resultat -> resultat.dokumentMalType().equals(DokumentMalType.FORLENGET_PERIODE))
            .findFirst()
            .orElseThrow();

        assertFullAutomatiskBrev(forlengetResultat, DokumentMalType.FORLENGET_PERIODE, ForlengetPeriodeInnholdBygger.class);

        var inntektResultat = totalresultater.vedtaksbrevResultater().stream()
            .filter(resultat -> resultat.dokumentMalType().equals(DokumentMalType.ENDRING_INNTEKT))
            .findFirst()
            .orElseThrow();

        assertFullAutomatiskBrev(inntektResultat, DokumentMalType.ENDRING_INNTEKT, EndringInntektReduksjonInnholdBygger.class);
    }

    @Test
    void skal_overstyre_opphor_ved_maksdato_brev_med_forlenget_periode_brev() {
        LocalDate fom = LocalDate.now().minusWeeks(52).plusWeeks(2);
        LocalDate opprinneligSluttdato = fom.plusWeeks(52).minusDays(1);
        LocalDate nySluttdato = opprinneligSluttdato.plusWeeks(8).minusDays(1);

        var scenario = leggTilVarselOpphørVedMaksdato(
            ForlengetPeriodeScenarioer.forlengetPeriode(fom, opprinneligSluttdato, nySluttdato),
            nySluttdato);
        var behandling = lagBehandling(scenario);

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isTrue();
        assertThat(totalresultater.vedtaksbrevResultater())
            .extracting(Vedtaksbrev::dokumentMalType)
            .contains(DokumentMalType.FORLENGET_PERIODE)
            .doesNotContain(DokumentMalType.OPPHOR_VED_MAKSDATO_DOK);
    }

    @Test
    void skal_overstyre_opphor_ved_maksdato_brev_med_programperiodeendring_ved_manuelt_opphor() {
        LocalDate fom = LocalDate.of(2025, 1, 1);
        LocalDate opprinneligSluttdato = fom.plusWeeks(52).minusDays(1);
        LocalDate nySluttdato = opprinneligSluttdato.minusDays(20);

        var scenario = leggTilVarselOpphørVedMaksdato(
            EndringProgramPeriodeScenarioer.endringOpphør(new LocalDateInterval(fom, opprinneligSluttdato), nySluttdato),
            opprinneligSluttdato);
        var behandling = lagBehandling(scenario);

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());
        assertThat(totalresultater.harBrev()).isTrue();
        assertThat(totalresultater.vedtaksbrevResultater())
            .extracting(Vedtaksbrev::dokumentMalType)
            .contains(DokumentMalType.ENDRING_PROGRAMPERIODE)
            .doesNotContain(DokumentMalType.OPPHOR_VED_MAKSDATO_DOK);
    }

     static void assertRedigerbarBrev(Vedtaksbrev vedtaksbrev, DokumentMalType dokumentMalType, Class<? extends VedtaksbrevInnholdBygger> type) {
        var egenskaper = vedtaksbrev.vedtaksbrevEgenskaper();
        assertThat(vedtaksbrev.vedtaksbrevBygger()).isInstanceOf(type);
        assertThat(vedtaksbrev.dokumentMalType()).isEqualTo(dokumentMalType);
        assertThat(egenskaper.kanHindre()).isFalse();
        assertThat(egenskaper.kanOverstyreHindre()).isFalse();
        assertThat(egenskaper.kanRedigere()).isTrue();
        assertThat(egenskaper.kanOverstyreRediger()).isTrue();
    }

    private static void assertFullAutomatiskBrev(Vedtaksbrev vedtaksbrev, DokumentMalType dokumentMalType, Class<? extends VedtaksbrevInnholdBygger> type) {
        var egenskaper = vedtaksbrev.vedtaksbrevEgenskaper();
        assertThat(vedtaksbrev.vedtaksbrevBygger()).isInstanceOf(type);
        assertThat(vedtaksbrev.dokumentMalType()).isEqualTo(dokumentMalType);
        assertThat(egenskaper.kanHindre()).isFalse();
        assertThat(egenskaper.kanRedigere()).isFalse();
        assertThat(egenskaper.kanOverstyreRediger()).isFalse();
    }


    private Behandling lagBehandling(UngTestScenario ungTestGrunnlag) {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestGrunnlag);

        var behandling = scenarioBuilder.buildOgLagreMedUng(ungTestRepositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);

        behandling.avsluttBehandling();

        return behandling;
    }

    private static UngTestScenario leggTilVarselOpphørVedMaksdato(UngTestScenario scenario, LocalDate maksdato) {
        var triggere = new HashSet<>(scenario.behandlingTriggere());
        triggere.add(new Trigger(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO, DatoIntervallEntitet.fraOgMedTilOgMed(maksdato, maksdato)));
        return new UngTestScenario(
            scenario.navn(),
            scenario.programPerioder(),
            scenario.satser(),
            scenario.uttakPerioder(),
            scenario.tilkjentYtelsePerioder(),
            scenario.aldersvilkår(),
            scenario.ungdomsprogramvilkår(),
            scenario.fødselsdato(),
            scenario.søknadStartDato(),
            triggere,
            scenario.barn(),
            scenario.dødsdato(),
            scenario.kontrollerInntektPerioder(),
            maksdato);
    }

}
