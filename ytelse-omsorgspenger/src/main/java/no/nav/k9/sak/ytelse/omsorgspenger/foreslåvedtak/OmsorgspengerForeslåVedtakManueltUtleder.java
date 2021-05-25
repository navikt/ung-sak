package no.nav.k9.sak.ytelse.omsorgspenger.foreslåvedtak;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.formidling.kontrakt.informasjonsbehov.InformasjonsbehovListeDto;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ForeslåVedtakManueltUtleder;
import no.nav.k9.sak.domene.behandling.steg.vurdermanueltbrev.K9FormidlingKlient;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;


@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OmsorgspengerForeslåVedtakManueltUtleder implements ForeslåVedtakManueltUtleder {

    private static final List<Brevkode> BREVKODER_SØKNAD_OMS = List.of(Brevkode.SØKNAD_UTBETALING_OMS, Brevkode.SØKNAD_UTBETALING_OMS_AT);

    private K9FormidlingKlient formidlingKlient;
    private Boolean lansert;
    private MottatteDokumentRepository mottatteDokumentRepository;

    OmsorgspengerForeslåVedtakManueltUtleder() {
        //for CDI proxy
    }

    @Inject
    public OmsorgspengerForeslåVedtakManueltUtleder(MottatteDokumentRepository mottatteDokumentRepository,
                                                    K9FormidlingKlient formidlingKlient,
                                                    @KonfigVerdi(value = "FORMIDLING_RETUR_MALTYPER", defaultVerdi = "true") Boolean lansert) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.formidlingKlient = formidlingKlient;
        this.lansert = lansert;
    }

    @Override
    public boolean skalOppretteForeslåVedtakManuelt(Behandling behandling) {
        return erManuellRevurdering(behandling) || harSøknad(behandling) || trengerManueltBrev(behandling);
    }

    private boolean erManuellRevurdering(Behandling behandling) {
        return BehandlingType.REVURDERING == behandling.getType() && behandling.erManueltOpprettet();
    }

    private boolean trengerManueltBrev(Behandling behandling) {
        if (!lansert) {
            return false;
        }

        //TODO sjekk mot søknad på saken kan kanskje fjernes når k9-formidlings tjeneste er mer tilpasset behovet
        if (!harSøknad(behandling.getFagsak())) {
            return false;
        }
        InformasjonsbehovListeDto informasjonsbehov = formidlingKlient.hentInformasjonsbehov(behandling.getUuid(), behandling.getFagsakYtelseType());
        return !informasjonsbehov.getInformasjonsbehov().isEmpty();
    }

    private boolean harSøknad(Behandling behandling) {
        var omsorgspengerSøknader = mottatteDokumentRepository.hentMottatteDokumentForBehandling(behandling.getFagsakId(), behandling.getId(), BREVKODER_SØKNAD_OMS, false, DokumentStatus.GYLDIG);
        return !omsorgspengerSøknader.isEmpty();
    }

    private boolean harSøknad(Fagsak fagsak) {
        var dokumenterPåSaken = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsak.getId(), DokumentStatus.GYLDIG);
        return dokumenterPåSaken.stream()
            .anyMatch(dok -> BREVKODER_SØKNAD_OMS.contains(dok.getType()));
    }

}
