package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.metrikker.bigquery.tabeller.inntektskontroll.KontrollertePerioderRecord;
import no.nav.ung.sak.ytelse.kontroll.KontrollerteInntektperioderTjeneste;

import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.List;

@ApplicationScoped
public class KontrollerteInntektPerioderMetrikkPubliserer {

    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;
    private BigQueryKlient bigQueryKlient;
    private boolean bigQueryEnabled;


    public KontrollerteInntektPerioderMetrikkPubliserer() {
    }

    @Inject
    public KontrollerteInntektPerioderMetrikkPubliserer(
        KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste, BigQueryKlient bigQueryKlient,
        @KonfigVerdi(value = "BIGQUERY_ENABLED", required = false, defaultVerdi = "false") boolean bigQueryEnabled
    ) {
        this.kontrollerteInntektperioderTjeneste = kontrollerteInntektperioderTjeneste;
        this.bigQueryKlient = bigQueryKlient;
        this.bigQueryEnabled = bigQueryEnabled;
    }
    public void publiserKontrollertePerioderMetrikker(BehandlingReferanse behandlingReferanse) {
        List<KontrollertInntektPeriode> kontrollertInntektPerioder = kontrollerteInntektperioderTjeneste.finnPerioderKontrollertIBehandling(behandlingReferanse.getBehandlingId());
        List<KontrollertePerioderRecord> records = kontrollertInntektPerioder.stream().map(it -> new KontrollertePerioderRecord(
            behandlingReferanse.getSaksnummer(),
            it.getRapportertInntekt(),
            it.getRegisterInntekt(),
            it.getInntekt(),
            it.getErManueltVurdert(),
            YearMonth.of(it.getPeriode().getFomDato().getYear(), it.getPeriode().getFomDato().getMonth()),
            ZonedDateTime.now()
        )).toList();

        if (bigQueryEnabled && !records.isEmpty()) {
            bigQueryKlient.publish(BigQueryDataset.UNG_SAK_STATISTIKK_DATASET, KontrollertePerioderRecord.KONTROLLERTE_PERIODER_TABELL, records);
        }
    }

}
