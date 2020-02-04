package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static org.mockito.ArgumentMatchers.any;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerFelles;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerVedlegg;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;

public class DokumentmottakerVedleggHåndteringVedAvslåttBehandlingTest extends DokumentmottakerTestsupport {

    private DokumentmottakerVedlegg dokumentmottakerVedlegg;
    private Behandlingsoppretter behandlingsoppretterSpied;
    private DokumentmottakerFelles dokumentmottakerFellesSpied;

    @Before
    public void setup() {
        this.behandlingsoppretterSpied = Mockito.spy(behandlingsoppretter);
        this.dokumentmottakerFellesSpied = Mockito.spy(dokumentmottakerFelles);

        Mockito.doNothing().when(dokumentmottakerFellesSpied).opprettHistorikkinnslagForVedlegg(Mockito.any(), Mockito.any(), Mockito.any());

        dokumentmottakerVedlegg = new DokumentmottakerVedlegg(
            repositoryProvider,
            dokumentmottakerFellesSpied,
            behandlingsoppretterSpied,
            kompletthetskontroller);
    }

    @Test
    public void gittAvslåttBehandlingPgaManglendeDokMedIkkeUtløptFristForInnsendingSkalOppretteNyFørstegangsbehandling() {
        //Arrange
        Behandling nyBehandling = opprettNyBehandlingUtenVedtak(FagsakYtelseType.FORELDREPENGER);
        Mockito.doReturn(nyBehandling).when(dokumentmottakerFellesSpied).opprettNyFørstegangFraAvslag(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(true).when(dokumentmottakerFellesSpied).skalOppretteNyFørstegangsbehandling(any());
        Behandling behandling = opprettBehandling(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            VedtakResultatType.AVSLAG,
            DATO_FØR_INNSENDINGSFRISTEN);
        MottattDokument inntektsmelding = dummyVedleggDokument(behandling);

        // Act
        dokumentmottakerVedlegg.mottaDokument(inntektsmelding, behandling.getFagsak(), inntektsmelding.getDokumentType(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettNyFørstegangFraAvslag(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void gittAvslåttBehandlingPgaManglendeDokMedUtløptFristForInnsendingSkalOppretteTaskForÅVurdereDokument() {
        //Arrange
        Mockito.doNothing().when(dokumentmottakerFellesSpied).opprettTaskForÅVurdereDokument(Mockito.any(), Mockito.any(), Mockito.any());
        Behandling behandling = opprettBehandling(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            VedtakResultatType.AVSLAG,
            DATO_ETTER_INNSENDINGSFRISTEN);
        MottattDokument inntektsmelding = dummyVedleggDokument(behandling);

        // Act
        dokumentmottakerVedlegg.mottaDokument(inntektsmelding, behandling.getFagsak(), inntektsmelding.getDokumentType(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(behandlingsoppretterSpied, Mockito.never()).opprettNyFørstegangsbehandling(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettTaskForÅVurdereDokument(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void gittAvslåttBehandlingMenIkkePgaManglendeDokMedSkalOppretteTaskForÅVurdereDokument() {
        //Arrange
        Mockito.doNothing().when(dokumentmottakerFellesSpied).opprettTaskForÅVurdereDokument(Mockito.any(), Mockito.any(), Mockito.any());
        Behandling behandling = opprettBehandling(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG,
            VedtakResultatType.AVSLAG,
            DATO_ETTER_INNSENDINGSFRISTEN);
        MottattDokument inntektsmelding = dummyVedleggDokument(behandling);

        // Act
        dokumentmottakerVedlegg.mottaDokument(inntektsmelding, behandling.getFagsak(), inntektsmelding.getDokumentType(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(behandlingsoppretterSpied, Mockito.never()).opprettNyFørstegangsbehandling(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettTaskForÅVurdereDokument(Mockito.any(), Mockito.any(), Mockito.any());
    }

}
