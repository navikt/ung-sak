package no.nav.k9.sak.domene.person.tps;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.aktør.Adresseinfo;
import no.nav.k9.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.k9.sak.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningSikkerhetsbegrensing;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonhistorikkPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonhistorikkSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.feil.Kodeverdi;
import no.nav.tjeneste.virksomhet.person.v3.feil.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Periode;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkResponse;
import no.nav.vedtak.felles.integrasjon.aktør.klient.DetFinnesFlereAktørerMedSammePersonIdentException;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;
import no.nav.vedtak.felles.integrasjon.person.PersonConsumer;

@ApplicationScoped
public class TpsAdapterImpl implements TpsAdapter {

    private AktørTjeneste aktørTjeneste;
    private PersonConsumer personConsumer;
    private TpsOversetter tpsOversetter;

    public TpsAdapterImpl() {
    }

    @Inject
    public TpsAdapterImpl(AktørTjeneste aktørTjeneste,
                          PersonConsumer personConsumer,
                          TpsOversetter tpsOversetter) {
        this.aktørTjeneste = aktørTjeneste;
        this.personConsumer = personConsumer;
        this.tpsOversetter = tpsOversetter;
    }

    @Override
    public Optional<AktørId> hentAktørIdForPersonIdent(PersonIdent personIdent) {
        if (personIdent.erFdatNummer()) {
            // har ikke tildelt personnr
            return Optional.empty();
        }
        try {
            return aktørTjeneste.hentAktørIdForPersonIdent(personIdent);
        } catch (DetFinnesFlereAktørerMedSammePersonIdentException e) { // NOSONAR
            // Her sorterer vi ut dødfødte barn
            return Optional.empty();
        }
    }

    @Override
    public Optional<PersonIdent> hentIdentForAktørId(AktørId aktørId) {
        return aktørTjeneste.hentPersonIdentForAktørId(aktørId);
    }

    private Personinfo håndterPersoninfoRespons(AktørId aktørId, HentPersonRequest request)
            throws HentPersonPersonIkkeFunnet, HentPersonSikkerhetsbegrensning {
        HentPersonResponse response = personConsumer.hentPersonResponse(request);
        Person person = response.getPerson();
        if (!(person instanceof Bruker)) {
            throw TpsFeilmeldinger.FACTORY.ukjentBrukerType().toException();
        }
        return tpsOversetter.tilBrukerInfo(aktørId, (Bruker) person);
    }

    private Personhistorikkinfo håndterPersonhistorikkRespons(HentPersonhistorikkRequest request, String aktørId)
            throws HentPersonhistorikkSikkerhetsbegrensning, HentPersonhistorikkPersonIkkeFunnet {
        HentPersonhistorikkResponse response = personConsumer.hentPersonhistorikkResponse(request);
        return tpsOversetter.tilPersonhistorikkInfo(aktørId, response);
    }

    @Override
    public Personinfo hentKjerneinformasjon(PersonIdent personIdent, AktørId aktørId) {
        HentPersonRequest request = new HentPersonRequest();
        request.setAktoer(TpsUtil.lagPersonIdent(personIdent.getIdent()));
        request.getInformasjonsbehov().add(Informasjonsbehov.ADRESSE);
        request.getInformasjonsbehov().add(Informasjonsbehov.KOMMUNIKASJON);
        request.getInformasjonsbehov().add(Informasjonsbehov.FAMILIERELASJONER);
        try {
            return håndterPersoninfoRespons(aktørId, request);
        } catch (HentPersonPersonIkkeFunnet e) {
            throw TpsFeilmeldinger.FACTORY.fantIkkePerson(e).toException();
        } catch (HentPersonSikkerhetsbegrensning e) {
            throw TpsFeilmeldinger.FACTORY.tpsUtilgjengeligSikkerhetsbegrensning(formatter(e.getFaultInfo()), e).toException();
        }
    }

    @Override
    public PersoninfoBasis hentKjerneinformasjonBasis(PersonIdent personIdent, AktørId aktørId) {
        HentPersonRequest request = new HentPersonRequest();
        request.setAktoer(TpsUtil.lagPersonIdent(personIdent.getIdent()));
        try {
            HentPersonResponse response = personConsumer.hentPersonResponse(request);
            Person person = response.getPerson();
            if (!(person instanceof Bruker)) {
                throw TpsFeilmeldinger.FACTORY.ukjentBrukerType().toException();
            }
            return tpsOversetter.tilBrukerInfoBasis(aktørId, (Bruker) person);
        } catch (HentPersonPersonIkkeFunnet e) {
            throw TpsFeilmeldinger.FACTORY.fantIkkePerson(e).toException();
        } catch (HentPersonSikkerhetsbegrensning e) {
            throw TpsFeilmeldinger.FACTORY.tpsUtilgjengeligSikkerhetsbegrensning(formatter(e.getFaultInfo()), e).toException();
        }
    }

