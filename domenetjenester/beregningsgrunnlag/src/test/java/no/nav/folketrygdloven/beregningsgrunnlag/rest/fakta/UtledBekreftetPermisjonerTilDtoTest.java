package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.PermisjonDto;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.BekreftetPermisjonStatus;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;

public class UtledBekreftetPermisjonerTilDtoTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Test
    public void skal_returne_empty_hvis_bekreftet_permisjon_ikke_er_present(){

        // Arrange
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        BehandlingReferanse behandlingReferanse = scenario.lagMocked();

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();
        BGAndelArbeidsforhold bgAndelArbeidsforhold = lagBGAndelArbeidsforhold(Optional.of(arbeidsgiver), ref);
        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusDays(1);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.plusDays(1);

        YrkesaktivitetBuilder yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aa = lagAktivitetsAvtaleBuilder(yaBuilder, fom, tom);
        Permisjon permisjon = byggPermisjon(yaBuilder, fom, tom);
        YrkesaktivitetBuilder ya = lagYrkesaktivitetBuilder(yaBuilder, aa, arbeidsgiver, ref, List.of(permisjon));
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = lagAktørArbeidBuilder(behandlingReferanse, List.of(ya));

        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref);
        informasjonBuilder.leggTil(overstyringBuilder);

        InntektArbeidYtelseGrunnlag iayGrunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Optional<PermisjonDto> permisjonDto = UtledBekreftetPermisjonerTilDto.utled(iayGrunnlag, SKJÆRINGSTIDSPUNKT, bgAndelArbeidsforhold);

        // Assert
        assertThat(permisjonDto).isEmpty();

    }

    @Test
    public void skal_returne_empty_hvis_bekreftet_permisjon_slutter_før_stp(){

        // Arrange
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        BehandlingReferanse behandlingReferanse = scenario.lagMocked();

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();
        BGAndelArbeidsforhold bgAndelArbeidsforhold = lagBGAndelArbeidsforhold(Optional.of(arbeidsgiver), ref);
        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusYears(1);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.minusDays(1);

        YrkesaktivitetBuilder yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aa = lagAktivitetsAvtaleBuilder(yaBuilder, fom, tom);
        Permisjon permisjon = byggPermisjon(yaBuilder, fom, tom);
        YrkesaktivitetBuilder ya = lagYrkesaktivitetBuilder(yaBuilder, aa, arbeidsgiver, ref, List.of(permisjon));
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = lagAktørArbeidBuilder(behandlingReferanse, List.of(ya));

        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
            .medBekreftetPermisjon(new BekreftetPermisjon(fom, tom, BekreftetPermisjonStatus.BRUK_PERMISJON));
        informasjonBuilder.leggTil(overstyringBuilder);

        InntektArbeidYtelseGrunnlag iayGrunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Optional<PermisjonDto> permisjonDto = UtledBekreftetPermisjonerTilDto.utled(iayGrunnlag, SKJÆRINGSTIDSPUNKT, bgAndelArbeidsforhold);

        // Assert
        assertThat(permisjonDto).isEmpty();

    }

    @Test
    public void skal_returne_empty_hvis_bekreftet_permisjon_starter_etter_stp(){

        // Arrange
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        BehandlingReferanse behandlingReferanse = scenario.lagMocked();

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();
        BGAndelArbeidsforhold bgAndelArbeidsforhold = lagBGAndelArbeidsforhold(Optional.of(arbeidsgiver), ref);
        LocalDate fom = SKJÆRINGSTIDSPUNKT.plusDays(1);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.plusYears(1);

        YrkesaktivitetBuilder yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aa = lagAktivitetsAvtaleBuilder(yaBuilder, fom, tom);
        Permisjon permisjon = byggPermisjon(yaBuilder, fom, tom);
        YrkesaktivitetBuilder ya = lagYrkesaktivitetBuilder(yaBuilder, aa, arbeidsgiver, ref, List.of(permisjon));
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = lagAktørArbeidBuilder(behandlingReferanse, List.of(ya));

        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
            .medBekreftetPermisjon(new BekreftetPermisjon(fom, tom, BekreftetPermisjonStatus.BRUK_PERMISJON));
        informasjonBuilder.leggTil(overstyringBuilder);

        InntektArbeidYtelseGrunnlag iayGrunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Optional<PermisjonDto> permisjonDto = UtledBekreftetPermisjonerTilDto.utled(iayGrunnlag, SKJÆRINGSTIDSPUNKT, bgAndelArbeidsforhold);

        // Assert
        assertThat(permisjonDto).isEmpty();

    }

    @Test
    public void skal_returne_empty_hvis_bekreftet_permisjon_ikke_har_status_BRUK_PERMISJON(){

        // Arrange
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        BehandlingReferanse behandlingReferanse = scenario.lagMocked();

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();
        BGAndelArbeidsforhold bgAndelArbeidsforhold = lagBGAndelArbeidsforhold(Optional.of(arbeidsgiver), ref);
        LocalDate fom = SKJÆRINGSTIDSPUNKT.plusDays(1);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.plusYears(1);

        YrkesaktivitetBuilder yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aa = lagAktivitetsAvtaleBuilder(yaBuilder, fom, tom);
        Permisjon permisjon = byggPermisjon(yaBuilder, fom, tom);
        YrkesaktivitetBuilder ya = lagYrkesaktivitetBuilder(yaBuilder, aa, arbeidsgiver, ref, List.of(permisjon));
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = lagAktørArbeidBuilder(behandlingReferanse, List.of(ya));

        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
            .medBekreftetPermisjon(new BekreftetPermisjon(fom, tom, BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON));
        informasjonBuilder.leggTil(overstyringBuilder);

        InntektArbeidYtelseGrunnlag iayGrunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Optional<PermisjonDto> permisjonDto = UtledBekreftetPermisjonerTilDto.utled(iayGrunnlag, SKJÆRINGSTIDSPUNKT, bgAndelArbeidsforhold);

        // Assert
        assertThat(permisjonDto).isEmpty();

    }

    @Test
    public void skal_returne_permisjonDto(){

        // Arrange
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        BehandlingReferanse behandlingReferanse = scenario.lagMocked();

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();
        BGAndelArbeidsforhold bgAndelArbeidsforhold = lagBGAndelArbeidsforhold(Optional.of(arbeidsgiver), ref);
        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusDays(1);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.plusDays(1);

        YrkesaktivitetBuilder yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aa = lagAktivitetsAvtaleBuilder(yaBuilder, fom, tom);
        Permisjon permisjon = byggPermisjon(yaBuilder, fom, tom);
        YrkesaktivitetBuilder ya = lagYrkesaktivitetBuilder(yaBuilder, aa, arbeidsgiver, ref, List.of(permisjon));
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = lagAktørArbeidBuilder(behandlingReferanse, List.of(ya));

        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
            .medBekreftetPermisjon(new BekreftetPermisjon(fom, tom, BekreftetPermisjonStatus.BRUK_PERMISJON));
        informasjonBuilder.leggTil(overstyringBuilder);

        InntektArbeidYtelseGrunnlag iayGrunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Optional<PermisjonDto> permisjonDto = UtledBekreftetPermisjonerTilDto.utled(iayGrunnlag, SKJÆRINGSTIDSPUNKT, bgAndelArbeidsforhold);

        // Assert
        assertThat(permisjonDto).hasValueSatisfying(p -> {
            assertThat(p.getPermisjonFom()).isEqualTo(fom);
            assertThat(p.getPermisjonTom()).isEqualTo(tom);
        });

    }

    private InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder lagAktørArbeidBuilder(BehandlingReferanse behandlingReferanse, List<YrkesaktivitetBuilder> yrkesaktiviteter) {
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder
            .oppdatere(Optional.empty()).medAktørId(behandlingReferanse.getAktørId());
        yrkesaktiviteter.forEach(aktørArbeidBuilder::leggTilYrkesaktivitet);
        return aktørArbeidBuilder;
    }

    private InntektArbeidYtelseGrunnlag lagGrunnlag(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder,
                                                    Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjonOpt) {
        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.REGISTER)
            .leggTilAktørArbeid(aktørArbeidBuilder);
        InntektArbeidYtelseGrunnlagBuilder inntektArbeidYtelseGrunnlagBuilder = InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medData(inntektArbeidYtelseAggregatBuilder);
        arbeidsforholdInformasjonOpt.ifPresent(inntektArbeidYtelseGrunnlagBuilder::medInformasjon);
        return inntektArbeidYtelseGrunnlagBuilder.build();
    }

    private YrkesaktivitetBuilder lagYrkesaktivitetBuilder(YrkesaktivitetBuilder yrkesaktivitetBuilder, AktivitetsAvtaleBuilder aktivitetsAvtale,
                                                           Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref, List<Permisjon> permisjoner) {
        yrkesaktivitetBuilder
            .medArbeidsforholdId(ref)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsAvtale);
        permisjoner.forEach(yrkesaktivitetBuilder::leggTilPermisjon);
        return yrkesaktivitetBuilder;
    }

    private Permisjon byggPermisjon(YrkesaktivitetBuilder yrkesaktivitetBuilder, LocalDate fom, LocalDate tom) {
        return yrkesaktivitetBuilder.getPermisjonBuilder()
            .medProsentsats(BigDecimal.valueOf(100))
            .medPeriode(fom, tom)
            .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
            .build();
    }

    private AktivitetsAvtaleBuilder lagAktivitetsAvtaleBuilder(YrkesaktivitetBuilder yrkesaktivitetBuilder, LocalDate fom, LocalDate tom) {
        return yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
    }

    private BGAndelArbeidsforhold lagBGAndelArbeidsforhold(Optional<Arbeidsgiver> arbeidsgiverOpt, InternArbeidsforholdRef ref) {
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medGrunnbeløp(BigDecimal.valueOf(500_000))
            .build();
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(bg);
        BGAndelArbeidsforhold.Builder builder = BGAndelArbeidsforhold.builder()
            .medArbeidsforholdRef(ref);
        arbeidsgiverOpt.ifPresent(builder::medArbeidsgiver);
        BeregningsgrunnlagPrStatusOgAndel andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(builder)
            .build(periode);
        return andel.getBgAndelArbeidsforhold().orElseThrow();
    }

}
