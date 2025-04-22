package no.nav.ung.sak.hendelsemottak.tjenester;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.typer.AktørId;

@ApplicationScoped
@HendelseTypeRef("UNG_OPPHØR")
public class UngdomsprogramOpphørFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {

    private static final Logger logger = LoggerFactory.getLogger(UngdomsprogramOpphørFagsakTilVurderingUtleder.class);
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste;

    public UngdomsprogramOpphørFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public UngdomsprogramOpphørFagsakTilVurderingUtleder(BehandlingRepository behandlingRepository,
                                                         UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
                                                         FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.finnFagsakerForAktørTjeneste = finnFagsakerForAktørTjeneste;
    }

    @Override
    public Map<Fagsak, ÅrsakOgPeriode> finnFagsakerTilVurdering(Hendelse hendelse) {
        List<AktørId> aktører = hendelse.getHendelseInfo().getAktørIder();
        LocalDate opphørsdatoFraHendelse = hendelse.getHendelsePeriode().getFom();
        String hendelseId = hendelse.getHendelseInfo().getHendelseId();

        var fagsaker = new HashMap<Fagsak, ÅrsakOgPeriode>();

        for (AktørId aktør : aktører) {
            var relevantFagsak = finnFagsakerForAktørTjeneste.hentRelevantFagsakForAktørSomSøker(aktør, opphørsdatoFraHendelse);
            if (relevantFagsak.isEmpty()) {
                continue;
            }
            // Kan også vurdere om vi skal legge inn sjekk på om bruker har utbetaling etter opphørsdato
            if (erNyInformasjonIHendelsen(relevantFagsak.get(), opphørsdatoFraHendelse, hendelseId)) {
                fagsaker.put(relevantFagsak.get(), new ÅrsakOgPeriode(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, utledPeriode(relevantFagsak.get(), opphørsdatoFraHendelse)));
            }
        }


        return fagsaker;
    }

    private DatoIntervallEntitet utledPeriode(Fagsak fagsak, LocalDate nyTomdato) {
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow();

        final var tidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId());

        if (tidslinje.isEmpty()) {
            logger.info("Fant ikke ungdomsprogramperiodegrunnlag for behandling med id " + behandling.getId());
            return fagsak.getPeriode();
        }


        final var perioder = tidslinje.toSegments();

        if (perioder.size() > 1) {
            throw new IllegalStateException("Støtter ikke endring av periode for mer enn en periode");
        } else if (perioder.isEmpty()) {
            logger.info("Fant ikke ungdomsprogramperiodegrunnlag for behandling med id " + behandling.getId());
            return fagsak.getPeriode();
        }

        final var gammelTomDato = tidslinje.getMaxLocalDate().isAfter(fagsak.getPeriode().getTomDato()) ? fagsak.getPeriode().getTomDato() : tidslinje.getMaxLocalDate();
        return gammelTomDato.isBefore(nyTomdato) ? DatoIntervallEntitet.fraOgMedTilOgMed(gammelTomDato.plusDays(1), nyTomdato) : DatoIntervallEntitet.fraOgMedTilOgMed(nyTomdato.plusDays(1), gammelTomDato);
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
        final var ungdomsprogramTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId());
        if (!ungdomsprogramTidslinje.isEmpty()) {
            var erSisteDatoAlleredeSattTilOpphørsdato = ungdomsprogramTidslinje.getMaxLocalDate().equals(opphørsdato);
            if (erSisteDatoAlleredeSattTilOpphørsdato) {
                logger.info("Datagrunnlag på behandling {} for {} hadde ingen perioder med ungdomsprogram etter opphørsdato. Trigget av hendelse {}.", behandling.getUuid(), fagsak.getSaksnummer(), hendelseId);
                return false;
            }
        }
        return true;
    }

}
