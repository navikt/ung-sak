package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.uttak.OmsorgspengerUtfall;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.kontrakt.uttak.UttakArbeidsforhold;
import no.nav.k9.sak.kontrakt.uttak.UttaksperiodeOmsorgspenger;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumRequest;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumRestKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@ApplicationScoped
@BehandlingStegRef(kode = "VURDER_UTTAK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("OMP")
public class VurderÅrskvantumUttakSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste stpTjeneste;
    private ÅrskvantumTjeneste årskvantumTjeneste;


    VurderÅrskvantumUttakSteg() {
        // for proxy
    }

    @Inject
    public VurderÅrskvantumUttakSteg(BehandlingRepository behandlingRepository,
                                     SkjæringstidspunktTjeneste stpTjeneste,
                                     ÅrskvantumTjeneste årskvantumTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.stpTjeneste = stpTjeneste;
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var stp = stpTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, stp);


        var årskvantumResultat = årskvantumTjeneste.hentÅrskvantumUttak(ref);

        if (vurderUtfall(årskvantumResultat)) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        } else {
            //TODO 1 lage aksjonspunkt for manglende årskvantum.
            return BehandleStegResultat.settPåVent();

            //TODO 2 kan vi innvilge deler av periodene og avslå resten?
        }

        // K9 TODO:
        // 1. kalle årskvantum for å få vurdet fraværet
        //    a. hvis Ok gå videre uten aksjonspunkter
        //    b. hvis Ikke Ok opprett aksjonspunkt som må løses i dette steget (fosterforeldre, delt bosted, etc.)
        // 2. Lag REST tjeneste for GUI - vise hvor mye brukt (basert på samme tjeneste som kalles her
        // 3. Lag AksjonspunktOppdaterer for å skrive ned oppdatert kvantum til Årskvantum og la steget kjøre på nytt.

    }

    private boolean vurderUtfall(ÅrskvantumResultat årskvantumResultat) {
        return årskvantumResultat.getSamletUtfall() != null && OmsorgspengerUtfall.INNVILGET.equals(årskvantumResultat.getSamletUtfall())
            || årskvantumResultat.getSamletUtfall() == null && årskvantumResultat.getUttaksperioder().stream().anyMatch(it -> OmsorgspengerUtfall.INNVILGET.equals(it.getUtfall()));
    }

}
