package no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.historikk.VurderArbeidsforholdHistorikkinnslag;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.dokument.arkiv.DokumentArkivTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.k9.sak.historikk.HistorikkInnslagKonverter;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.integrasjon.saf.SafTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)

public class ArbeidsforholdHistorikkinnslagTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

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
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.minusDays(1));

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        // Act
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, arbeidsforholdDto.getBegrunnelse()), arbeidsforholdDto, virksomhet, ref, List.of());

        // Assert
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(0);

    }

    @Test
    public void skal_opprette_kun_et_historikkinnslag_når_arbeidsforholdet_skal_kun_bruke_permisjon() {

        // Arrange
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(true);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.minusDays(1));

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        // Act
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, arbeidsforholdDto.getBegrunnelse()),
            arbeidsforholdDto, virksomhet, ref, List.of());

        // Assert
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(1);
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).anySatisfy(del -> {
            assertThat(del.getHistorikkinnslagFelt()).hasSize(3);
            assertThat(del.getHistorikkinnslagFelt()).anySatisfy(felt ->
                assertThat(felt.getTilVerdi()).isEqualTo(VurderArbeidsforholdHistorikkinnslag.SØKER_ER_I_PERMISJON.getKode())
            );
        });

    }

    @Test
    public void skal_opprette_kun_et_historikkinnslag_når_arbeidsforholdet_skal_forsette_uten_inntektsmelding() {

        // Arrange
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setFortsettBehandlingUtenInntektsmelding(true);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.minusDays(1));

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        // Act
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, arbeidsforholdDto.getBegrunnelse()),
            arbeidsforholdDto, virksomhet, ref, List.of());

        // Assert
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(1);
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).anySatisfy(del -> {
            assertThat(del.getHistorikkinnslagFelt()).hasSize(3);
            assertThat(del.getHistorikkinnslagFelt()).anySatisfy(felt ->
                assertThat(felt.getTilVerdi()).isEqualTo(VurderArbeidsforholdHistorikkinnslag.BENYTT_A_INNTEKT_I_BG.getKode())
            );
        });

    }

    @Test
    public void skal_opprette_to_historikkinnslag_når_arbeidsforholdet_skal_forsette_uten_inntektsmelding_og_ikke_bruke_permisjon() {

        // Arrange
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(false);
        arbeidsforholdDto.setFortsettBehandlingUtenInntektsmelding(true);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.minusDays(1));

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        // Act
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, arbeidsforholdDto.getBegrunnelse()),
            arbeidsforholdDto, virksomhet, ref, List.of());

        // Assert
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(2);
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).anySatisfy(del -> {
            assertThat(del.getHistorikkinnslagFelt()).hasSize(3);
            assertThat(del.getHistorikkinnslagFelt()).anySatisfy(felt ->
                assertThat(felt.getTilVerdi()).isEqualTo(VurderArbeidsforholdHistorikkinnslag.SØKER_ER_IKKE_I_PERMISJON.getKode())
            );
        });
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).anySatisfy(del -> {
            assertThat(del.getHistorikkinnslagFelt()).hasSize(2);
            assertThat(del.getHistorikkinnslagFelt()).anySatisfy(felt ->
                assertThat(felt.getTilVerdi()).isEqualTo(VurderArbeidsforholdHistorikkinnslag.BENYTT_A_INNTEKT_I_BG.getKode())
            );
        });

    }

    @Test
    public void skal_opprette_et_historikkinnslag_når_arbeidsforholdet_ikke_skal_brukes() {

        // Arrange
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(false);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.minusDays(1));

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        // Act
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, arbeidsforholdDto.getBegrunnelse()),
            arbeidsforholdDto, virksomhet, ref, List.of());

        // Assert
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(1);
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).anySatisfy(del -> {
            assertThat(del.getHistorikkinnslagFelt()).hasSize(3);
            assertThat(del.getHistorikkinnslagFelt()).anySatisfy(felt ->
                assertThat(felt.getTilVerdi()).isEqualTo(VurderArbeidsforholdHistorikkinnslag.IKKE_BRUK.getKode())
            );
        });

    }

    @Test
    public void skal_ikke_opprette_noen_historikkinnslag_når_arbeidsforholdet_starter_på_stp() {

        // Arrange
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT);

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        // Act
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, arbeidsforholdDto.getBegrunnelse()),
            arbeidsforholdDto, virksomhet, ref, List.of());

        // Assert
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(0);

    }

    @Test
    public void skal_ikke_opprette_noen_historikkinnslag_når_arbeidsforholdet_starter_etter_stp() {

        // Arrange
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.plusDays(1));

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        // Act
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, arbeidsforholdDto.getBegrunnelse()),
            arbeidsforholdDto, virksomhet, ref, List.of());

        // Assert
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(0);

    }

}
