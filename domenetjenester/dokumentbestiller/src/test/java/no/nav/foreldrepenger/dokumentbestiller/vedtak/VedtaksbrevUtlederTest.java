package no.nav.foreldrepenger.dokumentbestiller.vedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

public class VedtaksbrevUtlederTest {

    private Behandlingsresultat behandlingsresultatMock = Mockito.mock(Behandlingsresultat.class);
    private BehandlingVedtak behandlingVedtakMock = Mockito.mock(BehandlingVedtak.class);
    private Behandling behandling = Mockito.mock(Behandling.class);

    @Before
    public void setup() {
        doReturn(behandling).when(behandlingsresultatMock).getBehandling();
        doReturn(Vedtaksbrev.AUTOMATISK).when(behandlingsresultatMock).getVedtaksbrev();
        doReturn(false).when(behandlingVedtakMock).isBeslutningsvedtak();
        doReturn(VedtakResultatType.INNVILGET).when(behandlingVedtakMock).getVedtakResultatType();
        doReturn(FagsakYtelseType.ENGANGSTØNAD).when(behandling).getFagsakYtelseType();
    }


    @Test
    public void skal_velge_positivt_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandlingsresultatMock, behandlingVedtakMock)).isEqualTo(DokumentMalType.INNVILGELSE_DOK);
    }

    @Test
    public void skal_velge_opphør_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        doReturn(true).when(behandlingsresultatMock).isBehandlingsresultatOpphørt();
        doReturn(VedtakResultatType.AVSLAG).when(behandlingVedtakMock).getVedtakResultatType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandlingsresultatMock, behandlingVedtakMock)).isEqualTo(DokumentMalType.OPPHØR_DOK);
    }

    @Test
    public void skal_velge_avslag_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        doReturn(BehandlingResultatType.AVSLÅTT).when(behandlingsresultatMock).getBehandlingResultatType();
        doReturn(VedtakResultatType.AVSLAG).when(behandlingVedtakMock).getVedtakResultatType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandlingsresultatMock, behandlingVedtakMock)).isEqualTo(DokumentMalType.AVSLAG__DOK);
    }

    @Test
    public void skal_velge_uendret_utfall() {
        doReturn(true).when(behandlingVedtakMock).isBeslutningsvedtak();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandlingsresultatMock, behandlingVedtakMock)).isEqualTo(DokumentMalType.UENDRETUTFALL_DOK);
    }

    @Test
    public void skal_velge_fritekst() {
        doReturn(Vedtaksbrev.FRITEKST).when(behandlingsresultatMock).getVedtaksbrev();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandlingsresultatMock, behandlingVedtakMock)).isEqualTo(DokumentMalType.FRITEKST_DOK);
    }

}
