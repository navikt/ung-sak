package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll.KontrollerInntektEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.OpphørVedMaksdatoEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.ProgramperiodeendringEtterlysningTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Orkestrerer opprettelse og avlysning av etterlysninger for ungdomsytelse basert på grunnlagsdata.
 *
 * Beslutninger baseres på faktisk grunnlagstilstand (UngdomsprogramPeriodeGrunnlag) fremfor
 * behandlingsårsaker, slik at resultatet er korrekt uavhengig av rekkefølgen prosesstasker kjører i.
 *
 * Håndterer scenarioer:
 * 1. Varsel om opphør ved maksdato: kun hvis perioden fortsatt er åpen og ikke forlenget
 * 2. Forlenget periode eller opphør overstyrer varsel → avbryt varsel, kjør normal flow
 * 3. Normal flow → inntektskontroll + programperiodeendring
 */
@ApplicationScoped
public class UngEtterlysningsOrkestreringTjeneste {

    private static final Logger log = LoggerFactory.getLogger(UngEtterlysningsOrkestreringTjeneste.class);

    private OpphørVedMaksdatoEtterlysningTjeneste opphørVedMaksdatoEtterlysningTjeneste;
    private KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste;
    private ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    public UngEtterlysningsOrkestreringTjeneste() {
    }

    @Inject
    public UngEtterlysningsOrkestreringTjeneste(OpphørVedMaksdatoEtterlysningTjeneste opphørVedMaksdatoEtterlysningTjeneste,
                                                KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste,
                                                ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste,
                                                UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.opphørVedMaksdatoEtterlysningTjeneste = opphørVedMaksdatoEtterlysningTjeneste;
        this.kontrollerInntektEtterlysningTjeneste = kontrollerInntektEtterlysningTjeneste;
        this.programperiodeendringEtterlysningTjeneste = programperiodeendringEtterlysningTjeneste;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    /**
     * Orkestrerer etterlysninger basert på grunnlagsdata og behandlingsårsaker.
     *
     * Grunnlaget er kilde til sannhet for forlenget periode og opphør.
     * Behandlingsårsaker brukes kun for å vite om RE_VARSEL_OPPHOR_VED_MAKSDATO er trigget,
     * men den faktiske beslutningen om hva som skal gjøres baseres på grunnlagstilstanden.
     *
     * @param behandlingReferanse referanse til behandlingen
     * @param årsaker alle behandlingsårsaker for behandlingen
     */
    public void orkestrerEtterlysninger(BehandlingReferanse behandlingReferanse, Collection<BehandlingÅrsakType> årsaker) {
        boolean harVarselOpphørVedMaksdato = årsaker.contains(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO);

        // Hent tilstand fra grunnlag — dette er kilde til sannhet
        var grunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingReferanse.getBehandlingId());
        boolean harForlengetPeriode = grunnlag.map(UngdomsprogramPeriodeGrunnlag::harForlengetPeriode).orElse(false);
        boolean harOpphør = harOpphørtProgramperiode(grunnlag);

        if (harVarselOpphørVedMaksdato && (harForlengetPeriode || harOpphør)) {
            // Grunnlaget viser at perioden er forlenget eller opphørt — varsel er ikke lenger relevant
            log.info("Behandling med RE_VARSEL_OPPHOR_VED_MAKSDATO, men grunnlag viser forlenget={} opphør={}. Avbryter varsel-etterlysning.",
                harForlengetPeriode, harOpphør);
            opphørVedMaksdatoEtterlysningTjeneste.avbrytEtterlysningForOpphørVedMaksdato(behandlingReferanse);
            kjørNormalEtterlysningsflyt(behandlingReferanse, harForlengetPeriode, harOpphør);
        } else if (harVarselOpphørVedMaksdato) {
            // Grunnlaget bekrefter at perioden fortsatt er åpen og ikke forlenget — varsel er relevant
            log.info("Behandling med RE_VARSEL_OPPHOR_VED_MAKSDATO. Grunnlag bekrefter åpen periode uten forlengelse. Oppretter varsel-etterlysning.");
            opphørVedMaksdatoEtterlysningTjeneste.opprettEtterlysningForOpphørVedMaksdato(behandlingReferanse);
        } else {
            // Normal flyt
            log.info("Normal etterlysningsflyt (forlenget={}, opphør={}). Oppretter inntektskontroll og programperiodeendring-etterlysninger.",
                harForlengetPeriode, harOpphør);
            kjørNormalEtterlysningsflyt(behandlingReferanse, harForlengetPeriode, harOpphør);
        }
    }

    /** Sjekker om programperioden er opphørt basert på grunnlagsdata (tom != TIDENES_ENDE). */
    private boolean harOpphørtProgramperiode(java.util.Optional<UngdomsprogramPeriodeGrunnlag> grunnlag) {
        return grunnlag
            .map(gr -> gr.getUngdomsprogramPerioder().getPerioder().stream()
                .noneMatch(p -> Tid.TIDENES_ENDE.equals(p.getPeriode().getTomDato())))
            .orElse(false);
    }

    /** Kjører normal etterlysningsflyt: inntektskontroll + programperiodeendring.
     * Programperiodeendring-etterlysning hoppes over for forlenget periode UTEN opphør,
     * da forlengelsen i seg selv ikke er en endring som krever brukervarsel.
     * Når det er opphør (endelig sluttdato satt), skal programperiodeendring alltid sjekkes. */
    private void kjørNormalEtterlysningsflyt(BehandlingReferanse behandlingReferanse, boolean harForlengetPeriode, boolean harOpphør) {
        kontrollerInntektEtterlysningTjeneste.opprettEtterlysninger(behandlingReferanse);
        if (!harForlengetPeriode || harOpphør) {
            programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
        }
    }
}

