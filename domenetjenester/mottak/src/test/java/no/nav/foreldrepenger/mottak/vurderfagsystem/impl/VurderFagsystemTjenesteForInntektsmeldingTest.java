package no.nav.foreldrepenger.mottak.vurderfagsystem.impl;

import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.BehandlingslagerTestUtil.lagNavBruker;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.ARBEIDSFORHOLDSID;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.JOURNALPOST_ID;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.VIRKSOMHETSNUMMER;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.buildFagsakMedUdefinertRelasjon;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.byggBehandlingUdefinert;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.byggVurderFagsystemForInntektsmelding;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesUtils;
import no.nav.foreldrepenger.mottak.vurderfagsystem.fp.VurderFagsystemTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingTema;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

public class VurderFagsystemTjenesteForInntektsmeldingTest {

    private VurderFagsystemFellesTjeneste vurderFagsystemTjeneste;
    @Mock
    private BehandlingRepository behandlingRepository;

    @Mock
    private FagsakRepository fagsakRepositoryMock;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private FagsakTjeneste fagsakTjenesteMock;

    private Fagsak fpFagsakUdefinert = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker());

    @Before
    public void setUp() {
        fagsakTjenesteMock = mock(FagsakTjeneste.class);
        when(fagsakTjenesteMock.hentJournalpost(any())).thenReturn(Optional.empty());
        BehandlingRepositoryProvider repositoryProvider = mock(BehandlingRepositoryProvider.class);
        behandlingRepository = mock(BehandlingRepository.class);
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        fagsakRepositoryMock = mock(FagsakRepository.class);
        when(repositoryProvider.getFagsakRepository()).thenReturn(fagsakRepositoryMock);
        MottatteDokumentTjeneste mottatteDokumentTjenesteMock = Mockito.mock(MottatteDokumentTjeneste.class);

        var skjæringsTidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        when(skjæringsTidspunktTjeneste.getSkjæringstidspunkter(any()))
            .thenReturn(Skjæringstidspunkt.builder().medFørsteUttaksdato(LocalDate.now()).medUtledetSkjæringstidspunkt(LocalDate.now()).build());
        var fellesUtil = new VurderFagsystemFellesUtils(behandlingRepository, mottatteDokumentTjenesteMock, skjæringsTidspunktTjeneste);
        var tjenesteFP = new VurderFagsystemTjenesteImpl(fellesUtil);
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjenesteMock, fellesUtil, new UnitTestLookupInstanceImpl<>(tjenesteFP));
    }

    @Test
    public void skalReturnereVedtaksløsningMedSaksnummerNårEnSakFinnesOgÅrsakInnsendingErEndring() {
        VurderFagsystem fagsystem = byggVurderFagsystemForInntektsmelding(VurderFagsystem.ÅRSAK_ENDRING, BehandlingTema.FORELDREPENGER, LocalDateTime.now(), AktørId.dummy(), JOURNALPOST_ID,
            ARBEIDSFORHOLDSID, VIRKSOMHETSNUMMER);

        when(fagsakTjenesteMock.finnFagsakerForAktør(any())).thenReturn(Collections.singletonList(buildFagsakMedUdefinertRelasjon(123L, false)));

        Optional<Behandling> behandling = Optional.of(byggBehandlingUdefinert(fpFagsakUdefinert));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandling);

        BehandlendeFagsystem result = vurderFagsystemTjeneste.vurderFagsystem(fagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isNotEmpty();
    }

    @Test
    public void skalReturnereInfotrygdNårBrukerIkkeHarSakIVL() {
        VurderFagsystem fagsystem = byggVurderFagsystemForInntektsmelding(VurderFagsystem.ÅRSAK_NY, BehandlingTema.FORELDREPENGER, LocalDateTime.now(), AktørId.dummy(), JOURNALPOST_ID,
            ARBEIDSFORHOLDSID, VIRKSOMHETSNUMMER);

        when(fagsakRepositoryMock.hentJournalpost(any())).thenReturn(Optional.empty());
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(Collections.emptyList());
        when(fagsakTjenesteMock.finnFagsakerForAktør(any())).thenReturn(Collections.emptyList());

        BehandlendeFagsystem result = vurderFagsystemTjeneste.vurderFagsystem(fagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VURDER_INFOTRYGD);
        assertThat(result.getSaksnummer()).isEmpty();
    }

    @Test
    public void skalFinneArbeidsforholdForArbeidsgiverSomErPrivatperson() {
        AktørId arbeidsgiverAktørId = AktørId.dummy();

        VurderFagsystem fagsystem = byggVurderFagsystemForInntektsmelding(VurderFagsystem.ÅRSAK_ENDRING, BehandlingTema.FORELDREPENGER, LocalDateTime.now(),
            AktørId.dummy(), JOURNALPOST_ID, ARBEIDSFORHOLDSID, null);
        fagsystem.setArbeidsgiverAktørId(arbeidsgiverAktørId);

        when(fagsakTjenesteMock.finnFagsakerForAktør(any())).thenReturn(Collections.singletonList(buildFagsakMedUdefinertRelasjon(123L, false)));

        Optional<Behandling> behandling = Optional.of(byggBehandlingUdefinert(fpFagsakUdefinert));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandling);

        BehandlendeFagsystem result = vurderFagsystemTjeneste.vurderFagsystem(fagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isNotEmpty();
    }
}
