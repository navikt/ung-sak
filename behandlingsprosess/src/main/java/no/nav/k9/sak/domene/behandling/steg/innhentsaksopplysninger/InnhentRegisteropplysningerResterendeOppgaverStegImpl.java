package no.nav.k9.sak.domene.behandling.steg.innhentsaksopplysninger;


import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING;
import static no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunkt;
import static no.nav.k9.sak.domene.behandling.steg.kompletthet.VurderKompletthetStegFelles.autopunktAlleredeUtført;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.kompletthet.KompletthetModell;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;


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
        if (!etterlysIM.erOppfylt() && erIkkeOmsorgspenger(ref)) {
            // Dette autopunktet har tilbakehopp/gjenopptak. Går ut av steget hvis auto utført før frist (manuelt av vent). Utført på/etter frist antas automatisk gjenopptak.
            if (!etterlysIM.erFristUtløpt() && !autopunktAlleredeUtført(AUTO_VENT_ETTERLYST_INNTEKTSMELDING, behandling)) {
                return BehandleStegResultat.utførtMedAksjonspunktResultater(singletonList(opprettForAksjonspunkt(AUTO_VENT_ETTERLYST_INNTEKTSMELDING)));
            }
        }

        return BehandleStegResultat.utførtMedAksjonspunkter(sjekkPersonstatus(ref));

    }

    private boolean erIkkeOmsorgspenger(BehandlingReferanse ref) {
        // FIXME : Finn noe bedre
        return !FagsakYtelseType.OMSORGSPENGER.equals(ref.getFagsakYtelseType());
    }

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
        return søker.getFødselsdato().isAfter(LocalDate.now().minusYears(18));
    }

}

