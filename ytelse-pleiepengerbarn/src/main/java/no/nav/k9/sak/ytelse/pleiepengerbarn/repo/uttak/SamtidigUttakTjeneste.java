package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriode;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriodeRepository;
import no.nav.k9.sak.utsatt.UtsattPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.kjøreplan.Kjøreplan;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.kjøreplan.KjøreplanUtleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.MapInputTilUttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Endringsstatus;
import no.nav.pleiepengerbarn.uttak.kontrakter.Simulering;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;

/**
 * Håndtering av krav fra forskjellige søkere på den samme dagen.
 *
 * <h4>Regler for samtidig uttak</h4>
 *
 * <ol>
 *   <li>Et krav er definert som søknadsdata for en gitt dato.</li>
 *   <li>Kravet med det tidligste mottattidspunktet prioriteres.</li>
 *   <li>Et krav betyr at man har prioritet på 100% av gjenværende tid.</li>
 *   <li>Lavere prioriterte krav kan benytte gjenværende tid.</li>
 *   <li>Lavere prioriterte krav kan miste tiden hvis høyere prioriterte krav
 *       øker uttaket (gjennom etterrapportering, tilbakedatering av krav osv)</li>
 *   <li>Etter at en sak har blitt besluttet starter det automatisk revurdering
 *       av berørte saker</li>
 *   <li>Ved vurdering av gjenværende uttak baserer man seg på data fra
 *       besluttede behandlinger, men med en reberegning basert på gjeldende
 *       kravprioritet.</li>
 * </ol>
 *
 * <p>
 * Det vurderes som juridisk problematisk å tilbakekreve penger fra en lavere prioritert
 * søker grunnet etterrapportering fra den prioriterte søkeren. En slik tilbakekreving
 * vurderes likevel å være den beste løsningen ut fra kravet om at vi skal sikre at uttaket
 * ikke blir høyere enn pleiebehovet. Det jobbes for å sikre en god juridisk forankring av
 * denne praksisen.
 * </p>
 *
 * <p>
 * Merk at en alternativ løsning der en søker kan ha flere, ulikt prioriterte, krav på samme dag
 * har blitt forkastet som mulig løsning. Dette fordi den første søkeren skal ha mulighet til
 * etterrapportering uten fare for at man har mistet retten/prioriteten.
 * </p>
 *
 *
 * <h4>Om løsningen i denne klassen</h4>
 *
 * <p>
 * Denne tjenesten forsøker å hindre feilaktige vedtak, ved å kreve at behandlinger
 * med prioriterte perioder skal håndteres før behandlinger som har lavere prioritet.
 * </p>
 *
 * <p>
 * Merk at det kan være tilfeller der to behandlinger gjensidig har perioder som har
 * prioritet fremfor den andre. Dette håndteres ved at én av disse behandlingene blir
 * besluttet med for mye utbetalt. Etter at den andre behandlingen har blitt besluttet
 * vil det automatisk bli opprettet en revurdering som korrigerer dette.
 * </p>
 *
 * <p>
 * En bedre løsning ville vært å utsette behandling av perioder der man ikke har prioritet
 * til en senere behandling, men dette har ikke blitt implementert ennå.
 * </p>
 *
 * @see PleietrengendeKravprioritet#vurderKravprioritet
 * @see no.nav.k9.sak.ytelse.pleiepengerbarn.foreslåvedtak.PSBYtelsespesifikkForeslåVedtak
 * @see no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett.VurderOmPleiepengerVedtakPåvirkerAndreSakerTjeneste
 */
@Dependent
public class SamtidigUttakTjeneste {

    private MapInputTilUttakTjeneste mapInputTilUttakTjeneste;
    private UttakTjeneste uttakTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingModellRepository behandlingModellRepository;
    private KjøreplanUtleder kjøreplanUtleder;
    private UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository;


    @Inject
    public SamtidigUttakTjeneste(MapInputTilUttakTjeneste mapInputTilUttakTjeneste,
                                 UttakTjeneste uttakTjeneste,
                                 BehandlingRepository behandlingRepository,
                                 BehandlingModellRepository behandlingModellRepository,
                                 KjøreplanUtleder kjøreplanUtleder,
                                 UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository) {
        this.mapInputTilUttakTjeneste = mapInputTilUttakTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.behandlingModellRepository = behandlingModellRepository;
        this.kjøreplanUtleder = kjøreplanUtleder;
        this.utsattBehandlingAvPeriodeRepository = utsattBehandlingAvPeriodeRepository;
    }

