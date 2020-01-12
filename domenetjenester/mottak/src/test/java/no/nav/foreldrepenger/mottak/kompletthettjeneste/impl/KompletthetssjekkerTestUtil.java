package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class KompletthetssjekkerTestUtil {

    public static final AktørId AKTØR_ID  = AktørId.dummy();
    public static final String ARBGIVER1 = "123456789";
    public static final String ARBGIVER2 = "234567890";

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    public KompletthetssjekkerTestUtil(BehandlingRepositoryProvider repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
    }

    public TestScenarioBuilder opprettRevurderingsscenarioForMor() {
        var scenario = TestScenarioBuilder.builderMedSøknad(AKTØR_ID);
        Behandling førstegangsbehandling = opprettOgAvsluttFørstegangsbehandling(scenario);

        var scenario2 = TestScenarioBuilder.builderUtenSøknad(AKTØR_ID)
            .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
            .medBehandlingType(BehandlingType.REVURDERING);
        return scenario2;
    }

    private Behandling opprettOgAvsluttFørstegangsbehandling(AbstractTestScenario<?> scenario) {
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(7))
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler("Nav Navsdotter")
            .build();
        Behandling førstegangsbehandling = scenario.lagre(repositoryProvider);
        avsluttBehandlingOgFagsak(førstegangsbehandling);
        return førstegangsbehandling;
    }

    private void avsluttBehandlingOgFagsak(Behandling behandling) {
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.LØPENDE);
    }

    public void byggOgLagreSøknadMed(Behandling behandling, boolean erEndringssøknad, LocalDate søknadsDato) {
        SøknadEntitet søknad = new SøknadEntitet.Builder().medElektroniskRegistrert(true)
            .medSøknadsdato(søknadsDato)
            .medMottattDato(LocalDate.now())
            .medErEndringssøknad(erEndringssøknad)
            .build();
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
    }


    public void byggOgLagreFørstegangsSøknadMedMottattdato(Behandling behandling, LocalDate søknadsdato) {
        SøknadEntitet søknad = new SøknadEntitet.Builder().medElektroniskRegistrert(true)
            .medSøknadsdato(søknadsdato)
            .medMottattDato(søknadsdato)
            .medErEndringssøknad(false)
            .build();
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
    }

}
