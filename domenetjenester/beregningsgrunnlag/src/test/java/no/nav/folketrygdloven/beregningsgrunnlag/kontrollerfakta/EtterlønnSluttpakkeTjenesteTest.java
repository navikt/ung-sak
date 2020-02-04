package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.EtterlønnSluttpakkeTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.iay.Inntektskategori;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public class EtterlønnSluttpakkeTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, 9, 30);

    public EtterlønnSluttpakkeTjenesteTest() {
    }

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void skalGiTilfelleDersomSøkerHarAndelMedEtterlønnSluttpakke() {
        //Arrange
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.ARBEIDSTAKER, Collections.singletonList(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE));

        //Act
        boolean brukerHarEtterlønnSluttpakke = act(beregningsgrunnlag);

        //Assert
        assertThat(brukerHarEtterlønnSluttpakke).isTrue();
    }

    @Test
    public void skalIkkeGiTilfelleDersomSøkerIkkeHarAndelMedEtterlønnSluttpakke() {
        //Arrange
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.ARBEIDSTAKER, Collections.singletonList(OpptjeningAktivitetType.ARBEID));

        //Act
        boolean brukerHarEtterlønnSluttpakke = act(beregningsgrunnlag);

        //Assert
        assertThat(brukerHarEtterlønnSluttpakke).isFalse();
    }

    @Test
    public void skalIkkeGiTilfelleDersomSøkerIkkeErArbeidstaker() {
        //Arrange
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, Collections.singletonList(OpptjeningAktivitetType.NÆRING));

        //Act
        boolean brukerHarEtterlønnSluttpakke = act(beregningsgrunnlag);

        //Assert
        assertThat(brukerHarEtterlønnSluttpakke).isFalse();
    }

    @Test
    public void skalGiTilfelleDersomSøkerHarAndreAndelerMenOgsåEtterlønnSluttpakke() {
        //Arrange
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.ARBEIDSTAKER, List.of(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE, OpptjeningAktivitetType.VENTELØNN_VARTPENGER));

        //Act
        boolean brukerHarEtterlønnSluttpakke = act(beregningsgrunnlag);

        //Assert
        assertThat(brukerHarEtterlønnSluttpakke).isTrue();
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlag(AktivitetStatus aktivitetStatus, List<OpptjeningAktivitetType> opptjeningAktivitetTypes) {
        BeregningsgrunnlagAktivitetStatus.Builder asb = BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(aktivitetStatus);
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medGrunnbeløp(BigDecimal.valueOf(93000))
            .leggTilAktivitetStatus(asb)
            .build();
        BeregningsgrunnlagPeriode.Builder periodeBuilder = BeregningsgrunnlagPeriode.builder().medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1), null);
        BeregningsgrunnlagPeriode periode = periodeBuilder.build(beregningsgrunnlag);
        for (OpptjeningAktivitetType type : opptjeningAktivitetTypes) {
            BeregningsgrunnlagPrStatusOgAndel.Builder builder = BeregningsgrunnlagPrStatusOgAndel.builder()
                .medAktivitetStatus(aktivitetStatus)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medBeregnetPrÅr(null)
                .medArbforholdType(type);
            if (aktivitetStatus.erArbeidstaker()) {
                builder.medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(Arbeidsgiver.fra(AktørId.dummy())));
            }
            builder
                .build(periode);
        }
        return beregningsgrunnlag;
    }

    private boolean act(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        BeregningsgrunnlagGrunnlagEntitet grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .build(1L, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        return EtterlønnSluttpakkeTjeneste.skalVurdereOmBrukerHarEtterlønnSluttpakke(grunnlag);
    }
}
