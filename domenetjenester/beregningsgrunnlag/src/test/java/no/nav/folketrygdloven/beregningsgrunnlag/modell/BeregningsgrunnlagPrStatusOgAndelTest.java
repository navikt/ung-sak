package no.nav.folketrygdloven.beregningsgrunnlag.modell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.AktørId;

public class BeregningsgrunnlagPrStatusOgAndelTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private static final String ORGNR = "987";
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final OpptjeningAktivitetType ARBEIDSFORHOLD_TYPE = OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE;
    private final LocalDate PERIODE_FOM = LocalDate.now();

    private BeregningsgrunnlagPeriode beregningsgrunnlagPeriode;
    private BeregningsgrunnlagPrStatusOgAndel prStatusOgAndel;

    @Before
    public void setup() {
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag();
        BeregningsgrunnlagPeriode.Builder builder = lagBeregningsgrunnlagPeriodeBuilder();
        beregningsgrunnlagPeriode= builder.build(beregningsgrunnlag);
        prStatusOgAndel = lagBeregningsgrunnlagPrStatusOgAndel(beregningsgrunnlagPeriode);
    }

    @Test
    public void skal_bygge_instans_med_påkrevde_felter() {
        assertThat(prStatusOgAndel.getBeregningsgrunnlagPeriode()).isEqualTo(beregningsgrunnlagPeriode);
        assertThat(prStatusOgAndel.getAktivitetStatus()).isEqualTo(AktivitetStatus.ARBEIDSTAKER);
        assertThat(prStatusOgAndel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdOrgnr).get()).isEqualTo(ORGNR);
        assertThat(prStatusOgAndel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef).get()).isEqualTo(ARBEIDSFORHOLD_ID);
        assertThat(prStatusOgAndel.getArbeidsforholdType()).isEqualTo(ARBEIDSFORHOLD_TYPE);
    }

    @Test
    public void skal_ikke_bygge_instans_hvis_mangler_påkrevde_felter() {
        BeregningsgrunnlagPrStatusOgAndel.Builder builder = BeregningsgrunnlagPrStatusOgAndel.builder();
        try {
            builder.build(null);
            fail();
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("beregningsgrunnlagPeriode");
        }

        try {
            builder.build(beregningsgrunnlagPeriode);
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("aktivitetStatus");
        }

        try {
            builder.medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER);
            builder.build(beregningsgrunnlagPeriode);
            fail();
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("bgAndelArbeidsforhold");
        }

        try {
            builder.medArbforholdType(OpptjeningAktivitetType.ARBEID);
            builder.build(beregningsgrunnlagPeriode);
            fail();
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("bgAndelArbeidsforhold");
        }

        try {
            builder.medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder());
            fail();
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("arbeidsgiver");
        }

        try {
            builder.medBeregningsperiode(PERIODE_FOM, PERIODE_FOM.plusDays(2));
        } catch (IllegalArgumentException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void skal_håndtere_null_this_feilKlasse_i_equals() {
        assertThat(prStatusOgAndel).isNotEqualTo(null);
        assertThat(prStatusOgAndel).isNotEqualTo("blabla");
        assertThat(prStatusOgAndel).isEqualTo(prStatusOgAndel);
    }

    @Test
    public void skal_ha_refleksiv_equalsOgHashCode() {
        BeregningsgrunnlagPrStatusOgAndel prStatusOgAndel2 = lagBeregningsgrunnlagPrStatusOgAndel(beregningsgrunnlagPeriode);

        assertThat(prStatusOgAndel).isEqualTo(prStatusOgAndel2);
        assertThat(prStatusOgAndel2).isEqualTo(prStatusOgAndel);
        assertThat(prStatusOgAndel.hashCode()).isEqualTo(prStatusOgAndel2.hashCode());
        assertThat(prStatusOgAndel2.hashCode()).isEqualTo(prStatusOgAndel.hashCode());

        BeregningsgrunnlagPrStatusOgAndel.Builder builder = lagBeregningsgrunnlagPrStatusOgAndelBuilder(Arbeidsgiver.virksomhet(ORGNR));
        builder.medAktivitetStatus(AktivitetStatus.FRILANSER);
        prStatusOgAndel2 = builder.build(beregningsgrunnlagPeriode);
        assertThat(prStatusOgAndel).isNotEqualTo(prStatusOgAndel2);
        assertThat(prStatusOgAndel2).isNotEqualTo(prStatusOgAndel);
        assertThat(prStatusOgAndel.hashCode()).isNotEqualTo(prStatusOgAndel2.hashCode());
        assertThat(prStatusOgAndel2.hashCode()).isNotEqualTo(prStatusOgAndel.hashCode());
    }

    @Test
    public void skal_bruke_aktivitetStatus_i_equalsOgHashCode() {
        BeregningsgrunnlagPrStatusOgAndel prStatusOgAndel2 = lagBeregningsgrunnlagPrStatusOgAndel(beregningsgrunnlagPeriode);

        assertThat(prStatusOgAndel).isEqualTo(prStatusOgAndel2);
        assertThat(prStatusOgAndel.hashCode()).isEqualTo(prStatusOgAndel2.hashCode());

        BeregningsgrunnlagPrStatusOgAndel.Builder builder = lagBeregningsgrunnlagPrStatusOgAndelBuilder(Arbeidsgiver.virksomhet(ORGNR));
        builder.medAktivitetStatus(AktivitetStatus.FRILANSER);
        prStatusOgAndel2 = builder.build(beregningsgrunnlagPeriode);

        assertThat(prStatusOgAndel).isNotEqualTo(prStatusOgAndel2);
        assertThat(prStatusOgAndel.hashCode()).isNotEqualTo(prStatusOgAndel2.hashCode());
    }

    @Test
    public void skal_runde_av_og_sette_dagsats_riktig() {
        prStatusOgAndel = BeregningsgrunnlagPrStatusOgAndel.builder(prStatusOgAndel)
            .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(377127.4))
            .medRedusertRefusjonPrÅr(BigDecimal.valueOf(214892.574))
            .build(beregningsgrunnlagPeriode);

        assertThat(prStatusOgAndel.getDagsatsBruker()).isEqualTo(1450);
        assertThat(prStatusOgAndel.getDagsatsArbeidsgiver()).isEqualTo(827);

    }

    @Test
    public void skal_kunne_ha_privatperson_som_arbeidsgiver() {
        AktørId aktørId = AktørId.dummy();
        BeregningsgrunnlagPrStatusOgAndel.Builder builder = lagBeregningsgrunnlagPrStatusOgAndelBuilder(Arbeidsgiver.person(aktørId));
        builder.medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER);
        BeregningsgrunnlagPrStatusOgAndel bgpsa = builder.build(beregningsgrunnlagPeriode);

        assertThat(bgpsa.getBgAndelArbeidsforhold().get().getArbeidsgiver().getIdentifikator()).isEqualTo(aktørId.getId());
    }

    @Test
    public void oppdatering_av_beregnet_skal_ikkje_endre_brutto_om_fordelt_er_satt() {
        prStatusOgAndel = BeregningsgrunnlagPrStatusOgAndel.builder(prStatusOgAndel)
            .medFordeltPrÅr(BigDecimal.valueOf(377127.4))
            .medBeregnetPrÅr(BigDecimal.valueOf(214892.574))
            .build(beregningsgrunnlagPeriode);

        assertThat(prStatusOgAndel.getBruttoPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(377127.4));
    }

    @Test
    public void oppdatering_av_beregnet_skal_ikkje_endre_brutto_om_overstyrt_er_satt() {
        prStatusOgAndel = BeregningsgrunnlagPrStatusOgAndel.builder(prStatusOgAndel)
            .medOverstyrtPrÅr(BigDecimal.valueOf(377127.4))
            .medBeregnetPrÅr(BigDecimal.valueOf(214892.574))
            .build(beregningsgrunnlagPeriode);

        assertThat(prStatusOgAndel.getBruttoPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(377127.4));
    }

    @Test
    public void oppdatering_av_overstyrt_skal_ikkje_endre_brutto_om_fordelt_er_satt() {
        prStatusOgAndel = BeregningsgrunnlagPrStatusOgAndel.builder(prStatusOgAndel)
            .medFordeltPrÅr(BigDecimal.valueOf(377127.4))
            .medOverstyrtPrÅr(BigDecimal.valueOf(214892.574))
            .build(beregningsgrunnlagPeriode);

        assertThat(prStatusOgAndel.getBruttoPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(377127.4));
    }


    private BeregningsgrunnlagPrStatusOgAndel lagBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        return lagBeregningsgrunnlagPrStatusOgAndelBuilder(Arbeidsgiver.virksomhet(ORGNR)).build(beregningsgrunnlagPeriode);
    }

    private BeregningsgrunnlagPrStatusOgAndel.Builder lagBeregningsgrunnlagPrStatusOgAndelBuilder(Arbeidsgiver arbeidsgiver) {
        BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdRef(ARBEIDSFORHOLD_ID)
            .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
            .medArbeidsperiodeTom(LocalDate.now().plusYears(2));

        return BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(bga)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medArbforholdType(ARBEIDSFORHOLD_TYPE);
    }

    private BeregningsgrunnlagPeriode.Builder lagBeregningsgrunnlagPeriodeBuilder() {
        return BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(PERIODE_FOM, null);
    }

    private static BeregningsgrunnlagEntitet lagBeregningsgrunnlag() {
        return lagBeregningsgrunnlagMedSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT);
    }

    private static BeregningsgrunnlagEntitet lagBeregningsgrunnlagMedSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        return BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(skjæringstidspunkt).build();
    }
}