    public boolean isAnnenSakSomMåBehandlesFørst(BehandlingReferanse ref) {
        var kjøreplan = kjøreplanUtleder.utled(ref);
        var kanAktuellFagsakFortsette = kjøreplan.kanAktuellFagsakFortsette();
        // har ikke endring i utstatte perioder
        return !kanAktuellFagsakFortsette || harEndringIUtsattePerioder(ref, kjøreplan.getPerioderSomSkalUtsettes(ref.getFagsakId()));
    }

    private boolean harEndringIUtsattePerioder(BehandlingReferanse ref, NavigableSet<DatoIntervallEntitet> perioderSomSkalUtsettes) {
        var eksisterendeUtsattePerioder = utsattBehandlingAvPeriodeRepository.hentGrunnlag(ref.getBehandlingId())
            .map(UtsattBehandlingAvPeriode::getPerioder)
            .orElse(Set.of())
            .stream()
            .map(UtsattPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));

        return !eksisterendeUtsattePerioder.equals(perioderSomSkalUtsettes);
    }

    public boolean isSkalHaTilbakehopp(BehandlingReferanse ref) {
        if (!harKommetTilUttak(ref)) {
            return false;
        }

        final Behandling behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        final boolean harÅpentVenteaksjonspunkt = behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VENT_ANNEN_PSB_SAK).isPresent();

        final boolean annenSakSomMåBehandlesFørst = isAnnenSakSomMåBehandlesFørst(ref);
        if (annenSakSomMåBehandlesFørst && !harÅpentVenteaksjonspunkt) {
            // Send til steg for å sette på vent.
            return true;
        }

        if (!annenSakSomMåBehandlesFørst && harÅpentVenteaksjonspunkt) {
            // Fremtving at steget skal kjøres på nytt for å fjerne venting.
            return true;
        }

        // Det skal kun slippes én behandling gjennom.

        final Simulering simulering = simulerUttakKunBesluttet(ref);

        return simulering.getUttakplanEndret();
    }

    public boolean harKommetTilUttak(BehandlingReferanse ref) {
        final var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        final BehandlingStegType steg = behandling.getAktivtBehandlingSteg();
        final BehandlingModell modell = behandlingModellRepository.getModell(behandling.getType(), behandling.getFagsakYtelseType());
        return !modell.erStegAFørStegB(steg, BehandlingStegType.VURDER_UTTAK_V2);
    }

    private Simulering simulerUttak(BehandlingReferanse ref) {
        final Uttaksgrunnlag uttaksgrunnlag = mapInputTilUttakTjeneste.hentUtUbesluttededataOgMapRequest(ref);

        return uttakTjeneste.simulerUttaksplan(uttaksgrunnlag);
    }

    private Simulering simulerUttakKunBesluttet(BehandlingReferanse ref) {
        final Uttaksgrunnlag uttaksGrunnlag = mapInputTilUttakTjeneste.hentUtOgMapRequest(ref);

        return uttakTjeneste.simulerUttaksplan(uttaksGrunnlag);
    }

    public NavigableSet<DatoIntervallEntitet> perioderMedEndringerMedUbesluttedeData(BehandlingReferanse ref) {
        final Simulering simulering = simulerUttak(ref);
        // Hvis en sak ikke har kommet til uttak betyr det at true returneres her.
        if (simulering.getUttakplanEndret()) {
            return simulering.getSimulertUttaksplan().getPerioder().entrySet().stream()
                .filter(entry -> entry.getValue().getEndringsstatus() == Endringsstatus.ENDRET)
                .map(entry -> DatoIntervallEntitet.fraOgMedTilOgMed(entry.getKey().getFom(), entry.getKey().getTom()))
                .collect(Collectors.toCollection(TreeSet::new));
        }
        return new TreeSet<>();
    }

    public Kjøreplan utledPrioriteringsrekkefølge(BehandlingReferanse ref) {
        return kjøreplanUtleder.utled(ref);
    }
}
