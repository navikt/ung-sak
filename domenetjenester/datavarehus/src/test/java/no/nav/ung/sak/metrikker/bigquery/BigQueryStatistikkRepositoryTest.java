package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BigQueryStatistikkRepositoryTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private @Any Instance<ProsessTaskHandler> handlers;

    @Inject
    private FagsakRepository fagsakRepository;

    private BigQueryStatistikkRepository statistikkRepository;

    @BeforeEach
    void setup() {
        statistikkRepository = new BigQueryStatistikkRepository(entityManager, handlers);
    }

    @Test
    void skal_kunne_hente_fagsak_status_statistikk() {
        // Gitt eksisterende fagsaker med ulike status
        lagreFagsaker(List.of(
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.OPPRETTET),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.OPPRETTET),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.UNDER_BEHANDLING),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.UNDER_BEHANDLING),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.UNDER_BEHANDLING),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.LØPENDE),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.LØPENDE),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.LØPENDE),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.LØPENDE),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.AVSLUTTET),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.AVSLUTTET),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.AVSLUTTET),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.AVSLUTTET),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.AVSLUTTET),
            byggFagsak(FagsakYtelseType.OBSOLETE, FagsakStatus.LØPENDE) // OBSOLETE fagsak for å teste at den ikke telles med
        ));

        // Når vi henter statistikken
        Collection<FagsakStatusRecord> fagsakStatusRecords = statistikkRepository.fagsakStatusStatistikk();

        // Så skal vi ha riktige records med korrekt antall for hver status
        assertThat(fagsakStatusRecords).isNotNull();
        assertThat(fagsakStatusRecords).hasSize(4); // 4 forskjellige statuser

        // Verifiser at vi har korrekt antall for hver status
        Map<FagsakStatus, Long> statusCounts = fagsakStatusRecords.stream()
            .collect(Collectors.groupingBy(
                FagsakStatusRecord::fagsakStatus,
                Collectors.summingLong(record -> record.antall().longValue())
            ));

        assertThat(statusCounts)
            .containsEntry(FagsakStatus.OPPRETTET, 2L)
            .containsEntry(FagsakStatus.UNDER_BEHANDLING, 3L)
            .containsEntry(FagsakStatus.LØPENDE, 4L)
            .containsEntry(FagsakStatus.AVSLUTTET, 5L);
    }

    @Test
    void skal_kunne_hente_hyppig_rapporterte_metrikker() {
        // Gitt eksisterende fagsaker med ulike status
        lagreFagsaker(List.of(
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.OPPRETTET),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.LØPENDE),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.AVSLUTTET),
            byggFagsak(FagsakYtelseType.OBSOLETE, FagsakStatus.LØPENDE) // OBSOLETE fagsak
        ));

        // Når vi henter hyppig rapporterte metrikker
        List<Tuple<BigQueryTabell<?>, Collection<?>>> metrikker = statistikkRepository.hentHyppigRapporterte();

        // Så skal vi ha minst én metrikk
        assertThat(metrikker).isNotEmpty();

        // Og første metrikk skal være for FAGSAK_STATUS_V1 tabellen
        Tuple<BigQueryTabell<?>, Collection<?>> fagsakStatusMetrikk = metrikker.get(0);
        assertThat(fagsakStatusMetrikk.getElement1()).isEqualTo(Tabeller.FAGSAK_STATUS_V2);

        // Og den skal inneholde records (vi verifiserer ikke antallet her siden det er testet i den andre testen)
        Collection<?> records = fagsakStatusMetrikk.getElement2();
        assertThat(records).isNotEmpty();
        assertThat(records.iterator().next()).isInstanceOf(FagsakStatusRecord.class);
    }

    private Fagsak byggFagsak(FagsakYtelseType fagsakYtelseType, FagsakStatus status) {
        return FagsakBuilder.nyFagsak(fagsakYtelseType).medStatus(status).build();
    }

    private void lagreFagsaker(List<Fagsak> fagsaker) {
        fagsaker.forEach(
            fagsak -> {
                fagsakRepository.opprettNy(fagsak);
                entityManager.flush();
            }
        );
    }
}
