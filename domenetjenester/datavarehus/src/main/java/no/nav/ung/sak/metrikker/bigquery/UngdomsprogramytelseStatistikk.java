package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.util.Tuple;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.aksjonspunkt.AksjonspunktRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsresultat.BehandslingsresultatStatistikkRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingstatus.BehandlingStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsårsak.BehandlingÅrsakRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.etterlysning.EtterlysningRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.fagsakstatus.FagsakStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.personopplysninger.AlderOgKjønnRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.sats.SatsStatistikkRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.ungdomsprogram.DagerIProgrammetRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.uttalelse.UttalelseRecord;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static no.nav.ung.sak.metrikker.bigquery.YtelseStatistikk.metrikk;

/**
 * Metrikker for ungdomsprogramytelsen. Publiseres til de opprinnelige tabellene i BigQuery.
 */
@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public class UngdomsprogramytelseStatistikk implements YtelseStatistikk {

    private static final FagsakYtelseType YTELSE = FagsakYtelseType.UNGDOMSYTELSE;

    private BigQueryStatistikkRepository statistikkRepository;
    private AntallDagerStatistikk antallDagerStatistikk;

    UngdomsprogramytelseStatistikk() {
        // for CDI proxy
    }

    @Inject
    public UngdomsprogramytelseStatistikk(BigQueryStatistikkRepository statistikkRepository, AntallDagerStatistikk antallDagerStatistikk) {
        this.statistikkRepository = statistikkRepository;
        this.antallDagerStatistikk = antallDagerStatistikk;
    }

    @Override
    public List<Tuple<BigQueryTabell<?>, Collection<?>>> hentHyppigRapporterte(LocalDateTime sistKjørtTidspunkt) {
        return List.of(
            metrikk(FagsakStatusRecord.FAGSAK_STATUS_TABELLER.ungdomsytelse(), statistikkRepository.fagsakStatusStatistikk(YTELSE)),
            metrikk(BehandlingStatusRecord.BEHANDLING_STATUS_TABELLER.ungdomsytelse(), statistikkRepository.behandlingStatusStatistikk(YTELSE)),
            metrikk(AksjonspunktRecord.AKSJONSPUNKT_TABELLER.ungdomsytelse(), statistikkRepository.aksjonspunktStatistikk(YTELSE)),
            metrikk(SatsStatistikkRecord.SATS_STATISTIKK_TABELLER.ungdomsytelse(), statistikkRepository.satsSatistikk(YTELSE)),
            metrikk(BehandslingsresultatStatistikkRecord.BEHANDLINGSRESULTAT_STATISTIKK_TABELLER.ungdomsytelse(), statistikkRepository.behandlingResultatStatistikk(YTELSE)),
            metrikk(EtterlysningRecord.ETTERLYSNING_TABELLER.ungdomsytelse(), statistikkRepository.etterlysningData(YTELSE, sistKjørtTidspunkt)),
            metrikk(UttalelseRecord.UTTALELSE_TABELLER.ungdomsytelse(), statistikkRepository.uttalelseData(YTELSE, sistKjørtTidspunkt, null)),
            metrikk(BehandlingÅrsakRecord.BEHANDLING_ÅRSAK_TABELLER.ungdomsytelse(), statistikkRepository.behandlingÅrsakStatistikk(YTELSE))
        );
    }

    @Override
    public List<Tuple<BigQueryTabell<?>, Collection<?>>> hentDagligRapporterte() {
        return List.of(
            metrikk(AlderOgKjønnRecord.ALDER_OG_KJØNN_TABELLER.ungdomsytelse(), statistikkRepository.alderOgKjønnStatistikk(YTELSE)),
            metrikk(DagerIProgrammetRecord.DAGER_I_PROGRAMMET_LØPENDE_BIG_QUERY_TABELL, antallDagerStatistikk.dagerIProgrammet())
        );
    }
}
