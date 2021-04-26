package no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.hendelse.vedtak.VurderOmVedtakPåvirkerSakerTjeneste;
import no.nav.k9.sak.kontrakt.vedtak.VedtakHendelse;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class VurderOmPSBVedtakPåvirkerAndreSakerTjeneste implements VurderOmVedtakPåvirkerSakerTjeneste {

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomVurderingRepository sykdomVurderingRepository;
    private SykdomVurderingService sykdomVurderingService;

    VurderOmPSBVedtakPåvirkerAndreSakerTjeneste() {
    }

    @Inject
    public VurderOmPSBVedtakPåvirkerAndreSakerTjeneste(BehandlingRepository behandlingRepository,
                                                       FagsakRepository fagsakRepository,
                                                       SykdomGrunnlagRepository sykdomGrunnlagRepository,
                                                       SykdomVurderingRepository sykdomVurderingRepository,
                                                       SykdomVurderingService sykdomVurderingService) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
        this.sykdomVurderingService = sykdomVurderingService;
    }

    @Override
    public List<Saksnummer> utledSakerSomErKanVærePåvirket(Ytelse vedtakHendelse) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(vedtakHendelse.getSaksnummer())).orElseThrow();
        Behandling vedtattBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow();
        SykdomGrunnlagBehandling vedtattSykdomGrunnlagBehandling = sykdomGrunnlagRepository.hentGrunnlagForBehandling(vedtattBehandling.getUuid()).orElseThrow();

        AktørId pleietrengende = vedtattBehandling.getFagsak().getPleietrengendeAktørId();
        List<Saksnummer> alleSaksnummer = sykdomVurderingRepository.hentAlleSaksnummer(pleietrengende);

        var result = new ArrayList<Saksnummer>();
        for (Saksnummer kandidatsaksnummer : alleSaksnummer) {
            if (!kandidatsaksnummer.equals(vedtattSykdomGrunnlagBehandling.getSaksnummer())) {
                SykdomGrunnlagBehandling kandidatSykdomBehandling = sykdomGrunnlagRepository.hentSisteBehandling(kandidatsaksnummer)
                    .flatMap(uuid -> sykdomGrunnlagRepository.hentGrunnlagForBehandling(uuid))
                    .orElseThrow();

                final LocalDateTimeline<Boolean> endringerISøktePerioder = sykdomVurderingService.utledRelevanteEndringerSidenForrigeBehandling(
                    kandidatsaksnummer, kandidatSykdomBehandling.getBehandlingUuid(), pleietrengende, List.of()).getDiffPerioder();

                if (!endringerISøktePerioder.isEmpty()) {
                    result.add(kandidatsaksnummer);
                }
            }
        }

        return result;
    }
}
