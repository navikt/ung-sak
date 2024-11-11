package no.nav.ung.sak.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandling.Skjæringstidspunkt;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.dokument.arkiv.DokumentArkivTjeneste;
import no.nav.ung.sak.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.ung.sak.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.ung.sak.historikk.HistorikkInnslagKonverter;
import no.nav.ung.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.ung.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.felles.integrasjon.saf.SafTjeneste;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class ArbeidsforholdHistorikkinnslagTjenesteTest {

    @Inject
    private EntityManager entityManager;

    Behandling behandling;
    Aksjonspunkt aksjonspunkt;
    Skjæringstidspunkt skjæringstidspunkt;
    @Mock
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste ;
    private SafTjeneste mockSafTjeneste ;
    private IAYRepositoryProvider provider ;
    private HistorikkRepository historikkRepository ;
    private AksjonspunktTestSupport aksjonspunktTestSupport ;
    private HistorikkInnslagKonverter historikkInnslagKonverter ;
    private DokumentArkivTjeneste dokumentApplikasjonTjeneste ;

    private HistorikkTjenesteAdapter historikkAdapter;
    private ArbeidsforholdHistorikkinnslagTjeneste arbeidsforholdHistorikkinnslagTjeneste;
    private Arbeidsgiver virksomhet ;
    private InternArbeidsforholdRef ref ;

    @BeforeEach
    public void setup() {

        arbeidsgiverHistorikkinnslagTjeneste = Mockito.mock(ArbeidsgiverHistorikkinnslag.class);
        mockSafTjeneste = mock(SafTjeneste.class);
        provider = new IAYRepositoryProvider(entityManager);
        historikkRepository = new HistorikkRepository(entityManager);
        aksjonspunktTestSupport = new AksjonspunktTestSupport();
        historikkInnslagKonverter = new HistorikkInnslagKonverter();
        dokumentApplikasjonTjeneste = new DokumentArkivTjeneste(mockSafTjeneste);
        virksomhet = Arbeidsgiver.virksomhet("1");
        ref = InternArbeidsforholdRef.nyRef();

        historikkAdapter = new HistorikkTjenesteAdapter(historikkRepository, historikkInnslagKonverter, dokumentApplikasjonTjeneste);
        arbeidsforholdHistorikkinnslagTjeneste = new ArbeidsforholdHistorikkinnslagTjeneste(historikkAdapter, arbeidsgiverHistorikkinnslagTjeneste);
        IAYScenarioBuilder scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);
        behandling = scenario.lagre(provider);
        aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);
        skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(LocalDate.now())
            .build();
    }

    @Test
    public void skal_ikke_opprette_noen_historikkinnslag_når_arbeidsforholdet_kun_har_null_verdier() {

        // Arrange
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setHandlingType(ArbeidsforholdHandlingType.BRUK);

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        // Act
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, arbeidsforholdDto.getBegrunnelse()),
            arbeidsforholdDto, virksomhet, ref, List.of());

        // Assert
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(0);

    }
}
