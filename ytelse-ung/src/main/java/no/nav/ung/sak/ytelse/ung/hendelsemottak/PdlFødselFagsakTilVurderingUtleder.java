package no.nav.ung.sak.ytelse.ung.hendelsemottak;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.Foedselsdato;
import no.nav.k9.felles.integrasjon.pdl.FoedselsdatoResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.ForelderBarnRelasjon;
import no.nav.k9.felles.integrasjon.pdl.ForelderBarnRelasjonResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.ForelderBarnRelasjonRolle;
import no.nav.k9.felles.integrasjon.pdl.HentIdenterBolkQueryRequest;
import no.nav.k9.felles.integrasjon.pdl.HentIdenterBolkResultResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.HentPersonQueryRequest;
import no.nav.k9.felles.integrasjon.pdl.IdentGruppe;
import no.nav.k9.felles.integrasjon.pdl.IdentInformasjon;
import no.nav.k9.felles.integrasjon.pdl.IdentInformasjonResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.PdlKlient;
import no.nav.k9.felles.integrasjon.pdl.Person;
import no.nav.k9.felles.integrasjon.pdl.PersonResponseProjection;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.hendelsemottak.tjenester.FagsakerTilVurderingUtleder;
import no.nav.ung.sak.hendelsemottak.tjenester.HendelseTypeRef;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.ytelse.ung.periode.UngdomsprogramPeriodeRepository;

@ApplicationScoped
@HendelseTypeRef("PDL_FØDSEL")
public class PdlFødselFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {

    private static final Logger logger = LoggerFactory.getLogger(PdlFødselFagsakTilVurderingUtleder.class);
    public static final Set<ForelderBarnRelasjonRolle> AKTUELLE_RELASJONSROLLER = Set.of(ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.MEDMOR);
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste;
    private PersonopplysningRepository personopplysningRepository;
    private PdlKlient pdlKlient;

