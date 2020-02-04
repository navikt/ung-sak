package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettBruttoBeregningsgrunnlagSNDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.VurderVarigEndringEllerNyoppstartetSNDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.iay.Inntektskategori;

public class FastsettBruttoBeregningsgrunnlagSNHistorikkTjenesteTest {
    private AbstractTestScenario<?> scenario;
    @Rule
    public final UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private static final int BRUTTO_BG = 200000;

    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
    private Behandling behandling;
    private FastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste fastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste = new FastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste(lagMockHistory());
    private VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste = new VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste(lagMockHistory());


    @Test
    public void skal_generere_historikkinnslag_ved_fastsettelse_av_brutto_beregningsgrunnlag_SN() {
        // Arrange
        boolean varigEndring = true;
        buildOgLagreBeregningsgrunnlag();

        //Dto
        var vurderVarigEndringEllerNyoppstartetSNDto = new VurderVarigEndringEllerNyoppstartetSNDto("begrunnelse1", varigEndring);
        var fastsettBGDto = new FastsettBruttoBeregningsgrunnlagSNDto("begrunnelse2", BRUTTO_BG);

        // Act
        vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste.lagHistorikkInnslag(new AksjonspunktOppdaterParameter(behandling, Optional.empty(), vurderVarigEndringEllerNyoppstartetSNDto), vurderVarigEndringEllerNyoppstartetSNDto);
        fastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste.lagHistorikk(new AksjonspunktOppdaterParameter(behandling, Optional.empty(), fastsettBGDto), fastsettBGDto);

        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        List<HistorikkinnslagDel> historikkInnslag = tekstBuilder.medHendelse(HistorikkinnslagType.FAKTA_ENDRET).build(historikkinnslag);

        // Assert
        assertThat(historikkInnslag).hasSize(2);

        HistorikkinnslagDel del1 = historikkInnslag.get(0);
        assertThat(del1.getEndredeFelt()).hasSize(1);
        assertHistorikkinnslagFelt(del1, HistorikkEndretFeltType.ENDRING_NAERING, null, HistorikkEndretFeltVerdiType.VARIG_ENDRET_NAERING.getKode());
        assertThat(del1.getBegrunnelse()).as("begrunnelse").hasValueSatisfying(begrunnelse -> assertThat(begrunnelse).isEqualTo("begrunnelse1"));
        assertThat(del1.getSkjermlenke()).as("skjermlenkeOpt").hasValueSatisfying(skjermlenke ->
            assertThat(skjermlenke).as("skjermlenke1").isEqualTo(SkjermlenkeType.BEREGNING.getKode()));

        HistorikkinnslagDel del2 = historikkInnslag.get(1);
        assertThat(del2.getEndredeFelt()).hasSize(1);
        assertHistorikkinnslagFelt(del2, HistorikkEndretFeltType.BRUTTO_NAERINGSINNTEKT, null, "200000");
        assertThat(del2.getBegrunnelse()).as("begrunnelse").hasValueSatisfying(begrunnelse -> assertThat(begrunnelse).isEqualTo("begrunnelse2"));
        assertThat(del2.getSkjermlenke()).as("skjermlenke2").isNotPresent();
    }

    private void assertHistorikkinnslagFelt(HistorikkinnslagDel del, HistorikkEndretFeltType historikkEndretFeltType, String fraVerdi, String tilVerdi) {
        Optional<HistorikkinnslagFelt> feltOpt = del.getEndretFelt(historikkEndretFeltType);
        String feltNavn = historikkEndretFeltType.getKode();
        assertThat(feltOpt).hasValueSatisfying(felt -> {
            assertThat(felt.getNavn()).as(feltNavn + ".navn").isEqualTo(feltNavn);
            assertThat(felt.getFraVerdi()).as(feltNavn + ".fraVerdi").isEqualTo(fraVerdi);
            assertThat(felt.getTilVerdi()).as(feltNavn + ".tilVerdi").isEqualTo(tilVerdi);
        });
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        HistorikkTjenesteAdapter mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }

    private BeregningsgrunnlagEntitet buildOgLagreBeregningsgrunnlag() {
        scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = scenario.lagre(repositoryProvider);
        BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder = BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(BigDecimal.ONE)
            .medSkjæringstidspunkt(LocalDate.now().minusDays(5));

        leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagBuilder, LocalDate.now());

        return beregningsgrunnlagBuilder.build();
    }

    private void leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder, LocalDate fomDato) {
        BeregningsgrunnlagPeriode.Builder beregningsgrunnlagPeriodeBuilder = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(fomDato, null);
        leggTilBeregningsgrunnlagPrStatusOgAndel(beregningsgrunnlagPeriodeBuilder);
        beregningsgrunnlagBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriodeBuilder);
    }

    private void leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPeriode.Builder beregningsgrunnlagPeriodeBuilder) {

        BeregningsgrunnlagPrStatusOgAndel.Builder builder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medBeregnetPrÅr(BigDecimal.valueOf(30000));
        beregningsgrunnlagPeriodeBuilder.leggTilBeregningsgrunnlagPrStatusOgAndel(
            builder);
    }
}
