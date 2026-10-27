package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.kontroll.KontrollerteInntektperioderTjeneste;
import no.nav.ung.sak.metrikker.bigquery.tabeller.inntektskontroll.KontrollertePerioderRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@ApplicationScoped
public class KontrollerteInntektPerioderMetrikkPubliserer {

    private static final Logger log = LoggerFactory.getLogger(KontrollerteInntektPerioderMetrikkPubliserer.class);

    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;
    private BehandlingVedtakRepository vedtakRepository;
    private BigQueryKlient bigQueryKlient;
    private Instance<YtelseStatistikk> ytelseStatistikk;
    private boolean bigQueryEnabled;


    public KontrollerteInntektPerioderMetrikkPubliserer() {
    }

    @Inject
    public KontrollerteInntektPerioderMetrikkPubliserer(
        KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste, BehandlingVedtakRepository vedtakRepository, BigQueryKlient bigQueryKlient,
        @Any Instance<YtelseStatistikk> ytelseStatistikk,
        @KonfigVerdi(value = "BIGQUERY_ENABLED", required = false, defaultVerdi = "false") boolean bigQueryEnabled
    ) {
        this.kontrollerteInntektperioderTjeneste = kontrollerteInntektperioderTjeneste;
        this.vedtakRepository = vedtakRepository;
        this.bigQueryKlient = bigQueryKlient;
        this.ytelseStatistikk = ytelseStatistikk;
        this.bigQueryEnabled = bigQueryEnabled;
    }

    public void publiserKontrollertePerioderMetrikker(BehandlingReferanse behandlingReferanse) {
        if (!YtelseStatistikk.erStøttet(behandlingReferanse.getFagsakYtelseType())) {
            // Utgåtte fagsaker (OBSOLETE) skal ikke med i statistikken
            log.info("Publiserer ikke kontrollerte inntektsperioder for sak {} med ytelse {}", behandlingReferanse.getSaksnummer(), behandlingReferanse.getFagsakYtelseType());
            return;
        }
        List<KontrollertInntektPeriode> kontrollertInntektPerioder = kontrollerteInntektperioderTjeneste.finnPerioderKontrollertIBehandling(behandlingReferanse.getBehandlingId());
        var behandlingVedtak = vedtakRepository.hentBehandlingVedtakForBehandlingId(behandlingReferanse.getBehandlingId()).orElseThrow(() -> new IllegalStateException("Forventer å finne behandlingVedtak"));
        List<KontrollertePerioderRecord> records = kontrollertInntektPerioder.stream().map(it -> new KontrollertePerioderRecord(
            behandlingReferanse.getSaksnummer(),
            it.getRapportertInntekt(),
            it.getRegisterInntekt(),
            it.getInntekt(),
            it.getErManueltVurdert(),
            YearMonth.of(it.getPeriode().getFomDato().getYear(), it.getPeriode().getFomDato().getMonth()),
            it.getPeriode().getFomDato(),
            it.getPeriode().getTomDato(),
            behandlingVedtak.getVedtakstidspunkt().atZone(ZoneId.systemDefault())
        )).toList();

        if (bigQueryEnabled && !records.isEmpty()) {
            var tabell = YtelseStatistikk.finn(ytelseStatistikk, behandlingReferanse.getFagsakYtelseType()).kontrollertePerioderTabell();
            bigQueryKlient.publish(BigQueryDataset.UNG_SAK_STATISTIKK_DATASET, tabell, records);
        }
    }

}
