package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.kontroll.KontrollerteInntektperioderTjeneste;
import no.nav.ung.sak.metrikker.bigquery.tabeller.inntektskontroll.KontrollertePerioderRecord;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@ApplicationScoped
public class KontrollerteInntektPerioderMetrikkPubliserer {

    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;
    private BehandlingVedtakRepository vedtakRepository;
    private BigQueryKlient bigQueryKlient;
    private boolean bigQueryEnabled;


    public KontrollerteInntektPerioderMetrikkPubliserer() {
    }

    @Inject
    public KontrollerteInntektPerioderMetrikkPubliserer(
        KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste, BehandlingVedtakRepository vedtakRepository, BigQueryKlient bigQueryKlient,
        @KonfigVerdi(value = "BIGQUERY_ENABLED", required = false, defaultVerdi = "false") boolean bigQueryEnabled
    ) {
        this.kontrollerteInntektperioderTjeneste = kontrollerteInntektperioderTjeneste;
        this.vedtakRepository = vedtakRepository;
        this.bigQueryKlient = bigQueryKlient;
        this.bigQueryEnabled = bigQueryEnabled;
    }

    public void publiserKontrollertePerioderMetrikker(BehandlingReferanse behandlingReferanse) {
        List<KontrollertInntektPeriode> kontrollertInntektPerioder = kontrollerteInntektperioderTjeneste.finnPerioderKontrollertIBehandling(behandlingReferanse.getBehandlingId());
        var behandlingVedtak = vedtakRepository.hentBehandlingVedtakForBehandlingId(behandlingReferanse.getBehandlingId()).orElseThrow(() -> new IllegalStateException("Forventer å finne behandlingVedtak"));
        List<KontrollertePerioderRecord> records = kontrollertInntektPerioder.stream().map(it -> new KontrollertePerioderRecord(
            behandlingReferanse.getSaksnummer(),
            it.getRapportertInntekt(),
            it.getRegisterInntekt(),
            it.getInntekt(),
            it.getErManueltVurdert(),
            YearMonth.of(it.getPeriode().getFomDato().getYear(), it.getPeriode().getFomDato().getMonth()),
            behandlingVedtak.getVedtakstidspunkt().atZone(ZoneId.systemDefault())
        )).toList();

        if (bigQueryEnabled && !records.isEmpty()) {
            bigQueryKlient.publish(BigQueryDataset.UNG_SAK_STATISTIKK_DATASET, KontrollertePerioderRecord.KONTROLLERTE_PERIODER_TABELL, records);
        }
    }

}
