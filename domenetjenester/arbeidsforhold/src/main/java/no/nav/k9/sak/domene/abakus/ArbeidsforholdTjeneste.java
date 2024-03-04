package no.nav.k9.sak.domene.abakus;

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
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;

@Dependent
public class ArbeidsforholdTjeneste {

    private AbakusTjeneste abakusTjeneste;

    private K9AbakusTjeneste k9AbakusTjeneste;

    private boolean k9abakusEnabled;

    @Inject
    public ArbeidsforholdTjeneste(AbakusTjeneste abakusTjeneste, K9AbakusTjeneste k9AbakusTjeneste, @KonfigVerdi(value = "k9.abakus.enabled", defaultVerdi = "false") boolean k9abakusEnabled) {
        this.abakusTjeneste = abakusTjeneste;
        this.k9AbakusTjeneste = k9AbakusTjeneste;
        this.k9abakusEnabled = k9abakusEnabled;
    }

    public Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> finnArbeidsforholdForIdentPåDag(AktørId ident, LocalDate dato, FagsakYtelseType ytelseType) {
        final var request = new AktørDatoRequest(new AktørIdPersonident(ident.getId()), new Periode(dato, dato), YtelseType.fraKode(ytelseType.getKode()));

        if (k9abakusEnabled) {
            try {
                return k9AbakusTjeneste.hentArbeidsforholdIPerioden(request)
                    .stream()
                    .collect(Collectors.groupingBy(this::mapTilArbeidsgiver,
                        flatMapping(im -> Stream.of(EksternArbeidsforholdRef.ref(im.getArbeidsforholdId() != null ? im.getArbeidsforholdId().getEksternReferanse() : null)), Collectors.toSet())));
            } catch (Exception ignored) {

            }
        }


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
