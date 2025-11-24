package no.nav.ung.sak.hendelsemottak.tjenester;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.revurdering.ÅrsakOgPerioder;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@HendelseTypeRef("UNGDOMSPROGRAM_FJERN_PERIODE")
public class UngdomsprogramFjernDeltakelseFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {

    private static final Logger logger = LoggerFactory.getLogger(UngdomsprogramFjernDeltakelseFagsakTilVurderingUtleder.class);
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste;

    public UngdomsprogramFjernDeltakelseFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public UngdomsprogramFjernDeltakelseFagsakTilVurderingUtleder(BehandlingRepository behandlingRepository,
                                                                  UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                                                  FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.finnFagsakerForAktørTjeneste = finnFagsakerForAktørTjeneste;
    }

    @Override
    public Map<Fagsak, List<ÅrsakOgPerioder>> finnFagsakerTilVurdering(Hendelse hendelse) {
        List<AktørId> aktører = hendelse.getHendelseInfo().getAktørIder();
        Periode fjernetPeriode = hendelse.getHendelsePeriode();
        String hendelseId = hendelse.getHendelseInfo().getHendelseId();

        var fagsaker = new HashMap<Fagsak, List<ÅrsakOgPerioder>>();

        for (AktørId aktør : aktører) {
            var relevantFagsak = finnFagsakerForAktørTjeneste.hentRelevantFagsakForAktørSomSøker(aktør, fjernetPeriode.getFom());
            if (relevantFagsak.isEmpty()) {
                continue;
            }
            // Kan også vurdere om vi skal legge inn sjekk på om bruker har utbetaling etter opphørsdato
            if (erNyInformasjonIHendelsen(relevantFagsak.get(), fjernetPeriode, hendelseId)) {
                fagsaker.put(relevantFagsak.get(), List.of(new ÅrsakOgPerioder(
                    BehandlingÅrsakType.RE_HENDELSE_FJERN_PERIODE_UNGDOMSPROGRAM,
                    DatoIntervallEntitet.fra(fjernetPeriode)))
                );
            }
        }
        return fagsaker;
    }

    /**
     * idempotens-sjekk for å hindre at det opprettes flere revurderinger fra samme hendelse.
     * hindrer også revurdering hvis hendelsen kommer etter at behandlingen er oppdatert med ny data.
     */
    private boolean erNyInformasjonIHendelsen(Fagsak fagsak, Periode fjernetPeriode, String hendelseId) {
        Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandlingOpt.isEmpty()) {
            logger.info("Det er ingen behandling på fagsak. Ignorer hendelse");
            return false;
        }

        Behandling behandling = behandlingOpt.get();
        if (behandling.getBehandlingÅrsakerTyper().contains(BehandlingÅrsakType.RE_HENDELSE_FJERN_PERIODE_UNGDOMSPROGRAM)) {

            final var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId());
            final var fjernetPeriodeFinnesIGrunnlag = ungdomsprogramPeriodeGrunnlag.stream()
                .flatMap(it -> it.getUngdomsprogramPerioder().getPerioder().stream())
                .noneMatch(it -> it.getPeriode().overlapper(DatoIntervallEntitet.fra(fjernetPeriode)));

            if (fjernetPeriodeFinnesIGrunnlag) {
                logger.info("Behandling har allerede behandlingsårsak for hendelse og grunnlagsdata er oppdatert. Ignorer hendelse " + hendelseId);
                return false;
            }
        }
        return true;
    }

}
