package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static org.mockito.ArgumentMatchers.any;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.domene.uttak.UttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;

public class DokumentmottakerInntektsmeldingHåndteringVedAvslåttBehandlingTest extends DokumentmottakerTestsupport {

    private DokumentmottakerInntektsmelding dokumentmottakerInntektsmelding;
    private Behandlingsoppretter behandlingsoppretterSpied;
    private DokumentmottakerFelles dokumentmottakerFellesSpied;

    @Before
    public void setup() {
        this.behandlingsoppretterSpied = Mockito.spy(super.behandlingsoppretter);
        this.dokumentmottakerFellesSpied = Mockito.spy(super.dokumentmottakerFelles);
        UttakTjeneste uttakTjeneste = Mockito.mock(UttakTjeneste.class);

        Mockito.doNothing().when(dokumentmottakerFellesSpied).opprettTaskForÅVurdereDokument(any(), any(), any());
        
        dokumentmottakerInntektsmelding = new DokumentmottakerInntektsmelding(
            dokumentmottakerFellesSpied,
            mottatteDokumentTjeneste,
            behandlingsoppretterSpied,
            kompletthetskontroller,
            uttakTjeneste,
            repositoryProvider);
    }

    @Test
    public void gittAvslåttBehandlingPgaManglendeDokMedIkkeUtløptFristForInnsendingSkalOppretteNyFørstegangsbehandling() {
        //Arrange
        Behandling nyBehandling = opprettNyBehandlingUtenVedtak(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
        Mockito.doReturn(nyBehandling).when(dokumentmottakerFellesSpied).opprettNyFørstegangFraAvslag(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(true).when(dokumentmottakerFellesSpied).skalOppretteNyFørstegangsbehandling(any());

        Behandling avslåttBehandling = opprettBehandling(
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            VedtakResultatType.AVSLAG,
            DATO_FØR_INNSENDINGSFRISTEN);
        MottattDokument inntektsmelding = dummyInntektsmeldingDokument(avslåttBehandling);

        // Act
        dokumentmottakerInntektsmelding.mottaDokument(inntektsmelding, avslåttBehandling.getFagsak(), inntektsmelding.getDokumentType(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettNyFørstegangFraAvslag(any(), any(), any());
    }

    @Test
    public void gittAvslåttBehandlingPgaManglendeDokMedUtløptFristForInnsendingSkalOppretteTaskForÅVurdereDokument() {
        //Arrange
        Behandling behandling = opprettBehandling(
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            VedtakResultatType.AVSLAG,
            DATO_ETTER_INNSENDINGSFRISTEN);
        MottattDokument inntektsmelding = dummyInntektsmeldingDokument(behandling);

        // Act
        dokumentmottakerInntektsmelding.mottaDokument(inntektsmelding, behandling.getFagsak(), inntektsmelding.getDokumentType(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(behandlingsoppretterSpied, Mockito.never()).opprettNyFørstegangsbehandling(any(), any(), any());
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettTaskForÅVurdereDokument(any(), any(), any());
    }

    @Test
    public void gittAvslåttBehandlingMenIkkePgaManglendeDokMedSkalOppretteTaskForÅVurdereDokument() {
        //Arrange
        Behandling behandling = opprettBehandling(
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            VedtakResultatType.AVSLAG,
            DATO_ETTER_INNSENDINGSFRISTEN);
        MottattDokument inntektsmelding = dummyInntektsmeldingDokument(behandling);

        // Act
        dokumentmottakerInntektsmelding.mottaDokument(inntektsmelding, behandling.getFagsak(), inntektsmelding.getDokumentType(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(behandlingsoppretterSpied, Mockito.never()).opprettNyFørstegangsbehandling(any(), any(), any());
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettTaskForÅVurdereDokument(any(), any(), any());
    }

    @Test
    public void gittHenlagtBehandlingSkalOppretteVurderDokumentInntilVidere() {
        //Arrange
        Mockito.doReturn(true).when(behandlingsoppretterSpied).erBehandlingOgFørstegangsbehandlingHenlagt(any());
        
        Behandling behandling = opprettBehandling(
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.MANGLER_BEREGNINGSREGLER,
            VedtakResultatType.UDEFINERT,
            DATO_ETTER_INNSENDINGSFRISTEN);
        MottattDokument inntektsmelding = dummyInntektsmeldingDokument(behandling);

        // Act
        dokumentmottakerInntektsmelding.mottaDokument(inntektsmelding, behandling.getFagsak(), inntektsmelding.getDokumentType(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(behandlingsoppretterSpied, Mockito.never()).opprettNyFørstegangsbehandling(any(), any(), any());
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettTaskForÅVurdereDokument(any(), any(), any());
    }

}
