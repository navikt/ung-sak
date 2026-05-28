package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll.KontrollerInntektEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.OpphørVedMaksdatoEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.ProgramperiodeendringEtterlysningTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Orkestrerer opprettelse og avlysning av etterlysninger for ungdomsytelse basert på behandlingsårsaker.
 *
 * Håndterer tre scenarioer:
 * 1. Varsel om opphør ved maksdato alene → opprett varsel-etterlysning
 * 2. Varsel om opphør ved maksdato + forlenget periode → avbryt varsel, kjør normal flow for forlenget periode
 * 3. Varsel om opphør ved maksdato + manuelt opphør → avbryt varsel, kjør normal flow for opphør
 * 0. Normal flow (ingen varsel-årsak) → inntektskontroll + programperiodeendring
 */
@ApplicationScoped
public class UngEtterlysningsorkestrerserTjeneste {

    private static final Logger log = LoggerFactory.getLogger(UngEtterlysningsorkestrerserTjeneste.class);

    private OpphørVedMaksdatoEtterlysningTjeneste opphørVedMaksdatoEtterlysningTjeneste;
    private KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste;
    private ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste;

    public UngEtterlysningsorkestrerserTjeneste() {
    }

    @Inject
    public UngEtterlysningsorkestrerserTjeneste(OpphørVedMaksdatoEtterlysningTjeneste opphørVedMaksdatoEtterlysningTjeneste,
                                                 KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste,
                                                 ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste) {
        this.opphørVedMaksdatoEtterlysningTjeneste = opphørVedMaksdatoEtterlysningTjeneste;
        this.kontrollerInntektEtterlysningTjeneste = kontrollerInntektEtterlysningTjeneste;
        this.programperiodeendringEtterlysningTjeneste = programperiodeendringEtterlysningTjeneste;
    }

    /**
     * Orkestrerer etterlysninger basert på behandlingsårsaker.
     * Bestemmer hvilke etterlysninger som skal opprettes, avbrytes eller modifiseres.
     *
     * @param behandlingReferanse referanse til behandlingen
     * @param årsaker alle behandlingsårsaker for behandlingen
     */
    public void orkestrerEtterlysninger(BehandlingReferanse behandlingReferanse, Collection<BehandlingÅrsakType> årsaker) {
        boolean harVarselOpphørVedMaksdato = årsaker.contains(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO);
        boolean harForlengetPeriode = årsaker.contains(BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM);
        boolean harOpphør = årsaker.contains(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);

        if (harVarselOpphørVedMaksdato && (harForlengetPeriode || harOpphør)) {
            // Varsel-årsaken overstyres av forlenget periode eller manuelt opphør
            log.info("Behandling med RE_VARSEL_OPPHOR_VED_MAKSDATO overstyres av annen årsak. Avbryter varsel-etterlysning.");
            opphørVedMaksdatoEtterlysningTjeneste.avbrytEtterlysningForOpphørVedMaksdato(behandlingReferanse);
            kjørNormalEtterlysningsflyt(behandlingReferanse, harForlengetPeriode);
        } else if (harVarselOpphørVedMaksdato) {
            // Kun varsel om opphør ved maksdato
            log.info("Behandling med RE_VARSEL_OPPHOR_VED_MAKSDATO alene. Oppretter varsel-etterlysning.");
            opphørVedMaksdatoEtterlysningTjeneste.opprettEtterlysningForOpphørVedMaksdato(behandlingReferanse);
        } else {
            // Normal flyt (ingen varsel-årsak)
            log.info("Normal etterlysningsflyt. Oppretter inntektskontroll og programperiodeendring-etterlysninger.");
            kjørNormalEtterlysningsflyt(behandlingReferanse, harForlengetPeriode);
        }
    }

    /** Kjører normal etterlysningsflyt: inntektskontroll + programperiodeendring (hvis ikke forlenget). */
    private void kjørNormalEtterlysningsflyt(BehandlingReferanse behandlingReferanse, boolean harForlengetPeriode) {
        kontrollerInntektEtterlysningTjeneste.opprettEtterlysninger(behandlingReferanse);
        if (!harForlengetPeriode) {
            programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
        }
    }
}

