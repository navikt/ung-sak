package no.nav.k9.sak.ytelse.omsorgspenger.produksjonsstyring;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.produksjonsstyring.UtvidetSøknadÅrsak;
import no.nav.k9.kodeverk.uttak.SøknadÅrsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.hendelse.produksjonsstyring.søknadsårsak.SøknadsårsakUtleder;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist.SøknadPerioderTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
public class OmsorgspengerSøknadsårsakUtleder implements SøknadsårsakUtleder {
    private static final Logger logger = LoggerFactory.getLogger(OmsorgspengerSøknadsårsakUtleder.class);
    private SøknadPerioderTjeneste søknadPerioderTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    public OmsorgspengerSøknadsårsakUtleder() {
        //for CDI proxy
    }

    @Inject
    public OmsorgspengerSøknadsårsakUtleder(SøknadPerioderTjeneste søknadPerioderTjeneste,
                                            @FagsakYtelseTypeRef(OMSORGSPENGER) @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.søknadPerioderTjeneste = søknadPerioderTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    /**
     * Lister ut alle søknadsårsaker for søknad fra bruker som overlapper med periodene på behandlingen. I tillegg til oppgitte søknadsårsaker, utledes årsakene
     * SN (selvstendig næringsdrivende) og FL (frilanser); dette for å kunne bruke i kø-filter i los selv om årsak ikke oppgis spesifikt i søknaden for disse.
     */
    @Override
    public List<UtvidetSøknadÅrsak> utledSøknadÅrsaker(Behandling behandling) {
        LocalDateTimeline<?> behandlingensPerioder = TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurderingTjeneste.utledFraDefinerendeVilkår(behandling.getId()));
        LocalDateTimeline<Set<UtvidetSøknadÅrsak>> søknadsårsakerFagsak = finnSøknadsårsakerPåFagsak(behandling);
        LocalDateTimeline<Set<UtvidetSøknadÅrsak>> søknadsårsakerBehandling = søknadsårsakerFagsak.intersection(behandlingensPerioder, StandardCombinators::leftOnly);

        sanityCheck(søknadsårsakerBehandling);

        return søknadsårsakerBehandling.stream()
            .flatMap(s -> s.getValue().stream())
            .distinct()
            .toList();
    }

    private void sanityCheck(LocalDateTimeline<Set<UtvidetSøknadÅrsak>> søknadsårsaker) {
        LocalDateTimeline<Set<UtvidetSøknadÅrsak>> søktUtenÅrsak = søknadsårsaker.filterValue(Set::isEmpty);
        if (!søktUtenÅrsak.isEmpty()) {
            logger.warn("Har fraværPerioder fra bruker, men klarte ikke å utlede søknadsårsaker, for {}. Kan gjøre at behandlingen ikke kommer i alle forventede køer i los", søktUtenÅrsak);
        }
    }

    private LocalDateTimeline<Set<UtvidetSøknadÅrsak>> finnSøknadsårsakerPåFagsak(Behandling behandling) {
        List<LocalDateSegment<Set<UtvidetSøknadÅrsak>>> segmenter = søknadPerioderTjeneste.hentSøktePerioderMedKravdokumentPåFagsak(behandling.getFagsakId())
            .entrySet().stream()
            .filter(e -> e.getKey().getType() == KravDokumentType.SØKNAD)
            .flatMap(e -> e.getValue().stream())
            .map(SøktPeriode::getRaw)
            .map(fraværPeriode -> new LocalDateSegment<>(fraværPeriode.getFom(), fraværPeriode.getTom(), finnUtvidetSøknadÅrsak(fraværPeriode)))
            .toList();
        return new LocalDateTimeline<>(segmenter, TidslinjeUtil::union);
    }

    private Set<UtvidetSøknadÅrsak> finnUtvidetSøknadÅrsak(OppgittFraværPeriode fraværPeriode) {
        SøknadÅrsak oppgittSøknadÅrsak = fraværPeriode.getSøknadÅrsak();
        if (oppgittSøknadÅrsak != null && oppgittSøknadÅrsak != SøknadÅrsak.UDEFINERT) {
            return Set.of(map(oppgittSøknadÅrsak));
        }
        if (fraværPeriode.getAktivitetType() == UttakArbeidType.FRILANSER) {
            return Set.of(UtvidetSøknadÅrsak.FL);
        }
        if (fraværPeriode.getAktivitetType() == UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE) {
            return Set.of(UtvidetSøknadÅrsak.SN);
        }
        boolean trekkAvPeriode = Duration.ZERO.equals(fraværPeriode.getFraværPerDag());
        if (trekkAvPeriode) {
            logger.info("Har trekk av fraværPeriode fra bruker, klarte ikke å utlede søknadsårsak, for {}", fraværPeriode);
        } else {
            logger.warn("Har fraværPeriode fra bruker, men klarte ikke å utlede søknadsårsak, for {}. Kan gjøre at oppgaven ikke har forventet årsak, og dermed ikke kommer i alle forventede køer i los", fraværPeriode);
        }
        return Set.of();
    }

    private static UtvidetSøknadÅrsak map(SøknadÅrsak oppgittSøknadÅrsak) {
        return switch (oppgittSøknadÅrsak) {
            case ARBEIDSGIVER_KONKURS -> UtvidetSøknadÅrsak.ARBEIDSGIVER_KONKURS;
            case NYOPPSTARTET_HOS_ARBEIDSGIVER -> UtvidetSøknadÅrsak.NYOPPSTARTET_HOS_ARBEIDSGIVER;
            case KONFLIKT_MED_ARBEIDSGIVER -> UtvidetSøknadÅrsak.KONFLIKT_MED_ARBEIDSGIVER;
            case UDEFINERT -> throw new IllegalArgumentException("Ikke forvenet: " + oppgittSøknadÅrsak);
        };
    }
}
