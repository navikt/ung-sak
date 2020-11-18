package no.nav.k9.sak.historikk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class HistorikkRepositoryImplTest {

    @Inject
    private EntityManager entityManager;

    private Repository repository;
    private BasicBehandlingBuilder behandlingBuilder;
    private HistorikkRepository historikkRepository;
    private Fagsak fagsak;

    @BeforeEach
    public void setup() {
        repository = new Repository(entityManager);
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
        historikkRepository = new HistorikkRepository(entityManager);
        fagsak = behandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
    }

    @Test
    public void lagrerHistorikkinnslag() {
        repository.lagre(fagsak);
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        repository.lagre(behandling);
        repository.flush();

        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.SØKER);
        historikkinnslag.setBehandling(behandling);
        historikkinnslag.setType(HistorikkinnslagType.VEDTAK_FATTET);
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.VEDTAK_FATTET)
            .medSkjermlenke(SkjermlenkeType.VEDTAK);
        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
        List<Historikkinnslag> historikk = historikkRepository.hentHistorikk(behandling.getId());
        assertThat(historikk).hasSize(1);

        Historikkinnslag lagretHistorikk = historikk.get(0);
        assertThat(lagretHistorikk.getAktør().getKode()).isEqualTo(historikkinnslag.getAktør().getKode());
        assertThat(lagretHistorikk.getType().getKode()).isEqualTo(historikkinnslag.getType().getKode());
        assertThat(lagretHistorikk.getHistorikkTid()).isNull();
    }

    @Test
    public void henterAlleHistorikkinnslagForBehandling() {
        repository.lagre(fagsak);
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        repository.lagre(behandling);
        repository.flush();

        Historikkinnslag vedtakFattet = new Historikkinnslag();
        vedtakFattet.setAktør(HistorikkAktør.SØKER);
        vedtakFattet.setBehandling(behandling);
        vedtakFattet.setType(HistorikkinnslagType.VEDTAK_FATTET);
        HistorikkInnslagTekstBuilder vedtakFattetBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.VEDTAK_FATTET)
            .medSkjermlenke(SkjermlenkeType.VEDTAK);
        vedtakFattetBuilder.build(vedtakFattet);
        historikkRepository.lagre(vedtakFattet);

        Historikkinnslag brevSent = new Historikkinnslag();
        brevSent.setBehandling(behandling);
        brevSent.setType(HistorikkinnslagType.BREV_SENT);
        brevSent.setAktør(HistorikkAktør.SØKER);
        HistorikkInnslagTekstBuilder mottattDokBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.BREV_SENT);
        mottattDokBuilder.build(brevSent);
        historikkRepository.lagre(brevSent);

        List<Historikkinnslag> historikk = historikkRepository.hentHistorikk(behandling.getId());
        assertThat(historikk).hasSize(2);
        assertThat(historikk.stream().anyMatch(h -> HistorikkinnslagType.VEDTAK_FATTET.equals(h.getType()))).isTrue();
        assertThat(historikk.stream().anyMatch(h -> HistorikkinnslagType.BREV_SENT.equals(h.getType()))).isTrue();
    }
}
