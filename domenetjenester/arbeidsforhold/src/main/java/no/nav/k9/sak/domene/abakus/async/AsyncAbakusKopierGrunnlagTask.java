package no.nav.k9.sak.domene.abakus.async;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.request.Dataset;
import no.nav.abakus.iaygrunnlag.request.KopierGrunnlagRequest;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.k9.sak.domene.abakus.AbakusTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(AsyncAbakusKopierGrunnlagTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
class AsyncAbakusKopierGrunnlagTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "abakus.async.kopiergrunnlag";

    /** Angir hvilken behandling det skal kopieres fra. */
    static final String ORIGINAL_BEHANDLING_ID = "original.behandlingId";

    static final String DATASET = "kopiergrunnlag.dataset";

    private AbakusTjeneste abakusTjeneste;

    private BehandlingRepository behandlingRepository;

    AsyncAbakusKopierGrunnlagTask() {
        // for proxy
    }

    @Inject
    AsyncAbakusKopierGrunnlagTask(BehandlingRepository behandlingRepository, BehandlingLåsRepository behandlingLåsRepository, AbakusTjeneste abakusTjeneste) {
        super(behandlingRepository, behandlingLåsRepository);
        this.behandlingRepository = behandlingRepository;
        this.abakusTjeneste = abakusTjeneste;
    }

    @Override
    protected void doProsesser(ProsessTaskData input, Behandling tilBehandling) {

        String fraBehandlingId = Objects.requireNonNull(input.getPropertyValue(ORIGINAL_BEHANDLING_ID), ORIGINAL_BEHANDLING_ID);
        var fraBehandling = behandlingRepository.hentBehandling(fraBehandlingId);
        Set<Dataset> dataset = getDataset(input.getPropertyValue(DATASET));

        preconditions(fraBehandling, tilBehandling);

        var request = new KopierGrunnlagRequest(tilBehandling.getFagsak().getSaksnummer().getVerdi(),
            tilBehandling.getUuid(),
            fraBehandling.getUuid(),
            YtelseType.fraKode(tilBehandling.getFagsakYtelseType().getKode()),
            new AktørIdPersonident(tilBehandling.getAktørId().getId()),
            dataset);
        try {
            abakusTjeneste.kopierGrunnlag(request);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Kunne ikke kopiere abakus grunnlag: fra [%s] til [%s], dataset: %s", fraBehandlingId, tilBehandling.getId()), e);
        }
    }

    private static Set<Dataset> getDataset(String datasetStr) {
        if (datasetStr != null && !(datasetStr = datasetStr.trim()).isEmpty()) {
            return Arrays.asList(datasetStr.split(",\\s*")).stream().map(Dataset::valueOf).collect(Collectors.toSet());
        } else {
            return EnumSet.allOf(Dataset.class);
        }
    }

    public static void preconditions(Behandling originalBehandling, Behandling behandling) {
        if (!Objects.equals(behandling.getFagsakId(), originalBehandling.getFagsakId())) {
            throw new IllegalArgumentException("Behandling må høre til samme fagsak: [" + originalBehandling.getFagsakId() + "] vs [" + behandling.getFagsakId() + "]");
        } else if (!originalBehandling.erAvsluttet()) {
            throw new IllegalArgumentException("Kan ikke kopiere fra behandling som ikke er avsluttet: " + originalBehandling);
        } else if (behandling.erSaksbehandlingAvsluttet()) {
            throw new IllegalArgumentException("Kan ikke kopiere til behandling som ikke er åpen: " + originalBehandling);
        }
    }

}
