package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.alene;

import java.util.Collection;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadAngittPersonEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.Språk;
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.AnnenForelder.SituasjonType;
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerMidlertidigAlene;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP_MA")
@DokumentGruppeRef(Brevkode.SØKNAD_OMS_UTVIDETRETT_MA_KODE)
public class DokumentmottakerSøknadUtvidetRettAlene implements Dokumentmottaker {

    private SøknadRepository søknadRepository;

    private MottatteDokumentRepository mottatteDokumentRepository;

    private PersoninfoAdapter personinfoAdapter;

    DokumentmottakerSøknadUtvidetRettAlene() {
        // for CDI proxy
    }

    @Inject
    DokumentmottakerSøknadUtvidetRettAlene(BehandlingRepositoryProvider repositoryProvider,
                                           PersoninfoAdapter personinfoAdapter,
                                           MottatteDokumentRepository mottatteDokumentRepository) {
        this.personinfoAdapter = personinfoAdapter;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    @Override
    public void lagreDokumentinnhold(Collection<MottattDokument> dokumenter, Behandling behandling) {
        Long behandlingId = behandling.getId();

        for (MottattDokument dokument : dokumenter) {
            Søknad søknad = JsonUtils.fromString(dokument.getPayload(), Søknad.class);
            dokument.setBehandlingId(behandlingId);
            dokument.setInnsendingstidspunkt(søknad.getMottattDato().toLocalDateTime());
            mottatteDokumentRepository.lagre(dokument, DokumentStatus.BEHANDLER);
            // Søknadsinnhold som persisteres "lokalt" i k9-sak
            persister(søknad, behandling);
        }
    }

    void persister(Søknad søknad, Behandling behandling) {
        var behandlingId = behandling.getId();
        var søknadInnhold = (OmsorgspengerMidlertidigAlene) søknad.getYtelse();

        lagreSøknad(behandlingId, søknad, søknadInnhold);
    }

    private void lagreSøknad(Long behandlingId, Søknad søknad, OmsorgspengerMidlertidigAlene innsendt) {
        var søknadsperiode = innsendt.getSøknadsperiode();
        boolean elektroniskSøknad = true;
        DatoIntervallEntitet datoIntervall = søknadsperiode == null
            ? DatoIntervallEntitet.fraOgMed(søknad.getMottattDato().toLocalDate())
            : DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiode.getFraOgMed(), søknadsperiode.getFraOgMed());

        var søknadBuilder = new SøknadEntitet.Builder()
            .medSøknadsperiode(datoIntervall)
            .medElektroniskRegistrert(elektroniskSøknad)
            .medMottattDato(søknad.getMottattDato().toLocalDate())
            .medErEndringssøknad(false)
            .medSøknadsdato(søknad.getMottattDato().toLocalDate())
            .medSpråkkode(getSpråkValg(søknad.getSpråk()))
        ;
        if (innsendt.getBarn() != null) {
            for (var barn : innsendt.getBarn()) {
                var barnAktørId = personinfoAdapter.hentAktørIdForPersonIdent(new PersonIdent(barn.getPersonIdent().getVerdi()))
                    .orElseThrow(() -> new IllegalArgumentException("Mangler personIdent for søknadId=" + søknad.getSøknadId()));
                søknadBuilder.leggTilAngittPerson(new SøknadAngittPersonEntitet(barnAktørId, RelasjonsRolleType.BARN));
            }
        }

        var annenForelder = innsendt.getAnnenForelder();
        if (annenForelder.getPersonIdent() != null) {
            var aktørId = personinfoAdapter.hentAktørIdForPersonIdent(new PersonIdent(annenForelder.getPersonIdent().getVerdi())).orElseThrow();
            String tilleggsopplysninger = annenForelder.getSituasjonBeskrivelse();
            String situasjon = Optional.ofNullable(annenForelder.getSituasjonType()).map(SituasjonType::name).orElse(null);
            søknadBuilder.leggTilAngittPerson(new SøknadAngittPersonEntitet(aktørId, RelasjonsRolleType.ANNEN_PART_FRA_SØKNAD, situasjon, tilleggsopplysninger));
        } else {
            throw new UnsupportedOperationException("ikke implementert ennå støtte for annenforelder uten ident");
        }

        var søknadEntitet = søknadBuilder.build();
        søknadRepository.lagreOgFlush(behandlingId, søknadEntitet);
    }

    private Språkkode getSpråkValg(Språk språk) {
        if (språk != null) {
            return Språkkode.fraKode(språk.dto.toUpperCase());
        }
        return Språkkode.UDEFINERT;
    }

    @Override
    public BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    }

}
