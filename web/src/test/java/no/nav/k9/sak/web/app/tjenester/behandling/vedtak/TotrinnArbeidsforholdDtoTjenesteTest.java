package no.nav.k9.sak.web.app.tjenester.behandling.vedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.arbeidsforhold.BekreftetPermisjonStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.k9.sak.domene.iay.modell.BekreftetPermisjon;
import no.nav.k9.sak.kontrakt.vedtak.TotrinnsArbeidsforholdDto;
import no.nav.k9.sak.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.k9.sak.test.util.behandling.AbstractTestScenario;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
public class TotrinnArbeidsforholdDtoTjenesteTest {

    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final String ORGNR = "973093681";
    private static final String PRIVATPERSON_NAVN = "Mikke Mus";

    @RegisterExtension
    public static final JpaExtension repoRule = new JpaExtension();

    private final EntityManager entityManager = repoRule.getEntityManager();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdTjeneste;

    private TotrinnArbeidsforholdDtoTjeneste totrinnArbeidsforholdDtoTjeneste;
    private Virksomhet virksomhet = getVirksomheten();
    private Behandling behandling;
    private Totrinnsvurdering vurdering;

    @BeforeEach
    public void setup() {
        ArbeidsgiverTjeneste arbeidsgiverTjeneste = mock(ArbeidsgiverTjeneste.class);
        when(arbeidsgiverTjeneste.hent(Mockito.any())).thenReturn(new ArbeidsgiverOpplysninger(null, PRIVATPERSON_NAVN));
        when(arbeidsgiverTjeneste.hentVirksomhet(Mockito.any())).thenReturn(virksomhet);
        totrinnArbeidsforholdDtoTjeneste = new TotrinnArbeidsforholdDtoTjeneste(iayTjeneste, arbeidsgiverTjeneste);
        var scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = lagre(scenario);
        Totrinnsvurdering.Builder vurderingBuilder = new Totrinnsvurdering.Builder(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);
        vurdering = vurderingBuilder.medGodkjent(true).medBegrunnelse("").build();
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_opprette_arbeidsforholdDto_for_virksomhet_som_arbeidsgiver_med_bekreftet_permisjon_med_status_BRUK_PERMISJON() {
        // Arrange
        BekreftetPermisjon bekreftetPermisjon = new BekreftetPermisjon(LocalDate.now(), LocalDate.now(), BekreftetPermisjonStatus.BRUK_PERMISJON);
        opprettArbeidsforholdInformasjon(Arbeidsgiver.virksomhet(ORGNR), Optional.of(bekreftetPermisjon));
        // Act
        List<TotrinnsArbeidsforholdDto> dtoer = totrinnArbeidsforholdDtoTjeneste.hentArbeidsforhold(behandling, vurdering, Optional.empty());
        // Assert
        assertThat(dtoer).hasSize(1);
        assertThat(dtoer.get(0).getNavn()).isEqualTo("Virksomheten");
        assertThat(dtoer.get(0).getArbeidsgiverIdentifikator()).isEqualTo(ORGNR);
        assertThat(dtoer.get(0).getBrukPermisjon()).isTrue();
    }

    @Test
    public void skal_opprette_arbeidsforholdDto_for_privatperson_som_arbeidsgiver_med_bekreftet_permisjon_med_status_IKKE_BRUK_PERMISJON() {
        // Arrange
        BekreftetPermisjon bekreftetPermisjon = new BekreftetPermisjon(LocalDate.now(), LocalDate.now(), BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON);
        opprettArbeidsforholdInformasjon(Arbeidsgiver.person(AktørId.dummy()), Optional.of(bekreftetPermisjon));
        // Act
        List<TotrinnsArbeidsforholdDto> dtoer = totrinnArbeidsforholdDtoTjeneste.hentArbeidsforhold(behandling, vurdering, Optional.empty());
        // Assert
        assertThat(dtoer).hasSize(1);
        assertThat(dtoer.get(0).getNavn()).isEqualTo("Mikke Mus");
        assertThat(dtoer.get(0).getBrukPermisjon()).isFalse();
    }

    @Test
    public void skal_opprette_arbeidsforholdDto_for_privatperson_som_arbeidsgiver_med_bekreftet_permisjon_med_status_UGYLDIGE_PERIODER() {
        // Arrange
        BekreftetPermisjon bekreftetPermisjon = new BekreftetPermisjon(LocalDate.now(), LocalDate.now(), BekreftetPermisjonStatus.UGYLDIGE_PERIODER);
        opprettArbeidsforholdInformasjon(Arbeidsgiver.person(AktørId.dummy()), Optional.of(bekreftetPermisjon));
        // Act
        List<TotrinnsArbeidsforholdDto> dtoer = totrinnArbeidsforholdDtoTjeneste.hentArbeidsforhold(behandling, vurdering, Optional.empty());
        // Assert
        assertThat(dtoer).hasSize(1);
        assertThat(dtoer.get(0).getNavn()).isEqualTo("Mikke Mus");
        assertThat(dtoer.get(0).getBrukPermisjon()).isFalse();
    }

    @Test
    public void skal_opprette_arbeidsforholdDto_for_privatperson_som_arbeidsgiver_uten_bekreftet_permisjon() {
        // Arrange
        opprettArbeidsforholdInformasjon(Arbeidsgiver.person(AktørId.dummy()), Optional.empty());
        // Act
        List<TotrinnsArbeidsforholdDto> dtoer = totrinnArbeidsforholdDtoTjeneste.hentArbeidsforhold(behandling, vurdering, Optional.empty());
        // Assert
        assertThat(dtoer).hasSize(1);
        assertThat(dtoer.get(0).getNavn()).isEqualTo("Mikke Mus");
        assertThat(dtoer.get(0).getBrukPermisjon()).isNull();
    }

    private void opprettArbeidsforholdInformasjon(Arbeidsgiver arbeidsgiver, Optional<BekreftetPermisjon> bekreftetPermisjon) {
        ArbeidsforholdInformasjonBuilder informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandling.getId());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ARBEIDSFORHOLD_ID);
        overstyringBuilder.medHandling(ArbeidsforholdHandlingType.BRUK);
        bekreftetPermisjon.ifPresent(overstyringBuilder::medBekreftetPermisjon);
        overstyringBuilder.medArbeidsgiver(arbeidsgiver);
        informasjonBuilder.leggTil(overstyringBuilder);
        arbeidsforholdTjeneste.lagre(behandling.getId(), behandling.getAktørId(), informasjonBuilder);
    }

    private Virksomhet getVirksomheten() {
        return new Virksomhet.Builder()
            .medOrgnr(ORGNR)
            .medNavn("Virksomheten")
            .medRegistrert(LocalDate.now().minusYears(2L))
            .medOppstart(LocalDate.now().minusYears(1L))
            .build();
    }
}
