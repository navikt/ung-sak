package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.INIT_PERIODER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;

/**
 * Samle sammen fakta for fravær.
 */
@ApplicationScoped
@BehandlingStegRef(value = INIT_PERIODER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(OMSORGSPENGER)
public class InitierPerioderSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(InitierPerioderSteg.class);

    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private TrekkUtFraværTjeneste trekkUtFraværTjeneste;

    private BehandlingRepository behandlingRepository;

    protected InitierPerioderSteg() {
        // for proxy
    }

    @Inject
    public InitierPerioderSteg(OmsorgspengerGrunnlagRepository grunnlagRepository,
                               TrekkUtFraværTjeneste trekkUtFraværTjeneste, BehandlingRepository behandlingRepository) {
        this.trekkUtFraværTjeneste = trekkUtFraværTjeneste;
        this.grunnlagRepository = grunnlagRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        var samletFravær = trekkUtFraværTjeneste.samleSammenOppgittFravær(behandlingId);
        if (samletFravær.isEmpty() && !behandling.erManueltOpprettet()) {
            // FIXME veldig merkelig håndtering. Vi kommer hit ved IM uten kravperide, men ikke når vi også har andre krav.
            // ..... det kopieres magisk fra forrige behandling (eller inneværende, etter tilbakehopp). Bør være perioder fra fagsaken, for at det skal bli konsistent

            // Kan inntreffe dersom IM er av variant ikkeFravaer eller ikke refusjon. Da brukes fraværsperioder kopiert fra forrige behandling
            // TODO: Logg heller dokumenter tilknyttet behandling
            log.warn("Kun kravdokument uten fraværsperioder er knyttet til behandling. Fraværsperioder fra tidligere behandlinger brukes, forventer noop for ytelse.");
            var oppgittOpt = grunnlagRepository.hentSammenslåttOppgittFraværHvisEksisterer(behandling.getId());
            samletFravær = oppgittOpt.isPresent() ? new ArrayList<>(oppgittOpt.get().getPerioder()) : List.of();
        }

        if (samletFravær.isEmpty()) {
            throw new IllegalStateException("Har ingen kravperioder. Forventet automatisk henleggelse i tidligere steg, eventuelt at dokument på behandling aldri ble sendt fra fordel");
        }
        grunnlagRepository.lagreOgFlushSammenslåttOppgittFravær(behandlingId, new OppgittFravær(samletFravær));

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
