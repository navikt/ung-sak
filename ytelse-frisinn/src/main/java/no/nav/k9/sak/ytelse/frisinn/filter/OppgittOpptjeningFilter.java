package no.nav.k9.sak.ytelse.frisinn.filter;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.k9.sak.domene.iay.modell.OppgittEgenNæring;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilans;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
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
        var builder = OppgittOpptjeningBuilder.nyFraEksisterende(overstyrtOppgittOpptjening, overstyrtOppgittOpptjening.getEksternReferanse(), overstyrtOppgittOpptjening.getOpprettetTidspunkt());

        // Opptjening for ny søknadsperiode
        var senestePeriodeSN = oppgittOpptjening.getEgenNæring().stream().map(OppgittEgenNæring::getPeriode).max(Comparator.comparing(DatoIntervallEntitet::getTomDato));
        var senestePeriodeFL = oppgittOpptjening.getFrilans().flatMap(oppgittFrilans -> oppgittFrilans.getFrilansoppdrag().stream().map(OppgittFrilansoppdrag::getPeriode).max(Comparator.comparing(DatoIntervallEntitet::getTomDato)));
        var senestePeriode = senestePeriode(senestePeriodeSN, senestePeriodeFL);

        var senesteOverstyrtPeriodeSN = overstyrtOppgittOpptjening.getEgenNæring().stream().map(OppgittEgenNæring::getPeriode).max(Comparator.comparing(DatoIntervallEntitet::getTomDato));
        var senesteOverstyrtPeriodeFL = overstyrtOppgittOpptjening.getFrilans().flatMap(oppgittFrilans -> oppgittFrilans.getFrilansoppdrag().stream().map(OppgittFrilansoppdrag::getPeriode).max(Comparator.comparing(DatoIntervallEntitet::getTomDato)));

        finnSNPeriodeSomSkalLeggesTil(senestePeriodeSN, senestePeriode, senesteOverstyrtPeriodeSN, senesteOverstyrtPeriodeFL)
            .ifPresent(builder::leggTilEgneNæringer);
        finnFLPeriodeSomSkalLeggesTil(senestePeriodeFL, senestePeriode, senesteOverstyrtPeriodeSN, senesteOverstyrtPeriodeFL)
            .ifPresent(builder::leggTilFrilansOpplysninger);

        // Historiske opptjening
        var historiskPeriodeSN = oppgittOpptjening.getEgenNæring().stream().map(OppgittEgenNæring::getPeriode).filter(periode -> periode.getTomDato().isBefore(SKJÆRINGSTIDSPUNKT)).findFirst();
        var historiskOverstyrtPeriodeSN = overstyrtOppgittOpptjening.getEgenNæring().stream().map(OppgittEgenNæring::getPeriode).filter(periode -> periode.getTomDato().isBefore(SKJÆRINGSTIDSPUNKT)).findFirst();

        finnHistoriskSNPeriodeSomSkalLeggesTil(historiskPeriodeSN, historiskOverstyrtPeriodeSN)
            .ifPresent(builder::leggTilEgneNæringer);

        return builder.build();
    }

    private Optional<OppgittFrilans> finnFLPeriodeSomSkalLeggesTil(Optional<DatoIntervallEntitet> senestePeriodeFL, DatoIntervallEntitet senestePeriode,
                                                                   Optional<DatoIntervallEntitet> senesteOverstyrtPeriodeSN, Optional<DatoIntervallEntitet> senesteOverstyrtPeriodeFL) {

        if (senestePeriodeFL.isPresent() && senesteOverstyrtPeriodeFL.isPresent()) {
            if (senestePeriodeFL.get().getTomDato().isAfter(senesteOverstyrtPeriodeFL.get().getTomDato()) && senestePeriodeFL.get().getTomDato().equals(senestePeriode.getTomDato())) {
                return leggTilFrilans(senestePeriodeFL.get());
            }
        } else if (senestePeriodeFL.isPresent() && senesteOverstyrtPeriodeSN.isPresent()) {
            if (senestePeriodeFL.get().getTomDato().isAfter(senesteOverstyrtPeriodeSN.get().getTomDato())) {
                return leggTilFrilans(senestePeriodeFL.get());
            }
        }
        return Optional.empty();
    }

    private Optional<OppgittFrilans> leggTilFrilans(DatoIntervallEntitet senestePeriodeFL) {
       return oppgittOpptjening.getFrilans().map(oppgittFrilans -> {
            OppgittFrilansBuilder frilansBuilder = overstyrtOppgittOpptjening.getFrilans().isPresent() ? OppgittFrilansBuilder.fraEksisterende(overstyrtOppgittOpptjening.getFrilans().get()) : OppgittFrilansBuilder.ny();
            Optional<OppgittFrilansoppdrag> oppdrag = oppgittFrilans.getFrilansoppdrag().stream().filter(oppgittFrilansoppdrag -> oppgittFrilansoppdrag.getPeriode().equals(senestePeriodeFL)).findFirst();
            oppdrag.ifPresent(frilansBuilder::leggTilFrilansOppdrag);
            return frilansBuilder.build();
        });
    }

    private Optional<List<EgenNæringBuilder>> finnSNPeriodeSomSkalLeggesTil(Optional<DatoIntervallEntitet> senestePeriodeSN, DatoIntervallEntitet senestePeriode,
                                                                            Optional<DatoIntervallEntitet> senesteOverstyrtPeriodeSN, Optional<DatoIntervallEntitet> senesteOverstyrtPeriodeFL) {

        if (senestePeriodeSN.isPresent() && senesteOverstyrtPeriodeSN.isPresent()) {
            if (senestePeriodeSN.get().getTomDato().isAfter(senesteOverstyrtPeriodeSN.get().getTomDato()) && senestePeriodeSN.get().getTomDato().equals(senestePeriode.getTomDato())) {
                return leggTilSN(senestePeriodeSN.get());
            }
        } else if (senestePeriodeSN.isPresent() && senesteOverstyrtPeriodeFL.isPresent()) {
            if (senestePeriodeSN.get().getTomDato().isAfter(senesteOverstyrtPeriodeFL.get().getTomDato())) {
                return leggTilSN(senestePeriodeSN.get());
            }
        }
        return Optional.empty();
    }

    private Optional<List<EgenNæringBuilder>> finnHistoriskSNPeriodeSomSkalLeggesTil(Optional<DatoIntervallEntitet> historiskPeriodeSN,
                                                                                     Optional<DatoIntervallEntitet> historiskOverstyrtPeriodeSN) {
        if (historiskOverstyrtPeriodeSN.isPresent()) {
            return Optional.empty();
        }
        if (historiskPeriodeSN.isPresent()) {
            return leggTilSN(historiskPeriodeSN.get());
        }
        return Optional.empty();
    }

    private Optional<List<EgenNæringBuilder>> leggTilSN(DatoIntervallEntitet senestePeriodeSN) {
        List<OppgittEgenNæring> egenNæring = oppgittOpptjening.getEgenNæring().stream().filter(oppgittEgenNæring -> oppgittEgenNæring.getPeriode().equals(senestePeriodeSN)).collect(Collectors.toList());
        List<EgenNæringBuilder> egenNæringBuilder = egenNæring.stream().map(en -> EgenNæringBuilder.fraEksisterende(en)
                .medPeriode(en.getPeriode())
                .medBruttoInntekt(en.getBruttoInntekt()))
                .collect(Collectors.toList());

        return Optional.of(egenNæringBuilder);
    }

    private DatoIntervallEntitet senestePeriode(Optional<DatoIntervallEntitet> senestePeriodeSN, Optional<DatoIntervallEntitet> senestePeriodeFL) {
        if (senestePeriodeFL.isEmpty() && senestePeriodeSN.isEmpty()) {
            throw new IllegalStateException("Utviklerfeil: " + senestePeriodeSN + " og " + senestePeriodeFL + " er empty");
        } else if (senestePeriodeFL.isEmpty()) {
            return senestePeriodeSN.get();
        } else if (senestePeriodeSN.isEmpty()) {
            return senestePeriodeFL.get();
        }
        DatoIntervallEntitet fl = senestePeriodeFL.get();
        DatoIntervallEntitet sn = senestePeriodeSN.get();

        return fl.getTomDato().isAfter(sn.getTomDato()) || fl.getTomDato().equals(sn.getTomDato()) ? senestePeriodeFL.get() : senestePeriodeSN.get();
    }
}
