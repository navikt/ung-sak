package no.nav.ung.sak.hendelsemottak.tjenester;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.typer.AktørId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@HendelseTypeRef("UNGDOMSPROGRAM_ENDRET_STARTDATO")
public class UngdomsprogramEndretStartdatoFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {

    private static final Logger logger = LoggerFactory.getLogger(UngdomsprogramEndretStartdatoFagsakTilVurderingUtleder.class);
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste;

    public UngdomsprogramEndretStartdatoFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public UngdomsprogramEndretStartdatoFagsakTilVurderingUtleder(BehandlingRepository behandlingRepository,
                                                                  UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                                                  FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.finnFagsakerForAktørTjeneste = finnFagsakerForAktørTjeneste;
    }

    @Override
    public Map<Fagsak, BehandlingÅrsakType> finnFagsakerTilVurdering(Hendelse hendelse) {
        List<AktørId> aktører = hendelse.getHendelseInfo().getAktørIder();
        LocalDate nyFomdato = hendelse.getHendelsePeriode().getFom();
        String hendelseId = hendelse.getHendelseInfo().getHendelseId();

        var fagsaker = new HashMap<Fagsak, BehandlingÅrsakType>();

        for (AktørId aktør : aktører) {
            var relevantFagsak = finnFagsakerForAktørTjeneste.hentRelevantFagsakForAktørSomSøker(aktør, nyFomdato);
            if (relevantFagsak.isEmpty()) {
                continue;
            }
            // Kan også vurdere om vi skal legge inn sjekk på om bruker har utbetaling etter opphørsdato
            if (erNyInformasjonIHendelsen(relevantFagsak.get(), nyFomdato, hendelseId)) {
                fagsaker.put(relevantFagsak.get(), BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM);
            }
        }


        return fagsaker;
    }

    /**
     * idempotens-sjekk for å hindre at det opprettes flere revurderinger fra samme hendelse.
     * hindrer også revurdering hvis hendelsen kommer etter at behandlingen er oppdatert med ny data.
     */
    private boolean erNyInformasjonIHendelsen(Fagsak fagsak, LocalDate nyFomDato, String hendelseId) {
        Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandlingOpt.isEmpty()) {
            logger.info("Det er ingen behandling på fagsak. Ignorer hendelse");
            return false;
        }

        Behandling behandling = behandlingOpt.get();
        if (behandling.getBehandlingÅrsakerTyper().contains(BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM)) {

            final var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId());
            final var harPeriodeMedNyFomDato = ungdomsprogramPeriodeGrunnlag.stream()
                .flatMap(it -> it.getUngdomsprogramPerioder().getPerioder().stream())
                .anyMatch(it -> it.getPeriode().getFomDato().equals(nyFomDato));

            if (!harPeriodeMedNyFomDato) {
                logger.info("Behandling har allerede behandlingsårsak for hendelse og grunnlagsdata er oppdatert. Ignorer hendelse " + hendelseId);
                return false;
            };
        }
        return true;
    }

}
