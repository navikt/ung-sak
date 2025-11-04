package no.nav.ung.sak.hendelsemottak.tjenester;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.revurdering.ÅrsakOgPerioder;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.kontroll.KontrollerteInntektperioderTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@ApplicationScoped
@HendelseTypeRef("UNG_OPPHØR")
public class UngdomsprogramOpphørFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {

    private static final Logger logger = LoggerFactory.getLogger(UngdomsprogramOpphørFagsakTilVurderingUtleder.class);
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;
    private FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste;
    private boolean kontrollSisteMndEnabled;

    public UngdomsprogramOpphørFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public UngdomsprogramOpphørFagsakTilVurderingUtleder(BehandlingRepository behandlingRepository,
                                                         UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
                                                         KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste,
                                                         FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste,
                                                         @KonfigVerdi(value = "KONTROLL_SISTE_MND_ENABLED", defaultVerdi = "false") boolean kontrollSisteMndEnabled) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.kontrollerteInntektperioderTjeneste = kontrollerteInntektperioderTjeneste;
        this.finnFagsakerForAktørTjeneste = finnFagsakerForAktørTjeneste;
        this.kontrollSisteMndEnabled = kontrollSisteMndEnabled;
    }

    @Override
    public Map<Fagsak, List<ÅrsakOgPerioder>> finnFagsakerTilVurdering(Hendelse hendelse) {
        List<AktørId> aktører = hendelse.getHendelseInfo().getAktørIder();
        LocalDate opphørsdatoFraHendelse = hendelse.getHendelsePeriode().getFom();
        String hendelseId = hendelse.getHendelseInfo().getHendelseId();

        var fagsaker = new HashMap<Fagsak, List<ÅrsakOgPerioder>>();

        for (AktørId aktør : aktører) {
            var relevantFagsak = finnFagsakerForAktørTjeneste.hentRelevantFagsakForAktørSomSøker(aktør, opphørsdatoFraHendelse);
            if (relevantFagsak.isEmpty()) {
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

                if (kontrollSisteMndEnabled) {
                    LocalDate sisteDagIOpphørsmåned = opphørsdatoFraHendelse.with(TemporalAdjusters.lastDayOfMonth());
                    // Sjekker om det er gjort inntektskontroll for resten av måneden etter opphørsdato
                    // Dersom det er gjort kontroll og kontrollperioden inkluderer resten av måneden, må vi gjøre ny kontroll med ny periode
                    if (opphørsdatoFraHendelse.isBefore(sisteDagIOpphørsmåned)) {
                        LocalDateTimeline<BigDecimal> kontrollertePerioder = kontrollerteInntektperioderTjeneste.hentTidslinje(sisteBehandling.getId());
                        LocalDateInterval restenAvMåneden = new LocalDateInterval(opphørsdatoFraHendelse.plusDays(1), sisteDagIOpphørsmåned);
                        if (harGjortKontrollIRestenAvMåneden(kontrollertePerioder, restenAvMåneden)) {
                            LocalDate førsteDagIMåneden = opphørsdatoFraHendelse.withDayOfMonth(1);
                            årsaker.add(new ÅrsakOgPerioder(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(førsteDagIMåneden, opphørsdatoFraHendelse))));
                        }
                    }
                }

                fagsaker.put(relevantFagsak.get(), årsaker);
            }
        }


        return fagsaker;
    }

    private static boolean harGjortKontrollIRestenAvMåneden(LocalDateTimeline<BigDecimal> kontrollertePerioder, LocalDateInterval restenAvMåneden) {
        return !kontrollertePerioder.intersection(restenAvMåneden).isEmpty();
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

}
