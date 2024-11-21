package no.nav.ung.sak.domene.behandling.steg.innhentsaksopplysninger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.ung.sak.kompletthet.KompletthetModell;

import java.time.LocalDate;
import java.util.List;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.INREG_AVSL;

@BehandlingStegRef(value = INREG_AVSL)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class InnhentRegisteropplysningerResterendeOppgaverStegImpl implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private KompletthetModell kompletthetModell;

    InnhentRegisteropplysningerResterendeOppgaverStegImpl() {
        // for CDI proxy
    }

    @Inject
    public InnhentRegisteropplysningerResterendeOppgaverStegImpl(BehandlingRepository behandlingRepository,
                                                                 PersonopplysningTjeneste personopplysningTjeneste,
                                                                 KompletthetModell kompletthetModell) {

        this.behandlingRepository = behandlingRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.kompletthetModell = kompletthetModell;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
        return BehandleStegResultat.utførtMedAksjonspunkter(sjekkSøkerUnder18År(ref));

    }

    private List<AksjonspunktDefinisjon> sjekkSøkerUnder18År(BehandlingReferanse ref) {
        if (erSøkerUnder18ar(ref)) {
            return List.of(AksjonspunktDefinisjon.AVKLAR_VERGE);
        }
        return List.of();
    }

    private boolean erSøkerUnder18ar(BehandlingReferanse ref) {
        PersonopplysningerAggregat personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref, ref.getFagsakPeriode().getFomDato());
        PersonopplysningEntitet søker = personopplysninger.getSøker();
        return søker.getFødselsdato().isAfter(LocalDate.now().minusYears(18));
    }

}
