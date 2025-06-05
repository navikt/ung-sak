package no.nav.ung.sak.historikk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.historikk.HistorikkinnslagType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.db.util.Repository;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class HistorikkRepositoryImplTest {

    @Inject
    private EntityManager entityManager;

    private Repository repository;
    private BasicBehandlingBuilder behandlingBuilder;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private Fagsak fagsak;

    @BeforeEach
    public void setup() {
        repository = new Repository(entityManager);
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
        historikkinnslagRepository = new HistorikkinnslagRepository(entityManager);
        fagsak = behandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
    }

    @Test
    public void lagrerHistorikkinnslag() {
        repository.lagre(fagsak);
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        repository.lagre(behandling);
        repository.flush();

        var historikkinnslagBuilder = new Historikkinnslag.Builder();
        historikkinnslagBuilder.medAktør(HistorikkAktør.SØKER);
        historikkinnslagBuilder.medBehandlingId(behandling.getId());
        historikkinnslagBuilder.medFagsakId(behandling.getFagsakId());
        historikkinnslagBuilder.medTittel(SkjermlenkeType.VEDTAK);
        historikkinnslagBuilder.addLinje("Vedtak er fattet");

        historikkinnslagRepository.lagre(historikkinnslagBuilder.build());
        List<Historikkinnslag> historikk = historikkinnslagRepository.hent(behandling.getId());
        assertThat(historikk).hasSize(1);

        Historikkinnslag lagretHistorikk = historikk.get(0);
        assertThat(lagretHistorikk.getAktør().getKode()).isEqualTo(HistorikkAktør.SØKER.getKode());
        assertThat(lagretHistorikk.getSkjermlenke()).isEqualTo(SkjermlenkeType.VEDTAK);
        assertThat(lagretHistorikk.getLinjer().size()).isEqualTo(1);
    }

    @Test
    public void henterAlleHistorikkinnslagForBehandling() {
        repository.lagre(fagsak);
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        repository.lagre(behandling);
        repository.flush();

        var vedtakFattetHistorikkBuilder = new Historikkinnslag.Builder();
        vedtakFattetHistorikkBuilder.medAktør(HistorikkAktør.SØKER);
        vedtakFattetHistorikkBuilder.medBehandlingId(behandling.getId());
        vedtakFattetHistorikkBuilder.medFagsakId(behandling.getFagsakId());
        vedtakFattetHistorikkBuilder.medFagsakId(behandling.getFagsakId());
        vedtakFattetHistorikkBuilder.medTittel(SkjermlenkeType.VEDTAK);
        vedtakFattetHistorikkBuilder.addLinje("Vedtak er fattet");

        historikkinnslagRepository.lagre(vedtakFattetHistorikkBuilder.build());

        var brevSentHistorikkBuilder = new Historikkinnslag.Builder();
        brevSentHistorikkBuilder.medBehandlingId(behandling.getId());
        brevSentHistorikkBuilder.medFagsakId(behandling.getFagsakId());
        brevSentHistorikkBuilder.medTittel("Brev er sendt");
        brevSentHistorikkBuilder.addLinje("Brev er sendt til søker");
        brevSentHistorikkBuilder.medAktør(HistorikkAktør.SØKER);

        historikkinnslagRepository.lagre(brevSentHistorikkBuilder.build());

        List<Historikkinnslag> historikk = historikkinnslagRepository.hent(behandling.getId());
        assertThat(historikk).hasSize(2);
        assertThat(historikk.stream().anyMatch(h -> SkjermlenkeType.VEDTAK.equals(h.getSkjermlenke()))).isTrue();
        assertThat(historikk.stream().anyMatch(h -> "Brev er sendt".equals(h.getTittel()))).isTrue();
    }
}
