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
import org.assertj.core.api.MapAssert;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

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
        Tuple<BigQueryTable, JSONObject> fagsakStatusStatistikk = statistikkRepository.fagsakStatusStatistikk();
        assertThat(fagsakStatusStatistikk.getElement1()).isEqualTo(BigQueryTable.FAGSAK_STATUS_TABELL_V1);
        JSONObject jsonObject = fagsakStatusStatistikk.getElement2();
        System.out.println(jsonObject.toString(2));

        String tabellNavn = "fagsak_status_v1";
        String statusKolonne = "fagsak_status";
        String antallKolonne = "antall";

        // Så skal vi ha en JSON med riktig struktur og data
        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.isEmpty()).isFalse();

        // Sjekk at vi har de forventede statusene og antall
        assertJsonInneholderTabellNavn(jsonObject, tabellNavn);
        assertJsonInneholderAntallDataPunkter(jsonObject, tabellNavn, 4);

        assertJsonInneholerEksaktData(jsonObject, tabellNavn,
            Map.of(statusKolonne, FagsakStatus.OPPRETTET.getKode(), antallKolonne, 2),
            Map.of(statusKolonne, FagsakStatus.UNDER_BEHANDLING.getKode(), antallKolonne, 3),
            Map.of(statusKolonne, FagsakStatus.LØPENDE.getKode(), antallKolonne, 4),
            Map.of(statusKolonne, FagsakStatus.AVSLUTTET.getKode(), antallKolonne, 5)
        );
    }

    private static void assertJsonInneholerEksaktData(JSONObject jsonObject, String tabellNavn, Map<String, Object>... forventedeData) {
        assertThat(jsonObject.getJSONArray(tabellNavn).toList()).containsExactlyInAnyOrder(forventedeData);
    }

    private static void assertJsonInneholderAntallDataPunkter(JSONObject jsonObject, String tabellNavn, int antall) {
        assertThat(jsonObject.getJSONArray(tabellNavn).length()).isEqualTo(antall);
    }

    private static MapAssert<String, Object> assertJsonInneholderTabellNavn(JSONObject jsonObject, String tabellNavn) {
        return assertThat(jsonObject.toMap()).containsKey(tabellNavn);
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
