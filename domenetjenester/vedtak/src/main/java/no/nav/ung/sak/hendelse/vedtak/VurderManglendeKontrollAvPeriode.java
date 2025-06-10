package no.nav.ung.sak.hendelse.vedtak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.ytelse.kontroll.ManglendeKontrollperioderTjeneste;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Dependent
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public class VurderManglendeKontrollAvPeriode implements VurderOmVedtakPåvirkerSakerTjeneste {

    private ManglendeKontrollperioderTjeneste manglendeKontrollperioderTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public VurderManglendeKontrollAvPeriode(ManglendeKontrollperioderTjeneste manglendeKontrollperioderTjeneste, FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository) {
        this.manglendeKontrollperioderTjeneste = manglendeKontrollperioderTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public List<SakMedPeriode> utledSakerMedPerioderSomErKanVærePåvirket(Ytelse vedtakHendelse) {
        var saksnummer = new Saksnummer(vedtakHendelse.getSaksnummer());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow();
        Behandling vedtattBehandling = behandlingRepository.hentBehandling(((YtelseV1) vedtakHendelse).getVedtakReferanse());
        var perioderMedManglendeKontroll = manglendeKontrollperioderTjeneste.finnPerioderForManglendeKontroll(vedtattBehandling.getId());

        if (!perioderMedManglendeKontroll.isEmpty()) {
            return List.of(new SakMedPeriode(
                saksnummer,
                fagsak.getYtelseType(),
                perioderMedManglendeKontroll.stream().map(DatoIntervallEntitet::fra).collect(Collectors.toCollection(TreeSet::new)),
                BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
        }

        return List.of();
    }
}
