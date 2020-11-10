package no.nav.k9.sak.web.app.tjenester.behandling.historikk;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.RefusjonEndring;
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

public class RefusjonHistorikkTjenesteTest {

    public static final String ORGNR = "123456789";
    public static final String ORGANISASJONEN = "Organisasjonen";
    private VirksomhetTjeneste virksomhetTjeneste = Mockito.mock(VirksomhetTjeneste.class);
    private RefusjonHistorikkTjeneste refusjonHistorikkTjeneste = new RefusjonHistorikkTjeneste(
        new ArbeidsgiverHistorikkinnslag(
            new ArbeidsgiverTjeneste(null, virksomhetTjeneste)));

    @BeforeEach
    public void setUp() {
        Virksomhet.Builder virksomhetBuilder = new Virksomhet.Builder();
        virksomhetBuilder.medOrgnr(ORGNR);
        virksomhetBuilder.medNavn(ORGANISASJONEN);
        Mockito.when(virksomhetTjeneste.hentOrganisasjon(ORGNR)).thenReturn(virksomhetBuilder.build());
    }

    @Test
    public void skal_lage_historikk_når_refusjon_blir_endret() {
        // Arrange
        BigDecimal gammelRefusjon = BigDecimal.valueOf(200_000L);
        BigDecimal nyRefusjon = BigDecimal.valueOf(350_000L);
        BeregningsgrunnlagPrStatusOgAndelEndring andelEndring = new BeregningsgrunnlagPrStatusOgAndelEndring(
            null,
            null,
            new RefusjonEndring(gammelRefusjon, nyRefusjon),
            AktivitetStatus.ARBEIDSTAKER,
            OpptjeningAktivitetType.ARBEID,
            Arbeidsgiver.virksomhet(ORGNR),
            InternArbeidsforholdRef.nullRef());
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

        // Act
        refusjonHistorikkTjeneste.lagHistorikkOmEndret(tekstBuilder, List.of(), andelEndring);
        tekstBuilder.ferdigstillHistorikkinnslagDel();

        // Assert
        List<HistorikkinnslagDel> historikkinnslagDeler = tekstBuilder.getHistorikkinnslagDeler();
        assertThat(historikkinnslagDeler.size()).isEqualTo(1);
        assertThat(historikkinnslagDeler.get(0).getHistorikkinnslagFelt().size()).isEqualTo(1);
        HistorikkinnslagFelt historikkinnslagFelt = historikkinnslagDeler.get(0).getHistorikkinnslagFelt().get(0);
        assertThat(historikkinnslagFelt.getFraVerdi()).isEqualTo(gammelRefusjon.toString());
        assertThat(historikkinnslagFelt.getTilVerdi()).isEqualTo(nyRefusjon.toString());
        assertThat(historikkinnslagFelt.getNavnVerdi()).isEqualTo(ORGANISASJONEN + " (" + ORGNR + ")");
    }

    @Test
    public void skal_lage_historikk_når_refusjon_blir_satt() {
        // Arrange
        BigDecimal nyRefusjon = BigDecimal.valueOf(350_000L);
        BeregningsgrunnlagPrStatusOgAndelEndring andelEndring = new BeregningsgrunnlagPrStatusOgAndelEndring(
            null,
            null,
            new RefusjonEndring(null, nyRefusjon),
            AktivitetStatus.ARBEIDSTAKER,
            OpptjeningAktivitetType.ARBEID,
            Arbeidsgiver.virksomhet(ORGNR),
            InternArbeidsforholdRef.nullRef());
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

        // Act
        refusjonHistorikkTjeneste.lagHistorikkOmEndret(tekstBuilder, List.of(), andelEndring);
        tekstBuilder.ferdigstillHistorikkinnslagDel();

        // Assert
        List<HistorikkinnslagDel> historikkinnslagDeler = tekstBuilder.getHistorikkinnslagDeler();
        assertThat(historikkinnslagDeler.size()).isEqualTo(1);
        assertThat(historikkinnslagDeler.get(0).getHistorikkinnslagFelt().size()).isEqualTo(1);
        HistorikkinnslagFelt historikkinnslagFelt = historikkinnslagDeler.get(0).getHistorikkinnslagFelt().get(0);
        assertThat(historikkinnslagFelt.getTilVerdi()).isEqualTo(nyRefusjon.toString());
        assertThat(historikkinnslagFelt.getNavnVerdi()).isEqualTo(ORGANISASJONEN + " (" + ORGNR + ")");
    }

    @Test
    public void skal_lage_historikk_for_etterlønn_sluttpakke() {
        // Arrange
        BigDecimal gammelRefusjon = BigDecimal.valueOf(200_000L);
        BigDecimal nyRefusjon = BigDecimal.valueOf(350_000L);
        BeregningsgrunnlagPrStatusOgAndelEndring andelEndring = new BeregningsgrunnlagPrStatusOgAndelEndring(
            null,
            null,
            new RefusjonEndring(gammelRefusjon, nyRefusjon),
            AktivitetStatus.ARBEIDSTAKER,
            OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE,
            null,
            InternArbeidsforholdRef.nullRef());
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

        // Act
        refusjonHistorikkTjeneste.lagHistorikkOmEndret(tekstBuilder, List.of(), andelEndring);
        tekstBuilder.ferdigstillHistorikkinnslagDel();

        // Assert
        List<HistorikkinnslagDel> historikkinnslagDeler = tekstBuilder.getHistorikkinnslagDeler();
        assertThat(historikkinnslagDeler.size()).isEqualTo(1);
        assertThat(historikkinnslagDeler.get(0).getHistorikkinnslagFelt().size()).isEqualTo(1);
        HistorikkinnslagFelt historikkinnslagFelt = historikkinnslagDeler.get(0).getHistorikkinnslagFelt().get(0);
        assertThat(historikkinnslagFelt.getFraVerdi()).isEqualTo(gammelRefusjon.toString());
        assertThat(historikkinnslagFelt.getTilVerdi()).isEqualTo(nyRefusjon.toString());
        assertThat(historikkinnslagFelt.getNavn()).isEqualTo(HistorikkEndretFeltType.FASTSETT_ETTERLØNN_SLUTTPAKKE.toString());
    }

}
