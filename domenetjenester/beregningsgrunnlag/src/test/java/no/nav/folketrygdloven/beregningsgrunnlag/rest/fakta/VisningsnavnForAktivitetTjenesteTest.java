package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public class VisningsnavnForAktivitetTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.MAY, 10);
    public static final String ORGNR = "49382490";
    private static final String VIRKSOMHET_NAVN = "Virksomheten";
    private static final String KUNSTIG_VIRKSOMHET_NAVN = "Kunstig virksomhet";
    private static final VirksomhetEntitet VIRKSOMHETEN = new VirksomhetEntitet.Builder().medOrgnr(ORGNR).medNavn(VIRKSOMHET_NAVN).build();
    private static final VirksomhetEntitet KUNSTIG_VIRKSOMHET = new VirksomhetEntitet.Builder().medOrgnr(OrgNummer.KUNSTIG_ORG)
        .medNavn(KUNSTIG_VIRKSOMHET_NAVN).build();

    private static final String EKSTERN_ARBEIDSFORHOLD_ID = "EKSTERNREF";
    public static final long BEHANDLING_ID = 1234L;

    private VisningsnavnForAktivitetTjeneste visningsnavnForAktivitetTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste = mock(ArbeidsgiverTjeneste.class);
    private BehandlingReferanse ref = mock(BehandlingReferanse.class);
    private BeregningsgrunnlagEntitet beregningsgrunnlag;
    private BeregningsgrunnlagPeriode periode;
    private InntektArbeidYtelseGrunnlag iayGrunnlagMock = mock(InntektArbeidYtelseGrunnlag.class);

    @Before
    public void setUp() {
        when(ref.getBehandlingId()).thenReturn(BEHANDLING_ID);
        visningsnavnForAktivitetTjeneste = new VisningsnavnForAktivitetTjeneste(arbeidsgiverTjeneste);
        beregningsgrunnlag = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING).medGrunnbeløp(BigDecimal.valueOf(600000)).build();
        periode = BeregningsgrunnlagPeriode.builder().medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(beregningsgrunnlag);

        var arbeidsforholdinformasjonMock = mock(ArbeidsforholdInformasjon.class);
        when(arbeidsforholdinformasjonMock.finnEkstern(any(Arbeidsgiver.class), any(InternArbeidsforholdRef.class))).thenReturn(EksternArbeidsforholdRef.ref("EKSTERNREF"));
        when(iayGrunnlagMock.getArbeidsforholdInformasjon()).thenReturn(Optional.of(arbeidsforholdinformasjonMock));
    }

    @Test
    public void skal_lage_navn_for_brukers_andel() {
        // Arrange
        BeregningsgrunnlagPrStatusOgAndel andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medArbforholdType(OpptjeningAktivitetType.UDEFINERT)
            .medAktivitetStatus(AktivitetStatus.BRUKERS_ANDEL)
            .build(periode);

        // Act
        String visningsnavn = visningsnavnForAktivitetTjeneste.lagVisningsnavn(ref, iayGrunnlagMock, andel);

        // Assert
        assertThat(visningsnavn).isEqualTo("Brukers andel");
    }

    @Test
    public void skal_lage_navn_for_arbeid_i_virksomhet_uten_referanse() {
        // Arrange
        when(arbeidsgiverTjeneste.hentVirksomhet(ORGNR)).thenReturn(VIRKSOMHETEN);
        BeregningsgrunnlagPrStatusOgAndel andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(Arbeidsgiver.fra(VIRKSOMHETEN)))
            .medArbforholdType(OpptjeningAktivitetType.ARBEID)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode);

        // Act
        String visningsnavn = visningsnavnForAktivitetTjeneste.lagVisningsnavn(ref, iayGrunnlagMock, andel);

        // Assert
        assertThat(visningsnavn).isEqualTo(VIRKSOMHET_NAVN + " (" + ORGNR + ")");
    }

    @Test
    public void skal_lage_navn_for_arbeid_i_virksomhet_med_ekstern_referanse() {
        // Arrange
        when(arbeidsgiverTjeneste.hentVirksomhet(ORGNR)).thenReturn(VIRKSOMHETEN);
        BeregningsgrunnlagPrStatusOgAndel andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(Arbeidsgiver.fra(VIRKSOMHETEN))
                .medArbeidsforholdRef("123-234-345-456-6556"))
            .medArbforholdType(OpptjeningAktivitetType.ARBEID)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode);

        // Act
        String visningsnavn = visningsnavnForAktivitetTjeneste.lagVisningsnavn(ref, iayGrunnlagMock, andel);

        // Assert
        assertThat(visningsnavn).isEqualTo(VIRKSOMHET_NAVN + " (" + ORGNR + ") ..." + EKSTERN_ARBEIDSFORHOLD_ID.substring(EKSTERN_ARBEIDSFORHOLD_ID.length()-4));
    }


    @Test
    public void skal_lage_navn_for_kunstig_virksomhet() {
        // Arrange
        when(arbeidsgiverTjeneste.hentVirksomhet(OrgNummer.KUNSTIG_ORG)).thenReturn(KUNSTIG_VIRKSOMHET);
        BeregningsgrunnlagPrStatusOgAndel andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(Arbeidsgiver.fra(KUNSTIG_VIRKSOMHET)))
            .medArbforholdType(OpptjeningAktivitetType.ARBEID)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode);

        // Act
        String visningsnavn = visningsnavnForAktivitetTjeneste.lagVisningsnavn(ref, lagKunstigArbeidsforholdMock(), andel);

        // Assert
        assertThat(visningsnavn).isEqualTo(KUNSTIG_VIRKSOMHET_NAVN + " (" + OrgNummer.KUNSTIG_ORG + ")");
    }

    private InntektArbeidYtelseGrunnlag lagKunstigArbeidsforholdMock() {
        InntektArbeidYtelseGrunnlag iayGrunnlag = mock(InntektArbeidYtelseGrunnlag.class);
        ArbeidsforholdOverstyring overstyring = mock(ArbeidsforholdOverstyring.class);
        when(overstyring.getArbeidsgiverNavn()).thenReturn(KUNSTIG_VIRKSOMHET_NAVN);
        when(overstyring.getArbeidsgiver()).thenReturn(Arbeidsgiver.fra(KUNSTIG_VIRKSOMHET));
        when(iayGrunnlag.getArbeidsforholdOverstyringer()).thenReturn(List.of(overstyring));
        return iayGrunnlag;
    }
}
