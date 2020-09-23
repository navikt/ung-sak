package no.nav.k9.sak.web.app.tjenester.behandling.historikk;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.InntektEndring;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class InntektHistorikkTjenesteTest {

    public static final String ORGNR = "123456789";
    public static final String ORGANISASJONEN = "Organisasjonen";
    private VirksomhetTjeneste virksomhetTjeneste = Mockito.mock(VirksomhetTjeneste.class);
    private InntektHistorikkTjeneste inntektHistorikkTjeneste = new InntektHistorikkTjeneste(
        new ArbeidsgiverHistorikkinnslag(
        new ArbeidsgiverTjeneste(null, virksomhetTjeneste)));

    @Before
    public void setUp() {
        Virksomhet.Builder virksomhetBuilder = new Virksomhet.Builder();
        virksomhetBuilder.medOrgnr(ORGNR);
        virksomhetBuilder.medNavn(ORGANISASJONEN);
        when(virksomhetTjeneste.hentOrganisasjon(ORGNR)).thenReturn(virksomhetBuilder.build());
    }

    @Test
    public void skal_lage_historikk_når_inntekt_blir_satt() {
        // Arrange
        BigDecimal månedsbeløp = BigDecimal.TEN;
        BeregningsgrunnlagPrStatusOgAndelEndring andelEndring = new BeregningsgrunnlagPrStatusOgAndelEndring(
            new InntektEndring(null, månedsbeløp.multiply(BigDecimal.valueOf(12))),
            null,
            null,
            AktivitetStatus.ARBEIDSTAKER,
            OpptjeningAktivitetType.ARBEID,
            Arbeidsgiver.virksomhet(ORGNR),
            InternArbeidsforholdRef.nullRef()
        );
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

        // Act
        inntektHistorikkTjeneste.lagHistorikkOmEndret(tekstBuilder, List.of(), andelEndring);
        tekstBuilder.ferdigstillHistorikkinnslagDel();

        // Assert
        List<HistorikkinnslagDel> historikkinnslagDeler = tekstBuilder.getHistorikkinnslagDeler();
        assertThat(historikkinnslagDeler.size()).isEqualTo(1);
        assertThat(historikkinnslagDeler.get(0).getHistorikkinnslagFelt().size()).isEqualTo(1);
        HistorikkinnslagFelt historikkinnslagFelt = historikkinnslagDeler.get(0).getHistorikkinnslagFelt().get(0);
        assertThat(historikkinnslagFelt.getTilVerdi()).isEqualTo(månedsbeløp.toString());
        assertThat(historikkinnslagFelt.getNavnVerdi()).isEqualTo(ORGANISASJONEN + " (" + ORGNR + ")");
    }

    @Test
    public void skal_lage_historikk_når_inntekt_blir_satt_med_forrige() {
        // Arrange
        BigDecimal månedsbeløp = BigDecimal.TEN;
        BigDecimal forrigeMånedsbeløp = BigDecimal.ONE;
        BeregningsgrunnlagPrStatusOgAndelEndring andelEndring = new BeregningsgrunnlagPrStatusOgAndelEndring(
            new InntektEndring(forrigeMånedsbeløp.multiply(BigDecimal.valueOf(12)), månedsbeløp.multiply(BigDecimal.valueOf(12))),
            null,
            null,
            AktivitetStatus.ARBEIDSTAKER,
            OpptjeningAktivitetType.ARBEID,
            Arbeidsgiver.virksomhet(ORGNR),
            InternArbeidsforholdRef.nullRef()
        );
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

        // Act
        inntektHistorikkTjeneste.lagHistorikkOmEndret(tekstBuilder, List.of(), andelEndring);
        tekstBuilder.ferdigstillHistorikkinnslagDel();

        // Assert
        List<HistorikkinnslagDel> historikkinnslagDeler = tekstBuilder.getHistorikkinnslagDeler();
        assertThat(historikkinnslagDeler.size()).isEqualTo(1);
        assertThat(historikkinnslagDeler.get(0).getHistorikkinnslagFelt().size()).isEqualTo(1);
        HistorikkinnslagFelt historikkinnslagFelt = historikkinnslagDeler.get(0).getHistorikkinnslagFelt().get(0);
        assertThat(historikkinnslagFelt.getFraVerdi()).isEqualTo(forrigeMånedsbeløp.toString());
        assertThat(historikkinnslagFelt.getTilVerdi()).isEqualTo(månedsbeløp.toString());
        assertThat(historikkinnslagFelt.getNavnVerdi()).isEqualTo(ORGANISASJONEN + " (" + ORGNR + ")");
    }

    @Test
    public void skal_lage_historikk_for_etterlønn_sluttpakke() {
        // Arrange
        BigDecimal månedsbeløp = BigDecimal.TEN;
        BigDecimal forrigeMånedsbeløp = BigDecimal.ONE;
        BeregningsgrunnlagPrStatusOgAndelEndring andelEndring = new BeregningsgrunnlagPrStatusOgAndelEndring(
            new InntektEndring(forrigeMånedsbeløp.multiply(BigDecimal.valueOf(12)), månedsbeløp.multiply(BigDecimal.valueOf(12))),
            null,
            null,
            AktivitetStatus.ARBEIDSTAKER,
            OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE,
            null,
            InternArbeidsforholdRef.nullRef()
        );
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

        // Act
        inntektHistorikkTjeneste.lagHistorikkOmEndret(tekstBuilder, List.of(), andelEndring);
        tekstBuilder.ferdigstillHistorikkinnslagDel();

        // Assert
        List<HistorikkinnslagDel> historikkinnslagDeler = tekstBuilder.getHistorikkinnslagDeler();
        assertThat(historikkinnslagDeler.size()).isEqualTo(1);
        assertThat(historikkinnslagDeler.get(0).getHistorikkinnslagFelt().size()).isEqualTo(1);
        HistorikkinnslagFelt historikkinnslagFelt = historikkinnslagDeler.get(0).getHistorikkinnslagFelt().get(0);
        assertThat(historikkinnslagFelt.getFraVerdi()).isEqualTo(forrigeMånedsbeløp.toString());
        assertThat(historikkinnslagFelt.getTilVerdi()).isEqualTo(månedsbeløp.toString());
        assertThat(historikkinnslagFelt.getNavn()).isEqualTo(HistorikkEndretFeltType.FASTSETT_ETTERLØNN_SLUTTPAKKE.toString());
    }

    @Test
    public void skal_lage_historikk_for_frilans() {
        // Arrange
        BigDecimal månedsbeløp = BigDecimal.TEN;
        BigDecimal forrigeMånedsbeløp = BigDecimal.ONE;
        BeregningsgrunnlagPrStatusOgAndelEndring andelEndring = new BeregningsgrunnlagPrStatusOgAndelEndring(
            new InntektEndring(forrigeMånedsbeløp.multiply(BigDecimal.valueOf(12)), månedsbeløp.multiply(BigDecimal.valueOf(12))),
            null,
            null,
            AktivitetStatus.FRILANSER,
            OpptjeningAktivitetType.FRILANS,
            null,
            InternArbeidsforholdRef.nullRef()
        );
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

        // Act
        inntektHistorikkTjeneste.lagHistorikkOmEndret(tekstBuilder, List.of(), andelEndring);
        tekstBuilder.ferdigstillHistorikkinnslagDel();

        // Assert
        List<HistorikkinnslagDel> historikkinnslagDeler = tekstBuilder.getHistorikkinnslagDeler();
        assertThat(historikkinnslagDeler.size()).isEqualTo(1);
        assertThat(historikkinnslagDeler.get(0).getHistorikkinnslagFelt().size()).isEqualTo(1);
        HistorikkinnslagFelt historikkinnslagFelt = historikkinnslagDeler.get(0).getHistorikkinnslagFelt().get(0);
        assertThat(historikkinnslagFelt.getFraVerdi()).isEqualTo(forrigeMånedsbeløp.toString());
        assertThat(historikkinnslagFelt.getTilVerdi()).isEqualTo(månedsbeløp.toString());
        assertThat(historikkinnslagFelt.getNavn()).isEqualTo(HistorikkEndretFeltType.FRILANS_INNTEKT.toString());
    }

    @Test
    public void skal_lage_historikk_for_dagpenger() {
        // Arrange
        BigDecimal månedsbeløp = BigDecimal.TEN;
        BigDecimal forrigeMånedsbeløp = BigDecimal.ONE;
        BeregningsgrunnlagPrStatusOgAndelEndring andelEndring = new BeregningsgrunnlagPrStatusOgAndelEndring(
            new InntektEndring(forrigeMånedsbeløp.multiply(BigDecimal.valueOf(12)), månedsbeløp.multiply(BigDecimal.valueOf(12))),
            null,
            null,
            AktivitetStatus.DAGPENGER,
            null,
            null,
            InternArbeidsforholdRef.nullRef()
        );
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

        // Act
        inntektHistorikkTjeneste.lagHistorikkOmEndret(tekstBuilder, List.of(), andelEndring);
        tekstBuilder.ferdigstillHistorikkinnslagDel();

        // Assert
        List<HistorikkinnslagDel> historikkinnslagDeler = tekstBuilder.getHistorikkinnslagDeler();
        assertThat(historikkinnslagDeler.size()).isEqualTo(1);
        assertThat(historikkinnslagDeler.get(0).getHistorikkinnslagFelt().size()).isEqualTo(1);
        HistorikkinnslagFelt historikkinnslagFelt = historikkinnslagDeler.get(0).getHistorikkinnslagFelt().get(0);
        assertThat(historikkinnslagFelt.getFraVerdi()).isEqualTo(forrigeMånedsbeløp.toString());
        assertThat(historikkinnslagFelt.getTilVerdi()).isEqualTo(månedsbeløp.toString());
        assertThat(historikkinnslagFelt.getNavn()).isEqualTo(HistorikkEndretFeltType.DAGPENGER_INNTEKT.toString());
    }

    @Test
    public void skal_lage_historikk_for_selvstendig_næring() {
        // Arrange
        BigDecimal månedsbeløp = BigDecimal.TEN;
        BigDecimal forrigeMånedsbeløp = BigDecimal.ONE;
        BeregningsgrunnlagPrStatusOgAndelEndring andelEndring = new BeregningsgrunnlagPrStatusOgAndelEndring(
            new InntektEndring(forrigeMånedsbeløp.multiply(BigDecimal.valueOf(12)), månedsbeløp.multiply(BigDecimal.valueOf(12))),
            null,
            null,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE,
            null,
            null,
            InternArbeidsforholdRef.nullRef()
        );
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

        // Act
        inntektHistorikkTjeneste.lagHistorikkOmEndret(tekstBuilder, List.of(), andelEndring);
        tekstBuilder.ferdigstillHistorikkinnslagDel();

        // Assert
        List<HistorikkinnslagDel> historikkinnslagDeler = tekstBuilder.getHistorikkinnslagDeler();
        assertThat(historikkinnslagDeler.size()).isEqualTo(1);
        assertThat(historikkinnslagDeler.get(0).getHistorikkinnslagFelt().size()).isEqualTo(1);
        HistorikkinnslagFelt historikkinnslagFelt = historikkinnslagDeler.get(0).getHistorikkinnslagFelt().get(0);
        assertThat(historikkinnslagFelt.getFraVerdi()).isEqualTo(forrigeMånedsbeløp.toString());
        assertThat(historikkinnslagFelt.getTilVerdi()).isEqualTo(månedsbeløp.toString());
        assertThat(historikkinnslagFelt.getNavnVerdi()).isEqualTo(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE.getNavn());
        assertThat(historikkinnslagFelt.getNavn()).isEqualTo(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD.toString());
    }


}
