package no.nav.k9.sak.ytelse.ung.hendelsemottak;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval;
import no.nav.k9.sak.hendelsemottak.tjenester.FagsakerTilVurderingUtleder;
import no.nav.k9.sak.hendelsemottak.tjenester.HendelseTypeRef;
import no.nav.k9.sak.kontrakt.hendelser.Hendelse;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.ung.periode.UngdomsprogramPeriodeRepository;

@ApplicationScoped
@HendelseTypeRef("UNG_OPPHØR")
public class UngdomsprogramOpphørFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {

    private static final Logger logger = LoggerFactory.getLogger(UngdomsprogramOpphørFagsakTilVurderingUtleder.class);
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    public UngdomsprogramOpphørFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public UngdomsprogramOpphørFagsakTilVurderingUtleder(FagsakRepository fagsakRepository,
                                                         BehandlingRepository behandlingRepository,
                                                         UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    @Override
    public Map<Fagsak, BehandlingÅrsakType> finnFagsakerTilVurdering(Hendelse hendelse) {
        List<AktørId> aktører = hendelse.getHendelseInfo().getAktørIder();
        LocalDate opphørsdatoFraHendelse = hendelse.getHendelsePeriode().getFom();
        String hendelseId = hendelse.getHendelseInfo().getHendelseId();

        var fagsaker = new HashMap<Fagsak, BehandlingÅrsakType>();

        for (AktørId aktør : aktører) {
            var relevantFagsak = fagsakRepository.hentForBruker(aktør).stream()
                .filter(f -> f.getYtelseType().equals(FagsakYtelseType.UNGDOMSYTELSE))
                .filter(f -> f.getPeriode().overlapper(opphørsdatoFraHendelse, AbstractLocalDateInterval.TIDENES_ENDE)).findFirst();
            if (relevantFagsak.isEmpty()) {
                continue;
            }
            // Kan også vurdere om vi skal legge inn sjekk på om bruker har utbetaling etter opphørsdato
            if (erNyInformasjonIHendelsen(relevantFagsak.get(), opphørsdatoFraHendelse, hendelseId)) {
                fagsaker.put(relevantFagsak.get(), BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
            }
        }


        return fagsaker;
    }

    /**
     * idempotens-sjekk for å hindre at det opprettes flere revurderinger fra samme hendelse.
     * hindrer også revurdering hvis hendelsen kommer etter at behandlingen er oppdatert med ny data.
     */
    private boolean erNyInformasjonIHendelsen(Fagsak fagsak, LocalDate opphørsdato, String hendelseId) {
        Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandlingOpt.isEmpty()) {
            logger.info("Det er ingen behandling på fagsak. Ignorer hendelse");
            return false;
        }

        Behandling behandling = behandlingOpt.get();
        var periodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId());
        if (periodeGrunnlag.isPresent()) {
            var harIngenPerioderEtterOpphør = periodeGrunnlag.get().getUngdomsprogramPerioder().getPerioder().stream().noneMatch(p -> p.getPeriode().getTomDato().isAfter(opphørsdato));
            if (harIngenPerioderEtterOpphør) {
                logger.info("Datagrunnlag på behandling {} for {} hadde ingen perioder med ungdomsprogram etter opphørsdato. Trigget av hendelse {}.", behandling.getUuid(), fagsak.getSaksnummer(), hendelseId);
                return false;
            }
        }
        return true;
    }

}
