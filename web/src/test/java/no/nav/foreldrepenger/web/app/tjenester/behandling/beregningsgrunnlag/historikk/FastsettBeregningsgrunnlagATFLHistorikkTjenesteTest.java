package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
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
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjenesteImpl;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBeregningsgrunnlagATFLDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.InntektPrAndelDto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.integrasjon.organisasjon.OrganisasjonConsumer;

public class FastsettBeregningsgrunnlagATFLHistorikkTjenesteTest {
    private static final BigDecimal GRUNNBELØP = BigDecimal.valueOf(90000);
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final int BRUTTO_PR_AR = 150000;
    private static final int OVERSTYRT_PR_AR = 200000;
    private static final int FRILANSER_INNTEKT = 4000;

    @Rule
    public final UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());

    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
    private VirksomhetTjeneste virksomhetTjeneste = new VirksomhetTjeneste(mock(OrganisasjonConsumer.class), repositoryProvider.getVirksomhetRepository());

    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste = new ArbeidsgiverHistorikkinnslag(new ArbeidsgiverTjenesteImpl(null, virksomhetTjeneste));

    private FastsettBeregningsgrunnlagATFLHistorikkTjeneste fastsettBeregningsgrunnlagATFLHistorikkTjeneste;

    private Behandling behandling;
    private VirksomhetEntitet virk;
    private AbstractTestScenario<?> scenario;


    @Before
    public void setup() {
        when(inntektArbeidYtelseTjeneste.hentGrunnlag(anyLong())).thenReturn(InntektArbeidYtelseGrunnlagBuilder.nytt().build());
        fastsettBeregningsgrunnlagATFLHistorikkTjeneste = new FastsettBeregningsgrunnlagATFLHistorikkTjeneste(lagMockHistory(), arbeidsgiverHistorikkinnslagTjeneste, inntektArbeidYtelseTjeneste);
        virk = new VirksomhetEntitet.Builder()
            .medOrgnr("974760673")
            .medNavn("AF1")
            .oppdatertOpplysningerNå()
            .build();
        repositoryProvider.getVirksomhetRepository().lagre(virk);
    }
    @Test
    public void skal_generere_historikkinnslag_ved_fastsettelse_av_brutto_beregningsgrunnlag_AT() {
        // Arrange
        BeregningsgrunnlagEntitet bg = buildOgLagreBeregningsgrunnlag(false
        );

        //Dto
        FastsettBeregningsgrunnlagATFLDto dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", Collections.singletonList(new InntektPrAndelDto(OVERSTYRT_PR_AR, 1L)), null);

        // Act
        fastsettBeregningsgrunnlagATFLHistorikkTjeneste.lagHistorikk(new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto), dto, bg);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        List<HistorikkinnslagDel> historikkInnslag = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(historikkInnslag).hasSize(1);

        HistorikkinnslagDel del = historikkInnslag.get(0);
        List<HistorikkinnslagFelt> feltList = del.getEndredeFelt();
        assertThat(feltList).hasSize(1);
        assertThat(feltList.get(0)).satisfies(felt -> {
            assertThat(felt.getNavn()).as("navn").isEqualTo(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD.getKode());
            assertThat(felt.getNavnVerdi()).as("navnVerdi").contains("AF1 (974760673) ..."+ARBEIDSFORHOLD_ID.getReferanse().substring(ARBEIDSFORHOLD_ID.getReferanse().length() - 4));
            assertThat(felt.getFraVerdi()).as("fraVerdi").isNull();
            assertThat(felt.getTilVerdi()).as("tilVerdi").isEqualTo("200000");
        });
        assertThat(del.getBegrunnelse()).hasValueSatisfying(begrunnelse -> assertThat(begrunnelse).isEqualTo("begrunnelse"));
    }

    @Test
    public void skal_generere_historikkinnslag_ved_fastsettelse_av_brutto_beregningsgrunnlag_FL() {
        // Arrange
        BeregningsgrunnlagEntitet bg = buildOgLagreBeregningsgrunnlag(true);

        //Dto
        FastsettBeregningsgrunnlagATFLDto dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", FRILANSER_INNTEKT);

        // Act
        fastsettBeregningsgrunnlagATFLHistorikkTjeneste.lagHistorikk(new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto), dto, bg);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        List<HistorikkinnslagDel> historikkInnslag = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(historikkInnslag).hasSize(1);

        HistorikkinnslagDel del = historikkInnslag.get(0);
        List<HistorikkinnslagFelt> feltList = del.getEndredeFelt();
        assertThat(feltList).hasSize(1);
        assertThat(feltList.get(0)).satisfies(felt -> {
            assertThat(felt.getNavn()).as("navn").isEqualTo(HistorikkEndretFeltType.FRILANS_INNTEKT.getKode());
            assertThat(felt.getFraVerdi()).as("fraVerdi").isNull();
            assertThat(felt.getTilVerdi()).as("tilVerdi").isEqualTo("4000");
        });
        assertThat(del.getBegrunnelse()).hasValueSatisfying(begrunnelse -> assertThat(begrunnelse).isEqualTo("begrunnelse"));
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        HistorikkTjenesteAdapter mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }

    private BeregningsgrunnlagEntitet buildOgLagreBeregningsgrunnlag(boolean erFrilans) {
        scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = scenario.lagre(repositoryProvider);
        BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder = BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(LocalDate.now().minusDays(5));

        LocalDate fom = LocalDate.now().minusDays(20);
        leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagBuilder, fom, erFrilans);

        return beregningsgrunnlagBuilder.build();
    }

    private void leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder, LocalDate fomDato, boolean erFrilans) {
        BeregningsgrunnlagPeriode.Builder beregningsgrunnlagPeriodeBuilder = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(fomDato, null);
            leggTilBeregningsgrunnlagPrStatusOgAndel(beregningsgrunnlagPeriodeBuilder, virk, erFrilans);
        beregningsgrunnlagBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriodeBuilder);
    }

    private void leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPeriode.Builder beregningsgrunnlagPeriodeBuilder,
                                                          Virksomhet virksomheten, boolean erFrilans) {

        BeregningsgrunnlagPrStatusOgAndel.Builder builder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medInntektskategori(erFrilans ? Inntektskategori.FRILANSER : Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(erFrilans ? AktivitetStatus.FRILANSER : AktivitetStatus.ARBEIDSTAKER)
            .medBeregnetPrÅr(BigDecimal.valueOf(BRUTTO_PR_AR));
        if (virksomheten != null && !erFrilans) {
            BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold
                .builder()
                .medArbeidsforholdRef(FastsettBeregningsgrunnlagATFLHistorikkTjenesteTest.ARBEIDSFORHOLD_ID)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(virksomheten.getOrgnr()))
                .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
                .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
            builder.medBGAndelArbeidsforhold(bga);
        }
        beregningsgrunnlagPeriodeBuilder.leggTilBeregningsgrunnlagPrStatusOgAndel(
            builder);
    }
}
