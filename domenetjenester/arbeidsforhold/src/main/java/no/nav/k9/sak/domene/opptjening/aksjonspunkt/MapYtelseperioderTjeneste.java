package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.arbeidsforhold.Arbeidskategori;
import no.nav.k9.kodeverk.arbeidsforhold.RelatertYtelseTilstand;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.domene.iay.modell.*;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class MapYtelseperioderTjeneste {

    private static final OpptjeningAktivitetType UDEFINERT = OpptjeningAktivitetType.UDEFINERT;
    private static final String UTEN_ORGNR = "UTENORGNR";
    private final PåTversAvHelgErKantIKantVurderer påTversAvHelgErKantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();

    public MapYtelseperioderTjeneste() {
    }

    public static DatoIntervallEntitet hentUtDatoIntervall(Ytelse ytelse, YtelseAnvist ytelseAnvist) {
        LocalDate fom = ytelseAnvist.getAnvistFOM();
        if (Fagsystem.ARENA.equals(ytelse.getKilde()) && fom.isBefore(ytelse.getPeriode().getFomDato())) {
            // Kunne vært generell men er forsiktig pga at feil som gir fpsak-ytelser fom = siste uttaksperiode (er rettet)
            // OBS: TOM kan ikke justeres tilsvarende pga konvensjon rundt satsjustering ....
            fom = ytelse.getPeriode().getFomDato();
        }
        LocalDate tom = ytelseAnvist.getAnvistTOM();
        if (tom != null && !Tid.TIDENES_ENDE.equals(tom)) {
            if (Set.of(Fagsystem.INFOTRYGD, Fagsystem.K9SAK, Fagsystem.FPSAK, Fagsystem.VLSP).contains(ytelse.getKilde())
                && DayOfWeek.THURSDAY.getValue() < DayOfWeek.from(tom).getValue()) {
                tom = tom.plusDays((long) DayOfWeek.SUNDAY.getValue() - DayOfWeek.from(tom).getValue());
            }
            return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        }
        return DatoIntervallEntitet.fraOgMed(fom);
    }

    private static DatoIntervallEntitet slåSammenOverlappendeDatoIntervall(DatoIntervallEntitet periode1, DatoIntervallEntitet periode2) {
        LocalDate fom = periode1.getFomDato();
        if (periode2.getFomDato().isBefore(fom)) {
            fom = periode2.getFomDato();
        }
        LocalDate tom = periode2.getTomDato();
        if (periode1.getTomDato().isAfter(tom)) {
            tom = periode1.getTomDato();
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    public List<OpptjeningsperiodeForSaksbehandling> mapYtelsePerioder(BehandlingReferanse behandlingReferanse,
                                                                       DatoIntervallEntitet vilkårsPeriode,
                                                                       OpptjeningAktivitetVurdering vurderOpptjening,
                                                                       boolean skalIkkeTaMedEgenSak,
                                                                       YtelseFilter ytelseFilter) {
        List<OpptjeningsperiodeForSaksbehandling> ytelsePerioder = new ArrayList<>();
        ytelseFilter.getFiltrertYtelser().stream()
            .filter(ytelse -> !(Fagsystem.INFOTRYGD.equals(ytelse.getKilde()) && RelatertYtelseTilstand.ÅPEN.equals(ytelse.getStatus())))
            .filter(ytelse -> !skalIkkeTaMedEgenSak || !(ytelse.getKilde().equals(Fagsystem.K9SAK) && ytelse.getSaksnummer().equals(behandlingReferanse.getSaksnummer())))
            .filter(ytelse -> ytelse.getYtelseType().girOpptjeningsTid(behandlingReferanse.getFagsakYtelseType()))
            .forEach(behandlingRelaterteYtelse -> {
                List<OpptjeningsperiodeForSaksbehandling> periode = mapYtelseAnvist(behandlingRelaterteYtelse, behandlingReferanse, vilkårsPeriode, vurderOpptjening);
                ytelsePerioder.addAll(periode);
            });
        return slåSammenYtelsePerioder(ytelsePerioder);
    }

    private List<OpptjeningsperiodeForSaksbehandling> mapYtelseAnvist(Ytelse ytelse,
                                                                      BehandlingReferanse behandlingReferanse,
                                                                      DatoIntervallEntitet vilkårsPeriode,
                                                                      OpptjeningAktivitetVurdering vurderForSaksbehandling) {
        OpptjeningAktivitetType type = mapYtelseType(ytelse);
        List<OpptjeningsperiodeForSaksbehandling> ytelserAnvist = new ArrayList<>();
        List<YtelseStørrelse> grunnlagList = ytelse.getYtelseGrunnlag().map(YtelseGrunnlag::getYtelseStørrelse).orElse(Collections.emptyList());
        List<String> orgnumre = grunnlagList.stream()
            .map(ys -> ys.getOrgnr().orElse(null))
            .filter(Objects::nonNull)
            .toList();


        if (!ytelse.getYtelseAnvist().isEmpty()) {
            var ytelseAnvistAktivitetMapper = new YtelseAnvistAktivitetMapper(vurderForSaksbehandling, behandlingReferanse, vilkårsPeriode, type, ytelse);
            ytelse.getYtelseAnvist()
                .stream()
                .flatMap(ytelseAnvist -> ytelseAnvistAktivitetMapper.mapAnvisning(ytelseAnvist, orgnumre).stream())
                .forEach(ytelserAnvist::add);
        } else if (erDagpengerIVentetiden(ytelse, vilkårsPeriode)) {
            // Dagpenger i ventetiden skal gi opptjening på lik linje med utbetalte dagpenger
            var dagpengerIVentetiden = OpptjeningsperiodeForSaksbehandling.Builder.ny()
                .medPeriode(ytelse.getPeriode())
                .medOpptjeningAktivitetType(type)
                .medVurderingsStatus(vurderForSaksbehandling.vurderStatus(lagInput(type, behandlingReferanse, vilkårsPeriode, ytelse.getPeriode())));
            ytelserAnvist.add(dagpengerIVentetiden.build());
        }

        return ytelserAnvist;
    }

    /**
     * Avgjør om ytelse gjelder dagpenger i ventetiden
     * <p>
     * De tre første dagene av dagpengene kalles ventetid og gir ingen utbetaling av dagpenger.
     * Søker vil dermed ikke ha meldekort dersom oppstart av pleiepenger er i løpet av denne perioden.
     *
     * @param ytelse         Ytelse
     * @param vilkårsPeriode
     * @return Er dagpenger i ventetiden
     */
    private boolean erDagpengerIVentetiden(Ytelse ytelse, DatoIntervallEntitet vilkårsPeriode) {
        return ytelse.getYtelseType().equals(FagsakYtelseType.DAGPENGER)
            && ytelse.getYtelseAnvist().isEmpty()
            && ytelse.getPeriode().getFomDato().isBefore(vilkårsPeriode.getFomDato()) &&
            DatoIntervallEntitet.fraOgMedTilOgMed(ytelse.getPeriode().getFomDato(), vilkårsPeriode.getFomDato().minusDays(1)).antallArbeidsdager() <= 3;
    }


    private VurderStatusInput lagInput(OpptjeningAktivitetType type, BehandlingReferanse behandlingReferanse, DatoIntervallEntitet vilkårsPeriode, DatoIntervallEntitet periode) {
        var input = new VurderStatusInput(type, behandlingReferanse);
        input.setVilkårsperiode(vilkårsPeriode);
        input.setAktivitetPeriode(periode);
        return input;
    }

    public static OpptjeningAktivitetType mapYtelseType(Ytelse ytelse) {

        if (!FagsakYtelseType.RELATERT_YTELSE_TYPER_FOR_SØKER.contains(ytelse.getYtelseType())) {
            return UDEFINERT;
        }

        if (FagsakYtelseType.SYKEPENGER.equals(ytelse.getYtelseType())) {
            boolean harSPBasertPåDP = ytelse.getYtelseGrunnlag().flatMap(YtelseGrunnlag::getArbeidskategori)
                .stream().anyMatch(a -> Arbeidskategori.DAGPENGER.equals(a) || Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER.equals(a));
            if (harSPBasertPåDP) {
                return OpptjeningAktivitetType.SYKEPENGER_AV_DAGPENGER;
            }
            return OpptjeningAktivitetType.SYKEPENGER;
        }

        if (Set.of(FagsakYtelseType.PSB, FagsakYtelseType.PPN).contains(ytelse.getYtelseType())) {
            boolean harPSBBasertPåDP = ytelse.getYtelseGrunnlag().flatMap(YtelseGrunnlag::getArbeidskategori)
                .stream().anyMatch(a -> Arbeidskategori.DAGPENGER.equals(a) || Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER.equals(a));
            if (harPSBBasertPåDP) {
                return OpptjeningAktivitetType.PLEIEPENGER_AV_DAGPENGER;
            }
            return OpptjeningAktivitetType.PLEIEPENGER;
        }


        return OpptjeningAktivitetType.hentFraFagsakYtelseTyper()
            .getOrDefault(ytelse.getYtelseType(), Collections.singleton(UDEFINERT)).stream().findFirst().orElse(UDEFINERT);
    }

    private List<OpptjeningsperiodeForSaksbehandling> slåSammenYtelsePerioder(List<OpptjeningsperiodeForSaksbehandling> ytelser) {
        List<OpptjeningsperiodeForSaksbehandling> resultat = new ArrayList<>();
        if (ytelser.isEmpty()) {
            return resultat;
        }
        Map<Tuple<OpptjeningAktivitetType, String>, List<OpptjeningsperiodeForSaksbehandling>> sortering = ytelser.stream()
            .collect(Collectors.groupingBy(this::finnYtelseDiskriminator));
        sortering.forEach((key, value) -> resultat.addAll(slåSammenYtelsePerioderSammeType(value)));
        return resultat;
    }

    private Tuple<OpptjeningAktivitetType, String> finnYtelseDiskriminator(OpptjeningsperiodeForSaksbehandling ytelse) {
        String retOrgnr = ytelse.getArbeidsgiver() != null ? ytelse.getArbeidsgiver().getIdentifikator() : UTEN_ORGNR;
        return new Tuple<>(ytelse.getOpptjeningAktivitetType(), retOrgnr);
    }

    private List<OpptjeningsperiodeForSaksbehandling> slåSammenYtelsePerioderSammeType(List<OpptjeningsperiodeForSaksbehandling> ytelser) {
        if (ytelser.size() < 2) {
            return ytelser;
        }
        List<OpptjeningsperiodeForSaksbehandling> sorterFom = ytelser.stream()
            .sorted(Comparator.comparing(opfs -> opfs.getPeriode().getFomDato()))
            .toList();
        List<OpptjeningsperiodeForSaksbehandling> fusjonert = new ArrayList<>();

        Iterator<OpptjeningsperiodeForSaksbehandling> iterator = sorterFom.iterator();
        OpptjeningsperiodeForSaksbehandling prev = iterator.next();
        OpptjeningsperiodeForSaksbehandling next;
        while (iterator.hasNext()) {
            next = iterator.next();
            if (erKantIKantPåTversAvHelg(prev.getPeriode(), next.getPeriode()) && harSammeVurderingsstatus(prev, next)) {
                prev = slåSammenToPerioder(prev, next);
            } else {
                fusjonert.add(prev);
                prev = next;
            }
        }
        fusjonert.add(prev);
        return fusjonert;
    }

    private static boolean harSammeVurderingsstatus(OpptjeningsperiodeForSaksbehandling prev, OpptjeningsperiodeForSaksbehandling next) {
        return prev.getVurderingsStatus().equals(next.getVurderingsStatus());
    }

    boolean erKantIKantPåTversAvHelg(DatoIntervallEntitet periode1, DatoIntervallEntitet periode2) {
        return påTversAvHelgErKantIKantVurderer.erKantIKant(periode1, periode2);
    }

    private OpptjeningsperiodeForSaksbehandling slåSammenToPerioder(OpptjeningsperiodeForSaksbehandling opp1, OpptjeningsperiodeForSaksbehandling opp2) {
        return OpptjeningsperiodeForSaksbehandling.Builder.ny()
            .medPeriode(slåSammenOverlappendeDatoIntervall(opp1.getPeriode(), opp2.getPeriode()))
            .medOpptjeningAktivitetType(opp1.getOpptjeningAktivitetType())
            .medVurderingsStatus(opp1.getVurderingsStatus())
            .medArbeidsgiver(opp1.getArbeidsgiver())
            .medOpptjeningsnøkkel(opp1.getOpptjeningsnøkkel())
            .build();

    }
}
