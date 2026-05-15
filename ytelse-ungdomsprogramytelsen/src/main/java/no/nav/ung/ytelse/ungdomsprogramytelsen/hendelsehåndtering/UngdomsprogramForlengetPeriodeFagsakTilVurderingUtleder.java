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

import static no.nav.k9.felles.konfigurasjon.konfig.Tid.TIDENES_ENDE;

@ApplicationScoped
@HendelseTypeRef("UNGDOMSPROGRAM_FORLENGET_PERIODE")
public class UngdomsprogramForlengetPeriodeFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {

    private static final Logger logger = LoggerFactory.getLogger(UngdomsprogramForlengetPeriodeFagsakTilVurderingUtleder.class);

    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;

    public UngdomsprogramForlengetPeriodeFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public UngdomsprogramForlengetPeriodeFagsakTilVurderingUtleder(BehandlingRepository behandlingRepository,
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
                var forlengetPeriode = utledForlengetPeriode(fagsak);
                var årsakOgPerioder = new ArrayList<ÅrsakOgPerioder>();
                årsakOgPerioder.add(new ÅrsakOgPerioder(
                    BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM,
                    Set.of(forlengetPeriode)));

                fagsaker.put(fagsak, årsakOgPerioder);
            }
        }

        return fagsaker;
    }

    /**
     * Utleder initial trigger-periode for revurdering ved forlenget periode.
     *
     * <p>Den endelige tom-datoen er ikke kjent her – den settes av registret og lagres på
     * grunnlaget for den nye behandlingen i {@code InnhentUngdomsprogramperioderTask}.
     * Vi setter derfor en åpen trigger fra dagen etter opprinnelig maks-dato til {@code TIDENES_ENDE},
     * og lar {@code InnhentUngdomsprogramperioderTask} kappe tom-datoen til faktisk maks-dato
     * etter at registret er innhentet. På den måten unngår vi å kalle registret to ganger.
     */
    private DatoIntervallEntitet utledForlengetPeriode(Fagsak fagsak) {
        var eksisterendePeriode = fagsak.getPeriode();
        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (sisteBehandling.isEmpty()) {
            return eksisterendePeriode;
        }
        Long behandlingId = sisteBehandling.get().getId();
        var programTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId);
        if (programTidslinje.isEmpty()) {
            return eksisterendePeriode;
        }
        var fom = programTidslinje.getMinLocalDate();
        var tom = programTidslinje.getMaxLocalDate();

        // Beregn opprinnelig maks-dato (260 virkedager fra fom) – før forlengelsen.
        // For klippet programperiode brukes faktisk tom som opprinnelig maks-dato.
        LocalDate originalMaksDato;
        if (tom.equals(TIDENES_ENDE)) {
            originalMaksDato = FagsakperiodeUtleder.finnTomDato(fom, LocalDateTimeline.empty(), false, null);
        } else {
            originalMaksDato = tom;
        }

        // Initial trigger med åpen tom – kappes av InnhentUngdomsprogramperioderTask basert på
        // maksdato lagret på grunnlaget for den nye behandlingen etter register-innhenting.
        var nyFom = FagsakperiodeUtleder.justerTilNesteVirkedag(originalMaksDato.plusDays(1));
        return DatoIntervallEntitet.fraOgMedTilOgMed(nyFom, TIDENES_ENDE);
    }

    /**
     * Idempotens-sjekk for å hindre at det opprettes flere revurderinger fra samme hendelse.
     * Hindrer også revurdering hvis hendelsen kommer etter at behandlingen allerede er oppdatert med forlenget periode.
     */
    private boolean erNyInformasjonIHendelsen(Fagsak fagsak, String hendelseId) {
        Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandlingOpt.isEmpty()) {
            logger.info("Det er ingen behandling på fagsak. Ignorer hendelse");
            return false;
        }

        Behandling behandling = behandlingOpt.get();
        if (behandling.getBehandlingÅrsakerTyper().contains(BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM)) {
            var grunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId());
            boolean harForlengetPeriodeAllerede = grunnlag
                .map(gr -> gr.harForlengetPeriode())
                .orElse(false);

            if (harForlengetPeriodeAllerede) {
                logger.info("Behandling har allerede behandlingsårsak for hendelse og grunnlagsdata er oppdatert med forlenget periode. Ignorer hendelse {}", hendelseId);
                return false;
            }
        }

        return true;
    }
}
