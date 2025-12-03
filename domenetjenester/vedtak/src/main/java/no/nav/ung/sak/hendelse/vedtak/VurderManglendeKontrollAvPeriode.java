package no.nav.ung.sak.hendelse.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.kontroll.ManglendeKontrollperioderTjeneste;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class VurderManglendeKontrollAvPeriode implements VurderOmVedtakPåvirkerSakerTjeneste {

    private ManglendeKontrollperioderTjeneste manglendeKontrollperioderTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;

    public VurderManglendeKontrollAvPeriode() {
    }

    @Inject
    public VurderManglendeKontrollAvPeriode(ManglendeKontrollperioderTjeneste manglendeKontrollperioderTjeneste,
                                            FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository, UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste) {
        this.manglendeKontrollperioderTjeneste = manglendeKontrollperioderTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
    }

    @Override
    public List<SakMedPeriode> utledSakerMedPerioderSomErKanVærePåvirket(Ytelse vedtakHendelse) {
        var saksnummer = new Saksnummer(vedtakHendelse.getSaksnummer());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow();
        Behandling vedtattBehandling = behandlingRepository.hentBehandling(((YtelseV1) vedtakHendelse).getVedtakReferanse());
        if (erFørstegangssøknadEllerSøknadOmNyPeriode(vedtattBehandling) ||
            erEndretOppstartTilTidligereMåned(vedtattBehandling)) {
            var perioderMedManglendeKontroll = manglendeKontrollperioderTjeneste.finnPerioderForManglendeKontroll(vedtattBehandling.getId());
            if (!perioderMedManglendeKontroll.isEmpty()) {
                return List.of(new SakMedPeriode(
                    saksnummer,
                    fagsak.getYtelseType(),
                    perioderMedManglendeKontroll,
                    BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
            }
        }

        return List.of();
    }

    private static boolean erFørstegangssøknadEllerSøknadOmNyPeriode(Behandling vedtattBehandling) {
        return vedtattBehandling.getType() == BehandlingType.FØRSTEGANGSSØKNAD ||
            vedtattBehandling.getBehandlingÅrsakerTyper().contains(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE);
    }

    private boolean erEndretOppstartTilTidligereMåned(Behandling vedtattBehandling) {
        var endretStartdatoerFraOriginal = ungdomsprogramPeriodeTjeneste.finnEndretStartdatoerFraOriginal(BehandlingReferanse.fra(vedtattBehandling));
        boolean harEndringAvStartdatoTilTidligereMåned = endretStartdatoerFraOriginal.stream().anyMatch(it -> erEndretTilTidligereMåned(it.nyDato(), it.forrigeDato()));
        return vedtattBehandling.getBehandlingÅrsakerTyper().contains(BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM) && harEndringAvStartdatoTilTidligereMåned;
    }

    private static boolean erEndretTilTidligereMåned(LocalDate nyVerdi, LocalDate forrigeVerdi) {
        return nyVerdi.getMonth().getValue() < forrigeVerdi.getMonth().getValue() || nyVerdi.getYear() < forrigeVerdi.getYear();
    }
}
