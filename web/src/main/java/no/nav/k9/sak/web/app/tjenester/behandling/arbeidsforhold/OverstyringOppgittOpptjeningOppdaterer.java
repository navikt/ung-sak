package no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilans;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.EgenNæringBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.kontrakt.arbeidsforhold.BekreftOverstyrOppgittOpptjeningDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittFrilansDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.PeriodeDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.SøknadsperiodeOgOppgittOpptjeningDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftOverstyrOppgittOpptjeningDto.class, adapter = AksjonspunktOppdaterer.class)
public class OverstyringOppgittOpptjeningOppdaterer implements AksjonspunktOppdaterer<BekreftOverstyrOppgittOpptjeningDto> {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private UttakRepository uttakRepository;

    OverstyringOppgittOpptjeningOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public OverstyringOppgittOpptjeningOppdaterer(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, BehandlingskontrollTjeneste behandlingskontrollTjeneste, UttakRepository uttakRepository) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.uttakRepository = uttakRepository;
    }

    @Override
    public OppdateringResultat oppdater(BekreftOverstyrOppgittOpptjeningDto dto, AksjonspunktOppdaterParameter param) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(param.getBehandlingId());

        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        var søknadsperiodeOgOppgittOpptjening = dto.getSøknadsperiodeOgOppgittOpptjeningDto();
        leggerTilEgenæring(søknadsperiodeOgOppgittOpptjening).ifPresent(oppgittOpptjeningBuilder::leggTilEgneNæringer);
        leggerTilFrilans(søknadsperiodeOgOppgittOpptjening).ifPresent(oppgittOpptjeningBuilder::leggTilFrilansOpplysninger);

        inntektArbeidYtelseTjeneste.lagreOverstyrtOppgittOpptjening(kontekst.getBehandlingId(), oppgittOpptjeningBuilder);

        ArrayList<UttakAktivitetPeriode> perioderSomSkalMed = utledePerioder(dto);
        uttakRepository.lagreOgFlushFastsattUttak(param.getBehandlingId(), new UttakAktivitet(perioderSomSkalMed));

        return OppdateringResultat.utenOveropp();
    }

    private ArrayList<UttakAktivitetPeriode> utledePerioder(BekreftOverstyrOppgittOpptjeningDto dto) {
        PeriodeDto periodeFraSøknad = dto.getSøknadsperiodeOgOppgittOpptjeningDto().getPeriodeFraSøknad();

        var perioderSomSkalMed = new ArrayList<UttakAktivitetPeriode>();
        if (dto.getSøknadsperiodeOgOppgittOpptjeningDto().getSøkerYtelseForFrilans()) {
            perioderSomSkalMed.add(new UttakAktivitetPeriode(UttakArbeidType.FRILANSER, periodeFraSøknad.getFom(), periodeFraSøknad.getTom()));
        }
        if (dto.getSøknadsperiodeOgOppgittOpptjeningDto().getSøkerYtelseForNæring()) {
            perioderSomSkalMed.add(new UttakAktivitetPeriode(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, periodeFraSøknad.getFom(), periodeFraSøknad.getTom()));
        }
        return perioderSomSkalMed;
    }

    private Optional<OppgittFrilans> leggerTilFrilans(SøknadsperiodeOgOppgittOpptjeningDto søknadsperiodeOgOppgittOpptjening) {
        var frilansI = søknadsperiodeOgOppgittOpptjening.getISøkerPerioden().getOppgittFrilans();
        var frilansFør = søknadsperiodeOgOppgittOpptjening.getFørSøkerPerioden().getOppgittFrilans();

        if (frilansI != null || frilansFør != null) {
            var oppdrag = Optional.ofNullable(frilansI).map(OppgittFrilansDto::getOppgittFrilansoppdrag).orElse(Collections.emptyList());
            oppdrag.addAll(Optional.ofNullable(frilansFør).map(OppgittFrilansDto::getOppgittFrilansoppdrag).orElse(Collections.emptyList()));

            var oppgittFrilansoppdrag = oppdrag.stream().map(oppgittFrilansoppdragDto -> {
                var frilansOppdragBuilder = OppgittFrilansOppdragBuilder.ny();
                frilansOppdragBuilder.medInntekt(oppgittFrilansoppdragDto.getBruttoInntekt().getVerdi());
                frilansOppdragBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittFrilansoppdragDto.getPeriode().getFom(), oppgittFrilansoppdragDto.getPeriode().getTom()));
                return frilansOppdragBuilder.build();
            }).collect(Collectors.toList());

            var frilansBuilder = OppgittFrilansBuilder.ny();
            frilansBuilder.medErNyoppstartet(utled(oppgittFrilansoppdrag));
            frilansBuilder.leggTilOppgittOppdrag(oppgittFrilansoppdrag);
            return Optional.of(frilansBuilder.build());
        }
        return Optional.empty();
    }

    private boolean utled(List<OppgittFrilansoppdrag> oppgittFrilansoppdrag) {
        Optional<LocalDate> førstFomOpt = oppgittFrilansoppdrag.stream().map(oppdrag -> oppdrag.getPeriode().getFomDato()).min(LocalDate::compareTo);
        if (førstFomOpt.isPresent()) {
            LocalDate føsteFom = førstFomOpt.get();
            // regner ting etter 01.01.2019 som nyoppstartet
            return føsteFom.isAfter(LocalDate.of(2019, 1, 1));
        }
        return false;
    }

    private Optional<List<EgenNæringBuilder>> leggerTilEgenæring(SøknadsperiodeOgOppgittOpptjeningDto søknadsperiodeOgOppgittOpptjening) {
        var egenNæring = new ArrayList<>(søknadsperiodeOgOppgittOpptjening.getFørSøkerPerioden().getOppgittEgenNæring());
        egenNæring.addAll(søknadsperiodeOgOppgittOpptjening.getISøkerPerioden().getOppgittEgenNæring());

        if (!egenNæring.isEmpty()) {
            return Optional.of(egenNæring.stream().map(oppgittEgenNæringDto -> {
                var egenNæringBuilder = EgenNæringBuilder.ny();
                egenNæringBuilder.medBruttoInntekt(oppgittEgenNæringDto.getBruttoInntekt().getVerdi());
                egenNæringBuilder.medVirksomhetType(VirksomhetType.ANNEN);
                egenNæringBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittEgenNæringDto.getPeriode().getFom(), oppgittEgenNæringDto.getPeriode().getTom()));
                return egenNæringBuilder;
            }).collect(Collectors.toList()));
        }
        return Optional.empty();
    }
}
