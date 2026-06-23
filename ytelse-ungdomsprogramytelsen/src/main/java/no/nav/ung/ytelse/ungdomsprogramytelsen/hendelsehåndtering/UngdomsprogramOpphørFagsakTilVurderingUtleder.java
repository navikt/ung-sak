package no.nav.ung.ytelse.ungdomsprogramytelsen.hendelsehåndtering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
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
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;
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
    private VilkårTjeneste vilkårTjeneste;

    public UngdomsprogramOpphørFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public UngdomsprogramOpphørFagsakTilVurderingUtleder(BehandlingRepository behandlingRepository,
                                                         UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
                                                         FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste,
                                                         VilkårTjeneste vilkårTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.finnFagsakerForAktørTjeneste = finnFagsakerForAktørTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
    }

    @Override
    public Map<Fagsak, List<ÅrsakOgPerioder>> finnFagsakerTilVurdering(Hendelse hendelse) {
        List<AktørId> aktører = hendelse.getHendelseInfo().getAktørIder();
        LocalDate opphørsdatoFraHendelse = hendelse.getHendelsePeriode().getFom();
        String hendelseId = hendelse.getHendelseInfo().getHendelseId();

        var fagsaker = new HashMap<Fagsak, List<ÅrsakOgPerioder>>();

        for (AktørId aktør : aktører) {
            var relevantFagsak = finnFagsakerForAktørTjeneste
                .hentRelevantFagsakForAktørSomSøker(FagsakYtelseType.UNGDOMSYTELSE, aktør, opphørsdatoFraHendelse)
                .or(() -> finnFagsakerForAktørTjeneste.hentSisteFagsakForAktørSomSøker(FagsakYtelseType.UNGDOMSYTELSE, aktør)
                    .filter(f -> !opphørsdatoFraHendelse.isBefore(f.getPeriode().getFomDato())));
            if (relevantFagsak.isEmpty()) {
                continue;
            }

            // Ignorer opphørshendelse dersom opphørsdato == maksdato (naturlig avslutning).
            // Deltaker-appen setter sluttdato = maksdato automatisk. Ung-sak trenger ikke opprette revurdering.
            Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(relevantFagsak.get().getId());
            if (behandlingOpt.isEmpty()) {
                logger.info("Det er ingen behandling på fagsak. Ignorer hendelse");
                continue;
            }

            Behandling sisteBehandling = behandlingOpt.get();

            if (harIngenOppfyltYtelseEtterDato(sisteBehandling, opphørsdatoFraHendelse, hendelseId)) {
                continue;
            }

            // Kan også vurdere om vi skal legge inn sjekk på om bruker har utbetaling etter opphørsdato
            Saksnummer saksnummer = relevantFagsak.get().getSaksnummer();
            if (erNyInformasjonIHendelsen(sisteBehandling, opphørsdatoFraHendelse, hendelseId, saksnummer)) {
                var opphørsÅrsak = new ÅrsakOgPerioder(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM,
                    utledPeriode(relevantFagsak.get(), opphørsdatoFraHendelse));
                fagsaker.put(relevantFagsak.get(), List.of(opphørsÅrsak));
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
     * Sjekker om det finnes oppfylte vilkårsperioder etter opphørsdato.
     * Hvis ikke, er det ingen aktiv ytelse å opphøre — revurdering er unødvendig.
     * Dette fanger opp naturlig avslutning ved maksdato, aldersvilkår-avslag,
     * og andre scenarioer der ytelsen allerede er avsluttet/avslått.
     *
     * Dersom vilkårsresultatet er tomt (vilkår ikke vurdert), antas ytelsen å være aktiv.
     */
    private boolean harIngenOppfyltYtelseEtterDato(Behandling behandling, LocalDate opphørsdato, String hendelseId) {
        // Sjekk 1: Hvis opphørsdato == periodeMaksDato kunne dette vært en naturlig avslutning,
        // men vi må kontrollere om vilkårsperioden for ungdomsprogramvilkåret dekker videre enn maksdato.
        var maksdato = ungdomsprogramPeriodeTjeneste.finnPeriodeMaksDato(behandling.getId());
        if (maksdato.isPresent() && maksdato.get().equals(opphørsdato)) {
            var vilkårene = vilkårTjeneste.hentHvisEksisterer(behandling.getId());
            if (vilkårene.isPresent()) {
                var ungdomsprogramVilkårTimeline = vilkårene.get().getVilkårTimeline(VilkårType.UNGDOMSPROGRAMVILKÅRET)
                    .filterValue(v -> v.getGjeldendeUtfall() == Utfall.OPPFYLT);
                var vilkårsperiodeEtterMaksdato = ungdomsprogramVilkårTimeline.intersection(
                    new LocalDateInterval(opphørsdato.plusDays(1), LocalDateInterval.TIDENES_ENDE));
                if (!vilkårsperiodeEtterMaksdato.isEmpty()) {
                    // Vilkårsperioden strekker seg videre enn maksdato — revurdering nødvendig
                    return false;
                }
            }
            logger.info("Opphørsdato {} == periodeMaksDato fra grunnlag, og vilkårsperioden dekker ikke videre. Naturlig avslutning — ignorerer hendelse {}.",
                opphørsdato, hendelseId);
            return true;
        }

        // Sjekk 2: Hvis vilkårsresultat finnes og dekker perioden etter opphørsdato med kun ikke-oppfylte utfall
        var samletResultat = vilkårTjeneste.samletVilkårsresultat(behandling.getId());
        if (samletResultat.isEmpty()) {
            // Vilkår ikke vurdert ennå — anta at ytelsen er aktiv
            return false;
        }

        var resultatEtterOpphørsdato = samletResultat
            .intersection(new LocalDateInterval(opphørsdato.plusDays(1), LocalDateInterval.TIDENES_ENDE));

        if (resultatEtterOpphørsdato.isEmpty()) {
            // Vilkårsresultatet dekker ikke perioden etter opphørsdato (ikke evaluert ennå) — anta aktiv ytelse
            return false;
        }

        var oppfyltEtterOpphørsdato = resultatEtterOpphørsdato
            .filterValue(v -> v.getSamletUtfall() == Utfall.OPPFYLT);

        if (oppfyltEtterOpphørsdato.isEmpty()) {
            logger.info("Ingen oppfylte vilkårsperioder etter opphørsdato {} for behandling {}. Ignorerer hendelse {}.",
                opphørsdato, behandling.getId(), hendelseId);
            return true;
        }
        return false;
    }
}