    public PdlFødselFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public PdlFødselFagsakTilVurderingUtleder(BehandlingRepository behandlingRepository,
                                              UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                              FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste,
                                              PersonopplysningRepository personopplysningRepository,
                                              PdlKlient pdlKlient) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.finnFagsakerForAktørTjeneste = finnFagsakerForAktørTjeneste;
        this.personopplysningRepository = personopplysningRepository;
        this.pdlKlient = pdlKlient;
    }


    @Override
    public Map<Fagsak, BehandlingÅrsakType> finnFagsakerTilVurdering(Hendelse hendelse) {
        List<AktørId> aktører = hendelse.getHendelseInfo().getAktørIder();
        String hendelseId = hendelse.getHendelseInfo().getHendelseId();
        var fagsakÅrsakMap = new HashMap<Fagsak, BehandlingÅrsakType>();

        for (AktørId aktør : aktører) {
            var personInfo = hentPersonInformasjon(aktør);
            var aktuellDato = finnAktuellDato(personInfo);

            // Sjekker om det gjelder fødselshendelse for barn av søker
            // Finner først aktørid til foreldre
            var aktørIdenter = finnAktørIdForPersonerRelatertTil(personInfo);
            // ser så etter eksisterende fagsaker på foreldre som trengs å oppdateres med fødselsdato
            aktørIdenter.stream()
                .map(it -> finnFagsakerForAktørTjeneste.hentRelevantFagsakForAktørSomSøker(it, aktuellDato))
                .flatMap(Optional::stream)
                .filter(f -> deltarIProgramPåHendelsedato(f, aktuellDato, hendelseId))
                .filter(f -> erNyInformasjonIHendelsen(f, aktør, aktuellDato, hendelseId))
                .forEach(f -> fagsakÅrsakMap.put(f, BehandlingÅrsakType.RE_HENDELSE_FØDSEL));
        }

        return fagsakÅrsakMap;
    }



    /**
     * idempotens-sjekk for å hindre at det opprettes flere revurderinger fra samme hendelse.
     * hindrer også revurdering hvis hendelsen kommer etter at behandlingen er oppdatert med ny data.
     */
    private boolean erNyInformasjonIHendelsen(Fagsak fagsak, AktørId aktør, LocalDate fødselsdato, String hendelseId) {
        Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandlingOpt.isEmpty()) {
            logger.info("Det er ingen behandling på fagsak. Ignorer hendelse");
            return false;
        }

        Behandling behandling = behandlingOpt.get();
        PersonopplysningGrunnlagEntitet personopplysninger = personopplysningRepository.hentPersonopplysninger(behandling.getId());
        if (personopplysninger != null) {
            for (PersonopplysningEntitet personopplysning : personopplysninger.getGjeldendeVersjon().getPersonopplysninger()) {
                if (aktør.equals(personopplysning.getAktørId()) && Objects.equals(fødselsdato, personopplysning.getFødselsdato())) {
                    logger.info("Persondata på behandling {} for {} var allerede oppdatert med riktig dødsdato. Trigget av hendelse {}.", behandling.getUuid(), fagsak.getSaksnummer(), hendelseId);
                    return false;
                }
            }
        }
        return true;
    }


    private Person hentPersonInformasjon(AktørId aktør) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktør.getAktørId());
        var projection = new PersonResponseProjection()
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
            .forelderBarnRelasjon(new ForelderBarnRelasjonResponseProjection().relatertPersonsRolle()
                .relatertPersonsIdent().minRolleForPerson());
        return pdlKlient.hentPerson(query, projection);
    }

    private Set<AktørId> finnAktørIdForPersonerRelatertTil(Person personFraPdl) {
        var relaterteIdenter = personFraPdl.getForelderBarnRelasjon()
            .stream()
            .filter(it -> AKTUELLE_RELASJONSROLLER.contains(it.getRelatertPersonsRolle()))
            .map(ForelderBarnRelasjon::getRelatertPersonsIdent)
            .toList();

        var hentIdenterBolkQueryRequest = new HentIdenterBolkQueryRequest();
        hentIdenterBolkQueryRequest.setIdenter(relaterteIdenter);
        hentIdenterBolkQueryRequest.setGrupper(List.of(IdentGruppe.AKTORID));
        hentIdenterBolkQueryRequest.setHistorikk(true);
        var hentIdenterBolkProjection = new HentIdenterBolkResultResponseProjection().identer(
            new IdentInformasjonResponseProjection()
                .ident());
        var hentIdenterBolkResults = pdlKlient.hentIdenterBolkResults(hentIdenterBolkQueryRequest, hentIdenterBolkProjection);
        return hentIdenterBolkResults.stream()
            .flatMap(it -> it.getIdenter().stream().map(IdentInformasjon::getIdent))
            .map(AktørId::new)
            .collect(Collectors.toSet());
    }

    private boolean deltarIProgramPåHendelsedato(Fagsak fagsak, LocalDate relevantDato, String hendelseId) {
        Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandlingOpt.isEmpty()) {
            logger.info("Det er ingen behandling på fagsak. Ignorer hendelse");
            return false;
        }

        Behandling behandling = behandlingOpt.get();
        var periodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId());
        if (periodeGrunnlag.isPresent()) {
            var harIngenPerioderEtterHendelseDato = periodeGrunnlag.get().getUngdomsprogramPerioder().getPerioder().stream().noneMatch(p -> p.getPeriode().getTomDato().isAfter(relevantDato));
            if (harIngenPerioderEtterHendelseDato) {
                logger.info("Datagrunnlag på behandling {} for {} hadde ingen perioder med ungdomsprogram etter hendelsedato. Trigget av hendelse {}.", behandling.getUuid(), fagsak.getSaksnummer(), hendelseId);
                return false;
            }
        }
        return true;
    }

    private LocalDate finnAktuellDato(Person personFraPdl) {
        return personFraPdl.getFoedselsdato().stream()
            .map(Foedselsdato::getFoedselsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
    }

}
