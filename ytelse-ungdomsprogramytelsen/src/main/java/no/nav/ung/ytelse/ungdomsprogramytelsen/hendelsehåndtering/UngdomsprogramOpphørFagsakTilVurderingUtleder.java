package no.nav.ung.ytelse.ungdomsprogramytelsen.hendelsehåndtering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.revurdering.ÅrsakOgPerioder;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.hendelsemottak.tjenester.FagsakerTilVurderingUtleder;
import no.nav.ung.sak.hendelsemottak.tjenester.FinnFagsakerForAktørTjeneste;
import no.nav.ung.sak.hendelsemottak.tjenester.HendelseTypeRef;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramRegisterKlient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;

@ApplicationScoped
@HendelseTypeRef("UNG_OPPHØR")
public class UngdomsprogramOpphørFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {

    private static final Logger logger = LoggerFactory.getLogger(UngdomsprogramOpphørFagsakTilVurderingUtleder.class);
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste;
    private UngdomsprogramRegisterKlient ungdomsprogramRegisterKlient;

    public UngdomsprogramOpphørFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public UngdomsprogramOpphørFagsakTilVurderingUtleder(BehandlingRepository behandlingRepository,
                                                         UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
                                                         FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste,
                                                         UngdomsprogramRegisterKlient ungdomsprogramRegisterKlient) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.finnFagsakerForAktørTjeneste = finnFagsakerForAktørTjeneste;
        this.ungdomsprogramRegisterKlient = ungdomsprogramRegisterKlient;
    }

    @Override
    public Map<Fagsak, List<ÅrsakOgPerioder>> finnFagsakerTilVurdering(Hendelse hendelse) {
        List<AktørId> aktører = hendelse.getHendelseInfo().getAktørIder();
        LocalDate opphørsdatoFraHendelse = hendelse.getHendelsePeriode().getFom();
        String hendelseId = hendelse.getHendelseInfo().getHendelseId();

        var fagsaker = new HashMap<Fagsak, List<ÅrsakOgPerioder>>();

        for (AktørId aktør : aktører) {
            var relevantFagsak = finnFagsakerForAktørTjeneste.hentRelevantFagsakForAktørSomSøker(FagsakYtelseType.UNGDOMSYTELSE, aktør, opphørsdatoFraHendelse);
            if (relevantFagsak.isEmpty()) {
                continue;
            }

            // Scenario 4: Ignorer opphørshendelse dersom opphørsdato == maksdato (naturlig avslutning).
            // Deltaker-appen setter sluttdato = maksdato automatisk. Ung-sak trenger ikke opprette revurdering.
            if (erNaturligAvslutningVedMaksdato(aktør, opphørsdatoFraHendelse, hendelseId)) {
                continue;
            }

            Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(relevantFagsak.get().getId());
            if (behandlingOpt.isEmpty()) {
                logger.info("Det er ingen behandling på fagsak. Ignorer hendelse");
                continue;
            }

            Behandling sisteBehandling = behandlingOpt.get();

            // Kan også vurdere om vi skal legge inn sjekk på om bruker har utbetaling etter opphørsdato
            Saksnummer saksnummer = relevantFagsak.get().getSaksnummer();
            if (erNyInformasjonIHendelsen(sisteBehandling, opphørsdatoFraHendelse, hendelseId, saksnummer)) {
                var årsaker = new ArrayList<ÅrsakOgPerioder>();
                var opphørsÅrsak = new ÅrsakOgPerioder(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, utledPeriode(relevantFagsak.get(), opphørsdatoFraHendelse));
                årsaker.add(opphørsÅrsak);
                fagsaker.put(relevantFagsak.get(), List.of(new ÅrsakOgPerioder(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, utledPeriode(relevantFagsak.get(), opphørsdatoFraHendelse))));
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

        if (gammelTomDato.equals(nyTomdato)) {
            throw new IllegalStateException("Ny tomdato er lik gammel tomdato. Hendelsen burde ha blitt ignorert.");
        }

        return gammelTomDato.isBefore(nyTomdato) ? DatoIntervallEntitet.fraOgMedTilOgMed(gammelTomDato.plusDays(1), nyTomdato) : DatoIntervallEntitet.fraOgMedTilOgMed(nyTomdato.plusDays(1), gammelTomDato);
    }


    /**
     * idempotens-sjekk for å hindre at det opprettes flere revurderinger fra samme hendelse.
     * hindrer også revurdering hvis hendelsen kommer etter at behandlingen er oppdatert med ny data.
     */
    private boolean erNyInformasjonIHendelsen(Behandling sisteBehandling, LocalDate opphørsdato, String hendelseId, Saksnummer saksnummer) {
        final var ungdomsprogramTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(sisteBehandling.getId());
        if (!ungdomsprogramTidslinje.isEmpty()) {
            var erSisteDatoAlleredeSattTilOpphørsdato = ungdomsprogramTidslinje.getMaxLocalDate().equals(opphørsdato);
            if (erSisteDatoAlleredeSattTilOpphørsdato) {
                logger.info("Datagrunnlag på behandling {} for {} hadde ingen perioder med ungdomsprogram etter opphørsdato. Trigget av hendelse {}.", sisteBehandling.getUuid(), saksnummer, hendelseId);
                return false;
            }
        }
        return true;
    }

    /**
     * Sjekker om opphørshendelsen er en naturlig avslutning ved maksdato.
     * Dersom opphørsdato == kvoteMaksDato fra registeret, er dette en automatisk avslutning
     * fra deltaker-appen og ung-sak trenger ikke å opprette ny behandling.
     */
    private boolean erNaturligAvslutningVedMaksdato(AktørId aktør, LocalDate opphørsdato, String hendelseId) {
        try {
            var registerOpplysninger = ungdomsprogramRegisterKlient.hentForAktørId(aktør.getAktørId());
            var maksdato = registerOpplysninger.opplysninger().stream()
                .map(UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO::forlengetPeriodeMaksDato)
                .filter(Objects::nonNull)
                .filter(opphørsdato::equals)
                .findFirst();
            if (maksdato.isPresent()) {
                logger.info("Opphørsdato {} == kvoteMaksDato fra register. Naturlig avslutning — ignorerer hendelse {}.", opphørsdato, hendelseId);
                return true;
            }
        } catch (Exception e) {
            logger.warn("Kunne ikke hente maksdato fra register for aktør. Fortsetter med normal opphørshåndtering. Hendelse: {}", hendelseId, e);
        }
        return false;
    }
}
