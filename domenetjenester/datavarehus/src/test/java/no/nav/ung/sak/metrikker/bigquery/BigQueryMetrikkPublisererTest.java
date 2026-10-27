package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingstatus.BehandlingStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.personopplysninger.AlderOgKjønnRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.List;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_BEGYNNELSE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BigQueryMetrikkPublisererTest {

    private static final String TASKTYPE = "bigquery.metrikk.test.task";
    private static final BigQueryTabell<BehandlingStatusRecord> HYPPIG_TABELL = BehandlingStatusRecord.BEHANDLING_STATUS_TABELL;
    private static final BigQueryTabell<AlderOgKjønnRecord> DAGLIG_TABELL = AlderOgKjønnRecord.ALDER_OG_KJØNN_BIG_QUERY_TABELL;

    private final BigQueryKlient bigQueryKlient = mock(BigQueryKlient.class);
    private final ProsessTaskTjeneste prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
    @SuppressWarnings("unchecked")
    private final Instance<YtelseStatistikk> ytelseStatistikk = mock(Instance.class);
    private final YtelseStatistikk ungdomsytelseStatistikk = mock(YtelseStatistikk.class);
    private final YtelseStatistikk aktivitetspengerStatistikk = mock(YtelseStatistikk.class);

    private final List<BehandlingStatusRecord> hyppigeRecords = List.of(mock(BehandlingStatusRecord.class));
    private final List<AlderOgKjønnRecord> dagligeRecords = List.of(mock(AlderOgKjønnRecord.class));

    @BeforeEach
    void setUp() {
        registrer(FagsakYtelseType.UNGDOMSYTELSE, ungdomsytelseStatistikk);
        registrer(FagsakYtelseType.AKTIVITETSPENGER, aktivitetspengerStatistikk);
        when(aktivitetspengerStatistikk.hentHyppigRapporterte(any())).thenReturn(List.of(YtelseStatistikk.metrikk(HYPPIG_TABELL, hyppigeRecords)));
        when(aktivitetspengerStatistikk.hentDagligRapporterte()).thenReturn(List.of(YtelseStatistikk.metrikk(DAGLIG_TABELL, dagligeRecords)));
    }

    @Test
    void skal_hente_hyppige_metrikker_fra_siste_vellykkede_kjøring_av_tasken() {
        var sistKjørt = LocalDateTime.of(2026, 1, 15, 10, 1);
        when(prosessTaskTjeneste.finnAlle(TASKTYPE, ProsessTaskStatus.FERDIG))
            .thenReturn(List.of(kjørtTask(sistKjørt.minusHours(1)), kjørtTask(sistKjørt), kjørtTask(sistKjørt.minusHours(2))));

        publiserer(true).publiserHyppigRapporterte(FagsakYtelseType.AKTIVITETSPENGER, TASKTYPE);

        verify(aktivitetspengerStatistikk).hentHyppigRapporterte(sistKjørt);
        verify(bigQueryKlient).publish(BigQueryDataset.UNG_SAK_STATISTIKK_DATASET, HYPPIG_TABELL, hyppigeRecords);
        verifyNoInteractions(ungdomsytelseStatistikk);
    }

    @Test
    void skal_hente_hyppige_metrikker_fra_tidenes_begynnelse_første_gang_tasken_kjøres() {
        when(prosessTaskTjeneste.finnAlle(TASKTYPE, ProsessTaskStatus.FERDIG)).thenReturn(List.of());

        publiserer(true).publiserHyppigRapporterte(FagsakYtelseType.AKTIVITETSPENGER, TASKTYPE);

        verify(aktivitetspengerStatistikk).hentHyppigRapporterte(TIDENES_BEGYNNELSE.atStartOfDay());
    }

    @Test
    void skal_publisere_daglige_metrikker_kun_for_angitt_ytelse() {
        publiserer(true).publiserDagligRapporterte(FagsakYtelseType.AKTIVITETSPENGER);

        verify(bigQueryKlient).publish(BigQueryDataset.UNG_SAK_STATISTIKK_DATASET, DAGLIG_TABELL, dagligeRecords);
        verifyNoInteractions(ungdomsytelseStatistikk);
    }

    @Test
    void skal_ikke_publisere_når_bigquery_ikke_er_aktivert() {
        publiserer(false).publiserDagligRapporterte(FagsakYtelseType.AKTIVITETSPENGER);

        verifyNoInteractions(bigQueryKlient);
    }

    @Test
    void skal_kaste_feil_videre_slik_at_tasken_prøves_på_nytt() {
        var feil = new IllegalStateException("BigQuery er nede");
        doThrow(feil).when(bigQueryKlient).publish(any(), any(), any());

        assertThatThrownBy(() -> publiserer(true).publiserDagligRapporterte(FagsakYtelseType.AKTIVITETSPENGER)).isSameAs(feil);
    }

    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"OBSOLETE", "UDEFINERT"})
    void skal_avvise_ytelse_som_ikke_er_støttet(FagsakYtelseType ytelse) {
        var publiserer = publiserer(true);

        assertThatThrownBy(() -> publiserer.publiserDagligRapporterte(ytelse)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> publiserer.publiserHyppigRapporterte(ytelse, TASKTYPE)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(bigQueryKlient, ytelseStatistikk);
    }

    private BigQueryMetrikkPubliserer publiserer(boolean bigQueryEnabled) {
        return new BigQueryMetrikkPubliserer(bigQueryEnabled, bigQueryKlient, ytelseStatistikk, prosessTaskTjeneste);
    }

    @SuppressWarnings("unchecked")
    private void registrer(FagsakYtelseType ytelse, YtelseStatistikk statistikk) {
        Instance<YtelseStatistikk> valgt = mock(Instance.class);
        when(valgt.isResolvable()).thenReturn(true);
        when(valgt.get()).thenReturn(statistikk);
        when(ytelseStatistikk.select(new FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral(ytelse))).thenReturn(valgt);
    }

    private static ProsessTaskData kjørtTask(LocalDateTime sistKjørt) {
        var task = new ProsessTaskData(TASKTYPE);
        task.setSistKjørt(sistKjørt);
        return task;
    }
}
