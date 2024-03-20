package no.nav.k9.sak.ytelse.pleiepengerbarn.foreslåvedtak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.formidling.kontrakt.informasjonsbehov.InformasjonsbehovListeDto;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ForeslåVedtakManueltUtleder;
import no.nav.k9.sak.domene.behandling.steg.vurdermanueltbrev.K9FormidlingKlient;
import no.nav.k9.sak.punsj.PunsjRestKlient;


@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@ApplicationScoped
public class PleiepengerForeslåVedtakManueltUtleder implements ForeslåVedtakManueltUtleder {

    private static final Logger log = LoggerFactory.getLogger(PleiepengerForeslåVedtakManueltUtleder.class);

    private K9FormidlingKlient formidlingKlient;
    private PunsjRestKlient punsjKlient;

    PleiepengerForeslåVedtakManueltUtleder() {
        //for CDI proxy
    }

    @Inject
    public PleiepengerForeslåVedtakManueltUtleder(K9FormidlingKlient formidlingKlient, PunsjRestKlient punsjKlient) {
        this.formidlingKlient = formidlingKlient;
        this.punsjKlient = punsjKlient;
    }

    @Override
    public boolean skalOppretteForeslåVedtakManuelt(Behandling behandling) {
        return erManuellRevurdering(behandling) || trengerManueltBrev(behandling) || harOppgaveIPunsj(behandling);
    }

    private boolean erManuellRevurdering(Behandling behandling) {
        return BehandlingType.REVURDERING == behandling.getType() && behandling.erManueltOpprettet();
    }

    private boolean trengerManueltBrev(Behandling behandling) {
        InformasjonsbehovListeDto informasjonsbehov = formidlingKlient.hentInformasjonsbehov(behandling.getUuid(), behandling.getFagsakYtelseType());
        return !informasjonsbehov.getInformasjonsbehov().isEmpty();
    }

    private boolean harOppgaveIPunsj(Behandling behandling) {
        var uferdigJournalpostIderPåAktør = punsjKlient.getUferdigJournalpostIderPåAktør(behandling.getAktørId().getAktørId(), behandling.getFagsak().getPleietrengendeAktørId().getAktørId());
        boolean harPunsjoppgave = uferdigJournalpostIderPåAktør.isPresent()
            && (!uferdigJournalpostIderPåAktør.get().getJournalpostIder().isEmpty() || !uferdigJournalpostIderPåAktør.get().getJournalpostIderBarn().isEmpty());
        if (harPunsjoppgave) {
            log.info("Skal opprette foreslå vedtak manuelt pga uferdig oppgave i punsj");
        }
        return harPunsjoppgave;
    }
}
