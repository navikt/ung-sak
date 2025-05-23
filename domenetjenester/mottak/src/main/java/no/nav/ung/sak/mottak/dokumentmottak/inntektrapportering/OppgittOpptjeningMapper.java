package no.nav.ung.sak.mottak.dokumentmottak.inntektrapportering;

import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
import no.nav.k9.søknad.ytelse.ung.v1.inntekt.OppgittInntekt;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.domene.abakus.mapping.IAYTilDtoMapper;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class OppgittOpptjeningMapper {

    public static Optional<OppgittOpptjeningMottattRequest> mapRequest(BehandlingReferanse behandlingReferanse,
                                                                       MottattDokument dokument,
                                                                       OppgittInntekt oppgittInntekt) {


        final var oppgittArbeidOgFrilans = oppgittInntekt.getOppgittePeriodeinntekter()
            .stream()
            .filter(it -> it.getArbeidstakerOgFrilansInntekt() != null)
            .map(inntekter -> OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny()
                .medInntekt(inntekter.getArbeidstakerOgFrilansInntekt())
                .medArbeidType(ArbeidType.VANLIG)
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(inntekter.getPeriode().getFraOgMed(), inntekter.getPeriode().getTilOgMed())))
            .toList();

        if (!oppgittArbeidOgFrilans.isEmpty()) {
            var builder = OppgittOpptjeningBuilder.ny(UUID.randomUUID(), LocalDateTime.now());
            builder.leggTilOppgittArbeidsforhold(oppgittArbeidOgFrilans);
            builder.leggTilJournalpostId(dokument.getJournalpostId());
            builder.leggTilInnsendingstidspunkt(dokument.getInnsendingstidspunkt());
            return Optional.of(byggRequest(behandlingReferanse, builder));
        } else {
            return Optional.empty();
        }
    }

    private static OppgittOpptjeningMottattRequest byggRequest(BehandlingReferanse behandlingReferanse, OppgittOpptjeningBuilder builder) {
        var aktør = new AktørIdPersonident(behandlingReferanse.getAktørId().getId());
        var saksnummer = behandlingReferanse.getSaksnummer();
        var ytelseType = YtelseType.fraKode(behandlingReferanse.getFagsakYtelseType().getKode());
        var oppgittOpptjening = IAYTilDtoMapper.mapTilDto(builder);
        var request = new OppgittOpptjeningMottattRequest(saksnummer.getVerdi(), behandlingReferanse.getBehandlingUuid(), aktør, ytelseType, oppgittOpptjening);
        return request;
    }

}
