package no.nav.k9.sak.domene.behandling.steg.innhentsaksopplysninger;

import static java.util.Collections.singletonList;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING;
import static no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunkt;
import static no.nav.k9.sak.domene.behandling.steg.kompletthet.VurderKompletthetStegFelles.autopunktAlleredeUtført;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.kompletthet.KompletthetModell;
import no.nav.k9.sak.kompletthet.KompletthetResultat;

@BehandlingStegRef(kode = "INREG_AVSL")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class InnhentRegisteropplysningerResterendeOppgaverStegImpl implements BehandlingSteg {

    private static final Set<PersonstatusType> HÅNDTERTE_PERSONSTATUSER = EnumSet.of(PersonstatusType.BOSA, PersonstatusType.DØD, PersonstatusType.DØDD, PersonstatusType.UTVA, PersonstatusType.ADNR);

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
        KompletthetResultat etterlysIM = !autopunktAlleredeUtført(AUTO_VENT_ETTERLYST_INNTEKTSMELDING, behandling) ? kompletthetModell.vurderKompletthet(ref, List.of(AUTO_VENT_ETTERLYST_INNTEKTSMELDING)) : KompletthetResultat.oppfylt();
        if (!etterlysIM.erOppfylt()) {
            // Dette autopunktet har tilbakehopp/gjenopptak. Går ut av steget hvis auto utført før frist (manuelt av vent). Utført på/etter frist antas
            // automatisk gjenopptak.
            if (!etterlysIM.erFristUtløpt() && !autopunktAlleredeUtført(AUTO_VENT_ETTERLYST_INNTEKTSMELDING, behandling)) {
                return BehandleStegResultat.utførtMedAksjonspunktResultater(singletonList(opprettForAksjonspunkt(AUTO_VENT_ETTERLYST_INNTEKTSMELDING)));
            }
        }
        return BehandleStegResultat.utførtMedAksjonspunkter(sjekkPersonstatus(ref));

    }

    private List<AksjonspunktDefinisjon> sjekkPersonstatus(BehandlingReferanse ref) {
        var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref, ref.getFagsakPeriode().getFomDato());
        List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = new ArrayList<>();
        for (var personstatus : personopplysninger.getPersonstatuserFor(ref.getAktørId())) {
            if (!HÅNDTERTE_PERSONSTATUSER.contains(personstatus.getPersonstatus())) {
                throw new IllegalStateException("Personstatus ikke støttet i løsning ennå: " + personstatus.getPersonstatus());
            }
        }

        if (erSøkerUnder18ar(ref)) {
            aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.AVKLAR_VERGE);
        }
        return aksjonspunktDefinisjoner;
    }

    private boolean erSøkerUnder18ar(BehandlingReferanse ref) {
        PersonopplysningerAggregat personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref, ref.getFagsakPeriode().getFomDato());
        PersonopplysningEntitet søker = personopplysninger.getSøker();
        return søker.getFødselsdato().isAfter(LocalDate.now().minusYears(18));
    }

}
