package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.arbeidsforhold.Arbeidskategori;
import no.nav.k9.kodeverk.arbeidsforhold.RelatertYtelseTilstand;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseAnvist;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.iay.modell.YtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.YtelseStørrelse;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;

public class MapYtelseperioderTjeneste {

    private static final OpptjeningAktivitetType UDEFINERT = OpptjeningAktivitetType.UDEFINERT;
    private static final String UTEN_ORGNR = "UTENORGNR";

    public MapYtelseperioderTjeneste() {
    }

    private static DatoIntervallEntitet hentUtDatoIntervall(Ytelse ytelse, YtelseAnvist ytelseAnvist) {
        LocalDate fom = ytelseAnvist.getAnvistFOM();
        if (Fagsystem.ARENA.equals(ytelse.getKilde()) && fom.isBefore(ytelse.getPeriode().getFomDato())) {
            // Kunne vært generell men er forsiktig pga at feil som gir fpsak-ytelser fom = siste uttaksperiode (er rettet)
            // OBS: TOM kan ikke justeres tilsvarende pga konvensjon rundt satsjustering ....
            fom = ytelse.getPeriode().getFomDato();
        }
        LocalDate tom = ytelseAnvist.getAnvistTOM();
        if (tom != null && !Tid.TIDENES_ENDE.equals(tom)) {
            if (Set.of(Fagsystem.INFOTRYGD, Fagsystem.K9SAK, Fagsystem.FPSAK).contains(ytelse.getKilde())
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

    public List<OpptjeningsperiodeForSaksbehandling> mapYtelsePerioder(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag grunnlag, OpptjeningAktivitetVurdering vurderOpptjening, DatoIntervallEntitet opptjeningPeriode) {
        AktørId aktørId = behandlingReferanse.getAktørId();
        var filter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId)).før(opptjeningPeriode.getTomDato());
        List<OpptjeningsperiodeForSaksbehandling> ytelsePerioder = new ArrayList<>();
        filter.getFiltrertYtelser().stream()
            .filter(ytelse -> !(Fagsystem.INFOTRYGD.equals(ytelse.getKilde()) && RelatertYtelseTilstand.ÅPEN.equals(ytelse.getStatus())))
            .filter(ytelse -> !(ytelse.getKilde().equals(Fagsystem.K9SAK) && ytelse.getSaksnummer().equals(behandlingReferanse.getSaksnummer())))
            .filter(ytelse -> ytelse.getYtelseType().girOpptjeningsTid(behandlingReferanse.getFagsakYtelseType()))
            .forEach(behandlingRelaterteYtelse -> {
                List<OpptjeningsperiodeForSaksbehandling> periode = mapYtelseAnvist(behandlingRelaterteYtelse, behandlingReferanse, vurderOpptjening);
                ytelsePerioder.addAll(periode);
            });
        return slåSammenYtelsePerioder(ytelsePerioder);
    }

    private List<OpptjeningsperiodeForSaksbehandling> mapYtelseAnvist(Ytelse ytelse, BehandlingReferanse behandlingReferanse, OpptjeningAktivitetVurdering vurderForSaksbehandling) {
        OpptjeningAktivitetType type = mapYtelseType(ytelse);
        List<OpptjeningsperiodeForSaksbehandling> ytelserAnvist = new ArrayList<>();
        List<YtelseStørrelse> grunnlagList = ytelse.getYtelseGrunnlag().map(YtelseGrunnlag::getYtelseStørrelse).orElse(Collections.emptyList());
        List<String> orgnumre = grunnlagList.stream()
            .map(ys -> ys.getOrgnr().orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        ytelse.getYtelseAnvist().forEach(ytelseAnvist -> {
            var input = new VurderStatusInput(type, behandlingReferanse);
            if (orgnumre.isEmpty()) {
                OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
                    .medPeriode(hentUtDatoIntervall(ytelse, ytelseAnvist))
                    .medOpptjeningAktivitetType(type)
                    .medVurderingsStatus(vurderForSaksbehandling.vurderStatus(input));
                ytelserAnvist.add(builder.build());
            } else {
                orgnumre.forEach(orgnr -> {
                    OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
                        .medPeriode(hentUtDatoIntervall(ytelse, ytelseAnvist))
                        .medOpptjeningAktivitetType(type)
                        .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
                        .medOpptjeningsnøkkel(Opptjeningsnøkkel.forOrgnummer(orgnr))
                        .medVurderingsStatus(vurderForSaksbehandling.vurderStatus(input));
                    ytelserAnvist.add(builder.build());
                });
            }
        });
        return ytelserAnvist;
    }

    private OpptjeningAktivitetType mapYtelseType(Ytelse ytelse) {

        if (!FagsakYtelseType.RELATERT_YTELSE_TYPER_FOR_SØKER.contains(ytelse.getYtelseType())) {
            return OpptjeningAktivitetType.hentFraTemaUnderkategori()
                .getOrDefault(ytelse.getBehandlingsTema(), Collections.singleton(UDEFINERT)).stream().findFirst().orElse(UDEFINERT);
        }

        if (FagsakYtelseType.SYKEPENGER.equals(ytelse.getYtelseType())) {
            boolean harSPBasertPåDP = ytelse.getYtelseGrunnlag().flatMap(YtelseGrunnlag::getArbeidskategori)
                .stream().anyMatch(a -> Arbeidskategori.DAGPENGER.equals(a) || Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER.equals(a));
            if (harSPBasertPåDP) {
                return OpptjeningAktivitetType.SYKEPENGER_AV_DAGPENGER;
            }
            return OpptjeningAktivitetType.SYKEPENGER;
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
        String retOrgnr = ytelse.getOrgnr() != null ? ytelse.getOrgnr() : UTEN_ORGNR;
        return new Tuple<>(ytelse.getOpptjeningAktivitetType(), retOrgnr);
    }

    private List<OpptjeningsperiodeForSaksbehandling> slåSammenYtelsePerioderSammeType(List<OpptjeningsperiodeForSaksbehandling> ytelser) {
        if (ytelser.size() < 2) {
            return ytelser;
        }
        List<OpptjeningsperiodeForSaksbehandling> sorterFom = ytelser.stream()
            .sorted(Comparator.comparing(opfs -> opfs.getPeriode().getFomDato()))
            .collect(Collectors.toList());
        List<OpptjeningsperiodeForSaksbehandling> fusjonert = new ArrayList<>();

        Iterator<OpptjeningsperiodeForSaksbehandling> iterator = sorterFom.iterator();
        OpptjeningsperiodeForSaksbehandling prev = iterator.next();
        OpptjeningsperiodeForSaksbehandling next;
        while (iterator.hasNext()) {
            next = iterator.next();
            if (erKantIKantPåTversAvHelg(prev.getPeriode(), next.getPeriode())) {
                prev = slåSammenToPerioder(prev, next);
            } else {
                fusjonert.add(prev);
                prev = next;
            }
        }
        fusjonert.add(prev);
        return fusjonert;
    }

    boolean erKantIKantPåTversAvHelg(DatoIntervallEntitet periode1, DatoIntervallEntitet periode2) {
        return utledTomDato(periode1).equals(utledFom(periode2).minusDays(1)) || utledTomDato(periode2).equals(utledFom(periode1).minusDays(1));
    }

    private LocalDate utledFom(DatoIntervallEntitet periode1) {
        var fomDato = periode1.getFomDato();
        if (DayOfWeek.SATURDAY.equals(fomDato.getDayOfWeek())) {
            return fomDato.plusDays(2);
        } else if (DayOfWeek.SUNDAY.equals(fomDato.getDayOfWeek())) {
            return fomDato.plusDays(1);
        }
        return fomDato;
    }

    private LocalDate utledTomDato(DatoIntervallEntitet periode1) {
        var tomDato = periode1.getTomDato();
        if (DayOfWeek.FRIDAY.equals(tomDato.getDayOfWeek())) {
            return tomDato.plusDays(2);
        } else if (DayOfWeek.SATURDAY.equals(tomDato.getDayOfWeek())) {
            return tomDato.plusDays(1);
        }
        return tomDato;
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
