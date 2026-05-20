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
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.FagsakperiodeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;


@ApplicationScoped
@HendelseTypeRef("UNGDOMSPROGRAM_FORLENGET_PERIODE")
public class UngdomsprogramForlengetPeriodeFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {

    private static final Logger logger = LoggerFactory.getLogger(UngdomsprogramForlengetPeriodeFagsakTilVurderingUtleder.class);

    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste;

    public UngdomsprogramForlengetPeriodeFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public UngdomsprogramForlengetPeriodeFagsakTilVurderingUtleder(BehandlingRepository behandlingRepository,
                                                                   UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                                                   FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.finnFagsakerForAktørTjeneste = finnFagsakerForAktørTjeneste;
    }

    @Override
    public Map<Fagsak, List<ÅrsakOgPerioder>> finnFagsakerTilVurdering(Hendelse hendelse) {
        List<AktørId> aktører = hendelse.getHendelseInfo().getAktørIder();
        LocalDate periodeFom = hendelse.getHendelsePeriode().getFom();
        LocalDate hendelseTom = hendelse.getHendelsePeriode().getTom();
        String hendelseId = hendelse.getHendelseInfo().getHendelseId();

        var fagsaker = new HashMap<Fagsak, List<ÅrsakOgPerioder>>();

        for (AktørId aktør : aktører) {
            var relevantFagsak = finnFagsakerForAktørTjeneste.hentRelevantFagsakForAktørSomSøker(FagsakYtelseType.UNGDOMSYTELSE, aktør, periodeFom);
            if (relevantFagsak.isEmpty()) {
                continue;
            }
            if (erNyInformasjonIHendelsen(relevantFagsak.get(), hendelseId)) {
                var fagsak = relevantFagsak.get();
                var forlengetPeriode = utledForlengetPeriode(fagsak, hendelseTom);
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
     * Utleder trigger-periode for revurdering ved forlenget periode.
     *
     * <p>Bruker fagsakens eksisterende tom-dato som slutt på opprinnelig periode,
     * og hendelsens tom-dato som ny maks-dato etter forlengelsen.
     * Trigger-perioden blir dermed fra dagen etter opprinnelig maks-dato til ny maks-dato.
     */
    private DatoIntervallEntitet utledForlengetPeriode(Fagsak fagsak, LocalDate hendelseTom) {
        var nyFom = FagsakperiodeUtleder.justerTilNesteVirkedag(fagsak.getPeriode().getTomDato().plusDays(1));
        return DatoIntervallEntitet.fraOgMedTilOgMed(nyFom, hendelseTom);
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
