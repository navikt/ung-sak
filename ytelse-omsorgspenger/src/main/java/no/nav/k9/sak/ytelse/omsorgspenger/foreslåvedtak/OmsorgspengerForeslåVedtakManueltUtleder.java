package no.nav.k9.sak.ytelse.omsorgspenger.foreslåvedtak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.formidling.kontrakt.informasjonsbehov.InformasjonsbehovListeDto;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ForeslåVedtakManueltUtleder;
import no.nav.k9.sak.domene.behandling.steg.vurdermanueltbrev.K9FormidlingKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;


@FagsakYtelseTypeRef(OMSORGSPENGER)
@ApplicationScoped
public class OmsorgspengerForeslåVedtakManueltUtleder implements ForeslåVedtakManueltUtleder {

    private static final List<Brevkode> BREVKODER_SØKNAD_OMS = List.of(Brevkode.SØKNAD_UTBETALING_OMS, Brevkode.SØKNAD_UTBETALING_OMS_AT, Brevkode.PAPIRSØKNAD_UTBETALING_OMS_AT, Brevkode.FRAVÆRSKORRIGERING_IM_OMS);

    private static final Logger log = LoggerFactory.getLogger(InternalManipulerBehandling.class);

    private K9FormidlingKlient formidlingKlient;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private OmsorgspengerGrunnlagRepository grunnlagRepository;

    OmsorgspengerForeslåVedtakManueltUtleder() {
        //for CDI proxy
    }

    @Inject
    public OmsorgspengerForeslåVedtakManueltUtleder(MottatteDokumentRepository mottatteDokumentRepository,
                                                    K9FormidlingKlient formidlingKlient,
                                                    OmsorgspengerGrunnlagRepository grunnlagRepository) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.formidlingKlient = formidlingKlient;
        this.grunnlagRepository = grunnlagRepository;
    }

    @Override
    public boolean skalOppretteForeslåVedtakManuelt(Behandling behandling) {
        if (erManuellRevurdering(behandling)) {
            return skalOpprettes("Behandling er manuell revurdering");
        }
        if (trengerManueltBrev(behandling)) {
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

    private boolean trengerManueltBrev(Behandling behandling) {
        InformasjonsbehovListeDto informasjonsbehov = formidlingKlient.hentInformasjonsbehov(behandling.getUuid(), behandling.getFagsakYtelseType());
        return !informasjonsbehov.getInformasjonsbehov().isEmpty();
    }


    // Kan skje dersom IM uten refusjonskrav har blitt mottatt, uten at det finnes en matchende søknad
    private boolean harIkkeKravperioder(Behandling behandling) {
        return grunnlagRepository.hentSammenslåtteFraværPerioder(behandling.getId()).isEmpty();
    }

}
