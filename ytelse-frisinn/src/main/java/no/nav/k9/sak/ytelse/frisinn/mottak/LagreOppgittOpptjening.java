package no.nav.k9.sak.ytelse.frisinn.mottak;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilans;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.EgenNæringBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.søknad.felles.Periode;
import no.nav.k9.søknad.frisinn.Inntekter;
import no.nav.k9.søknad.frisinn.PeriodeInntekt;
import no.nav.k9.søknad.frisinn.SelvstendigNæringsdrivende;
import no.nav.vedtak.util.Tuple;

@Dependent
class LagreOppgittOpptjening {

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private BehandlingRepository behandlingRepository;

    LagreOppgittOpptjening() {
        // for proxy
    }

    @Inject
    LagreOppgittOpptjening(BehandlingRepository behandlingRepository,
                           InntektArbeidYtelseTjeneste iayTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
    }

    void lagreOpptjening(Behandling behandling, Inntekter inntekter, ZonedDateTime tidspunkt) {

        Long behandlingId = behandling.getId();

        Tuple<OppgittOpptjeningBuilder, Boolean> tuple = initOpptjeningBuilderFraEksisterende(behandling.getFagsakId(), tidspunkt);
        OppgittOpptjeningBuilder opptjeningBuilder = tuple.getElement1();

        boolean erNyeOpplysninger = false;
        if (inntekter.getFrilanser() != null) {
            var fri = inntekter.getFrilanser();
            List<OppgittFrilansoppdrag> oppdrag = fri.getInntekterSøknadsperiode().entrySet().stream().map(entry -> {
                OppgittFrilansOppdragBuilder builder = OppgittFrilansOppdragBuilder.ny();
                return builder
                        .medInntekt(entry.getValue().getBeløp())
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(entry.getKey().getFraOgMed(), entry.getKey().getTilOgMed()))
                        .build();
            }).collect(Collectors.toList());

            OppgittFrilansBuilder frilansBuilder = OppgittFrilansBuilder.ny();
            OppgittFrilans oppgittFrilans = frilansBuilder
                    .leggTilOppgittOppdrag(oppdrag)
                    .build();
            opptjeningBuilder.leggTilFrilansOpplysninger(oppgittFrilans);
            erNyeOpplysninger = true;
        }

        if (inntekter.getSelvstendig() != null) {
            var selv = inntekter.getSelvstendig();

            // slår sammen historiske og løpende inntekter her.  Bruker stp senere til håndtere før/etter inntektstap startet.

            var egenNæringFør = selv.getInntekterFør().entrySet().stream().map(e -> mapEgenNæring(selv, e)).collect(Collectors.toList());
            opptjeningBuilder.leggTilEgneNæringer(egenNæringFør);

            var egenNæringSøknadsperiode = selv.getInntekterSøknadsperiode().entrySet().stream().map(e -> mapEgenNæring(selv, e)).collect(Collectors.toList());
            opptjeningBuilder.leggTilEgneNæringer(egenNæringSøknadsperiode);

            erNyeOpplysninger |= !egenNæringFør.isEmpty() || !egenNæringSøknadsperiode.isEmpty();
        }

        if (inntekter.getArbeidstaker() != null) {
            var arbeidstaker = inntekter.getArbeidstaker();
            arbeidstaker.getInntekterSøknadsperiode().entrySet()
                    .stream()
                    .map(this::mapArbeidsforhold)
                    .forEach(opptjeningBuilder::leggTilOppgittArbeidsforhold);
            erNyeOpplysninger = true;
        }

        // FIXME K9: håndter lagring i egen task så det blir robust kall til abakus
        if (erNyeOpplysninger) {
            Boolean måOppdatereBasertPåtidligereBehandling = tuple.getElement2();
            if (måOppdatereBasertPåtidligereBehandling) {
                iayTjeneste.lagreOverstyrtOppgittOpptjening(behandlingId, opptjeningBuilder);
            } else {
                iayTjeneste.lagreOppgittOpptjening(behandlingId, opptjeningBuilder);
            }
        }
    }

    private Tuple<OppgittOpptjeningBuilder, Boolean> initOpptjeningBuilderFraEksisterende(Long fagsakId, ZonedDateTime tidspunkt) {
        // bygg på eksisterende hvis tidligere innrapportert for denne ytelsen (sikrer at vi får med originalt rapportert inntektsgrunnlag).
        // TODO: håndtere korreksjoner senere?  vil nå bare akkumulere innrapportert.
        var sisteBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
        if (sisteBehandling.isPresent()) {
            Optional<InntektArbeidYtelseGrunnlag> iayGrunnlagOpt = iayTjeneste.finnGrunnlag(sisteBehandling.get().getId());
            if (iayGrunnlagOpt.isPresent()) {
                // tar utgangspunkt i overstrying hvis det finnes
                var tidligereRegistrertOpptjening = iayGrunnlagOpt.get().getOverstyrtOppgittOpptjening().isPresent() ?
                        iayGrunnlagOpt.get().getOverstyrtOppgittOpptjening() : iayGrunnlagOpt.get().getOppgittOpptjening();
                if (tidligereRegistrertOpptjening.isPresent()) {
                    return new Tuple<>(OppgittOpptjeningBuilder.nyFraEksisterende(tidligereRegistrertOpptjening.get(), UUID.randomUUID(), tidspunkt.toLocalDateTime()), true);
                }
            }
        }
        return new Tuple<>(OppgittOpptjeningBuilder.ny(UUID.randomUUID(), tidspunkt.toLocalDateTime()), false);
    }

    private EgenNæringBuilder mapEgenNæring(SelvstendigNæringsdrivende selvstendig, Map.Entry<Periode, PeriodeInntekt> entry) {
        Periode periode = entry.getKey();
        PeriodeInntekt inntekt = entry.getValue();

        var builder = EgenNæringBuilder.ny();
        builder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFraOgMed(), periode.getTilOgMed()));
        builder.medBruttoInntekt(inntekt.getBeløp());
        builder.medVirksomhetType(VirksomhetType.ANNEN);
        builder.medRegnskapsførerNavn(selvstendig.getRegnskapsførerNavn());
        builder.medRegnskapsførerTlf(selvstendig.getRegnskapsførerTlf());
        return builder;
    }

    private OppgittArbeidsforholdBuilder mapArbeidsforhold(Entry<Periode, PeriodeInntekt> entry) {
        Periode periode = entry.getKey();
        PeriodeInntekt inntekt = entry.getValue();

        OppgittArbeidsforholdBuilder builder = OppgittArbeidsforholdBuilder.ny();
        builder.medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFraOgMed(), periode.getTilOgMed()))
                .medInntekt(inntekt.getBeløp());

        return builder;
    }
}
