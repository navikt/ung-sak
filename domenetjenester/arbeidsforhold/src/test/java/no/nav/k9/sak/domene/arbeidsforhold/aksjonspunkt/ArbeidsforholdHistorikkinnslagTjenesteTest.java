package no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.dokument.arkiv.DokumentArkivTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.k9.sak.historikk.HistorikkInnslagKonverter;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsforholdIdDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.PeriodeDto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.integrasjon.saf.SafTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.konfig.Tid;

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
    public void skal_opprette_historikkinnslag_når_arbeidsforholdet_har_arbeidsforholdHandlingType_bruk() {
        // Arrange
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var arbeidsgiver = Arbeidsgiver.virksomhet("000000000");
        LocalDate stp = LocalDate.of(2019, 1, 1);

        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        String navn = "Utlandet";
        LocalDate fomDato = stp.minusYears(3);
        BigDecimal stillingsprosent = BigDecimal.valueOf(100);
        arbeidsforholdDto.setStillingsprosent(stillingsprosent);
        arbeidsforholdDto.setNavn(navn);
        arbeidsforholdDto.setArbeidsforhold(new ArbeidsforholdIdDto(arbeidsforholdId.getUUIDReferanse(), "1234"));
        arbeidsforholdDto.setId(arbeidsgiver.getIdentifikator() + "-" + arbeidsforholdId.getUUIDReferanse()); // identifikator + "-" + arbeidsforholdsIdIntern
        arbeidsforholdDto.setStillingsprosent(stillingsprosent);
        arbeidsforholdDto.setAnsettelsesPerioder(Set.of(new PeriodeDto(fomDato, Tid.TIDENES_ENDE)));
        arbeidsforholdDto.setStillingsprosent(stillingsprosent);
        arbeidsforholdDto.setArbeidsgiver(arbeidsgiver);
        arbeidsforholdDto.setNavn(navn);
        arbeidsforholdDto.setHandlingType(ArbeidsforholdHandlingType.BRUK);

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        // Act
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, arbeidsforholdDto.getBegrunnelse()),
            arbeidsforholdDto, virksomhet, ref, List.of());

        // Assert
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(1);
    }
}
