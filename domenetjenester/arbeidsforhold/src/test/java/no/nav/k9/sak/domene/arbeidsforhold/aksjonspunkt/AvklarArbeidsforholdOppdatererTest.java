package no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
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
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforhold;
import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Stillingsprosent;

@CdiDbAwareTest
public class AvklarArbeidsforholdOppdatererTest {

    private static final String NAV_ORGNR = "889640782";

    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_REF = InternArbeidsforholdRef.namedRef("TEST-REF");
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
    private String randomId = UUID.randomUUID().toString();
    private VirksomhetTjeneste virksomhetTjeneste = Mockito.mock(VirksomhetTjeneste.class);
    private EntityManager entityManager;

    @BeforeEach
    public void oppsett() {
        repositoryProvider = new IAYRepositoryProvider(entityManager);
        var arbeidsgiverTjeneste = new ArbeidsgiverTjeneste(tpsTjeneste, virksomhetTjeneste);
        ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste = new ArbeidsforholdAdministrasjonTjeneste(
            vurderArbeidsforholdTjeneste,
            arbeidsgiverTjeneste,
            iayTjeneste);
        var arbeidsgiverHistorikkinnslagTjeneste = new ArbeidsgiverHistorikkinnslag(arbeidsgiverTjeneste);
        ArbeidsforholdHistorikkinnslagTjeneste arbeidsforholdHistorikkinnslagTjeneste = new ArbeidsforholdHistorikkinnslagTjeneste(historikkAdapter, arbeidsgiverHistorikkinnslagTjeneste);
        oppdaterer = new AvklarArbeidsforholdOppdaterer(
            arbeidsforholdAdministrasjonTjeneste,
            iayTjeneste,
            arbeidsforholdHistorikkinnslagTjeneste);
    }

    @Test
    public void skal_kreve_totrinn_hvis_saksbehandler_har_tatt_stilling_til_aksjonspunktet() {

        // Arrange
        var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        opprettIAYAggregat(behandling, false, LocalDate.of(2018, 1, 1));

        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);

        AvklarArbeidsforhold avklarArbeidsforholdDto = new AvklarArbeidsforhold("Har tatt stilling til dette", List.of());
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.of(2019, 1, 1)).build();

        // Act
        OppdateringResultat resultat = oppdaterer.oppdater(avklarArbeidsforholdDto,
            new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, avklarArbeidsforholdDto.getBegrunnelse()));

        // Assert
        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
        Behandling behandling1 = behandlingRepository.hentBehandling(behandling.getId());
        Set<Aksjonspunkt> aksjonspunkter = behandling1.getAksjonspunkter();
        assertThat(aksjonspunkter).hasSize(1);
        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    public void skal_kunne_legge_til_nytt_arbeidsforhold() {
        // Arrange
        var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);

        // simulere at 5080 har oppstått
        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);

        LocalDate stp = LocalDate.of(2019, 1, 1);

        AvklarArbeidsforholdDto nyttArbeidsforhod = new AvklarArbeidsforholdDto();
        String navn = "Utlandet";
        nyttArbeidsforhod.setNavn(navn);
        LocalDate fomDato = stp.minusYears(3);
        nyttArbeidsforhod.setFomDato(fomDato);
        BigDecimal stillingsprosent = BigDecimal.valueOf(100);
        nyttArbeidsforhod.setStillingsprosent(stillingsprosent);
        nyttArbeidsforhod.setLagtTilAvSaksbehandler(true);
        nyttArbeidsforhod.setBrukArbeidsforholdet(true);

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
    }

    @Test
    public void skal_kunne_legge_til_arbeidsforhold_basert_på_inntektsmelding() {
        // Arrange
        var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);

        // simulere at 5080 har oppstått
        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);

        LocalDate stp = LocalDate.of(2019, 1, 1);

        AvklarArbeidsforholdDto nyttArbeidsforhod = new AvklarArbeidsforholdDto();
        String navn = "Utlandet";
        nyttArbeidsforhod.setNavn(navn);
        LocalDate fomDato = stp.minusYears(3);
        nyttArbeidsforhod.setFomDato(fomDato);
        BigDecimal stillingsprosent = BigDecimal.valueOf(100);
        nyttArbeidsforhod.setStillingsprosent(stillingsprosent);
        nyttArbeidsforhod.setLagtTilAvSaksbehandler(false);
        nyttArbeidsforhod.setBasertPaInntektsmelding(true);
        nyttArbeidsforhod.setBrukArbeidsforholdet(true);
        nyttArbeidsforhod.setArbeidsgiverIdentifikator(NAV_ORGNR);

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
    }


    @Test
    public void skal_utlede_handling_lik_lagt_til_av_saksbehandler() {

        // Arrange
        String navn = "Utlandet";
        String arbeidsforholdId = InternArbeidsforholdRef.nyRef().getReferanse();
        LocalDate stpDato = LocalDate.of(2019, 1, 1);
        LocalDate fomDato = stpDato.minusYears(3);
        BigDecimal stillingsprosent = BigDecimal.valueOf(100);

        var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        opprettIAYAggregat(behandling, false, LocalDate.of(2018, 1, 1));

        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);

        AvklarArbeidsforholdDto arbeidsforhold = new AvklarArbeidsforholdDto();
        arbeidsforhold.setNavn(navn);
        arbeidsforhold.setFomDato(fomDato);
        arbeidsforhold.setStillingsprosent(stillingsprosent);
        arbeidsforhold.setLagtTilAvSaksbehandler(true);
        arbeidsforhold.setArbeidsforholdId(arbeidsforholdId);
        arbeidsforhold.setArbeidsgiverIdentifikator(NAV_ORGNR);
        arbeidsforhold.setBrukArbeidsforholdet(true);

        List<AvklarArbeidsforholdDto> nyeArbeidsforhold = List.of(arbeidsforhold);
        AvklarArbeidsforhold avklarArbeidsforholdDto = new AvklarArbeidsforhold("Har lagt til et nytt arbeidsforhold", nyeArbeidsforhold);

        Skjæringstidspunkt stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(stpDato)
            .build();

        AksjonspunktOppdaterParameter params = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, stp, avklarArbeidsforholdDto.getBegrunnelse());

        // Act
        oppdaterer.oppdater(avklarArbeidsforholdDto, params);

        // Assert
        List<ArbeidsforholdOverstyring> overstyring = hentGrunnlag(behandling)
            .getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getOverstyringer)
            .orElse(Collections.emptyList());

        assertThat(overstyring).hasSize(1);
        ArbeidsforholdOverstyring overstyrtArbeidsforhold = overstyring.get(0);
        assertThat(overstyrtArbeidsforhold.getHandling()).isEqualTo(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER);
    }

    private InntektArbeidYtelseGrunnlag hentGrunnlag(Behandling behandling) {
        return iayTjeneste.finnGrunnlag(behandling.getId()).orElseThrow();
    }

    private void opprettIAYAggregat(Behandling behandling, boolean medArbeidsforholdRef, LocalDate fom) {
        LocalDate tom = AbstractLocalDateInterval.TIDENES_ENDE;
        YrkesaktivitetBuilder yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();
        AktivitetsAvtaleBuilder aktivitetsAvtale = aktivitetsAvtaleBuilder
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtaleBuilder ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        yrkesaktivitetBuilder
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(NAV_ORGNR))
            .medArbeidsforholdId(medArbeidsforholdRef ? ARBEIDSFORHOLD_REF : null)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode);
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeid = aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        builder.leggTilAktørArbeid(aktørArbeid);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
