package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.bistand.BistandOppgaveOppretter;
import no.nav.ung.sak.etterlysning.bosted.BostedOppgaveOppretter;
import no.nav.ung.sak.etterlysning.kontroll.InntektkontrollOppgaveOppretter;
import no.nav.ung.sak.etterlysning.opphorvedmaksdato.OpphørVedMaksdatoOppgaveOppretter;
import no.nav.ung.sak.etterlysning.programperiode.EndretPeriodeOppgaveOppretter;
import no.nav.ung.sak.etterlysning.sluttdato.EndretSluttdatoOppgaveOppretter;
import no.nav.ung.sak.etterlysning.startdato.EndretStartdatoOppgaveOppretter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Dependent
public class OpprettOppgaveTjeneste {

    private final InntektkontrollOppgaveOppretter inntektkontrollOppgaveOppretter;
    private final EndretSluttdatoOppgaveOppretter endretSluttdatoOppgaveOppretter;
    private final EndretStartdatoOppgaveOppretter endretStartdatoOppgaveOppretter;
    private final EndretPeriodeOppgaveOppretter endretPeriodeOppgaveOppretter;
    private final OpphørVedMaksdatoOppgaveOppretter opphørVedMaksdatoOppgaveOppretter;
    private final BostedOppgaveOppretter bostedOppgaveOppretter;
    private final BistandOppgaveOppretter bistandOppgaveOppretter;
    private final EtterlysningRepository etterlysningRepository;
    private final HistorikkinnslagRepository historikkinnslagRepository;
    private final Duration ventePeriode;
    private final Boolean historikkinnslagEnabled;

    @Inject
    public OpprettOppgaveTjeneste(
        InntektkontrollOppgaveOppretter inntektkontrollOppgaveOppretter,
        EndretSluttdatoOppgaveOppretter endretSluttdatoOppgaveOppretter,
        EndretStartdatoOppgaveOppretter endretStartdatoOppgaveOppretter,
        EndretPeriodeOppgaveOppretter endretPeriodeOppgaveOppretter,
        OpphørVedMaksdatoOppgaveOppretter opphørVedMaksdatoOppgaveOppretter,
        BostedOppgaveOppretter bostedOppgaveOppretter,
        BistandOppgaveOppretter bistandOppgaveOppretter,
        EtterlysningRepository etterlysningRepository,
        HistorikkinnslagRepository historikkinnslagRepository,
        @KonfigVerdi(value = "VENTEFRIST_UTTALELSE", defaultVerdi = "P14D") String ventePeriode,
        @KonfigVerdi(value = "HISTORIKKINNSLAG_FOR_OPPGAVE_ENABLED", defaultVerdi = "false") Boolean historikkinnslagEnabled
    ) {
        this.inntektkontrollOppgaveOppretter = inntektkontrollOppgaveOppretter;
        this.endretSluttdatoOppgaveOppretter = endretSluttdatoOppgaveOppretter;
        this.endretStartdatoOppgaveOppretter = endretStartdatoOppgaveOppretter;
        this.endretPeriodeOppgaveOppretter = endretPeriodeOppgaveOppretter;
        this.opphørVedMaksdatoOppgaveOppretter = opphørVedMaksdatoOppgaveOppretter;
        this.bostedOppgaveOppretter = bostedOppgaveOppretter;
        this.bistandOppgaveOppretter = bistandOppgaveOppretter;
        this.etterlysningRepository = etterlysningRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.ventePeriode = Duration.parse(ventePeriode);
        this.historikkinnslagEnabled = historikkinnslagEnabled;
    }

    public List<Etterlysning> opprett(Behandling behandling, EtterlysningType etterlysningType) {
        var aktørId = behandling.getAktørId();
        var etterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), etterlysningType);
        etterlysninger.forEach(e -> e.vent(getFrist()));
        etterlysningRepository.lagre(etterlysninger);

        if (historikkinnslagEnabled) {
            opprettHistorikkinnslag(behandling, etterlysningType, etterlysninger);
        }

        // REST-kall for å opprette oppgave, gjør dette til slutt
        switch (etterlysningType) {
            case UTTALELSE_KONTROLL_INNTEKT ->
                inntektkontrollOppgaveOppretter.opprettOppgave(behandling, etterlysninger, aktørId);
            case UTTALELSE_ENDRET_STARTDATO ->
                endretStartdatoOppgaveOppretter.opprettOppgave(behandling, etterlysninger, aktørId);
            case UTTALELSE_ENDRET_SLUTTDATO ->
                endretSluttdatoOppgaveOppretter.opprettOppgave(behandling, etterlysninger, aktørId);
            case UTTALELSE_ENDRET_PERIODE ->
                endretPeriodeOppgaveOppretter.opprettOppgave(behandling, etterlysninger, aktørId);
            case UTTALELSE_OPPHOR_VED_MAKSDATO ->
                opphørVedMaksdatoOppgaveOppretter.opprettOppgave(behandling, etterlysninger, aktørId);
            case UTTALELSE_BOSTED ->
                bostedOppgaveOppretter.opprettOppgave(behandling, etterlysninger, aktørId);
            case UTTALELSE_BISTAND ->
                bistandOppgaveOppretter.opprettOppgave(behandling, etterlysninger, aktørId);
            default ->
                throw new IllegalArgumentException("Har ikke implementert oppretting av oppgave for etterlysningstype: " + etterlysningType);
        }
        return etterlysninger;
    }

    public LocalDateTime getFrist() {
        return LocalDateTime.now().plus(ventePeriode);
    }

    private void opprettHistorikkinnslag(Behandling behandling, EtterlysningType etterlysningType, Collection<Etterlysning> etterlysninger) {
        String tittel = "Oppretter oppgave til bruker";
        List<DatoIntervallEntitet> sortertePerioder = etterlysninger.stream().map(Etterlysning::getPeriode).sorted().toList();
        List<HistorikkinnslagLinjeBuilder> linjer = new ArrayList<>();
        linjer.add(HistorikkinnslagLinjeBuilder.plainTekstLinje(HistorikkinnslagLinjeBuilder.format(etterlysningType)));
        String prefix = "for periode ";
        for (DatoIntervallEntitet periode : sortertePerioder) {
            linjer.add(HistorikkinnslagLinjeBuilder.plainTekstLinje(prefix + HistorikkinnslagLinjeBuilder.format(periode)));
            prefix = "og ";
        }
        historikkinnslagRepository.lagre(new Historikkinnslag.Builder()
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medTittel(tittel)
            .medLinjer(linjer)
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .build());
    }

}
