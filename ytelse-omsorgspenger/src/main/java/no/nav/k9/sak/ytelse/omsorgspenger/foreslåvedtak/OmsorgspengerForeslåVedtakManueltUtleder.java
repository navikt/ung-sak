package no.nav.k9.sak.ytelse.omsorgspenger.foreslåvedtak;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ForeslåVedtakManueltUtleder;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;


@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OmsorgspengerForeslåVedtakManueltUtleder implements ForeslåVedtakManueltUtleder {

    private static final List<Brevkode> BREVKODER_SØKNAD_OMS = List.of(Brevkode.SØKNAD_UTBETALING_OMS, Brevkode.SØKNAD_UTBETALING_OMS_AT);
    private MottatteDokumentRepository mottatteDokumentRepository;

    OmsorgspengerForeslåVedtakManueltUtleder() {
        //for CDI proxy
    }

    @Inject
    public OmsorgspengerForeslåVedtakManueltUtleder(MottatteDokumentRepository mottatteDokumentRepository) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    @Override
    public boolean skalOppretteForeslåVedtakManuelt(Behandling behandling) {
        return BehandlingType.REVURDERING.equals(behandling.getType()) && behandling.erManueltOpprettet() || harSøknad(behandling);
    }

    private boolean harSøknad(Behandling behandling) {
        var omsorgspengerSøknader = mottatteDokumentRepository.hentMottatteDokumentForBehandling(behandling.getFagsakId(), behandling.getId(), BREVKODER_SØKNAD_OMS, false, DokumentStatus.GYLDIG);
        return !omsorgspengerSøknader.isEmpty();
    }

}
