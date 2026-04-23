package no.nav.ung.ytelse.ungdomsprogramytelsen.hendelsehåndtering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
                var utvidetPeriode = utledUtvidetPeriode(relevantFagsak.get());
                fagsaker.put(relevantFagsak.get(), List.of(new ÅrsakOgPerioder(
                    BehandlingÅrsakType.RE_HENDELSE_UTVIDET_KVOTE_UNGDOMSPROGRAM,
                    Set.of(utvidetPeriode))));
            }
        }

        return fagsaker;
    }

    /**
     * Utleder fagsakperiode utvidet med 300 virkedager (utvidet kvote) basert på programperiodene
     * fra siste behandling. Trigger-perioden må dekke hele den utvidede perioden slik at
     * vilkårsperiodene for de ekstra 40 virkedagene også evalueres.
     */
    private DatoIntervallEntitet utledUtvidetPeriode(Fagsak fagsak) {
        var eksisterendePeriode = fagsak.getPeriode();
        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (sisteBehandling.isPresent()) {
            var programTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(sisteBehandling.get().getId());
            if (!programTidslinje.isEmpty()) {
                var utvidetTom = FagsakperiodeUtleder.finnTomDato(programTidslinje.getMinLocalDate(), programTidslinje, true);
                return DatoIntervallEntitet.fraOgMedTilOgMed(eksisterendePeriode.getFomDato(), utvidetTom);
            }
        }
        return eksisterendePeriode;
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
