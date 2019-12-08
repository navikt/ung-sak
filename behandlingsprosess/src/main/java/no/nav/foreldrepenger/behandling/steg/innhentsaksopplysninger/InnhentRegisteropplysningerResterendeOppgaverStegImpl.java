package no.nav.foreldrepenger.behandling.steg.innhentsaksopplysninger;


import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.behandling.steg.kompletthet.VurderKompletthetStegFelles.autopunktAlleredeUtført;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunkt;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetModell;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.util.FPDateUtil;


@BehandlingStegRef(kode = "INREG_AVSL")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class InnhentRegisteropplysningerResterendeOppgaverStegImpl implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private KompletthetModell kompletthetModell;

    InnhentRegisteropplysningerResterendeOppgaverStegImpl() {
        // for CDI proxy
    }

    @Inject
    public InnhentRegisteropplysningerResterendeOppgaverStegImpl(BehandlingRepository behandlingRepository,
                                                                   PersonopplysningTjeneste personopplysningTjeneste,
                                                                   KompletthetModell kompletthetModell,
                                                                   SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {

        this.behandlingRepository = behandlingRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.kompletthetModell = kompletthetModell;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);

        KompletthetResultat etterlysIM = kompletthetModell.vurderKompletthet(ref, List.of(AUTO_VENT_ETTERLYST_INNTEKTSMELDING));
        if (!etterlysIM.erOppfylt()) {
            // Dette autopunktet har tilbakehopp/gjenopptak. Går ut av steget hvis auto utført før frist (manuelt av vent). Utført på/etter frist antas automatisk gjenopptak.
            if (!etterlysIM.erFristUtløpt() && !autopunktAlleredeUtført(AUTO_VENT_ETTERLYST_INNTEKTSMELDING, behandling)) {
                return BehandleStegResultat.utførtMedAksjonspunktResultater(singletonList(opprettForAksjonspunkt(AUTO_VENT_ETTERLYST_INNTEKTSMELDING)));
            }
        }

        return BehandleStegResultat.utførtMedAksjonspunkter(sjekkPersonstatus(ref));

    }

    // TODO(OJR) flytte denne til egen utleder?
    private List<AksjonspunktDefinisjon> sjekkPersonstatus(BehandlingReferanse ref) {
        List<PersonstatusType> liste = asList(PersonstatusType.BOSA, PersonstatusType.DØD, PersonstatusType.DØDD, PersonstatusType.UTVA, PersonstatusType.ADNR);

        PersonopplysningerAggregat personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);

        List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = new ArrayList<>();
        for (PersonstatusEntitet personstatus : personopplysninger.getPersonstatuserFor(ref.getAktørId())) {
            if (!liste.contains(personstatus.getPersonstatus())) {
                aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS);
                break; // Trenger ikke loope mer når vi får aksjonspunkt
            }
        }

        if (erSøkerUnder18ar(ref)) {
            aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.AVKLAR_VERGE);
        }
        return aksjonspunktDefinisjoner;
    }

    private boolean erSøkerUnder18ar(BehandlingReferanse ref) {
        PersonopplysningerAggregat personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);
        PersonopplysningEntitet søker = personopplysninger.getSøker();
        return søker.getFødselsdato().isAfter(FPDateUtil.iDag().minusYears(18));
    }

}