    @Override
    public Personhistorikkinfo hentPersonhistorikk(AktørId aktørId, no.nav.k9.sak.typer.Periode historikkPeriode) {
        HentPersonhistorikkRequest request = new HentPersonhistorikkRequest();
        AktoerId aktoerId = new AktoerId();
        aktoerId.setAktoerId(aktørId.getId());
        Periode periode = new Periode();

        periode.setFom(DateUtil.convertToXMLGregorianCalendar(historikkPeriode.getFom()));
        periode.setTom(DateUtil.convertToXMLGregorianCalendar(historikkPeriode.getTom().plusDays(1))); // må konvertere til start neste dag

        request.setAktoer(aktoerId);
        request.setPeriode(periode);

        try {
            return håndterPersonhistorikkRespons(request, String.valueOf(aktørId));
        } catch (HentPersonhistorikkPersonIkkeFunnet e) {
            throw TpsFeilmeldinger.FACTORY.fantIkkePersonhistorikkForAktørId(e).toException();
        } catch (HentPersonhistorikkSikkerhetsbegrensning e) {
            throw TpsFeilmeldinger.FACTORY.tpsUtilgjengeligSikkerhetsbegrensning(formatter(e.getFaultInfo()), e).toException();
        }
    }

    @Override
    public Adresseinfo hentAdresseinformasjon(PersonIdent personIdent) {
        HentPersonRequest request = new HentPersonRequest();
        request.getInformasjonsbehov().add(Informasjonsbehov.ADRESSE);
        request.setAktoer(TpsUtil.lagPersonIdent(personIdent.getIdent()));
        try {
            HentPersonResponse response = personConsumer.hentPersonResponse(request);
            Person person = response.getPerson();
            return tpsOversetter.tilAdresseInfo(person);
        } catch (HentPersonPersonIkkeFunnet e) {
            throw TpsFeilmeldinger.FACTORY.fantIkkePerson(e).toException();
        } catch (HentPersonSikkerhetsbegrensning e) {
            throw TpsFeilmeldinger.FACTORY.tpsUtilgjengeligSikkerhetsbegrensning(formatter(e.getFaultInfo()), e).toException();
        }
    }

    @Override
    public GeografiskTilknytning hentGeografiskTilknytning(PersonIdent personIdent) {
        HentGeografiskTilknytningRequest request = new HentGeografiskTilknytningRequest();
        request.setAktoer(TpsUtil.lagPersonIdent(personIdent.getIdent()));
        try {
            HentGeografiskTilknytningResponse response = personConsumer.hentGeografiskTilknytning(request);
            return tpsOversetter.tilGeografiskTilknytning(response.getGeografiskTilknytning(), response.getDiskresjonskode());
        } catch (HentGeografiskTilknytningSikkerhetsbegrensing e) {
            throw TpsFeilmeldinger.FACTORY.tpsUtilgjengeligGeografiskTilknytningSikkerhetsbegrensing(formatter(e.getFaultInfo()), e).toException();
        } catch (HentGeografiskTilknytningPersonIkkeFunnet e) {
            throw TpsFeilmeldinger.FACTORY.geografiskTilknytningIkkeFunnet(e).toException();
        }
    }

    private String formatter(Sikkerhetsbegrensning faultInfo) {
        if (faultInfo == null) {
            return null;
        }
        var begr = faultInfo.getSikkerhetsbegrensning();
        String sikkerhetsbegrensninger = begr == null ? null : begr.stream().map(Kodeverdi::getValue).collect(Collectors.joining(", "));
        String feilaarsak = faultInfo.getFeilaarsak();
        String feilkilde = faultInfo.getFeilkilde();
        String feilmelding = faultInfo.getFeilmelding();

        return faultInfo.getClass().getSimpleName() + "<"
            + (feilaarsak == null ? "" : "feilaarsak=" + feilaarsak)
            + (feilkilde == null ? "" : ", feilkilde=" + feilkilde)
            + (feilmelding == null ? "" : ", feilmelding=" + feilmelding)
            + (sikkerhetsbegrensninger == null ? "" : ", sikkerhetsbegrensninger=[" + sikkerhetsbegrensninger + "]")
            + ">";
    }

    @Override
    public List<GeografiskTilknytning> hentDiskresjonskoderForFamilierelasjoner(PersonIdent personIdent) {
        HentPersonRequest request = new HentPersonRequest();
        request.setAktoer(TpsUtil.lagPersonIdent(personIdent.getIdent()));
        request.getInformasjonsbehov().add(Informasjonsbehov.FAMILIERELASJONER);
        try {
            HentPersonResponse response = personConsumer.hentPersonResponse(request);
            Person person = response.getPerson();
            return tpsOversetter.tilDiskresjonsKoder(person);
        } catch (HentPersonPersonIkkeFunnet e) {
            throw TpsFeilmeldinger.FACTORY.fantIkkePerson(e).toException();
        } catch (HentPersonSikkerhetsbegrensning e) {
            throw TpsFeilmeldinger.FACTORY.tpsUtilgjengeligSikkerhetsbegrensning(formatter(e.getFaultInfo()), e).toException();
        }
    }
}
