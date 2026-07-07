package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.EtterlysningOppretter;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll.KontrollerInntektEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.maksdato.MaksdatoEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.ProgramperiodeendringEtterlysningTjeneste;

import java.util.List;

@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@ApplicationScoped
public class UngEtterlysningOppretter implements EtterlysningOppretter {

    private KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste;
    private ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste;
    private MaksdatoEtterlysningTjeneste maksdatoEtterlysningTjeneste;
    private BehandlingRepository behandlingRepository;

    public UngEtterlysningOppretter() {
    }

    @Inject
    public UngEtterlysningOppretter(KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste, ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste, MaksdatoEtterlysningTjeneste maksdatoEtterlysningTjeneste, BehandlingRepository behandlingRepository) {
        this.kontrollerInntektEtterlysningTjeneste = kontrollerInntektEtterlysningTjeneste;
        this.programperiodeendringEtterlysningTjeneste = programperiodeendringEtterlysningTjeneste;
        this.maksdatoEtterlysningTjeneste = maksdatoEtterlysningTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void opprettEtterlysninger(BehandlingReferanse behandlingReferanse) {
        var årsaker = behandlingRepository.hentBehandling(behandlingReferanse.getBehandlingId()).getBehandlingÅrsakerTyper();

        // Rene varsel-om-opphør-ved-maksdato-behandlinger skal kun varsle om opphør, og ikke trigge
        // inntektskontroll eller programperiodeendring-varsling som del av opphørsløpet.
        if (erRentVarselOpphørVedMaksdatoLøp(årsaker)) {
            maksdatoEtterlysningTjeneste.opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(behandlingReferanse);
            return;
        }

        if (årsaker.contains(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM)) {
            // Opphevelse av opphør gjør en eventuell ventende uttalelse om endret sluttdato irrelevant, siden
            // registerendringen fjerner sluttdatoen (maksdato er uendret). Dette gjelder også dersom behandlingen
            // (fordi opphevOpphør-hendelsen er slått sammen med en fortsatt åpen behandling som venter på
            // bekreftelse av det nå opphevede opphøret — se OpprettRevurderingEllerOpprettDiffTask) fortsatt har
            // den utdaterte årsaken RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM liggende igjen. Vi avbryter derfor alltid
            // slike sluttdato-etterlysninger her, uavhengig av hvilke andre årsaker som finnes. Etterlysninger om
            // endret startdato/periode berøres ikke, da opphevelse av opphør ikke sier noe om disse.
            programperiodeendringEtterlysningTjeneste.avbrytVentendeSluttdatoEtterlysninger(behandlingReferanse);

            // Rene opphevelse-av-opphør-behandlinger (eventuelt sammen med den nå utdaterte opphør-årsaken)
            // skal ikke etterlyse noe fra bruker: bruker har allerede vært i kontakt med Nav i forkant
            // (f.eks. medhold i klage), og skal derfor ikke varsles på nytt om at programperioden er gjenåpnet.
            if (erRentOpphørOpphevetLøp(årsaker)) {
                return;
            }

            kontrollerInntektEtterlysningTjeneste.opprettEtterlysninger(behandlingReferanse);
            maksdatoEtterlysningTjeneste.opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(behandlingReferanse);
            return;
        }

        kontrollerInntektEtterlysningTjeneste.opprettEtterlysninger(behandlingReferanse);
        programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
        maksdatoEtterlysningTjeneste.opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(behandlingReferanse);
    }

    private boolean erRentVarselOpphørVedMaksdatoLøp(List<BehandlingÅrsakType> årsaker) {
        return !årsaker.isEmpty() && årsaker.stream().allMatch(å -> å == BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO);
    }

    /**
     * "Rent løp" for opphevelse av opphør betyr at eneste (relevante) årsak er opphevelsen selv. Den nå utdaterte
     * årsaken for det opprinnelige opphøret (RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM) regnes ikke som en reell
     * tilleggsårsak i denne sammenhengen — den er kun et artefakt av at de to hendelsene kan bli slått sammen på
     * samme åpne behandling (se OpprettRevurderingEllerOpprettDiffTask). Andre årsaker (f.eks. inntektskontroll)
     * regnes derimot fortsatt som reelle tilleggsårsaker.
     */
    private boolean erRentOpphørOpphevetLøp(List<BehandlingÅrsakType> årsaker) {
        return årsaker.stream().allMatch(å -> å == BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM
            || å == BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
    }
}
