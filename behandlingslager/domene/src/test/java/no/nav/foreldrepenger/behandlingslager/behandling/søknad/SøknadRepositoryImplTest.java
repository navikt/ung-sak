package no.nav.foreldrepenger.behandlingslager.behandling.søknad;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;

public class SøknadRepositoryImplTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private SøknadRepository søknadRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    @Before
    public void setup() {
        søknadRepository = repositoryProvider.getSøknadRepository();
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        fagsakRepository = repositoryProvider.getFagsakRepository();
    }

    @Test
    public void skal_finne_endringssøknad_for_behandling() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(lagPerson()));
        fagsakRepository.opprettNy(fagsak);

        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        Behandling behandling2 = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling2, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling2));

        SøknadEntitet søknad = opprettSøknad(false);
        søknadRepository.lagreOgFlush(behandling, søknad);

        SøknadEntitet søknad2 = opprettSøknad(true);
        søknadRepository.lagreOgFlush(behandling2, søknad2);

        // Act
        Optional<SøknadEntitet> endringssøknad = repositoryProvider.getSøknadRepository().hentSøknadHvisEksisterer(behandling.getId());
        Optional<SøknadEntitet> endringssøknad2 = repositoryProvider.getSøknadRepository().hentSøknadHvisEksisterer(behandling2.getId());

        // Assert
        assertThat(endringssøknad).isPresent();
        assertThat(endringssøknad2).isPresent();
        assertThat(endringssøknad.get()).isNotEqualTo(endringssøknad2.get());
    }

    @Test
    public void skal_ikke_finne_endringssøknad_for_behandling() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(lagPerson()));
        fagsakRepository.opprettNy(fagsak);

        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        SøknadEntitet søknad = opprettSøknad(false);
        søknadRepository.lagreOgFlush(behandling, søknad);

        // Act
        Optional<SøknadEntitet> endringssøknad = repositoryProvider.getSøknadRepository().hentSøknadHvisEksisterer(behandling.getId());

        // Assert
        assertThat(endringssøknad).isPresent();
        assertThat(endringssøknad.get().erEndringssøknad()).isFalse();
    }

    @Test
    public void skal_kopiere_søknadsgrunnlaget_fra_behandling1_til_behandling2() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(lagPerson()));
        fagsakRepository.opprettNy(fagsak);

        Behandling behandling1 = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling1, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling1));
        SøknadEntitet søknad = opprettSøknad(false);
        søknadRepository.lagreOgFlush(behandling1, søknad);

        Behandling behandling2 = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling2, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling2));

        // Act
        søknadRepository.kopierGrunnlagFraEksisterendeBehandling(behandling1, behandling2);

        // Assert
        Optional<SøknadEntitet> søknadEntitet = søknadRepository.hentSøknadHvisEksisterer(behandling2.getId());
        assertThat(søknadEntitet).isPresent();
    }

    private SøknadEntitet opprettSøknad(boolean erEndringssøknad) {
        return new SøknadEntitet.Builder()
            .medSøknadsdato(LocalDate.now().minusDays(1))
            .medErEndringssøknad(erEndringssøknad)
            .build();
    }

    private Personinfo lagPerson() {
        final Personinfo personinfo = new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AktørId.dummy())
            .medFødselsdato(LocalDate.now().minusYears(20))
            .medLandkode(Landkoder.NOR)
            .medKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(new PersonIdent("12345678901"))
            .medForetrukketSpråk(Språkkode.nb)
            .build();
        return personinfo;
    }
}
