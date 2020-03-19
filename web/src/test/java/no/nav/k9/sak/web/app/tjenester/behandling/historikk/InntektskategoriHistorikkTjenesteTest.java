package no.nav.k9.sak.web.app.tjenester.behandling.historikk;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.output.InntektskategoriEndring;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.k9.sak.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.k9.sak.behandlingslager.virksomhet.VirksomhetRepository;
import no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjenesteImpl;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class InntektskategoriHistorikkTjenesteTest {

    public static final String ORGNR = "123456789";
    public static final String ORGANISASJONEN = "Organisasjonen";
    private final VirksomhetRepository virksomhetRepository = new VirksomhetRepository();
    private InntektskategoriHistorikkTjeneste inntektskategoriHistorikkTjeneste = new InntektskategoriHistorikkTjeneste(
        new ArbeidsgiverHistorikkinnslag(
            new ArbeidsgiverTjenesteImpl(null,
                new VirksomhetTjeneste(null, virksomhetRepository))));

    @Before
    public void setUp() {
        VirksomhetEntitet.Builder virksomhetBuilder = new VirksomhetEntitet.Builder();
        virksomhetBuilder.medOrgnr(ORGNR);
        virksomhetBuilder.medNavn(ORGANISASJONEN);
        virksomhetRepository.lagre(virksomhetBuilder.build());
    }

    @Test
    public void skal_lage_historikk_når_inntektskategori_blir_satt() {
        // Arrange
        Inntektskategori inntektskategori = Inntektskategori.ARBEIDSTAKER;
        InntektskategoriEndring inntektskategoriEndring = new InntektskategoriEndring(null, inntektskategori);
        BeregningsgrunnlagPrStatusOgAndelEndring andelEndring = new BeregningsgrunnlagPrStatusOgAndelEndring(
            null,
            inntektskategoriEndring,
            AktivitetStatus.ARBEIDSTAKER,
            OpptjeningAktivitetType.ARBEID,
            Arbeidsgiver.virksomhet(ORGNR),
            InternArbeidsforholdRef.nullRef()
        );
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

        // Act
        inntektskategoriHistorikkTjeneste.lagHistorikkOmEndret(tekstBuilder, List.of(), andelEndring);
        tekstBuilder.ferdigstillHistorikkinnslagDel();

        // Assert
        List<HistorikkinnslagDel> historikkinnslagDeler = tekstBuilder.getHistorikkinnslagDeler();
        assertThat(historikkinnslagDeler.size()).isEqualTo(1);
        assertThat(historikkinnslagDeler.get(0).getHistorikkinnslagFelt().size()).isEqualTo(1);
        HistorikkinnslagFelt historikkinnslagFelt = historikkinnslagDeler.get(0).getHistorikkinnslagFelt().get(0);
        assertThat(historikkinnslagFelt.getTilVerdi()).isEqualTo(inntektskategori.toString());
        assertThat(historikkinnslagFelt.getNavnVerdi()).isEqualTo(ORGANISASJONEN + " (" + ORGNR + ")");
    }

    @Test
    public void skal_lage_historikk_når_inntektskategori_blir_satt_med_forrige() {
        // Arrange
        Inntektskategori forrigeInntektskategori = Inntektskategori.FRILANSER;
        Inntektskategori inntektskategori = Inntektskategori.ARBEIDSTAKER;
        InntektskategoriEndring inntektskategoriEndring = new InntektskategoriEndring(forrigeInntektskategori, inntektskategori);
        BeregningsgrunnlagPrStatusOgAndelEndring andelEndring = new BeregningsgrunnlagPrStatusOgAndelEndring(
            null,
            inntektskategoriEndring,
            AktivitetStatus.ARBEIDSTAKER,
            OpptjeningAktivitetType.ARBEID,
            Arbeidsgiver.virksomhet(ORGNR),
            InternArbeidsforholdRef.nullRef()
        );
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

        // Act
        inntektskategoriHistorikkTjeneste.lagHistorikkOmEndret(tekstBuilder, List.of(), andelEndring);
        tekstBuilder.ferdigstillHistorikkinnslagDel();

        // Assert
        List<HistorikkinnslagDel> historikkinnslagDeler = tekstBuilder.getHistorikkinnslagDeler();
        assertThat(historikkinnslagDeler.size()).isEqualTo(1);
        assertThat(historikkinnslagDeler.get(0).getHistorikkinnslagFelt().size()).isEqualTo(1);
        HistorikkinnslagFelt historikkinnslagFelt = historikkinnslagDeler.get(0).getHistorikkinnslagFelt().get(0);
        assertThat(historikkinnslagFelt.getFraVerdi()).isEqualTo(forrigeInntektskategori.toString());
        assertThat(historikkinnslagFelt.getTilVerdi()).isEqualTo(inntektskategori.toString());
        assertThat(historikkinnslagFelt.getNavnVerdi()).isEqualTo(ORGANISASJONEN + " (" + ORGNR + ")");
    }

    @Test
    public void skal_lage_historikk_for_selvstendig_når_inntektskategori_blir_satt_med_forrige() {
        // Arrange
        Inntektskategori forrigeInntektskategori = Inntektskategori.FRILANSER;
        Inntektskategori inntektskategori = Inntektskategori.ARBEIDSTAKER;
        InntektskategoriEndring inntektskategoriEndring = new InntektskategoriEndring(forrigeInntektskategori, inntektskategori);
        BeregningsgrunnlagPrStatusOgAndelEndring andelEndring = new BeregningsgrunnlagPrStatusOgAndelEndring(
            null,
            inntektskategoriEndring,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE,
            null,
            null,
            InternArbeidsforholdRef.nullRef()
        );
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

        // Act
        inntektskategoriHistorikkTjeneste.lagHistorikkOmEndret(tekstBuilder, List.of(), andelEndring);
        tekstBuilder.ferdigstillHistorikkinnslagDel();

        // Assert
        List<HistorikkinnslagDel> historikkinnslagDeler = tekstBuilder.getHistorikkinnslagDeler();
        assertThat(historikkinnslagDeler.size()).isEqualTo(1);
        assertThat(historikkinnslagDeler.get(0).getHistorikkinnslagFelt().size()).isEqualTo(1);
        HistorikkinnslagFelt historikkinnslagFelt = historikkinnslagDeler.get(0).getHistorikkinnslagFelt().get(0);
        assertThat(historikkinnslagFelt.getFraVerdi()).isEqualTo(forrigeInntektskategori.toString());
        assertThat(historikkinnslagFelt.getTilVerdi()).isEqualTo(inntektskategori.toString());
        assertThat(historikkinnslagFelt.getNavnVerdi()).isEqualTo(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE.getNavn());
    }

}
