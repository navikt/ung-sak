package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import java.time.LocalDate;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningSatsType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.util.Tuple;

/**
 * Batchservice som finner alle behandlinger som skal gjenopptas, og lager en ditto prosess task for hver.
 * Kriterier for gjenopptagelse: Behandlingen har et åpent aksjonspunkt som er et autopunkt og
 * har en frist som er passert.
 */
@ApplicationScoped
@ProsessTask(AutomatiskGrunnbelopReguleringBatchTask.TASKTYPE)
public class AutomatiskGrunnbelopReguleringBatchTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "batch.grunnbelopRegulering";
    public static final String KEY_DRY_RUN = "dryRun";
    private static final Logger log = LoggerFactory.getLogger(AutomatiskGrunnbelopReguleringBatchTask.class);
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    @Inject
    public AutomatiskGrunnbelopReguleringBatchTask(BehandlingRevurderingRepository behandlingRevurderingRepository,
                                                   BeregningsresultatRepository beregningsresultatRepository,
                                                   ProsessTaskRepository prosessTaskRepository) {
        this.behandlingRevurderingRepository = behandlingRevurderingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final var dryRunProperty = prosessTaskData.getPropertyValue(KEY_DRY_RUN);
        final boolean dryRun = dryRunProperty != null && Boolean.parseBoolean(dryRunProperty);

        BeregningSats gjeldende = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now());
        BeregningSats forrige = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, gjeldende.getPeriode().getFomDato().minusDays(1));
        long avkortingAntallG = beregningsresultatRepository.avkortingMultiplikatorG(gjeldende.getPeriode().getFomDato().minusDays(1));
        List<Tuple<Long, AktørId>> tilVurdering = behandlingRevurderingRepository.finnSakerMedBehovForGrunnbeløpRegulering(gjeldende.getVerdi(), forrige.getVerdi(), avkortingAntallG, gjeldende.getPeriode().getFomDato());
        if (dryRun) {
            tilVurdering.forEach(sak -> log.info("[DRYRUN] Skal revurdere sak {} for aktør {}", sak.getElement1(), sak.getElement2()));
        } else {
            tilVurdering.forEach(sak -> opprettReguleringTask(sak.getElement1(), sak.getElement2()));
        }
        log.info("Fant {} saker til revurdering", tilVurdering.size());
    }

    private void opprettReguleringTask(Long fagsakId, AktørId aktørId) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskGrunnbelopReguleringTask.TASKTYPE);
        prosessTaskData.setFagsak(fagsakId, aktørId.getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }
}
