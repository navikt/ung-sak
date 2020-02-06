package no.nav.foreldrepenger.inngangsvilkaar.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.MedlemskapsvilkårGrunnlag;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class InngangsvilkårOversetterTest {
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private InngangsvilkårOversetter oversetter;

    @Inject
    private BasisPersonopplysningTjeneste personopplysningTjeneste;

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private YrkesaktivitetBuilder yrkesaktivitetBuilder;

    private DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMed(LocalDate.now());

    @Before
    public void oppsett() {
        oversetter = new InngangsvilkårOversetter(repositoryProvider, personopplysningTjeneste,
            iayTjeneste);
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }


    @Test
    public void skal_mappe_fra_domenemedlemskap_til_regelmedlemskap() {
        // Arrange

        LocalDate stp = LocalDate.now();

        var scenario = lagScenario();
        Behandling behandling = lagre(scenario);

        opprettArbeidOgInntektForBehandling(behandling, stp.minusMonths(5), stp.plusMonths(4), true);

        VurdertMedlemskap vurdertMedlemskap = new VurdertMedlemskapBuilder()
            .medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.MEDLEM)
            .medBosattVurdering(true)
            .medLovligOppholdVurdering(true)
            .medOppholdsrettVurdering(true)
            .build();
        MedlemskapRepository medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        medlemskapRepository.lagreMedlemskapVurdering(behandling.getId(), vurdertMedlemskap);

        // Act
        MedlemskapsvilkårGrunnlag grunnlag = oversetter.oversettTilRegelModellMedlemskap(lagRef(behandling, stp), periode);

        // Assert
        assertTrue(grunnlag.isBrukerAvklartBosatt());
        assertTrue(grunnlag.isBrukerAvklartLovligOppholdINorge());
        assertTrue(grunnlag.isBrukerAvklartOppholdsrett());
        assertTrue(grunnlag.isBrukerAvklartPliktigEllerFrivillig());
        assertTrue(grunnlag.isBrukerNorskNordisk());
        assertFalse(grunnlag.isBrukerBorgerAvEUEOS());
        assertTrue(grunnlag.harSøkerArbeidsforholdOgInntekt());
    }

    @Test
    public void skal_mappe_fra_domenemedlemskap_til_regelmedlemskap_med_ingen_relevant_arbeid_og_inntekt() {

        // Arrange
        LocalDate stp = LocalDate.now();
        var scenario = lagScenario();
        Behandling behandling = lagre(scenario);
        opprettArbeidOgInntektForBehandling(behandling, stp.minusMonths(5), stp.minusDays(1), true);

        // Act
        MedlemskapsvilkårGrunnlag grunnlag = oversetter.oversettTilRegelModellMedlemskap(lagRef(behandling, stp), periode);

        // Assert
        assertFalse(grunnlag.harSøkerArbeidsforholdOgInntekt());
    }

    @Test
    public void skal_mappe_fra_domenemedlemskap_til_regelmedlemskap_med_relevant_arbeid_og_ingen_pensjonsgivende_inntekt() {

        // Arrange
        LocalDate stp = LocalDate.now();
        var scenario = lagScenario();
        Behandling behandling = lagre(scenario);
        opprettArbeidOgInntektForBehandling(behandling, stp.minusMonths(5), stp.plusDays(10), false);

        // Act
        MedlemskapsvilkårGrunnlag grunnlag = oversetter.oversettTilRegelModellMedlemskap(lagRef(behandling, stp), periode);

        // Assert
        assertFalse(grunnlag.harSøkerArbeidsforholdOgInntekt());
    }

    private AbstractTestScenario<?> lagScenario() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medDefaultOppgittTilknytning();
        scenario.medSøknad()
            .medMottattDato(LocalDate.of(2017, 3, 15));

        PersonInformasjon søker = scenario.opprettBuilderForRegisteropplysninger()
            .medPersonas()
            .kvinne(scenario.getDefaultBrukerAktørId(), SivilstandType.GIFT, Region.NORDEN)
            .personstatus(PersonstatusType.BOSA)
            .statsborgerskap(Landkoder.NOR)
            .build();
        scenario.medRegisterOpplysninger(søker);
        return scenario;
    }

    private void opprettArbeidOgInntektForBehandling(Behandling behandling, LocalDate fom, LocalDate tom,
                                                                                   boolean harPensjonsgivendeInntekt) {

        String orgnr = "42";

        var aggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        AktørId aktørId = behandling.getAktørId();
        lagAktørArbeid(aggregatBuilder, aktørId, orgnr, fom, tom, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, Optional.empty());
        for (LocalDate dt = fom; dt.isBefore(tom); dt = dt.plusMonths(1)) {
            lagInntekt(aggregatBuilder, aktørId, orgnr, dt, dt.plusMonths(1), harPensjonsgivendeInntekt);
        }

        iayTjeneste.lagreIayAggregat(behandling.getId(), aggregatBuilder);
    }

    private AktørArbeid lagAktørArbeid(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId, String virksomhetOrgnr,
                                       LocalDate fom, LocalDate tom, ArbeidType arbeidType, Optional<InternArbeidsforholdRef> arbeidsforholdRef) {
        var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder
            .getAktørArbeidBuilder(aktørId);

        Opptjeningsnøkkel opptjeningsnøkkel;
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(virksomhetOrgnr);
        if (arbeidsforholdRef.isPresent()) {
            opptjeningsnøkkel = new Opptjeningsnøkkel(arbeidsforholdRef.get(), arbeidsgiver.getIdentifikator(), null);
        } else {
            opptjeningsnøkkel = Opptjeningsnøkkel.forOrgnummer(virksomhetOrgnr);
        }

        yrkesaktivitetBuilder = aktørArbeidBuilder
            .getYrkesaktivitetBuilderForNøkkelAvType(opptjeningsnøkkel, arbeidType);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        var aktivitetsAvtale = aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));

        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtale)
            .medArbeidType(arbeidType)
            .medArbeidsgiver(arbeidsgiver);

        yrkesaktivitetBuilder.medArbeidsforholdId(arbeidsforholdRef.orElse(null));

        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
        return aktørArbeidBuilder.build();
    }

    private void lagInntekt(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId, String virksomhetOrgnr,
                            LocalDate fom, LocalDate tom, boolean harPensjonsgivendeInntekt) {
        var opptjeningsnøkkel = Opptjeningsnøkkel.forOrgnummer(virksomhetOrgnr);

        var aktørInntektBuilder = inntektArbeidYtelseAggregatBuilder.getAktørInntektBuilder(aktørId);

        Stream<InntektsKilde> inntektsKildeStream;
        if (harPensjonsgivendeInntekt) {
            inntektsKildeStream = Stream.of(InntektsKilde.INNTEKT_BEREGNING, InntektsKilde.INNTEKT_SAMMENLIGNING, InntektsKilde.INNTEKT_OPPTJENING);
        } else {
            inntektsKildeStream = Stream.of(InntektsKilde.INNTEKT_BEREGNING, InntektsKilde.INNTEKT_SAMMENLIGNING);
        }

        inntektsKildeStream.forEach(kilde -> {
            InntektBuilder inntektBuilder = aktørInntektBuilder.getInntektBuilder(kilde, opptjeningsnøkkel);
            InntektspostBuilder inntektspost = InntektspostBuilder.ny()
                .medBeløp(BigDecimal.valueOf(35000))
                .medPeriode(fom, tom)
                .medInntektspostType(InntektspostType.LØNN);
            inntektBuilder.leggTilInntektspost(inntektspost).medArbeidsgiver(yrkesaktivitetBuilder.build().getArbeidsgiver());
            aktørInntektBuilder.leggTilInntekt(inntektBuilder);
            inntektArbeidYtelseAggregatBuilder.leggTilAktørInntekt(aktørInntektBuilder);
        });
    }

    private BehandlingReferanse lagRef(Behandling behandling, LocalDate stp) {
        return BehandlingReferanse.fra(behandling, stp);
    }

}
