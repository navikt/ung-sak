package no.nav.k9.sak.inngangsvilkår.opptjening;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetPeriode;
import no.nav.k9.sak.domene.opptjening.OpptjeningInntektPeriode;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.Aktivitet;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.Aktivitet.ReferanseType;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.AktivitetPeriode;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.Opptjeningsgrunnlag;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.Opptjeningsvilkår;

class OpptjeningsgrunnlagAdapter {
    private LocalDate behandlingstidspunkt;
    private LocalDate startDato;
    private LocalDate sluttDato;

    OpptjeningsgrunnlagAdapter(LocalDate behandlingstidspunkt, LocalDate startDato, LocalDate sluttDato) {
        this.behandlingstidspunkt = behandlingstidspunkt;
        this.startDato = startDato;
        this.sluttDato = sluttDato;
    }

    Opptjeningsgrunnlag mapTilGrunnlag(Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter,
                                              Collection<OpptjeningInntektPeriode> opptjeningInntekter) {
        Opptjeningsgrunnlag opptjeningsGrunnlag = new Opptjeningsgrunnlag(behandlingstidspunkt, startDato, sluttDato);

        // legger til alle rapporterte inntekter og aktiviteter hentet opp. håndterer duplikater/overlapp i
        // mellomregning.
        leggTilOpptjening(opptjeningAktiveter, opptjeningsGrunnlag);
        leggTilRapporterteInntekter(opptjeningInntekter, opptjeningsGrunnlag);

        return opptjeningsGrunnlag;
    }

    Opptjeningsgrunnlag mapTilGrunnlag(Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter,
                                       Collection<OpptjeningInntektPeriode> opptjeningInntekter,
                                       LocalDateTimeline<Boolean> aapTidslinje) {
        Objects.requireNonNull(aapTidslinje);
        Opptjeningsgrunnlag opptjeningsGrunnlag = new Opptjeningsgrunnlag(behandlingstidspunkt, startDato, sluttDato);

        // legger til alle rapporterte inntekter og aktiviteter hentet opp. håndterer duplikater/overlapp i
        // mellomregning.
        leggTilOpptjening(opptjeningAktiveter, opptjeningsGrunnlag);
        leggTilRapporterteInntekter(opptjeningInntekter, opptjeningsGrunnlag);
        LocalDateTimeline<Boolean> aapIOpptjeningsperiode = aapTidslinje.intersection(new LocalDateTimeline<>(startDato, sluttDato, true));
        opptjeningsGrunnlag.setAapPerioder(aapIOpptjeningsperiode.getLocalDateIntervals());
        return opptjeningsGrunnlag;
    }

    private void leggTilRapporterteInntekter(Collection<OpptjeningInntektPeriode> opptjeningInntekter,
                                             Opptjeningsgrunnlag opptjeningsGrunnlag) {
        for (OpptjeningInntektPeriode inn : opptjeningInntekter) {
            if (!InntektspostType.LØNN.equals(inn.getType())) {
                continue;
            }

            LocalDateInterval dateInterval = new LocalDateInterval(inn.getFraOgMed(), inn.getTilOgMed());
            long beløpHeltall = inn.getBeløp() == null ? 0L : inn.getBeløp().longValue();

            Opptjeningsnøkkel opptjeningsnøkkel = inn.getOpptjeningsnøkkel();

            ReferanseType refType = getAktivtetReferanseType(opptjeningsnøkkel.getType());

            if (refType != null) {
                Aktivitet aktivitet = new Aktivitet(Opptjeningsvilkår.ARBEID, opptjeningsnøkkel.getAktivitetReferanse(), refType);
                opptjeningsGrunnlag.leggTilRapportertInntekt(dateInterval, aktivitet, beløpHeltall);
            }
        }
    }

    private ReferanseType getAktivtetReferanseType(Opptjeningsnøkkel.Type type) {
        switch (type) {
            // skiller nå ikke på arbeidsforhold pr arbeidsgiver
            case ARBEIDSFORHOLD_ID:
            case ORG_NUMMER:
                return ReferanseType.ORGNR;
            case AKTØR_ID:
                return ReferanseType.AKTØRID;
            default:
                return null;
        }
    }

    private void leggTilOpptjening(Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter, Opptjeningsgrunnlag opptjeningsGrunnlag) {
        Collection<OpptjeningAktivitetPeriode> opptjeningAktiveterFiltrert = filtrer(opptjeningAktiveter);

        for (OpptjeningAktivitetPeriode opp : opptjeningAktiveterFiltrert) {
            LocalDateInterval dateInterval = new LocalDateInterval(opp.getPeriode().getFomDato(), opp.getPeriode().getTomDato());
            Opptjeningsnøkkel opptjeningsnøkkel = opp.getOpptjeningsnøkkel();
            if (opptjeningsnøkkel != null) {
                String identifikator = getIdentifikator(opp).getElement2();
                Aktivitet opptjeningAktivitet = new Aktivitet(opp.getOpptjeningAktivitetType().getKode(), identifikator, getAktivtetReferanseType(opptjeningsnøkkel.getArbeidsgiverType()));
                AktivitetPeriode aktivitetPeriode = new AktivitetPeriode(dateInterval, opptjeningAktivitet, mapStatus(opp));
                opptjeningsGrunnlag.leggTil(aktivitetPeriode);
            } else {
                Aktivitet opptjeningAktivitet = new Aktivitet(opp.getOpptjeningAktivitetType().getKode());
                AktivitetPeriode aktivitetPeriode = new AktivitetPeriode(dateInterval, opptjeningAktivitet, mapStatus(opp));
                opptjeningsGrunnlag.leggTil(aktivitetPeriode);
            }
        }
    }

