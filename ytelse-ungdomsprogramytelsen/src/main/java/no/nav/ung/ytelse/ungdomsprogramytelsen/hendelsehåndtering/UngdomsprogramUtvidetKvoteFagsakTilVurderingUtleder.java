package no.nav.ung.ytelse.ungdomsprogramytelsen.hendelsehåndtering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.revurdering.ÅrsakOgPerioder;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.hendelsemottak.tjenester.FagsakerTilVurderingUtleder;
import no.nav.ung.sak.hendelsemottak.tjenester.FinnFagsakerForAktørTjeneste;
import no.nav.ung.sak.hendelsemottak.tjenester.HendelseTypeRef;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.FagsakperiodeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;

@ApplicationScoped
@HendelseTypeRef("UNGDOMSPROGRAM_UTVIDET_KVOTE")
public class UngdomsprogramUtvidetKvoteFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {

    private static final Logger logger = LoggerFactory.getLogger(UngdomsprogramUtvidetKvoteFagsakTilVurderingUtleder.class);

    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;

    public UngdomsprogramUtvidetKvoteFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public UngdomsprogramUtvidetKvoteFagsakTilVurderingUtleder(BehandlingRepository behandlingRepository,
                                                               UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                                               FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste,
                                                               UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.finnFagsakerForAktørTjeneste = finnFagsakerForAktørTjeneste;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
    }

    @Override
    public Map<Fagsak, List<ÅrsakOgPerioder>> finnFagsakerTilVurdering(Hendelse hendelse) {
        List<AktørId> aktører = hendelse.getHendelseInfo().getAktørIder();
        LocalDate periodeFom = hendelse.getHendelsePeriode().getFom();
        String hendelseId = hendelse.getHendelseInfo().getHendelseId();

        var fagsaker = new HashMap<Fagsak, List<ÅrsakOgPerioder>>();

        for (AktørId aktør : aktører) {
            var relevantFagsak = finnFagsakerForAktørTjeneste.hentRelevantFagsakForAktørSomSøker(FagsakYtelseType.UNGDOMSYTELSE, aktør, periodeFom);
            if (relevantFagsak.isEmpty()) {
                continue;
            }
            if (erNyInformasjonIHendelsen(relevantFagsak.get(), hendelseId)) {
                var fagsak = relevantFagsak.get();
                var utvidetPeriode = utledUtvidetPeriode(fagsak);
                var årsakOgPerioder = new ArrayList<ÅrsakOgPerioder>();
                årsakOgPerioder.add(new ÅrsakOgPerioder(
                    BehandlingÅrsakType.RE_HENDELSE_UTVIDET_KVOTE_UNGDOMSPROGRAM,
                    Set.of(utvidetPeriode)));

                // Legg til RE_KONTROLL_REGISTER_INNTEKT for de nye (kant-i-kant) dagene slik at
                // kontrollperioder genereres og tilkjent ytelse dekker dem. De nye dagene er alltid
                // fremtidige (kant-i-kant etter eksisterende fagsakperiode), så dette trigger ikke
                // feilutbetaling mot allerede utbetalte perioder.
                var eksisterendeTom = fagsak.getPeriode().getTomDato();
                if (utvidetPeriode.getTomDato().isAfter(eksisterendeTom)) {
                    var nyeMåneder = DatoIntervallEntitet.fraOgMedTilOgMed(
                        eksisterendeTom.plusDays(1), utvidetPeriode.getTomDato());
                    årsakOgPerioder.add(new ÅrsakOgPerioder(
                        BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT,
                        Set.of(nyeMåneder)));
                }

                fagsaker.put(fagsak, årsakOgPerioder);
            }
        }

        return fagsaker;
    }

    /**
     * Utleder trigger-periode for revurdering ved utvidet kvote.
     *
     * <p>To scenarioer, begge håndteres kant-i-kant:
     * <ul>
     *   <li>Åpen programperiode (tom=9999-12-31, løpende): trigger-perioden strekker seg til
     *       300 virkedager fra fom.</li>
     *   <li>Klippet programperiode (opphør satt, eller 260 virkedager forbrukt): trigger-perioden
     *       strekkes med resterende virkedager (opp til 300 totalt) kant-i-kant etter eksisterende tom.</li>
     * </ul>
     */
    private DatoIntervallEntitet utledUtvidetPeriode(Fagsak fagsak) {
        var eksisterendePeriode = fagsak.getPeriode();
        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (sisteBehandling.isEmpty()) {
            return eksisterendePeriode;
        }
        var programTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(sisteBehandling.get().getId());
        if (programTidslinje.isEmpty()) {
            return eksisterendePeriode;
        }
        var fom = programTidslinje.getMinLocalDate();
        var tom = programTidslinje.getMaxLocalDate();
        LocalDate utvidetTom;
        if (tom.equals(LocalDate.of(9999, 12, 31))) {
            utvidetTom = FagsakperiodeUtleder.finnTomDato(fom, LocalDateTimeline.empty(), true);
        } else {
            var nyFom = tom.plusDays(1);
            utvidetTom = FagsakperiodeUtleder.finnTomDato(nyFom, programTidslinje, true);
            if (!utvidetTom.isAfter(tom)) {
                // 300 virkedager allerede forbrukt – ingen utvidelse mulig
                return eksisterendePeriode;
            }
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(eksisterendePeriode.getFomDato(), utvidetTom);
    }

    /**
     * Idempotens-sjekk for å hindre at det opprettes flere revurderinger fra samme hendelse.
     * Hindrer også revurdering hvis hendelsen kommer etter at behandlingen allerede er oppdatert med utvidet kvote.
     */
    private boolean erNyInformasjonIHendelsen(Fagsak fagsak, String hendelseId) {
        Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandlingOpt.isEmpty()) {
            logger.info("Det er ingen behandling på fagsak. Ignorer hendelse");
            return false;
        }

        Behandling behandling = behandlingOpt.get();
        if (behandling.getBehandlingÅrsakerTyper().contains(BehandlingÅrsakType.RE_HENDELSE_UTVIDET_KVOTE_UNGDOMSPROGRAM)) {
            var grunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId());
            boolean harUtvidetKvoteAllerede = grunnlag
                .map(gr -> gr.isHarUtvidetKvote())
                .orElse(false);

            if (harUtvidetKvoteAllerede) {
                logger.info("Behandling har allerede behandlingsårsak for hendelse og grunnlagsdata er oppdatert med utvidet kvote. Ignorer hendelse {}", hendelseId);
                return false;
            }
        }

        return true;
    }
}
