package no.nav.k9.sak.domene.opptjening;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.VurderStatusInput;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class OpptjeningAktivitetArbeidVurdererTest {

    @Inject
    private EntityManager entityManager;
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private TestScenarioBuilder scenario;
    private BehandlingRepositoryProvider repositoryProvider;

    private Behandling behandling;
    private final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet("974760673");
    private final InternArbeidsforholdRef arbeidsforholdId = InternArbeidsforholdRef.nyRef();

    private LocalDate skjæringstidspunkt = LocalDate.now();
    private DatoIntervallEntitet inntektsperiodeEttÅr = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusYears(1), skjæringstidspunkt);
    private DatoIntervallEntitet opptjeningPeriode28Dager = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusDays(28), skjæringstidspunkt);

    private OpptjeningAktivitetArbeidVurderer vurderer = new OpptjeningAktivitetArbeidVurderer();

    @BeforeEach
    public void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OMSORGSPENGER);
        behandling = scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_godkjenne_permisjon_inntil_14_dager() {
        var permisjonPeriode14Dager = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusDays(13), skjæringstidspunkt);
        var iayBuilder = opprettIAYMedYrkesaktivitet(behandling);
        leggTilPermisjon(iayBuilder, permisjonPeriode14Dager);
        InntektArbeidYtelseGrunnlag iayGrunnlag = lagreIayGrunnlag(iayBuilder);

        VurderStatusInput input = byggInput(behandling, iayGrunnlag, opptjeningPeriode28Dager);

        assertThat(vurderer.vurderArbeid(input)).isEqualTo(VurderingsStatus.TIL_VURDERING);
    }

    @Test
    public void skal_underkjenne_permisjon_over_14_dager() {
        var permisjonPeriode15Dager = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusDays(14), skjæringstidspunkt);

        var iayBuilder = opprettIAYMedYrkesaktivitet(behandling);
        leggTilPermisjon(iayBuilder, permisjonPeriode15Dager);
        var iayGrunnlag = lagreIayGrunnlag(iayBuilder);

        VurderStatusInput input = byggInput(behandling, iayGrunnlag, opptjeningPeriode28Dager);

        assertThat(vurderer.vurderArbeid(input)).isEqualTo(VurderingsStatus.UNDERKJENT);
    }

    @Test
    public void skal_underkjenne_sammenhengende_permisjoner_som_overstiger_14_dager() {
        var permisjonPeriode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusDays(15), skjæringstidspunkt.minusDays(10));
        var permisjonPeriode2 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusDays(10), skjæringstidspunkt);

        var iayBuilder = opprettIAYMedYrkesaktivitet(behandling);
        leggTilPermisjon(iayBuilder, permisjonPeriode1);
        leggTilPermisjon(iayBuilder, permisjonPeriode2);
        var iayGrunnlag = lagreIayGrunnlag(iayBuilder);

        VurderStatusInput input = byggInput(behandling, iayGrunnlag, opptjeningPeriode28Dager);

        assertThat(vurderer.vurderArbeid(input)).isEqualTo(VurderingsStatus.UNDERKJENT);
    }


    @Test
    public void skal_underkjenne_permisjoner_som_skjøtes_sammen_over_helg_til_14_dager() {
        var førsteLørdagIPeriode = hentFørsteLørdagIPeriode(opptjeningPeriode28Dager.getFomDato(), opptjeningPeriode28Dager.getTomDato());
        var permisjonPeriode1 = DatoIntervallEntitet.fraOgMedTilOgMed(opptjeningPeriode28Dager.getFomDato(), førsteLørdagIPeriode); // 1-7 dager
        var permisjonPeriode2 = DatoIntervallEntitet.fraOgMedTilOgMed(førsteLørdagIPeriode.plusDays(2), førsteLørdagIPeriode.plusDays(15)); // 14 dager

        var iayBuilder = opprettIAYMedYrkesaktivitet(behandling);
        leggTilPermisjon(iayBuilder, permisjonPeriode1);
        leggTilPermisjon(iayBuilder, permisjonPeriode2);
        var iayGrunnlag = lagreIayGrunnlag(iayBuilder);

        VurderStatusInput input = byggInput(behandling, iayGrunnlag, opptjeningPeriode28Dager);

        assertThat(vurderer.vurderArbeid(input)).isEqualTo(VurderingsStatus.UNDERKJENT);
    }

    private LocalDate hentFørsteLørdagIPeriode(LocalDate min, LocalDate max) {
        var neste = min.plusDays(1); // Unngå å starte på samme dag
        while (neste.isBefore(max)) {
            if (DayOfWeek.SATURDAY == neste.getDayOfWeek()) {
                break;
            }
            neste = neste.plusDays(1);
        }
        return neste;
    }

    private void leggTilPermisjon(InntektArbeidYtelseAggregatBuilder iayBuilder, DatoIntervallEntitet permisjonPeriode14Dager) {
        var aktørArbeidBuilder = iayBuilder.getAktørArbeidBuilder(behandling.getAktørId());
        YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
            new Opptjeningsnøkkel(arbeidsforholdId, virksomhet.getIdentifikator(), null), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilder
            .leggTilPermisjon(YrkesaktivitetBuilder.nyPermisjonBuilder()
                .medPeriode(permisjonPeriode14Dager.getFomDato(), permisjonPeriode14Dager.getTomDato())
                .medProsentsats(BigDecimal.valueOf(100))
                .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                .build());
    }

    private InntektArbeidYtelseAggregatBuilder opprettIAYMedYrkesaktivitet(Behandling behandling) {
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
            new Opptjeningsnøkkel(arbeidsforholdId, virksomhet.getIdentifikator(), null), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(inntektsperiodeEttÅr);

        yrkesaktivitetBuilder
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(arbeidsforholdId)
            .leggTilAktivitetsAvtale(aktivitetsAvtaleBuilder);

        builder.leggTilAktørArbeid(
            aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder));
        return builder;
    }

    private InntektArbeidYtelseGrunnlag lagreIayGrunnlag(InntektArbeidYtelseAggregatBuilder iayBuilder) {
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        return iayTjeneste.finnGrunnlag(behandling.getId()).orElseThrow();
    }

    private VurderStatusInput byggInput(Behandling behandling, InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet opptjeningsperiode) {
        var input = new VurderStatusInput(OpptjeningAktivitetType.ARBEID, BehandlingReferanse.fra(behandling));
        input.setOpptjeningsperiode(opptjeningsperiode);
        var yrkesaktiviteter = iayGrunnlag.getAktørArbeidFraRegister(behandling.getAktørId()).get().hentAlleYrkesaktiviteter();
        assertThat(yrkesaktiviteter).as("Forventer testcase med bare én yrkesaktivitet").hasSize(1);

        var yrkesaktivitet = yrkesaktiviteter.iterator().next();
        input.setRegisterAktivitet(yrkesaktivitet);
        return input;
    }
}
