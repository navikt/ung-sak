package no.nav.foreldrepenger.mottak.dokumentmottak.fp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.Dokumentmottaker;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerFelles;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerSøknadDefault;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerTestsupport;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

public class DokumentmottakerSøknadHåndteringVedAvslåttBehandlingTest extends DokumentmottakerTestsupport {

    private Dokumentmottaker dokumentmottakerSøknad;
    private Behandlingsoppretter behandlingsoppretterSpied;

    @Before
    public void setup() {
        this.behandlingsoppretterSpied = Mockito.spy(behandlingsoppretter);
        dokumentmottakerSøknad = new DokumentmottakerSøknadDefault(
            repositoryProvider,
            dokumentmottakerFelles,
            mottatteDokumentTjeneste,
            behandlingsoppretterSpied,
            kompletthetskontroller);
    }


    @Test
    public void skal_opprette_ny_førstegangsbehandling_når_forrige_behandling_var_avslått() {
        //Arrange
        MottatteDokumentTjeneste mockMD = Mockito.mock(MottatteDokumentTjeneste.class);
        HistorikkinnslagTjeneste mockHist = Mockito.mock(HistorikkinnslagTjeneste.class);
        BehandlendeEnhetTjeneste enhetsTjeneste = mock(BehandlendeEnhetTjeneste.class);
        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("0312", "enhetNavn");
        when(enhetsTjeneste.finnBehandlendeEnhetFraSøker(any(Fagsak.class))).thenReturn(enhet);
        when(enhetsTjeneste.finnBehandlendeEnhetFraSøker(any(Behandling.class))).thenReturn(enhet);
        ProsessTaskRepository taskrepo = mock(ProsessTaskRepository.class);
        DokumentmottakerFelles felles = new DokumentmottakerFelles(repositoryProvider,
            taskrepo,
            enhetsTjeneste,
            mockHist,
            mockMD,
            behandlingsoppretterSpied);
        dokumentmottakerSøknad = new DokumentmottakerSøknadDefault(
            repositoryProvider,
            felles,
            mockMD,
            behandlingsoppretterSpied,
            kompletthetskontroller);
        Behandling nyBehandling = opprettNyBehandlingUtenVedtak(FagsakYtelseType.FORELDREPENGER);
        Mockito.doReturn(nyBehandling).when(behandlingsoppretterSpied).opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(Mockito.any(),  Mockito.any());
        doNothing().when(mockMD).persisterDokumentinnhold(any(), any(), any());

        Behandling behandling = opprettBehandling(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            VedtakResultatType.AVSLAG,
            DATO_FØR_INNSENDINGSFRISTEN);
        MottattDokument søknadDokument = dummySøknadDokument(behandling);

        // Act
        dokumentmottakerSøknad.mottaDokument(søknadDokument, behandling.getFagsak(), søknadDokument.getDokumentType(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(behandlingsoppretterSpied, Mockito.times(1)).opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(Mockito.any(), Mockito.any());
    }
}
