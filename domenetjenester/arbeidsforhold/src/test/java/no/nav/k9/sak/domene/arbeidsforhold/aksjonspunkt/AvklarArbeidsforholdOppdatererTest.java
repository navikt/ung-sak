package no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.arbeidsforhold.SkatteOgAvgiftsregelType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtale;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsforholdIdDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforhold;
import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.PeriodeDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Stillingsprosent;
import no.nav.vedtak.konfig.Tid;

@CdiDbAwareTest
public class AvklarArbeidsforholdOppdatererTest {

    private IAYRepositoryProvider repositoryProvider;

    @Inject
    private HistorikkTjenesteAdapter historikkAdapter;
    @Inject
    private PersonIdentTjeneste tpsTjeneste;
    @Inject
    private VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste;

    private AksjonspunktTestSupport aksjonspunktTestSupport = new AksjonspunktTestSupport();
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private AvklarArbeidsforholdOppdaterer oppdaterer;
    private VirksomhetTjeneste virksomhetTjeneste = Mockito.mock(VirksomhetTjeneste.class);
    private EntityManager entityManager;

    @BeforeEach
    public void oppsett() {
        repositoryProvider = new IAYRepositoryProvider(entityManager);
        var arbeidsgiverTjeneste = new ArbeidsgiverTjeneste(tpsTjeneste, virksomhetTjeneste);
        var arbeidsforholdAdministrasjonTjeneste = new ArbeidsforholdAdministrasjonTjeneste(
            vurderArbeidsforholdTjeneste,
            arbeidsgiverTjeneste,
            iayTjeneste);
        var arbeidsgiverHistorikkinnslagTjeneste = new ArbeidsgiverHistorikkinnslag(arbeidsgiverTjeneste);
        var arbeidsforholdHistorikkinnslagTjeneste = new ArbeidsforholdHistorikkinnslagTjeneste(historikkAdapter, arbeidsgiverHistorikkinnslagTjeneste);
        oppdaterer = new AvklarArbeidsforholdOppdaterer(
            arbeidsforholdAdministrasjonTjeneste,
            iayTjeneste,
            arbeidsforholdHistorikkinnslagTjeneste);
    }

    @Test
    public void skal_kunne_legge_til_nytt_arbeidsforhold() {
        // Arrange
        var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        var arbeidsgiver = Arbeidsgiver.virksomhet("000000000");
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        opprettIAYMedInntektsmeldingOgInntekt(behandling, arbeidsgiver, arbeidsforholdId);
        // simulere at 5080 har oppstått
        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);

        LocalDate stp = LocalDate.of(2019, 1, 1);

        AvklarArbeidsforholdDto nyttArbeidsforhod = new AvklarArbeidsforholdDto();
        String navn = "Utlandet";
        LocalDate fomDato = stp.minusYears(3);
        BigDecimal stillingsprosent = BigDecimal.valueOf(100);
        nyttArbeidsforhod.setStillingsprosent(stillingsprosent);
        nyttArbeidsforhod.setNavn(navn);
        nyttArbeidsforhod.setArbeidsforhold(new ArbeidsforholdIdDto(arbeidsforholdId.getUUIDReferanse(), "1234"));
        nyttArbeidsforhod.setId(arbeidsgiver.getIdentifikator() + "-" + arbeidsforholdId.getUUIDReferanse()); // identifikator + "-" + arbeidsforholdsIdIntern
        nyttArbeidsforhod.setStillingsprosent(stillingsprosent);
        nyttArbeidsforhod.setAnsettelsesPerioder(Set.of(new PeriodeDto(fomDato, Tid.TIDENES_ENDE)));
        nyttArbeidsforhod.setStillingsprosent(stillingsprosent);
        nyttArbeidsforhod.setArbeidsgiver(arbeidsgiver);
        nyttArbeidsforhod.setNavn(navn);
        nyttArbeidsforhod.setHandlingType(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING);
        nyttArbeidsforhod.setHandlingType(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER);

        List<AvklarArbeidsforholdDto> nyeArbeidsforhold = List.of(nyttArbeidsforhod);
        AvklarArbeidsforhold avklarArbeidsforholdDto = new AvklarArbeidsforhold("Har lagt til et nytt arbeidsforhold", nyeArbeidsforhold);

