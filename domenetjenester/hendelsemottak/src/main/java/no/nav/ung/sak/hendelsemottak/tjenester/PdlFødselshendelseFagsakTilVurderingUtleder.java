package no.nav.ung.sak.hendelsemottak.tjenester;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.*;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.hendelser.FødselHendelse;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApplicationScoped
@HendelseTypeRef("PDL_FORELDERBARNRELASJON")
public class PdlFødselshendelseFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {

    private static final Logger logger = LoggerFactory.getLogger(PdlFødselshendelseFagsakTilVurderingUtleder.class);
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste;
    private PersonopplysningRepository personopplysningRepository;
    private Pdl pdlKlient;

    public PdlFødselshendelseFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public PdlFødselshendelseFagsakTilVurderingUtleder(BehandlingRepository behandlingRepository,
                                                       UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                                       FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste,
                                                       PersonopplysningRepository personopplysningRepository,
                                                       Pdl pdlKlient) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.finnFagsakerForAktørTjeneste = finnFagsakerForAktørTjeneste;
        this.personopplysningRepository = personopplysningRepository;
        this.pdlKlient = pdlKlient;
    }


    @Override
    /**
     * Finner og returnerer et kart \(`Map`\) over relevante `Fagsak`-instanser med tilhørende årsak og periode for vurdering,
     * basert på en gitt fødselshendelse \(`Hendelse`\). Metoden sjekker gyldige aktørIder, henter informasjon om barnet,
     * finner aktuell dato, og vurderer hver forelder-aktør sin fagsak for om den skal vurderes og om det er ny informasjon.
     *
     * @param hendelse fødselshendelsen som inneholder informasjon om barn og foreldre
     * @return et kart over `Fagsak` til `ÅrsakOgPeriode` for saker som skal vurderes for revurdering
     */
    public Map<Fagsak, ÅrsakOgPeriode> finnFagsakerTilVurdering(Hendelse hendelse) {
        FødselHendelse fødselsHendelse = (FødselHendelse) hendelse;
        String hendelseId = fødselsHendelse.getHendelseInfo().getHendelseId();

        List<AktørId> forelderAktørIder = fødselsHendelse.getHendelseInfo().getAktørIder();

        PersonIdent barnIdentPersonIdent = fødselsHendelse.getBarnIdent();
        String barnIdent = barnIdentPersonIdent.getIdent();
        Optional<AktørId> aktørIdBarn = pdlKlient.hentAktørIdForPersonIdent(barnIdent).map(AktørId::new);
        if (aktørIdBarn.isEmpty()) {
            logger.warn("Fødselshendelse med hendelseId={} har barn uten gyldig aktørId. Ignorerer hendelse.", hendelseId);
            return Collections.emptyMap();
        }

        Person barnInfo = hentPersonInformasjon(barnIdent);

        LocalDate aktuellDato = finnAktuellDato(barnInfo);

        var fagsakÅrsakMap = new HashMap<Fagsak, ÅrsakOgPeriode>();

        for (AktørId aktør : forelderAktørIder) {
            Optional<Fagsak> fagsak = finnFagsakerForAktørTjeneste.hentRelevantFagsakForAktørSomSøker(aktør, aktuellDato);

            fagsak.ifPresent(f -> {
                    if (deltarIProgramPåHendelsedato(f, aktuellDato, hendelseId) && erNyInformasjonIHendelsen(f, aktørIdBarn.get(), aktuellDato, hendelseId)) {
                        fagsakÅrsakMap.put(f, new ÅrsakOgPeriode(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, DatoIntervallEntitet.fraOgMedTilOgMed(aktuellDato, fagsak.get().getPeriode().getTomDato())));
                    }
                }
            );
        }

        return fagsakÅrsakMap;
    }


    /**
     * idempotens-sjekk for å hindre at det opprettes flere revurderinger fra samme hendelse.
     * hindrer også revurdering hvis hendelsen kommer etter at behandlingen er oppdatert med ny data.
     */
    private boolean erNyInformasjonIHendelsen(Fagsak fagsak, AktørId barnAktørId, LocalDate fødselsdato, String hendelseId) {
        Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandlingOpt.isEmpty()) {
            logger.info("Det er ingen behandling på fagsak. Ignorer hendelse");
            return false;
        }

        Behandling behandling = behandlingOpt.get();
        PersonopplysningGrunnlagEntitet personopplysninger = personopplysningRepository.hentPersonopplysninger(behandling.getId());
        if (personopplysninger != null) {
            for (PersonopplysningEntitet personopplysning : personopplysninger.getGjeldendeVersjon().getPersonopplysninger()) {
                // Sjekker om behandlingen allerede har registrert barn med samme aktørId og fødselsdato
                if (barnAktørId.equals(personopplysning.getAktørId()) && Objects.equals(fødselsdato, personopplysning.getFødselsdato())) {
                    logger.info("Persondata på behandling {} for {} var allerede oppdatert med riktig fødselsdato. Trigget av hendelse {}.", behandling.getUuid(), fagsak.getSaksnummer(), hendelseId);
                    return false;
                }
            }
        }
        return true;
    }


    private Person hentPersonInformasjon(String ident) {
        var query = new HentPersonQueryRequest();
        query.setIdent(ident);
        var projection = new PersonResponseProjection()
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
            .forelderBarnRelasjon(
                new ForelderBarnRelasjonResponseProjection()
                    .relatertPersonsRolle()
                    .relatertPersonsIdent()
                    .minRolleForPerson()
            );
        return pdlKlient.hentPerson(query, projection, List.of(Behandlingsnummer.UNGDOMSYTELSEN));
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
