package no.nav.ung.sak.hendelse.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.kontroll.ManglendeKontrollperioderTjeneste;
import no.nav.ung.sak.typer.Saksnummer;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class VurderManglendeKontrollAvPeriode implements VurderOmVedtakPåvirkerSakerTjeneste {

    private ManglendeKontrollperioderTjeneste manglendeKontrollperioderTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    public VurderManglendeKontrollAvPeriode() {
    }

    @Inject
    public VurderManglendeKontrollAvPeriode(ManglendeKontrollperioderTjeneste manglendeKontrollperioderTjeneste,
                                            FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository,
                                            UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.manglendeKontrollperioderTjeneste = manglendeKontrollperioderTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
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
            vedtattBehandling.getBehandlingÅrsakerTyper().contains(BehandlingÅrsakType.NY_SØKT_PERIODE);
    }

    private boolean erEndretOppstartTilTidligereMåned(Behandling vedtattBehandling) {
        if (!vedtattBehandling.getBehandlingÅrsakerTyper().contains(BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM)) {
            return false;
        }
        var gjeldendeStartdato = ungdomsprogramPeriodeRepository.hentGrunnlag(vedtattBehandling.getId())
            .flatMap(UngdomsprogramPeriodeGrunnlag::hentForEksaktEnPeriodeDersomFinnes)
            .map(p -> p.getFomDato())
            .orElse(null);
        var originalStartdato = vedtattBehandling.getOriginalBehandlingId()
            .flatMap(ungdomsprogramPeriodeRepository::hentGrunnlag)
            .flatMap(UngdomsprogramPeriodeGrunnlag::hentForEksaktEnPeriodeDersomFinnes)
            .map(p -> p.getFomDato())
            .orElse(null);
        if (gjeldendeStartdato == null || originalStartdato == null || gjeldendeStartdato.equals(originalStartdato)) {
            return false;
        }
        return erEndretTilTidligereMåned(gjeldendeStartdato, originalStartdato);
    }

    private static boolean erEndretTilTidligereMåned(LocalDate nyVerdi, LocalDate forrigeVerdi) {
        return nyVerdi.getMonth().getValue() < forrigeVerdi.getMonth().getValue() || nyVerdi.getYear() < forrigeVerdi.getYear();
    }
}