        // Act
        oppdaterer.oppdater(avklarArbeidsforholdDto,
            new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build(), avklarArbeidsforholdDto.getBegrunnelse()));

        // Assert
        List<ArbeidsforholdOverstyring> overstyring = hentGrunnlag(behandling).getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getOverstyringer).orElse(Collections.emptyList());

        assertThat(overstyring).hasSize(1);
        ArbeidsforholdOverstyring overstyrtArbeidsforhold = overstyring.get(0);
        assertThat(overstyrtArbeidsforhold.getStillingsprosent()).isEqualTo(new Stillingsprosent(stillingsprosent));
        assertThat(overstyrtArbeidsforhold.getArbeidsgiverNavn()).isEqualTo(navn);
        assertThat(overstyrtArbeidsforhold.getArbeidsforholdOverstyrtePerioder()).hasSize(1);
        assertThat(overstyrtArbeidsforhold.getArbeidsforholdOverstyrtePerioder().get(0).getOverstyrtePeriode()).isEqualByComparingTo(DatoIntervallEntitet.fraOgMed(fomDato));

        AktørId aktørId = behandling.getAktørId();

        // Henter opp yrkesaktivitet med overstyring
        InntektArbeidYtelseGrunnlag grunnlag = hentGrunnlag(behandling);
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId)).før(stp);
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getYrkesaktiviteter();
        Yrkesaktivitet yrkesaktivitet = yrkesaktiviteter.iterator().next();
        List<AktivitetsAvtale> ansettelsesPerioder = filter.getAnsettelsesPerioder(yrkesaktivitet);
        assertThat(ansettelsesPerioder).hasSize(1);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getFomDato()).isEqualTo(fomDato);
        Collection<AktivitetsAvtale> aktivitetsAvtaler = filter.getAktivitetsAvtalerForArbeid();
        assertThat(aktivitetsAvtaler).hasSize(1);
        assertThat(aktivitetsAvtaler.iterator().next().getProsentsats().getVerdi()).isEqualByComparingTo(stillingsprosent);

        var ref = BehandlingReferanse.fra(behandling, stp);
        var vurder = vurderArbeidsforholdTjeneste.vurderMedÅrsak(ref, grunnlag);

        assertThat(vurder).isEmpty();
    }

    @Test
    public void skal_kunne_legge_til_arbeidsforhold_basert_på_inntektsmelding() {
        // Arrange
        var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        var arbeidsgiver = Arbeidsgiver.virksomhet("000000000");
        var arbeidsforholdId = InternArbeidsforholdRef.nullRef();
        opprettIAYMedInntektsmeldingOgInntekt(behandling, arbeidsgiver, arbeidsforholdId);
        // simulere at 5080 har oppstått
        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);

        LocalDate stp = LocalDate.of(2019, 1, 1);

        AvklarArbeidsforholdDto nyttArbeidsforhod = new AvklarArbeidsforholdDto();
        String navn = "Utlandet";
        nyttArbeidsforhod.setNavn(navn);
        LocalDate fomDato = stp.minusYears(3);
        BigDecimal stillingsprosent = BigDecimal.valueOf(100);
        nyttArbeidsforhod.setArbeidsforhold(new ArbeidsforholdIdDto(null, null));
        nyttArbeidsforhod.setId(arbeidsgiver.getIdentifikator() + "-" + arbeidsforholdId.getUUIDReferanse()); // identifikator + "-" + arbeidsforholdsIdIntern
        nyttArbeidsforhod.setStillingsprosent(stillingsprosent);
        nyttArbeidsforhod.setAnsettelsesPerioder(Set.of(new PeriodeDto(fomDato, Tid.TIDENES_ENDE)));
        nyttArbeidsforhod.setStillingsprosent(stillingsprosent);
        nyttArbeidsforhod.setArbeidsgiver(arbeidsgiver);
        nyttArbeidsforhod.setNavn(navn);
        nyttArbeidsforhod.setHandlingType(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING);

        List<AvklarArbeidsforholdDto> nyeArbeidsforhold = List.of(nyttArbeidsforhod);
        AvklarArbeidsforhold avklarArbeidsforholdDto = new AvklarArbeidsforhold("Har lagt til et nytt arbeidsforhold", nyeArbeidsforhold);

        // Act
        oppdaterer.oppdater(avklarArbeidsforholdDto,
            new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build(), avklarArbeidsforholdDto.getBegrunnelse()));

        // Assert
        List<ArbeidsforholdOverstyring> overstyring = hentGrunnlag(behandling).getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getOverstyringer).orElse(Collections.emptyList());

        assertThat(overstyring).hasSize(1);
        ArbeidsforholdOverstyring overstyrtArbeidsforhold = overstyring.get(0);
        assertThat(overstyrtArbeidsforhold.getStillingsprosent()).isEqualTo(new Stillingsprosent(stillingsprosent));
        assertThat(overstyrtArbeidsforhold.getArbeidsgiverNavn()).isEqualTo(navn);
        assertThat(overstyrtArbeidsforhold.getArbeidsforholdOverstyrtePerioder()).hasSize(1);
        assertThat(overstyrtArbeidsforhold.getArbeidsforholdOverstyrtePerioder().get(0).getOverstyrtePeriode()).isEqualByComparingTo(DatoIntervallEntitet.fraOgMed(fomDato));

        AktørId aktørId = behandling.getAktørId();

        // Henter opp yrkesaktivitet med overstyring
        InntektArbeidYtelseGrunnlag grunnlag = hentGrunnlag(behandling);
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId)).før(stp);
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getYrkesaktiviteter();
        Yrkesaktivitet yrkesaktivitet = yrkesaktiviteter.iterator().next();
        List<AktivitetsAvtale> ansettelsesPerioder = filter.getAnsettelsesPerioder(yrkesaktivitet);
        assertThat(ansettelsesPerioder).hasSize(1);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getFomDato()).isEqualTo(fomDato);
        Collection<AktivitetsAvtale> aktivitetsAvtaler = filter.getAktivitetsAvtalerForArbeid();
        assertThat(aktivitetsAvtaler).hasSize(1);
        assertThat(aktivitetsAvtaler.iterator().next().getProsentsats().getVerdi()).isEqualByComparingTo(stillingsprosent);

        var ref = BehandlingReferanse.fra(behandling, stp);
        var vurder = vurderArbeidsforholdTjeneste.vurderMedÅrsak(ref, grunnlag);

        // Sjekker om aksjonspunktet er løst
        assertThat(vurder).isEmpty();
    }

    private InntektArbeidYtelseGrunnlag hentGrunnlag(Behandling behandling) {
        return iayTjeneste.finnGrunnlag(behandling.getId()).orElseThrow();
    }

    private void opprettIAYMedInntektsmeldingOgInntekt(Behandling behandling, Arbeidsgiver virksomhet, InternArbeidsforholdRef arbeidsforholdId) {
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aiBuilder = builder.getAktørInntektBuilder(behandling.getAktørId());
        var ibuilder = aiBuilder.getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING, Opptjeningsnøkkel.forArbeidsgiver(virksomhet));
        ibuilder.leggTilInntektspost(ibuilder.getInntektspostBuilder()
            .medInntektspostType(InntektspostType.LØNN)
            .medPeriode(LocalDate.now().minusMonths(3), LocalDate.now())
            .medBeløp(BigDecimal.TEN)
            .medSkatteOgAvgiftsregelType(SkatteOgAvgiftsregelType.NETTOLØNN));
        var aktørInntekt = aiBuilder.leggTilInntekt(ibuilder);
        builder.leggTilAktørInntekt(aktørInntekt);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        var inntektsmeldinger = List.of(InntektsmeldingBuilder.builder()
            .medArbeidsgiver(virksomhet)
            .medYtelse(FagsakYtelseType.OMSORGSPENGER)
            .medJournalpostId("1")
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref(arbeidsforholdId.getReferanse()))
            .medKanalreferanse("AR1")
            .medBeløp(BigDecimal.TEN));
        iayTjeneste.lagreInntektsmeldinger(behandling.getFagsak().getSaksnummer(), behandling.getId(), inntektsmeldinger);
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
