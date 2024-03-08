package no.nav.k9.sak.ytelse.pleiepengerbarn.foreslåvedtak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
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

    private K9FormidlingKlient formidlingKlient;
    private PunsjRestKlient punsjKlient;

    private boolean åpenPunsjoppgaveStopperAutomatiskVedtak;

    PleiepengerForeslåVedtakManueltUtleder() {
        //for CDI proxy
    }

    @Inject
    public PleiepengerForeslåVedtakManueltUtleder(K9FormidlingKlient formidlingKlient, PunsjRestKlient punsjKlient,
                                                  @KonfigVerdi(value = "AAPEN_PUNSJOPPGAVE_STOPPER_AUTOMATISK_VEDTAK", defaultVerdi = "false") boolean åpenPunsjoppgaveStopperAutomatiskVedtak) {
        this.formidlingKlient = formidlingKlient;
        this.punsjKlient = punsjKlient;
        this.åpenPunsjoppgaveStopperAutomatiskVedtak = åpenPunsjoppgaveStopperAutomatiskVedtak;
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
        if (!åpenPunsjoppgaveStopperAutomatiskVedtak) {
            return false;
        }
        var uferdigJournalpostIderPåAktør = punsjKlient.getUferdigJournalpostIderPåAktør(behandling.getAktørId().getAktørId(), behandling.getFagsak().getPleietrengendeAktørId().getAktørId());
        return uferdigJournalpostIderPåAktør.isPresent();
    }
}
