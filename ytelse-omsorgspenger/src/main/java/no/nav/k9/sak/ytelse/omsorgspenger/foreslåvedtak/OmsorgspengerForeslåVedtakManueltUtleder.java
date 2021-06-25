package no.nav.k9.sak.ytelse.omsorgspenger.foreslåvedtak;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ForeslåVedtakManueltUtleder;
import no.nav.k9.sak.domene.behandling.steg.vurdermanueltbrev.K9FormidlingKlient;


@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OmsorgspengerForeslåVedtakManueltUtleder implements ForeslåVedtakManueltUtleder {

    private static final List<Brevkode> BREVKODER_SØKNAD_OMS = List.of(Brevkode.SØKNAD_UTBETALING_OMS, Brevkode.SØKNAD_UTBETALING_OMS_AT);

    private K9FormidlingKlient formidlingKlient;
    private Boolean automatiskVedtakForSøknader;
    private MottatteDokumentRepository mottatteDokumentRepository;

    OmsorgspengerForeslåVedtakManueltUtleder() {
        //for CDI proxy
    }

    @Inject
    public OmsorgspengerForeslåVedtakManueltUtleder(MottatteDokumentRepository mottatteDokumentRepository,
                                                    K9FormidlingKlient formidlingKlient,
                                                    @KonfigVerdi(value = "AUTOMATISK_VEDTAK_OMP_SOKNAD", defaultVerdi = "true") Boolean automatiskVedtakForSøknader) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.formidlingKlient = formidlingKlient;
        this.automatiskVedtakForSøknader = automatiskVedtakForSøknader;
    }

    @Override
    public boolean skalOppretteForeslåVedtakManuelt(Behandling behandling) {
        return erManuellRevurdering(behandling) || harSøknad(behandling) || trengerManueltBrev(behandling);
    }

    private boolean erManuellRevurdering(Behandling behandling) {
        return BehandlingType.REVURDERING == behandling.getType() && behandling.erManueltOpprettet();
    }

    private boolean trengerManueltBrev(Behandling behandling) {
        return false;
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

}
