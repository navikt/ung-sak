package no.nav.ung.sak.web.app.tjenester.behandling.personopplysning;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.PdlKlient;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.ung.sak.mottak.inntektsmelding.InntektsmeldingParser;
import no.nav.ung.sak.mottak.inntektsmelding.v1.MottattDokumentWrapperInntektsmelding;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

@Dependent
public class FinnUnikeAktører {

    private final MottatteDokumentRepository mottatteDokumentRepository;
    private final PdlKlient pdlKlient;

    @Inject
    public FinnUnikeAktører(MottatteDokumentRepository mottatteDokumentRepository, PdlKlient pdlKlient) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.pdlKlient = pdlKlient;
    }

    public AktørerForSak finnUnikeAktørerMedDokumenter(Fagsak fagsak) {

        var mottatteDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(fagsak.getId());

        var personidenter = mottatteDokumenter.stream().map(this::hentPersonIdentFraDokument).collect(Collectors.toSet());


        var aktørIdPersonidentMap = new HashMap<String, Set<String>>();

        personidenter.stream().flatMap(Optional::stream).map(PersonIdent::getIdent).forEach(ident -> {
            var aktørId = pdlKlient.hentAktørIdForPersonIdent(ident);
            aktørId.ifPresent(id ->
                {
                    var eksisterende = aktørIdPersonidentMap.getOrDefault(id, new HashSet<>());
                    eksisterende.add(ident);
                    aktørIdPersonidentMap.put(id, eksisterende);
                }
            );

        });

        var personidenterForAktør = aktørIdPersonidentMap.entrySet().stream().map(e -> new AktørerForSak.PersonidenterForAktør(new AktørId(e.getKey()), e.getValue().stream().map(PersonIdent::new).collect(Collectors.toSet()))).collect(Collectors.toSet());
        return new AktørerForSak(personidenterForAktør, fagsak.getSaksnummer());

    }

    private Optional<PersonIdent> hentPersonIdentFraDokument(MottattDokument d) {
        if (Brevkode.SØKNAD_TYPER.contains(d.getType())) {
            var søknad = new SøknadParser().parseSøknad(d);
            return Optional.of(new PersonIdent(søknad.getSøker().getPersonIdent().getVerdi()));
        } else if (d.getType().equals(Brevkode.INNTEKTSMELDING)) {
            var wrapper = new InntektsmeldingParser().xmlTilWrapper(d);
            if (wrapper instanceof MottattDokumentWrapperInntektsmelding wrapperV1) {
                return Optional.of(new PersonIdent(wrapperV1.getArbeidstaker()));
            } else if (wrapper instanceof no.nav.ung.sak.mottak.inntektsmelding.v2.MottattDokumentWrapperInntektsmelding wrapperV2) {
                return Optional.of(new PersonIdent(wrapperV2.getArbeidstaker()));
            }
            throw new IllegalArgumentException("Kunne ikke hente personident fra xml");
        }

        return Optional.empty();

    }

    public static boolean erXml(String payload) {
        return payload != null && payload.substring(0, Math.min(50, payload.length())).trim().startsWith("<");
    }

    public static boolean erJson(String payload) {
        return payload != null && payload.substring(0, Math.min(50, payload.length())).trim().startsWith("{");
    }

}
