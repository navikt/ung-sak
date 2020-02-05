package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.FordelBeregningsgrunnlagHåndterer;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsatteVerdierDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettBeregningsgrunnlagAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettBeregningsgrunnlagPeriodeDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FordelBeregningsgrunnlagDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.RedigerbarAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjenesteImpl;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.FordelBeregningsgrunnlagHistorikkTjeneste;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.Beløp;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class FordelBeregningsgrunnlagOppdatererTest {
    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private final Beløp GRUNNBELØP = new Beløp(600000);
    private static final String ORG_NUMMER = "915933149";
    private static final Arbeidsgiver VIRKSOMHET = Arbeidsgiver.virksomhet(ORG_NUMMER);

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());

    @Inject
    private HistorikkTjenesteAdapter historikkAdapter;

    private InntektArbeidYtelseTjeneste iayTjeneste = mock(InntektArbeidYtelseTjeneste.class);

    private FordelBeregningsgrunnlagOppdaterer fordelBeregningsgrunnlagOppdaterer;

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;

    public TestScenarioBuilder scenario;
    public Behandling behandling;

    private final HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste = Mockito.mock(HentBeregningsgrunnlagTjeneste.class);
    private AksjonspunktTestSupport aksjonspunktRepository = new AksjonspunktTestSupport();
    private Aksjonspunkt aksjonspunkt;

    @Before
    public void setup() {
        this.scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad()
            .medSøknadsdato(LocalDate.now());

        VirksomhetTjeneste virksomhetTjeneste = mock(VirksomhetTjeneste.class);
        when(virksomhetTjeneste.hentOgLagreOrganisasjon(VIRKSOMHET.getIdentifikator()))
            .thenReturn(new VirksomhetEntitet.Builder().medOrgnr(VIRKSOMHET.getOrgnr()).build());
        arbeidsgiverHistorikkinnslagTjeneste = new ArbeidsgiverHistorikkinnslag(new ArbeidsgiverTjenesteImpl(null, virksomhetTjeneste));
        when(iayTjeneste.hentGrunnlag(any(Long.class))).thenReturn(InntektArbeidYtelseGrunnlagBuilder.nytt().build());
        FordelBeregningsgrunnlagHistorikkTjeneste fordelBeregningsgrunnlagHistorikkTjeneste = new FordelBeregningsgrunnlagHistorikkTjeneste(beregningsgrunnlagTjeneste,
            arbeidsgiverHistorikkinnslagTjeneste, historikkAdapter, iayTjeneste);
        FordelBeregningsgrunnlagHåndterer fordelBeregningsgrunnlagHåndterer = Mockito.mock(FordelBeregningsgrunnlagHåndterer.class);

        this.behandling = scenario.lagre(repositoryProvider);
        this.fordelBeregningsgrunnlagOppdaterer = new FordelBeregningsgrunnlagOppdaterer(fordelBeregningsgrunnlagHistorikkTjeneste, fordelBeregningsgrunnlagHåndterer);
        aksjonspunktRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.FORDEL_BEREGNINGSGRUNNLAG);
        aksjonspunkt = aksjonspunktRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.FORDEL_BEREGNINGSGRUNNLAG);
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlag() {
        return BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
    }

    private BeregningsgrunnlagPeriode lagPeriode(BeregningsgrunnlagEntitet forrigeBG, LocalDate fom, LocalDate tom) {
        return BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(fom, tom)
            .build(forrigeBG);
    }


    private FastsettBeregningsgrunnlagAndelDto lagFordeltAndel(BeregningsgrunnlagPrStatusOgAndel andel, InternArbeidsforholdRef arbId, Long andelsnr, boolean nyAndel, boolean lagtTilAvSaksbehandler, Integer refusjon, Integer fastsatt, Inntektskategori inntektskategori) {
        FastsatteVerdierDto fastsatteVerdier = new FastsatteVerdierDto(refusjon, fastsatt, inntektskategori, null);
        RedigerbarAndelDto andelDto = new RedigerbarAndelDto(nyAndel, ORG_NUMMER, arbId, andelsnr, lagtTilAvSaksbehandler, AktivitetStatus.ARBEIDSTAKER, OpptjeningAktivitetType.ARBEID);
        return new FastsettBeregningsgrunnlagAndelDto(andelDto, fastsatteVerdier, Inntektskategori.ARBEIDSTAKER,
            andel != null ? andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(BigDecimal.ZERO).intValue() : null,
            andel != null ? finnBrutto(andel) : null);
    }


    private Integer finnBrutto(BeregningsgrunnlagPrStatusOgAndel andel) {
        return andel.getBruttoPrÅr() == null ? null : andel.getBruttoPrÅr().intValue();
    }

    private BeregningsgrunnlagPrStatusOgAndel buildArbeidstakerAndel(InternArbeidsforholdRef arbId2, Long andelsnr2, BeregningsgrunnlagPeriode periode,
                                                                     BigDecimal refusjonskravPrÅr, boolean lagtTilAvSaksbehandler,
                                                                     Inntektskategori inntektskategori, boolean fastsattAvSaksbehandler, BigDecimal beregnetPrÅr) {
        return BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORG_NUMMER))
                .medArbeidsforholdRef(arbId2).medRefusjonskravPrÅr(refusjonskravPrÅr))
            .medAndelsnr(andelsnr2)
            .medBeregningsperiode(LocalDate.of(2019,7,1), LocalDate.of(2019,10,1))
            .medBeregnetPrÅr(beregnetPrÅr)
            .medLagtTilAvSaksbehandler(lagtTilAvSaksbehandler)
            .medFastsattAvSaksbehandler(fastsattAvSaksbehandler)
            .medInntektskategori(inntektskategori)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode);
    }

    @Test
    public void skal_sette_verdier_på_andel_som_eksisterte_fra_før_i_grunnlag_med_1_periode_og_1_andel_med_refusjon_og_sjekk_historikkinnslag() {

        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        Long andelsnr = 1L;
        BigDecimal refusjonskravPrÅr = BigDecimal.valueOf(120_000);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = lagPeriode(beregningsgrunnlag, SKJÆRINGSTIDSPUNKT, null);
        BeregningsgrunnlagPrStatusOgAndel andel = buildArbeidstakerAndel(arbId, andelsnr, periode,
            refusjonskravPrÅr, false, Inntektskategori.ARBEIDSTAKER, false,null);

        Mockito.when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagAggregatForBehandling(any())).thenReturn(beregningsgrunnlag);

        boolean nyAndel = false;
        boolean lagtTilAvSaksbehandler = false;
        Integer refusjonPrMåned = 5000;
        Integer nyRefusjonPrår = refusjonPrMåned*12;

        Integer fastsatt = 10000;
        Integer fastsattPrÅr = fastsatt*12;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = lagFordeltAndel(andel, arbId, andelsnr, nyAndel, lagtTilAvSaksbehandler, refusjonPrMåned, fastsatt, inntektskategori);
        FastsettBeregningsgrunnlagPeriodeDto endretPeriode = new FastsettBeregningsgrunnlagPeriodeDto(singletonList(fordeltAndel), SKJÆRINGSTIDSPUNKT, null);
        FordelBeregningsgrunnlagDto dto = new FordelBeregningsgrunnlagDto(singletonList(endretPeriode), "Begrunnelse");

        // Act
        fordelBeregningsgrunnlagOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        List<HistorikkinnslagDel> deler = historikkAdapter.tekstBuilder().getHistorikkinnslagDeler();
        List<HistorikkinnslagFelt> historikkinnslagFelt = deler.get(0).getHistorikkinnslagFelt();

        assertThat(deler).hasSize(2);
        assertThat(historikkinnslagFelt).hasSize(5);
        HistorikkinnslagDel del = deler.get(0);

        Optional<HistorikkinnslagFelt> gjeldendeFraFelt = del.getGjeldendeFraFelt();
        assertThat(gjeldendeFraFelt).isPresent();
        assertEndretFelt(gjeldendeFraFelt.get(), HistorikkEndretFeltType.NY_FORDELING, null, dtf.format(SKJÆRINGSTIDSPUNKT),
            arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(andel.getAktivitetStatus(), andel.getArbeidsgiver(), andel.getArbeidsforholdRef(), List.of()));

        assertEndretFelt(del, HistorikkEndretFeltType.NYTT_REFUSJONSKRAV,
            String.valueOf(refusjonskravPrÅr),
            String.valueOf(nyRefusjonPrår));

        assertEndretFelt(del, HistorikkEndretFeltType.INNTEKT, null, String.valueOf(fastsattPrÅr));

        assertEndretFelt(del, HistorikkEndretFeltType.INNTEKTSKATEGORI, null, Inntektskategori.SJØMANN.getKode());

        assertThat(del.getSkjermlenke()).hasValueSatisfying(skjermlenke ->
            assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_OM_FORDELING.getKode()));
    }

    private void assertEndretFelt(HistorikkinnslagDel del,
                                  HistorikkEndretFeltType endretFeltType,
                                  String expectedFraVerdi,
                                  String expectedTilVerdi) {
        Optional<HistorikkinnslagFelt> endretFeltOpt = del.getEndretFelt(endretFeltType);
        assertThat(endretFeltOpt).as("endretFelt").hasValueSatisfying(endretFelt ->
            assertEndretFelt(endretFelt, endretFeltType, expectedFraVerdi, expectedTilVerdi, null));
    }

    private void assertEndretFelt(HistorikkinnslagFelt endretFelt, HistorikkEndretFeltType endretFeltType, String expectedFraVerdi, String expectedTilVerdi, String expectedNavnVerdi) {
        assertThat(endretFelt.getNavn()).as("navn").isEqualTo(endretFeltType.getKode());
        assertThat(endretFelt.getTilVerdi()).as("tilVerdi").isEqualTo(expectedTilVerdi);
        assertThat(endretFelt.getFraVerdi()).as("fraVerdi").isEqualTo(expectedFraVerdi);
        assertThat(endretFelt.getNavnVerdi()).isEqualTo(expectedNavnVerdi);
    }

}
