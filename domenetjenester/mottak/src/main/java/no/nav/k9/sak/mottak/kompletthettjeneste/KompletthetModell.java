package no.nav.k9.sak.mottak.kompletthettjeneste;

import static java.util.stream.Collectors.toList;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD;
import static no.nav.vedtak.util.Objects.check;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;

@ApplicationScoped
public class KompletthetModell {

    private static Map<AksjonspunktDefinisjon, BiFunction<KompletthetModell, BehandlingReferanse, KompletthetResultat>> KOMPLETTHETSFUNKSJONER;

    static {
        Map<AksjonspunktDefinisjon, BiFunction<KompletthetModell, BehandlingReferanse, KompletthetResultat>> map = new EnumMap<>(AksjonspunktDefinisjon.class);
        map.put(AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, (kontroller, ref) -> finnKompletthetssjekker(kontroller, ref).vurderForsendelseKomplett(ref));
        map.put(VENT_PGA_FOR_TIDLIG_SØKNAD, (kontroller, ref) -> finnKompletthetssjekker(kontroller, ref).vurderSøknadMottattForTidlig(ref));
        map.put(AUTO_VENT_ETTERLYST_INNTEKTSMELDING, (kontroller, ref) -> finnKompletthetssjekker(kontroller, ref).vurderEtterlysningInntektsmelding(ref));

        KOMPLETTHETSFUNKSJONER = Collections.unmodifiableMap(map);
    }

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private KompletthetsjekkerProvider kompletthetsjekkerProvider;

    public KompletthetModell() {
        // For CDI proxy
    }

    @Inject
    public KompletthetModell(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                             KompletthetsjekkerProvider kompletthetsjekkerProvider) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.kompletthetsjekkerProvider = kompletthetsjekkerProvider;
    }

    private static Kompletthetsjekker finnKompletthetssjekker(KompletthetModell kompletthetModell, BehandlingReferanse ref) {
        return kompletthetModell.kompletthetsjekkerProvider.finnKompletthetsjekkerFor(ref.getFagsakYtelseType(), ref.getBehandlingType());
    }

    /**
     * Ranger autopunktene i kompletthetssjekk i samme rekkefølge som de ville ha blitt gjort i behandlingsstegene.
     * Dvs. at bruken av disse kompletthetssjekkene skjer UTENFOR behandlingsstegene, som introduserer risikoen for at
     * rekkefølgen avviker fra rekkefølgen INNE I behandlingsstegene. Bør være så enkel som mulig.
     **/
    // Rangering 1: Tidligste steg (dvs. autopunkt ville blitt eksekvert tidligst i behandlingsstegene)
    public List<AksjonspunktDefinisjon> rangerKompletthetsfunksjonerKnyttetTilAutopunkt(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        Comparator<AksjonspunktDefinisjon> stegRekkefølge = (apDef1, apDef2) ->
            behandlingskontrollTjeneste.sammenlignRekkefølge(ytelseType, behandlingType, apDef1.getBehandlingSteg(), apDef2.getBehandlingSteg());
        // Rangering 2: Autopunkt som kjøres igjen ved gjenopptakelse blir eksekvert FØR ikke-gjenopptagende i samme behandlingssteg
        //      Det er bare en implisitt antakelse at kodes riktig i stegene der Autopunkt brukes; bør forbedre dette.
        Comparator<AksjonspunktDefinisjon> tilbakehoppRekkefølge = (apDef1, apDef2) ->
            Boolean.compare(apDef1.tilbakehoppVedGjenopptakelse(), apDef2.tilbakehoppVedGjenopptakelse());

        return KOMPLETTHETSFUNKSJONER.keySet().stream()
            .sorted(stegRekkefølge
                .thenComparing(tilbakehoppRekkefølge.reversed()))
            .collect(toList());
    }

    public boolean erKompletthetssjekkPassert(Long behandlingId) {
        return behandlingskontrollTjeneste.erStegPassert(behandlingId, BehandlingStegType.VURDER_KOMPLETTHET);
    }

    public KompletthetResultat vurderKompletthet(BehandlingReferanse ref, List<AksjonspunktDefinisjon> åpneAksjonspunkter) {
        Optional<AksjonspunktDefinisjon> åpentAutopunkt = åpneAksjonspunkter.stream()
            .findFirst();
        if (åpentAutopunkt.map(this::erAutopunktTilknyttetKompletthetssjekk).orElse(false)) {
            return vurderKompletthet(ref, åpentAutopunkt.get());
        }
        if (!erKompletthetssjekkPassert(ref.getBehandlingId())) {
            // Kompletthetssjekk er ikke passert, men står heller ikke på autopunkt tilknyttet kompletthet som skal sjekkes
            return KompletthetResultat.oppfylt();
        }
        // Default dersom ingen match på åpent autopunkt tilknyttet kompletthet OG kompletthetssjekk er passert
        AksjonspunktDefinisjon defaultAutopunkt = finnSisteAutopunktKnyttetTilKompletthetssjekk(ref);
        return vurderKompletthet(ref, defaultAutopunkt);
    }

    private boolean erAutopunktTilknyttetKompletthetssjekk(AksjonspunktDefinisjon åpentAutopunkt) {
        return KOMPLETTHETSFUNKSJONER.containsKey(åpentAutopunkt);
    }

    private AksjonspunktDefinisjon finnSisteAutopunktKnyttetTilKompletthetssjekk(BehandlingReferanse ref) {
        List<AksjonspunktDefinisjon> rangerteAutopunkter = rangerKompletthetsfunksjonerKnyttetTilAutopunkt(ref.getFagsakYtelseType(), ref.getBehandlingType());
        check(rangerteAutopunkter.size() > 0, "Utvklerfeil: Skal alltid finnes kompletthetsfunksjoner"); //$NON-NLS-1$
        // Hent siste
        return rangerteAutopunkter.get(rangerteAutopunkter.size() - 1);
    }

    public KompletthetResultat vurderKompletthet(BehandlingReferanse ref, AksjonspunktDefinisjon autopunkt) {
        BiFunction<KompletthetModell, BehandlingReferanse, KompletthetResultat> kompletthetsfunksjon = KOMPLETTHETSFUNKSJONER.get(autopunkt);
        if (kompletthetsfunksjon == null) {
            throw new IllegalStateException("Utviklerfeil: Kan ikke finne kompletthetsfunksjon for autopunkt: " + autopunkt.getKode());
        }
        return kompletthetsfunksjon.apply(this, ref);
    }
}
