package no.nav.ung.domenetjenester.arkiv.journalpostvurderer;


import static no.nav.ung.domenetjenester.arkiv.journalpostvurderer.VurdertJournalpost.håndtert;
import static no.nav.ung.domenetjenester.arkiv.journalpostvurderer.VurdertJournalpost.ikkeHåndtert;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.domenetjenester.arkiv.JournalpostInfo;
import no.nav.ung.domenetjenester.arkiv.SøknadPayload;
import no.nav.ung.domenetjenester.arkiv.Søknadsformat;
import no.nav.ung.domenetjenester.arkiv.VurderStrukturertDokumentTask;
import no.nav.ung.fordel.handler.MottattMelding;
import no.nav.ung.fordel.kodeverdi.BrevkodeInformasjonUtleder;
import no.nav.ung.kodeverk.dokument.Brevkode;

@ApplicationScoped
public class StrukturertJournalpost implements Journalpostvurderer {

    private static final Logger log = LoggerFactory.getLogger(StrukturertJournalpost.class);
    public static final Set<String> GODKJENTE_KODER = Set.of(
        Brevkode.UNGDOMSYTELSE_SOKNAD.getOffisiellKode(),
        Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING.getOffisiellKode(),
        Brevkode.UNGDOMSYTELSE_OPPGAVE_BEKREFTELSE.getOffisiellKode()
    );

    private Boolean dumpPayload;

    public StrukturertJournalpost() {
    }

    @Inject
    public StrukturertJournalpost(
        @KonfigVerdi(value = "DUMP_PAYLOAD_VED_FEIL", defaultVerdi = "false") Boolean dumpPayload) {
        this.dumpPayload = dumpPayload;
    }

    @Override
    public VurdertJournalpost gjørVurdering(Vurderingsgrunnlag vurderingsgrunnlag) {
        var dataWrapper = vurderingsgrunnlag.melding();
        var journalpostInfo = vurderingsgrunnlag.journalpostInfo();
        mapBrevkodeInformasjon(dataWrapper, journalpostInfo);
        Optional<SøknadPayload> payload = deSerializeStrukturertPayload(journalpostInfo.getStrukturertPayload());
        payload.ifPresent(v -> dataWrapper.setPayload(v.getPayloadAsString()));
        var søknadsformat = payload.map(SøknadPayload::getFormat).orElse(null);

        if (søknadsformat == Søknadsformat.NY || søknadsformat == Søknadsformat.OPPGAVEBEKREFTELSE) {
            loggDokumenthåndtering("strukturert dokument", dataWrapper, journalpostInfo);
            return handleStrukturertDokument(dataWrapper, journalpostInfo);
        } else {
            return ikkeHåndtert();
        }
    }

    private static void loggDokumenthåndtering(String type, MottattMelding dataWrapper, JournalpostInfo journalpostInfo) {
        log.info("Håndterer {} journalpost[{}], brevkode={}, ytelseType={}",
            type, dataWrapper.getJournalPostId(), journalpostInfo.getBrevkode(), dataWrapper.getYtelseType().orElse(null));
    }

    @Override
    public boolean skalVurdere(Vurderingsgrunnlag vurderingsgrunnlag) {
        return GODKJENTE_KODER.contains(vurderingsgrunnlag.journalpostInfo().getBrevkode());
    }

    private void mapBrevkodeInformasjon(MottattMelding dataWrapper, JournalpostInfo journalpostInfo) {
        var brevkode = journalpostInfo.getBrevkode();
        var brevkodeInfo = BrevkodeInformasjonUtleder.getBrevkodeInformasjon(brevkode);
        if (brevkodeInfo.isEmpty()) {
            log.warn("Ukjent brevkode {}, journalpost={}, tema={}, forsendelseTidspunkt={}, strukturertInfo={}, tittel={}",
                brevkode,
                dataWrapper.getJournalPostId(),
                dataWrapper.getTema(),
                journalpostInfo.getForsendelseTidspunkt(),
                journalpostInfo.getInnholderStrukturertInformasjon(),
                journalpostInfo.getTittel());
        } else {
            var bi = brevkodeInfo.get();
            bi.getYtelseType().ifPresent(dataWrapper::setYtelseType);
            bi.getBehandlingTema().ifPresent(dataWrapper::setBehandlingTema);
            if (journalpostInfo.getInnholderStrukturertInformasjon()) {
                bi.getBehandlingTypeHvisStrukturert().ifPresent(dataWrapper::setBehandlingType);
            }
        }
    }


    private Optional<SøknadPayload> deSerializeStrukturertPayload(String payload) {
        try {
            return payload == null ? Optional.empty() : Søknadsformat.inspiserPayload(payload, dumpPayload);
        } catch (RuntimeException e) {
            if (dumpPayload) {
                log.error("Kunne ikke inspisere payload: " + payload, e);
            }
            throw e;
        }
    }


    private VurdertJournalpost handleStrukturertDokument(MottattMelding dataWrapper, JournalpostInfo journalpostInfo) {
        dataWrapper.setDokumentTittel(journalpostInfo.getTittel());
        dataWrapper.setPayload(journalpostInfo.getStrukturertPayload());
        dataWrapper.setBrevkode(journalpostInfo.getBrevkode());
        dataWrapper.setStrukturertDokument(true);

        // Vurder strukturert innhold
        return håndtert(dataWrapper.nesteSteg(VurderStrukturertDokumentTask.TASKTYPE));
    }


}
