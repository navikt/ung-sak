package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class AksjonspunktutlederForAlder implements AksjonspunktUtleder {

    private static final Logger log = LoggerFactory.getLogger(AksjonspunktutlederForAlder.class);

    private PersonopplysningTjeneste personopplysningTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private boolean autopunktEnabled;

    AksjonspunktutlederForAlder() {
        // CDI
    }

    @Inject
    public AksjonspunktutlederForAlder(PersonopplysningTjeneste personopplysningTjeneste,
                                       VilkårResultatRepository vilkårResultatRepository,
                                       @KonfigVerdi(value = "AKSJONSPUNKT_7035_ENABLED", defaultVerdi = "false", required = false) boolean autopunktEnabled) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.autopunktEnabled = autopunktEnabled;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        var oppgittFravær = vilkårResultatRepository.hentHvisEksisterer(param.getBehandlingId());
        if (oppgittFravær.isPresent()) {
            var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(param.getRef());
            var søker = personopplysningerAggregat.getSøker();
            var perioder = oppgittFravær.get().getVilkår(VilkårType.OPPTJENINGSVILKÅRET).orElseThrow().getPerioder().stream().map(VilkårPeriode::getFom).collect(Collectors.toSet());
            if (perioder.stream().anyMatch(dato -> erBruker70ÅrVed(dato, søker))) {
                return List
                    .of(AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_VENT_BRUKER_70_ÅR, Venteårsak.BRUKER_70ÅR_VED_REFUSJON, LocalDateTime.now().plusMonths(6)));
            }
        }
        return List.of();
    }

    private boolean erBruker70ÅrVed(LocalDate dato, PersonopplysningEntitet søker) {
        final var between = Period.between(søker.getFødselsdato(), dato);
        if (between.getYears() > 69) {
            if (!autopunktEnabled) {
                log.info("Dropper venting/aksjonspunkt 7035 - 'vent bruker over 70 år', ved opptjeningdato: {}", dato);
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

}
