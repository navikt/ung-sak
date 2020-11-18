package no.nav.k9.sak.ytelse.frisinn.filter;

import static no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder;
import static no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.nyFraEksisterende;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.k9.sak.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.k9.sak.domene.iay.modell.OppgittEgenNæring;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilans;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.EgenNæringBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class OppgittOpptjeningFilter {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2020, 3, 1);

    private OppgittOpptjening oppgittOpptjening;
    private OppgittOpptjening overstyrtOppgittOpptjening;


    public OppgittOpptjeningFilter(OppgittOpptjening oppgittOpptjening, OppgittOpptjening overstyrtOppgittOpptjening) {
        this.oppgittOpptjening = oppgittOpptjening;
        this.overstyrtOppgittOpptjening = overstyrtOppgittOpptjening;
    }

    public OppgittOpptjeningFilter(Optional<OppgittOpptjening> oppgittOpptjening, Optional<OppgittOpptjening> overstyrtOppgittOpptjening) {
        this(oppgittOpptjening.orElse(null), overstyrtOppgittOpptjening.orElse(null));
    }


    /**
     * Brukes for FRISINN
     *
     * @return OppgittOpptjening
     */
    public OppgittOpptjening getOppgittOpptjeningFrisinn() {
        if (overstyrtOppgittOpptjening == null) {
            return oppgittOpptjening;
        }
        OppgittOpptjening slåttSammenGammelOverstyrtMedNytilkommet = leggTilNyeOpptjeningerHvisTilkommet();
        return slåttSammenGammelOverstyrtMedNytilkommet;
    }

    /**
     * Brukes for andre ytelser
     *
     * @return OppgittOpptjening
     */
    public Optional<OppgittOpptjening> getOppgittOpptjeningStandard() {
        if (overstyrtOppgittOpptjening != null) {
            return Optional.of(overstyrtOppgittOpptjening);
        }
        return Optional.ofNullable(oppgittOpptjening);
    }

    private OppgittOpptjening leggTilNyeOpptjeningerHvisTilkommet() {
        var builder = nyFraEksisterende(overstyrtOppgittOpptjening, overstyrtOppgittOpptjening.getEksternReferanse(), overstyrtOppgittOpptjening.getOpprettetTidspunkt());

        var senesteOverstyrtPeriodeFL = overstyrtOppgittOpptjening.getFrilans()
            .flatMap(oppgittFrilans -> oppgittFrilans.getFrilansoppdrag().stream()
                .map(OppgittFrilansoppdrag::getPeriode)
                .max(Comparator.comparing(DatoIntervallEntitet::getTomDato)));
        var senesteOverstyrtPeriodeSN = overstyrtOppgittOpptjening.getEgenNæring().stream()
            .map(OppgittEgenNæring::getPeriode)
            .max(Comparator.comparing(DatoIntervallEntitet::getTomDato));
        var senesteOverstyrtOrdArbForhold = overstyrtOppgittOpptjening.getOppgittArbeidsforhold().stream()
            .map(OppgittArbeidsforhold::getPeriode)
            .max(Comparator.comparing(DatoIntervallEntitet::getTomDato));
        var senestePeriode = senestePeriode(senesteOverstyrtPeriodeSN, senesteOverstyrtPeriodeFL, senesteOverstyrtOrdArbForhold);

        List<EgenNæringBuilder> egenNæringBuilders = finnSNPerioderSomSkalLeggesTil(oppgittOpptjening.getEgenNæring(), senestePeriode);
        builder.leggTilEgneNæringer(egenNæringBuilders);
        OppgittFrilans oppgittFrilans = finnFLPerioderSomSkalLeggesTil(oppgittOpptjening.getFrilans(), senestePeriode);
        if (oppgittFrilans != null) {
            builder.leggTilFrilansOpplysninger(oppgittFrilans);
        }
        List<OppgittArbeidsforholdBuilder> oppgittArbeidsforholdBuilders = finnOrdArbForholdPerioderSomSkalLeggesTil(oppgittOpptjening.getOppgittArbeidsforhold(), senestePeriode);
        builder.leggTilOppgittArbeidsforhold(oppgittArbeidsforholdBuilders);

        // Historiske opptjening
        var historiskPeriodeSN = oppgittOpptjening.getEgenNæring().stream().map(OppgittEgenNæring::getPeriode).filter(periode -> periode.getTomDato().isBefore(SKJÆRINGSTIDSPUNKT)).findFirst();
        var historiskOverstyrtPeriodeSN = overstyrtOppgittOpptjening.getEgenNæring().stream().map(OppgittEgenNæring::getPeriode).filter(periode -> periode.getTomDato().isBefore(SKJÆRINGSTIDSPUNKT)).findFirst();

        finnHistoriskSNPeriodeSomSkalLeggesTil(historiskPeriodeSN, historiskOverstyrtPeriodeSN)
            .ifPresent(builder::leggTilEgneNæringer);

        return builder.build();
    }

    private OppgittFrilans finnFLPerioderSomSkalLeggesTil(Optional<OppgittFrilans> frilans, Optional<DatoIntervallEntitet> senesteOverstyrtPeriode) {
        List<DatoIntervallEntitet> perioderEtterOverstrying = frilans.map(OppgittFrilans::getFrilansoppdrag).orElse(Collections.emptyList()).stream()
            .filter(frilansoppdrag -> senesteOverstyrtPeriode.isEmpty() || frilansoppdrag.getPeriode().getFomDato().isAfter(senesteOverstyrtPeriode.get().getTomDato()))
            .map(OppgittFrilansoppdrag::getPeriode)
            .collect(Collectors.toList());

        if (perioderEtterOverstrying.isEmpty()) {
            return null;
        }

        OppgittFrilansBuilder frilansBuilder = overstyrtOppgittOpptjening.getFrilans().isPresent() ? OppgittFrilansBuilder.fraEksisterende(overstyrtOppgittOpptjening.getFrilans().get()) : OppgittFrilansBuilder.ny();
        for (DatoIntervallEntitet periode : perioderEtterOverstrying) {
            if (oppgittOpptjening.getFrilans().isPresent()) {
                OppgittFrilans oppgittFrilans = oppgittOpptjening.getFrilans().get();
                Optional<OppgittFrilansoppdrag> oppdrag = oppgittFrilans.getFrilansoppdrag().stream().filter(oppgittFrilansoppdrag -> oppgittFrilansoppdrag.getPeriode().equals(periode)).findFirst();
                oppdrag.ifPresent(frilansBuilder::leggTilFrilansOppdrag);
            }
        }
        return frilansBuilder.build();
    }

    private List<EgenNæringBuilder> finnSNPerioderSomSkalLeggesTil(List<OppgittEgenNæring> egenNæring, Optional<DatoIntervallEntitet> senesteOverstyrtPeriode) {
        List<DatoIntervallEntitet> perioderEtterOverstyring = egenNæring
            .stream()
            .filter(eg -> senesteOverstyrtPeriode.isEmpty() || eg.getPeriode().getFomDato().isAfter(senesteOverstyrtPeriode.get().getTomDato()))
            .map(OppgittEgenNæring::getPeriode)
            .collect(Collectors.toList());

        List<List<EgenNæringBuilder>> builders = new ArrayList<>();
        for (DatoIntervallEntitet periode : perioderEtterOverstyring) {
            builders.add(leggTilSN(periode));
        }

        return builders.stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    private Optional<List<EgenNæringBuilder>> finnHistoriskSNPeriodeSomSkalLeggesTil(Optional<DatoIntervallEntitet> historiskPeriodeSN,
                                                                                     Optional<DatoIntervallEntitet> historiskOverstyrtPeriodeSN) {
        if (historiskOverstyrtPeriodeSN.isPresent()) {
            return Optional.empty();
        }
        return historiskPeriodeSN.map(this::leggTilSN);
    }

    private List<EgenNæringBuilder> leggTilSN(DatoIntervallEntitet senestePeriodeSN) {
        List<OppgittEgenNæring> egenNæring = oppgittOpptjening.getEgenNæring().stream().filter(oppgittEgenNæring -> oppgittEgenNæring.getPeriode().equals(senestePeriodeSN)).collect(Collectors.toList());
        return egenNæring.stream().map(en -> EgenNæringBuilder.fraEksisterende(en)
            .medPeriode(en.getPeriode())
            .medBruttoInntekt(en.getBruttoInntekt()))
            .collect(Collectors.toList());
    }

    private List<OppgittArbeidsforholdBuilder> leggTilOrdinærtdArbForhold(DatoIntervallEntitet senestePeriode) {
        List<OppgittArbeidsforhold> oppgitteArbeidsforhold = oppgittOpptjening.getOppgittArbeidsforhold().stream().filter(it -> it.getPeriode().equals(senestePeriode)).collect(Collectors.toList());

        return oppgitteArbeidsforhold.stream().map(af -> OppgittArbeidsforholdBuilder.ny()
            .medArbeidType(af.getArbeidType())
            .medPeriode(af.getPeriode())
            .medInntekt(af.getInntekt()))
            .collect(Collectors.toList());
    }

    private List<OppgittArbeidsforholdBuilder> finnOrdArbForholdPerioderSomSkalLeggesTil(List<OppgittArbeidsforhold> oppgittArbeidsforhold, Optional<DatoIntervallEntitet> senesteOverstyrtPeriode) {
        List<DatoIntervallEntitet> perioderEtterOverstyring = oppgittArbeidsforhold
            .stream()
            .filter(eg -> senesteOverstyrtPeriode.isEmpty() || eg.getPeriode().getFomDato().isAfter(senesteOverstyrtPeriode.get().getTomDato()))
            .map(OppgittArbeidsforhold::getPeriode)
            .collect(Collectors.toList());

        List<List<OppgittArbeidsforholdBuilder>> builders = new ArrayList<>();
        for (DatoIntervallEntitet periode : perioderEtterOverstyring) {
            builders.add(leggTilOrdinærtdArbForhold(periode));
        }
        return builders.stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    private Optional<DatoIntervallEntitet> senestePeriode(Optional<DatoIntervallEntitet> senestePeriodeSN, Optional<DatoIntervallEntitet> senestePeriodeFL, Optional<DatoIntervallEntitet> senesteOverstyrtOrdArbForhold) {
        var senestePerioder = List.of(senestePeriodeSN, senestePeriodeFL, senesteOverstyrtOrdArbForhold).stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        return senestePerioder.stream()
            .reduce((p1, p2) -> p1.getTomDato().isAfter(p2.getTomDato()) || p1.getTomDato().equals(p2.getTomDato()) ? p1 : p2);
    }
}
