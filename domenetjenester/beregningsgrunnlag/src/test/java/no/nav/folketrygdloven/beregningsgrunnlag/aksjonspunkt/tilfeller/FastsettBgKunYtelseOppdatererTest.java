package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import static no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.BRUKERS_ANDEL;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FaktaBeregningLagreDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FastsattBrukersAndel;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FastsettBgKunYtelseDto;
import no.nav.k9.sak.typer.Beløp;

public class FastsettBgKunYtelseOppdatererTest {


    private static final Long ANDELSNR = 1L;
    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private final Beløp GRUNNBELØP = new Beløp(600000);

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private RepositoryProvider repositoryProvider = new RepositoryProvider(repositoryRule.getEntityManager());
    private FastsettBgKunYtelseOppdaterer fastsettBgKunYtelseOppdaterer = new FastsettBgKunYtelseOppdaterer();
    public TestScenarioBuilder scenario;
    private AktivitetStatus brukers_andel = BRUKERS_ANDEL;
    private BeregningsgrunnlagEntitet beregningsgrunnlag;

    @Before
    public void setup() {
        this.scenario = TestScenarioBuilder.nyttScenario();
        scenario.lagre(repositoryProvider);
        beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2).minusDays(1))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(ANDELSNR)
            .medLagtTilAvSaksbehandler(false)
            .medAktivitetStatus(brukers_andel)
            .build(periode1);
    }

    @Test
    public void skal_sette_verdier_på_andel_som_eksisterte_fra_før_i_grunnlag_ved_første_utførelse_av_aksjonspunkt() {
        // Arrange
        boolean nyAndel = false;
        boolean lagtTilAvSaksbehandler = false;
        Integer fastsatt = 100000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsattBrukersAndel andel = new FastsattBrukersAndel(nyAndel, ANDELSNR, lagtTilAvSaksbehandler, fastsatt,inntektskategori);
        FastsettBgKunYtelseDto kunYtelseDto = new FastsettBgKunYtelseDto(Collections.singletonList(andel), null);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BG_KUN_YTELSE),
            kunYtelseDto);

        // Act
        fastsettBgKunYtelseOppdaterer.oppdater(dto, null, beregningsgrunnlag, Optional.empty());

        // Assert
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        BeregningsgrunnlagPrStatusOgAndel oppdatert1 = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        assertThat(oppdatert1.getBeregnetPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt*12));
        assertThat(oppdatert1.getInntektskategori()).isEqualTo(inntektskategori);
        assertThat(oppdatert1.getAktivitetStatus()).isEqualTo(BRUKERS_ANDEL);
    }

    @Test
    public void skal_sette_verdier_på_andel_som_eksisterte_fra_før_i_grunnlag_ved_første_utførelse_av_aksjonspunkt_ved_besteberegning() {
        // Arrange
        final boolean nyAndel = false;
        final boolean lagtTilAvSaksbehandler = false;
        Integer fastsatt = 100000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsattBrukersAndel andel = new FastsattBrukersAndel(nyAndel, ANDELSNR, lagtTilAvSaksbehandler, fastsatt,inntektskategori);
        final boolean skalBrukeBesteberegning = true;
        FastsettBgKunYtelseDto kunYtelseDto = new FastsettBgKunYtelseDto(Collections.singletonList(andel), skalBrukeBesteberegning);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BG_KUN_YTELSE),
            kunYtelseDto);

        // Act
        fastsettBgKunYtelseOppdaterer.oppdater(dto, null, beregningsgrunnlag, Optional.empty());

        // Assert
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        BeregningsgrunnlagPrStatusOgAndel oppdatert1 = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        assertThat(oppdatert1.getBeregnetPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt*12));
        assertThat(oppdatert1.getBesteberegningPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt*12));
        assertThat(oppdatert1.getInntektskategori()).isEqualTo(inntektskategori);
        assertThat(oppdatert1.getAktivitetStatus()).isEqualTo(BRUKERS_ANDEL);
    }

    @Test
    public void skal_sette_verdier_på_andel_som_eksisterte_fra_før_i_grunnlag_ved_første_utførelse_av_aksjonspunkt_ved_ikkje_besteberegning() {
        // Arrange
        final boolean nyAndel = false;
        final boolean lagtTilAvSaksbehandler = false;
        Integer fastsatt = 100000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsattBrukersAndel andel = new FastsattBrukersAndel(nyAndel, ANDELSNR, lagtTilAvSaksbehandler, fastsatt,inntektskategori);
        final boolean skalBrukeBesteberegning = false;
        FastsettBgKunYtelseDto kunYtelseDto = new FastsettBgKunYtelseDto(Collections.singletonList(andel), skalBrukeBesteberegning);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BG_KUN_YTELSE),
            kunYtelseDto);

        // Act
        fastsettBgKunYtelseOppdaterer.oppdater(dto, null, beregningsgrunnlag, Optional.empty());

        // Assert
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        BeregningsgrunnlagPrStatusOgAndel oppdatert1 = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        assertThat(oppdatert1.getBeregnetPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt*12));
        assertThat(oppdatert1.getBesteberegningPrÅr()).isNull();
        assertThat(oppdatert1.getInntektskategori()).isEqualTo(inntektskategori);
        assertThat(oppdatert1.getAktivitetStatus()).isEqualTo(BRUKERS_ANDEL);
    }

    @Test
    public void skal_sette_verdier_på_andel_som_eksisterte_fra_før_i_grunnlag_med_fastsatt_lik_overstyrt_i_forrige_utførelse_av_aksonspunkt() {
        // Arrange
        boolean nyAndel = false;
        boolean lagtTilAvSaksbehandler = false;
        Integer fastsatt = 100000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsattBrukersAndel brukersAndel = new FastsattBrukersAndel(nyAndel, ANDELSNR, lagtTilAvSaksbehandler, fastsatt,inntektskategori);
        FastsettBgKunYtelseDto kunYtelseDto = new FastsettBgKunYtelseDto(Collections.singletonList(brukersAndel), null);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BG_KUN_YTELSE),
            kunYtelseDto);

        BeregningsgrunnlagEntitet eksisterendeGrunnlag = beregningsgrunnlag.dypKopi();
        eksisterendeGrunnlag.getBeregningsgrunnlagPerioder().forEach(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().forEach(andel ->
            BeregningsgrunnlagPrStatusOgAndel.builder(andel).medBeregnetPrÅr(BigDecimal.valueOf(fastsatt*12))));

        // Act
        fastsettBgKunYtelseOppdaterer.oppdater(dto, null, beregningsgrunnlag, Optional.of(eksisterendeGrunnlag));

        // Assert
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        BeregningsgrunnlagPrStatusOgAndel oppdatert1 = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        assertThat(oppdatert1.getBeregnetPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt*12));
        assertThat(oppdatert1.getInntektskategori()).isEqualTo(inntektskategori);
        assertThat(oppdatert1.getAktivitetStatus()).isEqualTo(BRUKERS_ANDEL);
    }

    @Test
    public void skal_sette_verdier_på_ny_andel() {
        // Arrange
        boolean nyAndel = true;
        boolean lagtTilAvSaksbehandler = true;
        Integer fastsatt = 100000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsattBrukersAndel brukersAndel = new FastsattBrukersAndel(nyAndel, null, lagtTilAvSaksbehandler, fastsatt,inntektskategori);
        FastsettBgKunYtelseDto kunYtelseDto = new FastsettBgKunYtelseDto(Collections.singletonList(brukersAndel), null);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BG_KUN_YTELSE),
            kunYtelseDto);

        // Act
        fastsettBgKunYtelseOppdaterer.oppdater(dto, null, beregningsgrunnlag, Optional.empty());

        // Assert
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
        List<BeregningsgrunnlagPrStatusOgAndel> lagtTil1 = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(BeregningsgrunnlagPrStatusOgAndel::getLagtTilAvSaksbehandler).collect(Collectors.toList());
        assertThat(lagtTil1).hasSize(1);
        assertThat(lagtTil1.get(0).getBeregnetPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt*12));
        assertThat(lagtTil1.get(0).getInntektskategori()).isEqualTo(inntektskategori);
        assertThat(lagtTil1.get(0).getAktivitetStatus()).isEqualTo(BRUKERS_ANDEL);
        List<BeregningsgrunnlagPrStatusOgAndel> fastsattAvSaksbehandler1 = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> Boolean.TRUE.equals(a.getFastsattAvSaksbehandler())).collect(Collectors.toList());
        assertThat(fastsattAvSaksbehandler1).hasSize(1);
    }

    @Test
    public void skal_sette_verdier_på_andel_lagt_til_av_saksbehandler_ved_tilbakehopp_til_KOFAKBER() {
        // Arrange
        boolean nyAndel = false;
        boolean lagtTilAvSaksbehandler = true;
        BeregningsgrunnlagEntitet førsteGrunnlag = beregningsgrunnlag.dypKopi();
        Long andelsnr = 2133L;
        førsteGrunnlag.getBeregningsgrunnlagPerioder().forEach(periode -> BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(andelsnr)
            .medLagtTilAvSaksbehandler(true)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER)
            .medAktivitetStatus(brukers_andel)
            .build(periode));
        Integer fastsatt = 100000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsattBrukersAndel brukersAndel = new FastsattBrukersAndel(nyAndel, andelsnr, lagtTilAvSaksbehandler, fastsatt,inntektskategori);
        FastsettBgKunYtelseDto kunYtelseDto = new FastsettBgKunYtelseDto(Collections.singletonList(brukersAndel), null);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BG_KUN_YTELSE),
            kunYtelseDto);

        // Act
        fastsettBgKunYtelseOppdaterer.oppdater(dto, null, beregningsgrunnlag, Optional.of(førsteGrunnlag));

        // Assert
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
        List<BeregningsgrunnlagPrStatusOgAndel> lagtTil1 = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(BeregningsgrunnlagPrStatusOgAndel::getLagtTilAvSaksbehandler).collect(Collectors.toList());
        assertThat(lagtTil1).hasSize(1);
        assertThat(lagtTil1.get(0).getBeregnetPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt*12));
        assertThat(lagtTil1.get(0).getInntektskategori()).isEqualTo(inntektskategori);
        assertThat(lagtTil1.get(0).getAktivitetStatus()).isEqualTo(BRUKERS_ANDEL);

        List<BeregningsgrunnlagPrStatusOgAndel> fastsattAvSaksbehandler1 = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> Boolean.TRUE.equals(a.getFastsattAvSaksbehandler())).collect(Collectors.toList());
        assertThat(fastsattAvSaksbehandler1).hasSize(1);
    }

    @Test
    public void skal_sette_verdier_på_andel_lagt_til_av_saksbehandler_ved_tilbakehopp_til_steg_før_KOFAKBER() {
        // Arrange
        BeregningsgrunnlagEntitet førsteGrunnlag = beregningsgrunnlag.dypKopi();
        Long andelsnr = 2133L;
        int overstyrt = 5000;
        førsteGrunnlag.getBeregningsgrunnlagPerioder().forEach(periode -> {
            periode.getBeregningsgrunnlagPrStatusOgAndelList().forEach(andel ->
                BeregningsgrunnlagPrStatusOgAndel.builder(andel)
                    .medBeregnetPrÅr(BigDecimal.valueOf(100000*12))
            );
            BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(andelsnr)
            .medLagtTilAvSaksbehandler(true)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER)
            .medAktivitetStatus(brukers_andel)
            .medBeregnetPrÅr(BigDecimal.valueOf(overstyrt *12))
            .build(periode);
        });
        boolean nyAndel = false;
        boolean lagtTilAvSaksbehandler = true;
        Integer fastsatt = 100000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsattBrukersAndel brukersAndel = new FastsattBrukersAndel(nyAndel, andelsnr, lagtTilAvSaksbehandler, fastsatt,inntektskategori);
        FastsattBrukersAndel brukersAndel2 = new FastsattBrukersAndel(nyAndel, ANDELSNR, false, fastsatt, Inntektskategori.ARBEIDSTAKER);

        FastsettBgKunYtelseDto kunYtelseDto = new FastsettBgKunYtelseDto(Arrays.asList(brukersAndel, brukersAndel2), null);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BG_KUN_YTELSE),
            kunYtelseDto);

        // Act
        fastsettBgKunYtelseOppdaterer.oppdater(dto, null, beregningsgrunnlag, Optional.of(førsteGrunnlag));

        // Assert
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
        List<BeregningsgrunnlagPrStatusOgAndel> lagtTil1 = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(BeregningsgrunnlagPrStatusOgAndel::getLagtTilAvSaksbehandler).collect(Collectors.toList());
        assertThat(lagtTil1).hasSize(1);
        assertThat(lagtTil1.get(0).getBeregnetPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt*12));
        assertThat(lagtTil1.get(0).getInntektskategori()).isEqualTo(inntektskategori);
        assertThat(lagtTil1.get(0).getAktivitetStatus()).isEqualTo(BRUKERS_ANDEL);
        List<BeregningsgrunnlagPrStatusOgAndel> fastsattAvSaksbehandler1 = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> Boolean.TRUE.equals(a.getFastsattAvSaksbehandler())).collect(Collectors.toList());
        assertThat(fastsattAvSaksbehandler1).hasSize(2);
    }

}
