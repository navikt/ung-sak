package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;

import no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittUtenlandskVirksomhet;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderOppgittOpptjening;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.test.util.behandling.AbstractTestScenario;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class AksjonspunktutlederForVurderOppgittOpptjeningTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider ;
    private Skjæringstidspunkt skjæringstidspunkt ;
    private InntektArbeidYtelseTjeneste iayTjeneste ;
    private VirksomhetTjeneste virksomhetTjeneste ;
    private OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider;
    private OppgittOpptjeningFilter oppgittOpptjeningFilter;

    @Spy
    private AksjonspunktutlederForVurderOppgittOpptjening utleder ;

    private OpptjeningRepository opptjeningRepository;

    @BeforeEach
    public void oppsett() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        virksomhetTjeneste = Mockito.mock(VirksomhetTjeneste.class);
        oppgittOpptjeningFilterProvider = Mockito.mock(OppgittOpptjeningFilterProvider.class);
        oppgittOpptjeningFilter = Mockito.mock(OppgittOpptjeningFilter.class);
        when(oppgittOpptjeningFilterProvider.finnOpptjeningFilter(anyLong())).thenReturn(oppgittOpptjeningFilter);
        utleder = new AksjonspunktutlederForVurderOppgittOpptjening(
            repositoryProvider.getOpptjeningRepository(), iayTjeneste, virksomhetTjeneste, oppgittOpptjeningFilterProvider);

        initMocks(this);
        opptjeningRepository = repositoryProvider.getOpptjeningRepository();
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunktet_5051() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        Behandling behandling = lagre(scenario);
        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private AksjonspunktUtlederInput lagRef(Behandling behandling) {
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, skjæringstidspunkt));
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_bruker_har_hatt_vartpenger() {
        // Arrange
        Behandling behandling = opprettBehandling(ArbeidType.VENTELØNN_VARTPENGER);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_bruker_har_oppgitt_frilansperiode() {
        // Arrange
        Behandling behandling = opprettBehandling(ArbeidType.FRILANSER);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_bruker_har_hatt_ventelønn() {
        // Arrange
        Behandling behandling = opprettBehandling(ArbeidType.VENTELØNN_VARTPENGER);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_bruker_har_hatt_militær_eller_siviltjeneste() {
        // Arrange
        Behandling behandling = opprettBehandling(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_bruker_har_hatt_etterlønn_sluttvederlag() {
        // Arrange
        Behandling behandling = opprettBehandling(ArbeidType.ETTERLØNN_SLUTTPAKKE);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_bruker_har_hatt_videre_og_etterutdanning() {
        // Arrange
        Behandling behandling = opprettBehandling(ArbeidType.LØNN_UNDER_UTDANNING);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_bruker_er_selvstendig_næringsdrivende_og_ikke_hatt_næringsinntekt_eller_registrert_næringen_senere() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Behandling behandling = opprettOppgittOpptjening(aktørId, false);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_bruker_har_utenlandsforhold() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Behandling behandling = opprettUtenlandskArbeidsforhold(aktørId);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_bruker_er_selvstendig_næringsdrivende_og_ikke_hatt_næringsinntekt_og_registrert_næringen_senere() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();

        LocalDate fraOgMed = LocalDate.now().minusMonths(1);
        LocalDate tilOgMed = LocalDate.now().plusMonths(1);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        String orgnr = "974760673";

        Behandling behandling = lagre(scenario);

        OppgittOpptjeningBuilder.EgenNæringBuilder egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();
        OppgittUtenlandskVirksomhet svenska_stat = new OppgittUtenlandskVirksomhet(Landkoder.SWE, "Svenska Stat");
        egenNæringBuilder
            .medPeriode(periode)
            .medUtenlandskVirksomhet(svenska_stat)
            .medBegrunnelse("Vet ikke")
            .medBruttoInntekt(BigDecimal.valueOf(100000))
            .medRegnskapsførerNavn("Jacob")
            .medRegnskapsførerTlf("+46678456345")
            .medVirksomhetType(VirksomhetType.FISKE)
            .medVirksomhet(orgnr);
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        oppgittOpptjeningBuilder
            .leggTilEgneNæringer(Collections.singletonList(egenNæringBuilder));

        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_bruker_er_selvstendig_næringsdrivende_og_hatt_næringsinntekt() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Behandling behandling = opprettOppgittOpptjening(aktørId, true);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private Behandling opprettUtenlandskArbeidsforhold(AktørId aktørId) {
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId);
        Behandling behandling = lagre(scenario);

        LocalDate fraOgMed = LocalDate.now().minusMonths(1);
        LocalDate tilOgMed = LocalDate.now().plusMonths(1);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);
        OppgittUtenlandskVirksomhet svenska_stat = new OppgittUtenlandskVirksomhet(Landkoder.SWE, "Svenska Stat");
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        oppgittOpptjeningBuilder
            .leggTilOppgittArbeidsforhold(OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny().medUtenlandskVirksomhet(svenska_stat).medPeriode(periode)
                .medErUtenlandskInntekt(true).medArbeidType(ArbeidType.UTENLANDSK_ARBEIDSFORHOLD));

        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);

        lagreOpptjeningsPeriode(behandling, tilOgMed);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        when(oppgittOpptjeningFilter.hentOppgittOpptjening(any(), any(), any(LocalDate.class))).thenReturn(iayGrunnlag.getOppgittOpptjening());

        return behandling;
    }

    private Behandling opprettOppgittOpptjening(AktørId aktørId, boolean medNæring) {
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId);

        LocalDate fraOgMed = LocalDate.now().minusMonths(1);
        LocalDate tilOgMed = LocalDate.now().plusMonths(1);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        OppgittOpptjeningBuilder.EgenNæringBuilder egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();
        OppgittUtenlandskVirksomhet svenska_stat = new OppgittUtenlandskVirksomhet(Landkoder.SWE, "Svenska Stat");
        egenNæringBuilder
            .medPeriode(periode)
            .medUtenlandskVirksomhet(svenska_stat)
            .medBegrunnelse("Vet ikke")
            .medBruttoInntekt(BigDecimal.valueOf(100000))
            .medRegnskapsførerNavn("Jacob")
            .medRegnskapsførerTlf("+46678456345")
            .medVirksomhetType(VirksomhetType.FISKE);
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        oppgittOpptjeningBuilder
            .leggTilEgneNæringer(Collections.singletonList(egenNæringBuilder));

        Behandling behandling = lagre(scenario);
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);

        if (medNæring) {
            var iayAggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
            var aktørInntektBuilder = iayAggregatBuilder.getAktørInntektBuilder(aktørId);
            var inntektBuilder = aktørInntektBuilder
                .getInntektBuilder(InntektsKilde.SIGRUN, new Opptjeningsnøkkel(null, null, aktørId.getId()));
            var inntektspost = inntektBuilder.getInntektspostBuilder()
                .medBeløp(BigDecimal.TEN)
                .medPeriode(LocalDate.now().minusYears(2L), LocalDate.now().minusYears(1L))
                .medInntektspostType(InntektspostType.SELVSTENDIG_NÆRINGSDRIVENDE);

            inntektBuilder.leggTilInntektspost(inntektspost);
            aktørInntektBuilder.leggTilInntekt(inntektBuilder);
            iayAggregatBuilder.leggTilAktørInntekt(aktørInntektBuilder);

            iayTjeneste.lagreIayAggregat(behandling.getId(), iayAggregatBuilder);
        }


        lagreOpptjeningsPeriode(behandling, tilOgMed);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        when(oppgittOpptjeningFilter.hentOppgittOpptjening(any(), any(), any(LocalDate.class))).thenReturn(iayGrunnlag.getOppgittOpptjening());

        return behandling;
    }

    private Behandling opprettBehandling(ArbeidType annenOpptjeningType) {
        var scenario = TestScenarioBuilder.builderMedSøknad();

        LocalDate fraOgMed = LocalDate.now().minusMonths(1);
        LocalDate tilOgMed = LocalDate.now().plusMonths(1);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        oppgittOpptjeningBuilder
            .leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode, annenOpptjeningType));

        Behandling behandling = lagre(scenario);

        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);

        lagreOpptjeningsPeriode(behandling, tilOgMed);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        when(oppgittOpptjeningFilter.hentOppgittOpptjening(any(), any(), any(LocalDate.class))).thenReturn(iayGrunnlag.getOppgittOpptjening());

        return behandling;
    }

    private void lagreOpptjeningsPeriode(Behandling behandling, LocalDate opptjeningTom) {
        opptjeningRepository.lagreOpptjeningsperiode(behandling, opptjeningTom.minusMonths(10), opptjeningTom, false);
    }
}
