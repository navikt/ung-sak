package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsatteVerdierDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettMånedsinntektUtenInntektsmeldingAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettMånedsinntektUtenInntektsmeldingDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.kodeverk.iay.AktivitetStatus;

public class FastsettMånedsinntektUtenInntektsmeldingOppdatererTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, 9, 30);
    public static final String ORGNR = "78327942834";
    public static final String ORGNR2 = "43253634231";
    public static final int ARBEIDSINNTEKT = 120000;

    private HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
    private BeregningsgrunnlagEntitet beregningsgrunnlag;
    private Arbeidsgiver arbeidsgiver;
    private Arbeidsgiver arbeidsgiver2;

    private VirksomhetTjeneste virksomhetTjeneste = mock(VirksomhetTjeneste.class);

    @Before
    public void setUp() {
        arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        arbeidsgiver2 = Arbeidsgiver.virksomhet(ORGNR2);

        Virksomhet virksomhet1 = new VirksomhetEntitet.Builder().medOrgnr(ORGNR).build();
        Virksomhet virksomhet2 = new VirksomhetEntitet.Builder().medOrgnr(ORGNR2).build();
        when(virksomhetTjeneste.hentOgLagreOrganisasjon(ORGNR)).thenReturn(virksomhet1);
        when(virksomhetTjeneste.hentOgLagreOrganisasjon(ORGNR2)).thenReturn(virksomhet2);

        HistorikkTjenesteAdapter historikkTjenesteAdapter = mock(HistorikkTjenesteAdapter.class);
        when(historikkTjenesteAdapter.tekstBuilder()).thenReturn(tekstBuilder);
        beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medGrunnbeløp(BigDecimal.valueOf(91425L))
            .leggTilFaktaOmBeregningTilfeller(singletonList(FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE))
            .build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode periode2 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2).plusDays(1), null)
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder().medAndelsnr(1L)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(arbeidsgiver)).build(periode1);
        BeregningsgrunnlagPrStatusOgAndel.builder().medAndelsnr(5L)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(arbeidsgiver)).build(periode2);
        BeregningsgrunnlagPrStatusOgAndel.builder().medAndelsnr(2L)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(arbeidsgiver2)).build(periode1);
        BeregningsgrunnlagPrStatusOgAndel.builder().medAndelsnr(1L)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(arbeidsgiver2)).build(periode2);
    }

    @Test
    public void skal_sette_inntekt_på_riktige_andeler_i_alle_perioder(){
        var fastsettMånedsinntektUtenInntektsmeldingOppdaterer = new FastsettMånedsinntektUtenInntektsmeldingOppdaterer();
        // Arrange
        FastsettMånedsinntektUtenInntektsmeldingDto dto = new FastsettMånedsinntektUtenInntektsmeldingDto();
        FastsettMånedsinntektUtenInntektsmeldingAndelDto andelDto = new FastsettMånedsinntektUtenInntektsmeldingAndelDto(1L,
            new FastsatteVerdierDto(ARBEIDSINNTEKT, null, null));
        List<FastsettMånedsinntektUtenInntektsmeldingAndelDto> andelListe = singletonList(andelDto);
        dto.setAndelListe(andelListe);
        FaktaBeregningLagreDto faktaLagreDto = new FaktaBeregningLagreDto(singletonList(FaktaOmBeregningTilfelle.FASTSETT_MÅNEDSLØNN_ARBEIDSTAKER_UTEN_INNTEKTSMELDING));
        faktaLagreDto.setFastsattUtenInntektsmelding(dto);

        // Act
        fastsettMånedsinntektUtenInntektsmeldingOppdaterer.oppdater(faktaLagreDto, mock(BehandlingReferanse.class), beregningsgrunnlag, Optional.empty());

        // Assert
        List<BeregningsgrunnlagPrStatusOgAndel> andelerMedFastsattInntekt = beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream().flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.getBgAndelArbeidsforhold().isPresent() && andel.getBgAndelArbeidsforhold().get().getArbeidsgiver().equals(arbeidsgiver))
            .collect(Collectors.toList());
        andelerMedFastsattInntekt.forEach(andel -> assertThat(andel.getBeregnetPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(ARBEIDSINNTEKT)));
        List<BeregningsgrunnlagPrStatusOgAndel> andelerUtenFastsattInntekt = beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream().flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.getBgAndelArbeidsforhold().isPresent() && andel.getBgAndelArbeidsforhold().get().getArbeidsgiver().equals(arbeidsgiver2))
            .collect(Collectors.toList());
        andelerUtenFastsattInntekt.forEach(andel -> assertThat(andel.getBeregnetPrÅr()).isNull());
        }
}
