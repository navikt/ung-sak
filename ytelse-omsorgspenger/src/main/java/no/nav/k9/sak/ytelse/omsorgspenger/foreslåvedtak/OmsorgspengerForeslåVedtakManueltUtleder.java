package no.nav.k9.sak.ytelse.omsorgspenger.foreslåvedtak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ForeslåVedtakManueltUtleder;
import no.nav.k9.sak.domene.behandling.steg.vurdermanueltbrev.VurderBrevTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;


@FagsakYtelseTypeRef(OMSORGSPENGER)
@ApplicationScoped
public class OmsorgspengerForeslåVedtakManueltUtleder implements ForeslåVedtakManueltUtleder {

    private static final List<Brevkode> BREVKODER_SØKNAD_OMS = List.of(Brevkode.SØKNAD_UTBETALING_OMS, Brevkode.SØKNAD_UTBETALING_OMS_AT, Brevkode.FRAVÆRSKORRIGERING_IM_OMS);

    private static final Logger log = LoggerFactory.getLogger(InternalManipulerBehandling.class);

    private VurderBrevTjeneste vurderBrevTjeneste;
    private Boolean automatiskVedtakForSøknader;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private OmsorgspengerGrunnlagRepository grunnlagRepository;

    OmsorgspengerForeslåVedtakManueltUtleder() {
        //for CDI proxy
    }

    @Inject
    public OmsorgspengerForeslåVedtakManueltUtleder(MottatteDokumentRepository mottatteDokumentRepository,
                                                    VurderBrevTjeneste vurderBrevTjeneste, OmsorgspengerGrunnlagRepository grunnlagRepository,
                                                    @KonfigVerdi(value = "AUTOMATISK_VEDTAK_OMP_SOKNAD", defaultVerdi = "true") Boolean automatiskVedtakForSøknader) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.vurderBrevTjeneste = vurderBrevTjeneste;
        this.grunnlagRepository = grunnlagRepository;
        this.automatiskVedtakForSøknader = automatiskVedtakForSøknader;
    }

    @Override
    public boolean skalOppretteForeslåVedtakManuelt(Behandling behandling) {
        if (erManuellRevurdering(behandling)) {
            return skalOpprettes("Behandling er manuell revurdering");
        }
        if (harSøknad(behandling)) {
            return skalOpprettes("Behandling har søknad");
        }
        if (vurderBrevTjeneste.trengerManueltBrev(behandling)) {
            return skalOpprettes("Behandling krever manuelt brev");
        }
        if (harIkkeKravperioder(behandling)) {
            return skalOpprettes("Behandling har ingen kravperioder");
        }
        return false;
    }

    private boolean skalOpprettes(String årsak) {
        log.info("Skal opprette aksjonspunkt Foreslå vedtak manuelt, årsak: " + årsak);
        return true;
    }

    private boolean erManuellRevurdering(Behandling behandling) {
        return BehandlingType.REVURDERING == behandling.getType() && behandling.erManueltOpprettet();
    }

    private boolean harSøknad(Behandling behandling) {
        if (automatiskVedtakForSøknader) {
            // Behandling skal ikke lenger opprette 5028 for å holde igjen behandlinger med søknad.
            // Skal bare opprette 5028 dersom respons fra formidling-tjenesten tilsier dette
            return false;
        }
        var omsorgspengerSøknader = mottatteDokumentRepository.hentMottatteDokumentForBehandling(behandling.getFagsakId(), behandling.getId(), BREVKODER_SØKNAD_OMS, false, DokumentStatus.GYLDIG);
        return !omsorgspengerSøknader.isEmpty();
    }

    // Kan skje dersom IM uten refusjonskrav har blitt mottatt, uten at det finnes en matchende søknad
    private boolean harIkkeKravperioder(Behandling behandling) {
        return grunnlagRepository.hentSammenslåtteFraværPerioder(behandling.getId()).isEmpty();
    }

}
