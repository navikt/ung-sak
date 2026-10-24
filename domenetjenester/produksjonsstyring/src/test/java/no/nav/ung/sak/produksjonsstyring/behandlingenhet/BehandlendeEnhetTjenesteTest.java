package no.nav.ung.sak.produksjonsstyring.behandlingenhet;

import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingAnsvarlig;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.typer.AktørId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BehandlendeEnhetTjenesteTest {

    private static final Long FAGSAK_ID = 1L;
    private static final Long FORRIGE_BEHANDLING_ID = 2L;
    private static final AktørId AKTØR_ID = AktørId.dummy();
    private static final OrganisasjonsEnhet SENTRAL_ENHET = new OrganisasjonsEnhet("4863", "Nav sentral");
    private static final OrganisasjonsEnhet NAV_KONTOR = new OrganisasjonsEnhet("0219", "Nav Bærum");

    private EnhetsTjeneste enhetsTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;
    private BehandlendeEnhetOppsett oppsett;
    private BehandlendeEnhetTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        enhetsTjeneste = mock(EnhetsTjeneste.class);
        behandlingRepository = mock(BehandlingRepository.class);
        behandlingAnsvarligRepository = mock(BehandlingAnsvarligRepository.class);
        oppsett = mock(BehandlendeEnhetOppsett.class);
        var provider = mock(BehandlingRepositoryProvider.class);
        when(provider.getBehandlingRepository()).thenReturn(behandlingRepository);
        tjeneste = new BehandlendeEnhetTjeneste(enhetsTjeneste, mock(BehandlingEnhetEventPubliserer.class), provider, behandlingAnsvarligRepository,
            new UnitTestLookupInstanceImpl<>(oppsett));
    }

    @Test
    void ungdomsytelse_får_bare_sentral_enhet() {
        when(oppsett.deler()).thenReturn(Set.of(BehandlingDel.SENTRAL));
        when(enhetsTjeneste.hentEnhetSjekkKunAktør(eq(AKTØR_ID), eq(FagsakYtelseType.UNGDOMSYTELSE), isNull())).thenReturn(SENTRAL_ENHET);

        var enheter = tjeneste.finnBehandlendeEnheterFor(fagsak(FagsakYtelseType.UNGDOMSYTELSE));

        assertThat(enheter).containsExactlyEntriesOf(Map.of(BehandlingDel.SENTRAL, SENTRAL_ENHET));
    }

    @Test
    void aktivitetspenger_slår_opp_hver_del_med_sitt_behandlingstema() {
        aktivitetspengerOppsett();
        when(enhetsTjeneste.hentEnhetSjekkKunAktør(AKTØR_ID, FagsakYtelseType.AKTIVITETSPENGER, "ytelse")).thenReturn(SENTRAL_ENHET);
        when(enhetsTjeneste.hentEnhetSjekkKunAktør(AKTØR_ID, FagsakYtelseType.AKTIVITETSPENGER, "inngangsvilkår")).thenReturn(NAV_KONTOR);

        var enheter = tjeneste.finnBehandlendeEnheterFor(fagsak(FagsakYtelseType.AKTIVITETSPENGER));

        assertThat(enheter)
            .containsEntry(BehandlingDel.SENTRAL, SENTRAL_ENHET)
            .containsEntry(BehandlingDel.NAV_KONTOR, NAV_KONTOR)
            .hasSize(2);
    }

    @Test
    void gjenbruker_gyldig_enhet_fra_forrige_behandling_per_del() {
        aktivitetspengerOppsett();
        forrigeBehandlingHarEnhet(BehandlingDel.NAV_KONTOR, NAV_KONTOR);
        when(enhetsTjeneste.finnOrganisasjonsEnhet(any(), any(), eq(NAV_KONTOR.getEnhetId()))).thenReturn(Optional.of(NAV_KONTOR));
        when(enhetsTjeneste.hentEnhetSjekkKunAktør(any(), any(), any())).thenReturn(SENTRAL_ENHET);

        var enheter = tjeneste.finnBehandlendeEnheterFor(fagsak(FagsakYtelseType.AKTIVITETSPENGER));

        assertThat(enheter)
            .containsEntry(BehandlingDel.SENTRAL, SENTRAL_ENHET)
            .containsEntry(BehandlingDel.NAV_KONTOR, NAV_KONTOR);
    }

    @Test
    void slår_opp_på_nytt_når_forrige_enhet_er_ugyldig() {
        forrigeBehandlingHarEnhet(BehandlingDel.SENTRAL, NAV_KONTOR);
        when(enhetsTjeneste.finnOrganisasjonsEnhet(any(), any(), any())).thenReturn(Optional.empty());
        when(enhetsTjeneste.hentEnhetSjekkKunAktør(eq(AKTØR_ID), eq(FagsakYtelseType.UNGDOMSYTELSE), isNull())).thenReturn(SENTRAL_ENHET);

        var enhet = tjeneste.finnBehandlendeEnhetFor(fagsak(FagsakYtelseType.UNGDOMSYTELSE));

        assertThat(enhet).isEqualTo(SENTRAL_ENHET);
        verify(behandlingAnsvarligRepository, never()).hentBehandlingAnsvarlig(FORRIGE_BEHANDLING_ID, BehandlingDel.NAV_KONTOR);
    }

    private void aktivitetspengerOppsett() {
        when(oppsett.deler()).thenReturn(Set.of(BehandlingDel.SENTRAL, BehandlingDel.NAV_KONTOR));
        when(oppsett.behandlingstema(BehandlingDel.SENTRAL)).thenReturn("ytelse");
        when(oppsett.behandlingstema(BehandlingDel.NAV_KONTOR)).thenReturn("inngangsvilkår");
    }

    private static Fagsak fagsak(FagsakYtelseType ytelseType) {
        var fagsak = mock(Fagsak.class);
        when(fagsak.getId()).thenReturn(FAGSAK_ID);
        when(fagsak.getYtelseType()).thenReturn(ytelseType);
        when(fagsak.getAktørId()).thenReturn(AKTØR_ID);
        return fagsak;
    }

    private void forrigeBehandlingHarEnhet(BehandlingDel behandlingDel, OrganisasjonsEnhet enhet) {
        var forrigeBehandling = mock(Behandling.class);
        when(forrigeBehandling.getId()).thenReturn(FORRIGE_BEHANDLING_ID);
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_ID)).thenReturn(Optional.of(forrigeBehandling));
        var ansvarlig = new BehandlingAnsvarlig(FORRIGE_BEHANDLING_ID, behandlingDel);
        ansvarlig.setBehandlendeEnhet(enhet);
        when(behandlingAnsvarligRepository.hentBehandlingAnsvarlig(FORRIGE_BEHANDLING_ID, behandlingDel)).thenReturn(Optional.of(ansvarlig));
    }
}
