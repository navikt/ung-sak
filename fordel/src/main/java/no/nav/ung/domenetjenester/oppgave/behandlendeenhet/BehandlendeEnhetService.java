package no.nav.ung.domenetjenester.oppgave.behandlendeenhet;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.person.Diskresjonskode;
import no.nav.ung.kodeverk.produksjonsstyring.OmrådeTema;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.typer.AktørId;

import java.util.List;
import java.util.Objects;

@Dependent
public class BehandlendeEnhetService {

    private final PersoninfoAdapter personinfoAdapter;
    private final no.nav.ung.sak.produksjonsstyring.behandlingenhet.EnhetsTjeneste enhetsTjeneste;

    @Inject
    public BehandlendeEnhetService(PersoninfoAdapter personinfoAdapter, no.nav.ung.sak.produksjonsstyring.behandlingenhet.EnhetsTjeneste enhetsTjeneste) {
        this.personinfoAdapter = personinfoAdapter;
        this.enhetsTjeneste = enhetsTjeneste;
    }

    public BehandlendeEnhet hentBehandlendeEnhet(OmrådeTema tema, BehandlingTema behandlingTema, AktørId hovedAktør) {
        Objects.requireNonNull(behandlingTema, "behandlingTema manglet");
        FagsakYtelseType ytelseType = switch (behandlingTema) {
            case UNGDOMSPROGRAMYTELSEN -> FagsakYtelseType.UNGDOMSYTELSE;
            //TODO legg inn aktivitetspenger her når det er klart.
            // Behandlingstema er ikke utledet ennå (f.eks. klager). Vi vet da ikke hvilken ytelse det gjelder,
            // og må sende med behandlingsnummer for alle aktuelle ytelser i PDL-oppslagene, jf. BehandlingsnummerMapper.
            case UDEFINERT -> FagsakYtelseType.UDEFINERT;
        };
        GeografiskTilknytning gjeldendeGeografiskTilknytning = hentGjeldendeGeografiskeTilknytning(hovedAktør, ytelseType);
        return finnBehandledeEnhet(gjeldendeGeografiskTilknytning, tema, behandlingTema);
    }

    BehandlendeEnhet finnBehandledeEnhet(GeografiskTilknytning geografiskTilknytning, OmrådeTema tema, BehandlingTema behandlingTema) {

        List<OrganisasjonsEnhet> aktiveEnheter = enhetsTjeneste.hentFordelingEnhetId(tema, behandlingTema, geografiskTilknytning);

        if (aktiveEnheter.isEmpty()) {
            throw new IllegalStateException("Forventet å få minst en behandlende enhet: tema=" + tema);
        }
        if (aktiveEnheter.size() != 1) {
            throw new IllegalStateException(String.format("Forventet å få én behandlende enhet for tema %s, fikk %s.", tema, aktiveEnheter.size()));
        }
        OrganisasjonsEnhet organisasjonsenhet = aktiveEnheter.get(0);
        return new BehandlendeEnhet(organisasjonsenhet.getEnhetId(), organisasjonsenhet.getEnhetNavn());
    }

    private GeografiskTilknytning hentGjeldendeGeografiskeTilknytning(AktørId hovedAktør, FagsakYtelseType ytelseType) {
        final GeografiskTilknytning geografiskTilknytningHovedAktør;
        if (hovedAktør != null) {
            geografiskTilknytningHovedAktør = hentGeografiskTilknytning(hovedAktør, ytelseType);
            if (Diskresjonskode.KODE6.equals(geografiskTilknytningHovedAktør.getDiskresjonskode())) {
                return geografiskTilknytningHovedAktør;
            }
        } else {
            geografiskTilknytningHovedAktør = new GeografiskTilknytning(null, null);
        }


        return geografiskTilknytningHovedAktør;
    }


    private GeografiskTilknytning hentGeografiskTilknytning(AktørId aktørId, FagsakYtelseType ytelseType) {
        return personinfoAdapter.hentIdentForAktørId(aktørId)
            .map(ident -> personinfoAdapter.hentGeografiskTilknytning(ident, ytelseType))
            .orElse(null);
    }

}
