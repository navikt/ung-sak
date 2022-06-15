package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.KompletthetFraværFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PSBVurdererSøknadsfristTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.PeriodeFraSøknadForBrukerTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.ArbeidstidMappingInput;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.MapArbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
public class PleiepengerKompletthetFraværFilter implements KompletthetFraværFilter {

    private PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste;
    private boolean markerFravær;
    private PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste;

    PleiepengerKompletthetFraværFilter() {
        // CDI
    }

    @Inject
    public PleiepengerKompletthetFraværFilter(@Any PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste,
                                              @KonfigVerdi(value = "kompletthet.marker.fravær", defaultVerdi = "false") boolean markerFravær,
                                              PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste) {
        this.søknadsfristTjeneste = søknadsfristTjeneste;
        this.markerFravær = markerFravær;
        this.periodeFraSøknadForBrukerTjeneste = periodeFraSøknadForBrukerTjeneste;
    }

    @Override
    public boolean harFraværFraArbeidetIPerioden(BehandlingReferanse ref,
                                                 DatoIntervallEntitet periode,
                                                 ManglendeVedlegg manglendeVedlegg) {

        var perioderFraSøknadene = periodeFraSøknadForBrukerTjeneste.hentPerioderFraSøknad(ref);
        var kravDokumenter = søknadsfristTjeneste.vurderSøknadsfrist(ref).keySet();
        var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periode.toLocalDateInterval(), true)));

        var arbeidstidInput = new ArbeidstidMappingInput(kravDokumenter,
            perioderFraSøknadene,
            timeline,
            null,
            null);
        var arbeidIPeriode = new MapArbeid().map(arbeidstidInput);

        var harFraværFraArbeidsgiverIPerioden = harFraværFraArbeidsgiverIPerioden(arbeidIPeriode, manglendeVedlegg);
        if (markerFravær) {
            manglendeVedlegg.setHarFraværFraArbeidsgiverIPerioden(harFraværFraArbeidsgiverIPerioden);
        }
        return harFraværFraArbeidsgiverIPerioden;
    }

    private boolean harFraværFraArbeidsgiverIPerioden(List<Arbeid> arbeidIPeriode, ManglendeVedlegg at) {
        return arbeidIPeriode.stream()
            .filter(it -> UttakArbeidType.ARBEIDSTAKER.equals(UttakArbeidType.fraKode(it.getArbeidsforhold().getType())))
            .anyMatch(it -> Objects.equals(at.getArbeidsgiver(), utledIdentifikator(it)) && harFravær(it.getPerioder()));
    }

    private Arbeidsgiver utledIdentifikator(Arbeid it) {
        if (it.getArbeidsforhold().getOrganisasjonsnummer() != null) {
            return Arbeidsgiver.virksomhet(it.getArbeidsforhold().getOrganisasjonsnummer());
        } else if (it.getArbeidsforhold().getAktørId() != null) {
            return Arbeidsgiver.fra(new AktørId(it.getArbeidsforhold().getAktørId()));
        }
        return null;
    }

    private boolean harFravær(Map<LukketPeriode, ArbeidsforholdPeriodeInfo> perioder) {
        return perioder.values().stream().anyMatch(it -> it.getJobberNormalt().compareTo(it.getJobberNå()) > 0);
    }

}
