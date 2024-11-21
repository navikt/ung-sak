package no.nav.ung.sak.domene.abakus;

import static java.util.stream.Collectors.flatMapping;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsforholdDto;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.request.AktørDatoRequest;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.EksternArbeidsforholdRef;

@Dependent
public class ArbeidsforholdTjeneste {

    private final AbakusTjeneste abakusTjeneste;

    @Inject
    public ArbeidsforholdTjeneste(AbakusTjeneste abakusTjeneste) {
        this.abakusTjeneste = abakusTjeneste;
    }

    public Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> finnArbeidsforholdForIdentPåDag(AktørId ident, LocalDate dato, FagsakYtelseType ytelseType) {
        final var request = new AktørDatoRequest(new AktørIdPersonident(ident.getId()), new Periode(dato, dato), YtelseType.fraKode(ytelseType.getKode()));
        return abakusTjeneste.hentArbeidsforholdIPerioden(request)
            .stream()
            .collect(Collectors.groupingBy(this::mapTilArbeidsgiver,
                flatMapping(im -> Stream.of(EksternArbeidsforholdRef.ref(im.getArbeidsforholdId() != null ? im.getArbeidsforholdId().getEksternReferanse() : null)), Collectors.toSet())));
    }

    private Arbeidsgiver mapTilArbeidsgiver(ArbeidsforholdDto arbeidsforhold) {
        final var arbeidsgiver = arbeidsforhold.getArbeidsgiver();
        if (arbeidsgiver.getErOrganisasjon()) {
            return Arbeidsgiver.virksomhet(arbeidsgiver.getIdent());
        } else if (arbeidsgiver.getErPerson()) {
            return Arbeidsgiver.person(new AktørId(arbeidsgiver.getIdent()));
        }
        throw new IllegalArgumentException("Arbeidsgiver er verken person eller organisasjon");
    }
}