    private Collection<OpptjeningAktivitetPeriode> filtrer(Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter) {
        List<OpptjeningAktivitetPeriode> utenNøkkel = opptjeningAktiveter.stream().filter(o -> o.getOpptjeningsnøkkel() == null).collect(Collectors.toList());
        //fjerner de uten opptjeningsnøkkel
        opptjeningAktiveter.removeAll(utenNøkkel);
        List<OpptjeningAktivitetPeriode> resultat = new ArrayList<>(utenNøkkel);

        Map<Tuple<String, String>, List<OpptjeningAktivitetPeriode>> identifikatorTilAktivitetMap = opptjeningAktiveter.stream()
            .collect(Collectors.groupingBy(this::getIdentifikator));
        for (Map.Entry<Tuple<String, String>, List<OpptjeningAktivitetPeriode>> entry : identifikatorTilAktivitetMap.entrySet()) {
            //legger de med ett innslag rett til i listen
            if (entry.getValue().size() == 1) {
                resultat.add(entry.getValue().get(0));
            } else {
                List<OpptjeningAktivitetPeriode> aktiviteterPåSammeOrgnummer = entry.getValue();
                List<LocalDateTimeline<OpptjeningAktivitetPeriode>> tidsserier = aktiviteterPåSammeOrgnummer.stream()
                    .map(a -> new LocalDateSegment<>(a.getPeriode().getFomDato(), a.getPeriode().getTomDato(), a))
                    .map(s -> new LocalDateTimeline<>(List.of(s)))
                    .collect(Collectors.toList());

                LocalDateTimeline<OpptjeningAktivitetPeriode> tidsserie = LocalDateTimeline.empty();

                for (LocalDateTimeline<OpptjeningAktivitetPeriode> tidsserieInput : tidsserier) {
                    tidsserie = tidsserie.combine(tidsserieInput, this::sjekkVurdering, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                }

                for (LocalDateInterval interval : tidsserie.getDatoIntervaller()) {
                    resultat.add(tidsserie.getSegment(interval).getValue());
                }
            }
        }
        return resultat;
    }

    private Tuple<String, String> getIdentifikator(OpptjeningAktivitetPeriode opp) {
        String identifikator = opp.getOpptjeningsnøkkel().getForType(Opptjeningsnøkkel.Type.ORG_NUMMER);
        if (identifikator == null) {
            identifikator = opp.getOpptjeningsnøkkel().getForType(Opptjeningsnøkkel.Type.AKTØR_ID);
        }
        return new Tuple<>(opp.getOpptjeningAktivitetType().getKode(), identifikator); // NOSONAR
    }

    private AktivitetPeriode.VurderingsStatus mapStatus(OpptjeningAktivitetPeriode periode) {
        return switch (periode.getVurderingsStatus()) {
            case UNDERKJENT -> AktivitetPeriode.VurderingsStatus.VURDERT_UNDERKJENT;
            case TIL_VURDERING -> AktivitetPeriode.VurderingsStatus.TIL_VURDERING;
            default -> throw new IllegalArgumentException("Oppstjeningsvilkår sin regelmodell støtter ikke status=" + periode.getVurderingsStatus());
        };
    }


    private LocalDateSegment<OpptjeningAktivitetPeriode> sjekkVurdering(LocalDateInterval di,
                                                                        LocalDateSegment<OpptjeningAktivitetPeriode> førsteVersjon,
                                                                        LocalDateSegment<OpptjeningAktivitetPeriode> sisteVersjon) {

        if (førsteVersjon == null && sisteVersjon != null) {
            return lagSegment(di, sisteVersjon.getValue());
        } else if (sisteVersjon == null && førsteVersjon != null) {
            return lagSegment(di, førsteVersjon.getValue());
        }

        OpptjeningAktivitetPeriode første = førsteVersjon.getValue();
        OpptjeningAktivitetPeriode siste = sisteVersjon.getValue();

        if (VurderingsStatus.TIL_VURDERING.equals(siste.getVurderingsStatus())) {
            return lagSegment(di, siste);
        } else {
            return lagSegment(di, første);
        }
    }

    private LocalDateSegment<OpptjeningAktivitetPeriode> lagSegment(LocalDateInterval di, OpptjeningAktivitetPeriode siste) {
        OpptjeningAktivitetPeriode.Builder builder = OpptjeningAktivitetPeriode.Builder.lagNyBasertPå(siste);
        OpptjeningAktivitetPeriode aktivitetPeriode = builder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(di.getFomDato(), di.getTomDato())).build();
        return new LocalDateSegment<>(di, aktivitetPeriode);
    }
}
