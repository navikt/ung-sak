package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.vedtak.IverksettingStatus;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.sak.kontrakt.behandling.BehandlingÅrsakDto;
import no.nav.k9.sak.kontrakt.behandling.UtvidetBehandlingDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;

public class BehandlingDtoForBackendTjenesteTest {

    private static final String ANSVARLIG_SAKSBEHANDLER = "ABCD";
    private static final BehandlingÅrsakType BEHANDLING_ÅRSAK_TYPE = BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    private static final BehandlingResultatType BEHANDLING_RESULTAT_TYPE = BehandlingResultatType.INNVILGET_ENDRING;
    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private BehandlingDtoForBackendTjeneste behandlingDtoForBackendTjeneste = new BehandlingDtoForBackendTjeneste(repositoryProvider);
    private LocalDateTime now = LocalDateTime.now();

    @Test
    public void skal_lage_BehandlingDto() {
        Behandling behandling = lagBehandling();
        lagBehandligVedtak(behandling);
        avsluttBehandling(behandling);

        UtvidetBehandlingDto utvidetBehandlingDto = behandlingDtoForBackendTjeneste.lagBehandlingDto(behandling, null);
        assertThat(utvidetBehandlingDto.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHANDLER);
        assertThat(utvidetBehandlingDto.isBehandlingPåVent()).isFalse();

        assertThat(utvidetBehandlingDto.getBehandlingÅrsaker()).isNotEmpty();
        assertThat(utvidetBehandlingDto.getBehandlingÅrsaker().size()).isEqualTo(1);
        BehandlingÅrsakDto behandlingÅrsak = utvidetBehandlingDto.getBehandlingÅrsaker().get(0);
        assertThat(behandlingÅrsak.getBehandlingArsakType()).isEqualByComparingTo(BEHANDLING_ÅRSAK_TYPE);

        assertThat(utvidetBehandlingDto.getSpråkkode()).isEqualTo(Språkkode.nb);
        assertThat(utvidetBehandlingDto.getOriginalVedtaksDato()).isEqualTo(now.toLocalDate().toString());
        assertThat(utvidetBehandlingDto.getBehandlingsresultat().getType()).isEqualByComparingTo(BEHANDLING_RESULTAT_TYPE);
        assertThat(utvidetBehandlingDto.getLinks()).isNotEmpty();
    }

    private Behandling lagBehandling() {
        Personinfo personinfo = lagPersonInfo();
        NavBruker navBruker = NavBruker.opprettNy(personinfo);
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker, new Saksnummer("12345"));
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);

        Behandling behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BEHANDLING_ÅRSAK_TYPE))
            .build();
        Behandlingsresultat behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BEHANDLING_RESULTAT_TYPE)
            .buildFor(behandling);
        behandling.setBehandlingresultat(behandlingsresultat);
        behandling.setAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER);

        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        Long behandlingId = behandlingRepository.lagre(behandling, behandlingLås);
        return behandlingRepository.hentBehandling(behandlingId);
    }

    private Personinfo lagPersonInfo() {
        return new Personinfo.Builder().medAktørId(AktørId.dummy())
            .medKjønn(NavBrukerKjønn.KVINNE)
            .medNavn("Lorem Ipsum")
            .medPersonIdent(new PersonIdent("1243434"))
            .medFødselsdato(LocalDate.now().minusYears(20))
            .medForetrukketSpråk(Språkkode.nb).build();
    }

    private void lagBehandligVedtak(Behandling behandling) {
        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder(behandling.getId())
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
            .medIverksettingStatus(IverksettingStatus.IVERKSATT)
            .medVedtakstidspunkt(now)
            .medBeslutning(true).build();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        repositoryProvider.getBehandlingVedtakRepository().lagre(behandlingVedtak, behandlingLås);
    }

    private void avsluttBehandling(Behandling behandling) {
        behandling.avsluttBehandling();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
    }
}
